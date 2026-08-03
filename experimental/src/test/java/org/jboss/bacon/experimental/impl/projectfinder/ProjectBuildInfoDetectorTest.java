package org.jboss.bacon.experimental.impl.projectfinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectBuildInfoDetectorTest {

    private ProjectBuildInfoDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ProjectBuildInfoDetector();
    }

    @Nested
    class ManifestParsing {

        @Test
        void parsesBuildJdkSpec() {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().putValue("Build-Jdk-Spec", "17");

            assertThat(detector.parseManifest(manifest)).isEqualTo(JdkVersion.JDK_17);
        }

        @Test
        void parsesBuildJdk() {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().putValue("Build-Jdk", "17.0.13+7");

            assertThat(detector.parseManifest(manifest)).isEqualTo(JdkVersion.JDK_17);
        }

        @Test
        void parsesCreatedBy() {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().putValue("Created-By", "21.0.2 (Red Hat, Inc.)");

            assertThat(detector.parseManifest(manifest)).isEqualTo(JdkVersion.JDK_21);
        }

        @Test
        void buildJdkSpecTakesPrecedence() {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().putValue("Build-Jdk-Spec", "17");
            manifest.getMainAttributes().putValue("Build-Jdk", "21.0.2");

            assertThat(detector.parseManifest(manifest)).isEqualTo(JdkVersion.JDK_17);
        }

        @Test
        void returnsNullForEmptyManifest() {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            assertThat(detector.parseManifest(manifest)).isNull();
        }

        @Test
        void parsesJdk8BuildJdk() {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().putValue("Build-Jdk", "1.8.0_362");

            assertThat(detector.parseManifest(manifest)).isEqualTo(JdkVersion.JDK_1_8);
        }
    }

    @Nested
    class PomXmlParsing {

        @Test
        void parsesCompilerRelease() {
            String pom = "<project><properties>"
                    + "<maven.compiler.release>17</maven.compiler.release>"
                    + "</properties></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_17);
            assertThat(result.detectionSource).contains("maven-compiler properties");
        }

        @Test
        void parsesCompilerSource() {
            String pom = "<project><properties>"
                    + "<maven.compiler.source>11</maven.compiler.source>"
                    + "<maven.compiler.target>11</maven.compiler.target>"
                    + "</properties></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_11);
        }

        @Test
        void parsesCompilerPluginConfig() {
            String pom = "<project><build><plugins><plugin>"
                    + "<artifactId>maven-compiler-plugin</artifactId>"
                    + "<configuration><release>21</release></configuration>"
                    + "</plugin></plugins></build></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_21);
            assertThat(result.detectionSource).contains("maven-compiler-plugin");
        }

        @Test
        void propertiesTakePrecedenceOverPluginConfig() {
            String pom = "<project>"
                    + "<properties><maven.compiler.release>17</maven.compiler.release></properties>"
                    + "<build><plugins><plugin>"
                    + "<artifactId>maven-compiler-plugin</artifactId>"
                    + "<configuration><release>11</release></configuration>"
                    + "</plugin></plugins></build>"
                    + "</project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_17);
        }

        @Test
        void releaseTakesPrecedenceOverSource() {
            String pom = "<project><properties>"
                    + "<maven.compiler.release>17</maven.compiler.release>"
                    + "<maven.compiler.source>11</maven.compiler.source>"
                    + "</properties></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_17);
        }

        @Test
        void handlesJdk18Source() {
            String pom = "<project><properties>"
                    + "<maven.compiler.source>1.8</maven.compiler.source>"
                    + "</properties></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_1_8);
        }

        @Test
        void storesParsedDocument() {
            String pom = "<project><properties>"
                    + "<maven.compiler.release>17</maven.compiler.release>"
                    + "</properties></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(result.parsedPomDoc).isNotNull();
        }

        @Test
        void parsesSimpleEnforcerMavenVersionAsMinimumRange() {
            String pom = "<project><build><plugins><plugin>"
                    + "<artifactId>maven-enforcer-plugin</artifactId>"
                    + "<configuration><rules><requireMavenVersion>"
                    + "<version>3.6.3</version>"
                    + "</requireMavenVersion></rules></configuration>"
                    + "</plugin></plugins></build></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(detector.parseEnforcerMavenVersion(result.parsedPomDoc)).isEqualTo("[3.6.3,)");
        }

        @Test
        void parsesCommonsParentMinimumMavenBuildVersionProperty() {
            String pom = "<project><properties>"
                    + "<minimalMavenBuildVersion>3.8.1</minimalMavenBuildVersion>"
                    + "</properties></project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(detector.parseDeclaredMinimumMavenVersion(result.parsedPomDoc)).isEqualTo("[3.8.1,)");
        }

        @Test
        void derivesMavenRequirementFromEnforcerPluginVersion() {
            String pom = "<project>"
                    + "<properties><enforcer.version>3.6.2</enforcer.version></properties>"
                    + "<build><plugins><plugin>"
                    + "<groupId>org.apache.maven.plugins</groupId>"
                    + "<artifactId>maven-enforcer-plugin</artifactId>"
                    + "<version>${enforcer.version}</version>"
                    + "</plugin></plugins></build>"
                    + "</project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(detector.parseEnforcerPluginMavenPrerequisite(result.parsedPomDoc))
                    .isEqualTo("[3.6.3,)");
        }

        @Test
        void intersectsDetectedMavenRequirements() {
            assertThat(detector.combineMavenRequirements("[3.6.3,4.0.0)", "[3.8.1,)", "[3.2.5,)"))
                    .isEqualTo("[3.8.1,4.0.0)");
        }

        @Test
        void resolvesPropertyInEnforcerMavenVersionRange() {
            String pom = "<project>"
                    + "<properties><minimum.maven.version>3.6.3</minimum.maven.version></properties>"
                    + "<build><plugins><plugin>"
                    + "<artifactId>maven-enforcer-plugin</artifactId>"
                    + "<configuration><rules><requireMavenVersion>"
                    + "<version>[${minimum.maven.version},4.0.0)</version>"
                    + "</requireMavenVersion></rules></configuration>"
                    + "</plugin></plugins></build>"
                    + "</project>";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(pom, result);

            assertThat(detector.parseEnforcerMavenVersion(result.parsedPomDoc)).isEqualTo("[3.6.3,4.0.0)");
        }

        @Test
        void keepsWrapperPreferenceAndHardPomRequirement() {
            ScmFileAccessor accessor = mock(ScmFileAccessor.class);
            ProjectBuildInfoDetector detector = new ProjectBuildInfoDetector(
                    mock(CloseableHttpClient.class),
                    accessor);
            when(
                    accessor.fetchFile(
                            "https://github.com/example/project",
                            "1.0",
                            ".mvn/wrapper/maven-wrapper.properties"))
                    .thenReturn(
                            Optional.of(
                                    "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.6.0/apache-maven-3.6.0-bin.zip"));

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parsePomXml(
                    "<project><build><plugins><plugin>"
                            + "<artifactId>maven-enforcer-plugin</artifactId>"
                            + "<configuration><rules><requireMavenVersion><version>3.6.3</version>"
                            + "</requireMavenVersion></rules></configuration>"
                            + "</plugin></plugins></build></project>",
                    result);

            detector.detectMavenVersion("https://github.com/example/project", "1.0", result);

            assertThat(result.buildToolVersion).isEqualTo("3.6.0");
            assertThat(result.buildToolVersionRange).isEqualTo("[3.6.3,)");
        }

    }

    @Nested
    class GradleBuildParsing {

        @Test
        void parsesToolchain() {
            String gradle = "java {\n"
                    + "    toolchain {\n"
                    + "        languageVersion.set(JavaLanguageVersion.of(21))\n"
                    + "    }\n"
                    + "}";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parseGradleBuild(gradle, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_21);
            assertThat(result.detectionSource).contains("toolchain");
        }

        @Test
        void parsesJavaVersion() {
            String gradle = "sourceCompatibility = JavaVersion.VERSION_17\n"
                    + "targetCompatibility = JavaVersion.VERSION_17";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parseGradleBuild(gradle, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_17);
            assertThat(result.detectionSource).contains("JavaVersion");
        }

        @Test
        void parsesSourceCompatibilityNumber() {
            String gradle = "sourceCompatibility = '11'";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parseGradleBuild(gradle, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_11);
            assertThat(result.detectionSource).contains("sourceCompatibility");
        }

        @Test
        void toolchainTakesPrecedence() {
            String gradle = "sourceCompatibility = '11'\n"
                    + "java {\n"
                    + "    toolchain {\n"
                    + "        languageVersion.set(JavaLanguageVersion.of(17))\n"
                    + "    }\n"
                    + "}";

            ProjectBuildInfoDetector.ScmDetectionResult result = new ProjectBuildInfoDetector.ScmDetectionResult();
            detector.parseGradleBuild(gradle, result);

            assertThat(result.jdkVersion).isEqualTo(JdkVersion.JDK_17);
        }
    }
}
