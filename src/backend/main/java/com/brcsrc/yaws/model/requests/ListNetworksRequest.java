package com.brcsrc.yaws.model.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * A request object for listing networks with pagination.
 */
@Data
public class ListNetworksRequest {

    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page;

    @Min(value = 1, message = "Max items must be at least 1")
    @Max(value = 10, message = "Max items must be no more than 10")
    private Integer maxItems = 10; // Default to 10 items per page
}
