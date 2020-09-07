package org.jboss.pnc.bacon.cli;

import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    @Test
    void testHelp() throws Exception {
        App app = new App();
        app.run(new String[] { "-h" });
        String text = tapSystemOut(() -> assertEquals(0, app.run(new String[] { "-h" })));
        assertTrue(text.contains("Usage: bacon [-hvV] [-p=<configurationFileLocation>] [--profile=<profile>]"));
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
}
