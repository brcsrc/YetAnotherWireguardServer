package com.brcsrc.yaws.model.wireguard;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * a [Peer] entry in a network (server side) config file. the allowedIps of a peer is the
 * single address the client is permitted to send from, always a /32. wireguard enforces this
 * on decryption so a peer cannot source traffic from any other address.
 */
@Data
@AllArgsConstructor
public class NetworkPeer {

    private final String publicKey;
    private final String allowedIps;
}
