package org.jboss.bacon.experimental.impl.projectfinder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
