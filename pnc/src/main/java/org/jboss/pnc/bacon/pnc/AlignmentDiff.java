/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pnc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a unified alignment diff into a build-affecting part and a metadata part.
 *
 * <p>
 * During alignment PNC/Reqour may commit files that are additional information about the alignment rather than changes
 * that influence the build: the Maven (PME) metadata {@value #REPOSITORIES_BACKUP}, {@value #MANIPULATION_JSON} and
 * {@value #ALIGNMENT_REPORT_JSON}, and the Gradle (GME) metadata {@value #GME_GRADLE},
 * {@value #GME_PLUGINCONFIGS_GRADLE} and {@value #GME_REPOS_GRADLE}. Keeping those out of the main diff makes the
 * build-affecting part apply cleanly on top of the original upstream sources while still preserving the metadata in a
 * separate part.
 *
 * <p>
 * Version-change separation is intentionally not handled here: it varies a lot per project and build type and is left
 * for a later iteration.
 */
final class AlignmentDiff {

    // Maven (PME) alignment metadata.
    static final String REPOSITORIES_BACKUP = "repositories-backup.xml";
    static final String MANIPULATION_JSON = "manipulation.json";
    static final String ALIGNMENT_REPORT_JSON = "alignmentReport.json";

    // Gradle (GME) alignment metadata. gme-repos.gradle is committed under gradle/, matched here by file name.
    static final String GME_GRADLE = "gme.gradle";
    static final String GME_PLUGINCONFIGS_GRADLE = "gme-pluginconfigs.gradle";
    static final String GME_REPOS_GRADLE = "gme-repos.gradle";

    private static final Set<String> METADATA_FILE_NAMES = Set.of(
            REPOSITORIES_BACKUP,
            MANIPULATION_JSON,
            ALIGNMENT_REPORT_JSON,
            GME_GRADLE,
            GME_PLUGINCONFIGS_GRADLE,
            GME_REPOS_GRADLE);

    // Matches the per-file header that starts every section of a `git diff`, capturing the b/ (new) path in group 2.
    private static final Pattern DIFF_GIT_HEADER = Pattern.compile("^diff --git a/(.*) b/(.*)$");

    private final String buildAffecting;
    private final String metadata;

    private AlignmentDiff(String buildAffecting, String metadata) {
        this.buildAffecting = buildAffecting;
        this.metadata = metadata;
    }

    /**
     * The changes that affect the build: everything except the metadata files. Applies on top of the original upstream
     * sources.
     */
    String buildAffecting() {
        return buildAffecting;
    }

    /**
     * The extracted metadata changes ({@value #REPOSITORIES_BACKUP}, {@value #MANIPULATION_JSON}), or an empty string
     * when the diff contains none of them.
     */
    String metadata() {
        return metadata;
    }

    boolean hasMetadata() {
        return !metadata.isEmpty();
    }

    /**
     * Routes each {@code diff --git} file section of the given unified diff to the build-affecting or metadata part.
     *
     * @param unifiedDiff the unified diff, may be null
     * @return the split result; both parts are empty strings when the input is null or empty
     */
    static AlignmentDiff split(String unifiedDiff) {
        if (unifiedDiff == null || unifiedDiff.isEmpty()) {
            return new AlignmentDiff("", "");
        }

        List<String> buildLines = new ArrayList<>();
        List<String> metadataLines = new ArrayList<>();

        // Content before the first header (there usually is none for a GitHub compare diff) stays with the
        // build-affecting part.
        List<String> current = buildLines;

        for (String line : unifiedDiff.split("\n", -1)) {
            Matcher matcher = DIFF_GIT_HEADER.matcher(line);
            if (matcher.matches()) {
                current = isMetadataPath(matcher.group(2)) ? metadataLines : buildLines;
            }
            current.add(line);
        }

        return new AlignmentDiff(String.join("\n", buildLines), String.join("\n", metadataLines));
    }

    private static boolean isMetadataPath(String path) {
        String name = path;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        return METADATA_FILE_NAMES.contains(name);
    }
}
