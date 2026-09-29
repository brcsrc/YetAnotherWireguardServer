package com.brcsrc.yaws.shell;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of CommandExecutor that delegates to the static Executor class.
 */
@Component
public class DefaultCommandExecutor implements CommandExecutor {
    @Override
    public ExecutionResult runCommand(String command) {
        return Executor.runCommand(command);
    }

    @Override
    public ExecutionResult runCommand(List<String> command) {
        return Executor.runCommand(command);
    }

    @Override
    public ExecutionResult runCommandWithInput(List<String> command, String stdin) {
        return Executor.runCommandWithInput(command, stdin);
    }
}
