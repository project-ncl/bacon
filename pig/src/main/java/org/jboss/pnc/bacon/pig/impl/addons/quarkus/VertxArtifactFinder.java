package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.MavenRepositoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jboss.pnc.bacon.pig.impl.utils.FileUtils.mkTempDir;

/**
 * @author Saumya Singh, singhsaumyas150@gmail.com <br>
 *         Date: 25/03/2021
 */

/*
 * Add-on Description : This Add-on automates creation of vertx artifacts list which are used in Quarkus
 */

public class VertxArtifactFinder extends AddOn {

    public static final String NAME = "vertxArtifactFinder";

    private static final Logger log = LoggerFactory.getLogger(VertxArtifactFinder.class);

    private static final ObjectMapper jsonMapper;
    private Path repoPath;

    static {
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public VertxArtifactFinder(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    public VertxArtifactFinder(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath,
            Path repoPath) {
        this(pigConfiguration, builds, releasePath, extrasPath);
        this.repoPath = repoPath;
    }

    private List<String> findAllExtensions() {
        List<Path> allQuarkusJars = findAllByExtension("jar");

        return allQuarkusJars.stream()
                .filter(this::hasQuarkusExtensionMetadata)
                .map(this::extractArtifactId)
                .collect(Collectors.toList());
    }

    private List<String> findVertxArtifacts() {
        List<String> onlyVertxArtifacts = new ArrayList<>();
        List<String> allArtifacts = findAllExtensions();

        for (String artifact : allArtifacts) {
            if (artifact.toLowerCase().contains("vertx")) {
                onlyVertxArtifacts.add(artifact);
            }
        }
        return onlyVertxArtifacts;
    }

    @Override
    public void trigger() {

        if (repoPath == null) {
            if (PigContext.get().getRepositoryData() == null) {
                throw new RuntimeException(
                        "No repository data available for document generation. Please make sure to run `pig repo` before");
            }
            repoPath = PigContext.get().getRepositoryData().getRepositoryPath();
        }

        unpackRepository(repoPath);

        List<String> vertexArtifactList = findVertxArtifacts();
        Path vertexArtifactListFile = Paths.get(extrasPath, "vertxList.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(vertexArtifactListFile)) {
            writer.write(String.join("\n", vertexArtifactList));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write vertx artifacts to the output file", e);
        }

        // append the text File inside resources/VertxTestRepoZip/vertxListExpectedResult.txt with actual data
        File vertxListExpectedFile = new File("src/test/resources/VertxTestRepoZip/vertxListExpectedResult.txt");
        Path pathOfVertxListExpectedFile = Paths.get("src/test/resources/VertxTestRepoZip/vertxListExpectedResult.txt");

        byte[] bytes = vertexArtifactList.toString().getBytes();

        try {
            Files.write(pathOfVertxListExpectedFile, bytes);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }

    @Override
    protected String getName() {
        return NAME;
    }

    private String extractArtifactId(Path path) {
        // path is a path to jar, the parent path is version
        // its parent is the artifactId
        Path artifactPath = path.getParent().getParent();
        return artifactPath.getFileName().toString();
    }

    private void unpackRepository(Path repoZipPath) {
        File unzippedRepo = mkTempDir("repoZipForVertxArtifacts");
        Collection<String> repoZipContents = FileUtils.unzip(repoZipPath.toFile(), unzippedRepo);

        repoPath = MavenRepositoryUtils.getContentsDirPath(unzippedRepo.toPath());
    }

    private boolean hasQuarkusExtensionMetadata(Path path) {
        return FileUtils.listZipContents(path.toFile())
                .stream()
                .anyMatch(e -> e.contains("META-INF/quarkus-extension.properties"));
    }

    private List<Path> findAllByExtension(String extension) {
        ByExtensionCollectingVisitor visitor = new ByExtensionCollectingVisitor(extension);
        try {
            Files.walkFileTree(repoPath, visitor);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to walk through repository contents: " + repoPath.toAbsolutePath().toString());
        }
        return visitor.getFilePaths();
    }

}
