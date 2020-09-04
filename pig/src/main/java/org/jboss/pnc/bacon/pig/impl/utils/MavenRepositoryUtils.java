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
package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jboss.pnc.bacon.pig.impl.repo.RepoDescriptor.MAVEN_REPOSITORY;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
public class MavenRepositoryUtils {

    public static Path getContentsDirPath(Path unzippedRepoPath) {
        try (Stream<Path> unzippedRepoStream = Files.list(unzippedRepoPath)) {
            List<Path> topLevelDirs = unzippedRepoStream.filter(Files::isDirectory).collect(Collectors.toList());
            if (topLevelDirs.size() != 1) {
                throw new RuntimeException(
                        "Expecting a single parent directory in the unzipped maven repo, found: " + topLevelDirs);
            }

            try (Stream<Path> topLevelDirsStream = Files.list(topLevelDirs.get(0))) {
                Optional<Path> maybeContentsDir = topLevelDirsStream
                        .filter(p -> p.getFileName().toString().equals(MAVEN_REPOSITORY.replace("/", "")))
                        .findAny();

                return maybeContentsDir.orElseThrow(
                        () -> new RuntimeException(
                                "Failed to find maven-repository directory in the unzipped maven repo: "
                                        + unzippedRepoPath));
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to get the contents directory for maven repo unzipped to: " + unzippedRepoPath,
                    e);
        }
    }

    private MavenRepositoryUtils() {
    }
}
