package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.NetworkClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for Create a client on a network.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNetworkClientResponse {

    private NetworkClient networkClient;
}
