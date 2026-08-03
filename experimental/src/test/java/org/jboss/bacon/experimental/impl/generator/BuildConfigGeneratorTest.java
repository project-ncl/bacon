package org.jboss.bacon.experimental.impl.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.jboss.bacon.experimental.impl.config.BuildConfigGeneratorConfig;
import org.jboss.bacon.experimental.impl.dependencies.Project;
import org.jboss.bacon.experimental.impl.projectfinder.EnvironmentResolver;
import org.jboss.bacon.experimental.impl.projectfinder.JdkVersion;
import org.jboss.bacon.experimental.impl.projectfinder.ProjectBuildInfo;
import org.jboss.bacon.experimental.impl.projectfinder.ProjectBuildInfoDetector;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.dto.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class BuildConfigGeneratorTest {

    @Test
    public void testOverrideRemoval() {
        String param = "test -DpluginOverride.*:*@*=  -DrepoReportingRemoval=true -DprojectMetaSkip=true -DdependencyOverride.javax.persistence:javax.persistence-api@*= -DprojectSrcSkip=false -DversionOverride=39";
        String paramNoDepOverride = BuildConfigGenerator.removeOverride(param, true);
        assertThat(paramNoDepOverride).isEqualTo(
                "test -DpluginOverride.*:*@*= -DrepoReportingRemoval=true -DprojectMetaSkip=true -DprojectSrcSkip=false -DversionOverride=39");
        String paramNoOverride = BuildConfigGenerator.removeOverride(param, false);
        assertThat(paramNoOverride).isEqualTo(
                "test -DpluginOverride.*:*@*= -DrepoReportingRemoval=true -DprojectMetaSkip=true -DprojectSrcSkip=false");

        String multilineParam = "test\t-DpluginOverride.*:*@*=\n-DrepoReportingRemoval=true\n-DprojectMetaSkip=true\t-DdependencyOverride.javax.persistence:javax.persistence-api@*=\n-DprojectSrcSkip=false\n-DversionOverride=39";
        String multilineParamNoOverride = BuildConfigGenerator.removeOverride(multilineParam, false);
        assertThat(multilineParamNoOverride).isEqualTo(
                "test -DpluginOverride.*:*@*= -DrepoReportingRemoval=true -DprojectMetaSkip=true -DprojectSrcSkip=false");

        String paramWithSpaces = "test -DpluginOverride.*:*@*=  -DrepoReportingRemoval=true \"-DprojectMetaSkip=true and not false\" -DdependencyOverride.javax.persistence:javax.persistence-api@*= -DprojectSrcSkip=false -DversionOverride=39";
        String paramWithSpacesNoOverride = BuildConfigGenerator.removeOverride(paramWithSpaces, false);
        assertThat(paramWithSpacesNoOverride).isEqualTo(
                "test -DpluginOverride.*:*@*= -DrepoReportingRemoval=true \"-DprojectMetaSkip=true and not false\" -DprojectSrcSkip=false");
    }

    @Test
    public void reselectsEnvironmentForExistingBuildConfig() {
        BuildConfigGeneratorConfig config = new BuildConfigGeneratorConfig();
        EnvironmentResolver environmentResolver = mock(EnvironmentResolver.class);
        ProjectBuildInfoDetector detector = mock(ProjectBuildInfoDetector.class);
        BuildConfigGenerator generator = new BuildConfigGenerator(config, environmentResolver, detector);

        Project project = mock(Project.class);
        ProjectBuildInfo detected = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_11)
                .buildType(BuildType.MVN)
                .buildToolVersion("3.9.6")
                .detectionSource("Maven Wrapper")
                .build();
        when(detector.detect(project)).thenReturn(detected);

        Environment oldEnvironment = environment("1", "OpenJDK 11.0; Mvn 3.5.4", "11", "3.5.4");
        Environment newEnvironment = environment("2", "OpenJDK 11.0; Mvn 3.9.6", "11", "3.9.6");
        when(environmentResolver.selectEnvironment(any(), same(oldEnvironment))).thenReturn(newEnvironment);

        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setName("jackson-parent-2.21");
        buildConfig.setBuildType("MVN");
        buildConfig.setBuildScript("mvn clean deploy");
        buildConfig.setSystemImageId("old-image");

        generator.reselectEnvironment(buildConfig, project, oldEnvironment);

        assertThat(buildConfig.getEnvironmentName()).isEqualTo(newEnvironment.getName());
        assertThat(buildConfig.getSystemImageId()).isNull();
        assertThat(buildConfig.getBuildScript()).contains("modified by Autobuilder");
    }

    @Test
    public void reselectsUsingOriginalDeprecatedEnvironmentAsVersionFloor() {
        BuildConfigGeneratorConfig config = new BuildConfigGeneratorConfig();
        EnvironmentResolver environmentResolver = mock(EnvironmentResolver.class);
        ProjectBuildInfoDetector detector = mock(ProjectBuildInfoDetector.class);
        BuildConfigGenerator generator = new BuildConfigGenerator(config, environmentResolver, detector);

        Project project = mock(Project.class);
        ProjectBuildInfo detected = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_1_8)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.6.3,)")
                .detectionSource("pom.xml")
                .build();
        when(detector.detect(project)).thenReturn(detected);

        Environment original = Environment.builder()
                .id("old")
                .name("OracleJDK8u192; Mvn 3.5.4")
                .deprecated(true)
                .hidden(false)
                .attributes(Map.of("JDK", "1.8", "MAVEN", "3.5.4"))
                .build();
        Environment automaticReplacement = environment(
                "922",
                "OpenJDK 1.8; OpenJDK 11; OpenJDK 17; Mvn 3.9.12; Gradle 8.14.4",
                "1.8",
                "3.9.12");
        Environment conservativeReplacement = environment(
                "600",
                "OpenJDK 1.8; Mvn 3.6.3",
                "1.8",
                "3.6.3");
        when(environmentResolver.resolve(original)).thenReturn(automaticReplacement);
        when(environmentResolver.selectEnvironment(any(), same(original))).thenReturn(conservativeReplacement);

        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setName("commons-parent-97");
        buildConfig.setBuildType("MVN");
        buildConfig.setBuildScript("mvn clean deploy");
        buildConfig.setEnvironmentName(automaticReplacement.getName());

        generator.reselectEnvironment(buildConfig, project, original);

        assertThat(buildConfig.getEnvironmentName()).isEqualTo(conservativeReplacement.getName());
    }

    @Test
    public void usesDetectedJdkWhenProjectDetectionIsConclusive() {
        BuildConfigGeneratorConfig config = new BuildConfigGeneratorConfig();
        EnvironmentResolver environmentResolver = mock(EnvironmentResolver.class);
        ProjectBuildInfoDetector detector = mock(ProjectBuildInfoDetector.class);
        BuildConfigGenerator generator = new BuildConfigGenerator(config, environmentResolver, detector);

        Project project = mock(Project.class);
        ProjectBuildInfo detected = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_17)
                .buildType(BuildType.MVN)
                .buildToolVersionRange("[3.6.3,)")
                .detectionSource("MANIFEST.MF from Maven Central")
                .build();
        when(detector.detect(project)).thenReturn(detected);

        Environment existing = environment("1", "OpenJDK 11.0; Mvn 3.5.4", "11", "3.5.4");
        when(environmentResolver.selectEnvironment(any(), same(existing))).thenReturn(existing);

        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setName("example");
        buildConfig.setBuildType("MVN");
        buildConfig.setBuildScript("mvn clean deploy");

        generator.reselectEnvironment(buildConfig, project, existing);

        ArgumentCaptor<ProjectBuildInfo> captor = ArgumentCaptor.forClass(ProjectBuildInfo.class);
        verify(environmentResolver).selectEnvironment(captor.capture(), same(existing));
        assertThat(captor.getValue().getJdkVersion()).isEqualTo(JdkVersion.JDK_17);
        assertThat(captor.getValue().getBuildType()).isEqualTo(BuildType.MVN);
    }

    @Test
    public void keepsExistingJdkWhenProjectDetectionUsesDefault() {
        BuildConfigGeneratorConfig config = new BuildConfigGeneratorConfig();
        EnvironmentResolver environmentResolver = mock(EnvironmentResolver.class);
        ProjectBuildInfoDetector detector = mock(ProjectBuildInfoDetector.class);
        BuildConfigGenerator generator = new BuildConfigGenerator(config, environmentResolver, detector);

        Project project = mock(Project.class);
        ProjectBuildInfo detected = ProjectBuildInfo.builder()
                .jdkVersion(JdkVersion.JDK_11)
                .buildType(BuildType.MVN)
                .detectionSource("default (no JDK version detected)")
                .build();
        when(detector.detect(project)).thenReturn(detected);

        Environment existing = environment("1", "OpenJDK 1.8; Mvn 3.5.4", "1.8", "3.5.4");
        when(environmentResolver.selectEnvironment(any(), same(existing))).thenReturn(existing);

        BuildConfig buildConfig = new BuildConfig();
        buildConfig.setName("example");
        buildConfig.setBuildType("MVN");
        buildConfig.setBuildScript("mvn clean deploy");

        generator.reselectEnvironment(buildConfig, project, existing);

        ArgumentCaptor<ProjectBuildInfo> captor = ArgumentCaptor.forClass(ProjectBuildInfo.class);
        verify(environmentResolver).selectEnvironment(captor.capture(), same(existing));
        assertThat(captor.getValue().getJdkVersion()).isEqualTo(JdkVersion.JDK_1_8);
    }

    private static Environment environment(String id, String name, String jdk, String maven) {
        return Environment.builder()
                .id(id)
                .name(name)
                .systemImageId(name.replace(' ', '-'))
                .deprecated(false)
                .hidden(false)
                .attributes(Map.of("JDK", jdk, "MAVEN", maven))
                .build();
    }
}
