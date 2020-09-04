/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.pig.impl.utils;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * todo: clean up + possibly move out to a separate lib TODO: remove !!
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/2/17
 */
public class OSCommandExecutor {
    private static final Logger log = LoggerFactory.getLogger(OSCommandExecutor.class);

    public static List<String> runCommand(String s) {
        return runCommandIn(s, null);
    }

    public static List<String> runCommandIn(String command, Path directory) {
        return executor(command).directory(directory)
                .redirectErrorStream(true)
                .failOnInvalidStatusCode()
                .exec()
                .getOut();
    }

    public static CommandExecutor executor(String command) {
        return new CommandExecutor(command);
    }

    @Getter
    public static class CommandExecutor {
        private final String command;
        private Path directory;
        private boolean failOnStatusCode = false;
        private boolean printOutputInOneLine = false;
        private int status = -1;
        private boolean timedOut = false;
        private int attempts = 1;
        private List<String> out = new ArrayList<>();
        private String outputFile = null;
        private boolean redirectErrorStream;
        private Integer timeout;

        public CommandExecutor(String command) {
            this.command = command;
        }

        /**
         * split by spaces, except for elements in double quotes
         *
         * @param command e.g. docker run prod-docs "ls -la"
         * @return e.g. ["docker", "run", "prod-docs", "ls -la"
         */
        private String[] splitCommand(String command) {
            List<String> result = new ArrayList<>();
            String[] splitByQuote = command.split("(?<!\\\\)\"");
            for (int i = 0; i < splitByQuote.length; i++) {
                String commandPart = splitByQuote[i].trim();
                if (i % 2 == 0) {
                    if (!commandPart.isEmpty()) {
                        List<String> parameters = asList(commandPart.split("\\s+"));
                        result.addAll(parameters);
                    }
                } else {
                    result.add(commandPart);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        public CommandExecutor directory(Path directory) {
            if (directory != null) {
                this.directory = directory;
            }
            return this;
        }

        public CommandExecutor failOnInvalidStatusCode() {
            failOnStatusCode = true;
            return this;
        }

        public CommandExecutor redirectErrorStream(boolean redirect) {
            redirectErrorStream = redirect;
            return this;
        }

        public CommandExecutor exec() {
            String command = prepareCommand(this.command);
            log.debug(
                    "will execute {}, execution directory {}",
                    command,
                    directory != null ? directory.toAbsolutePath().toString() : null);
            ProcessBuilder builder = new ProcessBuilder(unescape(splitCommand(command)));
            try {
                do {
                    out.clear();
                    if (directory != null) {
                        builder.directory(directory.toFile());
                    }
                    builder.redirectErrorStream(redirectErrorStream);
                    log.debug("Invoking command {}", builder.command());
                    Process process = builder.start();

                    CompletableFuture<Boolean> processExitWaiter = waitFor(process);
                    if (printOutputInOneLine) {
                        writeToStdoutInOneLine(process.getInputStream());
                    } else {
                        read(process.getInputStream(), out);
                    }
                    timedOut = processExitWaiter.get();

                    if (!timedOut) {
                        status = process.exitValue();
                        if (outputFile != null) {
                            outputToFile();
                        }
                        if (status != 0) {
                            log.debug("Command {} failed, will reattempt: {}", builder.command(), attempts > 0);
                            if (!redirectErrorStream) {
                                readSafely(process.getErrorStream(), out);
                            }
                        }
                    }
                    attempts--;

                } while ((timedOut || status != 0) && attempts > 0);
                if (failOnStatusCode && status != 0) {
                    log.error(
                            "Failed to execute command {}. Status code: {}. Process output: {}",
                            builder.command(),
                            status,
                            joinedOutput());
                    throw new OSCommandException(
                            "Failed to execute command " + builder.command() + ". Status code: " + status
                                    + " Process output: " + joinedOutput());
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                log.error("Failed to execute command {}. Process output: {}", builder.command(), joinedOutput(), e);
                throw new OSCommandException(
                        "Failed to execute command " + builder.command() + ". Process output: " + joinedOutput(),
                        e);
            }
            return this;
        }

        private void readSafely(InputStream errorStream, List<String> out) {
            try {
                read(errorStream, out);
            } catch (IOException e) {
                log.info("Error reading process output for {}, continuing", command, e);
            }
        }

        private CompletableFuture<Boolean> waitFor(Process process) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    if (timeout != null) {
                        boolean finishedOnTime = process.waitFor(timeout, TimeUnit.SECONDS);
                        if (!finishedOnTime) {
                            log.debug("Process timed out, destroying");
                            process.destroyForcibly();
                            log.debug("Destroyed");
                            return true;
                        }
                    } else {
                        process.waitFor();
                    }
                    return false;
                } catch (InterruptedException e) {
                    log.error("Failed to wait for process {}", command, e);
                    return true;
                }
            });
        }

        private void outputToFile() throws IOException {
            File outFile = new File(outputFile);
            if (!outFile.createNewFile()) {
                throw new OSCommandException("Could not create output file " + outputFile);
            }
            try (FileWriter writer = new FileWriter(outFile)) {
                writer.append(joinedOutput());
            }
        }

        private static String[] unescape(String[] input) {
            return Stream.of(input).map(CommandExecutor::stripQuoteMarks).toArray(String[]::new);
        }

        private static String stripQuoteMarks(String s) {
            for (String quoteMark : asList("\"", "'")) {
                if (s.startsWith(quoteMark) && s.endsWith(quoteMark)) {
                    s = s.substring(1, s.length() - 1);
                }
            }

            s = s.replaceAll("\\\\\"", "\"");
            s = s.replaceAll("\\\\'", "'");

            return s;
        }

        private static void consumeInputStream(InputStream inputStream, Consumer<String> consumer) throws IOException {
            try (InputStreamReader streamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(streamReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    consumer.accept(line);
                }
            }
        }

        private static void writeToStdoutInOneLine(InputStream inputStream) throws IOException {
            consumeInputStream(inputStream, l -> System.out.print(l + "\r"));
        }

        private static void read(InputStream inputStream, List<String> out) throws IOException {
            consumeInputStream(inputStream, out::add);
        }

        public CommandExecutor toFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public String joinedOutput() {
            return join(out, "\n");
        }

        public CommandExecutor printOutputInOneLine() {
            printOutputInOneLine = true;
            return this;
        }

        public CommandExecutor retrying(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public CommandExecutor timeout(int seconds) {
            timeout = seconds;
            return this;
        }

        protected String prepareCommand(String command) {
            return command;
        }
    }

    private OSCommandExecutor() {
    }
}
