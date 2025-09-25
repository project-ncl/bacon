package org.jboss.pnc.bacon.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.stream.SystemErr;
import uk.org.webcompere.systemstubs.stream.SystemOut;

@ExtendWith(SystemStubsExtension.class)
class AppTest {

    private static final String PNC_TEST_CLASSES = FilenameUtils.separatorsToSystem("pnc/target/test-classes");

    @SystemStub
    private SystemErr systemErr;

    @Test
    void testHelp(SystemProperties systemProperties, SystemOut systemOut) {
        App app = new App();
        systemProperties.set("picocli.ansi", "false");
        app.run(new String[] { "-h" });
        assertEquals(0, app.run(new String[] { "-h" }));

        assertThat(systemOut.getText()).contains(
                "Usage: bacon [-hoqvV] [--no-color] [-p=<configurationFileLocation>]",
                "[--profile=<profile>]");
    }

    @Test
    void testNoColor1(EnvironmentVariables environmentVariables) {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);
        environmentVariables.set("NO_COLOR", "true");
        App app = new App();
        assertEquals(
                1,
                app.run(
                        new String[] {
                                "-p",
                                configYaml.toString(),
                                "-v",
                                "pnc",
                                "-o",
                                "build",
                                "get",
                                "0" }));
        assertThat(systemErr.getText()).contains("Reconfiguring logger for NO_COLOR");
        // The first two lines will have the DEBUG with colour codes until it is reset
        assertThat(systemErr.getText()).contains(ANSIConstants.ESC_START);
        List<String> lines = new ArrayList<>(
                Arrays.asList(systemErr.getText().split(System.lineSeparator())));
        // Avoid issues debugging in IntelliJ due to classpath loading.
        lines.removeIf(
                s -> s.contains(
                        "Unable to retrieve manifest for class org.jboss.pnc.bacon.common.cli.VersionProvider as location is a directory not a jar"));
        assertThat(lines.subList(2, 10).toString()).doesNotContain(ANSIConstants.ESC_START);
    }

    @Test
    void testNoColor2(EnvironmentVariables environmentVariables) {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);
        App app = new App();

        environmentVariables.set("NO_COLOR", "true");
        assertEquals(
                1,
                app.run(new String[] { "-p", configYaml.toString(), "pnc", "-o", "build", "get", "0" }));
        assertThat(systemErr.getText()).doesNotContain(ANSIConstants.ESC_START);
        assertThat(System.getProperty("picocli.ansi")).contains("false");
    }

    @Test
    void testVerbose() {
        ObjectHelper.setRootLoggingLevel(Level.INFO);
        ObjectHelper.setLoggingLevel("org.jboss.pnc.client", Level.WARN);

        assertEquals(0, new App().run(new String[] { "pnc", "admin", "-h" }));
        assertThat(systemErr.getText()).doesNotContain("Log level set to DEBUG");
        assertEquals(0, new App().run(new String[] { "pnc", "-v", "-h" }));
        assertThat(systemErr.getText()).contains("Log level set to DEBUG");
    }

    @Test
    void testExampleUsage(SystemOut systemOut) {
        App app = new App();
        assertEquals(
                0,
                app.run(new String[] { "--verbose", "pnc", "admin", "pnc-status", "set", "-h" }));
        assertThat(systemOut.getText()).contains("""
                bacon pnc admin pnc-status set \\
                	--banner="Switching to maintenance mode for upcoming migration" \\
                	--eta="2025-07-30T15:00:00.000Z" isMaintenanceMode""");
    }

    @Test
    void testConfigPath() {
        String configPath = FilenameUtils.separatorsToSystem("/tmp/123456789");
        String configFile = FilenameUtils.separatorsToSystem(configPath + "/config.yaml");
        assertEquals(
                0,
                new App().run(new String[] { "-p", configPath, "-v", "pnc", "-o", "build", "-h" }));
        assertThat(systemErr.getText()).contains("Config file set from flag with profile default to " + configFile);
    }

    @Test
    void testJSONEnabled1() {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        assertEquals(
                1,
                new App().run(
                        new String[] { "-p", configYaml.toString(), "-v", "pnc", "-o", "build", "get", "0" }));
        assertThat(systemErr.getText()).contains("JSON command is enabled: true");
        assertThat(systemErr.getText()).contains(ANSIConstants.ESC_START);
        assertThat(
                Arrays.asList(systemErr.getText().split(System.lineSeparator()))
                        .subList(2, 10)
                        .toString())
                .contains(ANSIConstants.ESC_START);
    }

    @Test
    void testJSONEnabled2() {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        assertEquals(
                1,
                new App().run(
                        new String[] { "-o", "-p", configYaml.toString(), "-v", "pnc", "build", "get", "0" }));
        assertThat(systemErr.getText()).contains("JSON command is enabled: true");
    }

    @Test
    void testJSONEnabled3() {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        assertEquals(
                1,
                new App().run(
                        new String[] {
                                "--jsonOutput",
                                "-p",
                                configYaml.toString(),
                                "-v",
                                "pnc",
                                "build",
                                "get",
                                "0" }));
        assertThat(systemErr.getText()).contains("JSON command is enabled: true");
        System.out.println("### GOT TXT " + systemErr.getText());
    }

    @Test
    void testJSONDisabled() {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        assertEquals(
                1,
                new App().run(new String[] { "-p", configYaml.toString(), "-v", "pnc", "build", "get", "0" }));
        assertThat(systemErr.getText()).contains("JSON command is enabled: false");
    }

    @Test
    void testPigTempBuild(EnvironmentVariables environmentVariables, @TempDir File tempDir) {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File root = pncClasses.getParentFile().getParentFile().getParentFile();
        File buildConfig = new File(
                root,
                FilenameUtils.separatorsToSystem("integration-tests/src/test/resources/empty"));

        File configYaml = new File(root, PNC_TEST_CLASSES);

        ObjectHelper.setRootLoggingLevel(Level.INFO);
        ObjectHelper.setLoggingLevel("org.jboss.pnc.client", Level.WARN);

        environmentVariables.set(Constant.PIG_CONTEXT_DIR, tempDir.getAbsolutePath());
        assertEquals(
                1,
                new App().run(
                        new String[] {
                                "-p",
                                configYaml.toString(),
                                "-v",
                                "pig",
                                "configure",
                                "--releaseStorageUrl",
                                "http://www.example.com",
                                "--tempBuild",
                                buildConfig.toString() }));
        assertThat(systemErr.getText()).contains("Keycloak authentication failed!");
        assertThat(systemErr.getText()).contains("at org.jboss.pnc.bacon.auth.client.PncClientHelper.getCredential");
        assertThat(systemErr.getText()).doesNotContain("Unknown option: '--tempBuild'");
        System.out.println("### GOT TXT " + systemErr.getText());
    }

    @Test
    void testProfile(SystemProperties systemProperties, SystemOut systemOut) {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);
        App app = new App();

        systemProperties.set("picocli.ansi", "false");
        assertEquals(
                0,
                app.run(
                        new String[] {
                                "--verbose",
                                "pnc",
                                "--profile",
                                "foobar",
                                "admin",
                                "pnc-status",
                                "set",
                                "-h" }));

        String expected = """
                Usage: bacon pnc admin pnc-status set [-hoqvV] [--no-color] [--banner=<banner>]
                                                      [--eta=<eta>]
                                                      [-p=<configurationFileLocation>]
                                                      [--profile=<profile>] <isMaintenanceMode>
                This will set the PNC status
                      <isMaintenanceMode>   Is maintenance mode ON or OFF?
                      --banner=<banner>     Banner
                      --eta=<eta>           ETA
                  -h, --help                Show this help message and exit.
                      --no-color            Disable color output. Useful when running in a
                                              non-ANSI environment
                  -o, --jsonOutput          use json for output (default to yaml)
                  -p, --configPath=<configurationFileLocation>
                                            Path to PNC configuration folder
                      --profile=<profile>   PNC Configuration profile
                  -q, --quiet               Silent output
                  -v, --verbose             Verbose output
                  -V, --version             Print version information and exit.

                Example:
                $ bacon pnc admin pnc-status set \\
                	--banner="Switching to maintenance mode for upcoming migration" \\
                	--eta="2025-07-30T15:00:00.000Z" isMaintenanceMode""";
        assertThat(systemOut.getText()).contains(expected);

        assertEquals(
                0,
                app.run(
                        new String[] {
                                "--configPath",
                                configYaml.toString(),
                                "--verbose",
                                "pnc",
                                "--profile",
                                "foobar",
                                "admin",
                                "maintenance-mode",
                                "activate",
                                "-h" }));
        assertThat(systemErr.getText()).containsOnlyOnce("Config file set from flag with profile foobar to");
        System.out.println("### GOT TXT " + systemErr.getText());
    }
}
