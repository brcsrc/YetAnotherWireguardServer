package com.brcsrc.yaws.shell;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the outcome of running a system command: what it wrote and how it exited.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    private String stdout;
    private String stderr;
    private int exitCode;
}
