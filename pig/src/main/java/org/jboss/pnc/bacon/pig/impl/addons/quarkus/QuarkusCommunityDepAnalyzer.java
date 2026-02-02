package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import static org.jboss.pnc.bacon.pig.impl.utils.FileUtils.mkTempDir;

import java.io.BufferedWriter;
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.addons.runtime.CommunityDepAnalyzer;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.MavenRepositoryUtils;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/08/2019
 */
public class QuarkusCommunityDepAnalyzer extends AddOn {
    private static final Logger log = LoggerFactory.getLogger(QuarkusCommunityDepAnalyzer.class);

    private static final Set<String> importantScopes = Sets.newHashSet("compile", "runtime");

    private static final ObjectMapper jsonMapper;
    public static final String NAME = "quarkusCommunityDepAnalyzer";

    private final Deliverables deliverables;
    private final Set<String> skippedExtensions;

    private Path repoPath;
    private String quarkusVersion;
    private Collection<String> repoZipContents;

    static {
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public QuarkusCommunityDepAnalyzer(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath,
            Deliverables deliverables) {
        super(pigConfiguration, builds, releasePath, extrasPath);
        this.deliverables = deliverables;

        if (shouldRun()) {
            @SuppressWarnings("unchecked")
            Collection<String> skippedExtensions = (Collection<String>) getAddOnConfiguration()
                    .get("skippedExtensions");
            this.skippedExtensions = skippedExtensions == null ? Collections.emptySet()
                    : new HashSet<>(skippedExtensions);
        } else {
            skippedExtensions = Collections.emptySet();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private String getBomArtifactId() {
        return PigContext.get().getPigConfiguration().getFlow().getRepositoryGeneration().getBomArtifactId();
    }

    private boolean isProductBom(String bomArtifactId) {
        return bomArtifactId.equals("quarkus-product-bom");
    }

    @Override
    public void trigger() {
        log.info("releasePath: {}, extrasPath: {}, deliverables: {}", releasePath, extrasPath, deliverables);
        clearProducedFiles();

        if (PigContext.get().getRepositoryData() == null) {
            throw new RuntimeException(
                    "No repository data available for document generation. Please make sure to run `pig repo` before");
        }

        boolean tempBuild = PigContext.get().isTempBuild();
        String settingsXmlPath = Indy.getConfiguredIndySettingsXmlPath(tempBuild);

        unpackRepository(PigContext.get().getRepositoryData().getRepositoryPath());

        Multimap<GAV, GAV> dependenciesBySource = ArrayListMultimap.create();

        try {
            final MavenArtifactResolver mvnResolver = MavenArtifactResolver.builder()
                    .setUserSettings(new File(settingsXmlPath))
                    .setLocalRepository(repoPath.toAbsolutePath().toString())
                    .build();

            List<GAV> productizedExtensions = findProductizedExtensions();
            Set<GAV> dependencies = new HashSet<>();
            for (GAV extension : productizedExtensions) {
                if (skippedExtensions.contains(extension.getArtifactId())) {
                    continue;
                }
                DependencyResult dependencyResult = mvnResolver.getSystem()
                        .resolveDependencies(
                                mvnResolver.getSession(),
                                new DependencyRequest().setCollectRequest(
                                        mvnResolver.newCollectManagedRequest(
                                                new DefaultArtifact(
                                                        extension.getGroupId(),
                                                        extension.getArtifactId(),
                                                        extension.getPackaging(),
                                                        extension.getVersion()), // runtime extension artifact
                                                List.of(), // enforced direct dependencies, ignore this
                                                List.of(), // enforced direct dependencies, ignore this
                                                List.of(), // extra maven repos, ignore this
                                                List.of(), // exclusions
                                                Set.of(JavaScopes.TEST, JavaScopes.PROVIDED) // dependency scopes that
                                                                                             // should be ignored
                                        )));

                collectNonOptionalDependencies(
                        dependencyResult.getRoot(),
                        dependencies,
                        dependenciesBySource,
                        extension,
                        new HashSet<>());
            }

            CommunityDepAnalyzer depAnalyzer = new CommunityDepAnalyzer(
                    dependencies,
                    deps -> deps.stream()
                            .map(d -> new QuarkusCommunityDependency(dependenciesBySource.get(d.getGav()), d))
                            .collect(Collectors.toList()));

            Path targetPath = Paths.get(extrasPath, "community-dependencies.csv");
            depAnalyzer.generateAnalysis(targetPath.toAbsolutePath().toString());
            recordProducedFile(targetPath);

            Set<String> problematicDeps = gatherProblematicDeps();
            Path problematicDepsOut = Paths.get(extrasPath, "nonexistent-redhat-deps.txt");

            try (BufferedWriter writer = Files.newBufferedWriter(problematicDepsOut)) {
                writer.write(String.join("\n", problematicDeps));
            } catch (IOException e) {
                throw new RuntimeException("Failed to write problematic dependencies to the output file", e);
            }
            recordProducedFile(problematicDepsOut);
        } catch (BootstrapMavenException | DependencyResolutionException e) {
            throw new RuntimeException("Failed to analyze community dependencies of Quarkus", e);
        }
    }

    private void collectNonOptionalDependencies(
            DependencyNode depNode,
            Set<GAV> dependencies,
            Multimap<GAV, GAV> dependenciesBySource,
            GAV extension,
            HashSet<GAV> visitedNodes) {
        GAV current = gavFromDepNode(depNode);
        if (!visitedNodes.add(current)) {
            return;
        }

        depNode.getChildren().stream().filter(d -> !d.getDependency().isOptional()).peek(d -> {
            GAV gav = gavFromDepNode(d);
            if (!gav.getVersion().contains("redhat")) {
                dependenciesBySource.put(gav, extension);
                dependencies.add(gav);
            }
        }).forEach(d -> collectNonOptionalDependencies(d, dependencies, dependenciesBySource, extension, visitedNodes));
    }

    private GAV gavFromDepNode(DependencyNode depNode) {
        Artifact a = depNode.getArtifact();
        GAV current = new GAV(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getExtension(), a.getClassifier());
        return current;
    }

    private void unpackRepository(Path repoZipPath) {
        File unzippedRepo = mkTempDir("repoZipForDepAnalysis");
        repoZipContents = FileUtils.unzip(repoZipPath.toFile(), unzippedRepo);

        Optional<String> quarkusCore = repoZipContents.stream()
                .filter(file -> FilenameUtils.normalize(file, true).matches(".*/io/quarkus/quarkus-core/.*\\.jar"))
                .findAny();

        String quarkusCorePath = quarkusCore.orElseThrow(
                () -> new RuntimeException(
                        "Quarkus core not found in the repository, unable to determine Quarkus version"));
        String quarkusCoreDir = quarkusCorePath.substring(0, quarkusCorePath.lastIndexOf("/"));
        quarkusVersion = quarkusCoreDir.substring(quarkusCoreDir.lastIndexOf("/") + 1);

        repoPath = MavenRepositoryUtils.getContentsDirPath(unzippedRepo.toPath());
    }

    private List<GAV> findProductizedExtensions() {
        List<Path> allQuarkusJars = findAllByExtension("jar");
        // TODO: we may have different groupIds!!
        Set<String> extensionsJson = extractExtensionsJsonArtifactIds().stream()
                .map(GAV::getArtifactId)
                .collect(Collectors.toSet());
        return allQuarkusJars.stream()
                .filter(this::hasQuarkusExtensionMetadata)
                .map(this::extractGAV)
                .filter(gav -> extensionsJson.contains(gav.getArtifactId()))
                .collect(Collectors.toList());
    }

    private Set<GAV> extractExtensionsJsonArtifactIds() {
        List<Path> devtoolsCommonJars = findAllByExtension("json").stream()
                .filter(path -> path.endsWith(devtoolsJarName()))
                .collect(Collectors.toList());
        for (Path p : devtoolsCommonJars) {
            if (p.getParent().toString().contains("com/redhat")) {
                return unpackArtifactsFrom(p);
            } else
                continue;
        }
        throw new RuntimeException(
                "Expected to find a devtools json of name " + devtoolsJarName()
                        + " with groupId starting from com.redhat in the repo, found only: " + devtoolsCommonJars);
    }

    private String devtoolsJarName() {
        String bomArtifactId = getBomArtifactId();
        if (!isProductBom(bomArtifactId)) {
            return "quarkus-bom-quarkus-platform-descriptor-" + quarkusVersion + "-" + quarkusVersion + ".json";
        } else {
            return "quarkus-product-bom-quarkus-platform-descriptor-" + quarkusVersion + "-" + quarkusVersion + ".json";
        }
    }

    private Set<GAV> unpackArtifactsFrom(Path extensionsPath) {
        Set<GAV> artifacts = new HashSet<>();
        ObjectReader extensionReader = jsonMapper.readerFor(QuarkusExtensions.class);

        try (FileReader reader = new FileReader(extensionsPath.toFile())) {
            QuarkusExtensions extensions = extensionReader.readValue(reader);
            extensions.getExtensions()
                    .stream()
                    .map(QuarkusExtension::getArtifact)
                    .map(a -> a.replaceAll("::", ":")) // TODO: that's a hack to remove empty classifier
                    .map(GAV::fromColonSeparatedGAPV)
                    .forEach(artifacts::add);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read extensions.json from " + extensionsPath, e);
        }

        return artifacts;
    }

    private GAV extractGAV(Path path) {
        // path is a path to jar, the parent path is version
        // its parent is the artifactId
        return GAV.fromFileName(path.toFile().getAbsolutePath(), repoPath.toFile().getAbsolutePath());
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

    private Set<String> gatherProblematicDeps() {
        Set<String> problemmaticDeps = new TreeSet<>(checkBomContents(".*/io/quarkus/quarkus-bom/.*\\.pom"));
        if (isProductBom(getBomArtifactId())) {
            problemmaticDeps.addAll(checkBomContents(".*/com/redhat/quarkus/quarkus-product-bom/.*\\.pom"));
        }

        if (Boolean.TRUE.equals(getAddOnConfiguration().get("checkDeploymentBoms"))) {
            problemmaticDeps.addAll(checkBomContents(".*/io/quarkus/quarkus-bom-deployment/.*\\.pom"));
            if (isProductBom(getBomArtifactId())) {
                problemmaticDeps
                        .addAll(checkBomContents(".*/com/redhat/quarkus/quarkus-product-bom-deployment/.*\\.pom"));
            }
        }
        return problemmaticDeps;
    }

    private Collection<String> checkBomContents(String bomLocator) {
        String quarkusRuntimeBom = repoZipContents.stream()
                .filter(file -> FilenameUtils.normalize(file, true).matches(bomLocator))
                .findAny()
                .get();
        return checkReferencesInRepo(quarkusRuntimeBom);
    }

    private Collection<String> checkReferencesInRepo(String quarkusRuntimeBom) {
        try {
            String str = "/maven-repository/";
            int repoDirIdx = quarkusRuntimeBom.indexOf(str);
            quarkusRuntimeBom = quarkusRuntimeBom.substring(repoDirIdx + str.length());
            Path bomFile = repoPath.resolve(quarkusRuntimeBom).toAbsolutePath();
            Model model = new MavenXpp3Reader().read(Files.newInputStream(bomFile));

            List<Dependency> dependencies = model.getDependencyManagement().getDependencies();

            return dependencies.stream()
                    .filter(dep -> isRHAndMissing(dep, model))
                    .map(
                            dep -> String.format(
                                    "'%s:%s:%s:%s',",
                                    dep.getGroupId(),
                                    dep.getArtifactId(),
                                    deVar(model, dep.getVersion()),
                                    dep.getClassifier() != null ? dep.getClassifier() : ""))
                    .collect(Collectors.toSet());
        } catch (XmlPullParserException | IOException e) {
            log.error("Parsing error when generating quarkus artifact references", e);
            return Collections.emptySet();
        }
    }

    private boolean isRHAndMissing(Dependency dependency, Model model) {
        String version = dependency.getVersion();
        version = deVar(model, version);
        if (!version.contains("redhat")) {
            return false;
        }
        GAV gav = new GAV(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                version,
                dependency.getType() != null && dependency.getType().equals("jar") ? dependency.getType() : "jar",
                dependency.getClassifier() != null ? dependency.getClassifier() : null);
        Path filePath = repoPath.resolve(gav.toVersionPath()).resolve(gav.toFileName());
        return !filePath.toFile().exists();
    }

    private String deVar(Model model, String version) {
        if (version.startsWith("$")) {
            version = version.substring(2, version.length() - 1);
            version = model.getProperties().getProperty(version);
        }
        return version;
    }
}
