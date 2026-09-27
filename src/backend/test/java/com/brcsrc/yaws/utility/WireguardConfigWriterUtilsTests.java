package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.exceptions.WireguardConfigFileWriteException;
import com.brcsrc.yaws.model.wireguard.ClientConfig;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import com.brcsrc.yaws.model.wireguard.NetworkInterface;
import com.brcsrc.yaws.model.wireguard.NetworkPeer;
import com.brcsrc.yaws.model.wireguard.PeerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class WireguardConfigWriterUtilsTests {

    private NetworkConfig testNetworkConfig() {
        return new NetworkConfig(new NetworkInterface(
                "10.100.0.1/24",
                51820,
                "dummyNetworkPrivateKey"));
    }

    @Test
    void testRenderNetworkConfigProducesInterfaceSection() {
        String rendered = WireguardConfigWriterUtils.renderNetworkConfig(testNetworkConfig());

        assertEquals(
                "[Interface]\n"
                        + "Address = 10.100.0.1/24\n"
                        + "ListenPort = 51820\n"
                        + "PrivateKey = dummyNetworkPrivateKey\n",
                rendered);
    }

    @Test
    void testRenderNetworkConfigIncludesHookLines() {
        String hooks = "PostUp = iptables -N TEST-CHAIN\nPostDown = iptables -X TEST-CHAIN";
        String rendered = WireguardConfigWriterUtils.renderNetworkConfig(testNetworkConfig(), hooks);

        assertTrue(rendered.contains("PostUp = iptables -N TEST-CHAIN\n"));
        assertTrue(rendered.contains("PostDown = iptables -X TEST-CHAIN\n"));
        // hooks belong to the [Interface] section so they must precede any peer entry
        assertTrue(rendered.indexOf("PostUp") > rendered.indexOf("[Interface]"));
    }

    @Test
    void testRenderNetworkConfigWritesPeers() {
        NetworkConfig networkConfig = new NetworkConfig(
                new NetworkInterface("10.100.0.1/24", 51820, "dummyNetworkPrivateKey"),
                List.of(
                        new NetworkPeer("dummyPeerOneKey", "10.100.0.2/32"),
                        new NetworkPeer("dummyPeerTwoKey", "10.100.0.3/32")));

        String rendered = WireguardConfigWriterUtils.renderNetworkConfig(networkConfig);

        assertTrue(rendered.contains("[Peer] # 10.100.0.2/32\n"));
        assertTrue(rendered.contains("PublicKey = dummyPeerOneKey\n"));
        assertTrue(rendered.contains("AllowedIPs = 10.100.0.2/32\n"));
        assertTrue(rendered.contains("[Peer] # 10.100.0.3/32\n"));
        assertTrue(rendered.contains("PublicKey = dummyPeerTwoKey\n"));
        assertTrue(rendered.contains("AllowedIPs = 10.100.0.3/32\n"));
    }

    @Test
    void testRenderClientConfigProducesInterfaceAndPeerSections() {
        ClientConfig clientConfig = new ClientConfig(
                new NetworkInterface("10.100.0.2/32", 51820, "dummyClientPrivateKey"),
                new PeerConfig("dummyNetworkPublicKey", "vpn.example.com:51820", "0.0.0.0/0"),
                "1.1.1.1");

        String rendered = WireguardConfigWriterUtils.renderClientConfig(clientConfig);

        assertEquals(
                "[Interface]\n"
                        + "PrivateKey = dummyClientPrivateKey\n"
                        + "Address = 10.100.0.2/32\n"
                        + "DNS = 1.1.1.1\n"
                        + "[Peer]\n"
                        + "PublicKey = dummyNetworkPublicKey\n"
                        + "Endpoint = vpn.example.com:51820\n"
                        + "AllowedIPs = 0.0.0.0/0\n",
                rendered);
    }

    @Test
    void testBuildNetworkHookLinesInstallsPlumbingRules() {
        String hooks = WireguardConfigWriterUtils.buildNetworkHookLines("Network1", "10.100.0.1/24");

        // traffic must be able to reach the network and be NAT'd out to the internet. these were
        // previously applied out of band by the configure-iptables script
        assertTrue(hooks.contains("PostUp = iptables -I INPUT -s 10.100.0.1/24 -j ACCEPT"));
        assertTrue(hooks.contains("PostUp = iptables -I OUTPUT -d 10.100.0.1/24 -j ACCEPT"));
        assertTrue(hooks.contains("PostUp = iptables -t nat -A POSTROUTING -s 10.100.0.1/24"));
        assertTrue(hooks.contains("-j MASQUERADE"));
    }

    @Test
    void testBuildNetworkHookLinesResolvesEgressInterfaceFromDefaultRoute() {
        String hooks = WireguardConfigWriterUtils.buildNetworkHookLines("Network1", "10.100.0.1/24");

        // the egress interface must not be hardcoded to eth0. it is resolved by the shell when
        // the hook runs so the rules follow whatever interface carries traffic off the host
        assertTrue(hooks.contains("$(ip route show default"));
        assertTrue(!hooks.contains("-o eth0"), "egress interface must not be hardcoded to eth0");
    }

    @Test
    void testBuildNetworkHookLinesCreatesAndLinksIsolationChain() {
        String hooks = WireguardConfigWriterUtils.buildNetworkHookLines("Network1", "10.100.0.1/24");

        assertTrue(hooks.contains("PostUp = iptables -N YAWS-ISO-Network1"));
        // the chain must be reachable only from the peer to peer match, so rules in it cannot
        // affect a client reaching the internet or the server itself
        assertTrue(hooks.contains("PostUp = iptables -I FORWARD -i %i -o %i -j YAWS-ISO-Network1"));
    }

    @Test
    void testBuildNetworkHookLinesContainNoIsolationPolicy() {
        String hooks = WireguardConfigWriterUtils.buildNetworkHookLines("Network1", "10.100.0.1/24");

        // the isolation chain is created empty. policy is applied at runtime from the database so
        // isolation can be toggled without rewriting this file and cycling the interface
        assertTrue(!hooks.contains("-j DROP"), "hooks must not contain isolation policy rules");
        assertTrue(!hooks.contains(String.format("-A %s", "YAWS-ISO-Network1")),
                "hooks must not append rules into the isolation chain");
    }

    @Test
    void testBuildNetworkHookLinesTeardownMirrorsSetup() {
        String hooks = WireguardConfigWriterUtils.buildNetworkHookLines("Network1", "10.100.0.1/24");

        // every rule installed on the way up must be removed on the way down, otherwise rules
        // accumulate across interface cycles
        assertTrue(hooks.contains("PostDown = iptables -D INPUT -s 10.100.0.1/24 -j ACCEPT"));
        assertTrue(hooks.contains("PostDown = iptables -D OUTPUT -d 10.100.0.1/24 -j ACCEPT"));
        assertTrue(hooks.contains("PostDown = iptables -t nat -D POSTROUTING -s 10.100.0.1/24"));
        assertTrue(hooks.contains("PostDown = iptables -X YAWS-ISO-Network1"));
    }

    @Test
    void testBuildNetworkHookLinesUnlinkChainBeforeDeleting() {
        String hooks = WireguardConfigWriterUtils.buildNetworkHookLines("Network1", "10.100.0.1/24");
        String[] lines = hooks.split("\n");

        int unlinkIndex = -1;
        int deleteIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("PostDown") && lines[i].contains("-D FORWARD")) {
                unlinkIndex = i;
            }
            if (lines[i].startsWith("PostDown") && lines[i].contains("-X ")) {
                deleteIndex = i;
            }
        }

        assertTrue(unlinkIndex >= 0, "expected a PostDown line unlinking the chain from FORWARD");
        assertTrue(deleteIndex >= 0, "expected a PostDown line deleting the chain");
        // iptables refuses to delete a chain that is still referenced
        assertTrue(unlinkIndex < deleteIndex,
                "chain must be unlinked from FORWARD before it can be deleted");
    }

    @Test
    void testWriteConfigFileIsOwnerReadWriteOnly(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("owner-only.conf");

        WireguardConfigWriterUtils.writeConfigFile(configPath.toString(), "content\n");

        // config files hold private keys and must not be group or world readable
        assertEquals(
                "rw-------",
                PosixFilePermissions.toString(Files.getPosixFilePermissions(configPath)));
        assertEquals("content\n", Files.readString(configPath));
    }

    @Test
    void testWriteConfigFileOverwritesExistingFileAndResetsPermissions(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("existing.conf");
        Files.writeString(configPath, "old content\n");
        Files.setPosixFilePermissions(configPath, PosixFilePermissions.fromString("rw-r--r--"));

        WireguardConfigWriterUtils.writeConfigFile(configPath.toString(), "new content\n");

        assertEquals("new content\n", Files.readString(configPath));
        assertEquals(
                "rw-------",
                PosixFilePermissions.toString(Files.getPosixFilePermissions(configPath)));
    }

    @Test
    void testWriteConfigFileLeavesNoTempFilesBehind(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("clean.conf");

        WireguardConfigWriterUtils.writeConfigFile(configPath.toString(), "content\n");

        try (var entries = Files.list(tempDir)) {
            List<String> remaining = entries.map(path -> path.getFileName().toString()).toList();
            assertEquals(List.of("clean.conf"), remaining);
        }
    }

    @Test
    void testWriteConfigFileThrowsWhenPathIsNotWritable(@TempDir Path tempDir) throws IOException {
        // root ignores permission bits, so an unwritable directory cannot be simulated when the
        // suite runs as root, which it does inside the test container
        assumeFalse("root".equals(System.getProperty("user.name")),
                "cannot test unwritable paths as root");

        Path readOnlyDir = Files.createDirectory(tempDir.resolve("readonly"));
        Files.setPosixFilePermissions(readOnlyDir, PosixFilePermissions.fromString("r-xr-xr-x"));
        Path configPath = readOnlyDir.resolve("blocked.conf");

        assertThrows(
                WireguardConfigFileWriteException.class,
                () -> WireguardConfigWriterUtils.writeConfigFile(configPath.toString(), "content\n"));
        assertFalse(Files.exists(configPath));

        // restore permissions so the temp dir can be cleaned up
        Files.setPosixFilePermissions(readOnlyDir, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
