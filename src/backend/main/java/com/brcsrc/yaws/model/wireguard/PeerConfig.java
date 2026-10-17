package com.brcsrc.yaws.model.wireguard;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * the [Peer] section of a client config, describing the server the client connects to.
 */
@Data
@AllArgsConstructor
public class PeerConfig {

    private final String publicKey;
    private final String endpoint;
    private final String allowedIps;
}
