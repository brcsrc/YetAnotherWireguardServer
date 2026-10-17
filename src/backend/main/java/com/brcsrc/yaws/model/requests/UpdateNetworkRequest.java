package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.NetworkStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * A request object for updating a network.
 *
 * every field other than networkName is optional. a null field is left unchanged, which is how a
 * caller updates one attribute without having to restate the others.
 */
@Data
public class UpdateNetworkRequest {

    @NotNull
    @Schema(description = "name of the network to update")
    private String networkName;

    @Schema(description = "optional free form tag for the network")
    private String networkTag;

    @Schema(description = "ACTIVE brings the interface up, INACTIVE brings it down")
    private NetworkStatus networkStatus;

    // boxed so an omitted field can be distinguished from an explicit false
    @Schema(description = "when true, no peer on this network can reach any other peer on it")
    private Boolean peerIsolationEnabled;
}
