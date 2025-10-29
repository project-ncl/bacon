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
package org.jboss.pnc.bacon.pig.impl.nvr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.utils.BuildFinderUtils;
import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.build.finder.report.NVRReport;
import org.jboss.pnc.build.finder.report.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NvrListGenerator {
    private static final Logger log = LoggerFactory.getLogger(NvrListGenerator.class);

    private NvrListGenerator() {
    }

    public static boolean generateNvrList(Map<String, Collection<String>> checksums, Path targetPath) {
        log.info("Generating NVR list for {} checksums and saving result to {}", checksums.size(), targetPath);

        List<KojiBuild> builds = BuildFinderUtils.findBuilds(checksums, false);
        File outputDirectory = targetPath.normalize().getParent().toFile();
        Report nvrReport = new NVRReport(outputDirectory.toPath(), baseFileName(targetPath), builds);

        try {
            nvrReport.outputText();
        } catch (IOException e) {
            log.error("Failed to write file {} to {}: {}", targetPath, outputDirectory, e.getMessage(), e);
            return false;
        }

        return true;
    }

    private static String baseFileName(Path targetPath) {
        String fileName = targetPath.getFileName().toString();
        String txtExtension = ".txt";
        if (!fileName.endsWith(txtExtension)) {
            throw new RuntimeException(
                    "target file for the NVR list has to finish with .txt, provided path: " + targetPath);
        }
        return fileName.replaceAll("\\.txt$", "");
    }
}
