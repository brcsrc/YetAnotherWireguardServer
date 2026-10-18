package com.brcsrc.yaws.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.requests.CreateNetworkClientResponse;
import com.brcsrc.yaws.model.requests.DeleteNetworkClientRequest;
import com.brcsrc.yaws.model.requests.DeleteNetworkClientResponse;
import com.brcsrc.yaws.model.requests.DescribeNetworkClientRequest;
import com.brcsrc.yaws.model.requests.DescribeNetworkClientResponse;
import com.brcsrc.yaws.model.requests.GetNetworkClientConfigFileQRRequest;
import com.brcsrc.yaws.model.requests.GetNetworkClientConfigFileRequest;
import com.brcsrc.yaws.model.requests.GetNextAvailableClientAddressRequest;
import com.brcsrc.yaws.model.requests.GetNextAvailableClientAddressResponse;
import com.brcsrc.yaws.model.requests.ListNetworkClientsRequest;
import com.brcsrc.yaws.model.requests.ListNetworkClientsResponse;
import com.brcsrc.yaws.model.requests.UpdateNetworkClientRequest;
import com.brcsrc.yaws.model.requests.UpdateNetworkClientResponse;
import com.brcsrc.yaws.service.NetworkClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * every operation is POST /api/v1/client/<operation-name> and takes a <Operation>Request body.
 *
 * most return a matching <Operation>Response. the two config download operations are the
 * exception: they return the file itself, a .conf or a PNG, because the payload is not JSON.
 * their paths still follow the convention.
 */
@RestController
@RequestMapping(Constants.BASE_URL + "/client")
public class NetworkClientController {

    private final NetworkClientService networkClientService;
    private static final Logger logger = LoggerFactory.getLogger(NetworkClientController.class);

    @Autowired
    public NetworkClientController(NetworkClientService networkClientService) {
        this.networkClientService = networkClientService;
    }

    @Operation(summary = "Create Network Client", description = "create a client for a specific network")
    @PostMapping("/create-client")
    public CreateNetworkClientResponse createNetworkClient(@RequestBody CreateNetworkClientRequest request) {
        logger.info("received CreateNetworkClient request: {}", request);
        return new CreateNetworkClientResponse(this.networkClientService.addClientToNetwork(request));
    }

    @Operation(summary = "Describe Network Client", description = "describe a specific client for a specific network")
    @PostMapping("/describe-client")
    public DescribeNetworkClientResponse describeNetworkClient(@RequestBody DescribeNetworkClientRequest request) {
        logger.info("received DescribeNetworkClient request: {}", request);
        return new DescribeNetworkClientResponse(this.networkClientService.describeNetworkClient(
                request.getNetworkName(), request.getClientName()));
    }

    @Operation(summary = "List Network Clients", description = "list clients for a specific network with pagination")
    @PostMapping("/list-clients")
    public ListNetworkClientsResponse listNetworkClients(@RequestBody ListNetworkClientsRequest request) {
        logger.info("received ListNetworkClients request: {}", request);
        return this.networkClientService.listNetworkClients(request);
    }

    @Operation(summary = "Update Network Client", description = "update the tag or peer isolation of a client")
    @PostMapping("/update-client")
    public UpdateNetworkClientResponse updateNetworkClient(@RequestBody UpdateNetworkClientRequest request) {
        logger.info("received UpdateNetworkClient request: {}", request);
        return new UpdateNetworkClientResponse(this.networkClientService.updateNetworkClient(request));
    }

    @Operation(summary = "Delete Network Client", description = "delete a specific client for a network")
    @PostMapping("/delete-client")
    public DeleteNetworkClientResponse deleteNetworkClient(@RequestBody DeleteNetworkClientRequest request) {
        logger.info("received DeleteNetworkClient request: {}", request);
        return new DeleteNetworkClientResponse(this.networkClientService.deleteNetworkClient(
                request.getNetworkName(), request.getClientName()));
    }

    @Operation(summary = "Get Next Available Client Address",
            description = "get the next available IP address for a client on a given network")
    @PostMapping("/get-next-available-address")
    public GetNextAvailableClientAddressResponse getNextAvailableClientAddress(
            @RequestBody GetNextAvailableClientAddressRequest request) {
        logger.info("received GetNextAvailableClientAddress request: {}", request);
        return new GetNextAvailableClientAddressResponse(
                this.networkClientService.getNextAvailableClientAddress(request.getNetworkName()));
    }

    @Operation(summary = "Get Network Client Configuration File",
            description = "download a client's wireguard .conf file. returns the file rather than JSON")
    @PostMapping("/get-client-config")
    public ResponseEntity<Resource> getNetworkClientConfigFile(
            @RequestBody GetNetworkClientConfigFileRequest request) {
        logger.info("received GetNetworkClientConfigFile request: {}", request);
        java.io.File configFile = this.networkClientService.getNetworkClientConfigFile(
                request.getNetworkName(), request.getClientName());
        Resource configFileResource = new FileSystemResource(configFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + configFile.getName() + "\"")
                .body(configFileResource);
    }

    @Operation(
        summary = "Get Network Client Configuration QR Code",
        description = "render a client's configuration as a QR code. returns a PNG rather than JSON",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "QR code image",
                content = @Content(
                    mediaType = "image/png",
                    schema = @Schema(type = "string", format = "binary")
                )
            )
        }
    )
    @PostMapping(value = "/get-client-config-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getNetworkClientConfigFileQR(@RequestBody GetNetworkClientConfigFileQRRequest request) {
        logger.info("received GetNetworkClientConfigFileQR request: {}", request);
        return this.networkClientService.getNetworkClientConfigFileQR(
                request.getNetworkName(), request.getClientName());
    }
}
