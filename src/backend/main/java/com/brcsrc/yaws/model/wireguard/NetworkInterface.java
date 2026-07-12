package com.brcsrc.yaws.model.wireguard;

public class NetworkInterface {
    public String address;
    public int listenPort;
    public String privateKey;

    public NetworkInterface(String address, int listenPort, String privateKey) {
        this.address = address;
        this.listenPort = listenPort;
        this.privateKey = privateKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
