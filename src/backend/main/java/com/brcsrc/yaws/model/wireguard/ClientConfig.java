package com.brcsrc.yaws.model.wireguard;

public class ClientConfig {
    private final NetworkInterface networkInterface;
    private final PeerConfig peerConfig;
    private final String dns;

    public ClientConfig(NetworkInterface networkInterface, PeerConfig peerConfig, String dns) {
        this.networkInterface = networkInterface;
        this.peerConfig = peerConfig;
        this.dns = dns;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public PeerConfig getPeerConfig() {
        return peerConfig;
    }

    public String getDns() {
        return dns;
    }
}


