package com.brcsrc.yaws.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class NetworkController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    @Operation(summary = "List Networks", description = "list all networks")
    @GetMapping("/networks")
    public String listNetworks() {
        logger.info("fetching list of networks");
        return "goodbye, world!";
    }
}

