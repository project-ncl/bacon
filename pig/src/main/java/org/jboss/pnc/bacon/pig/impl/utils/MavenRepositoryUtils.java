package org.jboss.pnc.bacon.pig.impl.utils;

import lombok.experimental.UtilityClass;
import org.jboss.pnc.bacon.pig.impl.repo.RepoDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 13/08/2019
 */
@UtilityClass
public class MavenRepositoryUtils {
    public Path getContentsDirPath(Path unzippedRepoPath) {
        try (Stream<Path> unzippedRepoStream = Files.list(unzippedRepoPath)) {
            List<Path> topLevelDirs = unzippedRepoStream.filter(Files::isDirectory).collect(Collectors.toList());
            if (topLevelDirs.size() != 1) {
                throw new RuntimeException(
                        "Expecting a single parent directory in the unzipped maven repo, found: " + topLevelDirs);
            }

            try (Stream<Path> topLevelDirsStream = Files.list(topLevelDirs.get(0))) {
                Optional<Path> maybeContentsDir = topLevelDirsStream.filter(
                        p -> p.getFileName().toString().equals(RepoDescriptor.MAVEN_REPOSITORY.replace("/", "")))
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
}
