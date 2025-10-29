package com.brcsrc.yaws.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.model.requests.ListNetworksRequest;
import com.brcsrc.yaws.model.requests.ListNetworksResponse;
import com.brcsrc.yaws.model.requests.UpdateNetworkRequest;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.shell.Executor;
import com.brcsrc.yaws.utility.FilepathUtils;
import com.brcsrc.yaws.utility.IPUtils;

@Service
public class NetworkService {

    private final NetworkRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);

    public NetworkService(NetworkRepository repository) {
        this.repository = repository;
    }

    public List<Network> getAllNetworks() {
        return this.repository.findAll();
    }

    public ListNetworksResponse listNetworks(ListNetworksRequest request) {
        // see https://www.baeldung.com/spring-data-jpa-pagination-sorting for pagination approach
        // Set page size from request (defaults to 10 in the request class)
        int pageSize = request.getMaxItems();
        int pageNumber = request.getPage() != null ? request.getPage() : 0;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Network> networkPage = this.repository.findAll(pageable);
        
        // Calculate next page number
        Integer nextPage = null;
        if (networkPage.hasNext()) {
            nextPage = pageNumber + 1;
        }
        
        return new ListNetworksResponse(networkPage.getContent(), nextPage);
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
        if (!network.getNetworkName().matches(Constants.CHAR_64_ALPHANUMERIC_DASHES_UNDERSC_REGEXP)) {
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

        // determine absolute paths for different files
        final String NETWORK_DIR_PATH = FilepathUtils.getNetworkDirectoryPath(network.getNetworkName());
        final String NETWORK_KEYS_PATH = FilepathUtils.getNetworkKeysDirectoryPath(network.getNetworkName());
        final String NETWORK_CLIENTS_PATH = FilepathUtils.getNetworkClientsDirectoryPath(network.getNetworkName());
        final String NETWORK_CONFIG_PATH = FilepathUtils.getNetworkConfigPath(network.getNetworkName());
        final String NETWORK_PRIV_KEY_PATH = FilepathUtils.getNetworkKeyPath(network.getNetworkName(), network.getNetworkPrivateKeyName());
        final String NETWORK_PUB_KEY_PATH = FilepathUtils.getNetworkKeyPath(network.getNetworkName(), network.getNetworkPublicKeyName());

        // create the network specific directories to hold these files
        var directoriesToMake = List.of(
                NETWORK_DIR_PATH,
                NETWORK_KEYS_PATH,
                NETWORK_CLIENTS_PATH
        );
        for (String directory : directoriesToMake) {
            try {
                Path path = Paths.get(directory);
                Files.createDirectories(path);
            } catch (InvalidPathException | IOException e) {
                String errMsg = String.format("error in creating directory for network: %s", e.fillInStackTrace());
                logger.error(errMsg);
                throw new InternalServerException("failed to create network");
            }
        }

        // create the wireguard key pair
        logger.info(String.format("creating key pair: '%s', '%s'", NETWORK_PRIV_KEY_PATH, NETWORK_PUB_KEY_PATH));
        final String createKeyPairCommand = String.join(" ",
                "./create-key-pair ",
                "--private-key-name", NETWORK_PRIV_KEY_PATH,
                "--public-key-name", NETWORK_PUB_KEY_PATH
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
                "--config-name", NETWORK_CONFIG_PATH,
                "--network-cidr", network.getNetworkCidr(),
                "--network-listen-port", String.valueOf(network.getNetworkListenPort()),
                "--network-private-key-name", NETWORK_PRIV_KEY_PATH
        );
        ExecutionResult createNetConfigExecResult = Executor.runCommand(createNetworkConfigCommand);
        if (createNetConfigExecResult.getExitCode() != 0) {
            logger.error(String.format(
                    "command: '%s' exited %s with reason: %s",
                    createNetworkConfigCommand,
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
                    configureIptablesExecResult.getStderr()));
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
            final String NETWORK_DIR_PATH = FilepathUtils.getNetworkDirectoryPath(network.getNetworkName());
            logger.info(String.format("deleting network directory '%s'", NETWORK_DIR_PATH));
            Path networkDirPath = Paths.get(NETWORK_DIR_PATH);
            if (Files.exists(networkDirPath)) {
                try {
                    FilepathUtils.deleteDirectory(networkDirPath);
                } catch (IOException e) {
                    errorsOnRemoval = true;
                    logger.error(String.format("error deleting directory '%s': %s", NETWORK_DIR_PATH, e.fillInStackTrace()));
                }
            } else {
                logger.info(String.format("directory '%s' does not exist", NETWORK_DIR_PATH));
            }
            final String NETWORK_CONFIG_PATH = FilepathUtils.getNetworkConfigPath(network.getNetworkName());
            logger.info(String.format("deleting network config '%s'", NETWORK_CONFIG_PATH));
            File networkConfigFile = new File(NETWORK_CONFIG_PATH);
            if (networkConfigFile.exists()) {
                if (!networkConfigFile.delete()) {
                    errorsOnRemoval = true;
                    logger.error(String.format("error deleting file '%s'", NETWORK_CONFIG_PATH));
                }
            } else {
                logger.info(String.format("'%s' does not exist", NETWORK_CONFIG_PATH));
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

    public Network updateNetwork(String networkName, UpdateNetworkRequest updateNetworkRequest) {
        logger.info("Starting updateNetwork for networkName: {}", networkName);
        logger.debug("UpdateNetworkRequest received: {}", updateNetworkRequest);

        // Validate the UpdateNetworkRequest
        validateUpdateRequest(updateNetworkRequest);

        // Retrieve the existing network from DB or throw an exception if it doesn't exist
        // TODO: Maybe include system level check through wg show <networkName> ?
        Optional<Network> existingNetwork = this.repository.findByNetworkName(networkName);
        if (existingNetwork.isEmpty()) {
            String errMsg = String.format("Network '%s' does not exist", networkName);
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        Network network = existingNetwork.get();
        logger.debug("Existing network retrieved: {}", network);

        // Update the networkTag if provided
        if (updateNetworkRequest.getNetworkTag() != null) {
            updateNetworkTag(network, updateNetworkRequest.getNetworkTag());
        }

        // Update the networkStatus if provided
        if (updateNetworkRequest.getNetworkStatus() != null) {
            updateNetworkStatus(network, updateNetworkRequest.getNetworkStatus());
        }

        // Save the updated network using a helper method
        return saveUpdatedNetwork(networkName, network);
    }

    // Helper method to validate the UpdateNetworkRequest
    private void validateUpdateRequest(UpdateNetworkRequest updateNetworkRequest) {
        if (updateNetworkRequest == null) {
            String errMsg = "UpdateNetworkRequest cannot be null";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        if (updateNetworkRequest.getNetworkTag() == null && updateNetworkRequest.getNetworkStatus() == null) {
            String errMsg = "At least one field (networkTag or networkStatus) must be provided for update";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
    }

    // Helper method to update the networkTag
    private void updateNetworkTag(Network network, String networkTag) {
        logger.debug("Updating networkTag for network '{}': {}", network.getNetworkName(), networkTag);

        // Trim leading and trailing spaces
        networkTag = networkTag.trim();

        // Define MAX_TAG_LENGTH scoped to this method
        final int MAX_TAG_LENGTH = 64;

        // Regular expression to allow only alphanumeric characters, dashes, and underscores
        final String VALID_TAG_REGEX = "^[a-zA-Z0-9_-]+$";

        if (networkTag.isEmpty() || networkTag.length() > MAX_TAG_LENGTH || !networkTag.matches(VALID_TAG_REGEX)) {
            String errMsg = String.format(
                    "Invalid networkTag: must be non-empty, no more than %d characters, and contain only alphanumeric characters, dashes, or underscores",
                    MAX_TAG_LENGTH
            );
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        network.setNetworkTag(networkTag);
    }

    // Helper method to update the networkStatus
    private void updateNetworkStatus(Network network, NetworkStatus newStatus) {
        logger.debug("Updating networkStatus for network '{}': {}", network.getNetworkName(), newStatus);

        if (!EnumSet.of(NetworkStatus.ACTIVE, NetworkStatus.INACTIVE).contains(newStatus)) {
            String errMsg = String.format("Invalid networkStatus: '%s' is not a valid status", newStatus);
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // Handle status change
        if (newStatus == NetworkStatus.INACTIVE) {
            deactivateNetwork(network); // NEW: Call helper method to deactivate the network
        } else if (newStatus == NetworkStatus.ACTIVE) {
            activateNetwork(network); // NEW: Call helper method to activate the network
        }

        network.setNetworkStatus(newStatus);
    }

    // Helper method to save the updated network
    private Network saveUpdatedNetwork(String networkName, Network network) {
        try {
            Network updatedNetwork = repository.save(network);
            logger.info("Successfully updated network '{}': {}", networkName, updatedNetwork);
            return updatedNetwork;
        } catch (Exception e) {
            String errMsg = String.format("Failed to save updated network '%s': %s", networkName, e.getMessage());
            logger.error(errMsg, e);
            throw new InternalServerException("Failed to update network");
        }
    }

    // Helper method to deactivate a network
    private void deactivateNetwork(Network network) {
        // Check if Network is already INACTIVE
        if (network.getNetworkStatus() == NetworkStatus.INACTIVE) {
            logger.info("Network '{}' is already inactive. Skipping wg-quick down.", network.getNetworkName());
            String errMsg = String.format("Network %s is already in networkStatus of INACTIVE.", network.getNetworkName());
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        logger.info("Deactivating network '{}'", network.getNetworkName());

        // Run wg-quick down <NetworkName>
        final String wgDownCommand = String.format("wg-quick down %s", network.getNetworkName());
        ExecutionResult wgDownResult = Executor.runCommand(wgDownCommand);
        if (wgDownResult.getExitCode() != 0) {
            String errMsg = String.format("Failed to bring down WireGuard interface for network '%s': %s",
                    network.getNetworkName(), wgDownResult.getStdout());
            logger.error(errMsg);
            throw new InternalServerException(errMsg);
        }

        logger.info("Successfully deactivated network '{}'", network.getNetworkName());
    }

    // Helper method to activate a network
    private void activateNetwork(Network network) {
        // Check if Network is already ACTIVE
        if (network.getNetworkStatus() == NetworkStatus.ACTIVE) {
            logger.info("Network '{}' is already active. Skipping wg-quick up.", network.getNetworkName());
            String errMsg = String.format("Network %s is already in networkStatus of ACTIVE.", network.getNetworkName());
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        logger.info("Activating network '{}'", network.getNetworkName());

        // Run wg-quick up <NetworkName>
        final String wgUpCommand = String.format("wg-quick up %s", network.getNetworkName());
        ExecutionResult wgUpResult = Executor.runCommand(wgUpCommand);
        if (wgUpResult.getExitCode() != 0) {
            String errMsg = String.format("Failed to bring up WireGuard interface for network '%s': %s",
                    network.getNetworkName(), wgUpResult.getStdout());
            logger.error(errMsg);
            throw new InternalServerException(errMsg);
        }

        logger.info("Successfully activated network '{}'", network.getNetworkName());
    }
}
