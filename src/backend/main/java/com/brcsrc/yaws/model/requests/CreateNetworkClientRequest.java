package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.Constants;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateNetworkClientRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    @Pattern(regexp = Constants.IPV4_CIDR_REGEXP)
    private String clientCidr;

    @Pattern(regexp = Constants.IPV4_ADDRESS_REGEXP)
    private String clientDns;

    // TODO add custom validator as this could be address or cidr
    private String allowedIps;

    @NotBlank(message = "Network name is required")
    private String networkName;

    @Pattern(regexp = Constants.IPV4_ADDRESS_REGEXP)
    private String networkEndpoint;

    private String clientTag;

    @Schema(description = "when true this client can neither reach nor be reached by any other "
            + "peer on the network. its traffic still routes through the server to the internet")
    private boolean peerIsolationEnabled;
}
