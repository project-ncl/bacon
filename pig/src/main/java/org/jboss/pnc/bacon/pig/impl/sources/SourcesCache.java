package org.jboss.pnc.bacon.pig.impl.sources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A local filesystem cache for source tar ball archives.
 */
public class SourcesCache {
    private static final Logger log = LoggerFactory.getLogger(SourcesCache.class);

    private final Path sourcesCacheDirectory;

    /**
     * @return a new {@link SourcesCache} with {@code FileDownloadUtils.getCacheDirectory().resolve("sources")} set as
     *         {@link #sourcesCacheDirectory}
     */
    public static SourcesCache createDefault() {
        return new SourcesCache(FileDownloadUtils.getCacheDirectory().resolve("sources"));
    }

    SourcesCache(Path sourcesCacheDirectory) {
        this.sourcesCacheDirectory = sourcesCacheDirectory;
        if (!Files.exists(sourcesCacheDirectory)) {
            try {
                Files.createDirectories(sourcesCacheDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Could not create " + sourcesCacheDirectory, e);
            }
        }
    }

    /**
     * If available, return a locally stored archive for the given {@code buildName} and {@code buildId}
     * or download it using the given {@code downloader}.
     *
     * @param buildName the name of the build whose source archive should be looked up
     * @param buildId the buildId of the build whose source archive should be looked up
     * @param downloader the downloader to use when the archive is not available locally
     * @return the path to the local source archive file
     */
    public Path get(String buildName, String buildId, Consumer<Path> downloader) {
        final String fileName = buildName + " " + buildId + ".tar.gz";
        final Path localFile = sourcesCacheDirectory.resolve(fileName);
        if (!Files.exists(localFile)) {
            downloader.accept(localFile);
        } else {
            log.info("Getting source archive " + fileName + " from local cache " + sourcesCacheDirectory);
        }
        return localFile;
    }
}
