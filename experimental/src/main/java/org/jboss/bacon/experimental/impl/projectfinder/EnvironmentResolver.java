package org.jboss.bacon.experimental.impl.projectfinder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jboss.bacon.experimental.impl.config.BuildConfigGeneratorConfig;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.EnvironmentClient;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Environment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnvironmentResolver {

    private static final int DEFAULT_MAVEN_MAJOR = 3;
    private static final Pattern OPENJDK_IN_NAME = Pattern.compile("(?i)OpenJDK");

    private final Map<String, Environment> environments = new HashMap<>();
    private final BuildConfigGeneratorConfig config;

    public EnvironmentResolver(BuildConfigGeneratorConfig config) {
        this.config = config;
        try {
            EnvironmentClient environmentClient = new ClientCreator<>(EnvironmentClient::new).newClient();
            RemoteCollection<Environment> all = environmentClient.getAll(Optional.empty(), Optional.empty());
            for (Environment env : all) {
                environments.put(env.getId(), env);
            }
            validateDefaultEnvironment(config.getDefaultValues().getEnvironmentName());
        } catch (RemoteResourceException e) {
            throw new FatalException("Failed to load PNC environment list.", e);
        }
    }

    EnvironmentResolver(Map<String, Environment> environments, BuildConfigGeneratorConfig config) {
        this.config = config;
        this.environments.putAll(environments);
    }

    private void validateDefaultEnvironment(String defaultEnv) {
        Environment byName = null;
        for (Environment env : environments.values()) {
            if (env.getName().equals(defaultEnv)) {
                if (!env.isDeprecated()) {
                    return;
                }
                byName = env;
            }
        }
        if (byName == null) {
            throw new FatalException(
                    "Could not find environment \"" + defaultEnv
                            + "\" in PNC, update default environment value in config.");
        }
        Environment replacement = resolve(byName);
        String suggestion = replacement.isDeprecated()
                ? " (deprecation path did not lead to an active environment)"
                : " Suggested replacement: \"" + replacement.getName() + "\"";
        throw new FatalException(
                "Default environment \"" + defaultEnv
                        + "\" is deprecated, update default environment value in config." + suggestion);
    }

    public Environment resolve(Environment env) {
        if (!env.isDeprecated()) {
            return env;
        }
        String replacementID = env.getAttributes().get("DEPRECATION_REPLACEMENT");// TODO: replace with constant from
                                                                                  // 2.5
        if (replacementID == null) {
            log.error(
                    "Environment " + env.getName() + " #" + env.getId()
                            + " is deprecated, but does not have a DEPRECATION_REPLACEMENT provided.");// TODO: replace
                                                                                                                                                                     // with constant
                                                                                                                                                                     // from 2.5
            return env;
        }
        Environment replacement = environments.get(replacementID);
        if (replacement == null) {
            log.error(
                    "Environment " + env.getName() + " #" + env.getId()
                            + " is deprecated, but DEPRECATION_REPLACEMENT points to invalid Environment #"
                            + replacementID + ".");// TODO: replace with constant from 2.5
            return env;
        }
        return resolve(replacement);
    }

    public Environment selectEnvironment(ProjectBuildInfo buildInfo) {
        return selectEnvironment(buildInfo, null);
    }

    /**
     * Selects the least disruptive compatible environment. An active existing environment is retained when it still
     * satisfies the detected hard requirement and is not older than the preferred wrapper version.
     */
    public Environment selectEnvironment(ProjectBuildInfo buildInfo, Environment existingEnvironment) {
        List<Environment> candidates = environments.values()
                .stream()
                .filter(env -> !env.isDeprecated())
                .filter(env -> !env.isHidden())
                .filter(env -> matchesJdk(env, buildInfo.getJdkVersion()))
                .filter(env -> matchesBuildType(env, buildInfo.getBuildType()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            log.warn(
                    "No environment matches detected build info (JDK={}, type={}). Using default environment.",
                    buildInfo.getJdkVersion(),
                    buildInfo.getBuildType());
            return findByName(config.getDefaultValues().getEnvironmentName());
        }

        String attrKey = toolAttribute(buildInfo.getBuildType());
        String requiredRange = buildInfo.getBuildToolVersionRange();
        List<Environment> compatible = candidates;
        boolean requiredRangeSatisfied = true;
        if (hasText(requiredRange)) {
            compatible = environmentsSatisfyingRange(candidates, attrKey, requiredRange);
            requiredRangeSatisfied = !compatible.isEmpty();
            if (!requiredRangeSatisfied) {
                compatible = candidates;
                log.warn(
                        "No environment satisfies required {} version range {}. Selecting the highest available compatible environment.",
                        attrKey,
                        requiredRange);
            }
        }

        if (existingEnvironment != null
                && containsEnvironment(candidates, existingEnvironment)
                && (!requiredRangeSatisfied || containsEnvironment(compatible, existingEnvironment))
                && satisfiesPreferredVersion(existingEnvironment, attrKey, buildInfo.getBuildToolVersion())) {
            log.info(
                    "Keeping compatible existing environment '{}' for build info (JDK={}, type={}, preferredVersion={}, requiredRange={})",
                    existingEnvironment.getName(),
                    buildInfo.getJdkVersion(),
                    buildInfo.getBuildType(),
                    buildInfo.getBuildToolVersion(),
                    requiredRange);
            return existingEnvironment;
        }

        if (existingEnvironment != null) {
            compatible = avoidToolDowngrade(compatible, attrKey, existingEnvironment);
        }

        Environment selected = requiredRangeSatisfied
                ? selectBestEnvironment(compatible, buildInfo, attrKey)
                : selectHighestVersion(compatible, attrKey);
        log.info(
                "Selected environment '{}' for build info (JDK={}, type={}, preferredVersion={}, requiredRange={})",
                selected.getName(),
                buildInfo.getJdkVersion(),
                buildInfo.getBuildType(),
                buildInfo.getBuildToolVersion(),
                requiredRange);
        return selected;
    }

    private Environment selectBestEnvironment(
            List<Environment> candidates,
            ProjectBuildInfo buildInfo,
            String attrKey) {
        if (hasText(buildInfo.getBuildToolVersion())) {
            return selectPreferredVersion(candidates, attrKey, buildInfo.getBuildToolVersion());
        }

        List<Environment> compatible = buildInfo.getBuildType() == BuildType.MVN
                ? preferDefaultMavenMajor(candidates)
                : candidates;
        compatible = preferSimpleEnvironments(compatible, buildInfo.getBuildType());
        return selectLowestVersion(compatible, attrKey);
    }

    private List<Environment> environmentsSatisfyingRange(
            List<Environment> candidates,
            String attrKey,
            String requestedRange) {
        try {
            VersionRange range = VersionRange.createFromVersionSpec(normalizeRequiredRange(requestedRange));
            return candidates.stream()
                    .filter(env -> range.containsVersion(new DefaultArtifactVersion(toolVersion(env, attrKey))))
                    .collect(Collectors.toList());
        } catch (InvalidVersionSpecificationException e) {
            log.warn("Could not parse required {} version range '{}': {}", attrKey, requestedRange, e.getMessage());
            return List.of();
        }
    }

    private List<Environment> preferSimpleEnvironments(List<Environment> candidates, BuildType buildType) {
        List<Environment> simple = candidates.stream()
                .filter(environment -> !isBundledEnvironment(environment, buildType))
                .collect(Collectors.toList());
        if (!simple.isEmpty()) {
            return simple;
        }
        log.warn(
                "No simple {} environment is available for the detected JDK; bundled environments remain eligible.",
                buildType);
        return candidates;
    }

    private List<Environment> avoidToolDowngrade(
            List<Environment> candidates,
            String attrKey,
            Environment existingEnvironment) {
        String existingVersion = toolVersion(existingEnvironment, attrKey);
        if ("0".equals(existingVersion)) {
            return candidates;
        }

        ComparableVersion floor = new ComparableVersion(existingVersion);
        List<Environment> notOlder = candidates.stream()
                .filter(environment -> new ComparableVersion(toolVersion(environment, attrKey)).compareTo(floor) >= 0)
                .collect(Collectors.toList());
        if (!notOlder.isEmpty()) {
            return notOlder;
        }

        log.warn(
                "No active {} environment is at least as new as existing version {}. Keeping older candidates only as a last resort.",
                attrKey,
                existingVersion);
        return candidates;
    }

    private boolean satisfiesPreferredVersion(
            Environment environment,
            String attrKey,
            String preferredVersion) {
        if (!hasText(preferredVersion)) {
            return true;
        }
        int[] preferred = parseVersion(preferredVersion);
        int[] existing = parseVersion(toolVersion(environment, attrKey));
        return preferred[0] == existing[0] && compareVersions(existing, preferred) >= 0;
    }

    private String normalizeRequiredRange(String requestedRange) {
        String trimmed = requestedRange.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("(")) {
            return trimmed;
        }
        return "[" + trimmed + ",)";
    }

    private Environment selectPreferredVersion(
            List<Environment> candidates,
            String attrKey,
            String requestedVersion) {
        int[] requested = parseVersion(requestedVersion);
        List<Environment> sameMajor = filterByMajor(candidates, attrKey, requested[0]);
        List<Environment> eligible = sameMajor.isEmpty() ? candidates : sameMajor;
        return eligible.stream()
                .min(
                        Comparator.comparingInt((Environment env) -> toolVersionRank(env, attrKey, requested))
                                .thenComparingInt(env -> toolVersionDistance(env, attrKey, requested))
                                .thenComparing(simpleEnvironmentComparator(buildTypeForAttribute(attrKey))))
                .orElseThrow();
    }

    private List<Environment> preferDefaultMavenMajor(List<Environment> candidates) {
        int defaultMajor = defaultMavenMajor();
        List<Environment> sameMajor = filterByMajor(candidates, "MAVEN", defaultMajor);
        return sameMajor.isEmpty() ? candidates : sameMajor;
    }

    private int defaultMavenMajor() {
        String defaultName = config.getDefaultValues().getEnvironmentName();
        for (Environment environment : environments.values()) {
            if (environment.getName().equals(defaultName)) {
                String version = environment.getAttributes().get("MAVEN");
                return version == null ? DEFAULT_MAVEN_MAJOR : parseVersion(version)[0];
            }
        }
        return DEFAULT_MAVEN_MAJOR;
    }

    private List<Environment> filterByMajor(List<Environment> candidates, String attrKey, int major) {
        return candidates.stream()
                .filter(env -> parseVersion(toolVersion(env, attrKey))[0] == major)
                .collect(Collectors.toList());
    }

    private Environment selectHighestVersion(List<Environment> candidates, String attrKey) {
        return candidates.stream()
                .min(
                        Comparator.comparing(
                                (Environment env) -> new ComparableVersion(toolVersion(env, attrKey)),
                                Comparator.reverseOrder())
                                .thenComparing(simpleEnvironmentComparator(buildTypeForAttribute(attrKey))))
                .orElseThrow();
    }

    private Environment selectLowestVersion(List<Environment> candidates, String attrKey) {
        return candidates.stream()
                .min(
                        Comparator.comparing(
                                (Environment env) -> new ComparableVersion(toolVersion(env, attrKey)))
                                .thenComparing(simpleEnvironmentComparator(buildTypeForAttribute(attrKey))))
                .orElseThrow();
    }

    private Comparator<Environment> simpleEnvironmentComparator(BuildType buildType) {
        return Comparator.comparingInt((Environment env) -> isBundledEnvironment(env, buildType) ? 1 : 0)
                .thenComparingInt(env -> env.getAttributes().size())
                .thenComparing(Environment::getName)
                .thenComparing(Environment::getId);
    }

    private static BuildType buildTypeForAttribute(String attrKey) {
        return "GRADLE".equals(attrKey) ? BuildType.GRADLE : BuildType.MVN;
    }

    private static String toolAttribute(BuildType buildType) {
        return buildType == BuildType.GRADLE ? "GRADLE" : "MAVEN";
    }

    private static boolean isBundledEnvironment(Environment environment, BuildType buildType) {
        String otherTool = buildType == BuildType.MVN ? "GRADLE" : "MAVEN";
        if (environment.getAttributes().containsKey(otherTool)) {
            return true;
        }
        String name = environment.getName();
        if (name == null) {
            return false;
        }
        Matcher matcher = OPENJDK_IN_NAME.matcher(name);
        return matcher.find() && matcher.find();
    }

    private static String toolVersion(Environment env, String attrKey) {
        return env.getAttributes().getOrDefault(attrKey, "0");
    }

    private static boolean containsEnvironment(List<Environment> candidates, Environment environment) {
        return candidates.stream().anyMatch(candidate -> candidate.getId().equals(environment.getId()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean matchesJdk(Environment env, JdkVersion jdkVersion) {
        String jdkAttr = env.getAttributes().get("JDK");
        if (jdkAttr == null) {
            return false;
        }
        JdkVersion envJdk = JdkVersion.fromVersionString(jdkAttr);
        return envJdk == jdkVersion;
    }

    private static boolean matchesBuildType(Environment env, BuildType buildType) {
        Map<String, String> attrs = env.getAttributes();
        if (buildType == BuildType.GRADLE) {
            return attrs.containsKey("GRADLE");
        }
        return attrs.containsKey("MAVEN");
    }

    /**
     * Ranks environments into tiers: 0 = exact match, 1 = newer version (usable fallback), 2 = older or unparseable.
     */
    private static int toolVersionRank(Environment env, String attrKey, int[] requested) {
        String version = env.getAttributes().get(attrKey);
        if (version == null) {
            return 2;
        }
        int[] parsed = parseVersion(version);
        int cmp = compareVersions(parsed, requested);
        if (cmp == 0) {
            return 0;
        }
        if (cmp > 0) {
            return 1;
        }
        return 2;
    }

    private static int toolVersionDistance(Environment env, String attrKey, int[] requested) {
        String version = env.getAttributes().get(attrKey);
        if (version == null) {
            return Integer.MAX_VALUE;
        }
        int[] parsed = parseVersion(version);
        return versionDistance(parsed, requested);
    }

    static int versionDistance(int[] a, int[] b) {
        int d0 = Math.abs(a[0] - b[0]);
        int d1 = Math.abs(a[1] - b[1]);
        int d2 = Math.abs(a[2] - b[2]);
        return d0 * 1_000_000 + d1 * 1_000 + d2;
    }

    static int[] parseVersion(String version) {
        if (version == null) {
            return new int[] { 0, 0, 0 };
        }
        String[] parts = version.split("\\.");
        int[] result = new int[3];
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9].*", ""));
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    private static int compareVersions(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return Integer.compare(a[i], b[i]);
            }
        }
        return 0;
    }

    private Environment findByName(String name) {
        for (Environment env : environments.values()) {
            if (env.getName().equals(name)) {
                return env;
            }
        }
        if (environments.isEmpty()) {
            log.warn("No environments available, returning default fallback environment");
            return defaultFallbackEnvironment();
        }
        log.error("Could not find environment '{}' by name, returning first available environment", name);
        return environments.values().iterator().next();
    }

    private static Environment defaultFallbackEnvironment() {
        return Environment.builder()
                .id("1593")
                .name("OpenJDK 1.8; RHEL 8; Mvn 3.9.5")
                .deprecated(false)
                .hidden(false)
                .attributes(Map.of("JDK", "1.8.0", "MAVEN", "3.9.5", "OS", "Linux"))
                .build();
    }
}
