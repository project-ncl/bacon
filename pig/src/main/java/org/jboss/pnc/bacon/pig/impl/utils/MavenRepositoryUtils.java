package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.jboss.pnc.bacon.pig.impl.repo.RepoDescriptor.MAVEN_REPOSITORY;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
public class MavenRepositoryUtils {

    public static Path getContentsDirPath(Path unzippedRepoPath) {
        try {
            List<Path> topLevelDirs = Files.list(unzippedRepoPath)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
            if (topLevelDirs.size() != 1) {
                throw new RuntimeException(
                        "Expecting a single parent directory in the unzipped maven repo, found: " + topLevelDirs);
            }

            Optional<Path> maybeContentsDir = Files.list(topLevelDirs.get(0))
                    .filter(p -> p.getFileName().toString().equals(MAVEN_REPOSITORY.replace("/", "")))
                    .findAny();

            return maybeContentsDir.orElseThrow(
                    () -> new RuntimeException(
                            "Failed to find maven-repository directory in the unzipped maven repo: "
                                    + unzippedRepoPath));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to get the contents directory for maven repo unzipped to: " + unzippedRepoPath);
        }
    }

    private MavenRepositoryUtils() {
    }
}