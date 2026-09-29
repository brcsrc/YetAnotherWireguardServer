package com.brcsrc.yaws.shell;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class Executor {

    // commands are system calls to wg/iptables and should return promptly. a command that
    // exceeds this is considered hung and is destroyed so it cannot hold a request thread
    private static final long COMMAND_TIMEOUT_SECONDS = 60;

    private static String readStdFromStream(InputStream inputStream) throws IOException {
        StringBuilder std = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            std.append(line).append("\n");
        }
        return std.toString();
    }

    /**
     * runs a command given as a single string, split on whitespace. retained for existing
     * call sites. arguments that contain whitespace cannot be expressed this way, use
     * {@link #runCommand(List)} for those.
     */
    public static ExecutionResult runCommand(String command) {
        return runCommand(List.of(command.split("\\s+")));
    }

    /**
     * runs a command given as an already separated argument list. no shell is involved so
     * arguments are passed to the process verbatim and may contain whitespace.
     */
    public static ExecutionResult runCommand(List<String> command) {
        return runCommandWithInput(command, null);
    }

    /**
     * runs a command, writing stdin to the process before reading its output. used for commands
     * that read from stdin, such as 'wg pubkey' and 'wg syncconf'. a null stdin closes the
     * stream immediately.
     */
    public static ExecutionResult runCommandWithInput(List<String> command, String stdin) {
        String stdout = "";
        String stderr = "";
        int exitCode = 1;

        // stdout and stderr must be drained concurrently. reading one to completion before
        // the other lets the child block writing to a full pipe buffer while we block reading
        // the stream it is not writing to, which deadlocks both processes
        ExecutorService streamReaders = Executors.newFixedThreadPool(2);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            Future<String> stdoutFuture = streamReaders.submit(() -> readStdFromStream(process.getInputStream()));
            Future<String> stderrFuture = streamReaders.submit(() -> readStdFromStream(process.getErrorStream()));

            // stdin is written after the readers are started so a process that writes output
            // while still reading input cannot block us
            if (stdin != null) {
                try (OutputStream processInput = process.getOutputStream()) {
                    processInput.write(stdin.getBytes(StandardCharsets.UTF_8));
                    processInput.flush();
                }
            } else {
                process.getOutputStream().close();
            }

            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                stderr = String.format(
                        "command timed out after %s seconds and was terminated",
                        COMMAND_TIMEOUT_SECONDS);
                return new ExecutionResult(stdout, stderr, exitCode);
            }

            stdout = stdoutFuture.get();
            stderr = stderrFuture.get();
            exitCode = process.exitValue();
        } catch (IOException | InterruptedException | java.util.concurrent.ExecutionException e) {
            e.printStackTrace();
        } finally {
            streamReaders.shutdownNow();
        }
        return new ExecutionResult(stdout, stderr, exitCode);
    }
}
