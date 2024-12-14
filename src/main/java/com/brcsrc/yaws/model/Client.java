package com.brcsrc.yaws.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    private String clientName;

    private String clientPrivateKeyName;
    private String clientCidr;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPrivateKeyName() {
        return clientPrivateKeyName;
    }

    public void setClientPrivateKeyName(String clientPrivateKeyName) {
        this.clientPrivateKeyName = clientPrivateKeyName;
    }

    public String getClientCidr() {
        return clientCidr;
    }

    public void setClientCidr(String clientCidr) {
        this.clientCidr = clientCidr;
    }

    @Override
    public String toString() {
        return "Client{" +
                "clientName='" + clientName + '\'' +
                ", clientPrivateKeyName='" + clientPrivateKeyName + '\'' +
                ", clientCidr='" + clientCidr + '\'' +
                '}';
    }
}
