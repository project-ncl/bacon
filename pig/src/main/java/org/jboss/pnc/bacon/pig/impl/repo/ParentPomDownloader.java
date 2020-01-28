package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ken Finnigan
 */
public class ParentPomDownloader {

    public static final Logger log = LoggerFactory.getLogger(ParentPomDownloader.class);

    private ParentPomDownloader() {
    }

    public static void addParentPoms(Path repoPath) {
        if (!Files.isDirectory(repoPath)) {
            throw new IllegalStateException("Directory expected to be present: " + repoPath);
        }

        try {
            new ParentPomDownloader().process(repoPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to download parent poms", e);
        }
    }

    private void process(final Path repoPath) throws IOException {
        Set<Pom> pomArtifacts = retrievePoms(repoPath);

        File execDir = FileUtils.mkTempDir("parent-pom-retrieval");

        pomArtifacts.stream().filter(Pom::hasParent).forEach(p -> processPoms(repoPath, p));

        String settingsXml = ResourceUtils.extractToTmpFile("/indy-settings.xml", "settings", ".xml").getAbsolutePath();
        toDownload.parallelStream().forEach(gav -> {
            log.debug("Downloading: {}", gav.toString());
            // Call Maven to download dependency

            ProcessBuilder builder = new ProcessBuilder("mvn", "-B",
                    "org.apache.maven.plugins:maven-dependency-plugin:3.0.1:get", "-Dartifact=" + gav.toString(),
                    "-Dmaven.repo.local=" + repoPath.toAbsolutePath().toString(), "-s", settingsXml);

            builder.directory(execDir).inheritIO();

            Process process = null;
            try {
                process = builder.start();
            } catch (IOException e) {
                log.error("Unable to download gav {}", gav.toString(), e);
                System.out.println(14);
            }

            while (process.isAlive()) {
                try {
                    Thread.sleep(1000);
                    if (process.exitValue() == 0) {
                        break;
                    }
                } catch (IllegalThreadStateException e) {
                    // ignore as process not exited
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        );
    }

    private void processPoms(Path repoPath, Pom pom) {
        PomGAV coords = parentCoordinates(pom);
        if (!coords.version.contains("redhat")) {
            // community parent POM not required
            return;
        }

        Path parentDir = artifactDir(repoPath, coords);
        Path parentPomPath = parentDir.resolve(coords.artifactId() + "-" + coords.version() + ".pom");

        if (!alreadyChecked.contains(coords)) {
            if (!Files.isRegularFile(parentPomPath)) {
                // File missing
                toDownload.add(coords);
            }

            alreadyChecked.add(coords);
        }
    }

    private Set<Pom> retrievePoms(Path repoPath) throws IOException {
        return Files.walk(repoPath).filter(Files::isRegularFile).filter(ParentPomDownloader::isPom).map(Pom::new)
                .collect(Collectors.toSet());
    }

    private static boolean isPom(Path path) {
        return path.toString().endsWith(".pom");
    }

    private static Path artifactDir(Path repoPath, PomGAV coords) {
        Path groupDir = repoPath.resolve(coords.groupId().replace('.', File.separatorChar));
        Path artifactDir = groupDir.resolve(coords.artifactId());
        return artifactDir.resolve(coords.version());
    }

    private PomGAV parentCoordinates(Pom pom) {
        try {
            Document pomDoc = pom.parse();

            return new PomGAV(parentGroupIdExpression().evaluate(pomDoc), parentArtifactIdExpression().evaluate(pomDoc),
                    parentVersionExpression().evaluate(pomDoc));
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private XPath xPath() {
        if (xpath == null) {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            xpath = xPathFactory.newXPath();
        }
        return xpath;
    }

    private XPathExpression parentGroupIdExpression() throws XPathExpressionException {
        if (parentGroupIdExpression == null) {
            parentGroupIdExpression = xPath().compile("/project/parent/groupId");
        }
        return parentGroupIdExpression;
    }

    private XPathExpression parentArtifactIdExpression() throws XPathExpressionException {
        if (parentArtifactIdExpression == null) {
            parentArtifactIdExpression = xPath().compile("/project/parent/artifactId");
        }
        return parentArtifactIdExpression;
    }

    private XPathExpression parentVersionExpression() throws XPathExpressionException {
        if (parentVersionExpression == null) {
            parentVersionExpression = xPath().compile("/project/parent/version");
        }
        return parentVersionExpression;
    }

    private Set<PomGAV> alreadyChecked = new HashSet<>();

    private Set<PomGAV> toDownload = new HashSet<>();

    private XPath xpath;

    private XPathExpression parentGroupIdExpression;

    private XPathExpression parentArtifactIdExpression;

    private XPathExpression parentVersionExpression;

    private class Pom {
        private final Path path;

        private DocumentBuilder documentBuilder;

        Pom(Path path) {
            this.path = path;
        }

        public Path path() {
            return path;
        }

        boolean hasParent() {
            try {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).contains("<parent>");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Document parse() throws IOException, SAXException, ParserConfigurationException {
            final DocumentBuilder documentBuilder = documentBuilder();
            return documentBuilder.parse(path.toFile());
        }

        private DocumentBuilder documentBuilder() throws ParserConfigurationException {
            if (documentBuilder == null) {
                documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            }
            return documentBuilder;
        }
    }

    private class PomGAV {
        private final String groupId;

        private final String artifactId;

        private final String version;

        PomGAV(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        String groupId() {
            return groupId;
        }

        String artifactId() {
            return artifactId;
        }

        String version() {
            return version;
        }

        @Override
        public String toString() {
            return groupId + ':' + artifactId + ':' + version + ":pom";
        }
    }
}
