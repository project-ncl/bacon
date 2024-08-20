package org.jboss.pnc.bacon.pig.impl.repo.visitor;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FileSystemArtifactVisit implements ArtifactVisit {

    private static final String[] CHECKSUM_PREFIXES = { "sha", "md5" };

    private final FileSystemArtifactRepository repo;
    private final GAV gav;

    FileSystemArtifactVisit(FileSystemArtifactRepository repo, GAV gav) {
        this.repo = repo;
        this.gav = gav;
    }

    @Override
    public GAV getGav() {
        return gav;
    }

    @Override
    public Map<String, String> getChecksums() {
        return repo.processArtifact(gav, FileSystemArtifactVisit::readArtifactChecksums);
    }

    /**
     * Reads Maven checksum files for a given artifact file in a local Maven repository.
     *
     * @param artifactPath artifact file in a local Maven repository
     * @return checksum map with algorithm name as a key and checksum as a value
     */
    public static Map<String, String> readArtifactChecksums(Path artifactPath) {
        final String checksumFilePrefix = artifactPath.getFileName().toString() + ".";
        try (Stream<Path> stream = Files.list(artifactPath.getParent())) {
            Map<String, String> checksums = new HashMap<>(2);
            stream.forEach(file -> {
                var fileName = file.getFileName().toString();
                if (!fileName.startsWith(checksumFilePrefix)) {
                    return;
                }
                for (var checksumPrefix : CHECKSUM_PREFIXES) {
                    if (fileName.regionMatches(
                            true,
                            checksumFilePrefix.length(),
                            checksumPrefix,
                            0,
                            checksumPrefix.length())) {
                        final String algName = fileName.substring(checksumFilePrefix.length()).toLowerCase();
                        final String value;
                        try {
                            value = Files.readString(file);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to read " + file, e);
                        }
                        checksums.put(algName, value);
                    }
                }
            });
            return checksums;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
