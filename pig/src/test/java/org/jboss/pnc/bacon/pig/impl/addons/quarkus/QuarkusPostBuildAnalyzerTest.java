package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class QuarkusPostBuildAnalyzerTest {
    WireMockServer wireMockServer = new WireMockServer();

    @Test
    public void analyzePostBuildDeps() throws IOException {
        QuarkusPostBuildAnalyzer quarkusPostBuildAnalyzer = new QuarkusPostBuildAnalyzer(
                preConfig(),
                null,
                "/test-release-path",
                "/test-extras-path");
        wireMockServer.start();
        stubFor(
                get(urlEqualTo("/quarkus/?C=M;O=D")).willReturn(
                        aResponse().withHeader("Content-type", "text/html;charset=UTF-8")
                                .withBodyFile("build-stub.html")));
        // stubbing csv files
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.1/extras/community-dependencies.csv")).willReturn(
                        aResponse().withHeader("Content-type", "text/csv").withBodyFile("community-deps2.csv")));
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.0/extras/community-dependencies.csv")).willReturn(
                        aResponse().withHeader("Content-type", "text/csv").withBodyFile("community-deps1.csv")));
        // stubbing text files
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.1/extras/repository-artifact-list.txt")).willReturn(
                        aResponse().withHeader("Content-type", "text/plain; charset=UTF-8")
                                .withBodyFile("repository-artifact-list2.txt")));
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.0/extras/repository-artifact-list.txt")).willReturn(
                        aResponse().withHeader("Content-type", "text/plain; charset=UTF-8")
                                .withBodyFile("repository-artifact-list1.txt")));
        quarkusPostBuildAnalyzer.trigger();
        wireMockServer.stop();
        if (!verifyResults()) {
            throw new FatalException("The quarkus post build analyzer addon is not working correctly");
        }
    }

    @Test
    public void analyzePostBuildDepsFailTest() {
        QuarkusPostBuildAnalyzer quarkusPostBuildAnalyzer = new QuarkusPostBuildAnalyzer(
                preConfig(),
                null,
                "/test-release-path",
                "/test-extras-path");
        quarkusPostBuildAnalyzer.trigger();
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

    private boolean verifyResults() throws IOException {
        File expectedResult = new File(
                getClass().getClassLoader().getResource("__files/expectedPostBuildInfo.txt").getFile());
        File actualResult = new File("post-build-info.txt");
        return FileUtils.contentEquals(expectedResult, actualResult);
    }
}
