package org.jboss.bacon.experimental.impl.projectfinder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.bacon.experimental.impl.dependencies.Project;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.api.enums.BuildType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectBuildInfoDetector implements Closeable {

    private static final int DOWNLOAD_TIMEOUT = 60_000;
    private static final String MAVEN_CENTRAL_BASE = "https://repo1.maven.org/maven2";

    private static final Pattern GRADLE_SOURCE_COMPAT = Pattern
            .compile("(?:source|target)Compatibility\\s*=\\s*['\"]?(\\d+(?:\\.\\d+)?)['\"]?");
    private static final Pattern GRADLE_JAVA_VERSION = Pattern.compile("JavaVersion\\.VERSION_(\\d+)");
    private static final Pattern GRADLE_TOOLCHAIN = Pattern
            .compile("JavaLanguageVersion\\.of\\s*\\(\\s*(\\d+)\\s*\\)");
    private static final Pattern GRADLE_WRAPPER_VERSION = Pattern.compile("gradle-(\\d+\\.\\d+(?:\\.\\d+)?)-");
    private static final Pattern MAVEN_WRAPPER_VERSION = Pattern
            .compile("apache-maven-(\\d+\\.\\d+(?:\\.\\d+)?)-");
    private static final Pattern DOCKERFILE_JDK = Pattern
            .compile("(?i)FROM.*(?:jdk|openjdk)[:-]?(\\d+)");
    private static final Pattern GITHUB_ACTIONS_JAVA = Pattern
            .compile("java-version\\s*:\\s*['\"]?(\\d+)");
    private static final Pattern JENKINSFILE_JDK = Pattern
            .compile("(?i)jdk\\s*['\"]?(?:jdk|openjdk)?-?(\\d+)");

    private final CloseableHttpClient httpClient;
    private final ScmFileAccessor scmFileAccessor;

    public ProjectBuildInfoDetector() {
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(DOWNLOAD_TIMEOUT)
                                .setSocketTimeout(DOWNLOAD_TIMEOUT)
                                .build())
                .build();
        this.scmFileAccessor = new ScmFileAccessor();
    }

    ProjectBuildInfoDetector(CloseableHttpClient httpClient, ScmFileAccessor scmFileAccessor) {
        this.httpClient = httpClient;
        this.scmFileAccessor = scmFileAccessor;
    }

    public ProjectBuildInfo detect(Project project) {
        JdkVersion jdk = null;
        String source = "default";
        BuildType buildType = BuildType.MVN;
        String buildToolVersion = null;

        jdk = detectFromManifest(project);
        if (jdk != null) {
            source = "MANIFEST.MF from Maven Central";
        }

        ScmDetectionResult scmResult = detectFromScm(project);
        if (scmResult != null) {
            if (scmResult.buildType != null) {
                buildType = scmResult.buildType;
            }
            if (scmResult.buildToolVersion != null) {
                buildToolVersion = scmResult.buildToolVersion;
            }
            if (jdk == null && scmResult.jdkVersion != null) {
                jdk = scmResult.jdkVersion;
                source = scmResult.detectionSource;
            }
        }

        if (jdk == null) {
            jdk = JdkVersion.JDK_11;
            source = "default (no JDK version detected)";
            log.warn("Could not detect JDK version for project {}, defaulting to JDK 11", project.getFirstGAV());
        }

        log.info(
                "Detected build info for {}: JDK={}, buildType={}, toolVersion={}, source={}",
                project.getFirstGAV(),
                jdk,
                buildType,
                buildToolVersion,
                source);

        return ProjectBuildInfo.builder()
                .jdkVersion(jdk)
                .buildType(buildType)
                .buildToolVersion(buildToolVersion)
                .detectionSource(source)
                .build();
    }

    JdkVersion detectFromManifest(Project project) {
        GAV gav = project.getFirstGAV();
        String groupPath = gav.getGroupId().replace('.', '/');
        String url = String.format(
                "%s/%s/%s/%s/%s-%s.jar",
                MAVEN_CENTRAL_BASE,
                groupPath,
                gav.getArtifactId(),
                gav.getVersion(),
                gav.getArtifactId(),
                gav.getVersion());

        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                log.debug("Maven Central JAR not found for {}: HTTP {}", gav, response.getStatusLine().getStatusCode());
                EntityUtils.consumeQuietly(response.getEntity());
                return null;
            }

            try (InputStream is = response.getEntity().getContent();
                    JarInputStream jarIs = new JarInputStream(is)) {
                Manifest manifest = jarIs.getManifest();
                if (manifest == null) {
                    log.debug("No MANIFEST.MF in JAR for {}", gav);
                    return null;
                }
                return parseManifest(manifest);
            }
        } catch (IOException e) {
            log.debug("Failed to fetch JAR from Maven Central for {}: {}", gav, e.getMessage());
            return null;
        }
    }

    JdkVersion parseManifest(Manifest manifest) {
        Attributes mainAttrs = manifest.getMainAttributes();

        String jdkSpec = mainAttrs.getValue("Build-Jdk-Spec");
        if (jdkSpec != null) {
            JdkVersion v = JdkVersion.fromVersionString(jdkSpec.trim());
            if (v != null)
                return v;
        }

        String buildJdk = mainAttrs.getValue("Build-Jdk");
        if (buildJdk != null) {
            JdkVersion v = JdkVersion.fromVersionString(buildJdk.trim());
            if (v != null)
                return v;
        }

        String createdBy = mainAttrs.getValue("Created-By");
        if (createdBy != null) {
            String versionPart = createdBy.trim().split("[\\s(]")[0];
            return JdkVersion.fromVersionString(versionPart);
        }

        return null;
    }

    ScmDetectionResult detectFromScm(Project project) {
        String scmUrl = project.getSourceCodeURL();
        String revision = project.getSourceCodeRevision();
        if (scmUrl == null || revision == null) {
            return null;
        }

        ScmDetectionResult result = new ScmDetectionResult();

        Optional<String> pomXml = scmFileAccessor.fetchFile(scmUrl, revision, "pom.xml");
        if (pomXml.isPresent()) {
            result.buildType = BuildType.MVN;
            parsePomXml(pomXml.get(), result);
            detectMavenVersion(scmUrl, revision, result);
        }

        Optional<String> buildGradle = scmFileAccessor.fetchFile(scmUrl, revision, "build.gradle");
        if (buildGradle.isEmpty()) {
            buildGradle = scmFileAccessor.fetchFile(scmUrl, revision, "build.gradle.kts");
        }
        if (buildGradle.isPresent()) {
            if (!pomXml.isPresent()) {
                result.buildType = BuildType.GRADLE;
            }
            if (result.jdkVersion == null) {
                parseGradleBuild(buildGradle.get(), result);
            }
            detectGradleVersion(scmUrl, revision, result);
        }

        if (result.jdkVersion == null) {
            detectFromJavaVersionFile(scmUrl, revision, result);
        }

        if (result.jdkVersion == null) {
            detectFromCiFiles(scmUrl, revision, result);
        }

        return result;
    }

    void parsePomXml(String pomContent, ScmDetectionResult result) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(pomContent)));
            result.parsedPomDoc = doc;

            JdkVersion fromProperties = parseCompilerProperties(doc);
            if (fromProperties != null) {
                result.jdkVersion = fromProperties;
                result.detectionSource = "maven-compiler properties in pom.xml";
                return;
            }

            JdkVersion fromPlugin = parseCompilerPluginConfig(doc);
            if (fromPlugin != null) {
                result.jdkVersion = fromPlugin;
                result.detectionSource = "maven-compiler-plugin configuration in pom.xml";
            }
        } catch (Exception e) {
            log.debug("Failed to parse pom.xml: {}", e.getMessage());
        }
    }

    private JdkVersion parseCompilerProperties(Document doc) {
        NodeList properties = doc.getElementsByTagName("properties");
        if (properties.getLength() == 0) {
            return null;
        }

        Element propsElement = (Element) properties.item(0);

        String[] propertyNames = {
                "maven.compiler.release",
                "maven.compiler.source",
                "maven.compiler.target"
        };

        for (String propName : propertyNames) {
            NodeList propNodes = propsElement.getElementsByTagName(propName);
            if (propNodes.getLength() > 0) {
                String value = propNodes.item(0).getTextContent().trim();
                JdkVersion v = JdkVersion.fromVersionString(value);
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    private JdkVersion parseCompilerPluginConfig(Document doc) {
        NodeList plugins = doc.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            NodeList artifactIds = plugin.getElementsByTagName("artifactId");
            if (artifactIds.getLength() > 0
                    && "maven-compiler-plugin".equals(artifactIds.item(0).getTextContent().trim())) {
                NodeList configs = plugin.getElementsByTagName("configuration");
                if (configs.getLength() > 0) {
                    Element config = (Element) configs.item(0);
                    String[] configNames = { "release", "source", "target" };
                    for (String name : configNames) {
                        NodeList nodes = config.getElementsByTagName(name);
                        if (nodes.getLength() > 0) {
                            String value = nodes.item(0).getTextContent().trim();
                            JdkVersion v = JdkVersion.fromVersionString(value);
                            if (v != null) {
                                return v;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    void parseGradleBuild(String gradleContent, ScmDetectionResult result) {
        Matcher toolchainMatcher = GRADLE_TOOLCHAIN.matcher(gradleContent);
        if (toolchainMatcher.find()) {
            JdkVersion v = JdkVersion.fromVersionString(toolchainMatcher.group(1));
            if (v != null) {
                result.jdkVersion = v;
                result.detectionSource = "toolchain in build.gradle";
                return;
            }
        }

        Matcher javaVersionMatcher = GRADLE_JAVA_VERSION.matcher(gradleContent);
        if (javaVersionMatcher.find()) {
            JdkVersion v = JdkVersion.fromVersionString(javaVersionMatcher.group(1));
            if (v != null) {
                result.jdkVersion = v;
                result.detectionSource = "JavaVersion in build.gradle";
                return;
            }
        }

        Matcher sourceCompatMatcher = GRADLE_SOURCE_COMPAT.matcher(gradleContent);
        if (sourceCompatMatcher.find()) {
            JdkVersion v = JdkVersion.fromVersionString(sourceCompatMatcher.group(1));
            if (v != null) {
                result.jdkVersion = v;
                result.detectionSource = "sourceCompatibility in build.gradle";
            }
        }
    }

    private void detectMavenVersion(String scmUrl, String revision, ScmDetectionResult result) {
        Optional<String> wrapperProps = scmFileAccessor
                .fetchFile(scmUrl, revision, ".mvn/wrapper/maven-wrapper.properties");
        if (wrapperProps.isPresent()) {
            Matcher m = MAVEN_WRAPPER_VERSION.matcher(wrapperProps.get());
            if (m.find()) {
                result.buildToolVersion = m.group(1);
                return;
            }
        }

        if (result.parsedPomDoc != null) {
            result.buildToolVersion = parseEnforcerMavenVersion(result.parsedPomDoc);
        }
    }

    private String parseEnforcerMavenVersion(Document doc) {
        NodeList plugins = doc.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            NodeList artifactIds = plugin.getElementsByTagName("artifactId");
            if (artifactIds.getLength() > 0
                    && "maven-enforcer-plugin".equals(artifactIds.item(0).getTextContent().trim())) {
                NodeList requireMaven = plugin.getElementsByTagName("requireMavenVersion");
                if (requireMaven.getLength() > 0) {
                    Element req = (Element) requireMaven.item(0);
                    NodeList versions = req.getElementsByTagName("version");
                    if (versions.getLength() > 0) {
                        String version = versions.item(0).getTextContent().trim();
                        version = version.replaceAll("[\\[\\]()\\s,]", "");
                        Matcher m = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)").matcher(version);
                        if (m.find()) {
                            return m.group(1);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void detectGradleVersion(String scmUrl, String revision, ScmDetectionResult result) {
        if (result.buildType != BuildType.GRADLE) {
            return;
        }
        Optional<String> wrapperProps = scmFileAccessor
                .fetchFile(scmUrl, revision, "gradle/wrapper/gradle-wrapper.properties");
        if (wrapperProps.isPresent()) {
            Matcher m = GRADLE_WRAPPER_VERSION.matcher(wrapperProps.get());
            if (m.find()) {
                result.buildToolVersion = m.group(1);
            }
        }
    }

    private void detectFromJavaVersionFile(String scmUrl, String revision, ScmDetectionResult result) {
        Optional<String> javaVersion = scmFileAccessor.fetchFile(scmUrl, revision, ".java-version");
        if (javaVersion.isEmpty()) {
            javaVersion = scmFileAccessor.fetchFile(scmUrl, revision, ".jenv/version");
        }
        if (javaVersion.isPresent()) {
            String content = javaVersion.get().trim().split("\\R")[0].trim();
            JdkVersion v = JdkVersion.fromVersionString(content);
            if (v != null) {
                result.jdkVersion = v;
                result.detectionSource = ".java-version file";
            }
        }
    }

    private void detectFromCiFiles(String scmUrl, String revision, ScmDetectionResult result) {
        Optional<String> dockerfile = scmFileAccessor.fetchFile(scmUrl, revision, "Dockerfile");
        if (dockerfile.isPresent()) {
            Matcher m = DOCKERFILE_JDK.matcher(dockerfile.get());
            if (m.find()) {
                JdkVersion v = JdkVersion.fromVersionString(m.group(1));
                if (v != null) {
                    result.jdkVersion = v;
                    result.detectionSource = "Dockerfile";
                    return;
                }
            }
        }

        Optional<String> githubCi = scmFileAccessor.fetchFile(scmUrl, revision, ".github/workflows/ci.yml");
        if (githubCi.isEmpty()) {
            githubCi = scmFileAccessor.fetchFile(scmUrl, revision, ".github/workflows/build.yml");
        }
        if (githubCi.isEmpty()) {
            githubCi = scmFileAccessor.fetchFile(scmUrl, revision, ".github/workflows/maven.yml");
        }
        if (githubCi.isPresent()) {
            Matcher m = GITHUB_ACTIONS_JAVA.matcher(githubCi.get());
            if (m.find()) {
                JdkVersion v = JdkVersion.fromVersionString(m.group(1));
                if (v != null) {
                    result.jdkVersion = v;
                    result.detectionSource = "GitHub Actions workflow";
                    return;
                }
            }
        }

        Optional<String> jenkinsfile = scmFileAccessor.fetchFile(scmUrl, revision, "Jenkinsfile");
        if (jenkinsfile.isPresent()) {
            Matcher m = JENKINSFILE_JDK.matcher(jenkinsfile.get());
            if (m.find()) {
                JdkVersion v = JdkVersion.fromVersionString(m.group(1));
                if (v != null) {
                    result.jdkVersion = v;
                    result.detectionSource = "Jenkinsfile";
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
        scmFileAccessor.close();
    }

    static class ScmDetectionResult {
        JdkVersion jdkVersion;
        BuildType buildType;
        String buildToolVersion;
        String detectionSource;
        Document parsedPomDoc;
    }
}
