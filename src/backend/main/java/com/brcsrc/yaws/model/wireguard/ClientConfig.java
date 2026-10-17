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
}
