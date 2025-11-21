package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.service.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.BASE_URL + "/tools")
public class ToolController {

    private static final Logger logger = LoggerFactory.getLogger(ToolController.class);

    private final ToolService toolService;

    @Autowired
    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @Operation(summary = "Get Public IP", description = "get the server's public IP address")
    @GetMapping("/public-ip")
    public GetPublicIpResponse getPublicIp() {
        logger.info("received GetPublicIp request");
        String publicIp = this.toolService.getPublicIp();
        return new GetPublicIpResponse(publicIp);
    }

    public record GetPublicIpResponse(String publicIp) {}
}
