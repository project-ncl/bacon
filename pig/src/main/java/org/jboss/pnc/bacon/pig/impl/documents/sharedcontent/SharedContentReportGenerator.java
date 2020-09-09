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

import org.jboss.pnc.bacon.pig.impl.repo.RepoDescriptor;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/19/17
 */
public class SharedContentReportGenerator {
    private static final Logger log = LoggerFactory.getLogger(SharedContentReportGenerator.class);

    private static final String[] IGNORED = {
            "md5",
            "sha1",
            "-sources",
            "example-settings.xml",
            "README",
            "maven-metadata.xml" };

    private final Collection<File> repositoryFiles;
    private final DASearcher daSearcher = new DASearcher();
    private final Set<GAV> projectsArtifacts;
    private Integer limit;
    private AtomicInteger analyzed = new AtomicInteger(0);

    public SharedContentReportGenerator(Set<GAV> projectsArtifacts, Collection<File> repositoryFiles, Integer limit) {
        this.repositoryFiles = repositoryFiles;
        this.limit = limit;
        this.projectsArtifacts = projectsArtifacts;
    }

    public SharedContentReportGenerator(Collection<File> repositoryFiles, Set<GAV> projectsArtifacts) {
        this(projectsArtifacts, repositoryFiles, null);
    }

    public void generateReport(File reportFile) throws IOException {
        StringBuilder output = generateReportString();
        try (FileWriter fileWriter = new FileWriter(reportFile)) {
            fileWriter.append(output.toString());
        }
    }

    protected StringBuilder generateReportString() {
        StringBuilder output = new StringBuilder(
                "Artifact;Product name; Product version; "
                        + "Released?; Build id; Build Author; Candidate tags; All tags\n");
        List<SharedContentReportRow> rows = repositoryFiles.stream()
                .filter(f -> Stream.of(IGNORED).noneMatch(f.getAbsolutePath()::contains))
                .map(f -> new SharedContentReportRow(f, RepoDescriptor.MAVEN_REPOSITORY))
                .filter(r -> !projectsArtifacts.contains(r.getGav()))
                .distinct()
                .collect(Collectors.toList());
        if (limit == null) {
            limit = rows.size();
        }
        rows = rows.subList(0, limit);
        log.info("Gathering data for shared content report");
        rows.parallelStream().forEach(this::fillDaData);
        List<SharedContentReportRow> toFillBrewData = rows.stream()
                .filter(row -> row.getProductName() == null || row.getProductVersion() == null)
                .collect(Collectors.toList());

        BrewSearcher.fillBrewData(toFillBrewData);

        rows.stream().sorted(SharedContentReportRow::byProductAndGav).forEach(r -> r.printTo(output));
        return output;
    }

    private void fillDaData(SharedContentReportRow row) {
        log.debug("Will fill {}", row.toGapv());
        daSearcher.fillDAData(row);
        MRRCSearcher.getInstance().fillMRRCData(row);
        if (log.isDebugEnabled()) {
            log.debug("Analyzed {}/{}", analyzed.incrementAndGet(), limit);
        }
    }
}
