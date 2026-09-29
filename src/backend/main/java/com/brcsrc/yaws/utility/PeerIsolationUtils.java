package com.brcsrc.yaws.utility;

/**
 * naming for the per network peer isolation chain.
 *
 * the chain name is needed in two places that must agree: the config file, where the chain is
 * created and linked by the interface hooks, and the runtime toggle, which inserts and deletes
 * policy rules inside it while the interface is up. it lives here so neither owns it.
 *
 * peer to peer traffic on a wireguard network enters and leaves the same interface, so it is the
 * only traffic matching '-i <iface> -o <iface>' in the FORWARD chain. the isolation chain is
 * jumped to from exactly that match, which means rules placed in it can never affect a client
 * reaching the internet (wg -> egress) or the server itself (INPUT).
 */
public class PeerIsolationUtils {

    // iptables chain names are capped at 28 characters. network names are capped at 15 by
    // IFNAMSIZ, so a fixed prefix of this length always fits
    private static final String CHAIN_PREFIX = "YAWS-ISO-";

    /**
     * the isolation chain name for a network. a fixed prefix rather than a suffix so the length
     * is bounded by the prefix and cannot overflow the 28 character limit on long network names.
     * the prefix also makes yaws owned chains identifiable in 'iptables -L' on a host where
     * other things write rules.
     */
    public static String getIsolationChainName(String networkName) {
        return CHAIN_PREFIX + networkName;
    }
}
