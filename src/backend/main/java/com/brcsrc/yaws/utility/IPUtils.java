package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.model.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class IPUtils {
    public static boolean isValidIpv4Cidr(String cidr) {
        return cidr.matches(Constants.IPV4_CIDR_REGEXP);
    }

    public static boolean isValidIpv4Address(String address) {
        return address.matches(Constants.IPV4_ADDRESS_REGEXP);
    }

    public static boolean isValidFQDN(String fqdn) {
        return fqdn.matches(Constants.FQDN_REGEXP);
    }

    public static boolean isValidOrigin(String origin) {
        return origin.matches(Constants.ORIGIN_REGEXP);
    }

    public static boolean isValidClientConfigEndpoint(String endpoint) {
        String[] parts = endpoint.split(":");
        if (parts.length != 2) {
            return false;
        }
        String endpointAddress = parts[0];
        if (!IPUtils.isValidIpv4Address(endpointAddress)) {
            return false;
        }

        String endpointPort = parts[1];
        try {
            int port = Integer.parseInt(endpointPort);
            if (port <= 0 || port > 65535) {
                return false;
            }
        } catch (NumberFormatException exception) {
            return false;
        }

        return true;
    }

    public static boolean isValidEndpoint(String endpoint) {
        // TODO this is currently used to check an incoming CreateNetworkClientRequest endpoint field
        // TODO this needs to validate that the input is either a valid IPV4 or a valid FQDN
        return isValidIpv4Address(endpoint);
    }

    public static boolean isNetworkMemberInNetworkRange(String networkCidr, String networkMemberCidr) {
        // TODO this assumes all clients want inter domain routing with a subnet like /24
        // TODO if they want to not talk to other peers then passing only an address does not work
        // check cidrs match pattern
        if (!isValidIpv4Cidr(networkCidr)) {
            throw new IllegalArgumentException(String.format("%s is not a valid CIDR", networkCidr));
        }

        if (!isValidIpv4Cidr(networkMemberCidr)) {
            throw new IllegalArgumentException(String.format("%s is not a valid CIDR", networkMemberCidr));
        }

        String[] networkCidrParts = networkCidr.split("/");
        int networkSubnetMask = Integer.parseInt(networkCidrParts[1]);

        String[] networkMemberCidrParts = networkMemberCidr.split("/");
        int networkMemberSubnetMask = Integer.parseInt(networkMemberCidrParts[1]);

        // they technically should be able to use a smaller subnet mask on the client
        // but the client would lose routing to other hosts on the same network
        if (networkMemberSubnetMask != 32 && networkSubnetMask < networkMemberSubnetMask) {
            throw new IllegalArgumentException(String.format("client subnet mask /%s is outside of network subnet mask /%s", networkMemberSubnetMask, networkSubnetMask));
        }

        String networkAddress = networkCidrParts[0];
        String networkMemberAddress = networkMemberCidrParts[0];

        String[] networkAddressNetworkOctects = Arrays.copyOfRange(networkAddress.split("\\."), 0, 3);
        String[] networkMemberAddressNetworkOctets = Arrays.copyOfRange(networkMemberAddress.split("\\."), 0, 3);

        // if the last octet of each matches then it is not available
        if (Objects.equals(networkAddress.split("\\.")[3], networkMemberAddress.split("\\.")[3])) {
            return false;
        }
        // if the member address host octet is broadcast (255) then it is not in range
        if(Objects.equals(networkMemberAddress.split("\\.")[3], "255")) {
            return false;
        }
        // for now the network takes up at least 24 bits, so the first 3 octets of both CIDRs should match
        for (int i = 0; i < networkMemberAddressNetworkOctets.length; i++) {
            if (!Objects.equals(networkAddressNetworkOctects[i], networkMemberAddressNetworkOctets[i])) {
                return false;
            }
        }

        return true;
    }

    public static String getNextAvailableIpv4Address(String networkCidr, ArrayList<String> unavailableAddresses) {
        if (!isValidIpv4Cidr(networkCidr)) {
            throw new IllegalArgumentException(String.format("%s is not a valid CIDR", networkCidr));
        }

        String[] cidrParts = networkCidr.split("/");
        String networkAddress = cidrParts[0];
        int subnetMask = Integer.parseInt(cidrParts[1]);

        // Get the network octets (first 3 for /24)
        String[] networkOctets = networkAddress.split("\\.");
        String networkPrefix = networkOctets[0] + "." + networkOctets[1] + "." + networkOctets[2];

        // Start from 2 to skip the network address (.0) and gateway (.1)
        // End at 254 to skip broadcast (.255)
        for (int hostOctet = 2; hostOctet < 255; hostOctet++) {
            String candidateAddress = networkPrefix + "." + hostOctet;

            // Check if this address is already taken
            boolean isAvailable = true;
            for (String unavailableAddress : unavailableAddresses) {
                // Extract just the IP part if it's a CIDR
                String unavailableIp = unavailableAddress.contains("/")
                    ? unavailableAddress.split("/")[0]
                    : unavailableAddress;

                if (candidateAddress.equals(unavailableIp)) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                return candidateAddress;
            }
        }

        // No available addresses found
        throw new IllegalArgumentException(String.format("No available IP addresses in network %s", networkCidr));
    }
}
