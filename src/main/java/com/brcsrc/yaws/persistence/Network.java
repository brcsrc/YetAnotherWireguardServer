package com.brcsrc.yaws.persistence;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Network {

    @Id
    private String networkName;

    private String networkCidr;
    private String networkListenPort;
    private String networkPrivateKeyName;
    private String networkTag;
}

