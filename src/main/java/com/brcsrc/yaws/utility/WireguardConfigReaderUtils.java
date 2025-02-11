package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.exceptions.WireguardConfigFileReadException;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import com.brcsrc.yaws.model.wireguard.NetworkInterface;

import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;

public class WireguardConfigReaderUtils {
    public static NetworkConfig readNetworkConfig(String configFileName) {
        // parameter 'configFileName' must be 64 alphanumeric chars with no spaces and end in .conf
        // it must also prevent directory traversal
        if (!configFileName.matches("^[a-zA-Z0-9]{1,64}\\.conf$")) {
            throw new WireguardConfigFileReadException("file does not match expected naming pattern");
        }
        // we only want to read from this path and not provide any others
        String baseWireguardPath = "/etc/wireguard/";

        String absFilePath = String.format("%s%s", baseWireguardPath, configFileName);
        Path filePath = Paths.get(absFilePath);

        if (!Files.exists(filePath)) {
            throw new WireguardConfigFileReadException(String.format("file '%s' does not exist", absFilePath));
        }
        if (!Files.isRegularFile(filePath)) {
            throw new WireguardConfigFileReadException(String.format("file '%s' is not a regular file", absFilePath));
        }
        if (Files.isExecutable(filePath)) {
            throw new WireguardConfigFileReadException(String.format("file '%s' is marked executable, not proceeding", absFilePath));
        }
        if (!Files.isReadable(filePath)) {
            throw new WireguardConfigFileReadException(String.format("file '%s' is not readable", absFilePath));
        }

        boolean hasInterface = false;
        String address = null;
        int listenPort = -1;
        String privateKey = null;

        try (Scanner scanner = new Scanner(Files.newInputStream(filePath))) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine().trim();

                // parse values when expected format is as such
                //    [Interface]
                //    PrivateKey = dummyPrivateKey
                //    Address = 10.0.0.1
                //    ListenPort = 51820

                if (nextLine.startsWith("[Interface]")) {
                    hasInterface = true;
                } else if (hasInterface) {
                    String[] parts = nextLine.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();

                        switch (key) {
                            // valid network config fields
                            case "Address":
                                if (!IPUtils.isValidIpv4Cidr(value)) {
                                    throw new WireguardConfigFileReadException(String.format("invalid Address in file '%s'", absFilePath));
                                }
                                address = value;
                                break;
                            case "ListenPort":
                                try {
                                    listenPort = Integer.parseInt(value);
                                } catch (NumberFormatException e) {
                                    throw new WireguardConfigFileReadException(String.format("invalid ListenPort in file '%s'", absFilePath));
                                }
                                break;
                            case "PrivateKey":
                                // TODO validate key pattern
                                privateKey = value;
                                break;
                            // valid peer fields can be skipped
                            default:
                                break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new WireguardConfigFileReadException(String.format("unexpected IOException in reading file '%s'", absFilePath));
        }

        if (!hasInterface || address == null || listenPort == -1 || privateKey == null) {
            throw new WireguardConfigFileReadException(String.format("missing required fields in WireGuard config file '%s'", absFilePath));
        }

        return new NetworkConfig(
                new NetworkInterface(
                        address,
                        listenPort,
                        privateKey
                )
        );
    }
}
