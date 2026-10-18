package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for deleting a network, carrying the network as it was when removed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteNetworkResponse {

    private Network network;
}
