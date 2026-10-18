package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for authenticating and receiving an auth token cookie.
 */
@Data
public class AuthenticateRequest {

    @NotBlank(message = "User name is required")
    @Schema(description = "name of the user to authenticate")
    private String userName;

    @NotBlank(message = "Password is required")
    @Schema(description = "password for the user")
    private String password;
}
