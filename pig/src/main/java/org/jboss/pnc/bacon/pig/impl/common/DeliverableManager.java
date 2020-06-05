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

package org.jboss.pnc.bacon.pig.impl.common;

import org.jboss.pnc.bacon.pig.impl.config.GenerationData;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/**
 * A superclass for deliverable generation. Out of the box it supports downloading of artifacts and repackaging them to
 * deliverables. A concrete implementation can add different methods of generating the deliverables.
 * <p>
 * The execution is controlled by a Groovy snippet. The manager is injected to the script under 'gen'.
 * </p>
 * E.g. to download an artifact matching *license.zip from a build named WildFly-Swarm, one would use:
 * 
 * <pre>
 *     gen.downloadFrom 'WildFly-Swarm' matching '.*license\\.zip'
 * </pre>
 * <p>
 * If another method, <code>generate()</code> is added to a custom manager, it can be used as follows:
 * 
 * <pre>
 * gen.generate()
 * </pre>
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/5/17
 */
public abstract class DeliverableManager<MetadataType extends GenerationData<?>, ResultType> {

    private static final Logger log = LoggerFactory.getLogger(DeliverableManager.class);

    protected final PigConfiguration pigConfiguration;
    protected final String releasePath;
    protected final Deliverables deliverables;
    protected final Map<String, PncBuild> builds;
    protected final File workDir;

    protected DeliverableManager(
            PigConfiguration pigConfiguration,
            String releasePath,
            Deliverables deliverables,
            Map<String, PncBuild> builds) {
        this.pigConfiguration = pigConfiguration;
        this.releasePath = releasePath;
        this.deliverables = deliverables;
        this.builds = builds;
        workDir = FileUtils.mkTempDir("deliverable-generation");
    }

    protected ResultType downloadAndRepackage() {
        PncBuild build = getBuild(getGenerationData().getSourceBuild());
        File downloadedZip = new File(workDir, "downloaded.zip");

        build.downloadArtifact(getGenerationData().getSourceArtifact(), downloadedZip);

        File extractedZip = unzip(downloadedZip);
        File downloadedTopLevelDirectory = getTopLevelDirectory(extractedZip);
        File targetTopLevelDirectory = new File(workDir, getTargetTopLevelDirectoryName());

        targetTopLevelDirectory.mkdirs();
        repackage(downloadedTopLevelDirectory, targetTopLevelDirectory);
        zip(targetTopLevelDirectory, getTargetZipPath());

        return null;
    }

    protected PncBuild getBuild(String buildName) {
        PncBuild build = builds.get(buildName);
        if (build == null) {
            throw new RuntimeException("Unable to find build data for name: " + buildName);
        }
        return build;
    }

    protected File unzip(File downloadedZip) {
        File repoDirectory = new File(workDir, "extracted");
        repoDirectory.mkdirs();
        FileUtils.unzip(downloadedZip, repoDirectory);
        return repoDirectory;
    }

    protected void zip(File m2Repo, Path repoZipPath) {
        log.debug("zipping the repository");
        FileUtils.zip(repoZipPath.toFile(), m2Repo.getParentFile(), m2Repo);
    }

    protected File getTopLevelDirectory(File repoDirectory) {
        File[] files = repoDirectory.listFiles();
        if (files.length > 1) {
            throw new RuntimeException(
                    "Expected one top level directory in the repository zip, found: "
                            + Arrays.toString(repoDirectory.list()));
        }
        return files[0];
    }

    protected abstract void repackage(File contentsDirectory, File targetTopLevelDirectory);

    protected abstract MetadataType getGenerationData();

    protected abstract String getTargetTopLevelDirectoryName();

    protected abstract Path getTargetZipPath();
}
