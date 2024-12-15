package com.brcsrc.yaws.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@Entity
@Table(name = "networks")
public class Network {

    @Id
    @NotNull
    @Schema(description = "unique, alphanumeric name for the network")
    private String networkName;
    @NotNull
    @Schema(description = "CIDR block for the network")
    private String networkCidr;
    @NotNull
    @Schema(description = "server listen port for the network")
    private String networkListenPort;

    @Null
    private String networkPrivateKeyName;
    @Null
    private String networkPublicKeyName;
    @Null
    private String networkTag;

    // internal use only
    @Enumerated(EnumType.STRING)
    private NetworkStatus networkStatus;


    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getNetworkListenPort() {
        return networkListenPort;
    }

    public void setNetworkListenPort(String networkListenPort) {
        this.networkListenPort = networkListenPort;
    }

    public String getNetworkPrivateKeyName() {
        return networkPrivateKeyName;
    }

    public void setNetworkPrivateKeyName(String networkPrivateKeyName) {
        this.networkPrivateKeyName = networkPrivateKeyName;
    }

    public String getNetworkPublicKeyName() {
        return networkPublicKeyName;
    }

    public void setNetworkPublicKeyName(String networkPublicKeyName) {
        this.networkPublicKeyName = networkPublicKeyName;
    }

    public String getNetworkTag() {
        return networkTag;
    }

    public void setNetworkTag(String networkTag) {
        this.networkTag = networkTag;
    }

    public NetworkStatus getNetworkStatus() {
        return networkStatus;
    }

    public void setNetworkStatus(NetworkStatus networkStatus) {
        this.networkStatus = networkStatus;
    }

    @Override
    public String toString() {
        return "Network{" +
                "networkName='" + networkName + '\'' +
                ", networkCidr='" + networkCidr + '\'' +
                ", networkListenPort='" + networkListenPort + '\'' +
                ", networkPrivateKeyName='" + networkPrivateKeyName + '\'' +
                ", networkPublicKeyName='" + networkPublicKeyName + '\'' +
                ", networkTag='" + networkTag + '\'' +
                ", networkStatus=" + networkStatus +
                '}';
    }
}

