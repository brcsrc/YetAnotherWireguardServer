package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Network;
import java.util.List;

/**
 * A response object for listing networks with pagination.
 */
public class ListNetworksResponse {

    private List<Network> networks;
    private Integer nextPage;

    public ListNetworksResponse() {
    }

    public ListNetworksResponse(List<Network> networks, Integer nextPage) {
        this.networks = networks;
        this.nextPage = nextPage;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public void setNetworks(List<Network> networks) {
        this.networks = networks;
    }

    public Integer getNextPage() {
        return nextPage;
    }

    public void setNextPage(Integer nextPage) {
        this.nextPage = nextPage;
    }

    @Override
    public String toString() {
        return "ListNetworksResponse{" +
                "networks=" + networks +
                ", nextPage=" + nextPage +
                '}';
    }
}