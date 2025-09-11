/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 4/16/18
 */
public class FileDownloadUtils {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadUtils.class);

    private static final int CONNECTION_TIMEOUT = 300000;

    private static final int READ_TIMEOUT = 900000;

    private static final int DEFAULT_ATTEMPTS = 1;
    private static final int MAX_ATTEMPTS = 20;

    // TODO move it out from here and add it as a method parameter
    private static int attempts = DEFAULT_ATTEMPTS;

    private static final RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
            .setConnectTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(READ_TIMEOUT)
            .build();

    public static final Path CACHE_DIRECTORY = Path.of(System.getProperty("user.home") + "/.cache/pnc-bacon");
    public static final String FILE_CACHE_LOCATION = CACHE_DIRECTORY.resolve("pnc-bacon-artifact-cache.db").toString();

    private static boolean enableCache = false;
    private static List<String> urlsToCache = List.of();

    /**
     * Please use it via getFileCache() method, not directly
     */
    private static FileCache fileCache = null;

    /**
     * Lazily initialize the file cache only when needed
     *
     * @return
     */
    private static FileCache getFileCache() {
        if (fileCache == null) {
            fileCache = new FileCache(Path.of(FILE_CACHE_LOCATION));
        }
        return fileCache;
    }

    public static Path getCacheDirectory() {
        return CACHE_DIRECTORY;
    }

    public static void controlCache(boolean enableCacheTemp, List<String> urlsToCacheTemp) {
        enableCache = enableCacheTemp;
        urlsToCache = urlsToCacheTemp;
    }

    public static void downloadTo(URI downloadUrl, File targetPath) {
        downloadTo(downloadUrl, targetPath, enableCache);
    }

    public static void downloadTo(URI downloadUrl, File targetPath, boolean cache) {

        if (cache && inUrlsToCache(downloadUrl)) {
            boolean isCached = getFileCache().copyTo(downloadUrl.toString(), targetPath);
            if (isCached) {
                log.info("Retrieved {} from cache and put to {}", downloadUrl, targetPath);
                // no need to download anything, file retrieved from cache
                return;
            }
        }

        log.debug("Downloading {} to {}", downloadUrl, targetPath);
        doDownload(downloadUrl, targetPath, attempts, cache);

        if (cache) {
            log.debug("Writing content of {} to cache!", targetPath);
            getFileCache().put(downloadUrl.toString(), targetPath);
        }
    }

    private static void doDownload(URI downloadUrl, File targetPath, int attemptsLeft, boolean cache) {

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            downloadWithClient(httpClient, downloadUrl, targetPath);
        } catch (NotFoundException nfe) {
            throw new RuntimeException("URL " + downloadUrl + " returns status 404: Not Found", nfe);
        } catch (Exception e) {
            attemptsLeft--;
            if (attemptsLeft == 0) {
                throw new RuntimeException(
                        "failed to download " + downloadUrl + " to " + targetPath.getAbsolutePath(),
                        e);
            } else {
                log.debug("Failed to download {}. Will reattempt at most {} times", downloadUrl, attemptsLeft);
                // sleep with exponential backoff up to a maximum of 30 seconds
                int sleepTimeInSeconds = (int) Math.ceil(Math.pow(30, (double) 1 / attemptsLeft));
                log.debug("Sleeping for : {}", sleepTimeInSeconds);
                SleepUtils.sleep(sleepTimeInSeconds);
                doDownload(downloadUrl, targetPath, attemptsLeft, cache);
            }
        }
    }

    private static void downloadWithClient(CloseableHttpClient httpClient, URI downloadUrl, File targetPath)
            throws NotFoundException, HttpException, IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(downloadUrl))) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 404) {
                throw new NotFoundException();
            }
            if (statusCode < 200 || statusCode > 299) {
                throw new HttpException("Invalid status code for download");
            }
            try (InputStream input = response.getEntity().getContent();
                    FileOutputStream output = new FileOutputStream(targetPath)) {
                IOUtils.copy(input, output);
            }
        }
    }

    public static void setAttempts(int attempts) {
        if (attempts > MAX_ATTEMPTS) {
            log.warn(
                    "Maximum number of download attempts is {}. The attempts have been set to {}",
                    MAX_ATTEMPTS,
                    MAX_ATTEMPTS);
            attempts = MAX_ATTEMPTS;
        }

        if (attempts <= 0) {
            log.warn("Number of download attempts has to be a positive integer. Setting to {}", DEFAULT_ATTEMPTS);
            attempts = DEFAULT_ATTEMPTS;
        }

        FileDownloadUtils.attempts = attempts;
    }

    private static boolean inUrlsToCache(URI downloadUrl) {
        return urlsToCache.stream().anyMatch(url -> url.contains(downloadUrl.getHost()));
    }

    private FileDownloadUtils() {
    }
}
