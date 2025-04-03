package com.brcsrc.yaws.api;

import java.util.List;

import com.brcsrc.yaws.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.requests.UpdateNetworkRequest;
import com.brcsrc.yaws.service.NetworkService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(Constants.BASE_URL + "/networks")
public class NetworkController {

    private final NetworkService networkService;
    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    @Autowired
    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Operation(summary = "List Networks", description = "list all networks")
    @GetMapping
    public List<Network> listNetworks() {
        logger.info("received ListNetwork request");
        return this.networkService.getAllNetworks();
    }

    @Operation(summary = "Describe Network", description = "describe a network")
    @GetMapping("/{networkName}")
    public Network describeNetwork(@PathVariable String networkName) {
        logger.info("received DescribeNetwork request");
        return this.networkService.describeNetwork(networkName);
    }

    @Operation(summary = "Create Network", description = "create a network")
    @PostMapping
    public Network createNetwork(@RequestBody Network network) {
        logger.info("received CreateNetwork request: {}", network);
        return this.networkService.createNetwork(network);
    }

    @Operation(summary = "Delete Network", description = "delete a network")
    @DeleteMapping("/{networkName}")
    public Network DeleteNetwork(@PathVariable String networkName) {
        logger.info("received DeleteNetwork request: {}", networkName);
        return this.networkService.deleteNetwork(networkName);
    }

    @Operation(summary = "Update Network", description = "update the tag or status of a network")
    @PatchMapping("/{networkName}")
    public Network updateNetwork(@PathVariable String networkName, @RequestBody UpdateNetworkRequest updateNetworkRequest) {
        logger.info("received UpdateNetworkTag request, updating network {} with new tag: {}", networkName, updateNetworkRequest);
        return this.networkService.updateNetwork(networkName, updateNetworkRequest);
    }
}