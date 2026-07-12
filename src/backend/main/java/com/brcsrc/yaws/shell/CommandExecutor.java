package com.brcsrc.yaws.shell;

/**
 * Interface for executing shell commands. Allows for mocking in tests.
 */
public interface CommandExecutor {
    ExecutionResult runCommand(String command);
}
