package com.brcsrc.yaws.system;

import com.brcsrc.yaws.shell.CommandExecutor;
import com.brcsrc.yaws.shell.ExecutionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

public class WireguardInformationProviderTests {

    private ScheduledExecutorService testScheduler;
    private WireguardInformationProvider provider;

    @BeforeEach
    void setUp() {
        testScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void tearDown() {
        if (testScheduler != null && !testScheduler.isShutdown()) {
            testScheduler.shutdownNow();
        }
        if (provider != null) {
            provider.shutdown();
        }
    }

    @Test
    void testParseWgShowDump_WithValidData() {
        // Create a mock executor
        CommandExecutor mockExecutor = command -> {
            String mockOutput = "wg0\tprivateKeyValue\tpHIHd17qGbJlqYmKnBZcXxXJkPUeARJNfIJadpgKHG0=\t62230\t0\n" +
                    "wg0\tuRj79LQYtcfZP4W7of9kvxuO7JSOyyJvkSRhhpSpgno=\tpresharedKeyValue\t172.56.149.120:62118\t10.6.0.6/32\t1234567890\t2764800000\t14400000000\t0\n" +
                    "wg0\tanotherPeerKey123456789012345678901234567890ab=\tpresharedKeyValue2\t192.168.1.1:51820\t10.6.0.7/32\t1234567900\t1024000\t2048000\t25\n";
            return new ExecutionResult(mockOutput, "", 0);
        };

        provider = new WireguardInformationProvider(mockExecutor, testScheduler);

        String testData = "wg0\tprivateKeyValue\tpHIHd17qGbJlqYmKnBZcXxXJkPUeARJNfIJadpgKHG0=\t62230\t0\n" +
                "wg0\tuRj79LQYtcfZP4W7of9kvxuO7JSOyyJvkSRhhpSpgno=\tpresharedKeyValue\t172.56.149.120:62118\t10.6.0.6/32\t1234567890\t2764800000\t14400000000\t0\n" +
                "wg0\tanotherPeerKey123456789012345678901234567890ab=\tpresharedKeyValue2\t192.168.1.1:51820\t10.6.0.7/32\t1234567900\t1024000\t2048000\t25\n";

        WireguardInformationProvider.WireguardConnectionData result = provider.parseWgShowDump(testData);

        // Verify network was parsed
        Map<String, NetworkConnectionInformation> networks = result.getNetworksByPublicKey();
        assertEquals(1, networks.size());
        assertTrue(networks.containsKey("pHIHd17qGbJlqYmKnBZcXxXJkPUeARJNfIJadpgKHG0="));

        NetworkConnectionInformation network = networks.get("pHIHd17qGbJlqYmKnBZcXxXJkPUeARJNfIJadpgKHG0=");
        assertEquals("wg0", network.getInterfaceName());
        assertEquals("pHIHd17qGbJlqYmKnBZcXxXJkPUeARJNfIJadpgKHG0=", network.getPublicKey());
        assertEquals(62230, network.getListeningPort());

        // Verify peers were parsed
        Map<String, ClientConnectionInformation> peers = result.getPeersByPublicKey();
        assertEquals(2, peers.size());

        // Verify first peer
        ClientConnectionInformation peer1 = peers.get("uRj79LQYtcfZP4W7of9kvxuO7JSOyyJvkSRhhpSpgno=");
        assertNotNull(peer1);
        assertEquals("uRj79LQYtcfZP4W7of9kvxuO7JSOyyJvkSRhhpSpgno=", peer1.getPublicKey());
        assertEquals("172.56.149.120:62118", peer1.getEndpoint());
        assertEquals("10.6.0.6/32", peer1.getAllowedIps());
        assertEquals(1234567890L, peer1.getLatestHandshakeEpochSeconds());
        assertEquals(2764800000L, peer1.getBytesReceived());
        assertEquals(14400000000L, peer1.getBytesSent());

        // Verify second peer
        ClientConnectionInformation peer2 = peers.get("anotherPeerKey123456789012345678901234567890ab=");
        assertNotNull(peer2);
        assertEquals("anotherPeerKey123456789012345678901234567890ab=", peer2.getPublicKey());
        assertEquals("192.168.1.1:51820", peer2.getEndpoint());
        assertEquals("10.6.0.7/32", peer2.getAllowedIps());
        assertEquals(25L, peer2.getPersistentKeepalive());

        // Verify peers are associated with network
        Map<String, ClientConnectionInformation> networkPeers = network.getPeers();
        assertEquals(2, networkPeers.size());
        assertTrue(networkPeers.containsKey("uRj79LQYtcfZP4W7of9kvxuO7JSOyyJvkSRhhpSpgno="));
        assertTrue(networkPeers.containsKey("anotherPeerKey123456789012345678901234567890ab="));
    }

    @Test
    void testParseWgShowDump_WithEmptyOutput() {
        CommandExecutor mockExecutor = command -> new ExecutionResult("", "", 0);
        provider = new WireguardInformationProvider(mockExecutor, testScheduler);

        WireguardInformationProvider.WireguardConnectionData result = provider.parseWgShowDump("");

        assertTrue(result.getNetworksByPublicKey().isEmpty());
        assertTrue(result.getPeersByPublicKey().isEmpty());
    }

    @Test
    void testParseWgShowDump_WithMultipleInterfaces() {
        CommandExecutor mockExecutor = command -> new ExecutionResult("", "", 0);
        provider = new WireguardInformationProvider(mockExecutor, testScheduler);

        String testData = "wg0\tprivateKey1\tpublicKey1\t62230\t0\n" +
                "wg0\tpeerKey1\tpsk1\t1.2.3.4:1234\t10.0.0.1/32\t1000\t100\t200\t0\n" +
                "wg1\tprivateKey2\tpublicKey2\t51820\t0\n" +
                "wg1\tpeerKey2\tpsk2\t5.6.7.8:5678\t10.0.1.1/32\t2000\t300\t400\t0\n";

        WireguardInformationProvider.WireguardConnectionData result = provider.parseWgShowDump(testData);

        assertEquals(2, result.getNetworksByPublicKey().size());
        assertEquals(2, result.getPeersByPublicKey().size());

        assertTrue(result.getNetworksByPublicKey().containsKey("publicKey1"));
        assertTrue(result.getNetworksByPublicKey().containsKey("publicKey2"));
        assertTrue(result.getPeersByPublicKey().containsKey("peerKey1"));
        assertTrue(result.getPeersByPublicKey().containsKey("peerKey2"));
    }

    @Test
    void testRefreshConnectionData_Success() {
        CommandExecutor mockExecutor = command -> {
            String mockOutput = "wg0\tprivateKey\tpublicKey\t62230\t0\n" +
                    "wg0\tpeerKey\tpsk\t1.2.3.4:1234\t10.0.0.1/32\t1000\t100\t200\t0\n";
            return new ExecutionResult(mockOutput, "", 0);
        };

        provider = new WireguardInformationProvider(mockExecutor, testScheduler);

        // Trigger refresh
        provider.refreshConnectionData();

        // Verify data was loaded
        NetworkConnectionInformation network = provider.getNetworkByPublicKey("publicKey");
        assertNotNull(network);
        assertEquals("wg0", network.getInterfaceName());

        ClientConnectionInformation peer = provider.getPeerByPublicKey("peerKey");
        assertNotNull(peer);
        assertEquals("1.2.3.4:1234", peer.getEndpoint());
    }

    @Test
    void testRefreshConnectionData_CommandFailure() {
        CommandExecutor mockExecutor = command -> new ExecutionResult("", "command not found", 127);

        provider = new WireguardInformationProvider(mockExecutor, testScheduler);

        // Trigger refresh - should not throw exception
        assertDoesNotThrow(() -> provider.refreshConnectionData());

        // Data should be empty since command failed
        assertTrue(provider.getAllNetworks().isEmpty());
        assertTrue(provider.getAllPeers().isEmpty());
    }

    @Test
    void testGetNetworkByPublicKey() {
        CommandExecutor mockExecutor = command -> {
            String mockOutput = "wg0\tprivateKey\ttestPublicKey\t62230\t0\n";
            return new ExecutionResult(mockOutput, "", 0);
        };

        provider = new WireguardInformationProvider(mockExecutor, testScheduler);
        provider.refreshConnectionData();

        NetworkConnectionInformation network = provider.getNetworkByPublicKey("testPublicKey");
        assertNotNull(network);
        assertEquals("testPublicKey", network.getPublicKey());
        assertEquals("wg0", network.getInterfaceName());

        // Test non-existent key
        assertNull(provider.getNetworkByPublicKey("nonExistentKey"));
    }

    @Test
    void testGetPeerByPublicKey() {
        CommandExecutor mockExecutor = command -> {
            String mockOutput = "wg0\tprivateKey\tnetworkKey\t62230\t0\n" +
                    "wg0\ttestPeerKey\tpsk\t1.2.3.4:1234\t10.0.0.1/32\t1000\t100\t200\t0\n";
            return new ExecutionResult(mockOutput, "", 0);
        };

        provider = new WireguardInformationProvider(mockExecutor, testScheduler);
        provider.refreshConnectionData();

        ClientConnectionInformation peer = provider.getPeerByPublicKey("testPeerKey");
        assertNotNull(peer);
        assertEquals("testPeerKey", peer.getPublicKey());
        assertEquals("1.2.3.4:1234", peer.getEndpoint());

        // Test non-existent key
        assertNull(provider.getPeerByPublicKey("nonExistentKey"));
    }

    @Test
    void testGetAllNetworks() {
        CommandExecutor mockExecutor = command -> {
            String mockOutput = "wg0\tprivateKey1\tkey1\t62230\t0\n" +
                    "wg1\tprivateKey2\tkey2\t51820\t0\n";
            return new ExecutionResult(mockOutput, "", 0);
        };

        provider = new WireguardInformationProvider(mockExecutor, testScheduler);
        provider.refreshConnectionData();

        Map<String, NetworkConnectionInformation> networks = provider.getAllNetworks();
        assertEquals(2, networks.size());
        assertTrue(networks.containsKey("key1"));
        assertTrue(networks.containsKey("key2"));
    }

    @Test
    void testGetAllPeers() {
        CommandExecutor mockExecutor = command -> {
            String mockOutput = "wg0\tprivateKey\tnetworkKey\t62230\t0\n" +
                    "wg0\tpeer1\tpsk1\t1.2.3.4:1234\t10.0.0.1/32\t1000\t100\t200\t0\n" +
                    "wg0\tpeer2\tpsk2\t5.6.7.8:5678\t10.0.0.2/32\t2000\t300\t400\t0\n";
            return new ExecutionResult(mockOutput, "", 0);
        };

        provider = new WireguardInformationProvider(mockExecutor, testScheduler);
        provider.refreshConnectionData();

        Map<String, ClientConnectionInformation> peers = provider.getAllPeers();
        assertEquals(2, peers.size());
        assertTrue(peers.containsKey("peer1"));
        assertTrue(peers.containsKey("peer2"));
    }
}
