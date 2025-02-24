package org.jboss.pnc.bacon.pig.impl.addons.cachi2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.pnc.bacon.pig.impl.repo.visitor.ArtifactVisit;
import org.jboss.pnc.bacon.pig.impl.repo.visitor.VisitableArtifactRepository;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.fs.util.ZipUtils;

/**
 * Cachi2 lock file generator
 */
public class Cachi2LockfileGenerator {

    private static final Logger log = LoggerFactory.getLogger(Cachi2LockfileGenerator.class);

    private static final String MAVEN_RESPOSITORY_DIR = "maven-repository/";
    private static final String FORMAT_BASE = "[%s/%s %.1f%%] ";
    private static final String CACHI_2_LOCKFILE_ADDED = "Cachi2 lockfile added ";
    private static final String CACHI_2_LOCKFILE_SKIPPED_DUPLICATE = "Cachi2 lockfile skipped duplicate ";
    private static final String SHA = "sha";
    public static final String DEFAULT_OUTPUT_FILENAME = "artifacts.lock.yaml";
    public static final String DEFAULT_REPOSITORY_URL = Indy.getIndyUrl();

    public static Cachi2LockfileGenerator newInstance() {
        return new Cachi2LockfileGenerator();
    }

    private Path outputDir;
    private String outputFileName;
    private Path outputFile;
    private VisitableArtifactRepository repository;
    private List<Path> repositoryLocations = List.of();
    private String defaultRepositoryUrl = DEFAULT_REPOSITORY_URL;
    private String preferredChecksumAlg;

    /**
     * Set the output directory. If not set, the default value will be the current user directory. The output file will
     * be created in the output directory with the name configured with {@link #setOutputFileName(String)} or its
     * default value {@link #DEFAULT_OUTPUT_FILENAME} unless the target output file was configured with
     * {@link #setOutputFile(Path)}, in which case the output directory value and the file name will be ignored.
     *
     * @param outputDir output directory
     * @return this instance
     */
    public Cachi2LockfileGenerator setOutputDirectory(Path outputDir) {
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
    public Cachi2LockfileGenerator setOutputFileName(String outputFileName) {
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
    public Cachi2LockfileGenerator setOutputFile(Path outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    /**
     * Maven repository that implements the visitor pattern for its artifacts. If a Maven repository is configured with
     * this method and {@link #addMavenRepository(Path)} the artifacts from all the Maven repositories will be collected
     * in a single lock file.
     *
     * @param mavenRepository visitable Maven repository
     * @return this instance
     */
    public Cachi2LockfileGenerator setMavenRepository(VisitableArtifactRepository mavenRepository) {
        this.repository = mavenRepository;
        return this;
    }

    /**
     * Path to a local Maven repository to generate a lock file for. The path can point to a directory or a ZIP file. In
     * case {@link #setMavenRepository(VisitableArtifactRepository)} is also called, the artifacts from all repositories
     * will be collected in a single lock file.
     *
     * @param mavenRepo path to a local Maven repository
     * @return this instance
     */
    public Cachi2LockfileGenerator addMavenRepository(Path mavenRepo) {
        if (this.repositoryLocations.isEmpty()) {
            this.repositoryLocations = new ArrayList<>(1);
        }
        this.repositoryLocations.add(mavenRepo);
        return this;
    }

    /**
     * Sets the default Maven repository URL, which will be used in case PNC information is not available for an
     * artifact.
     *
     * @param defaultMavenRepositoryUrl default Maven repository URL for artifacts
     * @return this instance
     */
    public Cachi2LockfileGenerator setDefaultMavenRepositoryUrl(String defaultMavenRepositoryUrl) {
        this.defaultRepositoryUrl = defaultMavenRepositoryUrl;
        return this;
    }

    /**
     * Preferred checksum algorithm to include in the generated lock file. If not configured, the strongest available
     * SHA version will be used.
     *
     * @param preferredChecksumAlg preferred checksum algorithm to include in the generated lock file
     */
    public void setPreferredChecksumAlg(String preferredChecksumAlg) {
        this.preferredChecksumAlg = preferredChecksumAlg;
    }

    private Path getOutputFile() {
        if (outputFile != null) {
            return outputFile;
        }

        return (outputDir == null ? Path.of("") : outputDir)
                .resolve(outputFileName == null ? DEFAULT_OUTPUT_FILENAME : outputFileName);
    }

    public void generate() {
        log.info("Generating Cachi2 lockfile");
        var start = System.currentTimeMillis();
        final Path lockfileYaml = persistLockfile(generateLockfile());
        logDone(lockfileYaml, System.currentTimeMillis() - start);
    }

    private Path persistLockfile(Cachi2Lockfile lockfile) {
        final Path lockfileYaml = getOutputFile();
        log.debug("Persisting the lock file to {}", lockfileYaml);
        Cachi2Lockfile.persistTo(lockfile, lockfileYaml);
        return lockfileYaml;
    }

    private Cachi2Lockfile generateLockfile() {
        var arr = collectArtifacts();
        Arrays.sort(arr, Comparator.comparing(Cachi2Lockfile.Cachi2Artifact::getFilename));
        final Cachi2Lockfile lockfile = new Cachi2Lockfile();
        lockfile.setMetadata(Map.of("version", "1.0"));
        lockfile.setArtifacts(List.of(arr));
        return lockfile;
    }

    private Cachi2Lockfile.Cachi2Artifact[] collectArtifacts() {
        final Map<String, Cachi2Lockfile.Cachi2Artifact> cachi2Artifacts = new ConcurrentHashMap<>();
        collectArtifacts(cachi2Artifacts);
        return cachi2Artifacts.values().toArray(new Cachi2Lockfile.Cachi2Artifact[0]);
    }

    private void collectArtifacts(Map<String, Cachi2Lockfile.Cachi2Artifact> cachi2Artifacts) {
        if (repository != null) {
            generateLockfile(repository, cachi2Artifacts);
        } else if (repositoryLocations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Neither visitable Maven repository nor Maven repository paths were configured");
        }
        for (var repositoryLocation : repositoryLocations) {
            if (Files.isDirectory(repositoryLocation)) {
                generateLockfile(VisitableArtifactRepository.of(repositoryLocation), cachi2Artifacts);
            } else {
                try (FileSystem fs = ZipUtils.newFileSystem(repositoryLocation)) {
                    generateLockfile(VisitableArtifactRepository.of(fs.getPath("")), cachi2Artifacts);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private void generateLockfile(
            VisitableArtifactRepository mavenRepo,
            Map<String, Cachi2Lockfile.Cachi2Artifact> cachi2Artifacts) {
        final Phaser phaser = new Phaser(1);
        final Collection<Exception> errors = new ConcurrentLinkedDeque<>();
        final AtomicInteger artifactCounter = new AtomicInteger();
        mavenRepo.visit(visit -> {
            if (cachi2Artifacts.containsKey(visit.getGav().toGapvc())) {
                logProcessedArtifact(
                        CACHI_2_LOCKFILE_SKIPPED_DUPLICATE,
                        visit.getGav(),
                        artifactCounter,
                        mavenRepo.getArtifactsTotal());
            } else {
                phaser.register();
                CompletableFuture.runAsync(() -> {
                    final Cachi2Lockfile.Cachi2Artifact ca = new Cachi2Lockfile.Cachi2Artifact();
                    ca.setType("maven");
                    try {
                        ca.setFilename(MAVEN_RESPOSITORY_DIR + visit.getGav().toUri());
                        addNonPncArtifact(visit, ca);
                        cachi2Artifacts.put(visit.getGav().toGapvc(), ca);
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                    logProcessedArtifact(
                            CACHI_2_LOCKFILE_ADDED,
                            visit.getGav(),
                            artifactCounter,
                            mavenRepo.getArtifactsTotal());
                });
            }
        });
        phaser.arriveAndAwaitAdvance();
        assertNoErrors(errors);
    }

    private static void logDone(Path lockfileYaml, long totalMs) {
        var secTotal = totalMs / 1000;
        var minTotal = secTotal / 60;
        var hoursTotal = minTotal / 60;
        var sb = new StringBuilder().append("Generated Cachi2 lock file ").append(lockfileYaml).append(" in ");
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

    private void addNonPncArtifact(ArtifactVisit artifactVisit, Cachi2Lockfile.Cachi2Artifact cachi2Artifact) {
        var gav = artifactVisit.getGav();
        cachi2Artifact.setGroupId(gav.getGroupId());
        cachi2Artifact.setArtifactId(gav.getArtifactId());
        if (gav.getClassifier() != null && !gav.getClassifier().isEmpty()) {
            cachi2Artifact.setClassifier(gav.getClassifier());
        }
        cachi2Artifact.setArtifactType(gav.getPackaging());
        cachi2Artifact.setVersion(gav.getVersion());
        cachi2Artifact.setRepositoryUrl(defaultRepositoryUrl);
        var checksums = artifactVisit.getChecksums();
        if (!checksums.isEmpty()) {
            String checksum = null;
            if (preferredChecksumAlg != null) {
                checksum = checksums.get(preferredChecksumAlg);
                if (checksum != null) {
                    checksum = preferredChecksumAlg + ":" + checksum;
                }
            }
            if (checksum == null) {
                checksum = getPreferredChecksum(checksums);
            }
            cachi2Artifact.setChecksum(checksum);
        }
    }

    /**
     * Selects the preferred checksum out of the available ones. The current implementation will look for an SHA
     * algorithm with the highest number. If an SHA algorithm was not found among the available ones, the first checksum
     * will be returned.
     * <p>
     * The returned value will follow the format {@code <checksum-alg>:<checksum-value>}.
     *
     * @param checksums checksums to choose from
     * @return preferred checksum value
     */
    private static String getPreferredChecksum(Map<String, String> checksums) {
        String strongestAlg = null;
        String strongestValue = null;
        int strongestAlgNumber = 0;
        for (var e : checksums.entrySet()) {
            if (strongestAlg == null) {
                strongestAlg = e.getKey();
                strongestValue = e.getValue();
            }
            if (e.getKey().startsWith(SHA)) {
                final int algNumber = Integer.parseInt(e.getKey().substring(SHA.length()));
                if (algNumber > strongestAlgNumber) {
                    strongestAlg = e.getKey();
                    strongestValue = e.getValue();
                    strongestAlgNumber = algNumber;
                }
            }
        }
        return strongestAlg + ":" + strongestValue;
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
}
