package com.brcsrc.yaws.model.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateNetworkClientRequest {

    private final String validIpv4AddressRegex = "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$";
    private final String validIpv4CidrRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(3[0-2]|[1-2][0-9]|[0-9]))$";

    @NotBlank(message = "Client name is required")
    private String clientName;

    @Pattern(regexp = validIpv4CidrRegex)
    private String clientCidr;

    @Pattern(regexp = validIpv4AddressRegex)
    private String clientDns;

    // TODO add custom validator
    private String allowedIps;

    @NotBlank(message = "Network name is required")
    private String networkName;

    @Pattern(regexp = validIpv4AddressRegex)
    private String networkEndpoint;

    private String clientTag;



    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
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

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getNetworkEndpoint() {
        return networkEndpoint;
    }

    public void setNetworkEndpoint(String networkEndpoint) {
        this.networkEndpoint = networkEndpoint;
    }

    public String getClientTag() {
        return clientTag;
    }

    public void setClientTag(String clientTag) {
        this.clientTag = clientTag;
    }

    @Override
    public String toString() {
        return "CreateNetworkClientRequest{" +
                "clientName='" + clientName + '\'' +
                ", clientCidr='" + clientCidr + '\'' +
                ", clientDns='" + clientDns + '\'' +
                ", allowedIps='" + allowedIps + '\'' +
                ", networkName='" + networkName + '\'' +
                ", networkEndpoint='" + networkEndpoint + '\'' +
                ", clientTag='" + clientTag + '\'' +
                '}';
    }
}


