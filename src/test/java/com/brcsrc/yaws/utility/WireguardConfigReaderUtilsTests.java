package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.exceptions.WireguardConfigFileReadException;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.wireguard.ClientConfig;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import static org.junit.jupiter.api.Assertions.*;

public class WireguardConfigReaderUtilsTests {

    private static final Logger logger = LoggerFactory.getLogger(WireguardConfigReaderUtilsTests.class);

    private static final String BASE_PATH = String.format("%s/", Constants.BASE_WIREGUARD_DIR);
    private static final String testNetworkName = "Network1";
    private static final String clientConfigName = "client1";
    private static final String clientConfigFileName = String.format("%s.conf", clientConfigName);

    @BeforeEach
    public void setUp() {
        String testPathDirs = String.format("%s%s/clients", BASE_PATH, testNetworkName);
        File dir = new File(testPathDirs);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IllegalStateException(String.format("failed to create test directory: '%s'", testPathDirs));
            }
        }
    }

    @AfterEach
    public void teardown() throws IOException {
        String testNetworkConfigToRemovePath = String.format("%s%s.conf", BASE_PATH, testNetworkName);

        File testNetworkConfigToRemove = new File(testNetworkConfigToRemovePath);
        if (testNetworkConfigToRemove.exists()) {
            if (!testNetworkConfigToRemove.delete()) {
                throw new IllegalStateException(String.format("failed to delete test network config after test: '%s'", testNetworkConfigToRemove));
            }
        }

        String testNetworkDirToRemovePath = String.format("%s%s", BASE_PATH, testNetworkName);

        File testNetworkDirToRemove = new File(testNetworkDirToRemovePath);
        if (testNetworkDirToRemove.exists()) {
            FilepathUtils.deleteDirectory(Paths.get(testNetworkDirToRemovePath));
        }
    }


    private void createFile(String filePath, String content) throws IOException {
        Files.write(
                Paths.get(filePath),
                content.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    @Test
    public void testReadNetworkConfigPassesOnValidFileName() throws IOException {
        String validFileName = "validConfig.conf";
        String filePath = BASE_PATH + validFileName;
        String content = """
                [Interface]
                PrivateKey = ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890=
                Address = 10.0.0.1/24
                ListenPort = 51820
                """;
        createFile(filePath, content);

        assertDoesNotThrow(() -> WireguardConfigReaderUtils.readNetworkConfig(validFileName));
    }

    @Test
    public void testReadNetworkConfigPassesOnValidConfigFile() throws IOException {
        String fileName = "validConfig.conf";
        String filePath = BASE_PATH + fileName;
        String content = """
                [Interface]
                PrivateKey = ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890=
                Address = 10.0.0.1/24
                ListenPort = 51820
                """;

        createFile(filePath, content);

        assertDoesNotThrow(() -> WireguardConfigReaderUtils.readNetworkConfig(fileName));

        NetworkConfig config = WireguardConfigReaderUtils.readNetworkConfig(fileName);
        assertEquals(config.networkInterface.address, "10.0.0.1/24");
        assertEquals(config.networkInterface.listenPort, 51820);
        assertEquals(config.networkInterface.privateKey, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890=");

        Files.delete(Paths.get(filePath));
    }

    @Test
    public void testReadNetworkConfigThrowsExceptionOnInvalidFileName() {
        String invalidFileName = "invalid config!.conf"; // Invalid due to spaces and special characters

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(invalidFileName);
        });
        assertEquals("file does not match expected naming pattern", exception.getMessage());
    }

    @Test
    public void testReadNetworkConfigThrowsExceptionOnFileDoesNotExist() {
        String nonExistentFileName = "nonExistent.conf";

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(nonExistentFileName);
        });
        assertTrue(exception.getMessage().contains("file '/etc/wireguard/nonExistent.conf' does not exist"));
    }

    @Test
    public void testReadNetworkConfigThrowsExceptionOnFileIsNotRegularFile() throws IOException {
        String fileName = "notAFile.conf";
        String filePath = BASE_PATH + fileName;
        Files.createDirectory(Paths.get(filePath));

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(fileName);
        });
        assertTrue(exception.getMessage().contains("file '/etc/wireguard/notAFile.conf' is not a regular file"));

        Files.delete(Paths.get(filePath));
    }

    // TODO not a good test until the container run-as user is non root. file is always readable as root user
//    @Test
//    public void testFileIsNotReadable() throws IOException {
//        String fileName = "notReadable.conf";
//        String filePath = BASE_PATH + fileName;
//        createFile(filePath);
//
//        // remove read permissions
//        Files.setPosixFilePermissions(Paths.get(filePath), PosixFilePermissions.fromString("-w-------"));
//
//        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
//            WireguardConfigReaderUtils.readNetworkConfig(fileName);
//        });
//        logger.info("DEBUG");
//        logger.info(exception.getMessage());
//        assertTrue(exception.getMessage().contains("file '/etc/wireguard/notReadable.conf' is not readable"));
//
//        Files.delete(Paths.get(filePath));
//    }

    @Test
    public void testReadNetworkConfigThrowsExceptionOnFileIsExecutable() throws IOException {
        String fileName = "executable.conf";
        String filePath = BASE_PATH + fileName;
        createFile(filePath, "");

        // set executable permissions
        Files.setPosixFilePermissions(Paths.get(filePath), PosixFilePermissions.fromString("rwxr-xr-x"));

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(fileName);
        });
        assertTrue(exception.getMessage().contains("file '/etc/wireguard/executable.conf' is marked executable, not proceeding"));

        Files.delete(Paths.get(filePath));
    }

    @Test
    public void testReadNetworkConfigThrowsExceptionOnMissingPrivateKey() throws IOException {
        String fileName = "missingPrivateKey.conf";
        String filePath = BASE_PATH + fileName;
        String content = """
                [Interface]
                Address = 10.0.0.1/24
                ListenPort = 51820
                """;

        createFile(filePath, content);

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(fileName);
        });

        assertTrue(exception.getMessage().contains("missing required fields"));

        Files.delete(Paths.get(filePath));
    }

    @Test
    public void testReadNetworkConfigThrowsExceptionOnMissingAddress() throws IOException {
        String fileName = "missingAddress.conf";
        String filePath = BASE_PATH + fileName;
        String content = """
                [Interface]
                PrivateKey = ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890=
                ListenPort = 51820
                """;

        createFile(filePath, content);

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(fileName);
        });

        assertTrue(exception.getMessage().contains("missing required fields"));

        Files.delete(Paths.get(filePath));
    }

    @Test
    public void testReadNetworkConfigThrowsExceptionOnMissingListenPort() throws IOException {
        String fileName = "missingListenPort.conf";
        String filePath = BASE_PATH + fileName;
        String content = """
                [Interface]
                PrivateKey = ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890=
                Address = 10.0.0.1/24
                """;

        createFile(filePath, content);

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(fileName);
        });

        assertTrue(exception.getMessage().contains("missing required fields"));

        Files.delete(Paths.get(filePath));
    }

    @Test
    public void testReadClientConfigPassesOnValidClientConfig() throws IOException {
        String filePath = String.format("%s%s/clients/%s", BASE_PATH, testNetworkName, clientConfigFileName);

        String privateKey = "qG+6d5CnIwEM6PjPaX0boyls1bu6SM+TEw+dDUOoHGs=";
        String address = "10.100.0.3/24";
        String dns = "1.1.1.1";
        String publicKey = "gI0o+VaIUDxe9mFyMFGKE+ExEzKldwzUYoE1m8JUSy0=";
        String endpoint = "127.0.0.1:51820";
        String allowedIps = "0.0.0.0/0";

        String content = String.format("""
            [Interface]
            PrivateKey = %s
            Address = %s
            DNS = %s
            [Peer]
            PublicKey = %s
            Endpoint = %s
            AllowedIPs = %s
        """, privateKey, address, dns, publicKey, endpoint, allowedIps);

        createFile(filePath, content);

        ClientConfig clientConfig = WireguardConfigReaderUtils.readClientConfig(testNetworkName, clientConfigName);

        assertEquals(privateKey, clientConfig.getNetworkInterface().getPrivateKey());
        assertEquals(address, clientConfig.getNetworkInterface().getAddress());
        assertEquals(dns, clientConfig.getDns());
        assertEquals(publicKey, clientConfig.getPeerConfig().getPublicKey());
        assertEquals(endpoint, clientConfig.getPeerConfig().getEndpoint());
        assertEquals(allowedIps, clientConfig.getPeerConfig().getAllowedIps());
    }


    @Test
    public void testReadClientConfigThrowsExceptionOnFileDoesNotExist() {
        String nonExistentConfigName = "nonExistent";

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readClientConfig(testNetworkName, nonExistentConfigName);
        });
        assertTrue(exception.getMessage().contains("file '/etc/wireguard/Network1/clients/nonExistent.conf' does not exist"));
    }

    @Test
    public void testReadClientConfigThrowsExceptionOnFileIsNotRegularFile() throws IOException {
        String configName = "notAFile";
        String configFileName = String.format("%s.conf", configName);

        String filePath = String.format("%s%s/clients/%s", BASE_PATH, testNetworkName, configFileName);
        Files.createDirectory(Paths.get(filePath));

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readClientConfig(testNetworkName, configName);
        });
        assertTrue(exception.getMessage().contains("file '/etc/wireguard/Network1/clients/notAFile.conf' is not a regular file"));
    }

    @Test
    public void testReadClientConfigThrowsExceptionOnFileIsExecutable() throws IOException {
        String configName = "executable";
        String configFileName = String.format("%s.conf", configName);
        String filePath = String.format("%s%s/clients/%s", BASE_PATH, testNetworkName, configFileName);
        createFile(filePath, "");

        // Set executable permissions
        Files.setPosixFilePermissions(Paths.get(filePath), PosixFilePermissions.fromString("rwxr-xr-x"));

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readClientConfig(testNetworkName, configName);
        });
        assertTrue(exception.getMessage().contains("is marked executable, not proceeding"));

        Files.delete(Paths.get(filePath));
    }

    @Test
    public void testReadClientConfigThrowsExceptionOnMissingPrivateKey() throws IOException {
        String configName = "missingPrivateKey";
        String configFileName = String.format("%s.conf", configName);
        String filePath = String.format("%s%s/clients/%s", BASE_PATH, testNetworkName, configFileName);

        String content = """
            [Interface]
            Address = 10.100.0.3/24
            DNS = 1.1.1.1
            [Peer]
            PublicKey = gI0o+VaIUDxe9mFyMFGKE+ExEzKldwzUYoE1m8JUSy0=
            Endpoint = 127.0.0.1:51820
            AllowedIPs = 0.0.0.0/0
        """;
        createFile(filePath, content);

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readClientConfig(testNetworkName, configName);
        });

        assertTrue(exception.getMessage().contains("missing required fields"));
    }

    @Test
    public void testReadClientConfigThrowsExceptionOnMissingAddress() throws IOException {
        String configName = "missingAddress";
        String configFileName = String.format("%s.conf", configName);
        String filePath = String.format("%s%s/clients/%s", BASE_PATH, testNetworkName, configFileName);

        String content = """
            [Interface]
            PrivateKey = qG+6d5CnIwEM6PjPaX0boyls1bu6SM+TEw+dDUOoHGs=
            DNS = 1.1.1.1
            [Peer]
            PublicKey = gI0o+VaIUDxe9mFyMFGKE+ExEzKldwzUYoE1m8JUSy0=
            Endpoint = 127.0.0.1:51820
            AllowedIPs = 0.0.0.0/0
        """;
        createFile(filePath, content);

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readClientConfig(testNetworkName, configName);
        });

        assertTrue(exception.getMessage().contains("missing required fields"));
    }

    @Test
    public void testReadClientConfigThrowsExceptionOnMissingDNS() throws IOException {
        String configName = "missingDns";
        String configFileName = String.format("%s.conf", configName);
        String filePath = String.format("%s%s/clients/%s", BASE_PATH, testNetworkName, configFileName);

        String content = """
            [Interface]
            PrivateKey = qG+6d5CnIwEM6PjPaX0boyls1bu6SM+TEw+dDUOoHGs=
            Address = 10.100.0.3/24
            [Peer]
            PublicKey = gI0o+VaIUDxe9mFyMFGKE+ExEzKldwzUYoE1m8JUSy0=
            Endpoint = 127.0.0.1:51820
            AllowedIPs = 0.0.0.0/0
        """;
        createFile(filePath, content);

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readClientConfig(testNetworkName, configName);
        });

        assertTrue(exception.getMessage().contains("missing required fields"));
    }

    @Test
    public void testReadClientConfigThrowsExceptionOnMissingPublicKey() throws IOException {
        String configName = "missingPublicKey";
        String configFileName = String.format("%s.conf", configName);
        String filePath = String.format("%s%s/clients/%s", BASE_PATH, testNetworkName, configFileName);

        String content = """
            [Interface]
            PrivateKey = qG+6d5CnIwEM6PjPaX0boyls1bu6SM+TEw+dDUOoHGs=
            Address = 10.100.0.3/24
            DNS = 1.1.1.1
            [Peer]
            Endpoint = 127.0.0.1:51820
            AllowedIPs = 0.0.0.0/0
        """;
        createFile(filePath, content);

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readClientConfig(testNetworkName, configName);
        });

        assertTrue(exception.getMessage().contains("missing peer fields"));
    }

}


