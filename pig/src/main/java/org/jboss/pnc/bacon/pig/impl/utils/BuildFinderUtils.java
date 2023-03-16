package org.jboss.pnc.bacon.pig.impl.utils;

import com.redhat.red.build.koji.KojiClientException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.build.finder.core.BuildConfig;
import org.jboss.pnc.build.finder.core.BuildFinder;
import org.jboss.pnc.build.finder.core.BuildSystemInteger;
import org.jboss.pnc.build.finder.core.Checksum;
import org.jboss.pnc.build.finder.core.ChecksumType;
import org.jboss.pnc.build.finder.core.DistributionAnalyzer;
import org.jboss.pnc.build.finder.core.LocalFile;
import org.jboss.pnc.build.finder.core.Utils;
import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.build.finder.koji.KojiClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public final class BuildFinderUtils {
    private static final Logger log = LoggerFactory.getLogger(BuildFinderUtils.class);

    private BuildFinderUtils() {

    }

    private static final String KOJI_BUILD_FINDER_CONFIG_ENV = "KOJI_BUILD_FINDER_CONFIG";

    private static final String KOJI_BUILD_FINDER_CONFIG_PROP = "koji.build.finder.config";

    private static final String KOJI_BUILD_FINDER_CONFIG_TEMPLATE = "/koji-build-finder/config.json";

    private static Map<Checksum, Collection<String>> mapToMultiMap(Map<String, Collection<String>> checksums) {
        Set<Map.Entry<String, Collection<String>>> entries = checksums.entrySet();
        MultiValuedMap<Checksum, String> multiMap = new ArrayListValuedHashMap<>(entries.size());

        for (Map.Entry<String, Collection<String>> entry : entries) {
            String md5sum = entry.getKey();
            Collection<String> filenames = entry.getValue();

            for (String filename : filenames) {
                Checksum checksum = new Checksum(ChecksumType.md5, md5sum, filename, 0);
                multiMap.put(checksum, filename);
            }
        }

        return Collections.unmodifiableMap(multiMap.asMap());
    }

    public static BuildConfig getKojiBuildFinderConfigFromFile(File file) {
        try {
            return BuildConfig.load(file);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed read Koji Build Finder configuration from file: " + file.getAbsolutePath(),
                    e);
        }
    }

    public static BuildConfig getKojiBuildFinderConfigFromFile(String filename) {
        return getKojiBuildFinderConfigFromFile(new File(filename));
    }

    public static BuildConfig getKojiBuildFinderConfigFromResource(String resourceName) {
        Properties props = new Properties();
        props.setProperty("KOJI_URL", Config.instance().getActiveProfile().getPig().getKojiHubUrl());

        String json = ResourceUtils.extractToStringWithFiltering(resourceName, props);

        return getKojiBuildFinderConfigFromJson(json);
    }

    public static BuildConfig getKojiBuildFinderConfigFromJson(String json) {
        try {
            return BuildConfig.load(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed read Koji Build Finder configuration from json: " + json, e);
        }
    }

    public static BuildConfig getKojiBuildFinderConfig() {
        String propFilename = System.getProperty(KOJI_BUILD_FINDER_CONFIG_PROP);

        if (propFilename != null) {
            return getKojiBuildFinderConfigFromFile(propFilename);
        }

        String envFilename = System.getenv(KOJI_BUILD_FINDER_CONFIG_ENV);

        if (envFilename != null) {
            return getKojiBuildFinderConfigFromFile(envFilename);
        }

        return getKojiBuildFinderConfigFromResource(KOJI_BUILD_FINDER_CONFIG_TEMPLATE);
    }

    /**
     * Compute checksums for the given file and for each file entry if the file is an archive.
     *
     * @param file the file
     * @return the checksums
     */
    public static Map<String, Collection<String>> findChecksums(File file) {
        BuildConfig config = getKojiBuildFinderConfig();
        List<String> inputs = Collections.singletonList(file.getPath());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        DistributionAnalyzer analyzer = new DistributionAnalyzer(inputs, config);
        Future<Map<ChecksumType, MultiValuedMap<String, LocalFile>>> futureChecksums = pool.submit(analyzer);

        try {
            Map<ChecksumType, MultiValuedMap<String, LocalFile>> checksums = futureChecksums.get();
            MultiValuedMap<String, LocalFile> md5s = checksums.get(ChecksumType.md5);
            Map<String, Collection<String>> map = md5s.asMap()
                    .entrySet()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue()
                                            .stream()
                                            .map(LocalFile::getFilename)
                                            .collect(Collectors.toList())));
            return Collections.unmodifiableMap(map);
        } catch (InterruptedException e) {
            log.error("Failed to get checksums: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return Collections.emptyMap();
        } catch (ExecutionException e) {
            log.error("Failed to get checksums: {}", e.getMessage(), e);
            return Collections.emptyMap();
        } finally {
            Utils.shutdownAndAwaitTermination(pool);
        }
    }

    /**
     * Find builds in Koji for the given checksums.
     *
     * @param checksums the checksums
     * @param includeNotFound whether or not to include not found files as build index 0
     * @return the builds, including not found files if includeNotFound is true, or the found builds otherwise
     */
    public static List<KojiBuild> findBuilds(Map<String, Collection<String>> checksums, boolean includeNotFound) {
        BuildConfig config = getKojiBuildFinderConfig();

        try (KojiClientSession session = new KojiClientSession(config.getKojiHubURL())) {
            BuildFinder finder = new BuildFinder(session, config);

            Map<Checksum, Collection<String>> multiMap = mapToMultiMap(checksums);

            finder.findBuilds(multiMap);
            return includeNotFound ? finder.getBuilds() : finder.getBuildsFound();
        } catch (KojiClientException e) {
            log.error("Failed to get builds: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    /**
     * Find builds in Koji for the given file.
     *
     * @param file the input file
     * @param includeNotFound whether or not to include not found files as build index 0
     * @return the builds, including not found files if includeNotFound is true, or the found builds otherwise
     */
    public static List<KojiBuild> findBuilds(File file, boolean includeNotFound) {
        List<String> inputs = Collections.singletonList(file.getPath());
        return findBuilds(includeNotFound, inputs, 2);
    }

    public static List<KojiBuild> findBuilds(boolean includeNotFound, Collection<String> inputs, int poolSize) {
        BuildConfig config = getKojiBuildFinderConfig();
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        DistributionAnalyzer analyzer = new DistributionAnalyzer(new ArrayList<>(inputs), config);
        Future<Map<ChecksumType, MultiValuedMap<String, LocalFile>>> futureChecksums = pool.submit(analyzer);

        try (KojiClientSession session = new KojiClientSession(config.getKojiHubURL())) {
            BuildFinder finder = new BuildFinder(session, config, analyzer);
            Future<Map<BuildSystemInteger, KojiBuild>> futureBuilds = pool.submit(finder);

            try {
                futureChecksums.get();
            } catch (InterruptedException e) {
                log.error("Failed to get checksums: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
                return Collections.emptyList();
            } catch (ExecutionException e) {
                log.error("Failed to get checksums: {}", e.getMessage(), e);
                return Collections.emptyList();
            }

            try {
                futureBuilds.get();
            } catch (InterruptedException e) {
                log.error("Failed to get builds: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
                return Collections.emptyList();
            } catch (ExecutionException e) {
                log.error("Failed to get builds: {}", e.getMessage(), e);
                return Collections.emptyList();
            }

            return includeNotFound ? finder.getBuilds() : finder.getBuildsFound();
        } catch (KojiClientException e) {
            log.error("Koji client error: {}", e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            Utils.shutdownAndAwaitTermination(pool);
        }
    }

    /**
     * Find builds in Koji for the given file.
     *
     * @param path the input file as a path
     * @param includeNotFound whether or not to include not found files as build index 0
     * @return the builds, including not found files if includeNotFound is true, or the found builds otherwise
     */
    public static List<KojiBuild> findBuilds(Path path, boolean includeNotFound) {
        return findBuilds(path.toFile(), includeNotFound);
    }

    /**
     * Find builds in Koji for the given file.
     *
     * @param filename the input file as a string
     * @param includeNotFound whether or not to include not found files as build index 0
     * @return the builds, including not found files if includeNotFound is true, or the found builds otherwise
     */
    public static List<KojiBuild> findBuilds(String filename, boolean includeNotFound) {
        return findBuilds(new File(filename), includeNotFound);
    }
}
