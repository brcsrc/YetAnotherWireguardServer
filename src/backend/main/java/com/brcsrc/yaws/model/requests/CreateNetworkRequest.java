package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Constants;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A request object for creating a network.
 */
public class CreateNetworkRequest {

    @NotNull
    @Pattern(regexp = Constants.CHAR_15_ALPHANUMERIC_DASHES_UNDERSC_REGEXP)
    @Schema(description = "unique, alphanumeric, 4-15 character name for the network")
    private String networkName;

    @NotNull
    @Pattern(regexp = Constants.IPV4_CIDR_REGEXP)
    @Schema(description = "CIDR block for the network")
    private String networkCidr;

    @NotNull
    @Min(1025)
    @Max(65535)
    @Schema(description = "server listen port for the network")
    private int networkListenPort;

    @Schema(description = "optional free form tag for the network")
    private String networkTag;

    @Schema(description = "when true, no peer on this network can reach any other peer on it")
    private boolean peerIsolationEnabled;

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

    public int getNetworkListenPort() {
        return networkListenPort;
    }

    public void setNetworkListenPort(int networkListenPort) {
        this.networkListenPort = networkListenPort;
    }

    public String getNetworkTag() {
        return networkTag;
    }

    public void setNetworkTag(String networkTag) {
        this.networkTag = networkTag;
    }

    public boolean isPeerIsolationEnabled() {
        return peerIsolationEnabled;
    }

    public void setPeerIsolationEnabled(boolean peerIsolationEnabled) {
        this.peerIsolationEnabled = peerIsolationEnabled;
    }

    @Override
    public String toString() {
        return "CreateNetworkRequest{"
                + "networkName='" + networkName + '\''
                + ", networkCidr='" + networkCidr + '\''
                + ", networkListenPort=" + networkListenPort
                + ", networkTag='" + networkTag + '\''
                + ", peerIsolationEnabled=" + peerIsolationEnabled
                + '}';
    }
}
