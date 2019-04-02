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

import org.apache.commons.io.IOUtils;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/11/17
 */
public class RuntimeDependenciesAnalyzer extends AddOn {
    private static final Logger log = LoggerFactory.getLogger(RuntimeDependenciesAnalyzer.class);

    public RuntimeDependenciesAnalyzer(Config config,
                                       Map<String, PncBuild> builds,
                                       String releasePath,
                                       String extrasPath) {
        super(config, builds, releasePath, extrasPath);
    }

    @Override
    protected String getName() {
        return "runtimeDependenciesAnalyzer";
    }

    @Override
    public void trigger() {
        log.info("Running RuntimeDependenciesAnalyzer");
        File workDir = FileUtils.mkTempDir("runtimeDepAnalyzer");

        String buildName = (String) getConfig().get("downloadFrom");
        String regex = (String) getConfig().get("matching");
        String referenceBuildName = (String) getConfig().get("referenceBuild");
        File dependencyListPath = new File(workDir, "runtime-dependency-list.txt");

        builds.get(buildName).downloadArtifact(regex, dependencyListPath);

        List<String> dependencies;

        try (InputStream stream = new FileInputStream(dependencyListPath)) {
            dependencies = IOUtils.readLines(stream, "UTF-8");
        } catch (Exception any) {
            throw new RuntimeException("Unable to read dependency list from " + dependencyListPath);
        }

        List<String> communityDependencies = dependencies.stream()
                .filter(d -> !d.contains("redhat"))
                .collect(Collectors.toList());

        List<String> buildLog = builds.get(referenceBuildName).getBuildLog();

        Path targetPath = Paths.get(extrasPath, "community-dependencies.csv");

        CommunityDepAnalyzer analyzer = new CommunityDepAnalyzer(communityDependencies, buildLog);
        analyzer.skipDa(false);   // TODO: 
        analyzer.generateAnalysis(targetPath.toAbsolutePath().toString());
        log.info("Done");
    }
}
