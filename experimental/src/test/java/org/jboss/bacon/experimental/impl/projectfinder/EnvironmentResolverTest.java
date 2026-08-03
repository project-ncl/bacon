package org.jboss.bacon.experimental.impl.projectfinder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.jboss.bacon.experimental.impl.config.BuildConfigGeneratorConfig;
import org.jboss.bacon.experimental.impl.config.DefaultBuildConfigValues;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.dto.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvironmentResolverTest {

    private Map<String, Environment> environments;
    private BuildConfigGeneratorConfig config;

    @BeforeEach
    void setUp() {
        environments = new HashMap<>();
        config = new BuildConfigGeneratorConfig();
        DefaultBuildConfigValues defaults = new DefaultBuildConfigValues();
        defaults.setEnvironmentName("OpenJDK 11.0; Mvn 3.8.6");
        config.setDefaultValues(defaults);
    }

    private Environment buildEnv(String id, String name, Map<String, String> attrs) {
        return Environment.builder()
                .id(id)
                .name(name)
                .deprecated(false)
                .hidden(false)
                .attributes(attrs)
                .build();
    }

    private void addEnv(String id, String name, Map<String, String> attrs) {
        environments.put(id, buildEnv(id, name, attrs));
    }

    @Test
    void selectsJdk17MavenEnvironment() {
        addEnv("1", "OpenJDK 11.0; Mvn 3.8.6", Map.of("JDK", "11", "MAVEN", "3.8.6", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.9.6", Map.of("JDK", "17.0", "MAVEN", "3.9.6", "OS", "Linux"));
        addEnv("3", "OpenJDK 21; Mvn 3.9.6", Map.of("JDK", "21", "MAVEN", "3.9.6", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void selectsGradleEnvironment() {
        addEnv(
                "1",
                "OpenJDK 17.0; Mvn 3.9.6",
                Map.of("JDK", "17.0", "MAVEN", "3.9.6", "OS", "Linux"));
        addEnv(
                "2",
                "OpenJDK 17.0; Mvn 3.9.6; Gradle 8.0.2",
                Map.of("JDK", "17.0", "MAVEN", "3.9.6", "GRADLE", "8.0.2", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.GRADLE)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void prefersExactToolVersionMatch() {
        addEnv(
                "1",
                "OpenJDK 17.0; Mvn 3.8.6",
                Map.of("JDK", "17.0", "MAVEN", "3.8.6", "OS", "Linux"));
        addEnv(
                "2",
                "OpenJDK 17.0; Mvn 3.9.6",
                Map.of("JDK", "17.0", "MAVEN", "3.9.6", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.9.6")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void prefersSimplestEnvironmentWhenNoToolVersionPreference() {
        addEnv(
                "1",
                "OpenJDK 17.0; Mvn 3.9.6",
                Map.of("JDK", "17.0", "MAVEN", "3.9.6", "OS", "Linux"));
        addEnv(
                "2",
                "OpenJDK 17.0; Mvn 3.9.6; Gradle 8.0.2; SBT 1.9.9",
                Map.of("JDK", "17.0", "MAVEN", "3.9.6", "GRADLE", "8.0.2", "SBT", "1.9.9", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void prefersSimplestMavenEnvironmentWhenNoVersionWasDetected() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.6.0", Map.of("JDK", "17.0", "MAVEN", "3.6.0", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.9.9", Map.of("JDK", "17.0", "MAVEN", "3.9.9", "OS", "Linux"));
        addEnv("3", "OpenJDK 17.0; Mvn 3.8.8", Map.of("JDK", "17.0", "MAVEN", "3.8.8", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void doesNotAutomaticallyCrossFromDefaultMaven3ToMaven4() {
        addEnv("0", "OpenJDK 11.0; Mvn 3.8.6", Map.of("JDK", "11.0", "MAVEN", "3.8.6", "OS", "Linux"));
        addEnv("1", "OpenJDK 17.0; Mvn 3.9.9", Map.of("JDK", "17.0", "MAVEN", "3.9.9", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 4.0.0", Map.of("JDK", "17.0", "MAVEN", "4.0.0", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void hardRequirementOverridesOlderWrapperPreference() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.6.0", Map.of("JDK", "17.0", "MAVEN", "3.6.0"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.6.3", Map.of("JDK", "17.0", "MAVEN", "3.6.3"));
        addEnv("3", "OpenJDK 17.0; Mvn 3.9.9", Map.of("JDK", "17.0", "MAVEN", "3.9.9"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.6.0")
                .buildToolVersionRange("[3.6.3,)")
                .build();

        assertThat(resolver.selectEnvironment(buildInfo).getId()).isEqualTo("2");
    }

    @Test
    void wrapperPreferenceRemainsInsideRequiredUpperBound() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.5.4", Map.of("JDK", "17.0", "MAVEN", "3.5.4"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.8.8", Map.of("JDK", "17.0", "MAVEN", "3.8.8"));
        addEnv("3", "OpenJDK 17.0; Mvn 3.9.6", Map.of("JDK", "17.0", "MAVEN", "3.9.6"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.8.0")
                .buildToolVersionRange("[3.6.3,3.9.0)")
                .build();

        assertThat(resolver.selectEnvironment(buildInfo).getId()).isEqualTo("2");
    }

    @Test
    void honorsUpperBoundOfRequiredMavenRange() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.8.8", Map.of("JDK", "17.0", "MAVEN", "3.8.8", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.9.9", Map.of("JDK", "17.0", "MAVEN", "3.9.9", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.6.3,3.9.0)")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void excludesDeprecatedEnvironments() {
        environments.put(
                "1",
                Environment.builder()
                        .id("1")
                        .name("OpenJDK 17.0; Mvn 3.9.6")
                        .deprecated(true)
                        .hidden(false)
                        .attributes(Map.of("JDK", "17.0", "MAVEN", "3.9.6"))
                        .build());
        addEnv("2", "OpenJDK 17.0; Mvn 3.8.6", Map.of("JDK", "17.0", "MAVEN", "3.8.6", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void excludesHiddenEnvironments() {
        environments.put(
                "1",
                Environment.builder()
                        .id("1")
                        .name("OpenJDK 17.0; Mvn 3.9.6")
                        .deprecated(false)
                        .hidden(true)
                        .attributes(Map.of("JDK", "17.0", "MAVEN", "3.9.6"))
                        .build());
        addEnv("2", "OpenJDK 17.0; Mvn 3.8.6", Map.of("JDK", "17.0", "MAVEN", "3.8.6", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void fallsBackToDefaultWhenNoMatch() {
        addEnv("1", "OpenJDK 11.0; Mvn 3.8.6", Map.of("JDK", "11", "MAVEN", "3.8.6", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_25)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getName()).isEqualTo("OpenJDK 11.0; Mvn 3.8.6");
    }

    @Test
    void fallsBackToNewerVersionWhenExactNotAvailable() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.8.6", Map.of("JDK", "17.0", "MAVEN", "3.8.6", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.9.9", Map.of("JDK", "17.0", "MAVEN", "3.9.9", "OS", "Linux"));
        addEnv("3", "OpenJDK 17.0; Mvn 3.9.16", Map.of("JDK", "17.0", "MAVEN", "3.9.16", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.9.6")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void prefersNewerOverOlderWhenNoExactMatch() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.6.3", Map.of("JDK", "17.0", "MAVEN", "3.6.3", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.9.9", Map.of("JDK", "17.0", "MAVEN", "3.9.9", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.8.6")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void prefersClosestNewerVersion() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.9.6", Map.of("JDK", "17.0", "MAVEN", "3.9.6", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.9.16", Map.of("JDK", "17.0", "MAVEN", "3.9.16", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.9.3")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void fallsBackToOlderWhenNoNewerAvailable() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.6.3", Map.of("JDK", "17.0", "MAVEN", "3.6.3", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.8.6", Map.of("JDK", "17.0", "MAVEN", "3.8.6", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.9.6")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void exactMatchStillWinsOverNewer() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.9.6", Map.of("JDK", "17.0", "MAVEN", "3.9.6", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.9.9", Map.of("JDK", "17.0", "MAVEN", "3.9.9", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.9.6")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void selectsLowestMavenVersionThatSatisfiesRequiredRange() {
        addEnv("1", "OpenJDK 11.0; Mvn 3.5.4", Map.of("JDK", "11", "MAVEN", "3.5.4"));
        addEnv("2", "OpenJDK 11.0; Mvn 3.6.3", Map.of("JDK", "11", "MAVEN", "3.6.3"));
        addEnv(
                "3",
                "OpenJDK 11.0; Mvn 3.9.12",
                Map.of("JDK", "11", "MAVEN", "3.9.12", "GRADLE", "8.14.4"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_11)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.6.3,)")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }

    @Test
    void keepsExistingEnvironmentWhenItAlreadySatisfiesRequiredRange() {
        Environment existing = buildEnv(
                "1",
                "OpenJDK 11.0; Mvn 3.6.3",
                Map.of("JDK", "11", "MAVEN", "3.6.3"));
        environments.put(existing.getId(), existing);
        addEnv("2", "OpenJDK 11.0; Mvn 3.9.12", Map.of("JDK", "11", "MAVEN", "3.9.12"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_11)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.6.3,)")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo, existing);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void keepsActiveBundledEnvironmentWhenItAlreadySatisfiesRequirement() {
        Environment bundled = buildEnv(
                "1",
                "OpenJDK 1.8; OpenJDK 11; OpenJDK 17; Mvn 3.9.12; Gradle 8.14.4",
                Map.of("JDK", "11", "MAVEN", "3.9.12", "GRADLE", "8.14.4"));
        environments.put(bundled.getId(), bundled);
        addEnv("2", "OpenJDK 11.0; Mvn 3.6.3", Map.of("JDK", "11", "MAVEN", "3.6.3"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_11)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.6.3,)")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo, bundled);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void deprecatedExistingEnvironmentNeverDowngradesMaven() {
        Environment deprecated = Environment.builder()
                .id("old")
                .name("OracleJDK8u192; Mvn 3.5.4")
                .deprecated(true)
                .hidden(false)
                .attributes(Map.of("JDK", "1.8", "MAVEN", "3.5.4"))
                .build();
        addEnv(
                "570",
                "OpenJDK 1.8; Mvn 3.3.9; Gcc; Make; Cmake3; Protobuf",
                Map.of("JDK", "1.8", "MAVEN", "3.3.9", "GCC", "1", "CMAKE", "3"));
        addEnv("600", "OpenJDK 1.8; Mvn 3.6.3", Map.of("JDK", "1.8", "MAVEN", "3.6.3"));
        addEnv(
                "922",
                "OpenJDK 1.8; OpenJDK 11; OpenJDK 17; Mvn 3.9.12; Gradle 8.14.4",
                Map.of("JDK", "1.8", "MAVEN", "3.9.12", "GRADLE", "8.14.4"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_1_8)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo, deprecated);
        assertThat(selected.getId()).isEqualTo("600");
    }

    @Test
    void commonsParentMinimumSelectsSmallestSimpleCompatibleEnvironment() {
        Environment deprecated = Environment.builder()
                .id("old")
                .name("OracleJDK8u192; Mvn 3.5.4")
                .deprecated(true)
                .hidden(false)
                .attributes(Map.of("JDK", "1.8", "MAVEN", "3.5.4"))
                .build();
        addEnv("600", "OpenJDK 1.8; Mvn 3.6.3", Map.of("JDK", "1.8", "MAVEN", "3.6.3"));
        addEnv("700", "OpenJDK 1.8; Mvn 3.9.0", Map.of("JDK", "1.8", "MAVEN", "3.9.0"));
        addEnv(
                "922",
                "OpenJDK 1.8; OpenJDK 11; OpenJDK 17; Mvn 3.9.12; Gradle 8.14.4",
                Map.of("JDK", "1.8", "MAVEN", "3.9.12", "GRADLE", "8.14.4"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_1_8)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.8.1,)")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo, deprecated);
        assertThat(selected.getId()).isEqualTo("700");
    }

    @Test
    void requiredUpperBoundCanOverrideNoDowngradePreference() {
        Environment existing = buildEnv(
                "1",
                "OpenJDK 11.0; Mvn 3.9.6",
                Map.of("JDK", "11", "MAVEN", "3.9.6"));
        environments.put(existing.getId(), existing);
        addEnv("2", "OpenJDK 11.0; Mvn 3.8.8", Map.of("JDK", "11", "MAVEN", "3.8.8"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_11)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.6.3,3.9.0)")
                .build();

        assertThat(resolver.selectEnvironment(buildInfo, existing).getId()).isEqualTo("2");
    }

    @Test
    void versionDistanceHandlesLargeMinorVersions() {
        assertThat(EnvironmentResolver.versionDistance(new int[] { 3, 9, 16 }, new int[] { 3, 10, 6 }))
                .isGreaterThan(0);
        assertThat(EnvironmentResolver.versionDistance(new int[] { 3, 9, 106 }, new int[] { 3, 10, 6 }))
                .isGreaterThan(0);
        assertThat(
                EnvironmentResolver.versionDistance(
                        new int[] { 3, 9, 16 },
                        new int[] { 3, 9, 16 }))
                .isEqualTo(0);
    }

    @Test
    void prefersCloserVersionEvenWithHighMinor() {
        addEnv("1", "OpenJDK 17.0; Mvn 3.9.16", Map.of("JDK", "17.0", "MAVEN", "3.9.16", "OS", "Linux"));
        addEnv("2", "OpenJDK 17.0; Mvn 3.11.0", Map.of("JDK", "17.0", "MAVEN", "3.11.0", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.9.9")
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("1");
    }

    @Test
    void mavenBuildTypeExcludesGradleOnlyEnvironments() {
        addEnv(
                "1",
                "OpenJDK 17.0; Gradle 8.0.2",
                Map.of("JDK", "17.0", "GRADLE", "8.0.2", "OS", "Linux"));
        addEnv(
                "2",
                "OpenJDK 17.0; Mvn 3.9.6",
                Map.of("JDK", "17.0", "MAVEN", "3.9.6", "OS", "Linux"));

        EnvironmentResolver resolver = new EnvironmentResolver(environments, config);
        ProjectBuildInfo buildInfo = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .build();

        Environment selected = resolver.selectEnvironment(buildInfo);
        assertThat(selected.getId()).isEqualTo("2");
    }
}
