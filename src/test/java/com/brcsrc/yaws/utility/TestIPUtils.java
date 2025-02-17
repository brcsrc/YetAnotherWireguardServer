package com.brcsrc.yaws.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestIPUtils {

    @Test
    public void testIsValidIpv4CidrReturnsTrueOnValidIpv4Cidr() {
        String networkCidr = "10.0.0.1/24";
        boolean isValid = IPUtils.isValidIpv4Cidr(networkCidr);
        assertTrue(isValid);
    }

    @Test
    public void testIsValidIpv4CidrReturnsFalseOnInvalidIpv4Cidr() {
        String[] badCidrs = new String[]{
                "1000.0.0.1/32",    // more than 3 chars in one octet
                "900.0.0.1/24",     // single octet is over 255
                "10.0.0.1/33",      // subnet mask is invalid
                "10.0.0.1",         // subnet mask is missing
                "10.0.0/24",        // missing an octet
        };
        for (String c : badCidrs) {
            boolean isValid = IPUtils.isValidIpv4Cidr(c);
            assertFalse(isValid);
        }
    }

    @Test
    public void testIsValidIpv4AddressReturnsTrueOnValidIpv4Address() {
        String networkCidr = "10.0.0.1";
        boolean isValid = IPUtils.isValidIpv4Address(networkCidr);
        assertTrue(isValid);
    }

    @Test
    public void testIsValidIpv4AddressReturnsTrueOnValidIpv4Cidr() {
        String[] badAddresses = new String[]{
                "1000.0.0.1",
                "256.1.1.1",
                "19Z.168.0.1"
        };
        for (String a : badAddresses) {
            boolean isValid = IPUtils.isValidIpv4Address(a);
            assertFalse(isValid);
        }
    }

    @Test
    public void testIsValidEndpointReturnsTrueOnValidEndpoint() {
        String validEndpoint = "123.12.1.40:51820";
        boolean isValid = IPUtils.isValidEndpoint(validEndpoint);
        assertTrue(isValid);
    }

    @Test
    public void testIsValidEndpointReturnsFalseOnInvalidEndpoint() {
        String[] badEndpoints = new String[]{
            "100.300.33.4:51820",   // bad ip
            "10.0.0.1:66000",       // port too high
            "10.0.0.1:0",           // port too low
            "123.45.79.1-51820",    // bad notation
        };
        for (String e : badEndpoints) {
            boolean isValid = IPUtils.isValidEndpoint(e);
            assertFalse(isValid);
        }
    }

    @Test
    public void testNetworkMemberIsInNetworkRange() {
        String networkCidr = "10.0.0.1/24";
        String networkMemberCidr = "10.0.0.2/24";
        boolean inRange = IPUtils.isNetworkMemberInNetworkRange(networkCidr, networkMemberCidr);
        assertTrue(inRange);
    }

    @Test
    public void testNetworkMemberIsOutOfRangeWithSameHostAddress() {
        String networkCidr = "10.0.0.1/24";
        String networkMemberCidr = "10.0.0.1/24";
        boolean inRange = IPUtils.isNetworkMemberInNetworkRange(networkCidr, networkMemberCidr);
        assertFalse(inRange);
    }

    @Test
    public void testNetworkMemberIsOutOfRangeWithBroadcastAddress() {
        String networkCidr = "10.0.0.1/24";
        String networkMemberCidr = "10.0.0.255/24";
        boolean inRange = IPUtils.isNetworkMemberInNetworkRange(networkCidr, networkMemberCidr);
        assertFalse(inRange);
    }

    @Test
    public void testNetworkMemberIsOutOfRangeWithDifferingNetworkAddress() {
        String networkCidr = "10.0.0.1/24";
        String networkMemberCidr = "10.10.0.1/24";
        boolean inRange = IPUtils.isNetworkMemberInNetworkRange(networkCidr, networkMemberCidr);
        assertFalse(inRange);
    }

    @Test
    public void testNetworkMemberIsOutOfRangeWithBroaderSubnetMask() {
        String networkCidr = "10.0.0.1/24";
        String networkMemberCidr = "10.0.0.2/25";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            IPUtils.isNetworkMemberInNetworkRange(networkCidr, networkMemberCidr);
        });
        assertTrue(exception.getMessage().contains("client subnet mask /25 is outside of network subnet mask /24"));

    }
}
