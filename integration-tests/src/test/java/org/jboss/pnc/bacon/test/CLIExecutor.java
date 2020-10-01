package org.jboss.pnc.bacon.test;

import org.jboss.pnc.bacon.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * @author jbrazdil
 */
public final class CLIExecutor {
    private static final Logger log = LoggerFactory.getLogger(CLIExecutor.class);

    private static final Path BACON_JAR = Paths.get("..", "cli", "target", "bacon.jar").toAbsolutePath().normalize();

    public static final Path CONFIG_LOCATION = Paths.get("target", "test-config");

    private CLIExecutor() {

    }

    public static ExecutionResult runCommand(String... args) {
        try {
            checkExecutable();

            String[] cmdarray = new String[args.length + 3];
            cmdarray[0] = "java";
            cmdarray[1] = "-jar";
            cmdarray[2] = BACON_JAR.toString();
            System.arraycopy(args, 0, cmdarray, 3, args.length);
            String[] env = { Constant.CONFIG_ENV + "=" + CONFIG_LOCATION };

            System.out.println(
                    "Running command: " + Arrays.stream(cmdarray).collect(Collectors.joining("' '", "'", "'"))
                            + "\n\twith env " + Arrays.toString(env));
            Process process = Runtime.getRuntime().exec(cmdarray, env);

            CompletableFuture<String> output = CompletableFuture
                    .supplyAsync(() -> readInputStream(process.getInputStream()));
            CompletableFuture<String> error = CompletableFuture
                    .supplyAsync(() -> readInputStream(process.getErrorStream()));

            process.waitFor(10L, TimeUnit.MINUTES);
            return new ExecutionResult(output.get(), error.get(), process.exitValue());
        } catch (IOException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error while executing command.", ex);
        }
    }

    private static void checkExecutable() {
        if (!Files.isReadable(BACON_JAR)) {
            throw new IllegalStateException("Can't find or read bacon jar at " + BACON_JAR);
        }
    }

    private static String readInputStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Error closing buffered reader", e);
        }
        // For test debuging purposes, you can print the process stdin and stdout:
        // return reader.lines().peek(s -> System.out.println(">>>" + s)).collect(Collectors.joining("\n"));
    }
}
