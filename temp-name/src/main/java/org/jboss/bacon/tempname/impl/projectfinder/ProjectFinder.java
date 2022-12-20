package org.jboss.bacon.tempname.impl.projectfinder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.jboss.bacon.da.DaHelper;
import org.jboss.bacon.da.rest.endpoint.LookupApi;
import org.jboss.bacon.tempname.impl.dependencies.DependencyResult;
import org.jboss.bacon.tempname.impl.dependencies.Project;
import org.jboss.da.lookup.model.MavenVersionsRequest;
import org.jboss.da.lookup.model.MavenVersionsResult;
import org.jboss.da.lookup.model.VersionDistanceRule;
import org.jboss.da.model.rest.GA;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.restclient.util.ArtifactUtil;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ProjectFinder {

    private final LookupApi lookupApi;
    private final ArtifactClient artifactClient;
    private final BuildClient buildClient;
    private final BuildConfigurationClient buildConfigClient;

    public ProjectFinder() {
        lookupApi = DaHelper.createLookupApi();
        artifactClient = new ClientCreator<>(ArtifactClient::new).newClient();
        buildClient = new ClientCreator<>(BuildClient::new).newClient();
        buildConfigClient = new ClientCreator<>(BuildConfigurationClient::new).newClient();
    }

    ProjectFinder(
            LookupApi lookupApi,
            ArtifactClient artifactClient,
            BuildClient buildClient,
            BuildConfigurationClient buildConfigClient) {
        this.lookupApi = lookupApi;
        this.artifactClient = artifactClient;
        this.buildClient = buildClient;
        this.buildConfigClient = buildConfigClient;
    }

    public FoundProjects findProjects(DependencyResult dependencies) {
        Set<Project> projects = new HashSet<>();
        traverseTree(projects, dependencies.getTopLevelProjects());

        Set<GAV> allGAVs = projects.stream().flatMap(p -> p.getGavs().stream()).collect(Collectors.toSet());

        Map<GAV, List<String>> availableVersions = findAvailableVersions(allGAVs);

        FoundProjects foundProjects = new FoundProjects();
        for (Project project : projects) {
            foundProjects.getFoundProjects().add(findProject(project, availableVersions));
        }

        return foundProjects;
    }

    private FoundProject findProject(Project project, Map<GAV, List<String>> availableVersions) {
        FoundProject found = new FoundProject();
        found.setGavs(project.getGavs());
        Set<GAV> gavs = project.getGavs();
        GAV gav = gavs.stream()
                .sorted(Comparator.comparing(GAV::getGroupId).thenComparing(GAV::getArtifactId))
                .findFirst()
                .get();
        BuildVersion buildVersion = findBuild(gav, availableVersions.get(gav));

        if (buildVersion == null) {
            found.setBuildConfig(Optional.empty());
            return found;
        }
        found.setComplete(validateBuild(gavs, buildVersion.build));
        found.setExactMatch(isExactVersion(gav.getVersion(), buildVersion.version));
        found.setBuildConfig(Optional.of(getBuildConfigurationRevision(buildVersion.build)));
        return found;
    }

    private boolean isExactVersion(String query, String found) {
        String suffixless = found.replaceFirst("[.-]redhat-[0-9]+$", "");
        return query.equals(suffixless);
    }

    private boolean validateBuild(Set<GAV> gavs, Build build) {
        try {
            Set<GA> missing = gavs.stream().map(GAV::getGA).collect(Collectors.toCollection(HashSet::new));
            RemoteCollection<Artifact> builtArtifacts = buildClient.getBuiltArtifacts(build.getId());
            for (Artifact builtArtifact : builtArtifacts) {
                SimpleArtifactRef coords = ArtifactUtil.parseMavenCoordinates(builtArtifact);
                if (coords != null) {
                    GA ga = new GA(coords.getGroupId(), coords.getArtifactId());
                    missing.remove(ga);
                    if (missing.isEmpty())
                        return true;
                }
            }
            log.warn("Build " + build + "does not produce these GAs: " + missing);
            return false;
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<GAV, List<String>> findAvailableVersions(Set<GAV> allGAVs) {
        MavenVersionsRequest request = MavenVersionsRequest.builder()
                .mode("TEMPORARY_PREFER_PERSISTENT")
                .artifacts(allGAVs)
                .distanceRule(VersionDistanceRule.CLOSEST_BY_PARTS)
                .build();
        Set<MavenVersionsResult> versionsResults = lookupApi.versionsMaven(request);
        for (MavenVersionsResult versionsResult : versionsResults) {
            versionsResult.getGav();
        }

        Map<GAV, List<String>> availableVersions = versionsResults.stream()
                .collect(Collectors.toMap(MavenVersionsResult::getGav, MavenVersionsResult::getAvailableVersions));
        return availableVersions;
    }

    private BuildVersion findBuild(GAV gav, List<String> versions) {
        try {
            for (String version : versions) {
                GAV toSearch = new GAV(gav.getGA(), version);
                Build build = searchBuild(toSearch);
                if (build != null) {
                    return new BuildVersion(build, toSearch.getVersion());
                }
            }
            return null;
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }
    }

    private BuildConfigurationRevision getBuildConfigurationRevision(Build build) {
        try {
            BuildConfigurationRevisionRef buildConfigRevision = build.getBuildConfigRevision();
            return buildConfigClient.getRevision(buildConfigRevision.getId(), buildConfigRevision.getRev());
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }
    }

    private Build searchBuild(GAV gav) throws RemoteResourceException {
        String identifier = gav.getGroupId() + ":" + gav.getArtifactId() + ":pom:" + gav.getVersion();
        String identifierQuery = "identifier==" + identifier;
        RemoteCollection<Artifact> artifacts = artifactClient
                .getAll(null, null, null, Optional.empty(), Optional.of(identifierQuery));
        if (artifacts.size() == 0) {
            throw new IllegalStateException("There should exist artifact with identifier " + identifier);
        } else if (artifacts.size() > 1) {
            throw new IllegalStateException("There should exist only one artifact with identifier " + identifier);
        }
        Artifact singleArtifact = artifacts.iterator().next();
        return singleArtifact.getBuild();
    }

    private void traverseTree(Set<Project> projects, Set<Project> topLevelProjects) {
        for (Project p : topLevelProjects) {
            if (!projects.contains(p)) {
                projects.add(p);
                traverseTree(projects, p.getDependencies());
            }
        }
    }

    @AllArgsConstructor
    private static class BuildVersion {
        private Build build;
        private String version;
    }

}
