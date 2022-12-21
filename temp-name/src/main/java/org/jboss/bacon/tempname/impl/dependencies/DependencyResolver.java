package org.jboss.bacon.tempname.impl.dependencies;

import io.quarkus.bom.decomposer.ReleaseOrigin;
import io.quarkus.bom.decomposer.maven.ProjectDependencyConfig;
import io.quarkus.bom.decomposer.maven.ProjectDependencyResolver;
import io.quarkus.bom.decomposer.maven.ReleaseRepo;
import org.jboss.bacon.tempname.impl.config.DependencyResolutionConfig;
import org.jboss.da.model.rest.GAV;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyResolver {

    private final ProjectDependencyConfig.Mutable config;

    public DependencyResolver(DependencyResolutionConfig dependencyResolutionConfig) {
        config = ProjectDependencyConfig.builder();
        config.setExcludePatterns(Set.of()) // TODO
                .setExcludeBomImports(false) // TODO
                .setExcludeGroupIds(Set.of()) // TODO
                .setExcludeKeys(Set.of()) // TODO
                .setExcludeParentPoms(false) // TODO
                .setIncludeArtifacts(Set.of()) // TODO
                .setIncludeGroupIds(Set.of()) // TODO
                .setIncludeKeys(Set.of()) // TODO
                .setLevel(-1)
                .setProjectArtifacts(Set.of())
                // .setProjectArtifacts(rootArtifacts.stream().map(ArtifactCoords::fromString).collect(Collectors.toList()))
                .setValidateCodeRepoTags(false) // TODO
                .setIncludeAlreadyBuilt(true); // TODO
    }

    public DependencyResult resolve() {
        ProjectDependencyResolver resolver = ProjectDependencyResolver.builder().setDependencyConfig(config).build();

        return parseReleaseRepos(resolver.getReleaseRepos());
    }

    private DependencyResult parseReleaseRepos(Collection<ReleaseRepo> releaseRepos) {
        Map<ReleaseRepo, Project> mapping = new HashMap<>();
        Set<Project> rootProjects = new HashSet<>();
        for (ReleaseRepo repo : releaseRepos) {
            Project project = new Project();
            Set<GAV> gavs = repo.getArtifacts()
                    .stream()
                    .map(a -> new GAV(a.getGroupId(), a.getArtifactId(), a.getVersion()))
                    .collect(Collectors.toSet());
            project.setGavs(gavs);
            project.setSourceCodeURL(getSourceCodeURL(repo.id().origin()));
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
        return result;
    }

    private String getSourceCodeURL(ReleaseOrigin origin) {
        return origin.toString(); // TODO: this API will probably change
    }
}
