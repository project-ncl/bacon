package org.jboss.pnc.bacon.pig.impl.addons.releasehandover;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

public class ReleaseHandoverAddon extends AddOn {

    public static final String ADDON_NAME = "releaseHandover";
    public static final String CONFIG_PARAM_DOCUMENT = "document";

    public ReleaseHandoverAddon(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    public String getName() {
        return ADDON_NAME;
    }

    @Override
    public void trigger() {
        DocumentTemplate documentTemplate = null;
        var config = getAddOnConfiguration();
        if (config != null) {
            documentTemplate = DocumentTemplate.fromMap(uncheckedCast(config));
        }

        if (documentTemplate == null) {
            throw new RuntimeException("No document config specified for release-handover addon.");
        }

        PigContext pigContext = PigContext.get();
        var licenseZipPath = pigContext.getDeliverables().getLicenseZipName();
        var repoPath = pigContext.getRepositoryData().getRepositoryPath().toString();
        var sourceZipPath = pigContext.getDeliverables().getSourceZipName();

        var generator = new ReleaseHandoverGenerator(documentTemplate);
        var yamlString = generator.generateYaml(List.of(licenseZipPath), List.of(repoPath), List.of(sourceZipPath));

        // write yaml to file
        var file = Path.of("release-handover.yaml");
        try {
            Files.write(file, yamlString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write release-handover.yaml", e);
        }

    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> uncheckedCast(Map<String, ?> config) {
        return (Map<String, ?>) config.get(ReleaseHandoverAddon.CONFIG_PARAM_DOCUMENT);
    }

}
