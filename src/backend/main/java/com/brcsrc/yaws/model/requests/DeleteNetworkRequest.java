package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for deleting a network.
 */
@Data
public class DeleteNetworkRequest {

    @NotBlank(message = "Network name is required")
    @Schema(description = "name of the network to delete")
    private String networkName;
}
