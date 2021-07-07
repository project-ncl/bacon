package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VertxArtifactFinderTest {

    @Test
    public void vertxArtifactFinderTest() throws IOException {

        Path repoPath = Paths.get("src/test/resources/VertxTestRepoZip/rh-quarkus-maven-repository.zip/");

        VertxArtifactFinder vertxArtifactFinder = new VertxArtifactFinder(
                preConfig(),
                null,
                "/test-release-path",
                "/test-extras-path",
                repoPath);

        File vertxListExpectedFile = new File("src/test/resources/VertxTestRepoZip/vertxListExpectedResult.txt");
        Path vertxListExpectedFilePath = Paths.get("src/test/resources/VertxTestRepoZip/vertxListExpectedResult.txt");
        List<String> vertexArtifactList = Arrays.asList("vertx", "vertx-core");
        byte[] bytes = vertexArtifactList.toString().getBytes();

        try {
            Files.write(vertxListExpectedFilePath, bytes);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        vertxArtifactFinder.trigger();

        if (!verifyResult()) {
            throw new FatalException("The quarkus vertex Artifact Finder addon is not working correctly");
        }
    }

    private boolean verifyResult() throws IOException {
        // TODO : change actual Result
        File actualResult = new File(
                "/home/sausingh/Desktop/Productisation/build-configurations/Quarkus/1.11/target/quarkus-1.11.5.ER8/extras/vertxList.txt");
        File expectedResult = new File("src/test/resources/VertxTestRepoZip/vertxListExpectedResult.txt");
        return FileUtils.contentEquals(expectedResult, actualResult);
    }

    private PigConfiguration preConfig() {
        PigConfiguration config = new PigConfiguration();
        HashMap<String, Map<String, ?>> addons = new HashMap<>();
        HashMap<String, Object> addOnConfig = new HashMap<>();
        addOnConfig.put("stagingPath", "http://localhost:8080/");
        addOnConfig.put("productName", "quarkus");
        addons.put(QuarkusPostBuildAnalyzer.NAME, addOnConfig);
        config.setAddons(addons);
        return config;
    }
}
