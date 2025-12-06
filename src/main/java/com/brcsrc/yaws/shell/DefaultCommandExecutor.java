package com.brcsrc.yaws.shell;

import org.springframework.stereotype.Component;

/**
 * Default implementation of CommandExecutor that delegates to the static Executor class.
 */
@Component
public class DefaultCommandExecutor implements CommandExecutor {
    @Override
    public ExecutionResult runCommand(String command) {
        return Executor.runCommand(command);
    }
}
