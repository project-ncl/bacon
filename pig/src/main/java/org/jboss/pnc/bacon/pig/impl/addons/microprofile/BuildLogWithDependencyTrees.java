package org.jboss.pnc.bacon.pig.impl.addons.microprofile;

import com.google.common.collect.Sets;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BuildLogWithDependencyTrees {
    private static final Logger log = LoggerFactory.getLogger(BuildLogWithDependencyTrees.class);

    private static final Pattern dependencyTreeStart = Pattern
            .compile("^\\[INFO] --- maven-dependency-plugin:.*?:tree .*? @ (.*?) ---$");
    private static final Pattern dependencyTreeEnd = Pattern.compile("^\\[INFO][\\s-]*$");
    private static final Set<String> relevantScopes = Sets.newHashSet("compile", "runtime");

    public final Map<String, Set<GAV>> communityGavsForModules;

    public BuildLogWithDependencyTrees(List<String> buildLogLines) {
        communityGavsForModules = parseDependencyTreeInvocations(buildLogLines);
    }

    private static Map<String, Set<GAV>> parseDependencyTreeInvocations(List<String> buildLogLines) {
        Map<String, Set<GAV>> result = new HashMap<>();

        String currentModuleName = null;
        List<String> currentModuleDependencyTree = null;

        for (String line : buildLogLines) {
            Matcher matcher = dependencyTreeStart.matcher(line);
            if (matcher.matches()) {
                currentModuleName = matcher.group(1);
                currentModuleDependencyTree = new ArrayList<>();
                continue;
            }

            if (dependencyTreeEnd.matcher(line).matches() && currentModuleDependencyTree != null) {
                result.put(currentModuleName, communityGavsInDepTree(currentModuleDependencyTree));
                currentModuleName = null;
                currentModuleDependencyTree = null;
                continue;
            }

            if (currentModuleDependencyTree != null) {
                currentModuleDependencyTree.add(line);
            }
        }

        return result;
    }

    private static Set<GAV> communityGavsInDepTree(List<String> depTreeOutput) {
        return depTreeOutput.stream()
                .filter(l -> l.startsWith("[INFO] "))
                .map(BuildLogWithDependencyTrees::parseLineToGav)
                .filter(Objects::nonNull)
                .filter(GAV::isCommunity)
                .collect(Collectors.toSet());
    }

    private static GAV parseLineToGav(String mvnDepTreeLine) {
        String gavString = mvnDepTreeLine.replaceFirst("\\[INFO] [+|\\\\\\-\\s]+", "");
        String[] splitGav = gavString.split(":");
        if (splitGav.length < 5 || !relevantScopes.contains(splitGav[splitGav.length - 1])) {
            return null;
        }
        switch (splitGav.length) {
            case 5:
                return new GAV(splitGav[0], splitGav[1], splitGav[3], splitGav[2]);
            case 6:
                return new GAV(splitGav[0], splitGav[1], splitGav[4], splitGav[2], splitGav[3]);
            default:
                log.warn(
                        "suspicious line in the dependency tree '{}', assuming it's not a dependency and skipping",
                        gavString);
                return null;
        }
    }
}
