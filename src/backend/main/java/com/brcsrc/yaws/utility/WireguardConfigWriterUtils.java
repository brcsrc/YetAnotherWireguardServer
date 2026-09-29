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
import java.util.List;
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
     * builds the PostUp/PostDown lines for a network.
     *
     * these carry every iptables rule the interface needs, so bringing the interface up installs
     * them and bringing it down removes them. nothing applies interface level rules out of band,
     * which means there is no saved ruleset to restore and no state that can drift from the
     * config file. the config file is the persistence.
     *
     * two groups of rules are written:
     *
     * the plumbing rules, which let traffic reach the network and NAT it out to the internet.
     * the egress interface is resolved from the default route at hook time rather than assumed
     * to be eth0, so the rules follow whatever interface actually carries traffic off the host.
     *
     * the peer isolation chain, created empty and linked from the peer to peer match in FORWARD.
     * policy rules are inserted into it at runtime from the database, never written here, so
     * isolation can be toggled with a live iptables call instead of rewriting this file and
     * cycling the interface, which would drop every client on the network.
     */
    public static String buildNetworkHookLines(String networkName, String networkCidr) {
        String chainName = PeerIsolationUtils.getIsolationChainName(networkName);
        // $(...) is evaluated by the shell wg-quick runs the hook in, so the egress interface is
        // resolved when the interface comes up rather than baked into the file
        String egressInterface = "$(ip route show default | awk '/default/ {print $5; exit}')";

        return String.join("\n", List.of(
                // allow traffic to and from the network
                String.format("PostUp = iptables -I INPUT -s %s -j ACCEPT", networkCidr),
                String.format("PostUp = iptables -I OUTPUT -d %s -j ACCEPT", networkCidr),
                // NAT client traffic out to the internet
                String.format("PostUp = iptables -t nat -A POSTROUTING -s %s -o %s -j MASQUERADE",
                        networkCidr, egressInterface),
                // peer isolation chain, reachable only from the peer to peer match
                String.format("PostUp = iptables -N %s", chainName),
                String.format("PostUp = iptables -I FORWARD -i %%i -o %%i -j %s", chainName),

                // teardown mirrors the above in reverse. a chain cannot be deleted while it is
                // still referenced, so the FORWARD jump goes first
                String.format("PostDown = iptables -D FORWARD -i %%i -o %%i -j %s", chainName),
                String.format("PostDown = iptables -F %s", chainName),
                String.format("PostDown = iptables -X %s", chainName),
                String.format("PostDown = iptables -t nat -D POSTROUTING -s %s -o %s -j MASQUERADE",
                        networkCidr, egressInterface),
                String.format("PostDown = iptables -D OUTPUT -d %s -j ACCEPT", networkCidr),
                String.format("PostDown = iptables -D INPUT -s %s -j ACCEPT", networkCidr)
        ));
    }

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
     * PostUp/PostDown entries, as produced by
     * {@link #buildNetworkHookLines(String, String)}.
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
