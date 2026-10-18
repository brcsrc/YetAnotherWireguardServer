package com.brcsrc.yaws.model.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object carrying the next free client address on a network.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetNextAvailableClientAddressResponse {

    private String nextAvailableAddress;
}
