package org.jboss.bacon.experimental.impl.projectfinder;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScmFileAccessor implements Closeable {

    private static final int DOWNLOAD_TIMEOUT = 60_000;
    private static final Pattern GITHUB_PATTERN = Pattern.compile("https?://github\\.com/([^/]+/[^/]+?)(?:\\.git)?$");
    private static final Pattern GITLAB_PATTERN = Pattern
            .compile("https?://([^/]+)/(.+?)(?:\\.git)?$");

    private final CloseableHttpClient httpClient;

    public ScmFileAccessor() {
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(DOWNLOAD_TIMEOUT)
                                .setSocketTimeout(DOWNLOAD_TIMEOUT)
                                .build())
                .build();
    }

    ScmFileAccessor(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Optional<String> fetchFile(String scmUrl, String revision, String filePath) {
        if (scmUrl == null || revision == null || filePath == null) {
            return Optional.empty();
        }

        String rawUrl = buildRawFileUrl(scmUrl, revision, filePath);
        if (rawUrl == null) {
            log.debug("Unsupported SCM host for file access: {}", scmUrl);
            return Optional.empty();
        }

        try {
            HttpGet request = new HttpGet(rawUrl);
            HttpResponse response = httpClient.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                return Optional.of(EntityUtils.toString(response.getEntity(), "UTF-8"));
            }
            EntityUtils.consumeQuietly(response.getEntity());
            if (status != 404) {
                log.debug("HTTP {} fetching {} from {}", status, filePath, rawUrl);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.debug("Error fetching file {} from SCM: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }

    String buildRawFileUrl(String scmUrl, String revision, String filePath) {
        String clean = scmUrl.trim();
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }

        Matcher githubMatcher = GITHUB_PATTERN.matcher(clean);
        if (githubMatcher.matches()) {
            String ownerRepo = githubMatcher.group(1);
            return "https://raw.githubusercontent.com/" + ownerRepo + "/" + revision + "/" + filePath;
        }

        if (clean.contains("gitlab")) {
            Matcher gitlabMatcher = GITLAB_PATTERN.matcher(clean);
            if (gitlabMatcher.matches()) {
                String host = gitlabMatcher.group(1);
                String projectPath = gitlabMatcher.group(2);
                String encodedProject = URLEncoder.encode(projectPath, StandardCharsets.UTF_8);
                String encodedFile = URLEncoder.encode(filePath, StandardCharsets.UTF_8);
                return "https://" + host + "/api/v4/projects/" + encodedProject
                        + "/repository/files/" + encodedFile + "/raw?ref=" + revision;
            }
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
