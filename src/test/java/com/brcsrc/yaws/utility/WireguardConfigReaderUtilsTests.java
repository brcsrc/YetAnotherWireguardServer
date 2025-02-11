package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.exceptions.WireguardConfigFileReadException;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import static org.junit.jupiter.api.Assertions.*;

public class WireguardConfigReaderUtilsTests {

    private static final Logger logger = LoggerFactory.getLogger(WireguardConfigReaderUtilsTests.class);

    private static final String BASE_PATH = "/etc/wireguard/";

    @BeforeEach
    public void setUp() {
        File dir = new File(BASE_PATH);
        if (!dir.exists()) {
            logger.info(String.format("making wireguard directory for tests, '%s'", BASE_PATH));
            dir.mkdirs();
        }
    }

    private void createFile(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void testValidFileName() throws IOException {
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
    public void testInvalidFileName() {
        String invalidFileName = "invalid config!.conf"; // Invalid due to spaces and special characters

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(invalidFileName);
        });
        assertEquals("file does not match expected naming pattern", exception.getMessage());
    }

    @Test
    public void testFileDoesNotExist() {
        String nonExistentFileName = "nonExistent.conf";

        WireguardConfigFileReadException exception = assertThrows(WireguardConfigFileReadException.class, () -> {
            WireguardConfigReaderUtils.readNetworkConfig(nonExistentFileName);
        });
        assertTrue(exception.getMessage().contains("file '/etc/wireguard/nonExistent.conf' does not exist"));
    }

    @Test
    public void testFileIsNotRegularFile() throws IOException {
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
    public void testFileIsExecutable() throws IOException {
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
    public void testMissingPrivateKey() throws IOException {
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
    public void testMissingAddress() throws IOException {
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
    public void testMissingListenPort() throws IOException {
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
    public void testValidConfigFile() throws IOException {
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
}


