package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

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
        // stubbing artifact text files
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.1/extras/repository-artifact-list.txt")).willReturn(
                        aResponse().withHeader("Content-type", "text/plain; charset=UTF-8")
                                .withBodyFile("repository-artifact-list2.txt")));
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.0/extras/repository-artifact-list.txt")).willReturn(
                        aResponse().withHeader("Content-type", "text/plain; charset=UTF-8")
                                .withBodyFile("repository-artifact-list1.txt")));
        // stubbing nonexistent-redhat-deps files
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.1/extras/nonexistent-redhat-deps.txt")).willReturn(
                        aResponse().withHeader("Content-type", "text/plain; charset=UTF-8")
                                .withBodyFile("nonexistent-redhat-deps2.txt")));
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.0/extras/nonexistent-redhat-deps.txt")).willReturn(
                        aResponse().withHeader("Content-type", "text/plain; charset=UTF-8")
                                .withBodyFile("nonexistent-redhat-deps1.txt")));
        // stubbing license files
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.1/")).willReturn(
                        aResponse().withHeader("Content-type", "text/html;charset=UTF-8")
                                .withBodyFile("new-prod-deliverable-stub.html")));
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.0/")).willReturn(
                        aResponse().withHeader("Content-type", "text/html;charset=UTF-8")
                                .withBodyFile("old-prod-deliverable-stub.html")));
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.1/quarkus-1.0.1-license.zip")).willReturn(
                        aResponse().withHeader("Content-type", "application/zip")
                                .withBodyFile("quarkus-1.0.1-licenses.zip")));
        stubFor(
                get(urlEqualTo("/quarkus/quarkus-1.0.0/quarkus-1.0.0-license.zip")).willReturn(
                        aResponse().withHeader("Content-type", "application/zip")
                                .withBodyFile("quarkus-1.0.0-licenses.zip")));
        quarkusPostBuildAnalyzer.trigger();
        wireMockServer.stop();
        if (!verifyResults()) {
            cleanup();
            throw new FatalException("The quarkus post build analyzer addon is not working correctly");
        }
        cleanup();
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

    private void cleanup() {
        List<String> files = Arrays.asList(
                new String[] {
                        "latest.zip",
                        "latest_artifacts.txt",
                        "latest_nonexistent-redhat-deps.txt",
                        "new_dependencies.csv",
                        "new_license.xml",
                        "old.zip",
                        "old_artifacts.txt",
                        "old_dependencies.csv",
                        "old_license.xml",
                        "old_nonexistent-redhat-deps.txt",
                        "post-build-info.txt" });
        files.stream().forEach(path -> {
            File f = new File(path);
            f.delete();
        });
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
