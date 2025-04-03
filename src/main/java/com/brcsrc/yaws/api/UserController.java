package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.responses.AuthenticationResponse;
import com.brcsrc.yaws.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.BASE_URL + "/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create Admin User", description = "creates an admin user if one does not exist")
    @PostMapping("/register")
    public User createAdminUser(@RequestBody User newUser) {
        logger.info("received CreateAdminUser request");
        return this.userService.createAdminUser(newUser);
    }

    @Operation(summary = "Authenticate user and get token", description = "attempts to authenticate a user and returns an encoded JWT")
    @PostMapping("/authenticate")
    public AuthenticationResponse authenticateAndIssueToken(@RequestBody User user) {
        logger.info("got AuthenticateAndIssueToken request");
        return this.userService.authenticateAndIssueToken(user);
    }
}
