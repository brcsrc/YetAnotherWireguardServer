package com.brcsrc.yaws.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ExecutorTests {
    @Test
    void testExecutorReturnsStdout() {
        ExecutionResult result = Executor.runCommand("which uname");
        Assertions.assertTrue(result.getStdout().contains("/bin/uname"));
        Assertions.assertEquals(0, result.getExitCode());
        Assertions.assertEquals("", result.getStderr());
    }

    @Test
    void testExecutorReturnsExitCode() {
        ExecutionResult result = Executor.runCommand("which uname");
        Assertions.assertEquals(0, result.getExitCode());
    }

    @Test
    void testExecutorReturnsStderr() {
        ExecutionResult result = Executor.runCommand("/bin/bash -c notacommand");
        Assertions.assertEquals("", result.getStdout());
        Assertions.assertNotEquals(0, result.getExitCode());
        Assertions.assertTrue(result.getStderr().contains("command not found"));
    }
}
