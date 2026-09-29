package com.brcsrc.yaws.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.wireguard.ClientConfig;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import com.brcsrc.yaws.model.wireguard.NetworkPeer;
import com.brcsrc.yaws.shell.CommandExecutor;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.utility.FilepathUtils;
import com.brcsrc.yaws.utility.WireguardConfigReaderUtils;
import com.brcsrc.yaws.utility.WireguardConfigWriterUtils;

/**
 * owns interaction with wireguard on the system. config file content is produced in java by
 * WireguardConfigWriterUtils and the commands here only manipulate interface state, they do
 * not build files.
 */
@Service
public class WireguardService {

    private final CommandExecutor commandExecutor;
    private static final Logger logger = LoggerFactory.getLogger(WireguardService.class);

    public WireguardService(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * generates a wireguard key pair, writing the private key 0600 and returning the public key
     * value. replaces the create-key-pair script.
     */
    public String createKeyPair(String privateKeyPath, String publicKeyPath) {
        ExecutionResult genKeyResult = this.commandExecutor.runCommand(List.of("wg", "genkey"));
        if (genKeyResult.getExitCode() != 0) {
            logger.error("failed to generate wireguard private key: {}", genKeyResult.getStderr());
            throw new InternalServerException("failed to create key pair");
        }
        String privateKey = genKeyResult.getStdout().trim();

        String publicKey = derivePublicKey(privateKey);

        try {
            Path privateKeyFilePath = Paths.get(privateKeyPath);
            Path parentDir = privateKeyFilePath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // the private key file must never be group or world readable
            Files.writeString(privateKeyFilePath, privateKey + "\n");
            Files.setPosixFilePermissions(
                    privateKeyFilePath,
                    PosixFilePermissions.fromString("rw-------"));
            Files.writeString(Paths.get(publicKeyPath), publicKey + "\n");
        } catch (IOException e) {
            logger.error("failed to write key pair files: {}", e.getMessage());
            throw new InternalServerException("failed to create key pair");
        }

        return publicKey;
    }

    /**
     * derives the public key for a private key. wg pubkey reads the private key on stdin, so it
     * is fed directly to the process rather than going through a shell redirect.
     */
    private String derivePublicKey(String privateKey) {
        ExecutionResult pubKeyResult = this.commandExecutor.runCommandWithInput(
                List.of("wg", "pubkey"),
                privateKey + "\n");
        if (pubKeyResult.getExitCode() != 0) {
            logger.error("failed to derive wireguard public key: {}", pubKeyResult.getStderr());
            throw new InternalServerException("failed to create key pair");
        }
        return pubKeyResult.getStdout().trim();
    }

    /**
     * writes a network config, always including the interface hook lines. every write goes
     * through here so a peer add or remove cannot silently drop the hooks from the file.
     *
     * the network CIDR the hooks need is taken from the config's own interface address, so the
     * rules can never disagree with the interface they are written alongside.
     */
    public void writeNetworkConfig(String networkName, NetworkConfig networkConfig) {
        String hookLines = WireguardConfigWriterUtils.buildNetworkHookLines(
                networkName,
                networkConfig.getNetworkInterface().getAddress());
        WireguardConfigWriterUtils.writeNetworkConfig(
                FilepathUtils.getNetworkConfigPath(networkName),
                networkConfig,
                hookLines);
    }

    public void writeClientConfig(String networkName, String clientName, ClientConfig clientConfig) {
        WireguardConfigWriterUtils.writeClientConfig(
                FilepathUtils.getClientConfigPath(networkName, clientName),
                clientConfig);
    }

    /**
     * adds a peer to the network config and applies it to the running interface. replaces the
     * add-peer-to-network script.
     *
     * the peer is applied with wg syncconf, which updates the live interface in place. the old
     * script followed syncconf with a wg-quick down/up cycle, which dropped every client on the
     * network on every client add. syncconf alone is sufficient and non disruptive.
     */
    public void addPeerToNetwork(String networkName, NetworkPeer peer) {
        String configFileName = String.format("%s.conf", networkName);
        NetworkConfig networkConfig = WireguardConfigReaderUtils.readNetworkConfig(configFileName);
        networkConfig.addPeer(peer);
        writeNetworkConfig(networkName, networkConfig);
        syncConfig(networkName);
    }

    /**
     * removes the peer holding the given /32 from the network config and applies the change to
     * the running interface. replaces the remove-peer-from-network script, which deleted lines
     * with sed against an escaped pattern.
     */
    public void removePeerFromNetwork(String networkName, String peerAllowedIps) {
        String configFileName = String.format("%s.conf", networkName);
        NetworkConfig networkConfig = WireguardConfigReaderUtils.readNetworkConfig(configFileName);
        boolean removed = networkConfig.removePeerByAllowedIps(peerAllowedIps);
        if (!removed) {
            logger.warn("no peer with allowedIps '{}' found in config for network '{}'",
                    peerAllowedIps, networkName);
        }
        writeNetworkConfig(networkName, networkConfig);
        syncConfig(networkName);
    }

    /**
     * applies the on disk config to the running interface without tearing it down. a no op when
     * the interface is not up, since the config will be applied when it is next brought up.
     */
    public void syncConfig(String networkName) {
        if (!interfaceExists(networkName)) {
            logger.info("interface '{}' is not up, skipping syncconf", networkName);
            return;
        }
        String configPath = FilepathUtils.getNetworkConfigPath(networkName);

        // wg-quick strip removes the keys wg itself does not understand, including Address and
        // the PostUp/PostDown hooks, leaving only what syncconf accepts
        ExecutionResult stripResult = this.commandExecutor.runCommand(
                List.of("wg-quick", "strip", networkName));
        if (stripResult.getExitCode() != 0) {
            logger.error("failed to strip config '{}': {}", configPath, stripResult.getStderr());
            throw new InternalServerException("failed to apply network configuration");
        }

        // syncconf takes the stripped config on stdin. the old script used bash process
        // substitution for this, which is why it had to run as a shell script
        ExecutionResult syncResult = this.commandExecutor.runCommandWithInput(
                List.of("wg", "syncconf", networkName, "/dev/stdin"),
                stripResult.getStdout());
        if (syncResult.getExitCode() != 0) {
            logger.error("failed to sync config for network '{}': {}",
                    networkName, syncResult.getStderr());
            throw new InternalServerException("failed to apply network configuration");
        }
    }

    public boolean interfaceExists(String networkName) {
        ExecutionResult result = this.commandExecutor.runCommand(List.of("wg", "show", networkName));
        return result.getExitCode() == 0;
    }

    public void interfaceUp(String networkName) {
        ExecutionResult result = this.commandExecutor.runCommand(
                List.of("wg-quick", "up", networkName));
        if (result.getExitCode() != 0) {
            logger.error("failed to bring up interface '{}': {}", networkName, result.getStderr());
            throw new InternalServerException(String.format(
                    "Failed to bring up WireGuard interface for network '%s': %s",
                    networkName, result.getStderr()));
        }
    }

    public void interfaceDown(String networkName) {
        ExecutionResult result = this.commandExecutor.runCommand(
                List.of("wg-quick", "down", networkName));
        if (result.getExitCode() != 0) {
            logger.error("failed to bring down interface '{}': {}", networkName, result.getStderr());
            throw new InternalServerException(String.format(
                    "Failed to bring down WireGuard interface for network '%s': %s",
                    networkName, result.getStderr()));
        }
    }
}
