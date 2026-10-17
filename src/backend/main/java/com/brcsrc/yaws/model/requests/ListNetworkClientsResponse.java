package com.brcsrc.yaws.model.requests;

import java.util.List;

import com.brcsrc.yaws.model.Client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for listing the clients on a network with pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListNetworkClientsResponse {

    private List<Client> clients;
    private Integer nextPage;
}
