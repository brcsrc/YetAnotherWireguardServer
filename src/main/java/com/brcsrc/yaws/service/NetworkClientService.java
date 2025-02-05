package com.brcsrc.yaws.service;

import com.brcsrc.yaws.exceptions.BadRequestException;
import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.persistence.ClientRepository;
import com.brcsrc.yaws.persistence.NetworkClientRepository;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.shell.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public NetworkClient addClientToNetwork(CreateNetworkClientRequest request) {
        Optional<Network> network = this.networkRepository.findByNetworkName(request.getNetworkName());
        if (network.isEmpty()) {
            throw new BadRequestException(String.format("network '%s' does not exist", request.getNetworkName()));
        }
        Network existingNetwork = network.get();
        logger.info(String.format("found existing network '%s'", existingNetwork.getNetworkName()));

        //todo check that client cidr is in range and is available

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
        final String addPeerToNetworkCommand = String.join(" ",
                "./add-peer-to-network",
                "--config-name", existingNetwork.getNetworkName(),
                "--client-cidr", client.getClientCidr(),
                "--client-public-key-name", client.getNetworkPublicKeyName()
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

    //public NetworkClient removeClientFromNetwork() {}
}
