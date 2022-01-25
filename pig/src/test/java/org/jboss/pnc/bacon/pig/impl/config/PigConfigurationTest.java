package org.jboss.pnc.bacon.pig.impl.config;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PigConfigurationTest {

    private PigConfiguration generated;

    @BeforeEach
    void setup() {
        EasyRandom easyRandom = new EasyRandom();
        generated = new EasyRandom().nextObject(PigConfiguration.class);
    }

    @Test
    void testSuffixSetInTopLevelDirectory() {
        String suffix = "laboheme";
        generated.setOutputSuffix(suffix);
        assertTrue(generated.getTopLevelDirectoryPrefix().endsWith("-" + suffix + "-"));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getVersion()));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getProduct().getStage()));

        generated.setOutputSuffix("");
        assertFalse(generated.getTopLevelDirectoryPrefix().endsWith("-" + suffix + "-"));
        // make sure we don't accidentally put 2 '-' if suffix is empty
        assertFalse(generated.getTopLevelDirectoryPrefix().endsWith("--"));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getVersion()));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getProduct().getStage()));

        generated.setOutputSuffix(null);
        assertFalse(generated.getTopLevelDirectoryPrefix().endsWith("-" + suffix + "-"));
        // make sure we don't accidentally put 2 '-' if suffix is empty
        assertFalse(generated.getTopLevelDirectoryPrefix().endsWith("--"));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getVersion()));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getProduct().getStage()));
    }

    @Test
    void testTopLevelDirectoryGeneration() {
        assertTrue(generated.getTopLevelDirectoryPrefix().startsWith(generated.getOutputPrefixes().getReleaseDir()));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getVersion()));
        assertTrue(generated.getTopLevelDirectoryPrefix().contains(generated.getProduct().getStage()));
    }
}
