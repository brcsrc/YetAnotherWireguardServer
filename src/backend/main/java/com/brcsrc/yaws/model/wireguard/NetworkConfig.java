package com.brcsrc.yaws.model.wireguard;

import java.util.ArrayList;
import java.util.List;

public class NetworkConfig {

    public NetworkInterface networkInterface;
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

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public List<NetworkPeer> getPeers() {
        return peers;
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
