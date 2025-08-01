package org.jboss.pnc.bacon.pig.impl.addons.releaseHandover;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.DocumentTemplate;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandoverAddon;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;


public class ReleaseHandoverTest {

    @SuppressWarnings("unchecked")
    @Test
    public void shouldParseConfig() {
        PigConfiguration pigConfiguration = loadConfig();
        var config = pigConfiguration.getAddons().get(ReleaseHandoverAddon.ADDON_NAME);
        DocumentTemplate documentTemplate = DocumentTemplate
                .fromMap((Map<String, ?>)config.get(ReleaseHandoverAddon.CONFIG_PARAM_DOCUMENT));
        
        assertThat(documentTemplate).isNotNull();
    }

    private PigConfiguration loadConfig() {
        InputStream configStream = PigConfiguration.class.getResourceAsStream("/release-handover-config.yaml");
        var variableOverrides = Map.of(
                "productVersion",
                "3.20",
                "platformScmRevision",
                "main");
        return PigConfiguration.load(configStream, variableOverrides);
    }

}
