package com.brcsrc.yaws.system;

import com.brcsrc.yaws.shell.CommandExecutor;
import com.brcsrc.yaws.shell.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

/**
 * Provides cached WireGuard connection information for all networks and clients.
 * This singleton-style component maintains an in-memory cache of connection data
 * from 'wg show dump' that is refreshed every 5 seconds in the background.
 *
 * Startup Sequence:
 * 1. Application starts - Spring Boot begins initializing
 * 2. Component scanning - Spring discovers @Component classes, including:
 *    - StartupTasks
 *    - WireguardInformationProvider
 *    - DefaultCommandExecutor
 * 3. Dependency injection - Spring creates beans and injects dependencies:
 *    - Creates DefaultCommandExecutor
 *    - Creates WireguardInformationProvider (injecting DefaultCommandExecutor)
 *    - Calls @PostConstruct initialize() on WireguardInformationProvider
 * 4. initialize() runs:
 *    - Performs initial wg show dump (gets current state)
 *    - Starts background scheduler (refreshes every 5000ms)
 * 5. StartupTasks.restartActiveNetworks() runs (via @Async):
 *    - Restarts all active WireGuard networks
 *    - Runs independently/asynchronously
 *
 * Thread Safety:
 * - Uses AtomicReference for the main data holder
 * - Uses ConcurrentHashMap for all internal maps
 * - Safe for concurrent access from multiple SSE endpoints
 */
@Component
public class WireguardInformationProvider {
    private static final Logger logger = LoggerFactory.getLogger(WireguardInformationProvider.class);
    private static final long REFRESH_INTERVAL_MS = 5000;
    private static final String WG_SHOW_DUMP_COMMAND = "wg show all dump";

    private final AtomicReference<WireguardConnectionData> connectionDataRef;
    private final ScheduledExecutorService scheduler;
    private final CommandExecutor commandExecutor;

    @Autowired
    public WireguardInformationProvider(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.connectionDataRef = new AtomicReference<>(new WireguardConnectionData());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    // Constructor for testing
    WireguardInformationProvider(CommandExecutor commandExecutor, ScheduledExecutorService scheduler) {
        this.commandExecutor = commandExecutor;
        this.connectionDataRef = new AtomicReference<>(new WireguardConnectionData());
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing WireguardInformationProvider with {}ms refresh interval", REFRESH_INTERVAL_MS);

        // Initial fetch
        refreshConnectionData();

        // Schedule periodic refresh
        scheduler.scheduleAtFixedRate(
            this::refreshConnectionData,
            REFRESH_INTERVAL_MS,
            REFRESH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        logger.info("WireguardInformationProvider initialized successfully");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down WireguardInformationProvider");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    void refreshConnectionData() {
        try {
            ExecutionResult result = commandExecutor.runCommand(WG_SHOW_DUMP_COMMAND);

            if (result.getExitCode() != 0) {
                logger.error("Failed to execute '{}': exit code {}, stderr: {}",
                    WG_SHOW_DUMP_COMMAND, result.getExitCode(), result.getStderr());
                return;
            }

            WireguardConnectionData newData = parseWgShowDump(result.getStdout());
            connectionDataRef.set(newData);

            logger.debug("Successfully refreshed WireGuard connection data: {} interfaces, {} total peers",
                newData.getNetworksByPublicKey().size(),
                newData.getPeersByPublicKey().size());

        } catch (Exception e) {
            logger.error("Error refreshing WireGuard connection data", e);
        }
    }

    /**
     * Parses the output of 'wg show dump' command.
     *
     * Format (tab-delimited):
     * - Interface line: interface_name\tprivate_key\tpublic_key\tlisten_port\tfwmark
     * - Peer lines: interface_name\tpeer_public_key\tpreshared_key\tendpoint\tallowed_ips\tlatest_handshake\ttransfer_rx\ttransfer_tx\tpersistent_keepalive
     */
    WireguardConnectionData parseWgShowDump(String output) {
        WireguardConnectionData data = new WireguardConnectionData();

        if (output == null || output.trim().isEmpty()) {
            return data;
        }

        String[] lines = output.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\t");

            if (parts.length < 4) {
                logger.warn("Skipping malformed line in wg show dump output: {}", line);
                continue;
            }

            String interfaceName = parts[0];

            // Interface line has 5 fields, peer line has 9 fields
            if (parts.length == 5) {
                // Interface line: interface_name, private_key, public_key, listen_port, fwmark
                parseInterfaceLine(data, parts);
            } else if (parts.length >= 8) {
                // Peer line: interface_name, peer_public_key, preshared_key, endpoint, allowed_ips,
                //            latest_handshake, transfer_rx, transfer_tx, [persistent_keepalive]
                parsePeerLine(data, parts);
            } else {
                logger.warn("Unexpected number of fields ({}) in wg show dump line: {}", parts.length, line);
            }
        }

        return data;
    }

    private void parseInterfaceLine(WireguardConnectionData data, String[] parts) {
        String interfaceName = parts[0];
        String publicKey = parts[2];
        int listeningPort = parseIntOrDefault(parts[3], 0);

        NetworkConnectionInformation networkInfo = new NetworkConnectionInformation(
            interfaceName,
            publicKey,
            listeningPort
        );

        data.addNetwork(publicKey, networkInfo);
        logger.debug("Parsed interface: {} (public key: {})", interfaceName, publicKey);
    }

    private void parsePeerLine(WireguardConnectionData data, String[] parts) {
        String interfaceName = parts[0];
        String peerPublicKey = parts[1];
        String presharedKey = parts[2];
        String endpoint = parts[3];
        String allowedIps = parts[4];
        Long latestHandshake = parseLongOrNull(parts[5]);
        Long bytesReceived = parseLongOrNull(parts[6]);
        Long bytesSent = parseLongOrNull(parts[7]);
        Long persistentKeepalive = parts.length > 8 ? parseLongOrNull(parts[8]) : null;

        ClientConnectionInformation clientInfo = new ClientConnectionInformation(
            peerPublicKey,
            presharedKey,
            endpoint,
            allowedIps,
            latestHandshake,
            bytesReceived,
            bytesSent,
            persistentKeepalive
        );

        data.addPeer(peerPublicKey, clientInfo);

        // Also add this peer to its parent network
        NetworkConnectionInformation networkInfo = data.getNetworkByInterface(interfaceName);
        if (networkInfo != null) {
            networkInfo.addPeer(peerPublicKey, clientInfo);
        } else {
            logger.warn("Peer {} belongs to unknown interface {}", peerPublicKey, interfaceName);
        }

        logger.debug("Parsed peer: {} on interface {}", peerPublicKey, interfaceName);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long parseLongOrNull(String value) {
        try {
            if (value == null || value.trim().isEmpty() || value.equals("0")) {
                return null;
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get network connection information by public key
     */
    public NetworkConnectionInformation getNetworkByPublicKey(String publicKey) {
        return connectionDataRef.get().getNetworksByPublicKey().get(publicKey);
    }

    /**
     * Get peer (client) connection information by public key
     */
    public ClientConnectionInformation getPeerByPublicKey(String publicKey) {
        return connectionDataRef.get().getPeersByPublicKey().get(publicKey);
    }

    /**
     * Get all network connection information
     */
    public Map<String, NetworkConnectionInformation> getAllNetworks() {
        return new ConcurrentHashMap<>(connectionDataRef.get().getNetworksByPublicKey());
    }

    /**
     * Get all peer connection information
     */
    public Map<String, ClientConnectionInformation> getAllPeers() {
        return new ConcurrentHashMap<>(connectionDataRef.get().getPeersByPublicKey());
    }

    /**
     * Internal data structure to hold all WireGuard connection information
     */
    static class WireguardConnectionData {
        private final Map<String, NetworkConnectionInformation> networksByPublicKey;
        private final Map<String, NetworkConnectionInformation> networksByInterface;
        private final Map<String, ClientConnectionInformation> peersByPublicKey;

        public WireguardConnectionData() {
            this.networksByPublicKey = new ConcurrentHashMap<>();
            this.networksByInterface = new ConcurrentHashMap<>();
            this.peersByPublicKey = new ConcurrentHashMap<>();
        }

        public void addNetwork(String publicKey, NetworkConnectionInformation network) {
            networksByPublicKey.put(publicKey, network);
            networksByInterface.put(network.getInterfaceName(), network);
        }

        public void addPeer(String publicKey, ClientConnectionInformation peer) {
            peersByPublicKey.put(publicKey, peer);
        }

        public Map<String, NetworkConnectionInformation> getNetworksByPublicKey() {
            return networksByPublicKey;
        }

        public Map<String, ClientConnectionInformation> getPeersByPublicKey() {
            return peersByPublicKey;
        }

        public NetworkConnectionInformation getNetworkByInterface(String interfaceName) {
            return networksByInterface.get(interfaceName);
        }
    }
}
