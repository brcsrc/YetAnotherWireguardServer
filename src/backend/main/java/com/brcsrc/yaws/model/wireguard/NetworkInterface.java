package com.brcsrc.yaws.model.wireguard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the [Interface] section of a wireguard config.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkInterface {

    private String address;
    private int listenPort;
    private String privateKey;
}
