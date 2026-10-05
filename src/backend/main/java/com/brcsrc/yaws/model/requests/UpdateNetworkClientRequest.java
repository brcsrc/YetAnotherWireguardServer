package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * A request object for updating a client on a network.
 *
 * networkName and clientName identify the client. every other field is optional and a null field
 * is left unchanged, so a caller can update one attribute without restating the others.
 */
public class UpdateNetworkClientRequest {

    @NotBlank(message = "Network name is required")
    @Schema(description = "name of the network the client belongs to")
    private String networkName;

    @NotBlank(message = "Client name is required")
    @Schema(description = "name of the client to update")
    private String clientName;

    @Schema(description = "optional free form tag for the client")
    private String clientTag;

    // boxed so an omitted field can be distinguished from an explicit false
    @Schema(description = "when true this client can neither reach nor be reached by any other "
            + "peer on the network. its traffic still routes through the server to the internet")
    private Boolean peerIsolationEnabled;

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientTag() {
        return clientTag;
    }

    public void setClientTag(String clientTag) {
        this.clientTag = clientTag;
    }

    public Boolean getPeerIsolationEnabled() {
        return peerIsolationEnabled;
    }

    public void setPeerIsolationEnabled(Boolean peerIsolationEnabled) {
        this.peerIsolationEnabled = peerIsolationEnabled;
    }

    @Override
    public String toString() {
        return "UpdateNetworkClientRequest{"
                + "networkName='" + networkName + '\''
                + ", clientName='" + clientName + '\''
                + ", clientTag='" + clientTag + '\''
                + ", peerIsolationEnabled=" + peerIsolationEnabled
                + '}';
    }
}
