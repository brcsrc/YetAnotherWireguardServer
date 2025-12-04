package com.brcsrc.yaws.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client {

    // not adding validations here for now since this is used behind CreateNetworkClientRequest

    @Id
    private String clientName;
    private String clientPrivateKeyName;
    private String clientPublicKeyName;
    private String clientPublicKeyValue;
    private String clientCidr;
    private String clientDns;
    private String allowedIps;
    private String networkPublicKeyName;
    private String networkEndpoint;
    private int networkListenPort;
    private String clientTag;

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

    public String getClientDns() {
        return clientDns;
    }

    public void setClientDns(String clientDns) {
        this.clientDns = clientDns;
    }

    public String getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(String allowedIps) {
        this.allowedIps = allowedIps;
    }

    public String getNetworkPublicKeyName() {
        return networkPublicKeyName;
    }

    public void setNetworkPublicKeyName(String networkPublicKeyName) {
        this.networkPublicKeyName = networkPublicKeyName;
    }

    public String getClientPublicKeyName() {
        return clientPublicKeyName;
    }

    public void setClientPublicKeyName(String clientPublicKeyName) {
        this.clientPublicKeyName = clientPublicKeyName;
    }

    public String getClientPublicKeyValue() {
        return clientPublicKeyValue;
    }

    public void setClientPublicKeyValue(String clientPublicKeyValue) {
        this.clientPublicKeyValue = clientPublicKeyValue;
    }

    public String getNetworkEndpoint() {
        return networkEndpoint;
    }

    public void setNetworkEndpoint(String networkEndpoint) {
        this.networkEndpoint = networkEndpoint;
    }

    public int getNetworkListenPort() {
        return networkListenPort;
    }

    public void setNetworkListenPort(int networkListenPort) {
        this.networkListenPort = networkListenPort;
    }

    public String getClientTag() {
        return clientTag;
    }

    public void setClientTag(String clientTag) {
        this.clientTag = clientTag;
    }

    @Override
    public String toString() {
        return "Client{" +
                "clientName='" + clientName + '\'' +
                ", clientPrivateKeyName='" + clientPrivateKeyName + '\'' +
                ", clientPublicKeyName='" + clientPublicKeyName + '\'' +
                ", clientPublicKeyValue='" + clientPublicKeyValue + '\'' +
                ", clientCidr='" + clientCidr + '\'' +
                ", clientDns='" + clientDns + '\'' +
                ", allowedIps='" + allowedIps + '\'' +
                ", networkPublicKeyName='" + networkPublicKeyName + '\'' +
                ", networkEndpoint='" + networkEndpoint + '\'' +
                ", networkListenPort='" + networkListenPort + '\'' +
                ", clientTag='" + clientTag + '\'' +
                '}';
    }
}
