package com.brcsrc.yaws.shell;

import java.util.List;

/**
 * Interface for executing shell commands. Allows for mocking in tests.
 *
 * only runCommand(String) is abstract so this stays a functional interface and can be
 * implemented as a lambda in tests. the list and stdin forms default to delegating to it,
 * which implementations that need them should override.
 */
@FunctionalInterface
public interface CommandExecutor {
    ExecutionResult runCommand(String command);

    default ExecutionResult runCommand(List<String> command) {
        return runCommand(String.join(" ", command));
    }

    default ExecutionResult runCommandWithInput(List<String> command, String stdin) {
        return runCommand(command);
    }
}
