package com.brcsrc.yaws.shell;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Executor {

    private static String readStdFromStream(InputStream inputStream) throws IOException {
        StringBuilder std = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            std.append(line).append("\n");
        }
        return std.toString();
    }

    public static ExecutionResult runCommand(String command) {
        String stdout = "";
        String stderr = "";
        int exitCode = 1;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            Process process = processBuilder.start();
            stdout = readStdFromStream(process.getInputStream());
            stderr = readStdFromStream(process.getErrorStream());
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ExecutionResult(stdout, stderr, exitCode);
    }
}



