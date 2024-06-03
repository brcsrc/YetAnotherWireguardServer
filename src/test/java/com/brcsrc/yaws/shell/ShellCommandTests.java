package com.brcsrc.yaws.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ShellCommandTests {
    @Test
    void testShellTestSetup(){
        ExecutionResult result = Executor.runCommand("./test-setup");
        Assertions.assertEquals(0, result.exitCode);
    }
}