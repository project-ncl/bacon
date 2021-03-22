package org.jboss.pnc.bacon.pig.impl.addons.rhba;

import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.enums.RepositoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates the offline manifest text file, which contains all the dependencies, including third party, non-RH
 * dependencies. First, it adds the build artifacts for all the builds. Then, it executes a query to PNC, requesting the
 * third party dependencies for each build. Finally, the method generates a file, containing version paths and
 * checksums, where we can get it, for all build and dependency attributes. The offline manifest file in combination
 * with the offliner tool (https://repo1.maven.org/maven2/com/redhat/red/offliner/offliner) are used to download all the
 * attributes to a local maven repository.
 */
public class OfflineManifestGenerator extends AddOn {

    private static final String ADDON_NAME = "offlineManifestGenerator";

    private static final String OFFLINE_MANIFEST_DEFAULT_NAME = "offliner.txt";

    private static final Logger log = LoggerFactory.getLogger(OfflineManifestGenerator.class);

    private BuildInfoCollector buildInfoCollector;

    public OfflineManifestGenerator(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    /**
     * Use only for unit test purposes.
     *
     * @param pigConfiguration
     * @param builds
     * @param releasePath
     * @param extrasPath
     * @param buildInfoCollector
     * @deprecated
     */
    @Deprecated
    public OfflineManifestGenerator(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath,
            BuildInfoCollector buildInfoCollector) {
        super(pigConfiguration, builds, releasePath, extrasPath);
        this.buildInfoCollector = buildInfoCollector;
    }

    public String getName() {
        return ADDON_NAME;
    }

    public void trigger() {
        log.info("Will generate the offline manifest");
        if (buildInfoCollector == null) {
            buildInfoCollector = new BuildInfoCollector();
        }
        List<ArtifactWrapper> artifactsToListRaw = new ArrayList<>();
        for (PncBuild build : builds.values()) {
            artifactsToListRaw.addAll(build.getBuiltArtifacts());
            // TODO: Add filter, basing on the targetRepository.repositoryType, when NCL-6079 is done
            buildInfoCollector.addDependencies(build, "");
            if (build.getDependencyArtifacts() != null) {
                artifactsToListRaw.addAll(build.getDependencyArtifacts());
            }
        }
        log.debug("Number of collected artifacts for the Offline manifest: {}", artifactsToListRaw.size());

        List<String> exclusions = pigConfiguration.getFlow().getRepositoryGeneration().getExcludeArtifacts();
        artifactsToListRaw.removeIf(artifact -> {
            for (String exclusion : exclusions) {
                if (Pattern.matches(exclusion, artifact.getGapv())) {
                    return true;
                }
            }
            return false;
        });

        log.debug("Number of collected artifacts after exclusion: {}", artifactsToListRaw.size());

        List<ArtifactWrapper> artifactsToList = artifactsToListRaw.stream().distinct().collect(Collectors.toList());

        log.debug("Number of collected artifacts without duplicates: {}", artifactsToList.size());

        String offlineManifestFileName;
        if (getAddOnConfiguration() != null) {
            offlineManifestFileName = Optional.of((String) getAddOnConfiguration().get("offlineManifestFileName"))
                    .orElse(OFFLINE_MANIFEST_DEFAULT_NAME);
        } else {
            offlineManifestFileName = OFFLINE_MANIFEST_DEFAULT_NAME;
        }

        try (PrintWriter file = new PrintWriter(releasePath + offlineManifestFileName, StandardCharsets.UTF_8.name())) {
            for (ArtifactWrapper artifact : artifactsToList) {
                // TODO: Remove the check, when NCL-6079 is done
                if (artifact.getRepositoryType() == RepositoryType.MAVEN) {
                    GAV gav = artifact.toGAV();
                    String offlinerString = String
                            .format("%s,%s/%s", artifact.getSha256(), gav.toVersionPath(), gav.toFileName());
                    file.println(offlinerString);
                }

            }

            List<GAV> extraGavs = pigConfiguration.getFlow()
                    .getRepositoryGeneration()
                    .getExternalAdditionalArtifacts()
                    .stream()
                    .map(GAV::fromColonSeparatedGAPV)
                    .collect(Collectors.toList());
            for (GAV extraGav : extraGavs) {
                String offlinerString = String.format("%s/%s", extraGav.toVersionPath(), extraGav.toFileName());
                file.println(offlinerString);
            }

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new FatalException("Failed to generate the offline manifest", e.getMessage());
        }
    }
}
