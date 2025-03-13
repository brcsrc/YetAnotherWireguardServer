package com.brcsrc.yaws.api;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.requests.ListNetworkClientsRequest;
import com.brcsrc.yaws.service.NetworkClientService;
import com.brcsrc.yaws.utility.FilepathUtils;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(Constants.BASE_URL + "/clients")
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

    @Operation(summary = "Delete Network Client", description = "delete a specific client for a network")
    @DeleteMapping("/{networkName}/{clientName}")
    public NetworkClient deleteNetworkClient(@PathVariable String networkName, @PathVariable String clientName) {
        logger.info("received DeleteNetwork request");
        return this.networkClientService.deleteNetworkClient(networkName, clientName);
    }

    @Operation(summary = "Describe Network Clients", description = "describe a specific client for a specific network")
    @GetMapping("/{networkName}/{clientName}")
    public NetworkClient describeNetworkClient(@PathVariable String networkName, @PathVariable String clientName) {
        logger.info("received DescribeNetworkClient request");
        return this.networkClientService.describeNetworkClient(networkName, clientName);
    }

    @Operation(summary = "Get Network Client Configuration File", description = "get a  networkclient configuration .conf file for a client on a given network to be downloadable")
    @GetMapping("/{networkName}/{clientName}/config")
    public ResponseEntity<Resource> getNetworkClientConfigFile(@PathVariable String networkName, @PathVariable String clientName) {
        logger.info("received GetNetworkClientConfigFile request");
        try {
            String configFilePath = FilepathUtils.getClientConfigPath(networkName, clientName);

            File configFile = new File(configFilePath);

            if (!configFile.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration file not found!");
            }

            Resource resource = new FileSystemResource(configFile);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment;  filename=\"" + configFile.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error while getting network client config file", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while getting network client config file");
        }
    }
}
