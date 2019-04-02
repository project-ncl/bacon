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
package org.jboss.pnc.bacon.pig.impl.addons;

import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.pnc.Artifact;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 3/26/18
 */
public class ExtraDeliverableDownloader extends AddOn {

    protected ExtraDeliverableDownloader(Config config,
                                         Map<String, PncBuild> builds,
                                         String releasePath,
                                         String extrasPath) {
        super(config, builds, releasePath, extrasPath);
    }

    @Override
    protected String getName() {
        return "extraDeliverablesDownloader";
    }

    @Override
    public void trigger() {
        getConfig().forEach(
                (key, artifacts) -> downloadArtifacts(key, (List<Map<String, String>>) artifacts)
        );
    }

    private void downloadArtifacts(String buildName, List<Map<String, String>> artifacts) {
        PncBuild build = builds.get(buildName);
        artifacts.forEach(artifact ->
                downloadArtifact(build, artifact.get("matching"), artifact.get("suffix"))
        );
    }

    private void downloadArtifact(PncBuild build, String pattern, String suffix) {
        Artifact artifact = build.findArtifactByFileName(pattern);
        Path releaseDir = Paths.get(releasePath);
        Path targetFile = releaseDir.resolve(constructFileName(suffix));
        artifact.downloadTo(targetFile.toFile());
    }

    private String constructFileName(String suffix) {
        return String.format("%s-%s.%s-%s",
                config.getOutputPrefixes().getReleaseFile(), config.getVersion(), config.getMilestone(), suffix);
    }
}
