package com.brcsrc.yaws.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;


@Entity
@Table(name = "network_clients")
public class NetworkClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long networkClientId;

    @ManyToOne
    @JoinColumn(name = "network_name", referencedColumnName = "networkName")
    private Network network;

    // using CascadeType.REMOVE allows JPA to remove the client when the relation is deleted from
    // the network_clients table, which is desired since a client should only exist within a network and
    // not be detached by itself.
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "client_name", referencedColumnName = "clientName")
    private Client client;

    public Long getNetworkClientId() {
        return networkClientId;
    }

    public void setNetworkClientId(Long networkClientId) {
        this.networkClientId = networkClientId;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public String toString() {
        return "NetworkClient{" +
                "networkClientId=" + networkClientId +
                ", network=" + network +
                ", client=" + client +
                '}';
    }
}

