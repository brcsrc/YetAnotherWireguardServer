package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.model.requests.ListNetworkClientsRequest;
import com.brcsrc.yaws.service.NetworkClientService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
public class NetworkClientController {

    private final NetworkClientService networkClientService;
    private static final Logger logger = LoggerFactory.getLogger(NetworkClientController.class);

    @Autowired
    public NetworkClientController(NetworkClientService networkClientService) { this.networkClientService = networkClientService;}

    @Operation(summary = "Create Network Client", description = "create a client for a specific network")
    @PostMapping("/api/v1/clients")
    public NetworkClient createNetworkClient(@RequestBody CreateNetworkClientRequest createNetworkClientRequest) {
        logger.info("received CreateNetworkClient request");
        return this.networkClientService.addClientToNetwork(createNetworkClientRequest);
    }

    @Operation(summary = "List Network Clients", description = "list clients for a specific network")
    @GetMapping("/api/v1/clients")
    public List<Client> listNetworkClients(@RequestBody ListNetworkClientsRequest listNetworkClientsRequest) {
        logger.info("received ListNetworkClients request");
        return this.networkClientService.listNetworkClients(listNetworkClientsRequest);
    }

}
