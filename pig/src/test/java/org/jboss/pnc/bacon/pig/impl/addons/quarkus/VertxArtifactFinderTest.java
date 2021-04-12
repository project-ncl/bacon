package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.pnc.common.json.JsonOutputConverterMapper.log;

public class VertxArtifactFinderTest {

    @Test
    public void vertxArtifactFinderTest() throws IOException {

        Path repoPath = Paths.get("src/test/resources/VertxTestRepoZip/vertxListTextFile.txt");

        VertxArtifactFinder vertxArtifactFinder = new VertxArtifactFinder(
                preConfig(),
                null,
                "/test-release-path",
                "/test-extras-path",
                repoPath);

        vertxArtifactFinder.trigger();

        if (!verifyResult()) {
            throw new FatalException("The quarkus vertex Artifact Finder addon is not working correctly");
        } else {
            log.info(" RESULT IS VERIFIED , files matched ");
        }

    }

    private boolean verifyResult() throws IOException {
        log.info(" VERIFYING RESULT ");
        File actualResult = new File(
                "/home/sausingh/Desktop/Productisation/build-configurations/Quarkus/1.11/target/quarkus-1.11.5.ER8/extras/vertxList.txt");
        File expectedResult = new File("src/test/resources/VertxTestRepoZip/vertxListTextFile.txt");
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
