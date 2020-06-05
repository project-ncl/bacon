package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.util.Map;
import java.util.Optional;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com 2020-06-05
 */
public class ArtifactVersion {
    public static String prefix = "artifactVersion://";

    private ArtifactVersion() {
    }

    /*
     * gets artifact coordinates from a location of a form artifactVersion://groupId:artifactId[@buildName] and gets a
     * version of the artifact from the build
     */
    public static String get(String location, String defaultBuildName, Map<String, PncBuild> builds) {
        if (!location.startsWith(ArtifactVersion.prefix)) {
            throw new RuntimeException("location: " + location + " is not a proper artifactVersion location");
        }
        String value = location.substring(prefix.length());

        String[] split = value.split("@");

        String gaString = split[0];
        String[] gaSplit = gaString.split(":");
        if (gaSplit.length != 2) {
            throw new RuntimeException(
                    "Expected artifactVersion://groupId:artifactId... as the version locator, got: " + value);
        }

        String groupId = gaSplit[0];
        String artifactId = gaSplit[1];

        String buildName = split.length > 1 ? split[1] : defaultBuildName;

        PncBuild pncBuild = builds.get(buildName);
        if (pncBuild == null) {
            throw new RuntimeException("Build " + buildName + " not found among the builds");
        }

        Optional<ArtifactWrapper> maybeArtifact = pncBuild.getBuiltArtifacts().stream().filter(a -> {
            GAV gav = a.toGAV();
            return gav.getArtifactId().equals(artifactId) && gav.getGroupId().equals(groupId);
        }).findAny();

        return maybeArtifact
                .orElseThrow(
                        () -> new RuntimeException(
                                "Unable to find artifact matching " + groupId + ":" + artifactId
                                        + " in artifacts produced by " + buildName))
                .toGAV()
                .getVersion();
    }
}
