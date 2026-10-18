package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.brcsrc.yaws.model.requests.CreateNetworkRequest;
import com.brcsrc.yaws.model.requests.CreateNetworkResponse;
import com.brcsrc.yaws.model.requests.DeleteNetworkRequest;
import com.brcsrc.yaws.model.requests.DeleteNetworkResponse;
import com.brcsrc.yaws.model.requests.DescribeNetworkRequest;
import com.brcsrc.yaws.model.requests.DescribeNetworkResponse;
import com.brcsrc.yaws.model.requests.ListNetworksRequest;
import com.brcsrc.yaws.model.requests.ListNetworksResponse;
import com.brcsrc.yaws.model.requests.UpdateNetworkRequest;
import com.brcsrc.yaws.model.requests.UpdateNetworkResponse;
import com.brcsrc.yaws.service.NetworkService;

import io.swagger.v3.oas.annotations.Operation;

/**
 * every operation is POST /api/v1/network/<operation-name> and exchanges JSON, taking a
 * <Operation>Request and returning a <Operation>Response. arguments are never carried in the
 * path or query string, so adding a field to an operation is additive rather than a route change.
 */
@RestController
@RequestMapping(Constants.BASE_URL + "/network")
public class NetworkController {

    private final NetworkService networkService;
    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    @Autowired
    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Operation(summary = "Create Network", description = "create a network")
    @PostMapping("/create-network")
    public CreateNetworkResponse createNetwork(@RequestBody CreateNetworkRequest request) {
        logger.info("received CreateNetwork request: {}", request);
        return new CreateNetworkResponse(this.networkService.createNetwork(request));
    }

    @Operation(summary = "Describe Network", description = "describe a network")
    @PostMapping("/describe-network")
    public DescribeNetworkResponse describeNetwork(@RequestBody DescribeNetworkRequest request) {
        logger.info("received DescribeNetwork request: {}", request);
        return new DescribeNetworkResponse(
                this.networkService.describeNetwork(request.getNetworkName()));
    }

    @Operation(summary = "List Networks", description = "list all networks with pagination")
    @PostMapping("/list-networks")
    public ListNetworksResponse listNetworks(@RequestBody ListNetworksRequest request) {
        logger.info("received ListNetworks request: {}", request);
        return this.networkService.listNetworks(request);
    }

    @Operation(summary = "Update Network", description = "update the tag, status or peer isolation of a network")
    @PostMapping("/update-network")
    public UpdateNetworkResponse updateNetwork(@RequestBody UpdateNetworkRequest request) {
        logger.info("received UpdateNetwork request: {}", request);
        return new UpdateNetworkResponse(this.networkService.updateNetwork(request));
    }

    @Operation(summary = "Delete Network", description = "delete a network")
    @PostMapping("/delete-network")
    public DeleteNetworkResponse deleteNetwork(@RequestBody DeleteNetworkRequest request) {
        logger.info("received DeleteNetwork request: {}", request);
        return new DeleteNetworkResponse(
                this.networkService.deleteNetwork(request.getNetworkName()));
    }
}
