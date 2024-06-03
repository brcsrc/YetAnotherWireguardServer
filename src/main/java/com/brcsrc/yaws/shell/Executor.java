package com.brcsrc.yaws.shell;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Executor {

    public static ExecutionResult runCommand(String command) {
        StringBuilder stdout = new StringBuilder();
        int exitCode = 1;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                stdout.append(line).append("\n");
            }
            exitCode = process.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ExecutionResult(stdout.toString(), exitCode);
    }
}



