package com.brcsrc.yaws.service;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.model.requests.ListNetworkClientsRequest;
import com.brcsrc.yaws.persistence.ClientRepository;
import com.brcsrc.yaws.persistence.NetworkClientRepository;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.shell.Executor;
import com.brcsrc.yaws.utility.IPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

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
        // check network already exists
        Network existingNetwork = checkNetworkExists(request.getNetworkName());
        // check network can be placed in network based off requested cidr
        if (!IPUtils.isNetworkMemberInNetworkRange(existingNetwork.getNetworkCidr(), request.getClientCidr())) {
            String errMsg = "client cidr is outside of corresponding network cidr block";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        // check network does not have a client with that address
        boolean addressAlreadyInUse = this.netClientRepository.existsByNetworkNameAndClientCidr(
                request.getNetworkName(),
                request.getClientCidr()
        );
        if (addressAlreadyInUse) {
            String errMsg = String.format(
                    "network %s already has a client with address %s",
                    request.getNetworkName(),
                    request.getClientCidr()
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

        // create clients key pair
        logger.info(String.format("creating key pair: '%s', '%s'", client.getClientPrivateKeyName(), client.getClientPublicKeyName()));
        final String createKeyPairCommand = String.join(" ",
                "./create-key-pair",
                "--private-key-name", client.getClientPrivateKeyName(),
                "--public-key-name", client.getClientPublicKeyName()
        );
        ExecutionResult createKeyPairExecResult = Executor.runCommand(createKeyPairCommand);
        if (createKeyPairExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    createKeyPairCommand,
                    createKeyPairExecResult.getExitCode(),
                    createKeyPairExecResult.getStdout()));
            throw new InternalServerException("failed to create key pair");
        }

        // generate the client config
        final String createClientConfigCommand = String.join(" ",
                "./create-client-config",
                "--config-name", client.getClientName(),
                "--client-private-key-name", client.getClientPrivateKeyName(),
                "--client-cidr", client.getClientCidr(),
                "--client-dns", client.getClientDns(),
                "--network-public-key-name", existingNetwork.getNetworkPublicKeyName(),
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
                    createClientConfigExecResult.getStdout()));
            throw new InternalServerException("failed to create client configuration in system");
        }

        // add client to network in specified network config
        logger.info(String.format("adding peer '%s' to network config '%s'", client.getClientName(), existingNetwork.getNetworkName()));
        // the subnet that is added to network config needs to explicitly end in 32
        String networkConfigFormatClientCidr = String.format("%s/32", client.getClientCidr().split("/")[0]);
        final String addPeerToNetworkCommand = String.join(" ",
                "./add-peer-to-network",
                "--config-name", existingNetwork.getNetworkName(),
                "--client-cidr", networkConfigFormatClientCidr,
                "--client-public-key-name", client.getClientPublicKeyName()
        );
        ExecutionResult addPeerToNetExecResult = Executor.runCommand(addPeerToNetworkCommand);
        if (addPeerToNetExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    addPeerToNetworkCommand,
                    addPeerToNetExecResult.getExitCode(),
                    addPeerToNetExecResult.getStdout()));
            throw new InternalServerException("failed to add client to network config");
        }

        Client savedClient = this.clientRepository.save(client);
        NetworkClient networkClient = new NetworkClient();
        networkClient.setClient(savedClient);
        networkClient.setNetwork(existingNetwork);
        return this.netClientRepository.save(networkClient);
    }

    public List<Client> listNetworkClients(ListNetworkClientsRequest request) {
        checkNetworkExists(request.getNetworkName());
        // TODO pagination
        return this.netClientRepository.findClientsByNetworkName(request.getNetworkName());
    }

    //public NetworkClient removeClientFromNetwork() {}
}
