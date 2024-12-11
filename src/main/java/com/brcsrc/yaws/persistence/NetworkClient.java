package com.brcsrc.yaws.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class NetworkClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long networkClientId;

    @ManyToOne
    @JoinColumn(name = "network_name", referencedColumnName = "networkName")
    private Network network;

    @ManyToOne
    @JoinColumn(name = "client_name", referencedColumnName = "clientName")
    private Client client;
}

