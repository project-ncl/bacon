package org.jboss.pnc.bacon.pig.impl.addons.rhba;

import static org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy.BUILD_GROUP;
import static org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy.IGNORE;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.enums.RepositoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        log.info("Generating the Offliner manifest");
        if (buildInfoCollector == null) {
            buildInfoCollector = new BuildInfoCollector();
        }

        HashSet<String> artifacts = new HashSet<>();
        for (PncBuild build : sourceBuilds()) {
            Collection<String> builtArtifacts = filterExcludedArtifactsAndFormat(build.getBuiltArtifacts());
            log.debug("Collected {} built artifacts for build {}", builtArtifacts.size(), build.getName());
            artifacts.addAll(builtArtifacts);
            buildInfoCollector.addDependencies(build, "targetRepository.repositoryType==" + RepositoryType.MAVEN);
            if (build.getDependencyArtifacts() != null) {
                Collection<String> dependencies = filterExcludedArtifactsAndFormat(build.getDependencyArtifacts());
                log.debug("Collected {} dependencies for build {}", dependencies.size(), build.getName());
                artifacts.addAll(dependencies);
            }
        }

        try (PrintWriter file = new PrintWriter(
                releasePath + offlinerManifestFileName(),
                StandardCharsets.UTF_8.name())) {
            artifacts.forEach(file::println);
            pigConfiguration.getFlow()
                    .getRepositoryGeneration()
                    .getExternalAdditionalArtifacts()
                    .stream()
                    .map(GAV::fromColonSeparatedGAPV)
                    .map(extraGav -> String.format("%s/%s", extraGav.toVersionPath(), extraGav.toFileName()))
                    .forEach(file::println);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new FatalException("Failed to generate the Offliner manifest", e);
        }
    }

    private Collection<String> filterExcludedArtifactsAndFormat(Collection<ArtifactWrapper> builtArtifacts) {
        return builtArtifacts.stream()
                .filter(artifact -> !this.isArtifactExcluded(artifact))
                .map(
                        artifact -> String.format(
                                "%s,%s/%s",
                                artifact.getSha256(),
                                artifact.toGAV().toVersionPath(),
                                artifact.toGAV().toFileName()))
                .collect(Collectors.toList());
    }

    private String offlinerManifestFileName() {
        if (getAddOnConfiguration() == null || getAddOnConfiguration().get("offlineManifestFileName") == null) {
            return OFFLINE_MANIFEST_DEFAULT_NAME;
        } else {
            return (String) getAddOnConfiguration().get("offlineManifestFileName");
        }
    }

    private Collection<PncBuild> sourceBuilds() {
        RepoGenerationData generationData = pigConfiguration.getFlow().getRepositoryGeneration();
        RepoGenerationStrategy strategy = generationData.getStrategy();
        if (!Arrays.asList(IGNORE, BUILD_GROUP).contains(strategy)) {
            return builds.values();
        }

        List<String> excludeSourceBuilds = generationData.getExcludeSourceBuilds();
        return builds.values()
                .stream()
                .filter(build -> !excludeSourceBuilds.contains(build.getName()))
                .collect(Collectors.toList());
    }

    private boolean isArtifactExcluded(ArtifactWrapper artifact) {
        return pigConfiguration.getFlow()
                .getRepositoryGeneration()
                .getExcludeArtifacts()
                .stream()
                .anyMatch(exclusion -> Pattern.matches(exclusion, artifact.getGapv()));
    }
}
