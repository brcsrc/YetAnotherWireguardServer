package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.requests.WhoamiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.service.UserService;
import com.brcsrc.yaws.utility.HeaderUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;

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

    @Operation(
        summary = "Authenticate user and get token",
        description = "attempts to authenticate a user and returns an encoded JWT",
        responses = {
            @ApiResponse(responseCode = "204", description = "Authenticated, no content returned")
        }
    )
    @PostMapping("/authenticate")
    public ResponseEntity<Void> authenticateAndIssueToken(@RequestBody User user, HttpServletResponse response) {
        logger.info("got AuthenticateAndIssueToken request");
        final String jwt = this.userService.authenticateAndIssueToken(user);
        final String cookieValue = HeaderUtils.createResponseHttpOnlyAuthTokenCookieValue(jwt);
        response.setHeader("Set-Cookie", cookieValue);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "whoami", description = "returns the username of the logged in user if their token is valid")
    @PostMapping("/whoami")
    public WhoamiResponse whoami(HttpServletRequest request) {
        logger.info("got Whoami request");
        final String jwt = HeaderUtils.getRequestHttpOnlyAuthTokenCookieValue(request);
        final String username = this.userService.whoami(jwt);
        return new WhoamiResponse(username);
    }

    @Operation(summary = "logout", description = "clears the authentication token cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        logger.info("got Logout request");
        final String expiredCookie = HeaderUtils.createExpiredAuthTokenCookie();
        response.setHeader("Set-Cookie", expiredCookie);
        return ResponseEntity.noContent().build();
    }
}
