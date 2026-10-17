package com.brcsrc.yaws.model.requests;

import lombok.Data;

/**
 * A request object for streaming network WireGuard connection information.
 */
@Data
public class StreamNetworkConnectionInfoRequest {

    private String networkPublicKeyValue;
}
