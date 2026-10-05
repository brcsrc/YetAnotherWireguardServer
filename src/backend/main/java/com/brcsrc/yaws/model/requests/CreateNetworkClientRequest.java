package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateNetworkClientRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    @Pattern(regexp = Constants.IPV4_CIDR_REGEXP)
    private String clientCidr;

    @Pattern(regexp = Constants.IPV4_ADDRESS_REGEXP)
    private String clientDns;

    // TODO add custom validator as this could be address or cidr
    private String allowedIps;

    @NotBlank(message = "Network name is required")
    private String networkName;

    @Pattern(regexp = Constants.IPV4_ADDRESS_REGEXP)
    private String networkEndpoint;

    private String clientTag;

    @Schema(description = "when true this client can neither reach nor be reached by any other "
            + "peer on the network. its traffic still routes through the server to the internet")
    private boolean peerIsolationEnabled;

    public boolean isPeerIsolationEnabled() {
        return peerIsolationEnabled;
    }

    public void setPeerIsolationEnabled(boolean peerIsolationEnabled) {
        this.peerIsolationEnabled = peerIsolationEnabled;
    }

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
                ", peerIsolationEnabled=" + peerIsolationEnabled +
                '}';
    }
}


