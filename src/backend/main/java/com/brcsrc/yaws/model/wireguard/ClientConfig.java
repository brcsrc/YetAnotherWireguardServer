package com.brcsrc.yaws.model.wireguard;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * a client (peer side) config file: its own interface plus the server it peers with.
 */
@Data
@AllArgsConstructor
public class ClientConfig {

    private final NetworkInterface networkInterface;
    private final PeerConfig peerConfig;
    private final String dns;

    /**
     * PersistentKeepalive in seconds, or null to omit the directive.
     *
     * this belongs on the client side rather than the server's peer entry. a client whose NAT
     * mapping has expired cannot be reached by anything the server sends, so only traffic the
     * client originates keeps the session alive. see issue #69
     */
    private final Integer persistentKeepalive;

    public ClientConfig(NetworkInterface networkInterface, PeerConfig peerConfig, String dns) {
        this(networkInterface, peerConfig, dns, null);
    }
}
