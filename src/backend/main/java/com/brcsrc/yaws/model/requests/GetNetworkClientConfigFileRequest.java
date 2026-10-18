package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for downloading a client's wireguard configuration file.
 *
 * there is no matching response type: this operation returns the .conf file itself rather than
 * JSON, which is the documented exception to the request/response convention.
 */
@Data
public class GetNetworkClientConfigFileRequest {

    @NotBlank(message = "Network name is required")
    @Schema(description = "name of the network the client belongs to")
    private String networkName;

    @NotBlank(message = "Client name is required")
    @Schema(description = "name of the client whose config to download")
    private String clientName;
}
