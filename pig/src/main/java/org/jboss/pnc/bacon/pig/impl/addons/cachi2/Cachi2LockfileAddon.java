package org.jboss.pnc.bacon.pig.impl.addons.cachi2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

/**
 * An add-on that generates Cachi2 lock files.
 */
public class Cachi2LockfileAddon extends AddOn {

    /**
     * Output file name
     */
    private static final String PARAM_FILENAME = "filename";

    /**
     * Default repository URL for artifacts not recognized by PNC
     */
    private static final String PARAM_DEFAULT_REPO_URL = "default-repository-url";

    /**
     * Preferred checksum algorithm to record in the generated lock file
     */
    private static final String PARAM_PREFERRED_CHECKSUM_ALG = "preferred-checksum-alg";

    public Cachi2LockfileAddon(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    public String getName() {
        return "cachi2LockFile";
    }

    @Override
    public void trigger() {
        clearProducedFiles();

        var repoPath = PigContext.get().getRepositoryData().getRepositoryPath();
        if (!Files.exists(repoPath)) {
            throw new IllegalArgumentException(repoPath + " does not exist");
        }

        Cachi2LockfileGenerator cachi2Lockfile = Cachi2LockfileGenerator.newInstance()
                .setOutputDirectory(Path.of(extrasPath))
                .addMavenRepository(repoPath);

        setParams(cachi2Lockfile);

        cachi2Lockfile.generate();
    }

    /**
     * Set configured parameters on the generator.
     *
     * @param cachi2Lockfile lock file generator
     */
    private void setParams(Cachi2LockfileGenerator cachi2Lockfile) {
        var params = getAddOnConfiguration();
        if (params != null) {
            setFilename(cachi2Lockfile, params);
            setDefaultRepositoryUrl(cachi2Lockfile, params);
            setPreferredChecksumAlg(cachi2Lockfile, params);
        }
    }

    private void setFilename(Cachi2LockfileGenerator cachi2Lockfile, Map<String, ?> params) {
        var value = params.get(PARAM_FILENAME);
        if (value != null) {
            cachi2Lockfile.setOutputFileName(value.toString());
            recordProducedFile(Path.of(value.toString()));
        }
    }

    private void setDefaultRepositoryUrl(Cachi2LockfileGenerator cachi2Lockfile, Map<String, ?> params) {
        var value = params.get(PARAM_DEFAULT_REPO_URL);
        if (value != null) {
            cachi2Lockfile.setDefaultMavenRepositoryUrl(value.toString());
        }
    }

    private void setPreferredChecksumAlg(Cachi2LockfileGenerator cachi2Lockfile, Map<String, ?> params) {
        var value = params.get(PARAM_PREFERRED_CHECKSUM_ALG);
        if (value != null) {
            cachi2Lockfile.setPreferredChecksumAlg(value.toString());
        }
    }
}
