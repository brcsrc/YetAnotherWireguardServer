package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.requests.AuthenticateRequest;
import com.brcsrc.yaws.model.requests.CreateAdminUserRequest;
import com.brcsrc.yaws.model.requests.CreateAdminUserResponse;
import com.brcsrc.yaws.model.requests.WhoamiResponse;
import com.brcsrc.yaws.service.UserService;
import com.brcsrc.yaws.utility.HeaderUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * every operation is POST /api/v1/user/<operation-name> and takes a <Operation>Request body.
 *
 * authenticate and logout return 204 with a Set-Cookie header rather than a response body: the
 * auth token is an HttpOnly cookie and is deliberately never placed in JSON where page scripts
 * could read it.
 */
@RestController
@RequestMapping(Constants.BASE_URL + "/user")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create Admin User", description = "creates an admin user if one does not exist")
    @PostMapping("/create-admin-user")
    public CreateAdminUserResponse createAdminUser(@RequestBody CreateAdminUserRequest request) {
        logger.info("received CreateAdminUser request");
        User newUser = new User();
        newUser.setUserName(request.getUserName());
        newUser.setPassword(request.getPassword());
        // only the user name is returned. the saved entity carries the bcrypt password hash and
        // must never be serialized back to the caller
        return new CreateAdminUserResponse(this.userService.createAdminUser(newUser).getUserName());
    }

    @Operation(
        summary = "Authenticate",
        description = "attempts to authenticate a user and sets an HttpOnly auth token cookie",
        responses = {
            @ApiResponse(responseCode = "204", description = "Authenticated, token set as a cookie")
        }
    )
    @PostMapping("/authenticate")
    public ResponseEntity<Void> authenticate(
            @RequestBody AuthenticateRequest request, HttpServletResponse response) {
        logger.info("received Authenticate request");
        User user = new User();
        user.setUserName(request.getUserName());
        user.setPassword(request.getPassword());
        final String jwt = this.userService.authenticateAndIssueToken(user);
        response.setHeader("Set-Cookie", HeaderUtils.createResponseHttpOnlyAuthTokenCookieValue(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Whoami", description = "returns the username of the logged in user if their token is valid")
    @PostMapping("/whoami")
    public WhoamiResponse whoami(HttpServletRequest httpRequest) {
        logger.info("received Whoami request");
        final String jwt = HeaderUtils.getRequestHttpOnlyAuthTokenCookieValue(httpRequest);
        return new WhoamiResponse(this.userService.whoami(jwt));
    }

    @Operation(
        summary = "Logout",
        description = "clears the authentication token cookie",
        responses = {
            @ApiResponse(responseCode = "204", description = "Logged out, auth cookie cleared")
        }
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        logger.info("received Logout request");
        response.setHeader("Set-Cookie", HeaderUtils.createExpiredAuthTokenCookie());
        return ResponseEntity.noContent().build();
    }
}
