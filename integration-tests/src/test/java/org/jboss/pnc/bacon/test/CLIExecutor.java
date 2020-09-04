/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.test;

import org.jboss.pnc.bacon.common.Constant;

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
public class CLIExecutor {
    private static final Path BACON_JAR = Paths.get("..", "cli", "target", "bacon.jar").toAbsolutePath().normalize();
    public static final Path CONFIG_LOCATION = Paths.get("target", "test-config");

    public ExecutionResult runCommand(String... args) {
        try {
            checkExecutable();

            String[] cmdarray = new String[args.length + 3];
            cmdarray[0] = "java";
            cmdarray[1] = "-jar";
            cmdarray[2] = BACON_JAR.toString();
            System.arraycopy(args, 0, cmdarray, 3, args.length);
            String[] env = { Constant.CONFIG_ENV + "=" + CONFIG_LOCATION };

            System.out.println(
                    "Running command: " + Arrays.stream(cmdarray).collect(Collectors.joining("' '", "'", "'")));
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

    private void checkExecutable() {
        if (!Files.exists(BACON_JAR) || !Files.isReadable(BACON_JAR)) {
            throw new IllegalStateException("Can't find bacon executable at " + BACON_JAR);
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
