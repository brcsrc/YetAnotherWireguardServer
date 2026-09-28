package com.brcsrc.yaws.startup;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.service.PeerIsolationService;
import com.brcsrc.yaws.service.UserService;
import com.brcsrc.yaws.service.WireguardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupTasks {
    private final NetworkRepository networkRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final WireguardService wireguardService;
    private final PeerIsolationService peerIsolationService;
    private static final Logger logger = LoggerFactory.getLogger(StartupTasks.class);

    @Autowired
    public StartupTasks(NetworkRepository networkRepository,
                        UserRepository userRepository,
                        UserService userService,
                        WireguardService wireguardService,
                        PeerIsolationService peerIsolationService) {
        this.networkRepository = networkRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.wireguardService = wireguardService;
        this.peerIsolationService = peerIsolationService;
    }

    public void registerAdminUserFromEnv() {
        String username = System.getenv("YAWS_ADMIN_USERNAME");
        String password = System.getenv("YAWS_ADMIN_PASSWORD");

        if (username == null || password == null) {
            logger.info("YAWS_ADMIN_USERNAME or YAWS_ADMIN_PASSWORD not set, skipping env-based admin registration");
            return;
        }

        if (userRepository.findById(Constants.ADMIN_USER_ID).isPresent()) {
            logger.info("admin user already exists, skipping env-based admin registration");
            return;
        }

        logger.info("registering admin user from environment variables");
        User user = new User();
        user.setUserName(username);
        user.setPassword(password);
        userService.createAdminUser(user);
        logger.info("admin user registered successfully from environment variables");
    }

    @Async
    public void restartActiveNetworks() {
        logger.info("restartActiveNetworks called, finding existing active networks to restart");
        List<Network> activeNetworks = this.networkRepository.findAllByNetworkStatus(NetworkStatus.ACTIVE);
        logger.info(String.format("found %s active networks to restart", activeNetworks.size()));
        boolean errorsOnActivate = false;

        for (Network network : activeNetworks) {
            logger.info(String.format("activating existing network '%s'", network.getNetworkName()));
            try {
                this.wireguardService.interfaceUp(network.getNetworkName());
                // the isolation chain is created empty by the interface hooks, so policy has to be
                // reapplied from the database here too. without this, isolation would silently
                // turn itself off across a container restart
                this.peerIsolationService.applyNetworkPolicy(network);
            } catch (RuntimeException e) {
                errorsOnActivate = true;
                logger.error("failed to restart network '{}': {}",
                        network.getNetworkName(), e.getMessage());
            }
        }
        if (errorsOnActivate) {
            throw new InternalServerException("restartActiveNetworks ran into an error");
        }
    }
}
