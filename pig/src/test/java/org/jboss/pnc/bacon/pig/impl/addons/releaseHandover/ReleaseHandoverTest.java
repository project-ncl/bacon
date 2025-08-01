package org.jboss.pnc.bacon.pig.impl.addons.releaseHandover;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.DocumentTemplate;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.AdvisoryContent;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.AdvisoryRHxA;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.AdvisoryRHxAType;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandoverAddon;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandoverGenerator;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;

public class ReleaseHandoverTest {

    @Test
    public void shouldParseConfig() {
        DocumentTemplate documentTemplate = getDocumentTemplate();
        var advisory0 = documentTemplate.advisory().get(0);
        assertThat(advisory0.isRHxA()).isFalse();
        var contentAdvisory = (AdvisoryContent) advisory0;
        assertThat(contentAdvisory.id()).isEqualTo("RH12345");

        var advisory1 = documentTemplate.advisory().get(1);
        assertThat(advisory1.isRHxA()).isTrue();
        var rhxaAdvisory = (AdvisoryRHxA) advisory1;
        assertThat(rhxaAdvisory.type()).isEqualTo(AdvisoryRHxAType.RHSA);
    }

    @Test
    public void shouldGenerateHandoverDocument() {
        var template = getDocumentTemplate();
        var generator = new ReleaseHandoverGenerator(template);

        var yaml = generator.generateYaml(
                List.of("./src/test/resources/release-handover-config.yaml"),
                List.of("./src/test/resources/release-handover-config.yaml"),
                List.of("./src/test/resources/release-handover-config.yaml"));

        assertThat(yaml).isNotNull();
        System.out.println("yaml:\n" + yaml);
    }

    private DocumentTemplate getDocumentTemplate() {
        PigConfiguration pigConfiguration = loadConfig("/release-handover-config.yaml");
        var config = pigConfiguration.getAddons().get(ReleaseHandoverAddon.ADDON_NAME);
        @SuppressWarnings("unchecked")
        DocumentTemplate documentTemplate = DocumentTemplate
                .fromMap((Map<String, ?>) config.get(ReleaseHandoverAddon.CONFIG_PARAM_DOCUMENT));
        return documentTemplate;
    }

    private PigConfiguration loadConfig(String configFile) {
        InputStream configStream = PigConfiguration.class.getResourceAsStream(configFile);
        var variableOverrides = Map.of(
                "productVersion",
                "3.20",
                "platformScmRevision",
                "main");
        return PigConfiguration.load(configStream, variableOverrides);
    }

}
