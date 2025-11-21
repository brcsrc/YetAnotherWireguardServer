package com.brcsrc.yaws.utility;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class IPUtilsTests {

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

    @Test
    public void testGetNextAvailableIpv4AddressReturnsFirstAvailableWhenNoneUnavailable() {
        String networkCidr = "10.100.0.1/24";
        ArrayList<String> unavailableAddresses = new ArrayList<>();

        String result = IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);

        assertEquals("10.100.0.2", result);
    }

    @Test
    public void testGetNextAvailableIpv4AddressSkipsUnavailableAddresses() {
        String networkCidr = "10.100.0.1/24";
        ArrayList<String> unavailableAddresses = new ArrayList<>();
        unavailableAddresses.add("10.100.0.2");
        unavailableAddresses.add("10.100.0.3");
        unavailableAddresses.add("10.100.0.4");

        String result = IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);

        assertEquals("10.100.0.5", result);
    }

    @Test
    public void testGetNextAvailableIpv4AddressHandlesCidrInUnavailableList() {
        String networkCidr = "10.100.0.1/24";
        ArrayList<String> unavailableAddresses = new ArrayList<>();
        unavailableAddresses.add("10.100.0.2/32");
        unavailableAddresses.add("10.100.0.3/32");

        String result = IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);

        assertEquals("10.100.0.4", result);
    }

    @Test
    public void testGetNextAvailableIpv4AddressMixedCidrAndPlainAddresses() {
        String networkCidr = "10.100.0.1/24";
        ArrayList<String> unavailableAddresses = new ArrayList<>();
        unavailableAddresses.add("10.100.0.2");
        unavailableAddresses.add("10.100.0.3/32");
        unavailableAddresses.add("10.100.0.4");
        unavailableAddresses.add("10.100.0.5/24");

        String result = IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);

        assertEquals("10.100.0.6", result);
    }

    @Test
    public void testGetNextAvailableIpv4AddressReturnsHighAddress() {
        String networkCidr = "10.100.0.1/24";
        ArrayList<String> unavailableAddresses = new ArrayList<>();

        // Fill up addresses from .2 to .253
        for (int i = 2; i <= 253; i++) {
            unavailableAddresses.add("10.100.0." + i);
        }

        String result = IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);

        assertEquals("10.100.0.254", result);
    }

    @Test
    public void testGetNextAvailableIpv4AddressThrowsExceptionWhenNetworkFull() {
        String networkCidr = "10.100.0.1/24";
        ArrayList<String> unavailableAddresses = new ArrayList<>();

        // Fill up all addresses from .2 to .254
        for (int i = 2; i <= 254; i++) {
            unavailableAddresses.add("10.100.0." + i);
        }

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);
        });

        assertTrue(exception.getMessage().contains("No available IP addresses in network 10.100.0.1/24"));
    }

    @Test
    public void testGetNextAvailableIpv4AddressThrowsExceptionOnInvalidCidr() {
        String networkCidr = "10.100.0.1";  // Missing subnet mask
        ArrayList<String> unavailableAddresses = new ArrayList<>();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);
        });

        assertTrue(exception.getMessage().contains("is not a valid CIDR"));
    }

    @Test
    public void testGetNextAvailableIpv4AddressWithGapsInUnavailableList() {
        String networkCidr = "192.168.1.1/24";
        ArrayList<String> unavailableAddresses = new ArrayList<>();
        unavailableAddresses.add("192.168.1.2");
        unavailableAddresses.add("192.168.1.5");
        unavailableAddresses.add("192.168.1.10");

        String result = IPUtils.getNextAvailableIpv4Address(networkCidr, unavailableAddresses);

        // Should return the first gap at .3
        assertEquals("192.168.1.3", result);
    }
}
