package com.brcsrc.yaws.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class ToolService {

    private static final Logger logger = LoggerFactory.getLogger(ToolService.class);

    public String getPublicIp() {
        logger.info("Fetching public IP address");

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ifconfig.me"))
                    .header("User-Agent", "curl/7.68.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errMsg = String.format("Failed to get public IP: HTTP %d", response.statusCode());
                logger.error(errMsg);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errMsg);
            }

            String publicIp = response.body().trim();
            logger.info("Public IP: {}", publicIp);

            return publicIp;
        } catch (Exception e) {
            String errMsg = String.format("Failed to get public IP: %s", e.getMessage());
            logger.error(errMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errMsg);
        }
    }
}
