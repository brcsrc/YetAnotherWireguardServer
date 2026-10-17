package com.brcsrc.yaws.model.requests;

import lombok.Data;

/**
 * A request object for streaming client WireGuard connection information.
 */
@Data
public class StreamClientConnectionInfoRequest {

    private String clientPublicKeyValue;
}
