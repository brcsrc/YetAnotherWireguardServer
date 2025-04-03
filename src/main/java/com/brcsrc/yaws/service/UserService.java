package com.brcsrc.yaws.service;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.responses.AuthenticationResponse;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.security.JwtService;
import com.brcsrc.yaws.security.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsServiceImpl userDetailsService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public AuthenticationResponse authenticateAndIssueToken(@RequestBody User user) {
        logger.info("got AuthenticateAndIssueToken request");
        // TODO validate input fields
        // if the user is not found when attempting to authenticate, a BadCredentialsException is
        // thrown and is set in GlobalExceptionHandler to throw a 403
        Authentication authentication  = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUserName(),    // principal is the username
                        user.getPassword()     // credentials is the password
                )
        );
        // if authentication failed for any other reason then we should handle and
        // return a 401 for user being unauthenticated
        if (!authentication.isAuthenticated()) {
            String errMsg = "authentication failed for user: " + user.getUserName();
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errMsg);
        }

        final String jwt = this.jwtService.generateToken(this.userDetailsService.loadUserByUsername(user.getUserName()));
        return new AuthenticationResponse(jwt);
    }

    protected static boolean passwordMeetsComplexityRequirements(String requestedPassword) {
        int lowercaseAlphaChars = 0;
        int upperCaseAlphaChars = 0;
        int specialChars = 0;
        int numberChars = 0;
        int unmatchedChars = 0;

        int minLowercaseChars = 2;
        int minUpperCaseChars = 2;
        int minSpecialChars = 1;
        int minNumberChars = 1;
        int maxUnmatchedChars = 0;

        int passwordLength = requestedPassword.length();

        for (int i = 0; i < passwordLength; i++) {
            Character c = requestedPassword.charAt(i);

            if (Character.isUpperCase(c)) {
                upperCaseAlphaChars++;
                continue;
            } else if (Character.isLowerCase(c)) {
                lowercaseAlphaChars++;
                continue;
            } else if (Character.isDigit(c)) {
                numberChars++;
                continue;
            } else if (Constants.ADMIN_USER_PASSWORD_ALLOWED_SPECIAL_CHARS.contains(c.toString())) {
                specialChars++;
                continue;
            }
            // if we get to this block then the char is unmatched and we dont want it
            unmatchedChars++;
        }

        boolean meetsComplexityRequirements = (
            lowercaseAlphaChars >= minLowercaseChars &&
            upperCaseAlphaChars >= minUpperCaseChars &&
            specialChars >= minSpecialChars &&
            numberChars >= minNumberChars &&
            unmatchedChars <= maxUnmatchedChars &&
            passwordLength >= Constants.ADMIN_USER_PASSWORD_MIN_LENGTH
        );

        return meetsComplexityRequirements;
    }

    @Transactional
    public User createAdminUser(User newUser) {
        // input validation
        if (!newUser.getUserName().matches(Constants.CHAR_32_ALPHANUMERIC_DASHES_UNDERSC_REGEX)) {
            String errMsg = "user name must be 4-32 characters alphanumeric with dashes or underscores and with no spaces";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        if (newUser.getPassword().length() < Constants.ADMIN_USER_PASSWORD_MIN_LENGTH) {
            String errMsg = "password must be at least 12 characters long";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        if (!passwordMeetsComplexityRequirements(newUser.getPassword())) {
            String errMsg = "password must have 2 lowercase letters, 2 uppercase letters, 1 special character and 1 number";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // if there already is an admin user we reject the registration request
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isPresent()) {
            String errMsg = "cannot create admin user, admin user already exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        User user = new User();
        user.setId(Constants.ADMIN_USER_ID);
        user.setUserName(newUser.getUserName());

        // bcrypt the password so it is stored not in plaintext
        user.setPassword(passwordEncoder.encode(newUser.getPassword()));
        User savedUser = userRepository.save(user);
        logger.info("successfully created admin user");
        return savedUser;
    }

    @Transactional
    public User updateAdminUserName(String newUserName) {
        // TODO validate new user name input
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isEmpty()) {
            String errMsg = "cannot update admin user name, no admin user exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        User adminUser = adminUserOpt.get();
        adminUser.setUserName(newUserName);
        return userRepository.save(adminUser);
    }

    @Transactional
    public User updateAdminUserPassword(String newPassword) {
        // TODO validate new password input
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isEmpty()) {
            String errMsg = "cannot update admin password, no admin user exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        // TODO create complexity requirements for password
        User adminUser = adminUserOpt.get();
        adminUser.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(adminUser);
        logger.info("successfully successfully updated admin user password");
        return savedUser;
    }

    public User getAdminUser() {
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isEmpty()) {
            String errMsg = "cannot get admin user, no admin user exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errMsg);
        }
        return adminUserOpt.get();
    }
}

