package com.brcsrc.yaws.utility;


import com.brcsrc.yaws.model.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;


/**
 * used to determine the absolute path of files on the system.
 * if a change to where keys and configs need to be changed, we can
 * control that here and not in the service layer
 */
public class FilepathUtils {
    public static String getNetworkConfigPath(String networkName) {
        return String.format(
                "%s/%s.conf",
                Constants.BASE_WIREGUARD_DIR,
                networkName
        );
    }
    public static String getNetworkDirectoryPath(String networkName) {
        return String.format(
                "%s/%s/",
                Constants.BASE_WIREGUARD_DIR,
                networkName
        );
    }
    public static String getNetworkKeysDirectoryPath(String networkName) {
        return String.format(
                "%s/%s/keys/",
                Constants.BASE_WIREGUARD_DIR,
                networkName
        );
    }
    public static String getNetworkClientsDirectoryPath(String networkName) {
        return String.format(
                "%s/%s/clients/",
                Constants.BASE_WIREGUARD_DIR,
                networkName
        );
    }
    public static String getNetworkKeyPath(String networkName, String networkKeyName) {
        return String.format(
                "%s/%s/keys/%s",
                Constants.BASE_WIREGUARD_DIR,
                networkName,
                networkKeyName
        );
    }
    public static String getClientConfigPath(String networkName, String clientName) {
        return String.format(
                "%s/%s/clients/%s.conf",
                Constants.BASE_WIREGUARD_DIR,
                networkName,
                clientName
        );
    }
    public static String getClientKeyPath(String networkName, String clientKeyName) {
        return String.format(
                "%s/%s/keys/%s",
                Constants.BASE_WIREGUARD_DIR,
                networkName,
                clientKeyName
        );
    }
    public static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }
}
