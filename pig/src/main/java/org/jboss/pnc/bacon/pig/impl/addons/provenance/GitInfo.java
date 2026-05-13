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
package org.jboss.pnc.bacon.pig.impl.addons.provenance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to retrieve Git repository information from the current working directory.
 * This is used to capture the configuration repository context when the Bacon CLI is executed.
 */
@Slf4j
public final class GitInfo {

    private final String repositoryUrl;
    private final String commitHash;

    private GitInfo(String repositoryUrl, String commitHash) {
        this.repositoryUrl = repositoryUrl;
        this.commitHash = commitHash;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getCommitHash() {
        return commitHash;
    }

    /**
     * Attempts to retrieve Git repository information from the specified directory.
     *
     * @param workingDirectory the directory to check for git repository information
     * @return Optional containing GitInfo if successful, empty otherwise
     */
    public static Optional<GitInfo> fromDirectory(Path workingDirectory) {
        try {
            String repoUrl = executeGitCommand(workingDirectory, "git", "config", "--get", "remote.origin.url");
            String commitHash = executeGitCommand(workingDirectory, "git", "rev-parse", "HEAD");

            if (repoUrl != null && !repoUrl.isEmpty() && commitHash != null && !commitHash.isEmpty()) {
                log.debug("Retrieved git info from {}: repo={}, commit={}", workingDirectory, repoUrl, commitHash);
                return Optional.of(new GitInfo(repoUrl.trim(), commitHash.trim()));
            } else {
                log.debug("Git repository information incomplete in {}", workingDirectory);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.debug("Failed to retrieve git information from {}: {}", workingDirectory, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Attempts to retrieve Git repository information from the current working directory.
     *
     * @return Optional containing GitInfo if successful, empty otherwise
     */
    public static Optional<GitInfo> fromCurrentDirectory() {
        return fromDirectory(Path.of(System.getProperty("user.dir")));
    }

    /**
     * Executes a git command and returns the output.
     *
     * @param workingDir the directory to execute the command in
     * @param command the command and arguments to execute
     * @return the command output, or null if the command failed
     */
    private static String executeGitCommand(Path workingDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Git command timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.trace("Git command failed with exit code {}: {}", exitCode, output.toString().trim());
            return null;
        }

        return output.toString().trim();
    }

    @Override
    public String toString() {
        return "GitInfo{repositoryUrl='" + repositoryUrl + "', commitHash='" + commitHash + "'}";
    }
}
