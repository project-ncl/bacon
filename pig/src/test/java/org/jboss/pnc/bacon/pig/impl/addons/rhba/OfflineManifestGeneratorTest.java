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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OfflineManifestGeneratorTest {

    private EasyRandom easyRandom = new EasyRandom();

    @Test
    public void testGeneratedOfflineManifest() throws Exception {
        BuildInfoCollector buildInfoCollector = Mockito.mock(BuildInfoCollector.class);
        PncBuild build = new PncBuild();
        TargetRepository tr = TargetRepository.refBuilder()
                .identifier("id1")
                .repositoryType(RepositoryType.MAVEN)
                .build();
        Artifact artifact = Artifact.builder()
                .targetRepository(tr)
                .sha256("ada246bd35bf6c1251d2a6ab2e369cd4c22e0b7d7ee75316a6f0a95d51b094cd")
                .filename("optaplanner-benchmark-7.44.0.Final-redhat-00001.jar")
                .identifier("org.optaplanner:optaplanner-benchmark:jar:7.44.0.Final-redhat-00001")
                .build();
        List<Artifact> artifactList = new ArrayList<>();
        artifactList.add(artifact);
        build.addBuiltArtifacts(artifactList);
        Map<String, PncBuild> builds = new HashMap<>();
        builds.put("org:org1:jar", build);
        PigConfiguration pigConfiguration = easyRandom.nextObject(PigConfiguration.class);
        OfflineManifestGenerator generator = new OfflineManifestGenerator(
                pigConfiguration,
                builds,
                "target/",
                "extras",
                buildInfoCollector);
        generator.trigger();
        StringBuilder offlinerContent = new StringBuilder();
        assertTrue(Paths.get("target/offliner.txt").toFile().exists());
        try (Stream<String> lines = Files.lines(Paths.get("target/offliner.txt"))) {
            lines.forEach(string -> offlinerContent.append(string));
        }
        assertTrue(offlinerContent.length() > 0);
        String offlinerAsString = offlinerContent.toString();
        ArtifactWrapper artifactWrapper = new ArtifactWrapper(artifact);
        GAV gav = artifactWrapper.toGAV();
        String artifactAsOfflinerEntry = String
                .format("%s,%s/%s", artifact.getSha256(), gav.toVersionPath(), gav.toFileName());
        assertTrue(offlinerAsString.contains(artifactAsOfflinerEntry));
    }

}
