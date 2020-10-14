package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class QuarkusPostBuildAnalyzerTest {
    @Test
    public void analyzePostBuildDeps() {
        QuarkusPostBuildAnalyzer quarkusPostBuildAnalyzer = new QuarkusPostBuildAnalyzer(
                preConfig(),
                null,
                "/home/hamadhan/builds/quarkus/",
                "/home/hamadhan/builds/quarkus/extras");
        quarkusPostBuildAnalyzer.trigger();
    }

    private PigConfiguration preConfig() {
        PigConfiguration config = new PigConfiguration();
        HashMap<String, Map<String, ?>> addons = new HashMap<>();
        HashMap<String, Object> addOnConfig = new HashMap<>();
        addOnConfig.put("stagingPath", "http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/");
        addOnConfig.put("productName", "quarkus");
        addons.put(QuarkusPostBuildAnalyzer.NAME, addOnConfig);
        config.setAddons(addons);
        return config;
    }
}
