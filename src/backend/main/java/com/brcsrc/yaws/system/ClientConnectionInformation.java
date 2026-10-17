package com.brcsrc.yaws.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * live connection state for a single wireguard peer, as reported by 'wg show dump'.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientConnectionInformation {

    private String publicKey;
    private String presharedKey;
    private String endpoint;
    private String allowedIps;
    private Long latestHandshakeEpochSeconds;
    private Long bytesReceived;
    private Long bytesSent;
    private Long persistentKeepalive;
}
