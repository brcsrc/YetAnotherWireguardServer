package com.brcsrc.yaws.model.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A request object for creating the admin user.
 */
@Data
public class CreateAdminUserRequest {

    @NotBlank(message = "User name is required")
    @Schema(description = "name for the admin user")
    private String userName;

    @NotBlank(message = "Password is required")
    @Schema(description = "password for the admin user")
    private String password;
}
