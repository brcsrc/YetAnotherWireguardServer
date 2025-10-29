package com.brcsrc.yaws.model.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ListNetworkClientsRequest {

    @NotBlank(message = "Network name is required")
    private String networkName;

    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page;

    @Min(value = 1, message = "Max items must be at least 1")
    @Max(value = 10, message = "Max items must be no more than 10")
    private Integer maxItems = 10; // Default to 10 items per page

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

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
        return "ListNetworkClientsRequest{" +
                "networkName='" + networkName + '\'' +
                ", page=" + page +
                ", maxItems=" + maxItems +
                '}';
    }
}
