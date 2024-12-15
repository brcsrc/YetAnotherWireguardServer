package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.service.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
public class NetworkController {

    @Autowired
    NetworkService networkService;

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    @Autowired
    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Operation(summary = "List Networks", description = "list all networks")
    @GetMapping("/api/v1/networks")
    public List<Network> listNetworks() {
        logger.info("received ListNetwork request");
        return this.networkService.getAllNetworks();
    }

    @Operation(summary = "Create Network", description = "create a network")
    @PostMapping("/api/v1/networks")
    public Network createNetwork(@RequestBody Network network) {
        logger.info("received CreateNetwork request: {}", network);
        return this.networkService.createNetwork(network);
    }

    @Operation(summary = "Delete Network", description = "Delete a network")
    @DeleteMapping("/api/v1/networks/{networkName}")
    public Network DeleteNetwork(@PathVariable String networkName) {
        logger.info("received DeleteNetwork request: {}", networkName);
        return this.networkService.deleteNetwork(networkName);
    }
}

