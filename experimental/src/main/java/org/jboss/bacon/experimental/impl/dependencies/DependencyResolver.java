package org.jboss.bacon.experimental.impl.dependencies;

import io.quarkus.bom.decomposer.ReleaseId;
import io.quarkus.bom.decomposer.ReleaseOrigin;
import io.quarkus.bom.decomposer.ReleaseVersion;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.domino.CircularReleaseDependency;
import io.quarkus.domino.ProjectDependencyConfig;
import io.quarkus.domino.ProjectDependencyConfigMapper;
import io.quarkus.domino.ProjectDependencyResolver;
import io.quarkus.domino.ReleaseCollection;
import io.quarkus.domino.ReleaseRepo;
import io.quarkus.maven.dependency.ArtifactCoords;
import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.da.DaHelper;
import org.jboss.bacon.da.rest.endpoint.LookupApi;
import org.jboss.bacon.experimental.impl.config.DependencyResolutionConfig;
import org.jboss.da.lookup.model.MavenLookupRequest;
import org.jboss.da.lookup.model.MavenLookupResult;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.AutobuildConfig;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.common.version.SuffixedVersion;
import org.jboss.pnc.common.version.VersionParser;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class DependencyResolver {

    private final DependencyResolutionConfig config;
    private final VersionParser versionParser = new VersionParser("redhat");
    private final LookupApi lookupApi;

    public DependencyResolver(DependencyResolutionConfig dependencyResolutionConfig) {
        this.config = dependencyResolutionConfig;
        // Remove System.out print that is caused because of listeners defined in BootstramMavenContext
        System.setProperty("quarkus-internal.maven-cmd-line-args", "-ntp");

        lookupApi = DaHelper.createLookupApi();
    }

    private void setupConfig(ProjectDependencyConfig.Mutable dominoConfig) {
        config.getExcludeArtifacts().stream().map(GACTVParser::parse).forEach(dominoConfig::addExcludePattern);
        AutobuildConfig autobuildConfig = Objects.requireNonNull(
                Config.instance().getActiveProfile().getAutobuild(),
                "Missing the 'autobuild' option in your config profile.");
        autobuildConfig.validate();
        DependencyExcluder dependencyExcluder = new DependencyExcluder(autobuildConfig);
        final String[] excludedGavs = DependencyExcluder.getExcludedGavs(dependencyExcluder.fetchExclusionFile());
        log.info("There are {} dependencies to be excluded", excludedGavs.length);
        Arrays.stream(excludedGavs).map(GACTVParser::parse).forEach(dominoConfig::addExcludePattern);
        config.getIncludeArtifacts().stream().map(GACTVParser::parse).forEach(dominoConfig::addIncludePattern);
        Set<ArtifactCoords> artifacts = config.getAnalyzeArtifacts()
                .stream()
                .map(ArtifactCoords::fromString)
                .collect(Collectors.toSet());

        if (config.getAnalyzeBOM() != null) {
            dominoConfig.setProjectBom(ArtifactCoords.fromString(config.getAnalyzeBOM()));
        }

        dominoConfig.setExcludeBomImports(false) // TODO
                .setExcludeParentPoms(false) // TODO
                .setLevel(-1)
                .setIncludeOptionalDeps(config.isIncludeOptionalDependencies())
                .setWarnOnResolutionErrors(true)
                .setWarnOnMissingScm(true)
                .setRecipeRepos(config.getRecipeRepos())
                .setProjectArtifacts(artifacts)
                .setValidateCodeRepoTags(false) // TODO
                .setIncludeAlreadyBuilt(true); // TODO
    }

    public DependencyResult resolve(Path projectDir, Path dominoConfigFile) {
        ProjectDependencyConfig.Mutable dominoConfig;
        if (dominoConfigFile == null) {
            dominoConfig = ProjectDependencyConfig.builder();
        } else {
            try {
                dominoConfig = ProjectDependencyConfig.mutableFromFile(dominoConfigFile);
            } catch (IOException e) {
                throw new FatalException("Failed to load domino config file " + dominoConfigFile, e);
            }
        }
        ProjectDependencyResolver.Builder resolverBuilder = ProjectDependencyResolver.builder();
        if (projectDir != null) {
            dominoConfig.setProjectDir(projectDir);
            resolverBuilder.setArtifactResolver(getArtifactResolver(projectDir));
        }
        setupConfig(dominoConfig);
        ProjectDependencyConfig conf = dominoConfig.build();
        logDominoConfig(conf);
        ProjectDependencyResolver resolver = resolverBuilder.setMessageWriter(new Slf4jMessageWriter())
                .setDependencyConfig(conf)
                .build();
        PrintStream origOut = System.out;
        System.setOut(new PrintStream(new LogOutputStream()));
        ReleaseCollection releaseCollection = resolver.getReleaseCollection();
        System.setOut(origOut);
        return parseReleaseCollection(releaseCollection);
    }

    private void logDominoConfig(ProjectDependencyConfig conf) {
        if (log.isDebugEnabled()) {
            try (StringWriter writer = new StringWriter()) {
                ProjectDependencyConfigMapper.serialize(conf, writer);
                log.debug("Using domino config:\n" + writer.toString());
            } catch (IOException e) {
                log.info("Failed to serialize domino config.", e);
            }
        }
    }

    protected MavenArtifactResolver getArtifactResolver(Path projectDir) {
        try {
            return MavenArtifactResolver.builder()
                    .setCurrentProject(projectDir.toAbsolutePath().toString())
                    .setEffectiveModelBuilder(true)
                    .setPreferPomsFromWorkspace(true)
                    .build();
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize Maven artifact resolver", e);
        }
    }

    private DependencyResult parseReleaseCollection(ReleaseCollection releaseCollection) {
        var depsToCut = processCircularDependencies(releaseCollection.getCircularDependencies());

        Map<ReleaseRepo, Project> mapping = new HashMap<>();
        Set<Project> rootProjects = new HashSet<>();
        for (ReleaseRepo repo : releaseCollection) {
            Project project = mapToProject(repo, depsToCut);
            mapping.put(repo, project);
            if (repo.isRoot() && filterProductized(project)) {
                rootProjects.add(project);
            }
        }
        setupDependencies(mapping, depsToCut);
        setDepth(rootProjects);

        DependencyResult result = new DependencyResult();
        result.setTopLevelProjects(rootProjects);
        return result;
    }

    private void setupDependencies(Map<ReleaseRepo, Project> mapping, Map<ReleaseId, Set<ReleaseId>> depsToCut) {
        for (var entry : mapping.entrySet()) {
            ReleaseRepo repo = entry.getKey();
            Project project = entry.getValue();
            Set<ReleaseId> toCut = depsToCut.getOrDefault(repo.id(), Collections.emptySet());
            project.setDependencies(
                    repo.getDependencies()
                            .stream()
                            .filter(d -> !toCut.contains(d.id()))
                            .map(mapping::get)
                            .filter(this::filterProductized)
                            .collect(Collectors.toSet()));
        }
    }

    /**
     * Returns false if the project should be excluded.
     */
    private boolean filterProductized(Project releaseRepo) {
        boolean excludeAlreadyBuilt = !config.isRebuildNonAutoBuilds();
        boolean excludeRedhatSuffix = config.isExcludeProductizedArtifacts() || excludeAlreadyBuilt;
        if (excludeRedhatSuffix) {
            SuffixedVersion version = versionParser.parse(releaseRepo.getFirstGAV().getVersion());
            if (version.isSuffixed()) {
                return false;
            }
        }
        if (excludeAlreadyBuilt) {
            MavenLookupRequest request = MavenLookupRequest.builder()
                    .mode(DaHelper.getMode(false, false, null))
                    .brewPullActive(false)
                    .artifacts(releaseRepo.getGavs())
                    .build();
            Set<MavenLookupResult> mavenLookupResults = lookupApi.lookupMaven(request);
            Set<String> versionsFound = mavenLookupResults.stream()
                    .map(MavenLookupResult::getBestMatchVersion)
                    .collect(Collectors.toSet());
            boolean everythingBuilt = !versionsFound.contains(null);
            int minVersionCount = everythingBuilt ? 1 : 2; // if null is present, null + single version = 2 items in set
            boolean everythingInTheSameVersion = versionsFound.size() == minVersionCount;
            boolean anythingBuilt = everythingBuilt || versionsFound.size() > 1;
            if (everythingBuilt && everythingInTheSameVersion) {
                return false;
            }
            if (anythingBuilt) {
                String message = "";
                if (!everythingBuilt) {
                    message = " not all artifacts are built";
                }
                if (!everythingInTheSameVersion) {
                    if (!message.isEmpty()) {
                        message += " and";
                    }
                    message += " not all artifacts are built in the same version";
                }
                String debugOff = "";
                if (log.isDebugEnabled()) {
                    String artifacts = mavenLookupResults.stream()
                            .map(r -> r.getGav() + " -> " + r.getBestMatchVersion())
                            .collect(Collectors.joining("\n"));
                    log.debug("Artifacts and their found versions:\n" + artifacts);
                } else {
                    debugOff = " (Enable debug output with -v to see what built artifact versions were found.)";
                }
                log.warn(
                        "Excluding project " + releaseRepo.getFirstGAV() + " because some artifacts are build, however"
                                + message + "." + debugOff);
                return false;
            }
        }
        return true;
    }

    private Map<ReleaseId, Set<ReleaseId>> processCircularDependencies(
            Collection<CircularReleaseDependency> circularDependencies) {
        Map<ReleaseId, Set<ReleaseId>> depsToCut = new HashMap<>();
        if (!circularDependencies.isEmpty()) {
            log.error(
                    "Detected circular dependencies. This may be caused by incorrect SCM information, "
                            + "consider updating the recipe repository. We are cutting the dependency loop now, "
                            + "but this will lead to broken build config.");
            for (CircularReleaseDependency circularDependency : circularDependencies) {
                log.error("Detected loop: " + circularDependency);
                List<ReleaseId> releaseDependencyChain = circularDependency.getReleaseDependencyChain();
                ReleaseId child = releaseDependencyChain.get(releaseDependencyChain.size() - 1);
                ReleaseId parent = releaseDependencyChain.get(releaseDependencyChain.size() - 2);
                Set<ReleaseId> releaseIds = depsToCut.computeIfAbsent(parent, k -> new HashSet<>());
                releaseIds.add(child);
            }
        }
        return depsToCut;
    }

    private Project mapToProject(ReleaseRepo repo, Map<ReleaseId, Set<ReleaseId>> depsToCut) {
        Project project = new Project();
        Set<GAV> gavs = repo.getArtifacts()
                .keySet()
                .stream()
                .map(a -> new GAV(a.getGroupId(), a.getArtifactId(), a.getVersion()))
                .collect(Collectors.toSet());
        project.setGavs(gavs);
        project.setSourceCodeURL(getSourceCodeURL(repo.id()));
        project.setSourceCodeRevision(getSourceCodeRevision(repo.id()));
        if (depsToCut.containsKey(repo.id())) {
            GAV firstGAV = project.getFirstGAV();
            log.warn("Project " + firstGAV + " has cut some dependency(ies).");
            project.setCutDependency(true);
        }
        return project;
    }

    private void setDepth(Set<Project> rootProjects) {
        for (Project project : rootProjects) {
            setDepth(project, 0);
        }
    }

    private void setDepth(Project project, int depth) {
        if (depth > project.getDepth()) {
            project.setDepth(depth);
        }
        for (Project dependency : project.getDependencies()) {
            setDepth(dependency, depth + 1);
        }
    }

    private String getSourceCodeURL(ReleaseId releaseId) {
        ReleaseOrigin origin = releaseId.origin(); // TODO: this API will probably change
        if (origin.isUrl()) {
            return origin.toString();
        }
        return null;
    }

    private String getSourceCodeRevision(ReleaseId releaseId) {
        ReleaseVersion version = releaseId.version(); // TODO: this API will probably change
        if (version.isTag()) {
            return version.asString();
        }
        return null;
    }

    private static class Slf4jMessageWriter implements MessageWriter {
        private static final org.slf4j.Logger messageWriterLog = org.slf4j.LoggerFactory.getLogger("domino");

        @Override
        public void info(String s) {
            messageWriterLog.info(s);
        }

        @Override
        public void error(String s) {
            messageWriterLog.error(s);
        }

        @Override
        public boolean isDebugEnabled() {
            return messageWriterLog.isDebugEnabled();
        }

        @Override
        public void debug(String s) {
            messageWriterLog.debug(s);
        }

        @Override
        public void warn(String s) {
            messageWriterLog.warn(s);
        }
    }

    private static class LogOutputStream extends OutputStream {
        protected boolean closed = false;

        private StringBuffer stringBuffer = new StringBuffer();

        @Override
        public void close() {
            flush();
            closed = true;
        }

        @Override
        public void flush() {
            if (stringBuffer.length() == 0) {
                return;
            }
            log.info(stringBuffer.toString());
            stringBuffer.setLength(0);
        }

        @Override
        public void write(final int b) throws IOException {
            if (closed) {
                throw new IOException("Stream is closed.");
            }
            if (b == 0) {
                return;
            }
            if (b == '\n' || b == '\r') {
                flush();
                return;
            }
            stringBuffer.append((char) b);
        }

    }
}
