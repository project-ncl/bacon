/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/14/17
 */
public class RepoDescriptor {

    public static final String[] CHECKSUM_EXTENSIONS = { ".md5", ".sha1" };
    public static final String MAVEN_REPOSITORY = "maven-repository/";

    /**
     * Returns a collection of {@link GAV} (that are actually {@code groupId:artifactId:version}, i.e. ignoring the
     * classifier and the type attributes) found in a repository.
     *
     * @param m2RepoDirectory local Maven repository directory
     * @return a list of GAVs found in a repository
     */
    public static Collection<GAV> listGavs(File m2RepoDirectory) {
        Collection<GAV> allGavs = listArtifacts(m2RepoDirectory.toPath());
        Set<GAV> resultSet = new TreeSet<>(Comparator.comparing(GAV::toGav));
        resultSet.addAll(allGavs);
        return resultSet;
    }

    /**
     * Returns a collection of {@link GAV} that include all the attributes of artifact coordinates, including the
     * classifier and the type attributes.
     *
     * @param m2RepoDirectory local Maven repository directory
     * @return a list of GAVs found in a repository
     */
    public static Collection<GAV> listArtifacts(Path m2RepoDirectory) {
        try (Stream<Path> stream = Files.walk(m2RepoDirectory, FileVisitOption.FOLLOW_LINKS)) {
            return stream.filter(f -> !Files.isDirectory(f))
                    .filter(RepoDescriptor::isInRepoDir)
                    .filter(RepoDescriptor::isPotentialArtifact)
                    .map(f -> GAV.fromFileName(f.toAbsolutePath().toString(), MAVEN_REPOSITORY))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isPotentialArtifact(Path file) {
        // the parent must be the version directory
        final Path versionDir = file.getParent();
        final String version = getFileNameOrNull(versionDir);
        if (version == null) {
            return false;
        }
        // the parent of the version directory must be the artifact directory
        final String artifactId = getFileNameOrNull(versionDir.getParent());
        if (artifactId == null) {
            return false;
        }
        // the file name must start with artifactId-version
        final String fileName = file.getFileName().toString();
        if (!fileName.startsWith(artifactId)
                || !fileName.regionMatches(artifactId.length() + 1, version, 0, version.length())) {
            return false;
        }
        // filter out checksums
        for (var ext : CHECKSUM_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return false;
            }
        }
        return true;
    }

    private static String getFileNameOrNull(Path p) {
        if (p == null) {
            return null;
        }
        p = p.getFileName();
        return p == null ? null : p.toString();
    }

    private static boolean isInRepoDir(Path file) {
        if (file == null) {
            return false;
        }
        for (int i = 0; i < file.getNameCount(); ++i) {
            var name = file.getName(i);
            if (name.toString().regionMatches(0, MAVEN_REPOSITORY, 0, MAVEN_REPOSITORY.length() - 1)) {
                return true;
            }
        }
        return false;
    }

    public static Collection<File> listFiles(File m2RepoDirectory) {
        return org.apache.commons.io.FileUtils.listFiles(m2RepoDirectory, null, true);
    }

    private RepoDescriptor() {
    }
}
