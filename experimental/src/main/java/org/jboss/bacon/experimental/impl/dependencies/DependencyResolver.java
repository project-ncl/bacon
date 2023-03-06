package org.jboss.bacon.experimental.impl.dependencies;

import io.quarkus.bom.decomposer.ReleaseId;
import io.quarkus.bom.decomposer.ReleaseOrigin;
import io.quarkus.bom.decomposer.ReleaseVersion;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.domino.ProjectDependencyConfig;
import io.quarkus.domino.ProjectDependencyResolver;
import io.quarkus.domino.ReleaseRepo;
import io.quarkus.maven.dependency.ArtifactCoords;
import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.experimental.impl.config.DependencyResolutionConfig;
import org.jboss.da.model.rest.GAV;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class DependencyResolver {

    private final ProjectDependencyConfig.Mutable dominoConfig;
    private final DependencyResolutionConfig config;

    public DependencyResolver(DependencyResolutionConfig dependencyResolutionConfig) {
        this.config = dependencyResolutionConfig;
        dominoConfig = ProjectDependencyConfig.builder();

        config.getExcludeArtifacts().stream().map(GACTVParser::parse).forEach(dominoConfig::addExcludePattern);
        config.getIncludeArtifacts().stream().map(GACTVParser::parse).forEach(dominoConfig::addIncludePattern);
        Set<ArtifactCoords> artifacts = config.getAnalyzeArtifacts()
                .stream()
                .map(ArtifactCoords::fromString)
                .collect(Collectors.toSet());

        if (config.getAnalyzeBOM() != null) {
            dominoConfig.setProjectBom(ArtifactCoords.fromString(config.getAnalyzeBOM()));
        }

        dominoConfig.setIncludeNonManaged(true)
                .setExcludeBomImports(false) // TODO
                .setExcludeParentPoms(false) // TODO
                .setIncludeArtifacts(artifacts)
                .setLevel(-1)
                .setIncludeOptionalDeps(config.isIncludeOptionalDependencies())
                .setWarnOnResolutionErrors(true)
                .setWarnOnMissingScm(true)
                .setProjectArtifacts(Set.of()) // TODO
                .setValidateCodeRepoTags(false) // TODO
                .setIncludeAlreadyBuilt(true); // TODO
        // Remove System.out print that is caused because of listeners defined in BootstramMavenContext
        System.setProperty("quarkus-internal.maven-cmd-line-args", "-ntp");
    }

    public DependencyResult resolve(Path projectDir) {
        dominoConfig.setProjectDir(projectDir);
        ProjectDependencyResolver resolver = ProjectDependencyResolver.builder()
                .setArtifactResolver(getArtifactResolver(projectDir))
                .setMessageWriter(new Slf4jMessageWriter())
                .setDependencyConfig(dominoConfig)
                .build();
        return parseReleaseRepos(resolver.getReleaseRepos());
    }

    public DependencyResult resolve() {
        ProjectDependencyResolver resolver = ProjectDependencyResolver.builder()
                .setDependencyConfig(dominoConfig)
                .setMessageWriter(new Slf4jMessageWriter())
                .build();
        return parseReleaseRepos(resolver.getReleaseRepos());
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

    private DependencyResult parseReleaseRepos(Collection<ReleaseRepo> releaseRepos) {
        Map<ReleaseRepo, Project> mapping = new HashMap<>();
        Set<Project> rootProjects = new HashSet<>();
        for (ReleaseRepo repo : releaseRepos) {
            Project project = new Project();

            Set<GAV> gavs = repo.getArtifacts()
                    .keySet()
                    .stream()
                    .map(a -> new GAV(a.getGroupId(), a.getArtifactId(), a.getVersion()))
                    .collect(Collectors.toSet());
            project.setGavs(gavs);
            project.setSourceCodeURL(getSourceCodeURL(repo.id()));
            project.setSourceCodeRevision(getSourceCodeRevision(repo.id()));
            mapping.put(repo, project);
            if (repo.isRoot()) {
                rootProjects.add(project);
            }
        }
        for (ReleaseRepo repo : releaseRepos) {
            Project project = mapping.get(repo);
            project.setDependencies(repo.getDependencies().stream().map(mapping::get).collect(Collectors.toSet()));
        }
        detectCircularRepoDeps(rootProjects);

        DependencyResult result = new DependencyResult();
        result.setTopLevelProjects(rootProjects);
        result.setCount(releaseRepos.size());
        return result;
    }

    // TODO copied from domino, replace when there is an API that provides this info
    private void detectCircularRepoDeps(Set<Project> rootProjects) {
        for (Project r : rootProjects) {
            final List<Project> chain = new ArrayList<>();
            detectCircularRepoDeps(r, chain);
        }
    }

    private boolean detectCircularRepoDeps(Project r, List<Project> chain) {
        final int i = chain.indexOf(r);
        if (i >= 0) {
            final List<Project> loop = new ArrayList<>(chain.size() - i + 1);
            for (int j = i; j < chain.size(); ++j) {
                loop.add(chain.get(j));
            }
            loop.add(r);
            String loopS = loop.stream().map(p -> p.getFirstGAV().toString()).collect(Collectors.joining(" -> "));
            log.error(
                    "Detected circular dependency. This may be caused by incorrect SCM information, consider updating the recipe repository. We are cutting the dependency loop now, but this will lead to broken build config.");
            log.error("Detected loop: " + loopS);
            return true;
        }
        if (chain.size() > r.getDepth()) {
            r.setDepth(chain.size());
        }
        chain.add(r);
        Iterator<Project> it = r.getDependencies().iterator();
        while (it.hasNext()) {
            Project d = it.next();
            if (detectCircularRepoDeps(d, chain)) {
                it.remove();
                r.setCutDepenendecy(true);
            }
        }
        chain.remove(chain.size() - 1);
        return false;
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
}
