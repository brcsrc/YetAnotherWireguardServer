package com.brcsrc.yaws.model.requests;

import java.util.List;

import com.brcsrc.yaws.model.Network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for listing networks with pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListNetworksResponse {

    private List<Network> networks;
    private Integer nextPage;
}
