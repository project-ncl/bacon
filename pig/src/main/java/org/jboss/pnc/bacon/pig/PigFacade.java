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
package org.jboss.pnc.bacon.pig;

import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.addons.AddOnFactory;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.DocumentGenerator;
import org.jboss.pnc.bacon.pig.impl.javadoc.JavadocManager;
import org.jboss.pnc.bacon.pig.impl.license.LicenseManager;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildConfigData;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.ImportResult;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuilder;
import org.jboss.pnc.bacon.pig.impl.pnc.PncEntitiesImporter;
import org.jboss.pnc.bacon.pig.impl.repo.RepoDescriptor;
import org.jboss.pnc.bacon.pig.impl.repo.RepoManager;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.script.ScriptGenerator;
import org.jboss.pnc.bacon.pig.impl.sources.SourcesGenerator;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.dto.BuildPushResult;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.enums.BuildPushStatus;
import org.jboss.pnc.enums.RebuildMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * TODO: incremental suffix!
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/1/17
 */
public class PigFacade {

    private static final Logger log = LoggerFactory.getLogger(PigFacade.class);

    private PigFacade() {
    }

    public static ImportResult importPncEntities(boolean skipBranchCheck) {
        PncEntitiesImporter pncImporter = new PncEntitiesImporter();
        return pncImporter.performImport(skipBranchCheck);
    }

    public static ImportResult readPncEntities() {
        PncEntitiesImporter pncImporter = new PncEntitiesImporter();
        return pncImporter.readCurrentPncEntities();
    }

    public static Map<String, PncBuild> build(boolean tempBuild, boolean tempBuildTS, RebuildMode rebuildMode) {
        context().setTempBuild(tempBuild);
        ImportResult importResult = context().getPncImportResult();
        if (importResult == null) {
            importResult = readPncEntities();
        }

        if (tempBuild) {
            log.info("Temporary build");
        }

        new PncBuilder().buildAndWait(importResult.getBuildGroup(), tempBuild, tempBuildTS, rebuildMode);
        return getBuilds(importResult);
    }

    public static String run(
            boolean skipRepo,
            boolean skipPncUpdate,
            boolean skipBuilds,
            boolean skipSources,
            boolean skipJavadoc,
            boolean skipLicenses,
            boolean skipSharedContent,
            boolean removeGeneratedM2Dups,
            String repoZipPath,
            boolean tempBuild,
            boolean tempBuildTS,
            RebuildMode rebuildMode,
            boolean skipBranchCheck) {

        PigContext context = context();

        ImportResult importResult;
        if (skipPncUpdate) {
            importResult = readPncEntities();
        } else {
            importResult = importPncEntities(skipBranchCheck);
        }
        context.setPncImportResult(importResult);
        context.storeContext();

        Map<String, PncBuild> builds;
        if (skipBuilds) {
            log.info("Skipping builds");
            builds = getBuilds(importResult);
        } else {
            if (tempBuild) {
                log.info("Temprorary build");
            }
            builds = build(tempBuild, tempBuildTS, rebuildMode);
        }

        context.setBuilds(builds);
        context.storeContext();

        // TODO: there seems to be a gap between the build configs assigned to the product version
        // TODO: and build group
        // TODO: It is possible that someone adds a build config to the product version but not to the build group
        // TODO: should we bother with this case?

        RepositoryData repo = null;

        if (!skipRepo) {
            if (repoZipPath != null) {
                repo = parseRepository(new File(repoZipPath));
            } else {
                repo = PigFacade.generateRepo(removeGeneratedM2Dups);
            }
            context.setRepositoryData(repo);
            context.storeContext();
        } else {
            log.info("Skipping Maven Repository Generation");
        }

        if (!skipSources) {
            generateSources();

        } else {
            log.info("Skipping Source Generation");
        }

        if (!skipJavadoc) {
            generateJavadoc();
        } else {
            log.info("Skipping Javadoc Generation");
        }

        if (!skipLicenses && repo != null) {
            generateLicenses();
        } else {
            log.info("Skipping License Generation");
        }
        if (!skipSharedContent && repo != null) {
            prepareSharedContentAnalysis();
        }

        triggerAddOns();

        if (repo != null) {
            generateDocuments();
        } else {
            log.info("Skipping Document Generation");
        }

        if (!skipRepo && repo != null) {
            generateScripts();
        } else {
            log.info("Skipping Release Script Generation");
        }
        return "PiG run completed, the results are in: " + ""; // TODO target directory name

        // verifyZipContents(); TODO a separate Jenkins Job to do it?
    }

    public static void generateScripts() {
        ScriptGenerator scriptGenerator = new ScriptGenerator(
                context().getPigConfiguration(),
                context().getDeliverables());
        scriptGenerator.generateReleaseScripts(
                context().getPncImportResult().getMilestone(),
                context().getRepositoryData().getRepositoryPath(),
                Paths.get(context().getTargetPath()),
                Paths.get(context().getReleasePath()),
                getBrewTag(context().getPncImportResult().getVersion()),
                getBuildIdsToPush(context().getBuilds()));
    }

    public static void generateDocuments() {
        DocumentGenerator docGenerator = new DocumentGenerator(
                context().getPigConfiguration(),
                context().getReleasePath(),
                context().getExtrasPath(),
                context().getDeliverables());
        docGenerator.generateDocuments(context().getBuilds(), context().getRepositoryData());
    }

    public static void prepareSharedContentAnalysis() {
        try {
            DocumentGenerator docGenerator = new DocumentGenerator(
                    context().getPigConfiguration(),
                    context().getReleasePath(),
                    context().getExtrasPath(),
                    context().getDeliverables());
            docGenerator.generateSharedContentReport(context().getRepositoryData(), context().getBuilds());
        } catch (Exception any) {
            throw new RuntimeException("Failed to generate shared content request doc", any);
        }
    }

    public static void generateSources() {
        PigConfiguration pigConfiguration = context().getPigConfiguration();
        Map<String, PncBuild> builds = context().getBuilds();
        RepositoryData repo = context().getRepositoryData();
        SourcesGenerator sourcesGenerator = new SourcesGenerator(
                pigConfiguration.getFlow().getSourcesGeneration(),
                pigConfiguration.getTopLevelDirectoryPrefix() + "src",
                context().getReleasePath() + context().getDeliverables().getSourceZipName());
        sourcesGenerator.generateSources(builds, repo);
    }

    private static PigContext context() {
        return PigContext.get();
    }

    private static String getBrewTag(ProductVersionRef version) {
        return version.getAttributes().get("BREW_TAG_PREFIX");
    }

    private static List<String> getBuildIdsToPush(Map<String, PncBuild> builds) {
        return builds.values()
                .stream()
                .filter(PigFacade::notPushedToBrew)
                .map(PncBuild::getId)
                .collect(Collectors.toList());
    }

    private static boolean notPushedToBrew(PncBuild build) {
        BuildClient buildClient = new BuildClient(PncClientHelper.getPncConfiguration()); // todo factory or sth
        BuildPushResult pushResult = null;
        try {
            pushResult = buildClient.getPushResult(build.getId());
        } catch (ClientException e) {
            throw new RuntimeException("Failed to get push info of build " + build.getId(), e);
        }
        return pushResult != null && pushResult.getStatus() == BuildPushStatus.SUCCESS;
    }

    private static RepositoryData parseRepository(File repositoryZipPath) {
        File extracted = FileUtils.mkTempDir("extractedRepo");

        FileUtils.unzip(repositoryZipPath, extracted);

        RepositoryData result = new RepositoryData();
        result.setFiles(RepoDescriptor.listFiles(extracted));
        result.setGavs(RepoDescriptor.listGavs(extracted));
        result.setRepositoryPath(extracted.toPath());
        return result;
    }

    public static void triggerAddOns() {
        AddOnFactory
                .listAddOns(
                        context().getPigConfiguration(),
                        context().getBuilds(),
                        context().getReleasePath(),
                        context().getExtrasPath(),
                        context().getDeliverables())
                .stream()
                .filter(AddOn::shouldRun)
                .forEach(AddOn::trigger);
    }

    public static RepositoryData generateRepo(boolean removeGeneratedM2Dups) {
        PigContext context = context();
        RepoManager repoManager = new RepoManager(
                context.getPigConfiguration(),
                context().getReleasePath(),
                context().getDeliverables(),
                context().getBuilds(),
                Paths.get("."), // TODO!
                removeGeneratedM2Dups);
        return repoManager.prepare();
    }

    private static Map<String, PncBuild> getBuilds(ImportResult importResult) {
        BuildInfoCollector buildInfoCollector = new BuildInfoCollector();
        return importResult.getBuildConfigs()
                .parallelStream()
                .map(BuildConfigData::getId)
                .map(buildInfoCollector::getLatestBuild)
                .collect(Collectors.toMap(PncBuild::getName, Function.identity()));
    }

    public static void generateLicenses() {
        PigContext context = context();
        PigConfiguration pigConfiguration = context.getPigConfiguration();
        RepositoryData repo = context.getRepositoryData();
        Map<String, PncBuild> builds = context.getBuilds();
        new LicenseManager(pigConfiguration, context.getReleasePath(), context.getDeliverables(), builds, repo)
                .prepare();
    }

    public static void generateJavadoc() {
        PigConfiguration pigConfiguration = context().getPigConfiguration();
        Map<String, PncBuild> builds = context().getBuilds();
        new JavadocManager(pigConfiguration, context().getReleasePath(), context().getDeliverables(), builds).prepare();
    }
}
