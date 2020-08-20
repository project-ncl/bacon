package org.jboss.pnc.bacon.pig.impl.utils;

import com.redhat.red.build.finder.BuildConfig;
import com.redhat.red.build.finder.BuildFinder;
import com.redhat.red.build.finder.BuildSystemInteger;
import com.redhat.red.build.finder.DistributionAnalyzer;
import com.redhat.red.build.finder.KojiBuild;
import com.redhat.red.build.finder.KojiClientSession;
import com.redhat.red.build.finder.Utils;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiChecksumType;
import org.apache.commons.collections4.MultiValuedMap;
import org.jboss.pnc.bacon.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class BuildFinderUtils {
    private static final Logger log = LoggerFactory.getLogger(BuildFinderUtils.class);

    private BuildFinderUtils() {

    }

    private static final String KOJI_BUILD_FINDER_CONFIG_ENV = "KOJI_BUILD_FINDER_CONFIG";

    private static final String KOJI_BUILD_FINDER_CONFIG_PROP = "koji.build.finder.config";

    private static final String KOJI_BUILD_FINDER_CONFIG_TEMPLATE = "/koji-build-finder/config.json";

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
     * Find builds in Koji for the given file.
     *
     * @param file the input file
     * @param includeNotFound whether or not to include not found files as build index 0
     * @return the builds, including not found files if includeNotFound is true, or the found builds otherwise
     */
    public static List<KojiBuild> findBuilds(File file, boolean includeNotFound) {
        BuildConfig config = getKojiBuildFinderConfig();
        List<File> inputs = Collections.singletonList(file);
        ExecutorService pool = Executors.newFixedThreadPool(config.getChecksumTypes().size());
        DistributionAnalyzer analyzer = new DistributionAnalyzer(inputs, config);
        Future<Map<KojiChecksumType, MultiValuedMap<String, String>>> futureChecksum = pool.submit(analyzer);

        try (KojiClientSession session = new KojiClientSession(config.getKojiHubURL())) {
            BuildFinder finder = new BuildFinder(session, config, analyzer);
            Future<Map<BuildSystemInteger, KojiBuild>> futureBuilds = pool.submit(finder);

            try {
                futureChecksum.get();
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
