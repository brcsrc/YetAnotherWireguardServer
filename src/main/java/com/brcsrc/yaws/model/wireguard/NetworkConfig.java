package com.brcsrc.yaws.model.wireguard;

public class NetworkConfig {

    public NetworkInterface networkInterface;
    // TODO add Array<NetworkPeer> for client entries

    public NetworkConfig(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }
}
