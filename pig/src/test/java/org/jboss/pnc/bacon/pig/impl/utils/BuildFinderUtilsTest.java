package org.jboss.pnc.bacon.pig.impl.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.build.finder.core.BuildConfig;
import org.junit.jupiter.api.Test;

class BuildFinderUtilsTest {
    @Test
    void getKojiBuildFinderConfig() {
        File configFile = new File(
                new File(BuildFinderUtilsTest.class.getResource("/").getFile()).getParentFile()
                        .getParentFile()
                        .getParentFile(),
                "config.yaml");
        Config.configure(configFile.getParent(), configFile.getName(), "default");
        Config.instance().getActiveProfile().getPig().setKojiHubUrl("https://127.0.0.1");

        BuildConfig bc = BuildFinderUtils.getKojiBuildFinderConfig();
        assertTrue(bc.getKojiHubURL().toString().contains("127.0.0.1"));
    }
}
