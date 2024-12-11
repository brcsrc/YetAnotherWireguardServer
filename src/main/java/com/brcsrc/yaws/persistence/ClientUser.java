package com.brcsrc.yaws.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class ClientUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientUserId;

    @ManyToOne
    @JoinColumn(name = "user_name", referencedColumnName = "userName")
    private User user;

    @ManyToOne
    @JoinColumn(name = "client_name", referencedColumnName = "clientName")
    private Client client;
}

