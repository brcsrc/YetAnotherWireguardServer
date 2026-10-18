package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for describing a network.
 */
@Data
public class DescribeNetworkRequest {

    @NotBlank(message = "Network name is required")
    @Schema(description = "name of the network to describe")
    private String networkName;
}
