package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Client;
import java.util.List;

/**
 * A response object for listing network clients with pagination.
 */
public class ListNetworkClientsResponse {

    private List<Client> clients;
    private Integer nextPage;

    public ListNetworkClientsResponse() {
    }

    public ListNetworkClientsResponse(List<Client> clients, Integer nextPage) {
        this.clients = clients;
        this.nextPage = nextPage;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public Integer getNextPage() {
        return nextPage;
    }

    public void setNextPage(Integer nextPage) {
        this.nextPage = nextPage;
    }

    @Override
    public String toString() {
        return "ListNetworkClientsResponse{" +
                "clients=" + clients +
                ", nextPage=" + nextPage +
                '}';
    }
}