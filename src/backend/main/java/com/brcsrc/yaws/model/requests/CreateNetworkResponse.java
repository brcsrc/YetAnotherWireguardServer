package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Network;

/**
 * A response object for creating a network.
 */
public class CreateNetworkResponse {

    private Network network;

    public CreateNetworkResponse() {
    }

    public CreateNetworkResponse(Network network) {
        this.network = network;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public String toString() {
        return "CreateNetworkResponse{network=" + network + '}';
    }
}
