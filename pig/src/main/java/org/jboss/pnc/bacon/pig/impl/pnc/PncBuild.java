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

package org.jboss.pnc.bacon.pig.impl.pnc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/3/17
 */
@Getter
@ToString(exclude = "buildLog")
@AllArgsConstructor
public class PncBuild {
    private static final Logger log = LoggerFactory.getLogger(PncBuild.class);

    public static final String SUCCESSFUL_STATUS = "DONE";

    @Delegate
    private final Build build;

    private List<String> buildLog;
    private List<ArtifactWrapper> builtArtifacts;
    private List<ArtifactWrapper> dependencyArtifacts;

    public PncBuild(Build build) {
        this.build = build;
    }

    public void setBuiltArtifacts(List<Artifact> artifacts) {
        builtArtifacts = artifacts.stream()
              .map(ArtifactWrapper::new)
              .collect(Collectors.toList());
    }

    public void setBuildLog(String log) {
        buildLog = log == null
              ? Collections.emptyList()
              : asList(log.split("\\r?\\n"));
    }

    public void setDependencyArtifacts(List<Artifact> artifacts) {
        dependencyArtifacts = artifacts.stream().map(ArtifactWrapper::new).collect(Collectors.toList());
    }

    public ArtifactWrapper findArtifact(String regex) {
        List<ArtifactWrapper> matches = findArtifactsMatching(a -> a.getGapv().matches(regex));
        if (matches.size() != 1) {
            throw new RuntimeException(
                  "Expecting exactly one artifact matching " + regex + ", found " + matches.size()
            );
        }
        return matches.get(0);
    }

    public ArtifactWrapper findArtifactByFileName(String regex) {
        List<ArtifactWrapper> matches = findArtifactsMatching(a -> a.getFileName().matches(regex));
        if (matches.size() != 1) {
            throw new RuntimeException(
                  "Expecting exactly one artifact matching " + regex + ", found " + matches.size()
            );
        }
        return matches.get(0);
    }

    public void downloadArtifact(String pattern, File downloadedZip) {
        Predicate<ArtifactWrapper> query = a -> a.getFileName().matches(pattern);
        List<ArtifactWrapper> artifacts = findArtifactsMatching(query);

        if (artifacts.size() != 1) {
            throw new RuntimeException("Unexpected number of artifacts to download found.\n" +
                  "Expecting one artifact matching " + pattern + " in build " + build.getId() + " , found: " + artifacts);
        }

        ArtifactWrapper artifact = artifacts.get(0);
        artifact.downloadTo(downloadedZip);
    }

    public String getName() {
        return getBuildConfigRevision().getName();
    }

    private List<ArtifactWrapper> findArtifactsMatching(Predicate<ArtifactWrapper> query) {
        return getBuiltArtifacts()
              .stream()
              .filter(query)
              .collect(Collectors.toList());
    }
}
