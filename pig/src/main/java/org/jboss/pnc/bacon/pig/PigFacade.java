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
import org.jboss.pnc.bacon.pig.impl.config.GroupBuildInfo;
import org.jboss.pnc.bacon.pig.impl.config.JavadocGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.LicenseGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.documents.DocumentGenerator;
import org.jboss.pnc.bacon.pig.impl.javadoc.JavadocManager;
import org.jboss.pnc.bacon.pig.impl.license.LicenseManager;
import org.jboss.pnc.bacon.pig.impl.nvr.NvrListGenerator;
import org.jboss.pnc.bacon.pig.impl.out.PigReleaseOutput;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.ImportResult;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuilder;
import org.jboss.pnc.bacon.pig.impl.pnc.PncEntitiesImporter;
import org.jboss.pnc.bacon.pig.impl.repo.RepoDescriptor;
import org.jboss.pnc.bacon.pig.impl.repo.RepoManager;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.script.ScriptGenerator;
import org.jboss.pnc.bacon.pig.impl.sources.SourcesGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.sources.SourcesGenerator;
import org.jboss.pnc.bacon.pig.impl.utils.BuildFinderUtils;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.ProductVersionClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildPushResult;
import org.jboss.pnc.dto.GroupBuild;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.requests.BuildPushParameters;
import org.jboss.pnc.enums.BuildPushStatus;
import org.jboss.pnc.enums.RebuildMode;
import org.jboss.pnc.restclient.AdvancedBuildClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * TODO: javadoc
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/1/17
 */
public final class PigFacade {

    private static final Logger log = LoggerFactory.getLogger(PigFacade.class);

    private PigFacade() {
    }

    public static ImportResult configure(boolean skipBranchCheck, boolean temporaryBuild) {
        try (PncEntitiesImporter pncImporter = new PncEntitiesImporter()) {
            return pncImporter.performImport(skipBranchCheck, temporaryBuild);
        }
    }

    public static ImportResult readPncEntities() {
        try (PncEntitiesImporter pncImporter = new PncEntitiesImporter()) {
            return pncImporter.readCurrentPncEntities();
        }
    }

    public static GroupBuildInfo build(boolean tempBuild, boolean tempBuildTS, RebuildMode rebuildMode, boolean wait) {
        ImportResult importResult = context().getPncImportResult();
        if (importResult == null) {
            importResult = readPncEntities();
        }

        if (tempBuild) {
            log.info("Temporary build");
        }

        try (PncBuilder pncBuilder = new PncBuilder()) {
            GroupBuild groupBuild = pncBuilder
                    .build(importResult.getBuildGroup(), tempBuild, tempBuildTS, rebuildMode, wait);
            if (wait) {
                try (BuildInfoCollector buildInfoCollector = new BuildInfoCollector()) {
                    return buildInfoCollector.getBuildsFromGroupBuild(groupBuild);
                }
            }
            log.info("Not waiting for build to finish.");
            return null;
        }
    }

    public static GroupBuildInfo run(
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
            boolean skipBranchCheck,
            boolean strictLicenseCheck,
            Path configurationDirectory) {

        PigContext context = context();

        ImportResult importResult;
        if (skipPncUpdate) {
            importResult = readPncEntities();
        } else {
            importResult = configure(skipBranchCheck, tempBuild);
        }
        context.setPncImportResult(importResult);
        context.storeContext();

        GroupBuildInfo groupBuildInfo;
        if (skipBuilds) {
            log.info("Skipping builds");
            groupBuildInfo = getBuilds(importResult, tempBuild);
        } else {
            if (tempBuild) {
                log.info("Temporary build");
            }
            groupBuildInfo = build(tempBuild, tempBuildTS, rebuildMode, true);
        }

        context.setBuilds(groupBuildInfo.getBuilds());
        context.storeContext();

        // TODO: there seems to be a gap between the build configs assigned to the product version
        // TODO: and build group
        // TODO: It is possible that someone adds a build config to the product version but not to the build group
        // TODO: should we bother with this case?

        RepositoryData repo = null;

        if (repoZipPath != null || context.getPigConfiguration()
                .getFlow()
                .getRepositoryGeneration()
                .getStrategy() != RepoGenerationStrategy.IGNORE) {
            if (repoZipPath != null) {
                repo = parseRepository(new File(repoZipPath));
            } else {
                repo = generateRepo(removeGeneratedM2Dups, configurationDirectory, strictLicenseCheck);
            }
            context.setRepositoryData(repo);
            context.storeContext();
        } else {
            log.info("Skipping Repo Generation");
        }

        if (!(skipSources || context().getPigConfiguration()
                .getFlow()
                .getSourcesGeneration()
                .getStrategy() == SourcesGenerationStrategy.IGNORE)) {
            generateSources();

        } else {
            log.info("Skipping Source Generation");
        }

        if (!(skipJavadoc || context.getPigConfiguration()
                .getFlow()
                .getJavadocGeneration()
                .getStrategy() == JavadocGenerationStrategy.IGNORE)) {
            generateJavadoc();
        } else {
            log.info("Skipping Javadoc Generation");
        }

        if (!(skipLicenses || context.getPigConfiguration()
                .getFlow()
                .getLicensesGeneration()
                .getStrategy() == LicenseGenerationStrategy.IGNORE)) {
            generateLicenses(strictLicenseCheck);
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

        log.info("PiG run completed, the results are in: {}", Paths.get(context().getTargetPath()).toAbsolutePath());
        return groupBuildInfo;
    }

    public static PigReleaseOutput release() {

        abortIfBuildDataAbsentFromContext();
        pushToBrew(false);

        // if repository data not present, skip generation of nvr list and upload script
        if (context().getRepositoryData() == null) {
            log.info("Skipping generation of nvr list and upload script since repository has not been generated");
            return new PigReleaseOutput("", "", "");
        }

        generateNvrList();

        // generate upload to candidates script
        ScriptGenerator scriptGenerator = new ScriptGenerator(context().getPigConfiguration());
        scriptGenerator.generateReleaseScripts(Paths.get(context().getTargetPath()));

        PigContext context = PigContext.get();
        return new PigReleaseOutput(
                context.getReleaseDirName(),
                context.getReleasePath(),
                context.getDeliverables().getNvrListName());
    }

    /**
     * Generate the nvr list from the information gathered during repository generation. If the latter was ignored, then
     * nothing happens
     */
    private static void generateNvrList() {
        PigContext context = PigContext.get();
        Map<String, Collection<String>> checksums = context.getChecksums();

        if (checksums == null) {
            // checksums populated only when repository generation is switched on
            log.warn("No nvr list generated since repository generation may have been ignored");
        } else {
            Path targetPath = Paths.get(context.getReleasePath())
                    .resolve(context.getDeliverables().getNvrListName())
                    .toAbsolutePath();
            NvrListGenerator.generateNvrList(checksums, targetPath);
        }
    }

    private static void pushToBrew(boolean reimport) {
        abortIfBuildDataAbsentFromContext();
        Map<String, PncBuild> builds = PigContext.get().getBuilds();
        String tagPrefix = getBrewTag(context().getPncImportResult().getVersion());
        List<PncBuild> buildsToPush = getBuildsToPush(builds);
        if (log.isInfoEnabled()) {
            log.info(
                    "Pushing the following builds to brew: {}",
                    buildsToPush.stream().map(PncBuild::getId).collect(Collectors.toList()));
        }
        for (PncBuild build : buildsToPush) {
            BuildPushParameters request = BuildPushParameters.builder().tagPrefix(tagPrefix).reimport(reimport).build();

            // TODO: customize the timeout
            try (AdvancedBuildClient pushingClient = new AdvancedBuildClient(PncClientHelper.getPncConfiguration())) {
                BuildPushResult pushResult = pushingClient
                        .executeBrewPush(build.getId(), request, 15L, TimeUnit.MINUTES);
                if (pushResult.getStatus() != BuildPushStatus.SUCCESS) {
                    throw new RuntimeException(
                            "Failed to push build " + build.getId() + " to brew. Push result: " + pushResult);
                }
                log.info("{} pushed to brew", build.getId());
            } catch (RemoteResourceException e) {
                throw new RuntimeException("Failed to push build " + build.getId() + " to brew", e);
            }
        }
    }

    public static void generateDocuments() {
        abortIfContextDataAbsent();
        DocumentGenerator docGenerator = new DocumentGenerator(
                context().getPigConfiguration(),
                context().getReleasePath(),
                context().getExtrasPath(),
                context().getDeliverables());
        docGenerator.generateDocuments(context().getBuilds(), context().getRepositoryData());
    }

    public static void prepareSharedContentAnalysis() {
        abortIfContextDataAbsent();
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
        abortIfContextDataAbsent();
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

    private static String getBrewTag(ProductVersionRef versionRef) {
        try (ProductVersionClient productVersionClient = new ProductVersionClient(
                PncClientHelper.getPncConfiguration())) {
            String versionId = versionRef.getId();

            ProductVersion version;
            try {
                version = productVersionClient.getSpecific(versionId);
            } catch (RemoteResourceException e) {
                throw new RuntimeException("Unable to get product version for " + versionId, e);
            }
            return version.getAttributes().get("BREW_TAG_PREFIX");
        }
    }

    private static List<PncBuild> getBuildsToPush(Map<String, PncBuild> builds) {
        return builds.values().stream().filter(PigFacade::notPushedToBrew).collect(Collectors.toList());
    }

    private static boolean notPushedToBrew(PncBuild build) {
        try (BuildClient buildClient = new BuildClient(PncClientHelper.getPncConfiguration())) { // todo factory or sth
            BuildPushResult pushResult;
            try {
                pushResult = buildClient.getPushResult(build.getId());
            } catch (ClientException e) {
                // Didn't find results with 404 exception, therefore it's not pushed
                if (e.getCause().getClass().isAssignableFrom(NotFoundException.class)) {
                    return true;
                } else {
                    throw new RuntimeException("Failed to get push info of build " + build.getId(), e);
                }
            }
            return pushResult == null || pushResult.getStatus() != BuildPushStatus.SUCCESS;
        }
    }

    private static RepositoryData parseRepository(File repositoryZipPath) {
        File extracted = FileUtils.mkTempDir("extractedRepo");

        FileUtils.unzip(repositoryZipPath, extracted);

        RepositoryData result = new RepositoryData();
        result.setFiles(RepoDescriptor.listFiles(extracted));
        result.setGavs(RepoDescriptor.listGavs(extracted));
        result.setRepositoryPath(repositoryZipPath.toPath());
        return result;
    }

    public static void triggerAddOns() {
        abortIfBuildDataAbsentFromContext();
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

    public static RepositoryData generateRepo(
            boolean removeGeneratedM2Dups,
            Path configurationDirectory,
            boolean strictLicenseCheck) {
        abortIfBuildDataAbsentFromContext();
        PigContext context = context();
        try (RepoManager repoManager = new RepoManager(
                context.getPigConfiguration(),
                context.getReleasePath(),
                context.getDeliverables(),
                context.getBuilds(),
                configurationDirectory,
                removeGeneratedM2Dups,
                strictLicenseCheck)) {

            RepositoryData repositoryData = repoManager.prepare();

            if (repositoryData != null) {
                File repoZip = repositoryData.getRepositoryPath().toAbsolutePath().toFile();

                context.setChecksums(BuildFinderUtils.findChecksums(repoZip));
                context.storeContext();
            }

            return repositoryData;
        }
    }

    /**
     * From the group configuration defined in the import result, get the latest successful builds in it Used only in
     * with --skipBuilds
     *
     * @param importResult Data from the 'configuration' part. Will contain information about the group configuration
     *        used to trigger the builds
     * @param tempBuild whether the build performed was temporary or not
     * @return GroupBuildInfo that contains the group build, and the map of 'build config name' to PncBuild
     */
    private static GroupBuildInfo getBuilds(ImportResult importResult, boolean tempBuild) {
        try (BuildInfoCollector buildInfoCollector = new BuildInfoCollector()) {
            return buildInfoCollector
                    .getBuildsFromLatestGroupConfiguration(importResult.getBuildGroup().getId(), tempBuild);
        }
    }

    public static void generateLicenses(boolean strict) {
        abortIfContextDataAbsent();
        PigContext context = context();
        PigConfiguration pigConfiguration = context.getPigConfiguration();
        RepositoryData repo = context.getRepositoryData();

        Map<String, PncBuild> builds = context.getBuilds();
        new LicenseManager(pigConfiguration, context.getReleasePath(), strict, context.getDeliverables(), builds, repo)
                .prepare();
    }

    public static void generateJavadoc() {
        abortIfContextDataAbsent();
        PigConfiguration pigConfiguration = context().getPigConfiguration();
        Map<String, PncBuild> builds = context().getBuilds();
        new JavadocManager(pigConfiguration, context().getReleasePath(), context().getDeliverables(), builds).prepare();
    }

    /**
     * Throws RuntimeException if the build or repository data is not present
     */
    private static void abortIfContextDataAbsent() {
        abortIfBuildDataAbsentFromContext();
        abortIfRepositoryDataAbsentFromContext();
    }

    private static void abortIfBuildDataAbsentFromContext() {
        if (context().getBuilds() == null) {
            throw new RuntimeException("No build data available. Please make sure to run `pig run` before");
        }
    }

    private static void abortIfRepositoryDataAbsentFromContext() {
        if (context().getRepositoryData() == null) {
            throw new RuntimeException(
                    "No repository data available for document generation. Please make sure to run `pig repo` before");
        }
    }

}
