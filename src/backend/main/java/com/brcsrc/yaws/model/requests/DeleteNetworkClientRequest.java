package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for deleting a client from a network.
 */
@Data
public class DeleteNetworkClientRequest {

    @NotBlank(message = "Network name is required")
    @Schema(description = "name of the network the client belongs to")
    private String networkName;

    @NotBlank(message = "Client name is required")
    @Schema(description = "name of the client to delete")
    private String clientName;
}
