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

import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 4/16/18
 */
public class FileDownloadUtils {
    private FileDownloadUtils() {
    }

    private static final Logger log = LoggerFactory.getLogger(FileDownloadUtils.class);

    private static final int CONNECTION_TIMEOUT = 300000;

    private static final int READ_TIMEOUT = 900000;

    private static final int MAX_RETRIES = 5;

    private static final RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
            .setConnectTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(READ_TIMEOUT)
            .build();

    public static void downloadTo(URI downloadUrl, File targetPath) {
        log.info("Downloading {} to {}", downloadUrl, targetPath);

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            downloadWithClient(httpClient, downloadUrl, targetPath);
        } catch (Exception e) {
            log.warn("Failed to download {}. Will reattempt without SSL certificate check", downloadUrl);
            try (CloseableHttpClient unsafeHttpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build()) {
                downloadWithClient(unsafeHttpClient, downloadUrl, targetPath);
            } catch (Exception any) {
                throw new RuntimeException(
                        "failed to download " + downloadUrl + " to " + targetPath.getAbsolutePath(),
                        any);
            }
        }

        log.info("Downloaded {} to {}", downloadUrl, targetPath);
    }

    private static void downloadWithClient(CloseableHttpClient httpClient, URI downloadUrl, File targetPath)
            throws Exception {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(downloadUrl))) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode > 299) {
                throw new Exception("Invalid status code for download");
            }
            try (InputStream input = response.getEntity().getContent();
                    FileOutputStream output = new FileOutputStream(targetPath)) {
                IOUtils.copy(input, output);
            }
        }
    }
}
