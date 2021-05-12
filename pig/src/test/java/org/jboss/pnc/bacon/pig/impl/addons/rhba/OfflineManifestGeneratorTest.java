package org.jboss.pnc.bacon.pig.impl.addons.rhba;

import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.TargetRepository;
import org.jboss.pnc.enums.RepositoryType;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy.BUILD_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OfflineManifestGeneratorTest {

    private BuildInfoCollector buildInfoCollector;
    private PigConfiguration pigConfiguration;
    private Set<Artifact> artifacts;
    private Map<String, PncBuild> builds;

    @BeforeEach
    public void testSetUp() {
        buildInfoCollector = Mockito.mock(BuildInfoCollector.class);

        EasyRandom easyRandom = new EasyRandom();
        pigConfiguration = easyRandom.nextObject(PigConfiguration.class);
        pigConfiguration.getFlow().getRepositoryGeneration().setStrategy(BUILD_GROUP);

        mockBuildsAndArtifacts();
    }

    private void mockBuildsAndArtifacts() {
        TargetRepository repository = TargetRepository.refBuilder()
                .identifier("repo1")
                .repositoryType(RepositoryType.MAVEN)
                .build();

        builds = new HashMap<>();
        artifacts = new HashSet<>();
        for (int i = 1; i <= 5; i++) {
            String buildName = "build" + i;

            List<Artifact> builtArtifacts = new ArrayList<>();
            for (int j = 1; j <= i * 2; j++) {
                builtArtifacts.add(
                        Artifact.builder()
                                .targetRepository(repository)
                                .identifier("org:built:jar:" + i + "." + j)
                                .filename("built-" + i + "." + j + ".jar")
                                .sha256("built_" + i + "_" + j)
                                .build());
            }

            List<Artifact> dependencies = new ArrayList<>();
            for (int k = 1; k <= i * 3; k++) {
                dependencies.add(
                        Artifact.builder()
                                .targetRepository(repository)
                                .identifier("org:dependency:jar:" + i + "." + k)
                                .filename("dependency-" + i + "." + k + ".jar")
                                .sha256("dependency_" + i + "_" + k)
                                .build());
            }

            PncBuild build = new PncBuild();
            build.setId(buildName);
            build.setName(buildName);
            build.addBuiltArtifacts(builtArtifacts);
            build.addDependencyArtifacts(dependencies);
            builds.put(buildName, build);

            artifacts.addAll(builtArtifacts);
            artifacts.addAll(dependencies);
        }
    }

    @Test
    public void testGeneration() throws Exception {
        OfflineManifestGenerator generator = new OfflineManifestGenerator(
                pigConfiguration,
                builds,
                "target/",
                "extras",
                buildInfoCollector);
        generator.trigger();

        Path offlinerFilePath = Paths.get("target/offliner.txt");
        assertTrue(offlinerFilePath.toFile().exists());

        List<String> offlinerContent = Files.lines(offlinerFilePath).collect(Collectors.toList());
        assertEquals(artifacts.size(), offlinerContent.size());

        for (PncBuild build : builds.values()) {
            assertBuildInclusion(build, offlinerContent, true);
        }
    }

    @Test
    public void testBuildGroupExcludedSources() throws IOException {
        List<String> excludeSourceBuilds = Arrays.asList("build2", "build4");
        pigConfiguration.getFlow().getRepositoryGeneration().setExcludeSourceBuilds(excludeSourceBuilds);

        OfflineManifestGenerator generator = new OfflineManifestGenerator(
                pigConfiguration,
                builds,
                "target/",
                "extras",
                buildInfoCollector);
        generator.trigger();

        Path offlinerFilePath = Paths.get("target/offliner.txt");
        List<String> offlinerContent = Files.lines(offlinerFilePath).collect(Collectors.toList());

        for (PncBuild build : builds.values()) {
            assertBuildInclusion(build, offlinerContent, !excludeSourceBuilds.contains(build.getId()));
        }
    }

    private void assertBuildInclusion(PncBuild build, List<String> offlinerContent, boolean shouldBeIncluded) {
        List<ArtifactWrapper> builtAndDependencies = new ArrayList<>();
        builtAndDependencies.addAll(build.getBuiltArtifacts());
        builtAndDependencies.addAll(build.getDependencyArtifacts());
        for (ArtifactWrapper artifact : builtAndDependencies) {
            GAV gav = artifact.toGAV();
            String offlinerEntry = String
                    .format("%s,%s/%s", artifact.getSha256(), gav.toVersionPath(), gav.toFileName());
            assertEquals(shouldBeIncluded, offlinerContent.contains(offlinerEntry));
        }
    }
}
