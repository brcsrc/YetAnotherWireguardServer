package com.brcsrc.yaws.service;

import com.brcsrc.yaws.exceptions.BadRequestException;
import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.shell.Executor;
import com.brcsrc.yaws.utility.IPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
            String errMsg = String.format("network '%s' does not exist", networkName);
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        return existingNetwork.get();
    }

    public Network createNetwork(Network network) {
        if (network.getNetworkName().length() > 64 || !network.getNetworkName().matches(Constants.CHAR_64_ALPHANUMERIC_REGEXP)) {
            String errMsg = "networkName must be alphanumeric without spaces and no more than 64 characters";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // TODO check against 0.0.0.0
        if (!IPUtils.isValidIpv4Cidr(network.getNetworkCidr())) {
            String errMsg = "networkCidr is invalid";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        String[] cidr = network.getNetworkCidr().split("/");

        int subnetMask = Integer.parseInt(cidr[1]);
        if (subnetMask < 24) {
            // TODO support larger networks
            String errMsg = "subnet mask must not be less than 24";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        int networkInterfaceAddress = Integer.parseInt(cidr[0].split("\\.")[3]);
        if (networkInterfaceAddress == 0 || networkInterfaceAddress > 254) {
            String errMsg = "network interface address must be between .1 and .254";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        boolean resourcesInUse = this.repository.existsByNetworkNameOrNetworkCidrOrListenPort(
                network.getNetworkName(),
                network.getNetworkCidr(),
                network.getNetworkListenPort());
        if (resourcesInUse) {
            String errMsg = "network already exists by requested networkName or networkCidr or networkListenPort";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
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

        logger.info(String.format(
                "creating wireguard network config: CIDR = %s, listen port = %s",
                network.getNetworkCidr(),
                network.getNetworkListenPort()));
        final String createNetworkConfigCommand = String.join(" ",
                "./create-network-config",
                "--config-name", network.getNetworkName(),
                "--network-cidr", network.getNetworkCidr(),
                "--network-listen-port", String.valueOf(network.getNetworkListenPort()),
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
        ExecutionResult wgUpExecResult = Executor.runCommand(wgUpCommand);
        if (wgUpExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    wgUpCommand,
                    wgUpExecResult.getExitCode(),
                    wgUpExecResult.getStdout()));
            network.setNetworkStatus(NetworkStatus.INACTIVE);
            this.repository.save(network);
            CompletableFuture<Network> deletedNetworkFuture = asyncRemoveNetworkFromSystem(network);
            throw new InternalServerException("failed to create network");
        }

        network.setNetworkStatus(NetworkStatus.ACTIVE);
        savedNetwork = this.repository.save(network);
        logger.info("CreateNetwork operation complete");
        return savedNetwork;
    }

    @Async
    CompletableFuture<Network> asyncRemoveNetworkFromSystem(Network network) {
        boolean errorsOnRemoval = false;
        try {
            logger.info("asyncRemoveNetworkFromSystem called on thread: " + Thread.currentThread().getName());
            logger.info(String.format("bringing down the wireguard interface '%s'", network.getNetworkName()));

            final String checkWgIFaceExistsCmd = String.format("wg show %s", network.getNetworkName());
            ExecutionResult checkWgIFaceExistsCmdResult = Executor.runCommand(checkWgIFaceExistsCmd);
            boolean wgIFaceExists = (checkWgIFaceExistsCmdResult.getExitCode() == 0);

            if (wgIFaceExists) {
                final String wgDownCommand = String.format("wg-quick down %s", network.getNetworkName());
                ExecutionResult wgDownExecResult = Executor.runCommand(wgDownCommand);
                if (wgDownExecResult.getExitCode() != 0) {
                    errorsOnRemoval = true;
                    logger.error(String.format(
                            "command: '%s' exited %s with reason: %s",
                            wgDownCommand,
                            wgDownExecResult.getExitCode(),
                            wgDownExecResult.getStdout()));
                }
            } else {
                logger.info(String.format("wireguard interface '%s' does not exist", network.getNetworkName()));
            }

            // add rules to iptables to allow traffic to network
            logger.info(String.format("removing iptable rules for network %s", network.getNetworkName()));
            final String configureIptablesCommand = String.join(" ",
                    "./configure-iptables",
                    "--operation", "remove-network",
                    "--network-cidr", network.getNetworkCidr()
            );
            ExecutionResult configureIptablesExecResult = Executor.runCommand(configureIptablesCommand);
            if (configureIptablesExecResult.getExitCode() != 0) {
                //errorsOnRemoval = true; // TODO this also exits non 0 if the chain does not exist which should not be an error for this operation
                logger.error(String.format(
                        "command: '%s' exited %s with reason: %s",
                        configureIptablesCommand,
                        configureIptablesExecResult.getExitCode(),
                        configureIptablesExecResult.getStdout()));
            }

            // remove the files. not needed for wireguard but keeps disk space down
            logger.info(String.format("removing wireguard config for network %s", network.getNetworkName()));
            final String absPathConfig = String.format("/etc/wireguard/%s.conf", network.getNetworkName());
            File config = new File(absPathConfig);
            if (config.exists()) {
                if (!config.delete()) {
                    errorsOnRemoval = true;
                    logger.error(String.format("failed to delete %s", absPathConfig));
                }
            } else {
                logger.info(String.format("/etc/wireguard/%s.conf does not exist", network.getNetworkName()));
            }

            logger.info(String.format(
                    "removing key pair '%s' '%s'",
                    network.getNetworkPrivateKeyName(),
                    network.getNetworkPublicKeyName()
            ));
            final String absPathPrviKey = String.format("/etc/wireguard/%s", network.getNetworkPrivateKeyName());
            File privateKey = new File(absPathPrviKey);
            if (privateKey.exists()) {
                if (!privateKey.delete()) {
                    errorsOnRemoval = true;
                    logger.error(String.format("failed to delete %s", absPathPrviKey));
                }
            } else {
                logger.info(String.format("/etc/wireguard/%s does not exist", network.getNetworkPrivateKeyName()));
            }

            final String absPathPublicKey = String.format("/etc/wireguard/%s", network.getNetworkPublicKeyName());
            File publicKey = new File(absPathPublicKey);
            if (publicKey.exists()) {
                if (!publicKey.delete()) {
                    errorsOnRemoval = true;
                    logger.error(String.format("failed to delete %s", absPathPublicKey));
                }
            } else {
                logger.info(String.format("/etc/wireguard/%s does not exist", network.getNetworkPublicKeyName()));
            }


        } catch (Exception e) {
            logger.error("error in cleaning up network, removing thread");
            Thread.currentThread().interrupt();
        }

        if (!errorsOnRemoval) {
            logger.info(String.format(
                    "asyncRemoveNetworkFromSystem completed successfully for network %s",
                    network.getNetworkName())
            );
            this.repository.delete(network);
        }

        return CompletableFuture.completedFuture(network);
    }

    public Network deleteNetwork(String networkName) {
        // TODO input validation is missing
        // TODO check requested network does not have clients
        Optional<Network> existingNetwork = this.repository.findByNetworkName(networkName);
        if (existingNetwork.isEmpty()) {
            String errMsg = String.format("network '%s' does not exist", networkName);
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        Network network = existingNetwork.get();

        CompletableFuture<Network> deletedNetworkFuture = asyncRemoveNetworkFromSystem(network);
        try {
            Network deletedNetwork = deletedNetworkFuture.get();
            // if ExeceptionExecution is not thrown then the async job was successful
            this.repository.delete(network); // TODO maybe we dont need here also
            network.setNetworkStatus(NetworkStatus.DELETED);
            return network;
        } catch (InterruptedException | ExecutionException e) {
            logger.error(String.format("error in asyncRemoveNetworkFromSystem for network '%s': %s", networkName, e));
            throw new InternalServerException("error in deleting network");
        }
    }
}

