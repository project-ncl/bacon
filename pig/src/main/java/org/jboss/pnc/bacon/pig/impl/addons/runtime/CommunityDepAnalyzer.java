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
package org.jboss.pnc.bacon.pig.impl.addons.runtime;

import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CommunityDependency;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CsvExportable;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.DADao;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/1/17
 */
public class CommunityDepAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(CommunityDepAnalyzer.class);

    private final DADao daDao = DADao.getInstance();

    private final List<CommunityDependency> dependencies;
    private final Function<List<CommunityDependency>, List<? extends CsvExportable>> enricher;

    private boolean skipDa = false;

    public CommunityDepAnalyzer(
            List<String> dependencyLines,
            Function<List<CommunityDependency>, List<? extends CsvExportable>> enricher) {
        dependencies = dependencyLines.stream().map(CommunityDependency::new).collect(Collectors.toList());
        this.enricher = enricher;
    }

    public CommunityDepAnalyzer(
            Collection<GAV> gavs,
            Function<List<CommunityDependency>, List<? extends CsvExportable>> enricher) {
        dependencies = gavs.stream().map(CommunityDependency::new).collect(Collectors.toList());
        this.enricher = enricher;
    }

    public CommunityDepAnalyzer(Collection<GAV> gavs) {
        dependencies = gavs.stream().map(CommunityDependency::new).collect(Collectors.toList());
        enricher = null;
    }

    public File generateAnalysis(String path) {
        try {
            log.info("generating analysis to {} ...", path);
            List<? extends CsvExportable> csvContents = analyze();
            File csvFile = new File(path);

            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.append(
                        "Community dependencies;;Productized counterpart;Other productized versions;Additional info\n");
                csvContents.forEach(d -> d.appendToCsv(writer));
            }
            log.info("DONE");

            return csvFile;
        } catch (Exception anyCaughtException) {
            throw new IllegalStateException("Failed to generate analysis", anyCaughtException);
        }
    }

    protected List<? extends CsvExportable> analyze() {
        if (!skipDa) {
            analyzeDAResults();
        }
        return productSpecificAnalysis();
    }

    private List<? extends CsvExportable> productSpecificAnalysis() {
        return enricher != null ? enricher.apply(dependencies) : dependencies;
    }

    protected List<CommunityDependency> analyzeDAResults() {
        dependencies.parallelStream().forEach(daDao::fillDaData);
        return dependencies;
    }

    public void skipDa(boolean skip) {
        skipDa = skip;
    }
}
