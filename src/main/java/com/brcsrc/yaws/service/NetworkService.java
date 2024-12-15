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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    public Network describeNetwork(String networkName) {
        Optional<Network> existingNetwork = this.repository.findByNetworkName(networkName);
        if (existingNetwork.isEmpty()) {
            throw new BadRequestException(String.format("network '%s' does not exist", networkName));
        }
        return existingNetwork.get();
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
            // cleanup network on separate thread
            CompletableFuture<Network> deletedNetworkFuture = asyncRemoveNetworkFromSystem(network);
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
            CompletableFuture<Network> deletedNetworkFuture = asyncRemoveNetworkFromSystem(network);
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
            CompletableFuture<Network> deletedNetworkFuture = asyncRemoveNetworkFromSystem(network);
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
            CompletableFuture<Network> deletedNetworkFuture = asyncRemoveNetworkFromSystem(network);
            throw new InternalServerException("failed to create network");
        }

        network.setNetworkStatus(NetworkStatus.ACTIVE);
        savedNetwork = this.repository.save(network);
        return savedNetwork;
    }

    @Async
    CompletableFuture<Network> asyncRemoveNetworkFromSystem(Network network) {
        boolean errorsOnRemoval = false;
        try {
            logger.info("asyncRemoveNetworkFromSystem called on thread: " + Thread.currentThread().getName());

            //TODO delete all client configs from clients and network_clients tables

            logger.info("bringing down the wireguard interface");
            final String wgDownCommand = String.format("wg-quick down %s", network.getNetworkName());
            ExecutionResult wgDownExecResult = Executor.runCommand(wgDownCommand);
            // if this fails it is not necessarily a problem but should be attempted
            if (wgDownExecResult.getExitCode() != 0) {
                errorsOnRemoval = true;
                logger.error(String.format(
                        "command: '%s' exited %s with reason: %s",
                        wgDownCommand,
                        wgDownExecResult.getExitCode(),
                        wgDownExecResult.getStdout()));
            }

            // add rules to iptables to allow traffic to network
            logger.info("removing iptable rules for network");
            final String configureIptablesCommand = String.join(" ",
                    "./configure-iptables",
                    "--operation", "remove-network",
                    "--network-cidr", network.getNetworkCidr()
            );
            ExecutionResult configureIptablesExecResult = Executor.runCommand(configureIptablesCommand);
            if (configureIptablesExecResult.getExitCode() != 0) {
                errorsOnRemoval = true;
                logger.error(String.format(
                        "command: '%s' exited %s with reason: %s",
                        configureIptablesCommand,
                        configureIptablesExecResult.getExitCode(),
                        configureIptablesExecResult.getStdout()));
            }

            // remove the files. not needed for wireguard but keeps disk space down
            logger.info("removing wireguard config for network");
            final String absPathConfig = String.format("/etc/wireguard/%s.conf", network.getNetworkName());
            File config = new File(absPathConfig);
            if (!config.delete()) {
                errorsOnRemoval = true;
                logger.error(String.format("failed to delete %s", absPathConfig));
            }
            logger.info("removing key pair");
            final String absPathPrviKey = String.format("/etc/wireguard/%s", network.getNetworkPrivateKeyName());
            File privateKey = new File(absPathPrviKey);
            if (!privateKey.delete()) {
                errorsOnRemoval = true;
                logger.error(String.format("failed to delete %s", absPathPrviKey));
            }
            final String absPathPublicKey = String.format("/etc/wireguard/%s", network.getNetworkPublicKeyName());
            File publicKey = new File(absPathPublicKey);
            if (!publicKey.delete()) {
                errorsOnRemoval = true;
                logger.error(String.format("failed to delete %s", absPathPublicKey));
            }

        } catch (Exception e) {
            logger.error("error in cleaning up network, removing thread");
            Thread.currentThread().interrupt();
        }

        if (!errorsOnRemoval) {
            network.setNetworkStatus(NetworkStatus.DELETED);
            this.repository.delete(network);
        }

        return CompletableFuture.completedFuture(network);
    }

    public Network deleteNetwork(String networkName) {
        Optional<Network> existingNetwork = this.repository.findByNetworkName(networkName);
        if (existingNetwork.isEmpty()) {
            throw new BadRequestException(String.format("network '%s' does not exist", networkName));
        }
        Network network = existingNetwork.get();

        CompletableFuture<Network> deletedNetworkFuture = asyncRemoveNetworkFromSystem(network);
        try {
            Network deletedNetwork = deletedNetworkFuture.get();
            network.setNetworkStatus(NetworkStatus.DELETED);
            this.repository.delete(network);
            return network;
        } catch (InterruptedException | ExecutionException e) {
            logger.error(String.format("error in asyncRemoveNetworkFromSystem for network '%s': %s", networkName, e));
            throw new InternalServerException("error in deleting network");
        }
    }
}

