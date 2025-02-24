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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.SharedContentReportGenerator;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.enums.RepositoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/23/17
 */
public class DocumentGenerator {
    private static final Logger log = LoggerFactory.getLogger(DocumentGenerator.class);

    private final String extrasPath;
    private final String releasePath;
    private final PigConfiguration pigConfiguration;
    private final Deliverables deliverables;

    public DocumentGenerator(
            PigConfiguration pigConfiguration,
            String releasePath,
            String extrasPath,
            Deliverables deliverables) {
        this.releasePath = releasePath;
        this.extrasPath = extrasPath;
        this.pigConfiguration = pigConfiguration;
        this.deliverables = deliverables;
    }

    public void generateDocuments(Map<String, PncBuild> builds, RepositoryData repo) {
        Collection<GAV> gavs = repo.getGavs();
        String repositoryContents = gavs.stream().map(GAV::toGav).collect(Collectors.joining("\n"));
        String duplicates = gavs.stream()
                .collect(Collectors.groupingBy(GAV::getGa))
                .values()
                .stream()
                .filter(listForGa -> listForGa.size() > 1)
                .flatMap(Collection::stream)
                .map(GAV::toGav)
                .sorted()
                .collect(Collectors.joining("\n"));

        DataRoot templateData = new DataRoot(
                pigConfiguration,
                deliverables,
                duplicates,
                repositoryContents,
                builds.values(),
                Config.instance().getActiveProfile().getPnc().getUrl());

        FileGenerator generator = new FileGenerator(Optional.empty());
        log.debug("Generating documents with data: {}", templateData);
        generator.generateFiles(releasePath, extrasPath, templateData);
    }

    /**
     * TODO: we ignore any NPM artifacts for now. Maybe we should consider adding them in the future
     *
     * @param repoData
     * @param builds
     * @throws IOException
     */
    public void generateSharedContentReport(RepositoryData repoData, Map<String, PncBuild> builds) throws IOException {
        SharedContentReportGenerator sharedContentReportGenerator = new SharedContentReportGenerator(
                repoData.getFiles(),
                getAllMavenBuiltArtifacts(builds));
        File reportFile = new File(extrasPath, deliverables.getSharedContentReport());
        sharedContentReportGenerator.generateReport(reportFile);
    }

    private static Set<GAV> getAllMavenBuiltArtifacts(Map<String, PncBuild> builds) {
        return builds.values()
                .stream()
                .map(PncBuild::getBuiltArtifacts)
                .flatMap(List::stream)
                .filter(a -> Objects.equals(a.getRepositoryType(), RepositoryType.MAVEN))
                .map(ArtifactWrapper::toGAV)
                .collect(Collectors.toSet());
    }
}
