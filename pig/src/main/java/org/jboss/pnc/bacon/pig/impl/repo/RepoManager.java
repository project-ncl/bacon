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
import org.jboss.pnc.bacon.pig.impl.common.DeliverableManager;
import org.jboss.pnc.bacon.pig.impl.config.AdditionalArtifactsFromBuild;
import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/23/17
 */
public class RepoManager extends DeliverableManager<RepoGenerationData, RepositoryData> {
    private static final Logger log = LoggerFactory.getLogger(RepoManager.class);

    private final BuildInfoCollector buildInfoCollector;
    @Getter
    private final RepoGenerationData generationData;
    private File targetRepoContentsDir;
    private final boolean removeGeneratedM2Dups;
    private final Path configurationDirectory;

    public RepoManager(Config config, String releasePath, Deliverables deliverables, Map<String, PncBuild> builds,
            Path configurationDirectory, boolean removeGeneratedM2Dups) {
        super(config, releasePath, deliverables, builds);
        generationData = config.getFlow().getRepositoryGeneration();
        this.removeGeneratedM2Dups = removeGeneratedM2Dups;
        this.configurationDirectory = configurationDirectory;
        buildInfoCollector = new BuildInfoCollector();
    }

    public RepositoryData prepare() {
        switch (generationData.getStrategy()) {
            case DOWNLOAD:
                return downloadAndRepackage();
            case GENERATE:
                return generate();
            case PACK_ALL:
                return packAllBuiltAndDependencies();
            case IGNORE:
                log.info("Ignoring repository zip generation");
                return null;
            default:
                throw new IllegalStateException("Unsupported repository generation strategy");
        }
    }

    private RepositoryData packAllBuiltAndDependencies() {
        PncBuild build = getBuild(generationData.getSourceBuild());

        buildInfoCollector.addDependencies(build);
        List<ArtifactWrapper> artifactsToPack = build.getBuiltArtifacts();
        artifactsToPack.addAll(build.getDependencyArtifacts());

        artifactsToPack.removeIf(
                artifact -> !artifact.getGapv().contains("redhat-") && !artifact.getGapv().contains("eap-runtime-artifacts"));

        File sourceDir = new File(workDir, "maven-repository");
        sourceDir.mkdirs();

        artifactsToPack.forEach(a -> ExternalArtifactDownloader.downloadExternalArtifact(a.toGAV(), sourceDir.toPath()));
        return repackage(sourceDir);
    }

    protected RepositoryData downloadAndRepackage() {
        log.info("downloading and repackaging maven repository");
        File sourceTopLevelDirectory = download();
        return repackage(sourceTopLevelDirectory);
    }

    private RepositoryData repackage(File sourceTopLevelDirectory) {
        File targetTopLevelDirectory = new File(workDir, getTargetTopLevelDirectoryName());

        Path targetZipPath = getTargetZipPath();
        targetTopLevelDirectory.mkdirs();
        repackage(sourceTopLevelDirectory, targetTopLevelDirectory);

        addAdditionalArtifacts();

        ParentPomDownloader.addParentPoms(targetRepoContentsDir.toPath());

        RepositoryUtils.removeCommunityArtifacts(targetRepoContentsDir);
        RepositoryUtils.removeIrrelevantFiles(targetRepoContentsDir);

        RepositoryUtils.addCheckSums(targetRepoContentsDir);

        zip(targetTopLevelDirectory, targetZipPath);

        return result(targetTopLevelDirectory, targetZipPath);
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

        generationData.getExternalAdditionalArtifacts().stream().map(GAV::fromColonSeparatedGAPV)
                .forEach(this::downloadExternalArtifact);
    }

    private void downloadExternalArtifact(GAV gav) {
        ExternalArtifactDownloader.downloadExternalArtifact(gav, targetRepoContentsDir.toPath());
    }

    private void downloadArtifact(ArtifactWrapper artifact) {
        Path versionPath = targetRepoContentsDir.toPath().resolve(artifact.toGAV().toVersionPath());
        versionPath.toFile().mkdirs();
        artifact.downloadToDirectory(versionPath);
    }

    public RepositoryData generate() {
        log.info("generating maven repository");
        PncBuild build = getBuild(generationData.getSourceBuild());
        RepoBuilder repoBuilder = new RepoBuilder(config, configurationDirectory, removeGeneratedM2Dups);
        File bomDirectory = FileUtils.mkTempDir("repo-from-bom-generation");
        File bomFile = new File(bomDirectory, "bom.pom");
        build.downloadArtifact(generationData.getSourceArtifact(), bomFile);
        File topLevelDir = repoBuilder.build(bomFile);
        return repackage(new File(topLevelDir, "maven-repository"));
    }

    protected Path getTargetZipPath() {
        return Paths.get(releasePath + deliverables.getRepositoryZipName());
    }

    protected String getTargetTopLevelDirectoryName() {
        return config.getTopLevelDirectoryPrefix() + "maven-repository";
    }

    private RepositoryData result(File targetTopLevelDirectory, Path targetZipPath) {
        RepositoryData result = new RepositoryData();
        File contentsDirectory = new File(targetTopLevelDirectory, "maven-repository");
        result.setFiles(RepoDescriptor.listFiles(contentsDirectory));
        result.setGavs(RepoDescriptor.listGavs(contentsDirectory));
        result.setRepositoryPath(targetZipPath);
        return result;
    }

    protected void repackage(File contentsDirectory, File targetTopLevelDirectory) {
        targetRepoContentsDir = new File(targetTopLevelDirectory, RepoDescriptor.MAVEN_REPOSITORY);
        FileUtils.copy(contentsDirectory, targetRepoContentsDir);
        addExtraFiles(targetTopLevelDirectory);
    }

    private void addExtraFiles(File m2Repo) {
        log.debug("Adding repository documents");
        Properties properties = new Properties();
        properties.put("PRODUCT_NAME", config.getProduct().getName());

        ResourceUtils.copyResourceWithFiltering("/repository-example-settings.xml", "example-settings.xml", m2Repo, properties,
                configurationDirectory);
        ResourceUtils.copyResourceWithFiltering("/repository-README.md", "README.md", m2Repo, properties,
                configurationDirectory);
    }
}
