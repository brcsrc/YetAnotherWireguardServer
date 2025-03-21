package com.brcsrc.yaws.service;

import java.util.List;
import java.util.Optional;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import com.brcsrc.yaws.utility.FilepathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.requests.ListNetworkClientsRequest;
import com.brcsrc.yaws.persistence.ClientRepository;
import com.brcsrc.yaws.persistence.NetworkClientRepository;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.shell.Executor;
import com.brcsrc.yaws.utility.IPUtils;

@Service
public class NetworkClientService {

    private final NetworkClientRepository netClientRepository;
    private final NetworkRepository networkRepository;
    private final ClientRepository clientRepository;

    private static final Logger logger = LoggerFactory.getLogger(NetworkClientService.class);

    @Autowired
    public NetworkClientService(
            NetworkClientRepository netClientRepository,
            NetworkRepository networkRepository,
            ClientRepository clientRepository
    ) {
        this.netClientRepository = netClientRepository;
        this.networkRepository = networkRepository;
        this.clientRepository = clientRepository;
    }

    private Network checkNetworkExists(String networkName) {
        Optional<Network> network = this.networkRepository.findByNetworkName(networkName);
        if (network.isEmpty()) {
            String errMsg = String.format("network '%s' does not exist", networkName);
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        Network existingNetwork = network.get();
        logger.info(String.format("found existing network '%s'", existingNetwork.getNetworkName()));
        return existingNetwork;
    }

    public NetworkClient addClientToNetwork(CreateNetworkClientRequest request) {
        // check the requested client cidr is valid cidr or ip address
        boolean isValidClientCidr = IPUtils.isValidIpv4Cidr(request.getClientCidr());
        boolean isValidClientAddress = IPUtils.isValidIpv4Address(request.getClientCidr());
        if (!isValidClientCidr && !isValidClientAddress) {
            String errMsg = "client cidr is not a valid address or cidr block";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // check if the dns address is valid
        if (!IPUtils.isValidIpv4Address(request.getClientDns())) {
            String errMsg = "client dns is not a valid ip address";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // check if allowed ips block is valid cidr or ip address
        boolean isValidAllowedIpsCidr = IPUtils.isValidIpv4Cidr(request.getAllowedIps());
        boolean isValidAllowsIpsAddress = IPUtils.isValidIpv4Address(request.getAllowedIps());
        if (!isValidAllowedIpsCidr && !isValidAllowsIpsAddress) {
            String errMsg = "allowed ips is not a valid address or cidr block";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // check the network endpoint is valid
        if (!IPUtils.isValidEndpoint(request.getNetworkEndpoint())) {
            String errMsg = "network endpoint is not valid";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // check network name and client name are valid
        if (request.getNetworkName().length() > 64 || !request.getNetworkName().matches(Constants.CHAR_64_ALPHANUMERIC_REGEXP)) {
            String errMsg = "networkName is not valid";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        if (request.getClientName().length() > 64 || !request.getClientName().matches(Constants.CHAR_64_ALPHANUMERIC_REGEXP)) {
            String errMsg = "clientName is not valid";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // check requested network already exists
        Network existingNetwork = checkNetworkExists(request.getNetworkName());

        // check network can be placed in network based off requested cidr
        if (!IPUtils.isNetworkMemberInNetworkRange(existingNetwork.getNetworkCidr(), request.getClientCidr())) {
            String errMsg = "client cidr is outside of corresponding network cidr block";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // check network does not have a client with that address or name
        boolean addressOrNameAlreadyInUse = this.netClientRepository.existsByNetworkNameAndClientCidrOrClientName(
                request.getNetworkName(),
                request.getClientCidr(),
                request.getClientName()
        );
        if (addressOrNameAlreadyInUse) {
            String errMsg = String.format(
                    "network %s already has a client with requested name or address",
                    request.getNetworkName()
            );
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // client relevant fields
        Client client = new Client();
        client.setClientName(request.getClientName());
        client.setClientCidr(request.getClientCidr());
        client.setClientDns(request.getClientDns());
        client.setAllowedIps(request.getAllowedIps());
        client.setNetworkEndpoint(request.getNetworkEndpoint());
        client.setClientTag(request.getClientTag());

        // get these from existing network
        client.setNetworkListenPort(existingNetwork.getNetworkListenPort());
        client.setNetworkPublicKeyName(existingNetwork.getNetworkPublicKeyName());

        // build key names from client name
        client.setClientPrivateKeyName(client.getClientName() + "-private-key");
        client.setClientPublicKeyName(client.getClientName() + "-public-key");

        // get intended file paths
        final String CLIENT_PRIV_KEY_PATH = FilepathUtils.getClientKeyPath(existingNetwork.getNetworkName(), client.getClientPrivateKeyName());
        final String CLIENT_PUB_KEY_PATH = FilepathUtils.getClientKeyPath(existingNetwork.getNetworkName(), client.getClientPublicKeyName());
        final String CLIENT_CONFIG_PATH = FilepathUtils.getClientConfigPath(existingNetwork.getNetworkName(), client.getClientName());
        final String NETWORK_PUB_KEY_PATH = FilepathUtils.getNetworkKeyPath(existingNetwork.getNetworkName(), existingNetwork.getNetworkPublicKeyName());
        final String NETWORK_CONFIG_PATH = FilepathUtils.getNetworkConfigPath(existingNetwork.getNetworkName());

        // build client key pair
        logger.info(String.format("creating key pair: '%s', '%s'", CLIENT_PRIV_KEY_PATH, CLIENT_PUB_KEY_PATH));
        final String createKeyPairCommand = String.join(" ",
                "./create-key-pair",
                "--private-key-name", CLIENT_PRIV_KEY_PATH,
                "--public-key-name", CLIENT_PUB_KEY_PATH
        );
        ExecutionResult createKeyPairExecResult = Executor.runCommand(createKeyPairCommand);
        if (createKeyPairExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    createKeyPairCommand,
                    createKeyPairExecResult.getExitCode(),
                    createKeyPairExecResult.getStderr()));
            throw new InternalServerException("failed to create key pair");
        }

        // generate the client config
        logger.info(String.format("creating client configuration '%s'", CLIENT_CONFIG_PATH));
        final String createClientConfigCommand = String.join(" ",
                "./create-client-config",
                "--config-name", CLIENT_CONFIG_PATH,
                "--client-private-key-name", CLIENT_PRIV_KEY_PATH,
                "--client-cidr", client.getClientCidr(),
                "--client-dns", client.getClientDns(),
                "--network-public-key-name", NETWORK_PUB_KEY_PATH,
                "--network-endpoint", client.getNetworkEndpoint(),
                "--network-listen-port", String.valueOf(client.getNetworkListenPort()),
                "--allowed-ips", client.getAllowedIps()
        );
        ExecutionResult createClientConfigExecResult = Executor.runCommand(createClientConfigCommand);
        if (createClientConfigExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    createClientConfigCommand,
                    createClientConfigExecResult.getExitCode(),
                    createClientConfigExecResult.getStderr()));
            throw new InternalServerException("failed to create client configuration in system");
        }

        // add client to network in specified network config
        logger.info(String.format("adding peer '%s' to network config '%s'", client.getClientName(), existingNetwork.getNetworkName()));
        // the subnet that is added to network config needs to explicitly end in 32
        final String networkConfigFormatClientCidr = String.format("%s/32", client.getClientCidr().split("/")[0]);
        final String addPeerToNetworkCommand = String.join(" ",
                "./add-peer-to-network",
                "--config-name", NETWORK_CONFIG_PATH,
                "--interface-name", existingNetwork.getNetworkName(),
                "--client-cidr", networkConfigFormatClientCidr,
                "--client-public-key-name", CLIENT_PUB_KEY_PATH
        );
        ExecutionResult addPeerToNetExecResult = Executor.runCommand(addPeerToNetworkCommand);
        if (addPeerToNetExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    addPeerToNetworkCommand,
                    addPeerToNetExecResult.getExitCode(),
                    addPeerToNetExecResult.getStderr()));
            throw new InternalServerException("failed to add client to network config");
        }

        // save entities to database
        Client savedClient = this.clientRepository.save(client);
        NetworkClient networkClient = new NetworkClient();
        networkClient.setClient(savedClient);
        networkClient.setNetwork(existingNetwork);
        NetworkClient savedNetworkClient = this.netClientRepository.save(networkClient);
        logger.info("CreateNetworkClient operation successful");
        return savedNetworkClient;
    }

    @Async
    CompletableFuture<NetworkClient> asyncRemoveClientFromSystem(NetworkClient networkClient) {
        logger.info("asyncRemoveClientFromSystem called on thread: " + Thread.currentThread().getName());
        boolean errorsOnRemoval = false;

        // get intended file paths
        final String CLIENT_PRIV_KEY_PATH = FilepathUtils.getClientKeyPath(
                networkClient.getNetwork().getNetworkName(),
                networkClient.getClient().getClientPrivateKeyName()
        );
        final String CLIENT_PUB_KEY_PATH = FilepathUtils.getClientKeyPath(
                networkClient.getNetwork().getNetworkName(),
                networkClient.getClient().getClientPublicKeyName()
        );
        final String CLIENT_CONFIG_PATH = FilepathUtils.getClientConfigPath(
                networkClient.getNetwork().getNetworkName(),
                networkClient.getClient().getClientName()
        );
        final String NETWORK_CONFIG_PATH = FilepathUtils.getNetworkConfigPath(networkClient.getNetwork().getNetworkName());

        try {
            // remove peer from network
            logger.info(String.format(
                    "removing client '%s' from network '%s'",
                    networkClient.getClient().getClientName(),
                    networkClient.getNetwork().getNetworkName()));
            String networkConfigFormatClientCidr = String.format("%s/32", networkClient.getClient().getClientCidr().split("/")[0]);
            final String removeClientFromNetworkCmd = String.join(" ",
                    "./remove-peer-from-network",
                    "--config-name", NETWORK_CONFIG_PATH,
                    "--interface-name", networkClient.getNetwork().getNetworkName(),
                    "--client-cidr", networkConfigFormatClientCidr,
                    "--client-public-key-name", CLIENT_PUB_KEY_PATH
            );
            ExecutionResult removePeerFromNetworkExecRes = Executor.runCommand(removeClientFromNetworkCmd);
            if (removePeerFromNetworkExecRes.getExitCode() != 0) {
                errorsOnRemoval = true;
                logger.error(String.format(
                        "command: '%s' exited %s with reason: %s",
                        removeClientFromNetworkCmd,
                        removePeerFromNetworkExecRes.getExitCode(),
                        removePeerFromNetworkExecRes.getStderr()));
            }

            // check if client config exists
            logger.info(String.format("removing wireguard files for client '%s'", networkClient.getClient().getClientName()));
            var clientConfigFilePaths = List.of(
                    CLIENT_CONFIG_PATH,
                    CLIENT_PRIV_KEY_PATH,
                    CLIENT_PUB_KEY_PATH
            );
            for (String filepath : clientConfigFilePaths) {
                logger.info(String.format("deleting file '%s'", filepath));
                File file = new File(filepath);
                if (file.exists()) {
                    if (!file.delete()) {
                        errorsOnRemoval = true;
                        logger.error(String.format("failed to delete file '%s'", filepath));
                    }
                } else {
                    logger.info(String.format("file '%s' does not exist", filepath));
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
        if (errorsOnRemoval) {
            throw new CompletionException(new InternalServerException("error in cleaning up client"));
        }
        logger.info(String.format(
                "asyncRemoveClientFromSystem completed successfully for client %s",
                networkClient.getClient().getClientName())
        );
        return CompletableFuture.completedFuture(networkClient);
    }

    // used @Transactional annotation here because there is at least 2 db actions when
    // deleteNetworkClientByNetwork_NetworkNameAndClient_ClientName is called as CascadeType.REMOVE
    // is used on the NetworkClient.Client to remove the record from the  client table as well
    @Transactional
    public NetworkClient deleteNetworkClient(String networkName, String clientName) {
        // input validation
        if (!networkName.matches(Constants.CHAR_64_ALPHANUMERIC_REGEXP) || !clientName.matches(Constants.CHAR_64_ALPHANUMERIC_REGEXP)) {
            String errMsg = "network name or client name is invalid";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        logger.info(String.format(
                "deleting network client '%s' for network '%s'",
                clientName,
                networkName
        ));
        // check if network client exists
        NetworkClient existingNetworkClient = this.netClientRepository.findNetworkClientByNetwork_NetworkNameAndClient_ClientName(
                networkName,
                clientName
        );
        if (existingNetworkClient == null) {
            String errMsg = String.format(
                    "network client '%s' for network '%s' does not exist",
                    clientName,
                    networkName
                );
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // reuse async delete network client but wait for completion
        try {
            NetworkClient deletedNetworkClientResult = asyncRemoveClientFromSystem(existingNetworkClient).get();
            int deletedNetworkClientCount = this.netClientRepository.deleteNetworkClientByNetwork_NetworkNameAndClient_ClientName(
                    deletedNetworkClientResult.getNetwork().getNetworkName(),
                    deletedNetworkClientResult.getClient().getClientName()
            );
            if (deletedNetworkClientCount != 1) {
                logger.error(String.format("unexpected count of affected rows from deletion: %s", deletedNetworkClientCount));
                throw new InternalServerException("failed to remove client from database");
            }
        } catch (InterruptedException | ExecutionException exception) {
            logger.error(String.format(
                    "error in asyncRemoveClientFromSystem for client '%s' in network '%s': %s",
                    clientName,
                    networkName,
                    exception
            ));
            throw new InternalServerException("error in deleting network");
        }
        return existingNetworkClient;
    };

    // List Network Clients and return list of Clients objects from a single Network
    public List<Client> listNetworkClients(ListNetworkClientsRequest request) {
        checkNetworkExists(request.getNetworkName());
        // TODO pagination
        return this.netClientRepository.findClientsByNetworkName(request.getNetworkName());
    }

    // Describe a Network Client and return the Network and Client objects of that defined relationship
    public NetworkClient describeNetworkClient(String networkName, String clientName) {
        NetworkClient networkClient = this.netClientRepository.findByNetworkClientByNetworkNameAndClientName(networkName, clientName);
        if (networkClient == null) {
            String errMsg = String.format("client '%s' not found on network '%s'", clientName, networkName);
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errMsg);
        }
        return networkClient;
    }

    public ResponseEntity<Resource> getNetworkClientConfigFile(@PathVariable String networkName, @PathVariable String clientName) {
        // validate inputs before putting them in jpa queries
        if (networkName.length() > 64 || !networkName.matches(Constants.CHAR_64_ALPHANUMERIC_REGEXP)) {
            String errMsg = "networkName is not valid";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        if (clientName.length() > 64 || !clientName.matches(Constants.CHAR_64_ALPHANUMERIC_REGEXP)) {
            String errMsg = "clientName is not valid";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // this should return 400 if the network or the client does not exist
        NetworkClient existingNetworkClient = this.netClientRepository.findNetworkClientByNetwork_NetworkNameAndClient_ClientName(
                networkName,
                clientName
        );
        if (existingNetworkClient == null) {
            String errMsg = "network or client does not exist";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // check and get reference to the requested configuration file.
        // should be yaws responsibility to create and maintain the file. if not found return 500
        String configFilePath = FilepathUtils.getClientConfigPath(networkName, clientName);
        String configFileNotExistsErrMsg = "requested configuration file does not exist";
        File configFile;
        Resource configFileResource;
        try {
            configFile = new File(configFilePath);
            if (!configFile.exists()) {
                logger.error(configFileNotExistsErrMsg);
                throw new InternalServerException(configFileNotExistsErrMsg);
            }
            configFileResource = new FileSystemResource(configFile);
            logger.info(String.format("found config file '%s'", configFilePath));
        } catch (NullPointerException e) {
            logger.error(configFileNotExistsErrMsg);
            throw new InternalServerException(configFileNotExistsErrMsg);
        }

        // all error states should be handled before this block. if no errors return the file
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment;  filename=\"" + configFile.getName() + "\"")
                .body(configFileResource);
    }
}
