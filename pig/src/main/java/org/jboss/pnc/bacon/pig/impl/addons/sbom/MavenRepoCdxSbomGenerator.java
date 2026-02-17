package org.jboss.pnc.bacon.pig.impl.addons.sbom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.util.BomUtils;
import org.jboss.pnc.bacon.pig.impl.addons.cachi2.Cachi2LockfileGenerator;
import org.jboss.pnc.bacon.pig.impl.repo.visitor.ArtifactVisit;
import org.jboss.pnc.bacon.pig.impl.repo.visitor.VisitableArtifactRepository;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

import io.quarkus.fs.util.ZipUtils;
import io.quarkus.paths.PathTree;

public class MavenRepoCdxSbomGenerator {

    private static final Logger log = LoggerFactory.getLogger(MavenRepoCdxSbomGenerator.class);

    public static final String DEFAULT_OUTPUT_FILENAME = "maven-repository-cyclonedx.json";
    private static final String FORMAT_BASE = "[%s/%s %.1f%%] ";
    private static final String SBOM_ADDED = "SBOM added ";
    private static final String SBOM_SKIPPED_DUPLICATE = "SBOM skipped duplicate ";

    private Path outputDir;
    private String outputFileName;
    private Path outputFile;
    private VisitableArtifactRepository repository;
    private List<Path> repositoryLocations = List.of();
    private String schemaVersion;

    /**
     * Set the output directory. If not set, the default value will be the current user directory. The output file will
     * be created in the output directory with the name configured with {@link #setOutputFileName(String)} or its
     * default value {@link #DEFAULT_OUTPUT_FILENAME} unless the target output file was configured with
     * {@link #setOutputFile(Path)}, in which case the output directory value and the file name will be ignored.
     *
     * @param outputDir output directory
     * @return this instance
     */
    public MavenRepoCdxSbomGenerator setOutputDirectory(Path outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /**
     * Set the output file name. The output file will be created in the output directory with the name configured with
     * the configured file name or its default value {@link #DEFAULT_OUTPUT_FILENAME} unless the target output file was
     * configured with {@link #setOutputFile(Path)}, in which case the output directory value and the file name will be
     * ignored.
     *
     * @param outputFileName output file name
     * @return this instance
     */
    public MavenRepoCdxSbomGenerator setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
        return this;
    }

    /**
     * Sets the output file. If an output file is configured then values set with {@link #setOutputDirectory(Path)} and
     * {@link #setOutputFileName(String)} will be ignored.
     *
     * @param outputFile output file
     * @return this instance
     */
    public MavenRepoCdxSbomGenerator setOutputFile(Path outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    /**
     * Maven repository that implements the visitor pattern for its artifacts. If a Maven repository is configured with
     * this method and {@link #addMavenRepository(Path)} the artifacts from all the Maven repositories will be collected
     * in a single SBOM file.
     *
     * @param mavenRepository visitable Maven repository
     * @return this instance
     */
    public MavenRepoCdxSbomGenerator setMavenRepository(VisitableArtifactRepository mavenRepository) {
        this.repository = mavenRepository;
        return this;
    }

    /**
     * Path to a local Maven repository to generate an SBOM file for. The path can point to a directory or a ZIP file.
     * In
     * case {@link #setMavenRepository(VisitableArtifactRepository)} is also called, the artifacts from all repositories
     * will be collected in a single SBOM file.
     *
     * @param mavenRepo path to a local Maven repository
     * @return this instance
     */
    public MavenRepoCdxSbomGenerator addMavenRepository(Path mavenRepo) {
        if (this.repositoryLocations.isEmpty()) {
            this.repositoryLocations = new ArrayList<>(1);
        }
        this.repositoryLocations.add(mavenRepo);
        return this;
    }

    /**
     * Sets the desired CycloneDX schema version.
     *
     * @param schemaVersion CycloneDX schema version
     * @return this instance
     */
    public MavenRepoCdxSbomGenerator setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    private Path getOutputFile() {
        if (outputFile != null) {
            return outputFile;
        }

        return (outputDir == null ? Path.of("") : outputDir)
                .resolve(outputFileName == null ? DEFAULT_OUTPUT_FILENAME : outputFileName);
    }

    private Version getSchemaVersion() {
        if (schemaVersion == null) {
            return getLatestSupportedSchemaVersion();
        }
        for (var version : Version.values()) {
            if (version.getVersionString().equals(schemaVersion)) {
                return version;
            }
        }
        var sb = new StringBuilder()
                .append("Desired schema version ")
                .append(schemaVersion)
                .append(" was not found among supported versions: ");
        for (int i = 0; i < Version.values().length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Version.values()[i].getVersionString());
        }
        throw new IllegalArgumentException(sb.toString());
    }

    public static Version getLatestSupportedSchemaVersion() {
        return Version.values()[Version.values().length - 1];
    }

    public void generate() {
        log.info("Generating SBOM");
        var start = System.currentTimeMillis();
        Path outputFile = getOutputFile();
        persistSbom(generateSbom(), outputFile, "json");
        logDone(outputFile, System.currentTimeMillis() - start);
    }

    private void collectArtifacts(Map<String, Component> components) {
        if (repository != null) {
            collectComponents(repository, components);
        } else if (repositoryLocations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Neither visitable Maven repository nor Maven repository paths were configured");
        }
        for (var repositoryLocation : repositoryLocations) {
            if (Files.isDirectory(repositoryLocation)) {
                collectComponents(VisitableArtifactRepository.of(repositoryLocation), components);
            } else {
                try (FileSystem fs = ZipUtils.newFileSystem(repositoryLocation)) {
                    collectComponents(VisitableArtifactRepository.of(fs.getPath("")), components);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private void collectComponents(VisitableArtifactRepository mavenRepo, Map<String, Component> components) {
        final Phaser phaser = new Phaser(1);
        final Collection<Exception> errors = new ConcurrentLinkedDeque<>();
        final AtomicInteger artifactCounter = new AtomicInteger();
        mavenRepo.visit(visit -> {
            if (components.containsKey(visit.getGav().toGapvc())) {
                logProcessedArtifact(
                        SBOM_SKIPPED_DUPLICATE,
                        visit.getGav(),
                        artifactCounter,
                        mavenRepo.getArtifactsTotal());
            } else {
                phaser.register();
                CompletableFuture.runAsync(() -> {
                    try {
                        components.put(visit.getGav().toGapvc(), getComponent(visit));
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                    logProcessedArtifact(
                            SBOM_ADDED,
                            visit.getGav(),
                            artifactCounter,
                            mavenRepo.getArtifactsTotal());
                });
            }
        });
        phaser.arriveAndAwaitAdvance();
        assertNoErrors(errors);
    }

    private Component getComponent(ArtifactVisit artifactVisit) {
        var comp = new Component();
        initMavenComponent(artifactVisit.getGav(), comp);
        final Map<String, String> collectedHashes = artifactVisit.getChecksums();
        if (!collectedHashes.isEmpty()) {
            final List<Hash> hashes = new ArrayList<>(collectedHashes.size());
            for (var hash : collectedHashes.entrySet()) {
                hashes.add(new Hash(getHashAlgorithm(hash.getKey()), hash.getValue()));
            }
            comp.setHashes(hashes);
        }
        return comp;
    }

    private void persistSbom(Bom bom, Path sbomFile, String format) {

        var specVersion = getSchemaVersion();
        final String sbomContent;
        if (format.equalsIgnoreCase("json")) {
            try {
                sbomContent = BomGeneratorFactory.createJson(specVersion, bom).toJsonString();
            } catch (Throwable e) {
                throw new RuntimeException("Failed to generate an SBOM in JSON format", e);
            }
        } else if (format.equalsIgnoreCase("xml")) {
            try {
                sbomContent = BomGeneratorFactory.createXml(specVersion, bom).toXmlString();
            } catch (GeneratorException e) {
                throw new RuntimeException("Failed to generate an SBOM in XML format", e);
            }
        } else {
            throw new RuntimeException(
                    "Unsupported SBOM artifact type " + format + ", supported types are json and xml");
        }

        var outputDir = sbomFile.getParent();
        if (outputDir != null) {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("SBOM Content:{}{}", System.lineSeparator(), sbomContent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(sbomFile)) {
            writer.write(sbomContent);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write to " + sbomFile, e);
        }
    }

    private Bom generateSbom() {
        final Bom sbom = new Bom();
        sbom.setMetadata(new Metadata());
        addToolInfo(sbom);
        sbom.setComponents(getSortedComponents());
        return sbom;
    }

    private @NonNull List<Component> getSortedComponents() {
        final Map<String, Component> components = new ConcurrentHashMap<>();
        collectArtifacts(components);
        final List<String> componentKeys = new ArrayList<>(components.keySet());
        Collections.sort(componentKeys);
        final List<Component> componentList = new ArrayList<>(componentKeys.size());
        for (String key : componentKeys) {
            componentList.add(components.get(key));
        }
        return componentList;
    }

    private void addToolInfo(Bom bom) {

        var toolLocation = getToolLocation();
        if (toolLocation == null) {
            return;
        }
        List<Hash> hashes = null;
        if (!Files.isDirectory(toolLocation)) {
            try {
                hashes = BomUtils.calculateHashes(toolLocation.toFile(), getSchemaVersion());
            } catch (IOException e) {
                throw new RuntimeException("Failed to calculate hashes for the tool at " + toolLocation, e);
            }
        } else {
            log.warn("skipping tool hashing because " + toolLocation + " appears to be a directory");
        }

        if (getSchemaVersion().getVersion() >= 1.5) {
            final ToolInformation toolInfo = new ToolInformation();
            final Component toolComponent = new Component();
            toolComponent.setType(Component.Type.LIBRARY);
            var coords = getMavenArtifact(toolLocation);
            if (coords != null) {
                initMavenComponent(coords, toolComponent);
            } else {
                toolComponent.setName(toolLocation.getFileName().toString());
            }
            if (hashes != null) {
                toolComponent.setHashes(hashes);
            }
            toolInfo.setComponents(List.of(toolComponent));
            bom.getMetadata().setToolChoice(toolInfo);
        } else {
            var tool = new Tool();
            var coords = getMavenArtifact(toolLocation);
            if (coords != null) {
                tool.setVendor(coords.getGroupId());
                tool.setName(coords.getArtifactId());
                tool.setVersion(coords.getVersion());
            } else {
                tool.setName(toolLocation.getFileName().toString());
            }
            if (hashes != null) {
                tool.setHashes(hashes);
            }
            bom.getMetadata().setTools(List.of(tool));
        }
    }

    private Path getToolLocation() {
        var cs = getClass().getProtectionDomain().getCodeSource();
        if (cs == null) {
            log.warn("Failed to determine code source of the tool");
            return null;
        }
        var url = cs.getLocation();
        if (url == null) {
            log.warn("Failed to determine code source URL of the tool");
            return null;
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            log.warn("Failed to translate " + url + " to a file system path", e);
            return null;
        }
    }

    private static GAV getMavenArtifact(Path toolLocation) {
        final List<GAV> toolArtifact = new ArrayList<>(1);
        PathTree.ofDirectoryOrArchive(toolLocation).walkIfContains("META-INF/maven", visit -> {
            if (!Files.isDirectory(visit.getPath())
                    && visit.getPath().getFileName().toString().equals("pom.properties")) {
                try (BufferedReader reader = Files.newBufferedReader(visit.getPath())) {
                    var props = new Properties();
                    props.load(reader);
                    final String groupId = props.getProperty("groupId");
                    if (isBlanc(groupId)) {
                        return;
                    }
                    final String artifactId = props.getProperty("artifactId");
                    if (isBlanc(artifactId)) {
                        return;
                    }
                    final String version = props.getProperty("version");
                    if (isBlanc(version)) {
                        return;
                    }
                    toolArtifact.add(new GAV(groupId, artifactId, version, "jar"));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        if (toolArtifact.size() != 1) {
            return null;
        }
        return toolArtifact.get(0);
    }

    private static boolean isBlanc(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void initMavenComponent(GAV coords, Component c) {
        c.setGroup(coords.getGroupId());
        c.setName(coords.getArtifactId());
        c.setVersion(coords.getVersion());
        final PackageURL purl = getPackageURL(coords);
        c.setPurl(purl);
        c.setBomRef(purl.toString());
        c.setType(Component.Type.LIBRARY);
    }

    private static PackageURL getPackageURL(GAV gav) {
        final TreeMap<String, String> qualifiers = new TreeMap<>();
        qualifiers.put("type", gav.getPackaging());
        if (gav.getClassifier() != null && !gav.getClassifier().isEmpty()) {
            qualifiers.put("classifier", gav.getClassifier());
        }

        final PackageURL purl;
        try {
            purl = new PackageURL(
                    PackageURL.StandardTypes.MAVEN,
                    gav.getGroupId(),
                    gav.getArtifactId(),
                    gav.getVersion(),
                    qualifiers,
                    null);
        } catch (MalformedPackageURLException e) {
            throw new RuntimeException("Failed to generate Purl for " + gav, e);
        }
        return purl;
    }

    private static void logDone(Path lockfileYaml, long totalMs) {
        var secTotal = totalMs / 1000;
        var minTotal = secTotal / 60;
        var hoursTotal = minTotal / 60;
        var sb = new StringBuilder().append("Generated CycloneDX SBoM file ").append(lockfileYaml).append(" in ");
        boolean appendUnit = hoursTotal > 0;
        if (appendUnit) {
            sb.append(hoursTotal).append("h ");
        }
        appendUnit |= minTotal > 0;
        if (appendUnit) {
            sb.append(minTotal - hoursTotal * 60).append("min ");
        }
        appendUnit |= secTotal > 0;
        if (appendUnit) {
            sb.append(secTotal - minTotal * 60).append("sec ");
        }
        sb.append(totalMs - secTotal * 1000).append("ms");
        log.info(sb.toString());
    }

    private void logProcessedArtifact(String prefix, GAV artifact, AtomicInteger artifactCounter, int artifactsTotal) {
        var sb = new StringBuilder(180);
        var formatter = new Formatter(sb);
        var artifactIndex = artifactCounter.incrementAndGet();
        final double percents = ((double) artifactIndex * 100) / artifactsTotal;
        formatter.format(FORMAT_BASE, artifactIndex, artifactsTotal, percents);
        sb.append(prefix).append(artifact.toGapvc());
        log.info(sb.toString());
    }

    private static void assertNoErrors(Collection<Exception> errors) {
        if (!errors.isEmpty()) {
            var sb = new StringBuilder("The following errors were encountered while querying for artifact info:");
            log.error(sb.toString());
            var i = 1;
            for (var error : errors) {
                var prefix = i++ + ")";
                log.error(prefix, error);
                sb.append(System.lineSeparator()).append(prefix).append(" ").append(error.getLocalizedMessage());
                for (var e : error.getStackTrace()) {
                    sb.append(System.lineSeparator());
                    for (int j = 0; j < prefix.length(); ++j) {
                        sb.append(" ");
                    }
                    sb.append("at ").append(e);
                    if (e.getClassName().contains(Cachi2LockfileGenerator.class.getName())) {
                        sb.append(System.lineSeparator());
                        for (int j = 0; j < prefix.length(); ++j) {
                            sb.append(" ");
                        }
                        sb.append("...");
                        break;
                    }
                }
            }
            throw new RuntimeException(sb.toString());
        }
    }

    private Hash.Algorithm getHashAlgorithm(String extension) {
        return Hash.Algorithm.fromSpec(toOfficialHashAlgName(extension));
    }

    private static String toOfficialHashAlgName(String extension) {
        return switch (extension) {
            case "sha1" -> "SHA-1";
            case "sha256" -> "SHA-256";
            case "sha384" -> "SHA-384";
            case "sha512" -> "SHA-512";
            default -> extension;
        };
    }
}
