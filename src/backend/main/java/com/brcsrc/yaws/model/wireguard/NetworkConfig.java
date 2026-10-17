package com.brcsrc.yaws.model.wireguard;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * a network (server side) config file: the interface plus every peer on it.
 */
@Data
public class NetworkConfig {

    private NetworkInterface networkInterface;

    // owned by this object so peers can be added and removed in place. never reassigned, so a
    // caller cannot swap the list out from under a config being edited
    private final List<NetworkPeer> peers = new ArrayList<>();

    public NetworkConfig(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public NetworkConfig(NetworkInterface networkInterface, List<NetworkPeer> peers) {
        this.networkInterface = networkInterface;
        if (peers != null) {
            this.peers.addAll(peers);
        }
    }

    public void addPeer(NetworkPeer peer) {
        this.peers.add(peer);
    }

    /**
     * removes any peer matching the given /32 allowedIps. returns true when a peer was removed.
     */
    public boolean removePeerByAllowedIps(String allowedIps) {
        return this.peers.removeIf(peer -> peer.getAllowedIps().equals(allowedIps));
    }
}
