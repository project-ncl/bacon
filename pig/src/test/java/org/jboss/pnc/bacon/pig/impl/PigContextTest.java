package org.jboss.pnc.bacon.pig.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PigContextTest {

    @Test
    void testPrefixGeneration() {
        Path resourcesFolderPath = Paths.get("src", "test", "resources");
        PigContext pigContext = new PigContext();
        pigContext.initConfig(resourcesFolderPath, "targetPath", Optional.empty(), null);
        assertTrue(
                pigContext.getPrefix()
                        .startsWith(pigContext.getPigConfiguration().getOutputPrefixes().getReleaseFile()));
        assertTrue(pigContext.getPrefix().endsWith(pigContext.getPigConfiguration().getOutputSuffix()));
        assertTrue(pigContext.getPrefix().contains(pigContext.getPigConfiguration().getVersion()));
    }
}
