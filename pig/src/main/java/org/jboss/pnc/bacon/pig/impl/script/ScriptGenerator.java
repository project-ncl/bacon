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
package org.jboss.pnc.bacon.pig.impl.script;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.documents.FileGenerator;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.dto.ProductMilestoneRef;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/4/18
 */
public class ScriptGenerator {

    public static final String SCRIPT_NAME = "generate-nvr-list.sh";
    private final Config config;
    private final Deliverables deliverables;

    public ScriptGenerator(Config config, Deliverables deliverables) {
        this.config = config;
        this.deliverables = deliverables;
    }

    public void generateReleaseScripts(ProductMilestoneRef milestone, Path repoZipLocation, Path targetDir, Path releaseDir,
            String brewTag, List<String> buildIdsToPush) {
        ReleaseScriptData dataRoot = getReleaseScriptData(milestone, repoZipLocation, targetDir, releaseDir, brewTag,
                buildIdsToPush);
        generateCloseMilestoneScript(targetDir, dataRoot);
        generateUploadToCandidatesScript(targetDir, dataRoot);
    }

    private void generateUploadToCandidatesScript(Path targetDir, ReleaseScriptData dataRoot) {
        FileGenerator generator = new FileGenerator(Optional.empty());

        File uploadScriptLocation = targetDir.resolve("upload-to-candidates.sh").toFile();

        generator.generateFileFromResource(dataRoot, "uploadToCandidates.sh", uploadScriptLocation);
    }

    private void generateCloseMilestoneScript(Path targetPath, ReleaseScriptData dataRoot) {

        FileGenerator generator = new FileGenerator(Optional.empty());

        File releaseScriptLocation = targetPath.resolve("release.sh").toFile();

        generator.generateFileFromResource(dataRoot, "release.sh", releaseScriptLocation);
    }

    private ReleaseScriptData getReleaseScriptData(ProductMilestoneRef milestone, Path repoZipLocation, Path targetDir,
            Path releaseDir, String brewTag, List<String> buildIdsToPush) {
        String nvrListName = deliverables.getNvrListName();
        String nvrScriptLocation = extractNvrListScript(targetDir.toFile());
        String nvrListPath = releaseDir.resolve(nvrListName).toAbsolutePath().toString();
        String productWithVersion = config.getProduct().prefix() + "-" + config.getVersion() + "." + config.getMilestone();

        return new ReleaseScriptData(milestone.getId(), nvrScriptLocation, repoZipLocation.toAbsolutePath().toString(),
                nvrListPath, productWithVersion, brewTag, buildIdsToPush);
    }

    private String extractNvrListScript(File targetDir) {
        File nvrListScriptFile = new File(targetDir, SCRIPT_NAME);
        File scriptFile = ResourceUtils.extractToFile("/" + SCRIPT_NAME, nvrListScriptFile);
        return scriptFile.getAbsolutePath();
    }

    @Data
    @AllArgsConstructor
    public static class ReleaseScriptData {
        private String milestoneId;
        private String nvrListScriptLocation;
        private String repoZipLocation;
        private String targetPath;
        private String productWithVersion;
        private String brewTag;
        private List<String> buildsToPush;
    }
}
