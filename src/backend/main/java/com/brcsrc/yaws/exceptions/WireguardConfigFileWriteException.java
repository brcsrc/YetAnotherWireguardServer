package com.brcsrc.yaws.exceptions;

/**
 * used for any failure to write a wireguard server or client configuration file
 */
public class WireguardConfigFileWriteException extends RuntimeException {
    public WireguardConfigFileWriteException(String message) {
        super(message);
    }
}
