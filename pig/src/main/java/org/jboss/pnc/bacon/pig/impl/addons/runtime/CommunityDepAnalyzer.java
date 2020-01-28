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
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.DADao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/1/17
 */
public class CommunityDepAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(CommunityDepAnalyzer.class);

    private final DADao daDao;

    private final List<CommunityDependency> dependencies;
    private final List<String> downloadedForSwarm;
    private boolean skipDa = false;

    public CommunityDepAnalyzer(List<String> dependencyLines, List<String> swarmLog) {
        daDao = DADao.getInstance();
        downloadedForSwarm = swarmLog.stream().filter(line -> line.startsWith("Downloaded"))
                .filter(line -> line.contains(".jar"))
                // lines are of the form: Downloaded: http://... (some add. info)
                .map(l -> l.split("\\s+")[1]).sorted().collect(Collectors.toList());
        dependencies = dependencyLines.stream().map(CommunityDependency::new).collect(Collectors.toList());
    }

    public File generateAnalysis(String path) {
        try {
            if (!skipDa) {
                analyzeDAResults();
            }
            analyzeSwarmBuildLog();
            log.info("generating analysis to {} ...", path);
            File csvFile = new File(path);

            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.append(
                        "Community dependencies in Swarm;;Productized counterpart;Other productized versions;Used in the Swarm build\n");
                dependencies.forEach(d -> d.appendToCsv(writer));
            }
            log.info("DONE");

            return csvFile;
        } catch (Exception anyCaughtException) {
            throw new IllegalStateException("Failed to generate analysis", anyCaughtException);
        }
    }

    protected List<CommunityDependency> analyzeDAResults() {
        dependencies.parallelStream().forEach(daDao::fillDaData);
        return dependencies;
    }

    protected List<CommunityDependency> analyzeSwarmBuildLog() {
        dependencies.parallelStream().forEach(this::addSwarmBuildDependencies);
        return dependencies;
    }

    private void addSwarmBuildDependencies(CommunityDependency communityDependency) {
        List<String> swarmBuildDownloads = downloadedForSwarm.stream()
                .filter(d -> d.contains(communityDependency.toPathSubstring())).map(l -> l.substring(l.lastIndexOf("/") + 1))
                .collect(Collectors.toList());
        communityDependency.setUsedForSwarm(swarmBuildDownloads);
    }

    public void skipDa(boolean skip) {
        skipDa = skip;
    }
}
