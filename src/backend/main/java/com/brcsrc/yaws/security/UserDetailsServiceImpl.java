package com.brcsrc.yaws.security;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // TODO inject via constructor
    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt = this.userRepository.findByUserName(username);
        if (userOpt.isEmpty()) {
            String errMsg = "could not find user by username";
            logger.error(errMsg);
            throw new UsernameNotFoundException(errMsg);
        }
        User user = userOpt.get();
        // users table intended to only have one row for the admin user by id 1L
        if (!Objects.equals(user.getId(), Constants.ADMIN_USER_ID)) {
            logger.error("admin user exists but was not the expected row or id");
            throw new InternalServerException("unexpected error in getting user");
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserName())
                .password(user.getPassword())
                .build();
    }
}
