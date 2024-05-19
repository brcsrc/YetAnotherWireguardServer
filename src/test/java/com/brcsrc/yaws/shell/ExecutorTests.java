package com.brcsrc.yaws.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ExecutorTests {

    // test suite requires wireguard to be installed on the system
    @Test
    void testWgInstalled(){
        ExecutionResult result = Executor.runCommand("which wg");
        Assertions.assertEquals(0, result.exitCode);
        result = Executor.runCommand("wg --help");
        Assertions.assertEquals(0, result.exitCode);
        Assertions.assertTrue(result.stdout.startsWith("Usage: wg <cmd> [<args>]Available subcommands:  show: Shows the current configuration and device information"));
    }
}
