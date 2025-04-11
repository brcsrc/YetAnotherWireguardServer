package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.service.UserService;
import com.brcsrc.yaws.utility.HeaderUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> authenticateAndIssueToken(@RequestBody User user, HttpServletResponse response) {
        logger.info("got AuthenticateAndIssueToken request");
        final String jwt = this.userService.authenticateAndIssueToken(user);
        final String cookieValue = HeaderUtils.createResponseHttpOnlyAuthTokenCookieValue(jwt);
        // Set-Cookie tells the client browser to set cookie for that domain
        response.setHeader("Set-Cookie", cookieValue);
        return ResponseEntity.ok().build();
    }
}
