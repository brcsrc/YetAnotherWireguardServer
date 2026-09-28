package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Network;

/**
 * A response object for updating a network.
 */
public class UpdateNetworkResponse {

    private Network network;

    public UpdateNetworkResponse() {
    }

    public UpdateNetworkResponse(Network network) {
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
        return "UpdateNetworkResponse{network=" + network + '}';
    }
}
