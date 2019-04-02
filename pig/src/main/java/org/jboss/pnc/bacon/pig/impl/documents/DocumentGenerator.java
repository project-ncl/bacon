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
package org.jboss.pnc.bacon.pig.impl.documents;

import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.SharedContentReportGenerator;
import org.jboss.pnc.bacon.pig.impl.pnc.Artifact;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/23/17
 */
public class DocumentGenerator {
    private static final Logger log = LoggerFactory.getLogger(DocumentGenerator.class);

    private final String extrasPath;
    private final String releasePath;
    private final Config config;
    private final Deliverables deliverables;

    public DocumentGenerator(Config config,
                             String releasePath,
                             String extrasPath,
                             Deliverables deliverables) {
        this.releasePath = releasePath;
        this.extrasPath = extrasPath;
        this.config = config;
        this.deliverables = deliverables;
    }

    public void generateDocuments(Map<String, PncBuild> builds,
                                  RepositoryData repo) {
        String repositoryContents =
                repo.getGavs().stream().map(GAV::toGav).collect(Collectors.joining("\n"));
        String duplicates = repo.getGavs().stream()
                .collect(Collectors.groupingBy(GAV::getGa))
                .values()
                .stream()
                .filter(listForGa -> listForGa.size() > 1)
                .flatMap(Collection::stream)
                .map(GAV::toGav)
                .sorted()
                .collect(Collectors.joining("\n"));

        DataRoot templateData = new DataRoot(
                config,
                deliverables,
                duplicates,
                repositoryContents,
                builds.values());

        FileGenerator generator = new FileGenerator(Optional.empty());
        log.debug("Generating documents with data: {}", templateData);
        generator.generateFiles(releasePath, extrasPath, templateData);
    }


    public void generateSharedContentReport(RepositoryData repoData,
                                            Map<String, PncBuild> builds) throws IOException {
        SharedContentReportGenerator sharedContentReportGenerator =
                new SharedContentReportGenerator(repoData.getFiles(), getAllBuiltArtifacts(builds));
        File reportFile = new File(extrasPath, deliverables.getSharedContentReport());
        sharedContentReportGenerator.generateReport(reportFile);
    }


    private static Set<GAV> getAllBuiltArtifacts(Map<String, PncBuild> builds) {
        return builds.values().stream()
                .map(PncBuild::getBuiltArtifacts)
                .flatMap(List::stream)
                .map(Artifact::toGAV)
                .collect(Collectors.toSet());
    }
}
