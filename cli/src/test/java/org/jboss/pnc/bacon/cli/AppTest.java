package org.jboss.pnc.bacon.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import org.apache.commons.io.FilenameUtils;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    private static final String PNC_TEST_CLASSES = FilenameUtils.separatorsToSystem("pnc/target/test-classes");

    @Test
    void testHelp() throws Exception {
        App app = new App();
        restoreSystemProperties(() -> {
            System.setProperty("picocli.ansi", "false");
            app.run(new String[] { "-h" });
            String text = tapSystemOut(() -> assertEquals(0, app.run(new String[] { "-h" })));
            assertThat(text).contains(
                    "Usage: bacon [-hoqvV] [--no-color] [-p=<configurationFileLocation>]",
                    "[--profile=<profile>]");
        });
    }

    @Test
    void testNoColor1() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);
        App app = new App();
        withEnvironmentVariable("NO_COLOR", "true").execute(() -> {
            String text = tapSystemErr(
                    () -> assertEquals(
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
                                            "0" })));
            assertThat(text).contains("Reconfiguring logger for NO_COLOR");
            // The first two lines will have the DEBUG with colour codes until it is reset
            assertThat(text).contains(ANSIConstants.ESC_START);
            List<String> lines = new ArrayList<>(Arrays.asList(text.split(System.getProperty("line.separator"))));
            // Avoid issues debugging in IntelliJ due to classpath loading.
            lines.removeIf(
                    s -> s.contains(
                            "Unable to retrieve manifest for class org.jboss.pnc.bacon.common.cli.VersionProvider as location is a directory not a jar"));
            assertThat(lines.subList(2, 10).toString()).doesNotContain(ANSIConstants.ESC_START);
        });
    }

    @Test
    void testNoColor2() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);
        App app = new App();

        withEnvironmentVariable("NO_COLOR", "true").execute(() -> {
            String text = tapSystemErr(
                    () -> assertEquals(
                            1,
                            app.run(new String[] { "-p", configYaml.toString(), "pnc", "-o", "build", "get", "0" })));
            assertThat(text).doesNotContain(ANSIConstants.ESC_START);
            assertThat(System.getProperty("picocli.ansi")).contains("false");
        });
    }

    @Test
    void testVerbose() throws Exception {
        ObjectHelper.setRootLoggingLevel(Level.INFO);
        ObjectHelper.setLoggingLevel("org.jboss.pnc.client", Level.WARN);

        String text = tapSystemErr(() -> assertEquals(0, new App().run(new String[] { "pnc", "admin", "-h" })));
        assertThat(text).doesNotContain("Log level set to DEBUG");
        text = tapSystemErr(() -> assertEquals(0, new App().run(new String[] { "pnc", "-v", "-h" })));
        assertThat(text).contains("Log level set to DEBUG");
    }

    @Test
    void testExampleUsage() throws Exception {
        App app = new App();
        String text = tapSystemOut(
                () -> assertEquals(
                        0,
                        app.run(new String[] { "--verbose", "pnc", "admin", "maintenance-mode", "activate", "-h" })));
        assertThat(text).contains("bacon pnc admin maintenance-mode activate \"Switching");
    }

    @Test
    void testConfigPath() throws Exception {
        String configPath = FilenameUtils.separatorsToSystem("/tmp/123456789");
        String configFile = FilenameUtils.separatorsToSystem(configPath + "/config.yaml");
        String text = tapSystemErr(
                () -> assertEquals(
                        0,
                        new App().run(new String[] { "-p", configPath, "-v", "pnc", "-o", "build", "-h" })));
        assertThat(text).contains("Config file set from flag with profile default to " + configFile);
    }

    @Test
    void testJSONEnabled1() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        String text = tapSystemErr(
                () -> assertEquals(
                        1,
                        new App().run(
                                new String[] { "-p", configYaml.toString(), "-v", "pnc", "-o", "build", "get", "0" })));
        assertThat(text).contains("JSON command is enabled: true");
        assertThat(text).contains(ANSIConstants.ESC_START);
        assertThat(Arrays.asList(text.split(System.getProperty("line.separator"))).subList(2, 10).toString())
                .contains(ANSIConstants.ESC_START);
    }

    @Test
    void testJSONEnabled2() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        String text = tapSystemErr(
                () -> assertEquals(
                        1,
                        new App().run(
                                new String[] { "-o", "-p", configYaml.toString(), "-v", "pnc", "build", "get", "0" })));
        assertThat(text).contains("JSON command is enabled: true");
    }

    @Test
    void testJSONEnabled3() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        String text = tapSystemErr(
                () -> assertEquals(
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
                                        "0" })));
        assertThat(text).contains("JSON command is enabled: true");
    }

    @Test
    void testJSONDisabled() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        String text = tapSystemErr(
                () -> assertEquals(
                        1,
                        new App().run(new String[] { "-p", configYaml.toString(), "-v", "pnc", "build", "get", "0" })));
        assertThat(text).contains("JSON command is enabled: false");
    }

    @Test
    void testPigTempBuild() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File root = pncClasses.getParentFile().getParentFile().getParentFile();
        File buildConfig = new File(
                root,
                FilenameUtils.separatorsToSystem("integration-tests/src/test/resources/empty"));

        File configYaml = new File(root, PNC_TEST_CLASSES);

        ObjectHelper.setRootLoggingLevel(Level.INFO);
        ObjectHelper.setLoggingLevel("org.jboss.pnc.client", Level.WARN);

        String text;
        text = tapSystemErr(
                () -> assertEquals(
                        1,
                        new App().run(
                                new String[] {
                                        "-p",
                                        configYaml.toString(),
                                        "pig",
                                        "configure",
                                        "--releaseStorageUrl",
                                        "http://www.example.com",
                                        "--tempBuild",
                                        buildConfig.toString() })));

        assertThat(text).contains("Keycloak authentication failed!");
        assertThat(text).doesNotContain("at org.jboss.pnc.bacon.pnc.client.PncClientHelper.getBearerToken");
        assertThat(text).doesNotContain("Unknown option: '--tempBuild'");

        text = tapSystemErr(
                () -> assertEquals(
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
                                        buildConfig.toString() })));
        assertThat(text).contains("Keycloak authentication failed!");
        assertThat(text).contains("at org.jboss.pnc.bacon.pnc.client.PncClientHelper.getBearerToken");
        assertThat(text).doesNotContain("Unknown option: '--tempBuild'");
    }

    @Test
    void testProfile() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);
        App app = new App();

        restoreSystemProperties(() -> {
            System.setProperty("picocli.ansi", "false");
            String text = tapSystemOut(
                    () -> assertEquals(
                            0,
                            app.run(
                                    new String[] {
                                            "--verbose",
                                            "pnc",
                                            "--profile",
                                            "foobar",
                                            "admin",
                                            "maintenance-mode",
                                            "activate",
                                            "-h" })));

            String expected = String.format(
                    "Usage: bacon pnc admin maintenance-mode activate [-hoqvV] [--no-color]%n"
                            + "       [-p=<configurationFileLocation>] [--profile=<profile>] <reason>%n"
                            + "This will disable any new builds from being accepted%n"
                            + "      <reason>              Reason%n"
                            + "  -h, --help                Show this help message and exit.%n"
                            + "      --no-color            Disable color output. Useful when running in a%n"
                            + "                              non-ANSI environment%n"
                            + "  -o, --jsonOutput          use json for output (default to yaml)%n"
                            + "  -p, --configPath=<configurationFileLocation>%n"
                            + "                            Path to PNC configuration folder%n"
                            + "      --profile=<profile>   PNC Configuration profile%n"
                            + "  -q, --quiet               Silent output%n"
                            + "  -v, --verbose             Verbose output%n"
                            + "  -V, --version             Print version information and exit.%n" + "%n" + "Example:%n"
                            + "$ bacon pnc admin maintenance-mode activate \"Switching to maintenance mode for%n"
                            + "upcoming migration\"%n");
            assertThat(text).contains(expected);
        });

        String text2 = tapSystemErr(
                () -> assertEquals(
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
                                        "-h" })));
        assertThat(text2).containsOnlyOnce("Config file set from flag with profile foobar to");
    }
}
