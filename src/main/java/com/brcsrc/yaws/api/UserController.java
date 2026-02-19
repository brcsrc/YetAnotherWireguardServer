package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.requests.AuthenticateStartRequest;
import com.brcsrc.yaws.model.requests.AuthenticateStartResponse;
import com.brcsrc.yaws.model.requests.RegenerateRecoveryCodesResponse;
import com.brcsrc.yaws.model.requests.TotpEnrollConfirmRequest;
import com.brcsrc.yaws.model.requests.TotpEnrollStartResponse;
import com.brcsrc.yaws.model.requests.VerifyRecoveryCodeRequest;
import com.brcsrc.yaws.model.requests.VerifyTotpRequest;
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

    @Operation(
            summary = "Start authentication flow",
            description = "authenticates username/password and either returns challenge metadata or issues JWT",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Start authentication response")
            }
    )
    @PostMapping("/authenticate/start")
    public ResponseEntity<AuthenticateStartResponse> authenticateStart(@RequestBody AuthenticateStartRequest request, HttpServletResponse response) {
        logger.info("got AuthenticateStart request");
        UserService.AuthenticateStartResult authenticateStartResult = this.userService.authenticateStart(request.getUserName(), request.getPassword());
        if (authenticateStartResult.isTwoFactorRequired()) {
            response.setHeader(
                    "Set-Cookie",
                    HeaderUtils.createResponsePreAuthSessionCookieValue(
                            authenticateStartResult.getPreAuthSessionId(),
                            Constants.PRE_AUTH_TOKEN_VALIDITY_PERIOD_SECONDS
                    )
            );
            return ResponseEntity.ok(authenticateStartResult.toResponse());
        }

        response.setHeader("Set-Cookie", HeaderUtils.createResponseHttpOnlyAuthTokenCookieValue(authenticateStartResult.getIssuedJwt()));
        return ResponseEntity.ok(authenticateStartResult.toResponse());
    }

    @Operation(summary = "Verify TOTP second factor", description = "verifies TOTP code and issues JWT on success")
    @PostMapping("/2fa/verify/totp")
    public ResponseEntity<Void> verifyTotp(@RequestBody VerifyTotpRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        logger.info("got VerifyTotp request");
        String preAuthSessionId = HeaderUtils.getRequestPreAuthSessionCookieValue(httpRequest);
        String jwt = this.userService.verifyTotpAndIssueToken(preAuthSessionId, request.getOtpCode());
        response.addHeader("Set-Cookie", HeaderUtils.createResponseHttpOnlyAuthTokenCookieValue(jwt));
        response.addHeader("Set-Cookie", HeaderUtils.createExpiredPreAuthSessionCookie());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verify recovery code second factor", description = "verifies recovery code and issues JWT on success")
    @PostMapping("/2fa/verify/recovery-code")
    public ResponseEntity<Void> verifyRecoveryCode(@RequestBody VerifyRecoveryCodeRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        logger.info("got VerifyRecoveryCode request");
        String preAuthSessionId = HeaderUtils.getRequestPreAuthSessionCookieValue(httpRequest);
        String jwt = this.userService.verifyRecoveryCodeAndIssueToken(preAuthSessionId, request.getRecoveryCode());
        response.addHeader("Set-Cookie", HeaderUtils.createResponseHttpOnlyAuthTokenCookieValue(jwt));
        response.addHeader("Set-Cookie", HeaderUtils.createExpiredPreAuthSessionCookie());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Start TOTP enrollment", description = "generates a TOTP secret and returns setup metadata")
    @PostMapping("/2fa/totp/enroll/start")
    public ResponseEntity<TotpEnrollStartResponse> startTotpEnrollment(HttpServletRequest request) {
        logger.info("got StartTotpEnrollment request");
        String jwt = HeaderUtils.getRequestHttpOnlyAuthTokenCookieValue(request);
        TotpEnrollStartResponse response = this.userService.startTotpEnrollment(jwt);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirm TOTP enrollment", description = "verifies initial OTP and enables TOTP")
    @PostMapping("/2fa/totp/enroll/confirm")
    public ResponseEntity<Void> confirmTotpEnrollment(@RequestBody TotpEnrollConfirmRequest request, HttpServletRequest httpRequest) {
        logger.info("got ConfirmTotpEnrollment request");
        String jwt = HeaderUtils.getRequestHttpOnlyAuthTokenCookieValue(httpRequest);
        this.userService.confirmTotpEnrollment(jwt, request.getOtpCode());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Regenerate recovery codes", description = "regenerates one-time recovery codes")
    @PostMapping("/2fa/recovery-codes/regenerate")
    public ResponseEntity<RegenerateRecoveryCodesResponse> regenerateRecoveryCodes(HttpServletRequest request) {
        logger.info("got RegenerateRecoveryCodes request");
        String jwt = HeaderUtils.getRequestHttpOnlyAuthTokenCookieValue(request);
        RegenerateRecoveryCodesResponse response = this.userService.regenerateRecoveryCodes(jwt);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "whoami", description = "returns the username of the logged in user if their token is valid")
    @PostMapping("/whoami")
    public WhoamiResponse whoami(HttpServletRequest request) {
        logger.info("got Whoami request");
        final String jwt = HeaderUtils.getRequestHttpOnlyAuthTokenCookieValue(request);
        final String username = this.userService.whoami(jwt);
        return new WhoamiResponse(username, this.userService.isTwoFactorGloballyEnabled());
    }

    @Operation(summary = "logout", description = "clears the authentication token cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        logger.info("got Logout request");
        response.addHeader("Set-Cookie", HeaderUtils.createExpiredAuthTokenCookie());
        response.addHeader("Set-Cookie", HeaderUtils.createExpiredPreAuthSessionCookie());
        return ResponseEntity.noContent().build();
    }
}
