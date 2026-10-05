package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.NetworkClient;

/**
 * A response object for updating a client on a network.
 */
public class UpdateNetworkClientResponse {

    private NetworkClient networkClient;

    public UpdateNetworkClientResponse() {
    }

    public UpdateNetworkClientResponse(NetworkClient networkClient) {
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
        return "UpdateNetworkClientResponse{networkClient=" + networkClient + '}';
    }
}
