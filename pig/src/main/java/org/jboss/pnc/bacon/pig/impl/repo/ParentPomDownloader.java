package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Ken Finnigan
 */
public class ParentPomDownloader {

    private static final Logger log = LoggerFactory.getLogger(ParentPomDownloader.class);

    private ParentPomDownloader(Path repoPath) {
        this.repoPath = repoPath;
    }

    public static void addParentPoms(Path repoPath) {
        if (!Files.isDirectory(repoPath)) {
            throw new IllegalStateException("Directory expected to be present: " + repoPath);
        }

        try {
            new ParentPomDownloader(repoPath).process();
        } catch (IOException e) {
            throw new RuntimeException("Unable to download parent poms", e);
        }
    }

    private void process() throws IOException {
        // iterate until the working set is empty
        // this is to download parent POM of a parent POM of a parent POM of a parent POM of a ...
        do {
            toDownload.clear();
            doProcess();
        } while (!toDownload.isEmpty());
    }

    private void doProcess() throws IOException {
        try (Stream<Path> stream = Files.walk(repoPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(ParentPomDownloader::isPom)
                    .map(Pom::new)
                    .filter(Pom::hasParent)
                    .forEach(p -> processPom(repoPath, p));
        }

        toDownload.parallelStream().map(PomGAV::toGav).forEach(this::download);
    }

    private void download(GAV gav) {
        log.info("Downloading missing parent POM {}", gav);
        ExternalArtifactDownloader.downloadExternalArtifact(gav, repoPath, false);
    }

    private void processPom(Path repoPath, Pom pom) {
        PomGAV coords = parentCoordinates(pom);
        if (!coords.version.contains("redhat")) {
            // community parent POM not required
            return;
        }

        Path parentDir = artifactDir(repoPath, coords);
        Path parentPomPath = parentDir.resolve(coords.artifactId + "-" + coords.version + ".pom");

        if (!alreadyChecked.contains(coords)) {
            if (!Files.isRegularFile(parentPomPath)) {
                // File missing
                log.debug("Will download {} because it's a parent of {}", coords, pom.path);
                toDownload.add(coords);
            }

            alreadyChecked.add(coords);
        }
    }

    private static boolean isPom(Path path) {
        return path.toString().endsWith(".pom");
    }

    private static Path artifactDir(Path repoPath, PomGAV coords) {
        Path groupDir = repoPath.resolve(coords.groupId.replace('.', File.separatorChar));
        Path artifactDir = groupDir.resolve(coords.artifactId);
        return artifactDir.resolve(coords.version);
    }

    private PomGAV parentCoordinates(Pom pom) {
        try {
            Document pomDoc = pom.parse();

            return new PomGAV(
                    parentGroupIdExpression().evaluate(pomDoc),
                    parentArtifactIdExpression().evaluate(pomDoc),
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

    private final Set<PomGAV> alreadyChecked = new HashSet<>();

    private final Set<PomGAV> toDownload = new HashSet<>();

    private final Path repoPath;

    private XPath xpath;

    private XPathExpression parentGroupIdExpression;

    private XPathExpression parentArtifactIdExpression;

    private XPathExpression parentVersionExpression;

    private static class Pom {
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

    private static class PomGAV {
        private final String groupId;

        private final String artifactId;

        private final String version;

        PomGAV(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public String toString() {
            return groupId + ':' + artifactId + ':' + version + ":pom";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof PomGAV))
                return false;
            PomGAV pomGAV = (PomGAV) o;
            return Objects.equals(groupId, pomGAV.groupId) && Objects.equals(artifactId, pomGAV.artifactId)
                    && Objects.equals(version, pomGAV.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId, version);
        }

        public GAV toGav() {
            return new GAV(groupId, artifactId, version, "pom");
        }
    }
}
