package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for rendering a client's configuration as a QR code.
 *
 * there is no matching response type: this operation returns a PNG rather than JSON, which is the
 * documented exception to the request/response convention.
 */
@Data
public class GetNetworkClientConfigFileQRRequest {

    @NotBlank(message = "Network name is required")
    @Schema(description = "name of the network the client belongs to")
    private String networkName;

    @NotBlank(message = "Client name is required")
    @Schema(description = "name of the client whose config to render")
    private String clientName;
}
