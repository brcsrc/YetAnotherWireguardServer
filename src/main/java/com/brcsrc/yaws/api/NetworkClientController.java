package com.brcsrc.yaws.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.requests.ListNetworkClientsRequest;
import com.brcsrc.yaws.service.NetworkClientService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/clients")
public class NetworkClientController {

    private final NetworkClientService networkClientService;
    private static final Logger logger = LoggerFactory.getLogger(NetworkClientController.class);

    @Autowired
    public NetworkClientController(NetworkClientService networkClientService) {
        this.networkClientService = networkClientService;
    }

    @Operation(summary = "Create Network Client", description = "create a client for a specific network")
    @PostMapping
    public NetworkClient createNetworkClient(@RequestBody CreateNetworkClientRequest createNetworkClientRequest) {
        logger.info("received CreateNetworkClient request");
        return this.networkClientService.addClientToNetwork(createNetworkClientRequest);
    }

    @Operation(summary = "List Network Clients", description = "list clients for a specific network")
    @GetMapping
    public List<Client> listNetworkClients(@RequestBody ListNetworkClientsRequest listNetworkClientsRequest) {
        logger.info("received ListNetworkClients request");
        return this.networkClientService.listNetworkClients(listNetworkClientsRequest);
    }

    @Operation(summary = "Describe Network Clients", description = "describe a specific client for a specific network")
    @GetMapping("/{networkName}/{clientName}")
    public NetworkClient describeNetworkClient(@PathVariable String networkName, @PathVariable String clientName) {
        logger.info("received DescribeNetworkClient request");
        return this.networkClientService.describeNetworkClient(networkName, clientName);
    }
}
