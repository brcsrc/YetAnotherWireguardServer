package com.brcsrc.yaws.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PeerIsolationUtilsTests {

    // iptables caps chain names at this length
    private static final int IPTABLES_MAX_CHAIN_NAME_LENGTH = 28;
    // network names are capped at 15 characters by IFNAMSIZ, enforced by
    // Constants.CHAR_15_ALPHANUMERIC_DASHES_UNDERSC_REGEXP
    private static final int MAX_NETWORK_NAME_LENGTH = 15;

    @Test
    void testGetIsolationChainNameUsesFixedPrefix() {
        assertEquals("YAWS-ISO-Network1", PeerIsolationUtils.getIsolationChainName("Network1"));
    }

    @Test
    void testIsolationChainNameFitsIptablesLimitAtMaxNetworkNameLength() {
        String maxLengthNetworkName = "a".repeat(MAX_NETWORK_NAME_LENGTH);
        String chainName = PeerIsolationUtils.getIsolationChainName(maxLengthNetworkName);

        assertTrue(
                chainName.length() <= IPTABLES_MAX_CHAIN_NAME_LENGTH,
                String.format("chain name '%s' is %s chars, over the %s limit",
                        chainName, chainName.length(), IPTABLES_MAX_CHAIN_NAME_LENGTH));
    }
}
