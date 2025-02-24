package org.jboss.pnc.bacon.pig.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class PigContextTest {

    @Test
    void testPrefixGeneration() {
        Path resourcesFolderPath = Paths.get("src", "test", "resources");
        PigContext pigContext = new PigContext();
        pigContext.initConfig(resourcesFolderPath, "targetPath", null, null);
        pigContext.initFullVersion(false);
        pigContext.configureTargetDirectories();
        assertTrue(
                pigContext.getPrefix()
                        .startsWith(pigContext.getPigConfiguration().getOutputPrefixes().getReleaseFile()));
        assertTrue(pigContext.getPrefix().endsWith(pigContext.getPigConfiguration().getOutputSuffix()));
        assertTrue(pigContext.getPrefix().contains(pigContext.getPigConfiguration().getVersion()));
    }
}
