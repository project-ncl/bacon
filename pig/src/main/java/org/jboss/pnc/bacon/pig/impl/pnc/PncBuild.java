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

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.bacon.pnc.common.UrlGenerator;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.BuildStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/3/17
 */
@JsonIgnoreProperties({ "startTime", "endTime" })
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

    // only for jackson
    @Deprecated
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
        try (BuildClient buildClient = new BuildClient(PncClientHelper.getPncConfiguration(false))) {
            Optional<InputStream> streamLogs = buildClient.getBuildLogs(id);
            buildLog = Collections.emptyList();
            streamLogs.ifPresent(
                    inputStream -> buildLog = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().toList());
            return buildLog;
        } catch (RemoteResourceException e) {
            throw new RuntimeException(
                    "Failed to get build log for " + id + " (" + UrlGenerator.generateBuildUrl(id) + ")",
                    e);
        }
    }

    private List<ArtifactWrapper> findArtifactsMatching(Predicate<ArtifactWrapper> query) {
        return builtArtifacts.stream().filter(query).collect(Collectors.toList());
    }

    @java.lang.SuppressWarnings("all")
    public String getInternalScmUrl() {
        return this.internalScmUrl;
    }

    @java.lang.SuppressWarnings("all")
    public String getScmRevision() {
        return this.scmRevision;
    }

    @java.lang.SuppressWarnings("all")
    public String getScmTag() {
        return this.scmTag;
    }

    @java.lang.SuppressWarnings("all")
    public String getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    @java.lang.SuppressWarnings("all")
    public BuildStatus getBuildStatus() {
        return this.buildStatus;
    }

    @java.lang.SuppressWarnings("all")
    public List<ArtifactWrapper> getBuiltArtifacts() {
        return this.builtArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public List<ArtifactWrapper> getDependencyArtifacts() {
        return this.dependencyArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public void setInternalScmUrl(final String internalScmUrl) {
        this.internalScmUrl = internalScmUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setScmRevision(final String scmRevision) {
        this.scmRevision = scmRevision;
    }

    @java.lang.SuppressWarnings("all")
    public void setScmTag(final String scmTag) {
        this.scmTag = scmTag;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final String id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setAttributes(final Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildStatus(final BuildStatus buildStatus) {
        this.buildStatus = buildStatus;
    }

    @JsonIgnore
    @java.lang.SuppressWarnings("all")
    public void setBuildLog(final List<String> buildLog) {
        this.buildLog = buildLog;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuiltArtifacts(final List<ArtifactWrapper> builtArtifacts) {
        this.builtArtifacts = builtArtifacts;
    }

    @java.lang.SuppressWarnings("all")
    public void setDependencyArtifacts(final List<ArtifactWrapper> dependencyArtifacts) {
        this.dependencyArtifacts = dependencyArtifacts;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PncBuild(internalScmUrl=" + this.getInternalScmUrl() + ", scmRevision=" + this.getScmRevision()
                + ", scmTag=" + this.getScmTag() + ", id=" + this.getId() + ", name=" + this.getName() + ", attributes="
                + this.getAttributes() + ", buildStatus=" + this.getBuildStatus() + ", builtArtifacts="
                + this.getBuiltArtifacts() + ", dependencyArtifacts=" + this.getDependencyArtifacts() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public PncBuild(
            final String internalScmUrl,
            final String scmRevision,
            final String scmTag,
            final String id,
            final String name,
            final Map<String, String> attributes,
            final BuildStatus buildStatus,
            final List<String> buildLog,
            final List<ArtifactWrapper> builtArtifacts,
            final List<ArtifactWrapper> dependencyArtifacts) {
        this.internalScmUrl = internalScmUrl;
        this.scmRevision = scmRevision;
        this.scmTag = scmTag;
        this.id = id;
        this.name = name;
        this.attributes = attributes;
        this.buildStatus = buildStatus;
        this.buildLog = buildLog;
        this.builtArtifacts = builtArtifacts;
        this.dependencyArtifacts = dependencyArtifacts;
    }
}
