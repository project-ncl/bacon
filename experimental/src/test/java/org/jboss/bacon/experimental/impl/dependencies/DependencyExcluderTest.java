package org.jboss.bacon.experimental.impl.dependencies;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.net.URL;

import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

public class DependencyExcluderTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(
                    wireMockConfig().port(45656)
                            .usingFilesUnderDirectory("src/test/resources/wiremock")
                            .notifier(new ConsoleNotifier(false)))
            .build();

    private static WireMockServer mockServer;

    @BeforeAll
    static void setup() throws IOException {
        File file = new File(DependencyExcluderTest.class.getClassLoader().getResource("config/config.yaml").getFile());
        Config.configure(file.getParent(), "config.yaml", "default");
        Config.initialize();
    }

    @Test
    void parseEmpty() {
        // DependencyExcluder dependencyExcluder = new DependencyExcluder();
        String[] excludedDependencies = DependencyExcluder.getExcludedGavs("");
        assertThat(excludedDependencies).isNotNull();
        assertThat(excludedDependencies.length).isEqualTo(0);
    }

    @Test
    void parseNull() {
        // DependencyExcluder dependencyExcluder = new DependencyExcluder();
        String[] excludedDependencies = DependencyExcluder.getExcludedGavs(null);
        assertThat(excludedDependencies).isNotNull();
        assertThat(excludedDependencies.length).isEqualTo(0);
    }

    @Test
    void parseEmptyFile() {
        try {
            final String emptyExcludedFile = getFileString("excluded-empty.txt");
            String[] excludedDependencies = DependencyExcluder.getExcludedGavs(emptyExcludedFile);
        } catch (IOException e) {
            assertThat(false).isTrue();
        }
    }

    @Test
    void parseSampleFile() {
        try {
            final String emptyExcludedFile = getFileString("excluded-sample.txt");
            String[] excludedDependencies = DependencyExcluder.getExcludedGavs(emptyExcludedFile);
            assertThat(excludedDependencies).isNotNull();
            assertThat(excludedDependencies.length).isEqualTo(11);
            assertThat(excludedDependencies[0]).isEqualTo("io.vertx:vertx-amqp-client:4.3.7");
            assertThat(excludedDependencies[10]).isEqualTo("io.vertx:vertx-codegen:4.3.7");
        } catch (IOException e) {
            assertThat(false).isTrue();
        }
    }

    @Test
    void parseSampleFileFromHost() {
        DependencyExcluder de = new DependencyExcluder(Config.instance().getActiveProfile().getAutobuild());
        String exclusionFileContents = de.fetchExclusionFile();
        String[] excludedDependencies = DependencyExcluder.getExcludedGavs(exclusionFileContents);
        assertThat(excludedDependencies).isNotNull();
        assertThat(excludedDependencies.length).isEqualTo(10);
        assertThat(excludedDependencies[0]).isEqualTo("io.vertx:vertx-amqp-client:4.3.7");
        assertThat(excludedDependencies[4]).isEqualTo("junit:junit:4.13.2");
        assertThat(excludedDependencies[9]).isEqualTo("io.vertx:vertx-codegen:4.3.7");
    }

    @Test
    void missingGavExclusionFromBaconConfigShouldntFail() {
        File file = new File(
                DependencyExcluderTest.class.getClassLoader()
                        .getResource("config/config-missing-gavExclusionUrl.yaml")
                        .getFile());
        Config.configure(file.getParent(), "config-missing-gavExclusionUrl.yaml", "default");
        try {
            Config.initialize();
            DependencyExcluder de = new DependencyExcluder(Config.instance().getActiveProfile().getAutobuild());
            String exclusionFileContents = de.fetchExclusionFile();
            String[] excludedDependencies = DependencyExcluder.getExcludedGavs(exclusionFileContents);
            assertThat(excludedDependencies).isNotNull();
            assertThat(excludedDependencies.length).isEqualTo(0);
        } catch (IOException e) {
            assertThat(false).isTrue();
        }
    }

    public static String getFileString(final String fileName) throws IOException {
        String fileContents = null;
        ClassLoader classLoader = DependencyExcluderTest.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        InputStream resourceStream = classLoader.getResourceAsStream(fileName);
        if (resource != null) {
            StringBuilder textBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resourceStream))) {
                int c;
                while ((c = br.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            fileContents = textBuilder.toString();
        }
        return fileContents;
    }
}
