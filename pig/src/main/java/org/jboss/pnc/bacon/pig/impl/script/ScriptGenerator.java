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
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.FileGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/4/18
 */
public class ScriptGenerator {
    private static final Logger log = LoggerFactory.getLogger(ScriptGenerator.class);

    private final PigConfiguration pigConfiguration;

    public ScriptGenerator(PigConfiguration pigConfiguration) {
        this.pigConfiguration = pigConfiguration;
    }

    public void generateReleaseScripts(Path targetDir) {
        String version = PigContext.get().getFullVersion();
        String productWithVersion = pigConfiguration.getProduct().prefix() + "-" + version;

        ReleaseScriptData dataRoot = new ReleaseScriptData(productWithVersion);
        generateUploadToCandidatesScript(targetDir, dataRoot);
    }

    private static void generateUploadToCandidatesScript(Path targetDir, ReleaseScriptData dataRoot) {
        FileGenerator generator = new FileGenerator(Optional.empty());

        File uploadScriptLocation = targetDir.resolve("upload-to-candidates.sh").toFile();

        generator.generateFileFromResource(dataRoot, "uploadToCandidates.sh", uploadScriptLocation);

        makeScriptExecutable(uploadScriptLocation.toPath());
    }

    private static void makeScriptExecutable(Path script) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(script);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(script, permissions);
        } catch (IOException | UnsupportedOperationException e) {
            log.info("Couldn't make script {} executable: {}", script, e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    public static class ReleaseScriptData {
        private String productWithVersion;
    }
}
