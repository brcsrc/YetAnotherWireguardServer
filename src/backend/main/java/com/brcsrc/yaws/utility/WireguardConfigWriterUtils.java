package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.exceptions.WireguardConfigFileWriteException;
import com.brcsrc.yaws.model.wireguard.ClientConfig;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import com.brcsrc.yaws.model.wireguard.NetworkPeer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * writes wireguard config files. this replaces the shell scripts that previously built these
 * files by appending heredocs and deleting lines with sed. config content is rendered from the
 * model types so a peer removal is a rewrite of a parsed document rather than a line match.
 *
 * config files hold private keys, so every file written here is created 0600 rather than
 * inheriting the process umask.
 */
public class WireguardConfigWriterUtils {

    private static final Set<PosixFilePermission> OWNER_READ_WRITE =
            PosixFilePermissions.fromString("rw-------");

    /**
     * renders a network (server side) config. PostUp/PostDown lines are not written here,
     * see {@link #renderNetworkConfig(NetworkConfig, String)}.
     */
    public static String renderNetworkConfig(NetworkConfig networkConfig) {
        return renderNetworkConfig(networkConfig, null);
    }

    /**
     * renders a network (server side) config, optionally including hook lines placed in the
     * [Interface] section. hookLines is written verbatim and must already be newline separated
     * PostUp/PostDown entries.
     */
    public static String renderNetworkConfig(NetworkConfig networkConfig, String hookLines) {
        StringBuilder config = new StringBuilder();
        config.append("[Interface]\n");
        config.append(String.format("Address = %s%n", networkConfig.getNetworkInterface().getAddress()));
        config.append(String.format("ListenPort = %s%n", networkConfig.getNetworkInterface().getListenPort()));
        config.append(String.format("PrivateKey = %s%n", networkConfig.getNetworkInterface().getPrivateKey()));

        if (hookLines != null && !hookLines.isBlank()) {
            config.append(hookLines.stripTrailing()).append("\n");
        }

        for (NetworkPeer peer : networkConfig.getPeers()) {
            // the trailing comment on [Peer] identifies the entry for humans reading the file.
            // it is not used to locate entries, peers are matched on AllowedIPs when parsed
            config.append("\n");
            config.append(String.format("[Peer] # %s%n", peer.getAllowedIps()));
            config.append(String.format("PublicKey = %s%n", peer.getPublicKey()));
            config.append(String.format("AllowedIPs = %s%n", peer.getAllowedIps()));
        }

        return config.toString();
    }

    /**
     * renders a client config. this file is handed to the end user, we do not control what
     * they do with it once delivered.
     */
    public static String renderClientConfig(ClientConfig clientConfig) {
        StringBuilder config = new StringBuilder();
        config.append("[Interface]\n");
        config.append(String.format("PrivateKey = %s%n", clientConfig.getNetworkInterface().getPrivateKey()));
        config.append(String.format("Address = %s%n", clientConfig.getNetworkInterface().getAddress()));
        config.append(String.format("DNS = %s%n", clientConfig.getDns()));
        config.append("[Peer]\n");
        config.append(String.format("PublicKey = %s%n", clientConfig.getPeerConfig().getPublicKey()));
        config.append(String.format("Endpoint = %s%n", clientConfig.getPeerConfig().getEndpoint()));
        config.append(String.format("AllowedIPs = %s%n", clientConfig.getPeerConfig().getAllowedIps()));
        return config.toString();
    }

    public static void writeNetworkConfig(String configPath, NetworkConfig networkConfig) {
        writeNetworkConfig(configPath, networkConfig, null);
    }

    public static void writeNetworkConfig(String configPath, NetworkConfig networkConfig, String hookLines) {
        writeConfigFile(configPath, renderNetworkConfig(networkConfig, hookLines));
    }

    public static void writeClientConfig(String configPath, ClientConfig clientConfig) {
        writeConfigFile(configPath, renderClientConfig(clientConfig));
    }

    /**
     * writes content to configPath with owner only permissions. the file is written to a
     * temporary path in the same directory and moved into place so a failed write cannot
     * leave a partially written config where an interface may be brought up from it.
     */
    public static void writeConfigFile(String configPath, String content) {
        Path filePath = Paths.get(configPath);
        Path parentDir = filePath.getParent();
        if (parentDir == null) {
            throw new WireguardConfigFileWriteException(
                    String.format("cannot determine parent directory of '%s'", configPath));
        }

        Path tempPath = null;
        try {
            Files.createDirectories(parentDir);
            tempPath = Files.createTempFile(
                    parentDir,
                    filePath.getFileName().toString(),
                    ".tmp",
                    PosixFilePermissions.asFileAttribute(OWNER_READ_WRITE));
            Files.writeString(tempPath, content);
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            // the move target may pre-exist with other permissions, so set them after the move
            Files.setPosixFilePermissions(filePath, OWNER_READ_WRITE);
        } catch (IOException | UnsupportedOperationException e) {
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException cleanupException) {
                    // the write already failed, surface that rather than the cleanup failure
                }
            }
            throw new WireguardConfigFileWriteException(
                    String.format("failed to write config file '%s': %s", configPath, e.getMessage()));
        }
    }
}
