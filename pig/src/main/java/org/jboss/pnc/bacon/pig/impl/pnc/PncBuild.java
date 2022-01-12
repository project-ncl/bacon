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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.bacon.pnc.common.UrlGenerator;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.BuildStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/3/17
 */
@Getter
@Setter
@ToString(exclude = "buildLog")
@AllArgsConstructor
@JsonIgnoreProperties(value = { "startTime", "endTime" })
public class PncBuild {
    private static final Logger log = LoggerFactory.getLogger(PncBuild.class);

    public static final String SUCCESSFUL_STATUS = "DONE";

    private String internalScmUrl;
    private String scmRevision;
    private String scmTag;
    private String id;
    private String name;
    private Map<String, String> attributes;
    private BuildStatus buildStatus;

    // Don't print huge build logs in the user's output. It's usually not that useful
    @JsonIgnore
    private List<String> buildLog;
    private List<ArtifactWrapper> builtArtifacts;
    private List<ArtifactWrapper> dependencyArtifacts;

    @Deprecated // only for jackson
    public PncBuild() {

    }

    public PncBuild(Build build) {
        name = build.getBuildConfigRevision().getName();
        id = build.getId();
        internalScmUrl = build.getScmRepository().getInternalUrl();
        scmRevision = build.getScmRevision();
        scmTag = build.getScmTag();
        buildStatus = build.getStatus();
        attributes = build.getAttributes();
    }

    public void addBuiltArtifacts(List<Artifact> artifacts) {
        builtArtifacts = artifacts.stream().map(ArtifactWrapper::new).collect(Collectors.toList());
    }

    public void addBuildLog(String log) {
        buildLog = log == null ? Collections.emptyList() : asList(log.split("\\r?\\n"));
    }

    public void addBuildLog(List<String> log) {
        buildLog = log == null ? Collections.emptyList() : log;
    }

    public void addDependencyArtifacts(List<Artifact> artifacts) {
        dependencyArtifacts = artifacts.stream().map(ArtifactWrapper::new).collect(Collectors.toList());
    }

    public ArtifactWrapper findArtifact(String regex) {
        List<ArtifactWrapper> matches = findArtifactsMatching(a -> a.getGapv().matches(regex));
        if (matches.size() != 1) {
            throw new RuntimeException(
                    "Expecting exactly one artifact matching " + regex + ", found " + matches.size());
        }
        return matches.get(0);
    }

    public ArtifactWrapper findArtifactByFileName(String regex) {
        List<ArtifactWrapper> matches = findArtifactsMatching(a -> a.getFileName().matches(regex));
        if (matches.size() != 1) {
            throw new RuntimeException(
                    "Expecting exactly one artifact matching " + regex + ", found " + matches.size());
        }
        return matches.get(0);
    }

    public void downloadArtifact(String pattern, File downloadedZip) {
        Predicate<ArtifactWrapper> query = a -> a.getFileName().matches(pattern);
        List<ArtifactWrapper> artifacts = findArtifactsMatching(query);

        if (artifacts.size() != 1) {
            throw new RuntimeException(
                    "Unexpected number of artifacts to download found.\n" + "Expecting one artifact matching " + pattern
                            + " in build " + id + " , found: " + artifacts);
        }

        ArtifactWrapper artifact = artifacts.get(0);
        artifact.downloadTo(downloadedZip);
    }

    /**
     * Get the build log of the build on demand and result if cached. If the method is called again, the cached content
     * will be served if not null
     *
     * @return the logs of the build
     */
    public List<String> getBuildLog() {

        // use cached buildLog if present
        if (buildLog != null) {
            return buildLog;
        }

        try (BuildClient buildClient = new BuildClient(PncClientHelper.getPncConfiguration())) {
            Optional<InputStream> maybeBuildLogs = buildClient.getBuildLogs(id);

            if (maybeBuildLogs.isPresent()) {
                try (InputStream inputStream = maybeBuildLogs.get()) {
                    buildLog = readLog(inputStream);
                }
            } else {
                log.debug("Couldn't find logs for build id: {} ( {} )", id, UrlGenerator.generateBuildUrl(id));
                buildLog = Collections.emptyList();
            }

            return buildLog;
        } catch (ClientException | IOException e) {
            throw new RuntimeException(
                    "Failed to get build log for " + id + " (" + UrlGenerator.generateBuildUrl(id) + ")",
                    e);
        }
    }

    private static List<String> readLog(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();

        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader)) {
            reader.lines().forEach(lines::add);
            return lines;
        }
    }

    private List<ArtifactWrapper> findArtifactsMatching(Predicate<ArtifactWrapper> query) {
        return builtArtifacts.stream().filter(query).collect(Collectors.toList());
    }
}
