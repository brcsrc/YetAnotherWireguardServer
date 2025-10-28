package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.security.JwtService;
import com.brcsrc.yaws.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;

    private final String testUserName = "yaws-admin";
    private final String testPassword = "gH1@#oKl2ff1";

    private final RestClient restClient = RestClient.create();

    private static final Logger logger = LoggerFactory.getLogger(UserControllerTests.class);

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + port + Constants.BASE_URL + "/user";
    }

    @AfterEach
    public void teardown() {
        Optional<User> testAdminUserOpt = userRepository.findByUserName(testUserName);
        if (testAdminUserOpt.isPresent()) {
            User testAdminUser = testAdminUserOpt.get();
            logger.info("cleaning up test admin user");
            userRepository.delete(testAdminUser);
        }
    }

    @Test
    public void createAdminUserCreatesAdminUser() {
        User user = new User();
        user.setUserName(testUserName);
        user.setPassword(testPassword);

        String createUserUrl = baseUrl + "/register";

        ResponseEntity<User> response = restClient.post()
                .uri(createUserUrl)
                .body(user)
                .retrieve()
                .toEntity(User.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Optional<User> userFromDbOpt = userRepository.findByUserName(testUserName);
        assertTrue(userFromDbOpt.isPresent());

        User userFromDb = userFromDbOpt.get();
        assertEquals(testUserName, userFromDb.getUserName());
        assertEquals(Constants.ADMIN_USER_ID, userFromDb.getId());

        // we must be storing passwords in the database not as plaintext but as Bcrypt
        // encoded values. bcrypt will salt the hashed value with time based factors so
        // we can not reuse paswordEncoder.encode(testPassword) as the string values will be
        // slightly different. passwordEncoder.matches is good for this use case
        assertTrue(passwordEncoder.matches(testPassword, userFromDb.getPassword()));

        // assert the returned values from the api are appropriate
        assertEquals(testUserName, response.getBody().getUserName());
        assertTrue(passwordEncoder.matches(testPassword, response.getBody().getPassword()));
    }

    @Test
    public void createAdminUserThrowsExceptionWhenAlreadyExists() {
        // this application is intended to only have a single user, the 'admin' user.
        // when it already exists, further createAdminUser requests should be rejected
        String createUserUrl = baseUrl + "/register";

        User user = new User();
        user.setUserName(testUserName);
        user.setPassword(testPassword);

        ResponseEntity<User> response = restClient.post()
                .uri(createUserUrl)
                .body(user)
                .retrieve()
                .toEntity(User.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        User secondUser = new User();
        secondUser.setUserName("yaws-admin-2");
        secondUser.setPassword(testPassword);

        ResponseEntity<String> secondResponse = restClient.post()
                .uri(createUserUrl)
                .body(secondUser)
                .exchange((request, _secondResponse) -> {
                    String responseBody = _secondResponse.bodyTo(String.class);
                    return ResponseEntity.status(_secondResponse.getStatusCode()).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, secondResponse.getStatusCode());
        String responseBody = secondResponse.getBody();
        assert responseBody != null;

        String errMsg = "cannot create admin user, admin user already exists";
        assertTrue(responseBody.contains(errMsg));

        Optional<User> userFromDbOpt = userRepository.findByUserName(testUserName);
        assertTrue(userFromDbOpt.isPresent());
        User userFromDb = userFromDbOpt.get();
        assertEquals(testUserName, userFromDb.getUserName());
        assertEquals(Constants.ADMIN_USER_ID, userFromDb.getId());
    }

    @Test
    public void createAdminUserThrowsExceptionOnShortPassword() {
        String createUserUrl = baseUrl + "/register";

        User user = new User();
        user.setUserName(testUserName);
        user.setPassword("abcABC123!1");

        ResponseEntity<String> response = restClient.post()
                .uri(createUserUrl)
                .body(user)
                .exchange((request, _response) -> {
                    String responseBody = _response.bodyTo(String.class);
                    return ResponseEntity.status(_response.getStatusCode()).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assert responseBody != null;

        String errMsg = "password must be at least 12 characters long";
        assertTrue(responseBody.contains(errMsg));
    }

    @Test
    public void createAdminUserThrowsExceptionOnSimplePassword() {
        String createUserUrl = baseUrl + "/register";

        User user = new User();
        user.setUserName(testUserName);
        user.setPassword("ABCDEFGHIJKabcdefghijk"); // long enough but missing key chars

        ResponseEntity<String> response = restClient.post()
                .uri(createUserUrl)
                .body(user)
                .exchange((request, _response) -> {
                    String responseBody = _response.bodyTo(String.class);
                    return ResponseEntity.status(_response.getStatusCode()).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String responseBody = response.getBody();
        assert responseBody != null;

        String errMsg = "password must have 2 lowercase letters, 2 uppercase letters, 1 special character and 1 number";
        assertTrue(responseBody.contains(errMsg));
    }

    @Test
    public void authenticateAndIssueTokenAuthenticatesAndIssuesTokenOnGoodCredentials() {
        User user = new User();
        user.setUserName(testUserName);
        user.setPassword(testPassword);

        userService.createAdminUser(user);

        String authenticateUrl = baseUrl + "/authenticate";

        ResponseEntity<?> response = restClient.post()
                .uri(authenticateUrl)
                .body(user)
                .retrieve()
                .toEntity(Object.class);

        // we are using HTTP 204 NO_CONTENT in the auth API since there is no body and the token is `HttpOnly`
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/204
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // get token
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assert cookies != null;
        Optional<String> jwtOpt = cookies.stream()
                .filter(cookie -> cookie.contains("accessToken="))
                .map(cookie -> cookie.substring(cookie.indexOf("accessToken=") + "accessToken=".length(), cookie.indexOf(";")))
                .findFirst();

        assertTrue(jwtOpt.isPresent());
        String jwt = jwtOpt.get();

        // validate it with jwt service
        assertTrue(jwtService.isTokenValid(jwt));
    }

    @Test
    public void authenticateAndIssueTokenThrowsExceptionOnBadCredentials() {
        User user = new User();
        user.setUserName(testUserName);
        user.setPassword(testPassword);

        userService.createAdminUser(user);

        String authenticateUrl = baseUrl + "/authenticate";

        // test incorrect password
        String complexButIncorrectPassword = "Agh^&d90=Hib@";
        user.setPassword(complexButIncorrectPassword);

        ResponseEntity<String> authenticateResponseBadPass = restClient.post()
                .uri(authenticateUrl)
                .body(user)
                .exchange((request, response) -> {
                    String body = response.bodyTo(String.class);
                    return ResponseEntity.status(response.getStatusCode()).body(body);
                });

        assertEquals(HttpStatus.FORBIDDEN, authenticateResponseBadPass.getStatusCode());
        String authenticateResponseBadPassBody = authenticateResponseBadPass.getBody();
        assert authenticateResponseBadPassBody != null;
        assertTrue(authenticateResponseBadPassBody.contains("authentication failed"));

        // test incorrect username
        String incorrectUserName = "somebodyelse";
        user.setUserName(incorrectUserName);
        user.setPassword(testPassword);

        ResponseEntity<String> authenticateResponseBadUserName = restClient.post()
                .uri(authenticateUrl)
                .body(user)
                .exchange((request, response) -> {
                    String body = response.bodyTo(String.class);
                    return ResponseEntity.status(response.getStatusCode()).body(body);
                });

        assertEquals(HttpStatus.FORBIDDEN, authenticateResponseBadUserName.getStatusCode());
        String authenticateResponseBadUserNameBody = authenticateResponseBadPass.getBody();
        assert authenticateResponseBadUserNameBody != null;
        assertTrue(authenticateResponseBadUserNameBody.contains("authentication failed"));

        // test when both are bad
        user.setUserName(incorrectUserName);
        user.setPassword(complexButIncorrectPassword);

        ResponseEntity<String> authenticateResponseBadUserNameAndPass = restClient.post()
                .uri(authenticateUrl)
                .body(user)
                .exchange((request, response) -> {
                    String body = response.bodyTo(String.class);
                    return ResponseEntity.status(response.getStatusCode()).body(body);
                });

        assertEquals(HttpStatus.FORBIDDEN, authenticateResponseBadUserNameAndPass.getStatusCode());
        String authenticateResponseBadUserNameAndPassBody = authenticateResponseBadPass.getBody();
        assert authenticateResponseBadUserNameAndPassBody != null;
        assertTrue(authenticateResponseBadUserNameAndPassBody.contains("authentication failed"));
    }

    @Test
    public void authenticateAndIssueTokenThrowsExceptionOnBadCredentialsWhenNoUserExists() {
        // we dont want to signal different authentication failures to any potential attacker,
        // a complete failure to authenticate perfectly should respond the same to a partial failure
        // to authenticate
        User user = new User();
        user.setUserName(testUserName);
        user.setPassword(testPassword);

        // we do not create the user as part of test setup in this case

        String authenticateUrl = baseUrl + "/authenticate";

        ResponseEntity<String> authenticateResponseBadPass = restClient.post()
                .uri(authenticateUrl)
                .body(user)
                .exchange((request, response) -> {
                    String body = response.bodyTo(String.class);
                    return ResponseEntity.status(response.getStatusCode()).body(body);
                });

        assertEquals(HttpStatus.FORBIDDEN, authenticateResponseBadPass.getStatusCode());
        String authenticateResponseBadPassBody = authenticateResponseBadPass.getBody();
        assert authenticateResponseBadPassBody != null;
        assertTrue(authenticateResponseBadPassBody.contains("authentication failed"));
    }
}
