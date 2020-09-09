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

import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.jboss.pnc.bacon.pig.impl.utils.BuildFinderUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.build.finder.koji.KojiLocalArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TODO: 1. move out methods that manipulated on SharedContentReportRow TODO: 2. move to a dedicated package
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/19/17
 */
public class BrewSearcher {
    private static final Logger log = LoggerFactory.getLogger(BrewSearcher.class);

    public static void fillBrewData(List<SharedContentReportRow> rows) {
        List<String> pathsForRows = rows.stream()
                .map(row -> row.getFilePath().toAbsolutePath().toString())
                .collect(Collectors.toList());

        Map<GAV, SharedContentReportRow> rowsByGav = rows.stream()
                .collect(Collectors.toMap(SharedContentReportRow::getGav, Function.identity()));

        List<KojiBuild> builds;
        int attempts = 3;
        int i = 0;
        Exception lastError = null;
        while (true) {
            try {
                if (++i > attempts) {
                    throw new RuntimeException("Failed to fill brew data in the shared content csv", lastError);
                }
                builds = BuildFinderUtils.findBuilds(false, pathsForRows, 50);
                break;
            } catch (Exception e) {
                lastError = e;
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Failed to fill Brew data to shared content report row, attempt {} out of {}",
                            i + 1,
                            attempts,
                            e);
                }
            }
        }

        for (KojiBuild build : builds) {
            for (KojiLocalArchive archive : build.getArchives()) {
                GAV gav = GAV.fromKojiArchive(archive.getArchive());
                SharedContentReportRow row = rowsByGav.get(gav);
                if (row == null) {
                    throw new RuntimeException(
                            "sth weird happened in shared content generation, unexpected gav: " + gav);
                }
                int buildId = build.getBuildInfo().getId();
                log.debug("Got build id {} for artifact {}", buildId, row.toGapv());

                if (row.getBuildId() != null) {
                    row.setBuildId(row.getBuildId() + ", " + buildId);
                } else {
                    row.setBuildId(String.valueOf(buildId));
                }

                fillBuiltBy(row, build);
                fillTags(row, build);
            }
        }

    }

    public static List<KojiBuild> getBuilds(final Path filePath) {
        return BuildFinderUtils.findBuilds(filePath, false);
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
