package com.brcsrc.yaws.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ExecutorTests {
    @Test
    void testExecutorReturnsStdout(){
        ExecutionResult result = Executor.runCommand("which uname");
        Assertions.assertTrue(result.stdout.startsWith("/bin/uname"));
    }

    @Test
    void testExecutorReturnsExitCode(){
        ExecutionResult result = Executor.runCommand("which uname");
        Assertions.assertEquals(0, result.exitCode);
    }
}
