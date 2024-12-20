package com.brcsrc.yaws.utility;

public class IPUtils {
    public static boolean isValidIpv4Cidr(String cidr) {
        String validIpv4CidrRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(3[0-2]|[1-2][0-9]|[0-9]))$";
        return cidr.matches(validIpv4CidrRegex);
    }

    public static boolean isValidIpv4Address(String address) {
        String validIpv4AddressRegex = "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$";
        return address.matches(validIpv4AddressRegex);
    }
}
