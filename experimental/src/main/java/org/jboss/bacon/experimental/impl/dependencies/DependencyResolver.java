package org.jboss.bacon.experimental.impl.dependencies;

import io.quarkus.bom.decomposer.ReleaseId;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.domino.ProjectDependencyConfig;
import io.quarkus.domino.ProjectDependencyResolver;
import io.quarkus.domino.ReleaseRepo;
import io.quarkus.maven.dependency.ArtifactCoords;
import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.experimental.impl.config.DependencyResolutionConfig;
import org.jboss.da.model.rest.GAV;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

        config.getExcludeArtifacts().stream().map(ArtifactCoords::fromString).forEach(dominoConfig::addExcludePattern);
        config.getIncludeArtifacts().stream().map(ArtifactCoords::fromString).forEach(dominoConfig::addIncludePattern);
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
                .setProjectArtifacts(Set.of()) // TODO
                .setValidateCodeRepoTags(false) // TODO
                .setIncludeAlreadyBuilt(true); // TODO
    }

    public DependencyResult resolve(Path projectDir) {
        dominoConfig.setProjectDir(projectDir);
        ProjectDependencyResolver resolver = ProjectDependencyResolver.builder()
                .setArtifactResolver(getArtifactResolver(projectDir))
                .setDependencyConfig(dominoConfig)
                .build();
        return parseReleaseRepos(resolver.getReleaseRepos());
    }

    public DependencyResult resolve() {
        ProjectDependencyResolver resolver = ProjectDependencyResolver.builder()
                .setDependencyConfig(dominoConfig)
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

        DependencyResult result = new DependencyResult();
        result.setTopLevelProjects(rootProjects);
        result.setCount(releaseRepos.size());
        return result;
    }

    private String getSourceCodeURL(ReleaseId releaseId) {
        String sourceUrl = releaseId.origin().toString(); // TODO: this API will probably change
        try {
            URI uri = URI.create(sourceUrl);
            if (!uri.getScheme().startsWith("git") && !uri.getScheme().startsWith("http")) {
                return null;
            }
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return sourceUrl;
    }

    private String getSourceCodeRevision(ReleaseId releaseId) {
        return releaseId.version().asString(); // TODO: this API will probably change
    }
}
