package com.brcsrc.yaws.system;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

/**
 * live connection state for one wireguard interface and the peers attached to it.
 *
 * the peers map is concurrent because it is populated by the polling thread that refreshes
 * connection data while being read by request threads serving the SSE endpoints.
 */
@Data
public class NetworkConnectionInformation {

    private String interfaceName;
    private String publicKey;
    private int listeningPort;
    private Map<String, ClientConnectionInformation> peers = new ConcurrentHashMap<>();

    public NetworkConnectionInformation() {
    }

    public NetworkConnectionInformation(String interfaceName, String publicKey, int listeningPort) {
        this.interfaceName = interfaceName;
        this.publicKey = publicKey;
        this.listeningPort = listeningPort;
    }

    public void addPeer(String publicKey, ClientConnectionInformation peerInfo) {
        this.peers.put(publicKey, peerInfo);
    }
}
