package org.jboss.pnc.bacon.cli;

import org.junit.jupiter.api.Test;

import java.io.File;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    @Test
    void testHelp() throws Exception {
        App app = new App();
        app.run(new String[] { "-h" });
        String text = tapSystemOut(() -> assertEquals(0, app.run(new String[] { "-h" })));
        assertTrue(text.contains("Usage: bacon [-hovV] [-p=<configurationFileLocation>] [--profile=<profile>]"));
    }

    @Test
    void testExampleUsage() throws Exception {
        App app = new App();
        String text = tapSystemOut(
                () -> assertEquals(
                        0,
                        app.run(new String[] { "-v", "pnc", "admin", "maintenance-mode", "activate", "-h" })));
        assertTrue(text.contains("bacon pnc admin maintenance-mode activate \"Switching"));
    }

    @Test
    void testConfigPath() throws Exception {
        String text = tapSystemErr(
                () -> assertEquals(
                        0,
                        new App().run(new String[] { "-p", "/tmp/123456789", "-v", "pnc", "-o", "build", "-h" })));
        assertTrue(text.contains("Config file set from flag to /tmp/123456789/config.yaml"));
    }

    @Test
    void testJSONEnabled1() throws Exception {
        File pncClasses = new File(App.class.getClassLoader().getResource("").getFile());
        File configYaml = new File(
                pncClasses.getParentFile().getParentFile().getParentFile(),
                "pnc" + File.separator + "target" + File.separator + "test-classes");

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
        File configYaml = new File(
                pncClasses.getParentFile().getParentFile().getParentFile(),
                "pnc" + File.separator + "target" + File.separator + "test-classes");

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
        File configYaml = new File(
                pncClasses.getParentFile().getParentFile().getParentFile(),
                "pnc" + File.separator + "target" + File.separator + "test-classes");

        String text = tapSystemErr(
                () -> assertEquals(
                        1,
                        new App().run(new String[] { "-p", configYaml.toString(), "-v", "pnc", "build", "get", "0" })));
        assertTrue(text.contains("JSON command is enabled: false"));
    }
}
