package com.brcsrc.yaws.startup;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.service.UserService;
import com.brcsrc.yaws.shell.ExecutionResult;
import com.brcsrc.yaws.shell.Executor;
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
    private static final Logger logger = LoggerFactory.getLogger(StartupTasks.class);

    @Autowired
    public StartupTasks(NetworkRepository networkRepository, UserRepository userRepository, UserService userService) {
        this.networkRepository = networkRepository;
        this.userRepository = userRepository;
        this.userService = userService;
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
            final String activateNetworkInterfaceCommand = String.format("wg-quick up %s", network.getNetworkName());
            ExecutionResult activateResult = Executor.runCommand(activateNetworkInterfaceCommand);
            if (activateResult.getExitCode() != 0) {
                errorsOnActivate = true;
                logger.error(String.format(
                        "command: '%s' exited %s with reason: %s",
                        activateNetworkInterfaceCommand,
                        activateResult.getExitCode(),
                        activateResult.getStderr()));
            }
        }
        if (errorsOnActivate) {
            throw new InternalServerException("restartActiveNetworks ran into an error");
        }
    }
}
