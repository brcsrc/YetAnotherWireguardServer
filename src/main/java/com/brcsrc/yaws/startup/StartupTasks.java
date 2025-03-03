package com.brcsrc.yaws.startup;

import com.brcsrc.yaws.exceptions.InternalServerException;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.persistence.NetworkRepository;
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
    private static final Logger logger = LoggerFactory.getLogger(StartupTasks.class);

    @Autowired
    public StartupTasks(NetworkRepository networkRepository) {
        this.networkRepository = networkRepository;
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
            throw new InternalServerException("restarkActiveNetworks ran into an error");
        }
    }
}
