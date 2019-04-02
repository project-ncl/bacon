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
package org.jboss.pnc.bacon.pig.impl.repo;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;


/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 1/18/18
 */
public class RepositoryUtils {
    public static final Logger log = LoggerFactory.getLogger(RepositoryUtils.class);

    public static void addCheckSums(File mavenRepositoryDirectory) {
        log.debug("Generating missing checksums");
        try {
            Files.walk(mavenRepositoryDirectory.toPath())
                    .filter(p -> p.toFile().isFile())
                    .filter(RepositoryUtils::isNotCheckSumFile)
                    .forEach(RepositoryUtils::addCheckSums);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to generate checksums for " + mavenRepositoryDirectory.getAbsolutePath(), e);
        }
    }

    private static boolean isNotCheckSumFile(Path path) {
        return Stream.of(RepoDescriptor.CHECKSUM_EXTENSIONS)
                .noneMatch(ext -> path.toString().endsWith(ext));
    }

    protected static void addCheckSums(Path filePath) {
        generateCheckSum(filePath, "md5sum", ".md5");
        generateCheckSum(filePath, "sha1sum", ".sha1");
    }

    private static void generateCheckSum(Path filePath, String checksumType, String extension) {
        String file = filePath.toAbsolutePath().toString();
        File checkSumFile = new File(file + extension);

        if (checkSumFile.exists()) {
            return;
        }

        try {
            String checksum;

            try (FileInputStream data = new FileInputStream(file)) {
                switch (checksumType) {
                case "md5sum":
                    checksum = DigestUtils.md5Hex(data);
                    break;
                case "sha1sum":
                    checksum = DigestUtils.sha1Hex(data);
                    break;
                default:
                    throw new IOException("Unknown checksum type: " + checksumType);
                }
            }

            FileUtils.write(checkSumFile, checksum, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create checksum file: " + checkSumFile.getAbsolutePath());
        }
    }

    public static void removeIrrelevantFiles(File element) {
        log.debug("removing internal maven files from the repository");
        removeMatchingCondition(element, file -> file.getName().equals("_remote.repositories"));
        removeMatchingCondition(element, file -> file.getName().endsWith(".lastUpdated"));
    }

    public static void removeCommunityArtifacts(File element) {
        log.debug("removing community dependencies from the repository");
        removeMatchingCondition(element, RepositoryUtils::isCommunity);
    }

    private static boolean isCommunity(File f) {
        String absolutePath = f.getAbsolutePath();
        return !absolutePath.contains("redhat-") && !absolutePath.contains("eap-runtime-artifacts");
    }


    private static void removeMatchingCondition(File element, Function<File, Boolean> condition) {
        if (element.isDirectory()) {
            Stream.of(element.listFiles())
                    .forEach(file -> removeMatchingCondition(file, condition));
            if (element.listFiles().length == 0) {
                element.delete();
            }
        } else {
            if (condition.apply(element)) {
                element.delete();
            }
        }
    }

    public static void keepOnlyLatestRedHatArtifacts(File mavenRepositoryDirectory) throws IOException {
        Files.walkFileTree(mavenRepositoryDirectory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                final File[] children = dir.toFile().listFiles();
                if(null == children) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                final List<File> redHatFiles =
                    Stream.of(children).filter(f -> !isCommunity(f)).collect(Collectors.toList());

                if(redHatFiles.isEmpty()) {
                    // continue since we are at an intermediate directory
                    return FileVisitResult.CONTINUE;
                }

                redHatFiles
                    .stream()
                    .map(f -> new AbstractMap.SimpleEntry<>(f, RedHatArtifactVersion.fromVersion(f.getName())))
                    // split by unique upstream version
                    .collect(Collectors.groupingBy(e -> e.getValue().getUpstreamVersion()))
                    // for each unique upstream version, delete the directories corresponding
                    // to redhat version less than the highest version
                    .forEach((upstreamVersion, matchingRHArtifactVersions) -> {
                      matchingRHArtifactVersions.sort(
                          comparingInt(e -> e.getValue().getRedhatBuildNumber())
                      );
                      Collections.reverse(matchingRHArtifactVersions);

                      matchingRHArtifactVersions.stream().skip(1).forEach(e -> {
                        final File directoryToBeDeleted = e.getKey();

                        log.info("Deleting redhat artifact " + directoryToBeDeleted.getAbsolutePath()
                            + " which is redundant since a newer redhat version exists");

                        try {
                          FileUtils.deleteDirectory(directoryToBeDeleted);
                        } catch (IOException e1) {
                          throw new RuntimeException(e1);
                        }
                      });
                    });

                return FileVisitResult.SKIP_SUBTREE;
            }
        });
    }

    private static class RedHatArtifactVersion {
        private final String upstreamVersion;
        private final int redhatBuildNumber;

        private static final String REGEX = "(.*)[\\.|-]redhat-(\\d+)";
        private static final Pattern  PATTERN = Pattern.compile(REGEX);

        private RedHatArtifactVersion(String upstreamVersion, int redhatBuildNumber) {
            this.upstreamVersion = upstreamVersion;
            this.redhatBuildNumber = redhatBuildNumber;
        }

        public static RedHatArtifactVersion fromVersion(String version) {
            final Matcher matcher = PATTERN.matcher(version);
            if(matcher.matches()) {
                return new RedHatArtifactVersion(matcher.group(1), Integer.parseInt(matcher.group(2)));
            }
            else {
                throw new IllegalStateException("Invalid use of '" + RedHatArtifactVersion.class.getName() +
                    "#fromVersion' for " + version +
                    ". Can only be used on redhat artifact versions");
            }
        }

        public String getUpstreamVersion() {
          return upstreamVersion;
        }

        public int getRedhatBuildNumber() {
          return redhatBuildNumber;
        }

        @Override
        public boolean equals(Object o) {
          if (this == o) {
            return true;
          }
          if (o == null || getClass() != o.getClass()) {
            return false;
          }
          RedHatArtifactVersion that = (RedHatArtifactVersion) o;
          return Objects.equals(upstreamVersion, that.upstreamVersion) &&
              Objects.equals(redhatBuildNumber, that.redhatBuildNumber);
        }

        @Override
        public int hashCode() {
          return Objects.hash(upstreamVersion, redhatBuildNumber);
        }
    }

    private RepositoryUtils() {
    }
}
