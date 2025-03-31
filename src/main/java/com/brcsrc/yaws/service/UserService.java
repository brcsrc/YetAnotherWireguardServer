package com.brcsrc.yaws.service;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
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

    public String authenticateAndIssueToken(@RequestBody User user) {
        logger.info("got AuthenticateAndIssueToken request");
        // TODO validate input fields
        Authentication authentication = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUserName(),    // principal is the username
                        user.getPassword()     // credentials is the password
                )
        );
        if (authentication.isAuthenticated()) {
            return this.jwtService.generateToken(this.userDetailsService.loadUserByUsername(user.getUserName()));
        } else {
            String errMsg = "unable to get UserDetails for user in authentication request";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
    }

    @Transactional
    public User createAdminUser(User newUser) {
        // TODO validate inputs
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

