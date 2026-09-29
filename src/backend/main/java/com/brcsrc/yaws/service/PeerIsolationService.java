package com.brcsrc.yaws.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.shell.CommandExecutor;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.utility.PeerIsolationUtils;

/**
 * applies peer isolation policy to a running wireguard interface.
 *
 * policy is stored in the database and applied here as live iptables rules against the network's
 * isolation chain, which the interface's PostUp hooks created. it is deliberately NOT written into
 * the config file: doing so would mean a config rewrite and a wg-quick cycle to toggle it, and
 * that drops every client on the network. inserting a rule into an existing chain disturbs
 * nothing, since a client's traffic to the internet never traverses this chain.
 *
 * the chain is empty on every interface bring up, so policy has to be reapplied afterwards.
 * {@link #applyNetworkPolicy(Network)} is that reapply, and every place that brings an interface
 * up must call it.
 */
@Service
public class PeerIsolationService {

    private final CommandExecutor commandExecutor;
    private static final Logger logger = LoggerFactory.getLogger(PeerIsolationService.class);

    public PeerIsolationService(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * applies all peer isolation policy for a network to the live interface.
     *
     * called after any wg-quick up, because the isolation chain is recreated empty each time. the
     * database is the source of truth, so this reasserts it rather than trusting kernel state.
     *
     * shaped as "apply this network's policy" rather than "apply network isolation" so per client
     * rules can be added here later without revisiting the call sites that bring interfaces up.
     */
    public void applyNetworkPolicy(Network network) {
        if (network.isPeerIsolationEnabled()) {
            logger.info("applying network wide peer isolation for network '{}'",
                    network.getNetworkName());
            addNetworkWideIsolationRule(network.getNetworkName());
        }
        // per client isolation rules are applied here once that feature lands
    }

    /**
     * enables or disables network wide peer isolation on the live interface.
     *
     * a no op when the requested state already matches what is in the chain, so repeated calls
     * cannot stack duplicate rules or fail trying to delete a rule that is not there.
     */
    public void setNetworkWideIsolation(String networkName, boolean enabled) {
        boolean currentlyIsolated = isNetworkWideIsolationRulePresent(networkName);
        if (enabled == currentlyIsolated) {
            logger.info("network wide isolation for '{}' is already {}, nothing to do",
                    networkName, enabled ? "enabled" : "disabled");
            return;
        }

        if (enabled) {
            addNetworkWideIsolationRule(networkName);
        } else {
            removeNetworkWideIsolationRule(networkName);
        }
    }

    /**
     * the rule denying all peer to peer traffic on a network.
     *
     * there is deliberately no conntrack exemption above this. a RELATED,ESTABLISHED accept would
     * let peer to peer flows that are already running continue until they went idle, so enabling
     * isolation would not actually isolate anything already connected. a bare DROP takes effect at
     * the next packet, including for established flows.
     */
    private List<String> networkWideIsolationRule(String networkName, String operation) {
        return List.of(
                "iptables", operation,
                PeerIsolationUtils.getIsolationChainName(networkName),
                "-j", "DROP");
    }

    private void addNetworkWideIsolationRule(String networkName) {
        // appended, so per client ACCEPT exceptions can later be inserted above it
        ExecutionResult result = this.commandExecutor.runCommand(
                networkWideIsolationRule(networkName, "-A"));
        if (result.getExitCode() != 0) {
            logger.error("failed to enable peer isolation for network '{}': {}",
                    networkName, result.getStderr());
            throw new InternalServerException(String.format(
                    "failed to enable peer isolation for network '%s'", networkName));
        }
    }

    private void removeNetworkWideIsolationRule(String networkName) {
        ExecutionResult result = this.commandExecutor.runCommand(
                networkWideIsolationRule(networkName, "-D"));
        if (result.getExitCode() != 0) {
            logger.error("failed to disable peer isolation for network '{}': {}",
                    networkName, result.getStderr());
            throw new InternalServerException(String.format(
                    "failed to disable peer isolation for network '%s'", networkName));
        }
    }

    /**
     * whether the network wide isolation rule is present in the live chain. false when the chain
     * does not exist, which is the case while the interface is down.
     */
    public boolean isNetworkWideIsolationRulePresent(String networkName) {
        ExecutionResult result = this.commandExecutor.runCommand(
                networkWideIsolationRule(networkName, "-C"));
        return result.getExitCode() == 0;
    }
}
