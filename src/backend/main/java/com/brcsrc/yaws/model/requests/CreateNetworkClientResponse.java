package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.NetworkClient;

/**
 * A response object for creating a client on a network.
 */
public class CreateNetworkClientResponse {

    private NetworkClient networkClient;

    public CreateNetworkClientResponse() {
    }

    public CreateNetworkClientResponse(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    @Override
    public String toString() {
        return "CreateNetworkClientResponse{networkClient=" + networkClient + '}';
    }
}
