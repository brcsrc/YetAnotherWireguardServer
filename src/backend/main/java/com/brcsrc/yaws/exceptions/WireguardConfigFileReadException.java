package com.brcsrc.yaws.exceptions;

/**
 * used for any failure to read a wireguard server or client configuration file
 */
public class WireguardConfigFileReadException extends RuntimeException {
    public WireguardConfigFileReadException(String message) {
        super(message);
    }
}
