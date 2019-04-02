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

import com.redhat.red.build.finder.BuildConfig;
import com.redhat.red.build.finder.BuildFinder;
import com.redhat.red.build.finder.DistributionAnalyzer;
import com.redhat.red.build.finder.KojiBuild;
import com.redhat.red.build.finder.KojiClientSession;
import com.redhat.red.build.finder.report.NVRReport;
import com.redhat.red.build.finder.report.Report;
import com.redhat.red.build.koji.KojiClientException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.BrewSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/24/17
 */
public class NvrListGenerator {
    private static final Logger log = LoggerFactory.getLogger(NvrListGenerator.class);

    public static boolean generateNvrList(String repoZipPath, String targetPath) {
        log.info("Generating NVR list for {} and saving result to {}", repoZipPath, targetPath);

        BuildConfig config = BrewSearcher.getKojiBuildFinderConfig();
        DistributionAnalyzer da = new DistributionAnalyzer(Collections.singletonList(new File(repoZipPath)), config);
        Map<String, Collection<String>> checksumTable;

        try {
            checksumTable = da.checksumFiles().asMap();
        } catch (IOException e) {
            log.error("Failed to get checksums for {}: {}", repoZipPath, e.getMessage());
            return false;
        }

        KojiClientSession session;

        try {
            session = new KojiClientSession(config.getKojiHubURL());
        } catch (KojiClientException e) {
            log.error("Failed to create Koji session: {}", e.getMessage());
            return false;
        }

        Map<Integer, KojiBuild> builds;

        try {
            BuildFinder bf = new BuildFinder(session, config);
            builds = bf.findBuilds(checksumTable);
        } finally {
            session.close();
        }

        List<KojiBuild> buildList = new ArrayList<>(builds.values());
        buildList.sort(Comparator.comparingInt(b -> b.getBuildInfo().getId()));
        buildList = Collections.unmodifiableList(buildList);

        File outputDirectory = new File(FilenameUtils.getPath(targetPath));
        Report nvrReport = new NVRReport(outputDirectory, buildList);
        nvrReport.outputText();

        File srcFile = new File(outputDirectory, nvrReport.getBaseFilename() + ".txt");
        File destFile = new File(targetPath);

        try {
            FileUtils.moveFile(srcFile, destFile);
        } catch (IOException e) {
            log.error("Failed to move {} to {}: {}", srcFile, destFile, e.getMessage());
            return false;
        }

        return destFile.exists();
    }

    private NvrListGenerator() {
    }
}
