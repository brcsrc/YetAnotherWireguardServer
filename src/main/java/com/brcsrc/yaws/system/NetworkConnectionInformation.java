package com.brcsrc.yaws.system;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class NetworkConnectionInformation {
    private String interfaceName;
    private String publicKey;
    private int listeningPort;
    private Map<String, ClientConnectionInformation> peers;

    public NetworkConnectionInformation() {
        this.peers = new ConcurrentHashMap<>();
    }

    public NetworkConnectionInformation(String interfaceName, String publicKey, int listeningPort) {
        this.interfaceName = interfaceName;
        this.publicKey = publicKey;
        this.listeningPort = listeningPort;
        this.peers = new ConcurrentHashMap<>();
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public void setListeningPort(int listeningPort) {
        this.listeningPort = listeningPort;
    }

    public Map<String, ClientConnectionInformation> getPeers() {
        return peers;
    }

    public void setPeers(Map<String, ClientConnectionInformation> peers) {
        this.peers = peers;
    }

    public void addPeer(String publicKey, ClientConnectionInformation peerInfo) {
        this.peers.put(publicKey, peerInfo);
    }

    @Override
    public String toString() {
        return "NetworkConnectionInformation{" +
                "interfaceName='" + interfaceName + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", listeningPort=" + listeningPort +
                ", peers=" + peers +
                '}';
    }
}
