package com.brcsrc.yaws.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_users")
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

    public Long getClientUserId() {
        return clientUserId;
    }

    public void setClientUserId(Long clientUserId) {
        this.clientUserId = clientUserId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public String toString() {
        return "ClientUser{" +
                "clientUserId=" + clientUserId +
                ", user=" + user +
                ", client=" + client +
                '}';
    }
}

