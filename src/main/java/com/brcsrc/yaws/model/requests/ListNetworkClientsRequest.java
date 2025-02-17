package com.brcsrc.yaws.model.requests;

import jakarta.validation.constraints.NotBlank;

public class ListNetworkClientsRequest {

    private String clientName;

    @NotBlank(message = "Network name is required")
    private String networkName;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    @Override
    public String toString() {
        return "ListNetworkClients{" +
                "clientName='" + clientName + '\'' +
                ", networkName='" + networkName + '\'' +
                '}';
    }
}
