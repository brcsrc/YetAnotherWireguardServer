package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for UpdateNetwork a network.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNetworkResponse {

    private Network network;
}
