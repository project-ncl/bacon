package org.jboss.pnc.bacon.cli;

import ch.qos.logback.classic.Level;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    private static final String PNC_TEST_CLASSES = FilenameUtils.separatorsToSystem("pnc/target/test-classes");

    @Test
    void testHelp() throws Exception {
        App app = new App();
        restoreSystemProperties(() -> {
            System.setProperty("picocli.ansi", "false");
            app.run(new String[] { "-h" });
            String text = tapSystemOut(() -> assertEquals(0, app.run(new String[] { "-h" })));
            assertTrue(text.contains("Usage: bacon [-hovV] [-p=<configurationFileLocation>] [--profile=<profile>]"));
        });
    }

    @Test
    void testVerbose() throws Exception {
        ObjectHelper.setRootLoggingLevel(Level.INFO);
        ObjectHelper.setLoggingLevel("org.jboss.pnc.client", Level.WARN);

        String text = tapSystemErr(() -> assertEquals(0, new App().run(new String[] { "pnc", "admin", "-h" })));
        assertFalse(text.contains("Log level set to DEBUG"));
        text = tapSystemErr(() -> assertEquals(0, new App().run(new String[] { "pnc", "-v", "-h" })));
        assertTrue(text.contains("Log level set to DEBUG"));
    }

    @Test
    void testExampleUsage() throws Exception {
        App app = new App();
        String text = tapSystemOut(
                () -> assertEquals(
                        0,
                        app.run(new String[] { "--verbose", "pnc", "admin", "maintenance-mode", "activate", "-h" })));
        assertTrue(text.contains("bacon pnc admin maintenance-mode activate \"Switching"));
    }

    @Test
    void testConfigPath() throws Exception {
        String configPath = FilenameUtils.separatorsToSystem("/tmp/123456789");
        String configFile = FilenameUtils.separatorsToSystem(configPath + "/config.yaml");
        String text = tapSystemErr(
                () -> assertEquals(
                        0,
                        new App().run(new String[] { "-p", configPath, "-v", "pnc", "-o", "build", "-h" })));
        assertTrue(text.contains("Config file set from flag with profile default to " + configFile));
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
        assertTrue(text.contains("JSON command is enabled: true"));
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
        assertTrue(text.contains("JSON command is enabled: true"));
    }

    @Test
    void testJSONDisabled() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);

        String text = tapSystemErr(
                () -> assertEquals(
                        1,
                        new App().run(new String[] { "-p", configYaml.toString(), "-v", "pnc", "build", "get", "0" })));
        assertTrue(text.contains("JSON command is enabled: false"));
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

        assertTrue(text.contains("Keycloak authentication failed!"));
        assertFalse(text.contains("at org.jboss.pnc.bacon.pnc.client.PncClientHelper.getBearerToken"));
        assertFalse(text.contains("Unknown option: '--tempBuild'"));

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
        assertTrue(text.contains("Keycloak authentication failed!"));
        assertTrue(text.contains("at org.jboss.pnc.bacon.pnc.client.PncClientHelper.getBearerToken"));
        assertFalse(text.contains("Unknown option: '--tempBuild'"));
    }

    @Test
    void testProfile() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(pncClasses.getParentFile().getParentFile().getParentFile(), PNC_TEST_CLASSES);
        App app = new App();

        restoreSystemProperties(() -> {
            System.setProperty("picocli.ansi", "false");
            String text = tapSystemOutNormalized(
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

            assertEquals(
                    "Usage: bacon pnc admin maintenance-mode activate [-hov]\n"
                            + "       [-p=<configurationFileLocation>] [--profile=<profile>] <reason>\n"
                            + "This will disable any new builds from being accepted\n"
                            + "      <reason>              Reason\n"
                            + "  -h, --help                display this help message\n"
                            + "  -o                        use json for output (default to yaml)\n"
                            + "  -p, --configPath=<configurationFileLocation>\n"
                            + "                            Path to PNC configuration folder\n"
                            + "      --profile=<profile>   PNC Configuration profile\n"
                            + "  -v, --verbose             Verbose output\n" + "\n" + "Example:\n"
                            + "$ bacon pnc admin maintenance-mode activate \"Switching to maintenance mode for\n"
                            + "upcoming migration\"\n",
                    text);
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
        assertEquals(StringUtils.countMatches(text2, "Config file set from flag with profile foobar to"), 1);
    }
}
