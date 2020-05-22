package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;

import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.addons.runtime.CommunityDepAnalyzer;
import org.jboss.pnc.bacon.pig.impl.config.Config;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.jboss.pnc.bacon.pig.impl.utils.FileUtils.mkTempDir;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/08/2019
 */
public class QuarkusCommunityDepAnalyzer extends AddOn {
    private static final Logger log = LoggerFactory.getLogger(QuarkusCommunityDepAnalyzer.class);

    private static final Set<String> importantScopes = Sets.newHashSet("compile", "runtime");

    private static final ObjectMapper jsonMapper;
    private static final Set<String> skipped = new HashSet<>();
    public static final String NAME = "quarkusCommunityDepAnalyzer";

    private final Deliverables deliverables;

    private String repoDefinition;
    private Path repoPath;
    private String quarkusVersion;
    private Collection<String> repoZipContents;

    static {
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public QuarkusCommunityDepAnalyzer(
            Config config,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath,
            Deliverables deliverables) {
        super(config, builds, releasePath, extrasPath);
        this.deliverables = deliverables;
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    public void trigger() {
        log.info("releasePath: {}, extrasPath: {}, deliverables: {}", releasePath, extrasPath, deliverables);
        skipped.addAll(skippedExtensions());

        Path repoZipPath = Paths.get(releasePath + deliverables.getRepositoryZipName());
        unpackRepository(repoZipPath);

        String additionalRepository = (String) getConfig().get("additionalRepository");
        String settingsSelector = "";
        if (additionalRepository != null) {
            try {
                String settingsTemplate = ResourceUtils.getResourceAsString("/settings-template.xml");
                String settings = settingsTemplate.replace("ADD_REPO_URL", additionalRepository);
                File settingsFile = File.createTempFile("settings-for-dep-analysis", ".xml");
                org.apache.commons.io.FileUtils.write(settingsFile, settings, "UTF-8");
                settingsSelector = " -s " + settingsFile.getAbsolutePath();
            } catch (IOException e) {
                throw new RuntimeException("Failed to prepare settings.xml to pass the additional repository", e);
            }
        }

        Path nonReactiveProject = buildNonReactiveProject(settingsSelector);
        Path reactiveProject = buildReactiveProject(settingsSelector);

        Set<GAV> gavs = listDependencies(
                nonReactiveProject,
                Paths.get(extrasPath, "community-analysis-non-reactive-tree.txt"),
                settingsSelector);
        gavs.addAll(
                listDependencies(
                        reactiveProject,
                        Paths.get(extrasPath, "community-analysis-reactive-tree.txt"),
                        settingsSelector));

        CommunityDepAnalyzer depAnalyzer = new CommunityDepAnalyzer(gavs);

        Path targetPath = Paths.get(extrasPath, "community-dependencies.csv");
        depAnalyzer.generateAnalysis(targetPath.toAbsolutePath().toString());
    }

    private Path buildReactiveProject(String settingsSelector) {
        Path projectPath = generateQuarkusProject(artifactId -> artifactId.contains("reactive"), settingsSelector);
        buildProject(projectPath, settingsSelector);
        return projectPath;
    }

    private Path buildNonReactiveProject(String settingsSelector) {
        Path projectPath = generateQuarkusProject(artifactId -> !artifactId.contains("reactive"), settingsSelector);
        buildProject(projectPath, settingsSelector);
        return projectPath;
    }

    private Collection<String> skippedExtensions() {
        // noinspection unchecked
        Collection<String> skipped = (Collection<String>) getConfig().get("skippedExtensions");
        return skipped == null ? Collections.emptyList() : skipped;
    }

    private void unpackRepository(Path repoZipPath) {
        File unzippedRepo = mkTempDir("repoZipForDepAnalysis");
        repoZipContents = FileUtils.unzip(repoZipPath.toFile(), unzippedRepo);

        Optional<String> quarkusCore = repoZipContents.stream()
                // todo file separator for Wi***ws may be different
                .filter(file -> file.matches(".*/io/quarkus/quarkus-core/.*\\.jar"))
                .findAny();

        String quarkusCorePath = quarkusCore.orElseThrow(
                () -> new RuntimeException(
                        "Quarkus core not found in the repository, unable to determine Quarkus version"));
        // todo separators!
        String quarkusCoreDir = quarkusCorePath.substring(0, quarkusCorePath.lastIndexOf("/"));
        quarkusVersion = quarkusCoreDir.substring(quarkusCoreDir.lastIndexOf("/") + 1);

        repoPath = MavenRepositoryUtils.getContentsDirPath(unzippedRepo.toPath());
        repoDefinition = " -Dmaven.repo.local=" + repoPath;
    }

    @SneakyThrows
    private Set<GAV> listDependencies(Path projectPath, Path depThreeOut, String settingsSelector) {
        List<String> result = OSCommandExecutor
                .runCommandIn("mvn dependency:tree" + repoDefinition + settingsSelector, projectPath);

        Files.write(depThreeOut, result);
        return depTreeToNonRedhatGAVs(result);
    }

    private Set<GAV> depTreeToNonRedhatGAVs(List<String> result) {
        return result.stream()
                .filter(l -> l.startsWith("[INFO] "))
                .map(this::parseLineToGav)
                .filter(Objects::nonNull)
                .filter(GAV::isCommunity)
                .collect(Collectors.toSet());
    }

    protected GAV parseLineToGav(String mvnDepTreeLine) {
        String gavString = mvnDepTreeLine.replaceFirst("\\[INFO] [+|\\\\\\-\\s]+", "");
        String[] splitGav = gavString.split(":");
        if (splitGav.length < 5 || !importantScopes.contains(splitGav[splitGav.length - 1])) {
            return null;
        }
        switch (splitGav.length) {
            case 5:
                return new GAV(splitGav[0], splitGav[1], splitGav[3], splitGav[2]);
            case 6:
                return new GAV(splitGav[0], splitGav[1], splitGav[4], splitGav[2], splitGav[3]);
            default:
                log.warn(
                        "A suspicious line in the dependency tree '{}', assuming it's not a dependency and skipping",
                        gavString);
                return null;
        }
    }

    private void buildProject(Path projectPath, String settingsSelector) {
        log.info("building the project " + projectPath.toAbsolutePath());
        OSCommandExecutor.runCommandIn("mvn -B clean package" + repoDefinition + settingsSelector, projectPath);
    }

    private Path generateQuarkusProject(Predicate<String> artifactIdSelector, String settingsSelector) {
        Path tempProjectLocation = mkTempDir("q-dep-analysis-generated-project").toPath();
        List<String> extensionArtifactIds = findProductizedExtensions().stream()
                .filter(artifactIdSelector)
                .collect(Collectors.toList());
        String command = String.format(
                "mvn -X io.quarkus:quarkus-maven-plugin:%s:create -DprojectGroupId=tmp -DprojectArtifactId=tmp "
                        + "-DplatformArtifactId=%s -DplatformVersion=%s -Dextensions=%s" + repoDefinition
                        + settingsSelector,
                quarkusVersion,
                "quarkus-bom",
                quarkusVersion,
                String.join(",", extensionArtifactIds));
        log.info("will create project with {}", command);
        OSCommandExecutor.runCommandIn(command, tempProjectLocation);

        return tempProjectLocation.resolve("tmp");
    }

    private List<String> findProductizedExtensions() {
        List<Path> allQuarkusJars = findAllByExtension("jar");
        Set<String> extensionsJson = extractExtensionsJsonArtifactIds();
        return allQuarkusJars.stream()
                .filter(this::hasQuarkusExtensionMetadata)
                .map(this::extractArtifactId)
                .filter(extensionsJson::contains)
                .filter(ext -> !skipped.contains(ext))
                .collect(Collectors.toList());
    }

    private Set<String> extractExtensionsJsonArtifactIds() {
        List<Path> devtoolsCommonJars = findAllByExtension("json").stream()
                .filter(path -> path.endsWith(devtoolsJarName()))
                .collect(Collectors.toList());

        if (devtoolsCommonJars.size() == 1) {
            return unpackArtifactIdsFrom(devtoolsCommonJars.get(0));
        }
        throw new RuntimeException(
                "Expected a single " + devtoolsJarName() + " in the repo, found: " + devtoolsCommonJars.size());
    }

    private String devtoolsJarName() {
        return "quarkus-bom-descriptor-json-" + quarkusVersion + ".json";
    }

    private Set<String> unpackArtifactIdsFrom(Path extensionsPath) {
        Set<String> artifactIds = new HashSet<>();
        ObjectReader extensionReader = jsonMapper.readerFor(QuarkusExtensions.class);

        try (FileReader reader = new FileReader(extensionsPath.toFile())) {
            QuarkusExtensions extensions = extensionReader.readValue(reader);
            extensions.getExtensions().stream().map(QuarkusExtension::getArtifactId).forEach(artifactIds::add);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read extensions.json from " + extensionsPath, e);
        }

        return artifactIds;
    }

    private String extractArtifactId(Path path) {
        // path is a path to jar, the parent path is version
        // its parent is the artifactId
        Path artifactPath = path.getParent().getParent();
        return artifactPath.getFileName().toString();
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
