package org.jboss.pnc.bacon.cli;

import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    @Test
    public void testHelp() throws Exception {
        App app = new App();
        String text = tapSystemOut(() -> assertEquals(0, app.run(new String[] { "-h" })));
        assertTrue(text.contains("Usage: bacon [<options>]"));
    }
}
