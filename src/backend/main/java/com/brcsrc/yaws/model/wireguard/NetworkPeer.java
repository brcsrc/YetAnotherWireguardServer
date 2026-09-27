package com.brcsrc.yaws.model.wireguard;

/**
 * a [Peer] entry in a network (server side) config file. the allowedIps of a peer is the
 * single address the client is permitted to send from, always a /32. wireguard enforces this
 * on decryption so a peer cannot source traffic from any other address.
 */
public class NetworkPeer {
    private final String publicKey;
    private final String allowedIps;

    public NetworkPeer(String publicKey, String allowedIps) {
        this.publicKey = publicKey;
        this.allowedIps = allowedIps;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getAllowedIps() {
        return allowedIps;
    }
}
