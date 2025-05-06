package org.jboss.bacon.experimental.impl.dependencies;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.pnc.bacon.config.AutobuildConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Collects the dependencies to be excluded.
 */
@Slf4j
public class DependencyExcluder {

    private static final int DOWNLOAD_TIMEOUT = 60_000;

    private final CloseableHttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(
                    RequestConfig.custom()
                            .setConnectTimeout(DOWNLOAD_TIMEOUT)
                            .setSocketTimeout(DOWNLOAD_TIMEOUT)
                            .build())
            .build();

    private final URI gavExclusionFileUrl;

    public DependencyExcluder(final AutobuildConfig autobuildConfig) {
        if (autobuildConfig != null) {
            final String exclusionFileUrl = autobuildConfig.getGavExclusionUrl();
            log.info("exclusionFileUrl: {}", exclusionFileUrl);
            if (exclusionFileUrl != null) {
                gavExclusionFileUrl = URI.create(exclusionFileUrl);
            } else {
                gavExclusionFileUrl = null;
            }
        } else {
            gavExclusionFileUrl = null;
        }
    }

    /**
     * Fecthes the contents of the file containing the autobuild exclusions.
     *
     * @return Ex
     */
    public String fetchExclusionFile() {
        String exclusions = "";
        if (gavExclusionFileUrl != null) {
            HttpGet request = new HttpGet(gavExclusionFileUrl);
            try {
                HttpResponse response = httpClient.execute(request);
                if (response.getStatusLine().getStatusCode() < 300) {
                    HttpEntity entity = response.getEntity();
                    exclusions = EntityUtils.toString(entity, "UTF-8");
                } else {
                    log.warn(
                            "Failed to obtain the exclusions file. HTTP Status Code: {}, {}",
                            response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException ex) {
                log.warn(ex.getMessage(), ex);
            }
        }
        return exclusions;
    }

    /**
     * Formats the exclusions into a {@link String[]} so they can be handled properly.
     *
     * @param exclusions {@link String} obtained from the GAV exclusion file.
     * @return {@link String[]} with the exclusions split.
     */
    public static String[] getExcludedGavs(final String exclusions) {
        String lines[] = new String[0];
        if (exclusions != null && exclusions.length() > 0) {
            lines = exclusions.split("\\r?\\n");
        }
        return lines;
    }
}
