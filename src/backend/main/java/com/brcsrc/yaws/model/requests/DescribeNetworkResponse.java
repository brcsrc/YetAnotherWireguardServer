package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for describing a network.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DescribeNetworkResponse {

    private Network network;
}
