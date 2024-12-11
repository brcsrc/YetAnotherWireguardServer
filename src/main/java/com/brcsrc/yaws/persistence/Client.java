package com.brcsrc.yaws.persistence;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Client {

    @Id
    private String clientName;

    private String clientPrivateKeyName;
    private String clientCidr;
}
