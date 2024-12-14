package com.brcsrc.yaws.service;

import com.brcsrc.yaws.exceptions.BadRequestException;
import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.shell.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NetworkService {

    private final NetworkRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);

    @Autowired
    public NetworkService(NetworkRepository repository) {
        this.repository = repository;
    }

    public List<Network> getAllNetworks() {
        return this.repository.findAll();
    }

    public Network createNetwork(Network network) {
        if (network.getNetworkName().length() > 64 || !network.getNetworkName().matches("^[a-zA-Z0-9]+$")) {
            String errMsg = "networkName must be alphanumeric without spaces and no more than 64 characters";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        String ipv4Cidr = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(3[0-2]|[1-2][0-9]|[0-9]))$";
        if (!network.getNetworkCidr().matches(ipv4Cidr)) {
            String errMsg = "networkCidr is invalid";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        int subnetMask = Integer.parseInt(network.getNetworkCidr().split("/")[1]);
        if (subnetMask < 24) {
            // TODO support larger networks
            String errMsg = "subnet mask must not be less than 24";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        int requestedPort;
        try {
            requestedPort = Integer.parseInt(network.getNetworkListenPort());
        } catch (NumberFormatException e) {
            String errMsg = "networkListenPort must be an integer";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }
        if (requestedPort < 1 || requestedPort > 65535) {
            String errMsg = "networkPort must be greater than or equal to 1 or less than or equal to 65535";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        boolean resourcesInUse = this.repository.existsByNetworkNameOrNetworkCidrOrListenPort(
                network.getNetworkName(),
                network.getNetworkCidr(),
                network.getNetworkListenPort());
        if (resourcesInUse) {
            String errMsg = "network already exists by requested networkName or networkCidr or networkListenPort";
            logger.error(errMsg);
            throw new BadRequestException(errMsg);
        }

        logger.info(String.format("creating network '%s'", network.getNetworkName()));
        network.setNetworkPrivateKeyName(network.getNetworkName() + "-private-key");
        network.setNetworkPublicKeyName(network.getNetworkName() + "-public-key");
        network.setNetworkStatus(NetworkStatus.CREATED);

        // save the network now with CREATED status
        Network savedNetwork = this.repository.save(network);

        // create the wireguard key pair
        logger.info(String.format("creating key pair: '%s', '%s'", network.getNetworkPrivateKeyName(), network.getNetworkPublicKeyName()));
        final String createKeyPairCommand = String.join(" ",
                "./create-key-pair ",
                "--private-key-name", network.getNetworkPrivateKeyName(),
                "--public-key-name", network.getNetworkPublicKeyName()
        );
        ExecutionResult createKeyPairExecResult = Executor.runCommand(createKeyPairCommand);
        if (createKeyPairExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    createKeyPairCommand,
                    createKeyPairExecResult.getExitCode(),
                    createKeyPairExecResult.getStdout()));
            // mark the network for removal
            network.setNetworkStatus(NetworkStatus.INACTIVE);
            this.repository.save(network);
            throw new InternalServerException("failed to create network");
        }

        // create the wireguard network config
        logger.info(String.format(
                "creating wireguard network config: CIDR = %s, listen port = %s",
                network.getNetworkCidr(),
                network.getNetworkListenPort()));
        final String createNetworkConfigCommand = String.join(" ",
                "./create-network-config",
                "--config-name", network.getNetworkName(),
                "--network-cidr", network.getNetworkCidr(),
                "--network-listen-port", network.getNetworkListenPort(),
                "--network-private-key-name", network.getNetworkPrivateKeyName()
        );
        ExecutionResult createNetConfigExecResult = Executor.runCommand(createNetworkConfigCommand);
        if (createNetConfigExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    createNetConfigExecResult,
                    createNetConfigExecResult.getExitCode(),
                    createNetConfigExecResult.getStdout()));
            network.setNetworkStatus(NetworkStatus.INACTIVE);
            this.repository.save(network);
            throw new InternalServerException("failed to create network");
        }

        // add rules to iptables to allow traffic to network
        logger.info("creating iptable rules for network");
        final String configureIptablesCommand = String.join(" ",
                "./configure-iptables",
                "--operation", "add-network",
                "--network-cidr", network.getNetworkCidr()
        );
        ExecutionResult configureIptablesExecResult = Executor.runCommand(configureIptablesCommand);
        if (configureIptablesExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    configureIptablesCommand,
                    configureIptablesExecResult.getExitCode(),
                    configureIptablesExecResult.getStdout()));
            network.setNetworkStatus(NetworkStatus.INACTIVE);
            this.repository.save(network);
            throw new InternalServerException("failed to create network");
        }

        // since this network is newly created we need bring it up in wireguard
        logger.info("bringing up the wireguard interface");
        final String wgUpCommand = String.format("wg-quick up %s", network.getNetworkName());
        ExecutionResult wgUpExecResul = Executor.runCommand(wgUpCommand);
        if (wgUpExecResul.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    wgUpCommand,
                    wgUpExecResul.getExitCode(),
                    wgUpExecResul.getStdout()));
            network.setNetworkStatus(NetworkStatus.INACTIVE);
            this.repository.save(network);
            throw new InternalServerException("failed to create network");
        }

        network.setNetworkStatus(NetworkStatus.ACTIVE);
        savedNetwork = this.repository.save(network);
        return savedNetwork;
    }
}

