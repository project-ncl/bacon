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

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GACTV;
import lombok.Getter;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.artifact.JavaScopes;
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
import org.jboss.pnc.bacon.pig.impl.utils.GavSet;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/23/17
 */
public class RepoManager extends DeliverableManager<RepoGenerationData, RepositoryData> implements Closeable {
    private static final String POM = "pom";
    private static final String JAR = "jar";
    private static final Logger log = LoggerFactory.getLogger(RepoManager.class);
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[, \t\n\r\f]+");

    private final BuildInfoCollector buildInfoCollector;
    @Getter
    private final RepoGenerationData generationData;
    private File targetRepoContentsDir;
    private final boolean removeGeneratedM2Dups;
    private final Path configurationDirectory;
    private final boolean strictLicenseCheck;
    private final boolean strictDownloadSource;
    private final boolean isTestMode;

    public RepoManager(
            PigConfiguration pigConfiguration,
            String releasePath,
            Deliverables deliverables,
            Map<String, PncBuild> builds,
            Path configurationDirectory,
            boolean removeGeneratedM2Dups,
            boolean strictLicenseCheck,
            boolean strictDownloadSource) {
        super(pigConfiguration, releasePath, deliverables, builds);
        generationData = pigConfiguration.getFlow().getRepositoryGeneration();
        this.removeGeneratedM2Dups = removeGeneratedM2Dups;
        this.configurationDirectory = configurationDirectory;
        this.strictLicenseCheck = strictLicenseCheck;
        this.strictDownloadSource = strictDownloadSource;
        buildInfoCollector = new BuildInfoCollector();
        isTestMode = false;
    }

    public RepoManager(
            PigConfiguration pigConfiguration,
            String releasePath,
            Deliverables deliverables,
            Map<String, PncBuild> builds,
            Path configurationDirectory,
            boolean removeGeneratedM2Dups,
            boolean strictLicenseCheck,
            boolean strictDownloadSource,
            BuildInfoCollector buildInfoCollector,
            Boolean isTestMode) {
        super(pigConfiguration, releasePath, deliverables, builds);
        generationData = pigConfiguration.getFlow().getRepositoryGeneration();
        this.removeGeneratedM2Dups = removeGeneratedM2Dups;
        this.configurationDirectory = configurationDirectory;
        this.strictLicenseCheck = strictLicenseCheck;
        this.strictDownloadSource = strictDownloadSource;
        this.buildInfoCollector = buildInfoCollector;
        this.isTestMode = isTestMode;
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
            case RESOLVE_ONLY:
                return resolveAndRepackage();
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
        // ⚠ this only selects maven-style identifiers where the version has redhat in it
        // <group-id>:<artifact-id>:<packaging>:<version>
        buildInfoCollector.addDependencies(build, "identifier=like=%:%:%:%redhat%");
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

        if (isFilterActive()) {
            artifactsToPack.removeIf(artifact -> !isArtifactInFilter(artifact.getGapv()));
        }

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
        gavsToPack.forEach(
                a -> ExternalArtifactDownloader.downloadExternalArtifact(a, sourceDir.toPath(), !strictDownloadSource));
    }

    @Deprecated
    private RepositoryData packAllBuiltAndDependencies() {
        log.warn("Repo generation strategy 'PACK_ALL' is deprecated please use BUILD_CONFIGS");
        PncBuild build = getBuild(generationData.getSourceBuild());
        List<ArtifactWrapper> artifactsToPack = new ArrayList<>();
        getRedhatArtifacts(artifactsToPack, build);
        File sourceDir = createMavenGenerationDir();
        filterAndDownload(artifactsToPack, sourceDir);
        return repackage(sourceDir);
    }

    private RepositoryData buildGroup() {
        log.info("Generating maven repo for build group [{}]", pigConfiguration.getGroup());
        if (builds.isEmpty()) {
            throw new RuntimeException("There are no builds captured for the build group. Aborting!");
        }
        List<ArtifactWrapper> artifactsToPack = new ArrayList<>();
        builds.values()
                .stream()
                .filter(b -> !generationData.getExcludeSourceBuilds().contains(b.getName()))
                .forEach(b -> getRedhatArtifacts(artifactsToPack, b));
        File sourceDir = createMavenGenerationDir();
        filterAndDownload(artifactsToPack, sourceDir);
        return repackage(sourceDir);
    }

    private RepositoryData buildConfigs() {
        log.info("Generating maven repo for named build configs");
        List<ArtifactWrapper> artifactsToPack = new ArrayList<>();

        if (generationData.getSourceBuilds().isEmpty()) {
            throw new RuntimeException("There are no build configs defined for maven repository generation. Aborting!");
        }
        for (String buildConfigName : generationData.getSourceBuilds()) {
            PncBuild build = getBuild(buildConfigName);
            getRedhatArtifacts(artifactsToPack, build);
        }
        File sourceDir = createMavenGenerationDir();

        filterAndDownload(artifactsToPack, sourceDir);
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
        return list.stream().noneMatch(a -> a.equals(gav) && POM.equals(a.getPackaging()));
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

    private boolean isFilterActive() {
        List<String> filterArtifacts = pigConfiguration.getFlow().getRepositoryGeneration().getFilterArtifacts();
        return !filterArtifacts.isEmpty();
    }

    private boolean isArtifactInFilter(String artifact) {
        List<String> filterArtifacts = pigConfiguration.getFlow().getRepositoryGeneration().getFilterArtifacts();
        return filterArtifacts.stream().anyMatch(filter -> Pattern.matches(filter, artifact));
    }

    @Override
    protected RepositoryData downloadAndRepackage() {
        log.info("Downloading and repackaging maven repository");
        File sourceTopLevelDirectory = download();
        return repackage(sourceTopLevelDirectory);
    }

    private RepositoryData repackage(File sourceTopLevelDirectory) {
        log.info("Repackaging the repository");
        File targetTopLevelDirectory = new File(workDir, getTargetTopLevelDirectoryName());

        Path targetZipPath = getTargetZipPath();
        targetTopLevelDirectory.mkdirs();
        repackage(sourceTopLevelDirectory, targetTopLevelDirectory);

        addAdditionalArtifacts();

        ParentPomDownloader.addParentPoms(targetRepoContentsDir.toPath());

        if (!isTestMode) {
            RepositoryUtils.removeCommunityArtifacts(targetRepoContentsDir);
            RepositoryUtils.removeIrrelevantFiles(targetRepoContentsDir);

            // Delete excluded artifacts in the maven-repository. Needed for resolve only generation where filtering
            // can't be done before download
            List<String> excludeArtifacts = pigConfiguration.getFlow().getRepositoryGeneration().getExcludeArtifacts();
            RepositoryUtils.removeExcludedArtifacts(targetRepoContentsDir, excludeArtifacts);
        }
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
        return generate(generationData);
    }

    private RepositoryData generate(RepoGenerationData generationData) {
        log.info("Generating maven repository");
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

    public RepositoryData resolveAndRepackage() {
        try {
            log.info("Generating maven repository");
            final File sourceDir = createMavenGenerationDir();
            final MavenArtifactResolver mvnResolver = MavenArtifactResolver.builder()
                    .setUserSettings(getIndyMavenSettings())
                    .setLocalRepository(sourceDir.getAbsolutePath())
                    .build();
            final Comparator<Artifact> artifactComparator = Comparator.comparing(Artifact::getGroupId)
                    .thenComparing(Artifact::getArtifactId)
                    .thenComparing(Artifact::getExtension)
                    .thenComparing(Artifact::getClassifier)
                    .thenComparing(Artifact::getVersion);
            if (generationData.getSteps().isEmpty()) {
                resolveAndRepackage(generationData, sourceDir, mvnResolver, artifactComparator);
            } else {
                for (RepoGenerationData step : generationData.getSteps()) {
                    final RepoGenerationData mergedData = RepoGenerationData.merge(generationData, step);
                    resolveAndRepackage(mergedData, sourceDir, mvnResolver, artifactComparator);
                }
            }
            return repackage(sourceDir);
        } catch (Exception bme) {
            throw new RuntimeException(
                    "Failed to generate Maven repository using " + generationData.getStrategy(),
                    bme);
        }
    }

    private void resolveAndRepackage(
            RepoGenerationData generationData,
            File sourceDir,
            MavenArtifactResolver mvnResolver,
            Comparator<Artifact> artifactComparator) throws BootstrapMavenException {
        final Map<String, Path> bannedDirs = parseBannedArtifactsParameter(generationData, sourceDir);

        /* We use TreeSet for reproducible ordering */
        final Set<Artifact> extensionRtArtifactList = new TreeSet<>(artifactComparator);

        final Map<String, String> params = generationData.getParameters();
        final String extensionsListUrl = params.get("extensionsListUrl");
        if (extensionsListUrl != null) {
            // extensionsListUrl is optional
            // Must point to a text file which contains list of format "groupId:artifactId:version:type:classifier"
            extensionRtArtifactList.addAll(parseExtensionsArtifactList(extensionsListUrl));
        }

        addResolveArtifacts(params.get("resolveArtifacts"), extensionRtArtifactList::add);

        final List<GAV> bomGAVs = getBomGavFromConfig(generationData);
        final Set<Dependency> bomConstraints = new LinkedHashSet<>();
        Artifact bom = null;
        for (GAV gav : bomGAVs) {
            bom = new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), null, POM, gav.getVersion());
            bomConstraints.addAll(mvnResolver.resolveDescriptor(bom).getManagedDependencies());
        }
        if (bomConstraints.isEmpty()) {
            throw new IllegalStateException("Failed to get constraints from BOMs " + bomGAVs);
        }

        /* Get the extensionsListUrl from the BOM based on resolveIncludes and resolveExcludes params */
        final String rawIncludes = params.get("resolveIncludes");
        final GavSet resolveSet;
        if (rawIncludes != null) {
            resolveSet = GavSet.builder().includes(rawIncludes).excludes(params.get("resolveExcludes")).build();
            bomConstraints.stream()
                    .map(Dependency::getArtifact)
                    .filter(
                            a -> resolveSet.contains(
                                    a.getGroupId(),
                                    a.getArtifactId(),
                                    a.getExtension(),
                                    a.getClassifier(),
                                    a.getVersion()))
                    .forEach(extensionRtArtifactList::add);
        } else {
            resolveSet = GavSet.includeAll();
        }

        final List<Exclusion> transitiveExclusions = parseExclusions(params.get("excludeTransitive"));

        ensureDefaultJarsIncluded(extensionRtArtifactList);

        if (log.isDebugEnabled()) {
            log.debug(
                    "About to resolve artifacts \n" + extensionRtArtifactList.stream()
                            .map(Artifact::toString)
                            .collect(Collectors.joining("\n")));
            if (!transitiveExclusions.isEmpty()) {
                log.debug("Applying exclusions to transitive dependencies:");
                transitiveExclusions.forEach(e -> log.debug("  " + e));
            }
        }

        Map<Artifact, String> redhatVersionExtensionArtifactMap = collectRedhatVersions(extensionRtArtifactList);

        final List<Dependency> managedDeps = new ArrayList<>(bomConstraints);
        final StringBuilder bannedReport = new StringBuilder();
        final Set<ArtifactCoords> collectedArtifacts = new HashSet<>();
        for (Artifact extensionRtArtifact : extensionRtArtifactList) {
            // this will resolve all the artifacts and their dependencies and as a consequence populate the local
            // Maven repo
            // specified in the user settings.xml
            String aptVersion = redhatVersionExtensionArtifactMap
                    .getOrDefault(extensionRtArtifact, extensionRtArtifact.getVersion());
            if (extensionRtArtifact.getArtifactId().contains("plugin")) {
                Artifact pluginArtifact = new DefaultArtifact(
                        extensionRtArtifact.getGroupId(),
                        extensionRtArtifact.getArtifactId(),
                        extensionRtArtifact.getExtension(),
                        aptVersion);
                log.debug("Resolving dependencies of plugin {}", pluginArtifact);
                mvnResolver.resolvePluginDependencies(pluginArtifact);
            } else {
                if (extensionRtArtifact.getArtifactId().contains("bom")
                        && !extensionRtArtifact.getExtension().equals("properties")
                        && !extensionRtArtifact.getExtension().equals("json")
                        || extensionRtArtifact.getExtension().equals(POM)) {
                    DefaultArtifact bomPomArtifact = new DefaultArtifact(
                            extensionRtArtifact.getGroupId(),
                            extensionRtArtifact.getArtifactId(),
                            POM,
                            aptVersion);
                    log.debug("Resolving POM {}", extensionRtArtifact);
                    mvnResolver.resolve(bomPomArtifact);
                } else {
                    DefaultArtifact redHatArtifact = new DefaultArtifact(
                            extensionRtArtifact.getGroupId(),
                            extensionRtArtifact.getArtifactId(),
                            extensionRtArtifact.getClassifier(),
                            extensionRtArtifact.getExtension(),
                            aptVersion);
                    log.debug("Resolving dependencies of {}", redHatArtifact);
                    mvnResolver.resolve(redHatArtifact);

                    /*
                     * We resolve the BOM (assuming it has no own dependencies) and add the artifact we actually want to
                     * resolve as its sole dependency. In this way, optional dependencies do not get resolved, which we
                     * want. If we resolved the artifact directly, it would also resolve the direct optional
                     * dependencies.
                     */
                    final DependencyNode root = mvnResolver.resolveManagedDependencies(
                            bom,
                            List.of(new Dependency(redHatArtifact, JavaScopes.RUNTIME, false, transitiveExclusions)),
                            managedDeps, // version constraints from the BOM
                            List.of(), // extra maven repos, ignore this
                            JavaScopes.TEST,
                            JavaScopes.PROVIDED // dependency scopes that should be ignored
                    ).getRoot();
                    root.getChildren().forEach(n -> collectArtifacts(n, collectedArtifacts));
                }
            }
            bannedDirs.entrySet()
                    .stream()
                    .filter(en -> Files.exists(en.getValue()) && hasProdVersionSubdir(en.getValue()))
                    .peek(en -> bannedReport.append("\n " + en.getKey() + " pulled by " + extensionRtArtifact))
                    .forEach(en -> {
                        try {
                            org.apache.commons.io.FileUtils.deleteDirectory(en.getValue().toFile());
                        } catch (IOException e) {
                            throw new RuntimeException("Could not delete " + en.getValue(), e);
                        }
                    });
        }
        if (bannedReport.length() > 0) {
            throw new IllegalStateException("Banned artifacts found: " + bannedReport.toString());
        }

        addMissingJarsForPoms(mvnResolver);
    }

    private static void collectArtifacts(DependencyNode node, Set<ArtifactCoords> collected) {
        final Artifact a = node.getArtifact();
        collected
                .add(new GACTV(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension(), a.getVersion()));
        for (DependencyNode c : node.getChildren()) {
            collectArtifacts(c, collected);
        }
    }

    /**
     * This method attempts to add JAR artifacts that are missing from the generated Maven repository for POM artifacts
     * that have jar packaging. This could happen when POM artifacts where resolved following the collect dependency
     * request.
     *
     * @param mvnResolver Maven artifact resolver
     */
    private void addMissingJarsForPoms(MavenArtifactResolver mvnResolver) {
        final Path mavenRepoDir = mvnResolver.getSession()
                .getLocalRepositoryManager()
                .getRepository()
                .getBasedir()
                .toPath();
        try {
            Files.walkFileTree(mavenRepoDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final String fileName = file.getFileName().toString();
                    if (!fileName.endsWith(".pom")) {
                        return FileVisitResult.CONTINUE;
                    }
                    final String jarName = fileName.substring(0, fileName.length() - POM.length()) + JAR;
                    if (!Files.exists(file.getParent().resolve(jarName))) {
                        final Model model = ModelUtils.readModel(file);
                        if (getPackaging(model).equals(JAR)) {
                            final Artifact a = new DefaultArtifact(
                                    ModelUtils.getGroupId(model),
                                    model.getArtifactId(),
                                    JAR,
                                    file.getParent().getFileName().toString());
                            try {
                                mvnResolver.resolve(a);
                            } catch (BootstrapMavenException e) {
                                log.debug("Failed to resolve default JAR " + a);
                            }
                        }
                    }
                    return FileVisitResult.SKIP_SIBLINGS;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to process " + mavenRepoDir, e);
        }
    }

    private static String getPackaging(Model model) {
        if (POM.equals(model.getPackaging())) {
            return POM;
        }
        return model.getPackaging() == null || model.getPackaging().isEmpty() || model.getPackaging().equals(JAR) ? JAR
                : model.getPackaging();
    }

    /**
     * This method makes sure that for every JAR artifact with a classifier, whose POM has jar packaging, the default
     * JAR artifact (with no classifier) is included.
     *
     * @param extensionRtArtifactList a set of artifact coordinates to resolve dependencies for
     */
    private void ensureDefaultJarsIncluded(Set<Artifact> extensionRtArtifactList) {
        final List<Artifact> missingDefaultJars = new ArrayList<>();
        for (Artifact a : extensionRtArtifactList) {
            if (!a.getClassifier().isEmpty() && JAR.equals(a.getExtension())) {
                final Artifact defaultJar = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), JAR, a.getVersion());
                if (!extensionRtArtifactList.contains(defaultJar)) {
                    missingDefaultJars.add(defaultJar);
                }
            }
        }
        extensionRtArtifactList.addAll(missingDefaultJars);
    }

    static void addResolveArtifacts(String resolveArtifacts, Consumer<Artifact> artifactConsumer) {
        if (resolveArtifacts != null) {
            final StringTokenizer st = new StringTokenizer(resolveArtifacts, ", \t\n\r\f");
            while (st.hasMoreTokens()) {
                final Artifact extensionRtArtifact;
                final String coords = st.nextToken();
                final String[] parts = coords.split(":");
                switch (parts.length) {
                    case 3:
                        extensionRtArtifact = new DefaultArtifact(parts[0], parts[1], null, JAR, parts[2]);
                        break;
                    case 4:
                        extensionRtArtifact = new DefaultArtifact(parts[0], parts[1], null, parts[2], parts[3]);
                        break;
                    case 5:
                        extensionRtArtifact = new DefaultArtifact(parts[0], parts[1], parts[3], parts[2], parts[4]);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unparseable artifact coordinates '" + coords + "' in resolveArtifacts parameter.");
                }
                artifactConsumer.accept(extensionRtArtifact);
            }
        }
    }

    static List<Exclusion> parseExclusions(String exclusionsStr) {
        if (exclusionsStr == null || exclusionsStr.isBlank()) {
            return List.of();
        }
        final StringTokenizer st = new StringTokenizer(exclusionsStr, ", \t\n\r\f");
        final List<Exclusion> result = new ArrayList<>(st.countTokens());
        while (st.hasMoreTokens()) {
            final String exclusion = st.nextToken();
            final String[] parts = exclusion.split(":");
            final Exclusion e;
            switch (parts.length) {
                case 2:
                    e = new Exclusion(parts[0], parts[1], "*", "*");
                    break;
                case 3:
                    e = new Exclusion(parts[0], parts[1], parts[2], "*");
                    break;
                case 4:
                    e = new Exclusion(parts[0], parts[1], parts[2], parts[3]);
                    break;
                default:
                    throw new IllegalStateException("Unparseable exclusion '" + exclusion + "'");
            }
            result.add(e);
        }
        return result;
    }

    static boolean hasProdVersionSubdir(Path path) {
        try (Stream<Path> files = Files.list(path)) {
            return files.anyMatch(p -> p.getFileName().toString().contains("redhat-"));
        } catch (IOException e) {
            throw new RuntimeException("Could not list " + path, e);
        }
    }

    private List<GAV> getBomGavFromConfig(RepoGenerationData generationData) {

        final List<GAV> result = new ArrayList<>();
        String sourceArtifact = generationData.getSourceArtifact();
        PncBuild pncBuild = null;
        if (sourceArtifact != null && !sourceArtifact.isEmpty()) {
            pncBuild = getBuild(sourceArtifact, generationData.getSourceBuild());
            final ArtifactWrapper bomArtifact = pncBuild.findArtifactByFileName(sourceArtifact);
            result.add(bomArtifact.toGAV());
        }

        final String rawBomGavs = generationData.getParameters().get("bomGavs");
        if (rawBomGavs != null && !rawBomGavs.isEmpty()) {
            for (String rawGav : SPLIT_PATTERN.split(rawBomGavs)) {
                String[] gav = rawGav.split(":");
                if (gav.length == 3) {
                    result.add(new GAV(gav[0], gav[1], gav[2], POM));
                } else if (gav.length == 2) {
                    if (pncBuild == null) {
                        pncBuild = getBuild(sourceArtifact, generationData.getSourceBuild());
                    }
                    result.add(pncBuild.findArtifact(gav[0] + ":" + gav[1] + ":pom:.*").toGAV());
                }
            }
        }

        return result;
    }

    private PncBuild getBuild(String sourceArtifact, final String sourceBuild) {
        if (sourceBuild == null || sourceBuild.isEmpty()) {
            throw new IllegalStateException(
                    "sourceBuild must be set if sourceArtifact is set for RESOLVE_ONLY strategy; found sourceBuild: ["
                            + sourceBuild + "] and sourceArtifact: [" + sourceArtifact + "]");
        }
        return getBuild(sourceBuild);
    }

    private static Map<String, Path> parseBannedArtifactsParameter(RepoGenerationData generationData, File sourceDir) {
        Map<String, String> params = generationData.getParameters();
        final Map<String, Path> result = new TreeMap<>();
        final String bannedArtifacts = params.get("bannedArtifacts");
        if (bannedArtifacts != null) {
            for (String c : SPLIT_PATTERN.split(bannedArtifacts)) {
                String[] coords = c.split(":");
                result.put(c, sourceDir.toPath().resolve(coords[0].replace('.', '/') + "/" + coords[1]));
            }
        }
        return result;
    }

    public Map<Artifact, String> collectRedhatVersions(Collection<Artifact> extensionArtifacts) {
        /*
         * Take only those versions from the PNC build which are not managed in the BOM. This is important when
         * performing quick local MRRC builds that rely on BOMs built and installed locally whose versions are newer
         * than in any available PNC build.
         */
        final List<Artifact> extensionArtifactsWithoutVersions = extensionArtifacts.stream()
                .filter(artifact -> artifact.getVersion() == null)
                .collect(Collectors.toList());
        if (extensionArtifactsWithoutVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Artifact, String> result = new HashMap<>();
        builds.forEach((key, pncBuild) -> {
            if (pncBuild.getBuiltArtifacts() != null) {
                pncBuild.getBuiltArtifacts().forEach((artifactWrapper -> {
                    extensionArtifactsWithoutVersions.forEach((extensionArtifact -> {
                        GAV gav = artifactWrapper.toGAV();
                        if (extensionArtifact.getGroupId().equals(gav.getGroupId())
                                && extensionArtifact.getArtifactId().equals(gav.getArtifactId())) {
                            result.put(extensionArtifact, gav.getVersion());
                        }
                    }));
                }));
            }
            if (pncBuild.getDependencyArtifacts() != null) {
                pncBuild.getDependencyArtifacts().forEach((artifactWrapper -> {
                    extensionArtifactsWithoutVersions.forEach((extensionArtifact -> {
                        GAV gav = artifactWrapper.toGAV();
                        if (extensionArtifact.getGroupId().equals(gav.getGroupId())
                                && extensionArtifact.getArtifactId().equals(gav.getArtifactId())) {
                            result.put(extensionArtifact, gav.getVersion());
                        }
                    }));
                }));
            }
        });

        return result;
    }

    /**
     * @param urlString url to extensionList file. File must contains the extensions in format of
     *        groupId:artifactId:version:type:classifier classifier and type are optionals
     * @return List of Artifacts
     * @throws Exception
     */
    public List<Artifact> parseExtensionsArtifactList(String urlString) {
        ArrayList<Artifact> list = new ArrayList<>();
        try {

            URL url = new URL(urlString);
            URLConnection connection = (URLConnection) url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String buffer = "";
            while (buffer != null) {
                buffer = br.readLine();
                if (buffer == null) {
                    break;
                }
                buffer = buffer.trim();
                if (buffer.isEmpty() || buffer.startsWith("#")) {
                    continue;
                }
                String[] parts = buffer.split(":");
                if (parts.length < 3 || parts.length > 5) {
                    throw new RuntimeException("Extension text file is not properly formatted");
                }

                final Artifact extensionRtArtifact;
                if (parts.length == 3) {
                    extensionRtArtifact = new DefaultArtifact(parts[0], parts[1], null, JAR, parts[2]);
                } else if (parts.length == 4) {
                    extensionRtArtifact = new DefaultArtifact(parts[0], parts[1], null, parts[3], parts[2]);
                } else {
                    extensionRtArtifact = new DefaultArtifact(parts[0], parts[1], parts[4], parts[3], parts[2]);
                }
                list.add(extensionRtArtifact);
            }
        } catch (MalformedURLException mfe) {
            throw new RuntimeException("url is not proper " + urlString, mfe);
        } catch (IOException e) {
            throw new RuntimeException("Unable to do IO operation. Url: " + urlString, e);
        }
        return list;
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
        log.info("Created repository: {}", targetZipPath);
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

    private static File getIndyMavenSettings() {
        final String settingsXmlPath = Indy.getConfiguredIndySettingsXmlPath(PigContext.get().isTempBuild());
        final File settings = new File(settingsXmlPath);
        if (!settings.exists()) {
            throw new RuntimeException("Failed to locate the Indy Maven settings at " + settings);
        }
        return settings;
    }
}
