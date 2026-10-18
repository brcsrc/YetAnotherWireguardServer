package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for finding the next free client address on a network.
 */
@Data
public class GetNextAvailableClientAddressRequest {

    @NotBlank(message = "Network name is required")
    @Schema(description = "name of the network to find a free address on")
    private String networkName;
}
