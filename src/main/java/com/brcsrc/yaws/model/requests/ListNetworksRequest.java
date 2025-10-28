package com.brcsrc.yaws.model.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * A request object for listing networks with pagination.
 */
public class ListNetworksRequest {

    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page;

    @Min(value = 1, message = "Max items must be at least 1")
    @Max(value = 10, message = "Max items must be no more than 10")
    private Integer maxItems;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    @Override
    public String toString() {
        return "ListNetworksRequest{" +
                "page=" + page +
                ", maxItems=" + maxItems +
                '}';
    }
}