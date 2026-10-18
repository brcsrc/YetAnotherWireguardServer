package com.brcsrc.yaws.model.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object carrying the server's public IP.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetPublicIpResponse {

    private String publicIp;
}
