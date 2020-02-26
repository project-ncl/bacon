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

package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.redhat.red.build.finder.BuildConfig;
import com.redhat.red.build.finder.BuildFinder;
import com.redhat.red.build.finder.DistributionAnalyzer;
import com.redhat.red.build.finder.KojiBuild;
import com.redhat.red.build.finder.KojiClientSession;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.json.util.KojiObjectMapper;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TODO: 1. move out methods that manipulated on SharedContentReportRow TODO: 2. move to a dedicated package
 * 
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/19/17
 */
public class BrewSearcher {
    private static final Logger log = LoggerFactory.getLogger(BrewSearcher.class);

    private static final String KOJI_BUILD_FINDER_CONFIG_ENV = "KOJI_BUILD_FINDER_CONFIG";

    private static final String KOJI_BUILD_FINDER_CONFIG_PROP = "koji.build.finder.config";

    private static final String KOJI_BUILD_FINDER_CONFIG_RES = "koji-build-finder/config.json";

    public static BuildConfig getKojiBuildFinderConfigFromFile(final File file) {
        try {
            final String json = org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return getKojiBuildFinderConfigFromJson(json);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed read Koji Build Finder configuration from file: " + file.getAbsolutePath(),
                    e);
        }
    }

    public static BuildConfig getKojiBuildFinderConfigFromFile(final String filename) {
        return getKojiBuildFinderConfigFromFile(new File(filename));
    }

    public static BuildConfig getKojiBuildFinderConfigFromResource(final String resourceName) {
        final URL url = Resources.getResource(resourceName);

        try {
            final String json = Resources.toString(url, Charsets.UTF_8);
            return getKojiBuildFinderConfigFromJson(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed read Koji Build Finder configuration from resource: " + url, e);
        }
    }

    public static BuildConfig getKojiBuildFinderConfigFromJson(final String json) {
        final ObjectMapper mapper = new KojiObjectMapper();

        try {
            final BuildConfig config = mapper.readValue(json, BuildConfig.class);
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Failed read Koji Build Finder configuration from json: " + json, e);
        }
    }

    public static BuildConfig getKojiBuildFinderConfig() {
        final String propFilename = System.getProperty(KOJI_BUILD_FINDER_CONFIG_PROP);

        if (propFilename != null) {
            return getKojiBuildFinderConfigFromFile(propFilename);
        }

        final String envFilename = System.getenv(KOJI_BUILD_FINDER_CONFIG_ENV);

        if (envFilename != null) {
            return getKojiBuildFinderConfigFromFile(envFilename);
        }

        return getKojiBuildFinderConfigFromResource(KOJI_BUILD_FINDER_CONFIG_RES);
    }

    public static void fillBrewData(SharedContentReportRow row) {
        log.debug("Asking for {}\n", row.toGapv());
        List<KojiBuild> builds = getBuilds(row);
        builds.forEach(build -> {
            int buildId = build.getBuildInfo().getId();
            log.debug("Got build id {} for artifact {}", buildId, row.toGapv());

            if (row.getBuildId() != null) {
                row.setBuildId(row.getBuildId() + ", " + buildId);
            } else {
                row.setBuildId(String.valueOf(buildId));
            }

            fillBuiltBy(row, build);
            fillTags(row, build);
        });
    }

    private static List<KojiBuild> getBuilds(SharedContentReportRow row) {
        Path filePath = row.getFilePath();
        return getBuilds(filePath);
    }

    public static List<KojiBuild> getBuilds(final Path filePath) {
        KojiClientSession session;
        BuildConfig config;

        try {
            config = getKojiBuildFinderConfig();
            session = new KojiClientSession(config.getKojiHubURL());
        } catch (KojiClientException e) {
            throw new IllegalStateException("Failed to create Koji session", e);
        }

        DistributionAnalyzer da = new DistributionAnalyzer(Collections.singletonList(filePath.toFile()), config);
        Map<String, Collection<String>> checksumTable;
        List<KojiBuild> buildList;

        try {
            checksumTable = da.checksumFiles().asMap();
            BuildFinder bf = new BuildFinder(session, config);
            Map<Integer, KojiBuild> builds = bf.findBuilds(checksumTable);
            buildList = new ArrayList<>(builds.values());
            buildList.sort(Comparator.comparingInt(b -> b.getBuildInfo().getId()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to find Koji builds", e);
        } finally {
            session.close();
        }

        List<KojiArchiveInfo> archiveInfos = buildList.stream()
                .map(KojiBuild::getProjectSourcesTgz)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try {
            session = new KojiClientSession(config.getKojiHubURL());
            session.enrichArchiveTypeInfo(archiveInfos);
        } catch (KojiClientException e) {
            throw new IllegalStateException("Failed to enrich Koji builds", e);
        } finally {
            session.close();
        }

        List<KojiBuild> ret = buildList.stream().skip(1).collect(Collectors.toList());

        return Collections.unmodifiableList(ret);
    }

    private static void fillBuiltBy(SharedContentReportRow row, KojiBuild build) {
        if (build.getBuildInfo() == null || build.getBuildInfo().getOwnerName() == null) {
            return;
        }

        String value = build.getBuildInfo().getOwnerName();

        if (row.getBuildAuthor() != null) {
            value = row.getBuildAuthor() + "," + value;
        }

        row.setBuildAuthor(value);
    }

    private static void fillTags(SharedContentReportRow row, KojiBuild build) {
        if (build.getTags() == null) {
            return;
        }

        List<String> tags = build.getTags().stream().map(KojiTagInfo::getName).collect(Collectors.toList());

        if (row.getBuildTags() != null) {
            row.getBuildTags().addAll(tags);
        } else {
            row.setBuildTags(tags);
        }
    }

    private BrewSearcher() {
    }
}
