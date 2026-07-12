package com.brcsrc.yaws.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ExecutorTests {
    @Test
    void testExecutorReturnsStdout() {
        ExecutionResult result = Executor.runCommand("which uname");
        Assertions.assertTrue(result.stdout.contains("/bin/uname"));
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertEquals("", result.stderr);
    }

    @Test
    void testExecutorReturnsExitCode() {
        ExecutionResult result = Executor.runCommand("which uname");
        Assertions.assertEquals(0, result.exitCode);
    }

    @Test
    void testExecutorReturnsStderr() {
        ExecutionResult result = Executor.runCommand("/bin/bash -c notacommand");
        Assertions.assertEquals("", result.stdout);
        Assertions.assertNotEquals(0, result.exitCode);
        Assertions.assertTrue(result.stderr.contains("command not found"));
    }
}
