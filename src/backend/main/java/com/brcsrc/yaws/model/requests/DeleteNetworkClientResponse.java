package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.NetworkClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for deleting a client, carrying the client as it was when removed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteNetworkClientResponse {

    private NetworkClient networkClient;
}
