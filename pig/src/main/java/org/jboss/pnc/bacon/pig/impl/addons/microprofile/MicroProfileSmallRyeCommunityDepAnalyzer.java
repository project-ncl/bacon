package org.jboss.pnc.bacon.pig.impl.addons.microprofile;

import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.addons.runtime.CommunityDepAnalyzer;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Performs community dependency analysis on all builds (in "our" build group, that is). Assumes that each build
 * includes {@code mvn dependency:tree} in its build script. Community dependencies from modules whose names contain
 * {@code test} or {@code tck} are ignored. Only dependencies in the {@code compile} and {@code runtime} scopes are
 * included.
 */
public class MicroProfileSmallRyeCommunityDepAnalyzer extends AddOn {
    private static final Logger log = LoggerFactory.getLogger(MicroProfileSmallRyeCommunityDepAnalyzer.class);

    public static final String NAME = "microProfileSmallRyeCommunityDepAnalyzer";

    public MicroProfileSmallRyeCommunityDepAnalyzer(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    public void trigger() {
        log.info("Running MicroProfileSmallRyeCommunityDepAnalyzer");

        Set<GAV> allGavs = new HashSet<>();
        for (PncBuild build : builds.values()) {
            Set<GAV> gavs = new BuildLogWithDependencyTrees(build.getBuildLog()).communityGavsForModules.entrySet()
                    .stream()
                    .filter(e -> !e.getKey().contains("test") && !e.getKey().contains("tck"))
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toSet());

            Path targetPath = Paths.get(extrasPath, "community-dependencies-" + build.getName() + ".csv");
            new CommunityDepAnalyzer(gavs).generateAnalysis(targetPath.toAbsolutePath().toString());

            allGavs.addAll(gavs);
        }

        Path targetPath = Paths.get(extrasPath, "community-dependencies.csv");
        new CommunityDepAnalyzer(allGavs).generateAnalysis(targetPath.toAbsolutePath().toString());
    }
}
