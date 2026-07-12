package com.brcsrc.yaws.model.wireguard;

public class PeerConfig {
    private final String publicKey;
    private final String endpoint;
    private final String allowedIps;

    public PeerConfig(String publicKey, String endpoint, String allowedIps) {
        this.publicKey = publicKey;
        this.endpoint = endpoint;
        this.allowedIps = allowedIps;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getAllowedIps() {
        return allowedIps;
    }
}
