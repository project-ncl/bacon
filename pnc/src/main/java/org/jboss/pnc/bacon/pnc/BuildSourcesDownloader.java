/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pnc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.resteasy.util.HttpResponseCodes;

import lombok.extern.slf4j.Slf4j;

/**
 * Downloads the SCM archive for a PNC build.
 *
 * This intentionally does not use /builds/{id}/scm-archive because that endpoint can fail with 406 via the generated
 * REST client, or return browser/SAML HTML instead of a tarball.
 */
@Slf4j
public class BuildSourcesDownloader {

    private static final int PREVIEW_BYTES = 300;
    private static final int MAX_REDIRECTS = 10;

    private static final String ACCEPT_ARCHIVE = "application/x-gzip, application/gzip, application/x-tar, application/octet-stream";
    private static final String ACCEPT_GITHUB_API = "application/json";

    private static final int HTTP_MOVED_PERMANENTLY = 301;
    private static final int HTTP_FOUND = 302;
    private static final int HTTP_SEE_OTHER = 303;
    private static final int HTTP_TEMPORARY_REDIRECT = 307;
    private static final int HTTP_PERMANENT_REDIRECT = 308;

    public Path downloadSources(Build build, Path targetFile) {
        try {
            String githubToken = getGithubTokenFromActiveProfile();
            if (githubToken == null || githubToken.isBlank()) {
                throw new RuntimeException(
                        "Failed to download sources for build " + build.getId()
                                + ". Please specify a valid 'githubToken' in your Bacon config.yaml active profile.");
            }

            String scmUrl = selectScmUrl(build);
            String scmRevision = selectScmRevision(build);

            if (scmUrl == null || scmUrl.isBlank()) {
                throw new RuntimeException("Build " + build.getId() + " has no SCM URL.");
            }
            if (scmRevision == null || scmRevision.isBlank()) {
                throw new RuntimeException("Build " + build.getId() + " has no SCM revision.");
            }

            List<SourceArchiveCandidate> candidates = sourceArchiveCandidates(scmUrl, scmRevision);
            if (candidates.isEmpty()) {
                throw new RuntimeException(
                        "Could not derive any source archive URL for build " + build.getId()
                                + " from SCM URL: " + scmUrl);
            }

            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }

            Path temporaryFile = targetFile.resolveSibling(targetFile.getFileName().toString() + ".tmp");
            List<String> failures = new ArrayList<>();

            try {
                for (SourceArchiveCandidate candidate : candidates) {
                    for (String authorizationHeader : authorizationHeaders(githubToken)) {
                        try {
                            log.debug(
                                    "Downloading sources for build {} from {} to {}",
                                    build.getId(),
                                    candidate.uri,
                                    targetFile);

                            downloadWithRedirects(build, candidate, authorizationHeader, temporaryFile);
                            assertGzipArchive(temporaryFile, candidate.uri);

                            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                            return targetFile;
                        } catch (RuntimeException e) {
                            Files.deleteIfExists(temporaryFile);
                            failures.add(
                                    candidate.uri + " using " + authSchemeName(authorizationHeader) + ": "
                                            + e.getMessage());
                            log.debug(
                                    "Source archive candidate failed for build {}: {}",
                                    build.getId(),
                                    failures.get(failures.size() - 1));
                        }
                    }
                }
            } finally {
                Files.deleteIfExists(temporaryFile);
            }

            throw new RuntimeException(
                    "Failed to download sources for build " + build.getId() + ". Tried candidates:\n - "
                            + String.join("\n - ", failures));
        } catch (IOException e) {
            throw new RuntimeException("Failed to download sources for build " + build.getId(), e);
        }
    }

    private void downloadWithRedirects(
            Build build,
            SourceArchiveCandidate candidate,
            String authorizationHeader,
            Path targetFile) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

            URI originalUri = candidate.uri;
            URI currentUri = originalUri;

            for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
                boolean firstRequest = redirectCount == 0;
                String acceptHeader = firstRequest ? candidate.initialAcceptHeader : ACCEPT_ARCHIVE;

                HttpRequest request = newSourceArchiveRequest(
                        currentUri,
                        acceptHeader,
                        authorizationHeader,
                        shouldSendAuthorization(originalUri, currentUri));

                HttpResponse<InputStream> response = httpClient
                        .send(request, HttpResponse.BodyHandlers.ofInputStream());
                int statusCode = response.statusCode();

                if (isRedirect(statusCode)) {
                    closeQuietly(response.body());

                    URI previousUri = currentUri;
                    Optional<String> locationHeader = response.headers().firstValue("Location");
                    if (locationHeader.isEmpty()) {
                        throw new RuntimeException(
                                "HTTP " + statusCode + " redirect response did not include a Location header.");
                    }

                    currentUri = previousUri.resolve(locationHeader.get());

                    log.debug(
                            "Following source archive redirect for build {}: {} -> {}",
                            build.getId(),
                            previousUri,
                            currentUri);
                    continue;
                }

                if (statusCode != HttpResponseCodes.SC_OK) {
                    String responsePreview = responsePreview(response.body());
                    throw new RuntimeException(
                            "HTTP status: " + statusCode + " from " + currentUri
                                    + (responsePreview.isBlank() ? "" : ". Response preview: " + responsePreview));
                }

                try (InputStream in = response.body()) {
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }

                return;
            }

            throw new RuntimeException("Too many redirects; maximum allowed redirects: " + MAX_REDIRECTS);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while downloading " + candidate.uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading " + candidate.uri, e);
        }
    }

    private HttpRequest newSourceArchiveRequest(
            URI uri,
            String acceptHeader,
            String authorizationHeader,
            boolean sendAuthorization) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", acceptHeader)
                .header("User-Agent", "bacon")
                .GET();

        if (sendAuthorization) {
            builder.header("Authorization", authorizationHeader);
        }

        return builder.build();
    }

    private static boolean shouldSendAuthorization(URI originalUri, URI currentUri) {
        // Match curl -L behavior: do not forward Authorization to a different host such as codeload.github.ibm.com.
        return originalUri.getHost() != null && originalUri.getHost().equalsIgnoreCase(currentUri.getHost());
    }

    static List<SourceArchiveCandidate> sourceArchiveCandidates(String scmUrl, String scmRevision) {
        NormalizedGithubRepository repository = normalizeGithubRepository(scmUrl);
        if (repository == null) {
            return List.of();
        }

        Set<SourceArchiveCandidate> candidates = new LinkedHashSet<>();

        candidates.add(
                new SourceArchiveCandidate(
                        URI.create(
                                repository.webBaseUrl + "/" + repository.owner + "/" + repository.repository
                                        + "/archive/" + scmRevision + ".tar.gz"),
                        ACCEPT_ARCHIVE));

        if ("github.com".equals(repository.host)) {
            candidates.add(
                    new SourceArchiveCandidate(
                            URI.create(
                                    "https://api.github.com/repos/" + repository.owner + "/" + repository.repository
                                            + "/tarball/" + scmRevision),
                            ACCEPT_GITHUB_API));
        } else {
            candidates.add(
                    new SourceArchiveCandidate(
                            URI.create(
                                    repository.webBaseUrl + "/api/v3/repos/" + repository.owner + "/"
                                            + repository.repository + "/tarball/" + scmRevision),
                            ACCEPT_GITHUB_API));
        }

        return List.copyOf(candidates);
    }

    static NormalizedGithubRepository normalizeGithubRepository(String scmUrl) {
        if (scmUrl == null || scmUrl.isBlank()) {
            return null;
        }

        String value = scmUrl.trim();

        if (value.startsWith("git@")) {
            int atIndex = value.indexOf('@');
            int colonIndex = value.indexOf(':', atIndex + 1);
            if (colonIndex > 0) {
                String host = value.substring(atIndex + 1, colonIndex);
                String path = stripGitSuffix(value.substring(colonIndex + 1));
                return fromHostAndPath(host, path);
            }
        }

        if (value.startsWith("ssh://git@")) {
            URI uri = URI.create(value);
            String host = uri.getHost();
            String path = stripLeadingSlash(stripGitSuffix(uri.getPath()));
            return fromHostAndPath(host, path);
        }

        if (value.startsWith("http://") || value.startsWith("https://")) {
            URI uri = URI.create(value);
            String host = uri.getHost();
            String path = stripLeadingSlash(stripGitSuffix(uri.getPath()));
            return fromHostAndPath(host, path);
        }

        return null;
    }

    private static NormalizedGithubRepository fromHostAndPath(String host, String path) {
        if (host == null || host.isBlank() || path == null || path.isBlank()) {
            return null;
        }

        String[] parts = path.split("/");
        if (parts.length < 2) {
            return null;
        }

        return new NormalizedGithubRepository(host, "https://" + host, parts[0], parts[1]);
    }

    private static String selectScmUrl(Build build) {
        if (build.getScmUrl() != null && !build.getScmUrl().isBlank()) {
            return build.getScmUrl();
        }

        SCMRepository repository = build.getScmRepository();
        if (repository == null) {
            return null;
        }

        if (repository.getExternalUrl() != null && !repository.getExternalUrl().isBlank()) {
            return repository.getExternalUrl();
        }

        return repository.getInternalUrl();
    }

    static String selectScmRevision(Build build) {
        BuildConfigurationRevisionRef revision = build.getBuildConfigRevision();
        if (revision != null && revision.getScmRevision() != null && !revision.getScmRevision().isBlank()) {
            return revision.getScmRevision();
        }

        return build.getScmRevision();
    }

    static void assertGzipArchive(Path file, URI sourceArchiveUri) throws IOException {
        byte[] firstBytes = Files.readAllBytes(file);
        if (!isGzip(firstBytes)) {
            String preview = new String(
                    firstBytes,
                    0,
                    Math.min(firstBytes.length, PREVIEW_BYTES),
                    StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            throw new RuntimeException(
                    "Downloaded sources from " + sourceArchiveUri
                            + " are not a gzip archive. First bytes/text: " + preview);
        }
    }

    static boolean isGzip(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b;
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == HTTP_MOVED_PERMANENTLY
                || statusCode == HTTP_FOUND
                || statusCode == HTTP_SEE_OTHER
                || statusCode == HTTP_TEMPORARY_REDIRECT
                || statusCode == HTTP_PERMANENT_REDIRECT;
    }

    private static List<String> authorizationHeaders(String githubToken) {
        return List.of("Bearer " + githubToken, "token " + githubToken);
    }

    private static String authSchemeName(String authorizationHeader) {
        int spaceIndex = authorizationHeader.indexOf(' ');
        return spaceIndex > 0 ? authorizationHeader.substring(0, spaceIndex) : "unknown";
    }

    private static String stripGitSuffix(String value) {
        return value != null && value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }

    private static String stripLeadingSlash(String value) {
        return value != null && value.startsWith("/") ? value.substring(1) : value;
    }

    private static void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            log.debug("Unable to close source archive response stream", e);
        }
    }

    private static String responsePreview(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }

        try (InputStream in = inputStream) {
            byte[] bytes = in.readNBytes(PREVIEW_BYTES);
            return new String(bytes, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
        } catch (IOException e) {
            return "";
        }
    }

    private static String getGithubTokenFromActiveProfile() {
        try {
            Class<?> configClass = Class.forName("org.jboss.pnc.bacon.config.Config");
            Method instanceMethod = configClass.getMethod("instance");
            Object config = instanceMethod.invoke(null);

            Method getActiveProfileMethod = config.getClass().getMethod("getActiveProfile");
            Object activeProfile = getActiveProfileMethod.invoke(config);

            Method getGithubTokenMethod = activeProfile.getClass().getMethod("getGithubToken");
            Object githubToken = getGithubTokenMethod.invoke(activeProfile);

            return githubToken == null ? null : githubToken.toString();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to read githubToken from active Bacon profile.", e);
        }
    }

    static final class SourceArchiveCandidate {

        private final URI uri;
        private final String initialAcceptHeader;

        private SourceArchiveCandidate(URI uri, String initialAcceptHeader) {
            this.uri = uri;
            this.initialAcceptHeader = initialAcceptHeader;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SourceArchiveCandidate)) {
                return false;
            }
            SourceArchiveCandidate that = (SourceArchiveCandidate) other;
            return uri.equals(that.uri) && initialAcceptHeader.equals(that.initialAcceptHeader);
        }

        @Override
        public int hashCode() {
            return 31 * uri.hashCode() + initialAcceptHeader.hashCode();
        }
    }

    static final class NormalizedGithubRepository {

        private final String host;
        private final String webBaseUrl;
        private final String owner;
        private final String repository;

        private NormalizedGithubRepository(String host, String webBaseUrl, String owner, String repository) {
            this.host = host;
            this.webBaseUrl = webBaseUrl;
            this.owner = owner;
            this.repository = repository;
        }
    }
}
