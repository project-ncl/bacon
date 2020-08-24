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

import com.redhat.red.build.finder.KojiBuild;
import com.redhat.red.build.finder.report.NVRReport;
import com.redhat.red.build.finder.report.Report;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jboss.pnc.bacon.pig.impl.utils.BuildFinderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class NvrListGenerator {
    private static final Logger log = LoggerFactory.getLogger(NvrListGenerator.class);

    private NvrListGenerator() {
    }

    public static boolean generateNvrList(String repoZipPath, String targetPath) {
        log.info("Generating NVR list for {} and saving result to {}", repoZipPath, targetPath);

        List<KojiBuild> builds = BuildFinderUtils.findBuilds(repoZipPath, true);
        File outputDirectory = new File(FilenameUtils.getPath(targetPath));
        Report nvrReport = new NVRReport(outputDirectory, builds);
        String basename = nvrReport.getBaseFilename() + ".txt";

        try {
            nvrReport.outputText();
        } catch (IOException e) {
            log.error("Failed to write file {} to {}: {}", basename, outputDirectory, e.getMessage(), e);
            return false;
        }

        File srcFile = new File(outputDirectory, basename);
        File destFile = new File(targetPath);

        try {
            FileUtils.copyFile(srcFile, destFile, true);
        } catch (IOException e) {
            log.error("Failed to copy file {} to {}: {}", srcFile, destFile, e.getMessage(), e);
            return false;
        }

        return destFile.exists();
    }
}
