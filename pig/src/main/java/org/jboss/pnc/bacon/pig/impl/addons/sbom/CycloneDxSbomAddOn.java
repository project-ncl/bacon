package org.jboss.pnc.bacon.pig.impl.addons.sbom;

import java.nio.file.Files;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

/**
 * Generates an SBOM in CycloneDX format for the generated Maven repository ZIP.
 */
public class CycloneDxSbomAddOn extends AddOn {

    /**
     * Output file name
     */
    private static final String PARAM_OUTPUT_FILE = "outputFile";

    /**
     * CycloneDX schema version
     */
    private static final String PARAM_SCHEMA_VERSION = "schemaVersion";

    private static final String DEFAULT_OUTPUT_FILE = "extras/maven-repository-cyclonedx.json";

    public CycloneDxSbomAddOn(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    public String getName() {
        return "cycloneDxSbom";
    }

    @Override
    public void trigger() {
        clearProducedFiles();

        var repoPath = PigContext.get().getRepositoryData().getRepositoryPath();
        if (!Files.exists(repoPath)) {
            throw new IllegalArgumentException(repoPath + " does not exist");
        }

        new MavenRepoCdxSbomGenerator()
                .setOutputFileName(getOutputFile())
                .setSchemaVersion(getSchemaVersion())
                .addMavenRepository(repoPath)
                .generate();
    }

    private String getSchemaVersion() {
        var schemaVersion = getParameter(PARAM_SCHEMA_VERSION);
        return schemaVersion == null ? null : schemaVersion.toString();
    }

    private String getOutputFile() {
        var outputFile = getParameter(PARAM_OUTPUT_FILE);
        if (outputFile == null) {
            outputFile = DEFAULT_OUTPUT_FILE;
        }
        return outputFile.toString();
    }

    private Object getParameter(String name) {
        Map<String, ?> config = getAddOnConfiguration();
        return config == null ? null : config.get(name);
    }
}
