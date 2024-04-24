package com.brcsrc.yaws.shell;

public class ExecutionResult {
    String stdout;
    int exitCode;
    public ExecutionResult(String stdout, int exitCode) {
        this.stdout = stdout;
        this.exitCode = exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
                "stdout='" + stdout + '\'' +
                ", exitCode=" + exitCode +
                '}';
    }
}
