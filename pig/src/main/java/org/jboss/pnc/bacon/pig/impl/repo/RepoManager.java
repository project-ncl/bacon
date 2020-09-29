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
package org.jboss.pnc.bacon.pig.impl.repo;

import lombok.Getter;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.common.DeliverableManager;
import org.jboss.pnc.bacon.pig.impl.config.AdditionalArtifactsFromBuild;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.license.LicenseGenerator;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.enums.RepositoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/23/17
 */
public class RepoManager extends DeliverableManager<RepoGenerationData, RepositoryData> implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(RepoManager.class);

    private final BuildInfoCollector buildInfoCollector;
    @Getter
    private final RepoGenerationData generationData;
    private File targetRepoContentsDir;
    private final boolean removeGeneratedM2Dups;
    private final Path configurationDirectory;
    private final boolean strictLicenseCheck;

    public RepoManager(
            PigConfiguration pigConfiguration,
            String releasePath,
            Deliverables deliverables,
            Map<String, PncBuild> builds,
            Path configurationDirectory,
            boolean removeGeneratedM2Dups,
            boolean strictLicenseCheck) {
        super(pigConfiguration, releasePath, deliverables, builds);
        generationData = pigConfiguration.getFlow().getRepositoryGeneration();
        this.removeGeneratedM2Dups = removeGeneratedM2Dups;
        this.configurationDirectory = configurationDirectory;
        this.strictLicenseCheck = strictLicenseCheck;
        buildInfoCollector = new BuildInfoCollector();
    }

    public RepositoryData prepare() {
        switch (generationData.getStrategy()) {
            case DOWNLOAD:
                return downloadAndRepackage();
            case GENERATE:
                return generate();
            // PACK_ALL is deprecated, replaced with BUILD_CONFIGS, remove in next major version
            case PACK_ALL:
                return packAllBuiltAndDependencies();
            case BUILD_GROUP:
                return buildGroup();
            case MILESTONE:
                return milestone();
            case BUILD_CONFIGS:
                return buildConfigs();
            case OFFLINER_ONLY:
                generateOfflinerManifest();
                return null;
            case IGNORE:
                log.info(
                        "Ignoring repository zip generation because the config strategy is set to: {}",
                        RepoGenerationStrategy.IGNORE);
                return null;
            default:
                throw new IllegalStateException("Unsupported repository generation strategy");
        }
    }

    void getRedhatArtifacts(List<ArtifactWrapper> artifactsToPack, PncBuild build) {
        log.info("Getting all artifacts and dependencies for [{}]", build.getName());
        buildInfoCollector.addDependencies(build, "identifier=like=%redhat%");
        artifactsToPack.addAll(build.getBuiltArtifacts());
        artifactsToPack.addAll(build.getDependencyArtifacts());
    }

    private File createMavenGenerationDir() {
        File sourceDir = new File(workDir, "maven-repository");
        sourceDir.mkdirs();
        return sourceDir;
    }

    private void filterAndDownload(List<ArtifactWrapper> artifactsToPack, File sourceDir) {
        artifactsToPack.removeIf(
                artifact -> !artifact.getGapv().contains("redhat-")
                        && !artifact.getGapv().contains("eap-runtime-artifacts"));
        artifactsToPack.removeIf(artifact -> isArtifactExcluded(artifact.getGapv()));
        List<GAV> originalListToPack = artifactsToPack.stream()
                .map(ArtifactWrapper::toGAV)
                .collect(Collectors.toList());

        Set<GAV> gavsToPack = new TreeSet<>(GAV.gapvcComparator);
        gavsToPack.addAll(originalListToPack);
        originalListToPack.stream()
                .filter(GAV::isNormalJar)
                .filter(g -> areSourcesMissing(gavsToPack, g))
                .map(GAV::toSourcesJar)
                .forEach(gavsToPack::add);

        originalListToPack.stream()
                .filter(GAV::isNormalJar)
                .filter(g -> isPomMissing(gavsToPack, g))
                .map(GAV::toPom)
                .forEach(gavsToPack::add);

        if (pigConfiguration.getFlow().getRepositoryGeneration().isIncludeJavadoc()) {
            originalListToPack.stream()
                    .filter(GAV::isNormalJar)
                    .filter(g -> areJavadocsMissing(gavsToPack, g))
                    .map(GAV::toJavadocJar)
                    .forEach(gavsToPack::add);
        }
        gavsToPack.forEach(a -> ExternalArtifactDownloader.downloadExternalArtifact(a, sourceDir.toPath(), true));
    }

    @Deprecated
    private RepositoryData packAllBuiltAndDependencies() {
        log.warn("Repo generation stratagy 'PACK_ALL' is deprecated please use BUILD_CONFIGS");
        PncBuild build = getBuild(generationData.getSourceBuild());
        List<ArtifactWrapper> artifactsToPack = new ArrayList<>();
        getRedhatArtifacts(artifactsToPack, build);
        File sourceDir = createMavenGenerationDir();
        filterAndDownload(artifactsToPack, sourceDir);
        return repackage(sourceDir);
    }

    private RepositoryData buildGroup() {
        log.info("Generating maven repo for build group [{}]", pigConfiguration.getGroup());
        List<ArtifactWrapper> artifactsToPack = new ArrayList<>();
        for (PncBuild build : builds.values()) {
            getRedhatArtifacts(artifactsToPack, build);
        }
        File sourceDir = createMavenGenerationDir();
        filterAndDownload(artifactsToPack, sourceDir);
        if (generationData.isIncludeOffliner()) {
            generateOfflinerManifest();
        }
        return repackage(sourceDir);
    }

    private RepositoryData buildConfigs() {
        log.info("Generating maven repo for named build configs");
        List<ArtifactWrapper> artifactsToPack = new ArrayList<>();
        for (String buildConfigName : generationData.getSourceBuilds()) {
            PncBuild build = getBuild(buildConfigName);
            getRedhatArtifacts(artifactsToPack, build);
        }
        File sourceDir = createMavenGenerationDir();
        filterAndDownload(artifactsToPack, sourceDir);
        if (generationData.isIncludeOffliner()) {
            generateOfflinerManifest();
        }
        return repackage(sourceDir);
    }

    private static RepositoryData milestone() {
        log.info("Generating maven repo for milestone [{}]", PigContext.get().getFullVersion());
        // TODO
        throw new FatalException("Not yet implemented");
    }

    private static boolean areSourcesMissing(Set<GAV> list, GAV gav) {
        return list.stream().noneMatch(a -> a.equals(gav) && "sources".equals(a.getClassifier()));
    }

    private static boolean isPomMissing(Set<GAV> list, GAV gav) {
        return list.stream().noneMatch(a -> a.equals(gav) && "pom".equals(a.getPackaging()));
    }

    private static boolean areJavadocsMissing(Set<GAV> list, GAV gav) {
        return list.stream().noneMatch(a -> a.equals(gav) && "javadoc".equals(a.getClassifier()));
    }

    private boolean isArtifactExcluded(String artifact) {
        List<String> excludeArtifacts = pigConfiguration.getFlow().getRepositoryGeneration().getExcludeArtifacts();
        for (String exclusion : excludeArtifacts) {
            if (Pattern.matches(exclusion, artifact)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected RepositoryData downloadAndRepackage() {
        log.info("downloading and repackaging maven repository");
        File sourceTopLevelDirectory = download();
        return repackage(sourceTopLevelDirectory);
    }

    private RepositoryData repackage(File sourceTopLevelDirectory) {
        log.info("repackaging the repository");
        File targetTopLevelDirectory = new File(workDir, getTargetTopLevelDirectoryName());

        Path targetZipPath = getTargetZipPath();
        targetTopLevelDirectory.mkdirs();
        repackage(sourceTopLevelDirectory, targetTopLevelDirectory);

        addAdditionalArtifacts();

        ParentPomDownloader.addParentPoms(targetRepoContentsDir.toPath());

        RepositoryUtils.removeCommunityArtifacts(targetRepoContentsDir);
        RepositoryUtils.removeIrrelevantFiles(targetRepoContentsDir);

        addMissingSources();

        RepositoryUtils.addCheckSums(targetRepoContentsDir);
        if (generationData.isIncludeMavenMetadata()) {
            RepositoryUtils.generateMavenMetadata(targetRepoContentsDir);
        }
        zip(targetTopLevelDirectory, targetZipPath);

        return result(targetTopLevelDirectory, targetZipPath);
    }

    private void addMissingSources() {
        Collection<GAV> gavs = RepoDescriptor.listGavs(targetRepoContentsDir);

        for (GAV gav : gavs) {
            GAV sourceGav = gav.toSourcesJar();
            GAV jarGav = gav.toJar();

            File jarFile = ExternalArtifactDownloader.targetPath(jarGav, targetRepoContentsDir.toPath());
            File sourceFile = ExternalArtifactDownloader.targetPath(sourceGav, targetRepoContentsDir.toPath());

            if (jarFile.exists() && !sourceFile.exists()) {
                ExternalArtifactDownloader.downloadExternalArtifact(sourceGav, sourceFile, true);
            }
        }
    }

    private File download() {
        PncBuild build = getBuild(generationData.getSourceBuild());
        File downloadedZip = new File(workDir, "downloaded.zip");

        build.downloadArtifact(generationData.getSourceArtifact(), downloadedZip);

        File extractedZip = unzip(downloadedZip);
        return getTopLevelDirectory(extractedZip);
    }

    private void addAdditionalArtifacts() {
        List<AdditionalArtifactsFromBuild> artifactList = generationData.getAdditionalArtifacts();
        artifactList.forEach(artifacts -> {
            PncBuild build = getBuild(artifacts.getFrom());
            artifacts.getDownload().forEach(regex -> downloadArtifact(build.findArtifact(regex)));
        });

        generationData.getExternalAdditionalArtifacts()
                .stream()
                .map(GAV::fromColonSeparatedGAPV)
                .forEach(this::downloadExternalArtifact);
    }

    private void downloadExternalArtifact(GAV gav) {
        ExternalArtifactDownloader.downloadExternalArtifact(gav, targetRepoContentsDir.toPath(), false);
    }

    private void downloadArtifact(ArtifactWrapper artifact) {
        Path versionPath = targetRepoContentsDir.toPath().resolve(artifact.toGAV().toVersionPath());
        versionPath.toFile().mkdirs();
        artifact.downloadToDirectory(versionPath);
    }

    public RepositoryData generate() {
        log.info("generating maven repository");
        PncBuild build = getBuild(generationData.getSourceBuild());
        File bomDirectory = FileUtils.mkTempDir("repo-from-bom-generation");

        RepoBuilder repoBuilder = new RepoBuilder(
                pigConfiguration,
                generationData.getAdditionalRepo(),
                configurationDirectory,
                builds,
                removeGeneratedM2Dups);
        File bomFile = new File(bomDirectory, "bom.pom");
        build.downloadArtifact(generationData.getSourceArtifact(), bomFile);

        String topLevelDirectoryName = pigConfiguration.getTopLevelDirectoryPrefix() + "maven-repository";

        File repoWorkDir = FileUtils.mkTempDir("repository");
        File repoParentDir = new File(repoWorkDir, topLevelDirectoryName);
        List<Map<String, String>> stages = generationData.getStages();
        if (stages != null) {
            stages.forEach(stage -> repoBuilder.build(bomFile, repoParentDir, predicate(stage)));
        } else {
            repoBuilder.build(bomFile, repoParentDir, gav -> true);
        }

        return repackage(new File(repoParentDir, "maven-repository"));
    }

    private static Predicate<GAV> predicate(Map<String, String> stage) {
        String matching = stage.getOrDefault("matching", ".*");
        String notMatching = stage.getOrDefault("not-matching", "^$");
        return gav -> {
            String ga = String.format("%s:%s", gav.getGroupId(), gav.getArtifactId());
            return ga.matches(matching) && !ga.matches(notMatching);
        };
    }

    @Override
    protected Path getTargetZipPath() {
        return Paths.get(releasePath, deliverables.getRepositoryZipName());
    }

    @Override
    protected String getTargetTopLevelDirectoryName() {
        return pigConfiguration.getTopLevelDirectoryPrefix() + "maven-repository";
    }

    private static RepositoryData result(File targetTopLevelDirectory, Path targetZipPath) {
        RepositoryData result = new RepositoryData();
        File contentsDirectory = new File(targetTopLevelDirectory, "maven-repository");
        result.setFiles(RepoDescriptor.listFiles(contentsDirectory));
        result.setGavs(RepoDescriptor.listGavs(contentsDirectory));
        result.setRepositoryPath(targetZipPath);
        log.info("created repository: {}", targetZipPath);
        return result;
    }

    @Override
    protected void repackage(File contentsDirectory, File targetTopLevelDirectory) {
        targetRepoContentsDir = new File(targetTopLevelDirectory, RepoDescriptor.MAVEN_REPOSITORY);
        FileUtils.copy(contentsDirectory, targetRepoContentsDir);
        addExtraFiles(targetTopLevelDirectory);
    }

    private void addExtraFiles(File m2Repo) {
        log.debug("Adding repository documents");
        Properties properties = new Properties();
        properties.put("PRODUCT_NAME", pigConfiguration.getProduct().getName());

        ResourceUtils.copyResourceWithFiltering(
                "/repository-example-settings.xml",
                "example-settings.xml",
                m2Repo,
                properties,
                configurationDirectory);
        ResourceUtils.copyResourceWithFiltering(
                "/repository-README.md",
                "README.md",
                m2Repo,
                properties,
                configurationDirectory);

        if (generationData.isIncludeLicenses()) {
            LicenseGenerator.generateLicenses(
                    RepoDescriptor.listGavs(new File(m2Repo, RepoDescriptor.MAVEN_REPOSITORY)),
                    new File(m2Repo, "licenses"),
                    PigContext.get().isTempBuild(),
                    strictLicenseCheck);
        }
    }

    @Override
    public void close() {
        buildInfoCollector.close();
    }

    private void generateOfflinerManifest() {
        log.info("Will generate the Offline manifest");
        RepositoryData result = new RepositoryData();
        List<ArtifactWrapper> artifactsToListRaw = new ArrayList<>();
        for (PncBuild build : builds.values()) {
            artifactsToListRaw.addAll(build.getBuiltArtifacts());
            // TODO: Add filter, basing on the targetRepository.repositoryType, when
            // https://projects.engineering.redhat.com/browse/NCL-6079 is done
            buildInfoCollector.addDependencies(build, "");
            artifactsToListRaw.addAll(build.getDependencyArtifacts());
            log.debug("Dependencies for build {}: {}" + build.getId(), build.getDependencyArtifacts().size());
        }
        log.debug("Number of collected artifacts for the Offline manifest: {}", artifactsToListRaw.size());

        artifactsToListRaw.removeIf(artifact -> isArtifactExcluded(artifact.getGapv()));
        log.debug("Number of collected artifacts after exclusion: {}", artifactsToListRaw.size());
        Map<String, ArtifactWrapper> artifactsToList = new HashMap<>();
        for (ArtifactWrapper artifact : artifactsToListRaw) {
            artifactsToList.put(artifact.getGapv(), artifact);
        }
        log.info("Number of collected artifacts without duplicates: {}", artifactsToList.size());

        PrintWriter file = null;
        String filename = releasePath + getGenerationData().getOfflinerManifest();
        try {
            file = new PrintWriter(filename);
            for (Map.Entry<String, ArtifactWrapper> artifactEntry : artifactsToList.entrySet()) {
                ArtifactWrapper artifact = artifactEntry.getValue();
                // TODO: Remove the check, when https://projects.engineering.redhat.com/browse/NCL-6079 is done
                if (artifact.getRepositoryType().equals(RepositoryType.MAVEN)) {
                    file.println(artifact.getSha256() + "," + artifact.toGAV().toVersionPath());
                }

            }
            List<String> extraGavs = generationData.getExternalAdditionalArtifacts()
                    .stream()
                    .map(GAV::fromColonSeparatedGAPV)
                    .map(GAV::toVersionPath)
                    .collect(Collectors.toList());
            for (String extraGav : extraGavs) {
                file.println(extraGav);
            }
            result.setFiles(
                    Arrays.asList(
                            new File(getTargetTopLevelDirectoryName() + getGenerationData().getOfflinerManifest())));

        } catch (Exception e) {
            log.error("Error generating the Offline manifest", e);
        } finally {
            if (file != null) {
                file.flush();
                file.close();
            }
        }
    }
}
