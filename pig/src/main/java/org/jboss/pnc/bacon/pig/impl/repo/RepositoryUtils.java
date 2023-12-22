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
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/18/18
 */
public class RepositoryUtils {
    private static final Logger log = LoggerFactory.getLogger(RepositoryUtils.class);

    public static void generateMavenMetadata(File mavenRepositoryDirectory) {
        log.debug("Generating maven-metadata.xml files");
        Set<String> pomPaths = new HashSet<>();
        RepositoryUtils.searchForPomPaths(mavenRepositoryDirectory, mavenRepositoryDirectory, pomPaths);
        RepositoryUtils.generateMetadata(mavenRepositoryDirectory, pomPaths);
    }

    private static void searchForPomPaths(File root, File mavenRepositoryDirectory, Set<String> pomPaths) {
        if (root.isDirectory()) {
            for (File file : root.listFiles()) {
                searchForPomPaths(file, mavenRepositoryDirectory, pomPaths);
            }
        } else if (root.isFile() && root.getName().endsWith(".pom")) {
            pomPaths.add(root.getAbsolutePath().substring(mavenRepositoryDirectory.getAbsolutePath().length() + 1));
        }
    }

    /*
     * This code was taken from the offliner tool see
     * https://github.com/release-engineering/offliner/blob/main/src/main/java/com/redhat/red/offliner/Main.java#L687
     * discussed with John Casey if it would be possible to make this public and a deployed shared lib, there is also
     * functionality in offliner for creating the md5 and sha1 files which we could use as well
     */
    private static void generateMetadata(File mavenRepositoryDirectory, Set<String> pomPaths) {
        Map<ProjectRef, List<SingleVersion>> metas = new HashMap<>();
        for (String path : pomPaths) {
            ArtifactPathInfo artifactPathInfo = ArtifactPathInfo.parse(path);
            ProjectVersionRef gav = artifactPathInfo.getProjectId();
            List<SingleVersion> singleVersions = new ArrayList<>();
            if (!metas.isEmpty() && metas.containsKey(gav.asProjectRef())) {
                singleVersions = metas.get(gav.asProjectRef());
            }
            singleVersions.add((SingleVersion) gav.getVersionSpec());
            metas.put(gav.asProjectRef(), singleVersions);
        }
        for (Map.Entry<ProjectRef, List<SingleVersion>> entry : metas.entrySet()) {
            ProjectRef ga = entry.getKey();
            List<SingleVersion> singleVersions = entry.getValue();
            Collections.sort(singleVersions);

            Metadata main = new Metadata();
            main.setGroupId(ga.getGroupId());
            main.setArtifactId(ga.getArtifactId());
            Versioning versioning = new Versioning();
            for (SingleVersion v : singleVersions) {
                versioning.addVersion(v.renderStandard());
            }
            String latest = singleVersions.get(singleVersions.size() - 1).renderStandard();
            versioning.setLatest(latest);
            versioning.setRelease(latest);
            main.setVersioning(versioning);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            File metadataFile = Paths
                    .get(
                            mavenRepositoryDirectory.getAbsolutePath(),
                            ga.getGroupId().replace('.', File.separatorChar),
                            ga.getArtifactId(),
                            "maven-metadata.xml")
                    .toFile();
            try {
                new MetadataXpp3Writer().write(baos, main);
                FileUtils.writeByteArrayToFile(metadataFile, baos.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate maven-metadata file: %s" + metadataFile, e);
            }
        }
        log.debug("Finished generating maven-metadata.xml files");
    }

    public static void addCheckSums(File mavenRepositoryDirectory) {
        log.debug("Generating missing checksums");
        try (Stream<Path> stream = Files.walk(mavenRepositoryDirectory.toPath())) {
            stream.filter(p -> p.toFile().isFile())
                    .filter(RepositoryUtils::isNotCheckSumFile)
                    .forEach(RepositoryUtils::addCheckSums);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to generate checksums for " + mavenRepositoryDirectory.getAbsolutePath(),
                    e);
        }
    }

    private static boolean isNotCheckSumFile(Path path) {
        return Stream.of(RepoDescriptor.CHECKSUM_EXTENSIONS).noneMatch(ext -> path.toString().endsWith(ext));
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

    /**
     * Calculates and persists checksums for a file in a local Maven repository. The implementation reads the file once
     * for all the checksums. The algorithms passed in are currently limited to {@code md5} and {@code sha1}.
     *
     * @param file file to generate checksums for
     * @param algs checksum algorithms to use, for example md5, sha1.
     */
    public static void addCheckSums(Path file, String... algs) {
        byte[] content = null;
        for (var alg : algs) {
            var checksumFile = file.getParent().resolve(file.getFileName() + "." + alg);
            if (!Files.exists(checksumFile)) {
                if (content == null) {
                    try {
                        content = Files.readAllBytes(file);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                final String checksum;
                if ("md5".equals(alg)) {
                    checksum = DigestUtils.md5Hex(content);
                } else if ("sha1".equals(alg)) {
                    checksum = DigestUtils.sha1Hex(content);
                } else {
                    throw new IllegalArgumentException("Unexpected checksum type " + alg);
                }
                try (BufferedWriter writer = Files.newBufferedWriter(checksumFile)) {
                    writer.append(checksum);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    public static void removeIrrelevantFiles(File element) {
        log.debug("Removing internal maven files from the repository");
        removeMatchingCondition(element, file -> file.getName().equals("_remote.repositories"));
        removeMatchingCondition(element, file -> file.getName().endsWith(".lastUpdated"));
    }

    public static void removeCommunityArtifacts(File element) {
        log.debug("Removing community dependencies from the repository");
        removeMatchingCondition(element, RepositoryUtils::isCommunity);
    }

    /**
     * Given a folder having maven artifacts stored in a maven repository formatted folders, remove from that folder the
     * excluded artifacts, specified with the Maven identifier format <group id>:<artifactid>:<packaging>:<version>
     *
     * @param element
     * @param excludedArtifacts
     */
    public static void removeExcludedArtifacts(File element, List<String> excludedArtifacts) {
        List<String> excludedPaths = excludedArtifacts.stream()
                .map(a -> convertMavenIdentifierToPathRegex(a))
                .collect(Collectors.toList());
        if (element.isDirectory()) {
            Stream.of(element.listFiles()).forEach(file -> removeExcludedArtifacts(file, excludedArtifacts));
        } else {
            for (String excludedPath : excludedPaths) {
                Pattern matchesRegex = Pattern.compile(excludedPath);
                if (matchesRegex.matcher(element.getAbsolutePath()).find()) {
                    log.debug(
                            "Removing path {} from the repository since it matches regex: {}",
                            element.getAbsolutePath(),
                            excludedPath);
                    File parent = element.getParentFile();
                    element.delete();
                    recursivelyDeleteEmptyFolder(parent);
                }
            }
        }
    }

    private static boolean isCommunity(File f) {
        String absolutePath = f.getAbsolutePath();
        return !absolutePath.contains("redhat-") && !absolutePath.contains("eap-runtime-artifacts");
    }

    private static void removeMatchingCondition(File element, Function<File, Boolean> condition) {
        if (element.isDirectory()) {
            Stream.of(element.listFiles()).forEach(file -> removeMatchingCondition(file, condition));
            recursivelyDeleteEmptyFolder(element);
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
                if (null == children) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                final List<File> redHatFiles = Stream.of(children)
                        .filter(f -> !isCommunity(f))
                        .collect(Collectors.toList());

                if (redHatFiles.isEmpty()) {
                    // continue since we are at an intermediate directory
                    return FileVisitResult.CONTINUE;
                }

                redHatFiles.stream()
                        .map(f -> new AbstractMap.SimpleEntry<>(f, RedHatArtifactVersion.fromVersion(f.getName())))
                        // split by unique upstream version
                        .collect(Collectors.groupingBy(e -> e.getValue().getUpstreamVersion()))
                        // for each unique upstream version, delete the directories corresponding
                        // to redhat version less than the highest version
                        .forEach((upstreamVersion, matchingRHArtifactVersions) -> {
                            matchingRHArtifactVersions.sort(comparingInt(e -> e.getValue().getRedhatBuildNumber()));
                            Collections.reverse(matchingRHArtifactVersions);

                            matchingRHArtifactVersions.stream().skip(1).forEach(e -> {
                                final File directoryToBeDeleted = e.getKey();

                                log.info(
                                        "Deleting redhat artifact {} which is redundant since a newer redhat version exists",
                                        directoryToBeDeleted.getAbsolutePath());

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

    /**
     * If file is an empty folder, delete the folder, and recursively delete the parent of the folder if also empty
     *
     * @param element
     */
    public static void recursivelyDeleteEmptyFolder(File element) {
        if (element.isDirectory() && element.list().length == 0) {
            log.debug("Deleted empty folder {}", element);
            File parent = element.getParentFile();
            element.delete();
            recursivelyDeleteEmptyFolder(parent);
        }
    }

    /**
     * Method to transform a Maven identifier into a filesystem path
     *
     * Transform <group-id>:<artifact-id>:<packaging>:<version> to
     * <group-id>/<artifact-id>/<version>/<artifact-id>-<version>[-classifier].<packaging>
     *
     * @param identifier
     * @return
     */
    public static String convertMavenIdentifierToPathRegex(String identifier) {
        String[] sections = identifier.split(":");
        if (sections.length != 4 && sections.length != 5) {
            throw new IllegalArgumentException(
                    identifier
                            + " is wrongly formatted! It should be <group-id>:<artifact-id>:<packaging>:<version>:[classifier]");
        }
        String fileSeparator = System.getProperty("file.separator");

        String groupId = "";
        // if groupId is only a catch-all regex, let it be a catch-all regex
        if (sections[0].equals(".*")) {
            groupId = sections[0];
        } else {
            // break down the group id into several paths
            groupId = sections[0].replace(".", fileSeparator);
        }
        String artifactId = sections[1];
        String packaging = sections[2];
        String version = sections[3];
        String classifier = "";
        if (sections.length == 5) {
            classifier = "-" + sections[4];
        }
        // include the '$' at the end to make sure it matches the file extension, and nothing more!
        return groupId + fileSeparator + artifactId + fileSeparator + version + fileSeparator + artifactId + "-"
                + version + classifier + "\\." + packaging + "$";
    }

    private static class RedHatArtifactVersion {
        private final String upstreamVersion;
        private final int redhatBuildNumber;

        private static final String REGEX = "(.*)[\\.|-]redhat-(\\d+)";
        private static final Pattern PATTERN = Pattern.compile(REGEX);

        private RedHatArtifactVersion(String upstreamVersion, int redhatBuildNumber) {
            this.upstreamVersion = upstreamVersion;
            this.redhatBuildNumber = redhatBuildNumber;
        }

        public static RedHatArtifactVersion fromVersion(String version) {
            final Matcher matcher = PATTERN.matcher(version);
            if (matcher.matches()) {
                return new RedHatArtifactVersion(matcher.group(1), Integer.parseInt(matcher.group(2)));
            } else {
                throw new IllegalStateException(
                        "Invalid use of '" + RedHatArtifactVersion.class.getName() + "#fromVersion' for " + version
                                + ". Can only be used on redhat artifact versions");
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
            return Objects.equals(upstreamVersion, that.upstreamVersion)
                    && Objects.equals(redhatBuildNumber, that.redhatBuildNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(upstreamVersion, redhatBuildNumber);
        }
    }

    private RepositoryUtils() {
    }
}
