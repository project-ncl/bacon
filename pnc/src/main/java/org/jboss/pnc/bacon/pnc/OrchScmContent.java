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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.bacon.common.http.SafeHttp;
import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.bacon.config.Config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Build sources and alignment diffs through PNC (orch), which reads the internal repository through reqour: for
 * environments that reach PNC but not the git providers.
 */
@Slf4j
final class OrchScmContent {

    static final String REVISION_HEADER = "Reqour-Revision";
    static final String DIFF_BASE_HEADER = "Reqour-Diff-Base";
    static final String DIFF_HEAD_HEADER = "Reqour-Diff-Head";
    static final String DIFF_MODE_HEADER = "Reqour-Diff-Mode";
    static final String DIFF_MODE_COMMIT = "commit";
    /** PNC asks reqour, which may clone the repository first: answers can take minutes. */
    static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(10);
    private static final int PREVIEW_BYTES = 2000;
    /** A 503 (PNC busy, or the git provider rate-limiting) is retried once when PNC asks to wait at most this long. */
    static final Duration MAX_RETRY_WAIT = Duration.ofSeconds(60);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A complete alignment diff and how reqour obtained it. */
    record Diff(String content, String base, String head, String mode) {
    }

    private final URI restUrl;
    private final Supplier<Optional<String>> authorization;
    private final HttpClient httpClient;
    private final UrlRewrites rewrites;
    private final Sleeper sleeper;

    /** Waits before a retry; replaced in tests. */
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    /**
     * @param restUrl e.g. {@code https://orch.example.com/pnc-rest/v2}
     * @param authorization the Authorization header value, if credentials are configured
     * @param rewrites applied to the PNC URL and its redirects
     */
    OrchScmContent(
            URI restUrl,
            Supplier<Optional<String>> authorization,
            HttpClient httpClient,
            UrlRewrites rewrites) {
        this(restUrl, authorization, httpClient, rewrites, duration -> Thread.sleep(duration.toMillis()));
    }

    OrchScmContent(
            URI restUrl,
            Supplier<Optional<String>> authorization,
            HttpClient httpClient,
            UrlRewrites rewrites,
            Sleeper sleeper) {
        this.restUrl = restUrl;
        this.authorization = authorization;
        this.httpClient = httpClient;
        this.rewrites = rewrites;
        this.sleeper = sleeper;
    }

    static OrchScmContent fromProfile() {
        return new OrchScmContent(
                restUrl(Config.instance().getActiveProfile().getPnc().getUrl()),
                PncClientHelper::authorizationHeader,
                SafeHttp.newClient(),
                UrlRewrites.current());
    }

    /** The REST base of a PNC URL, as the PNC client builds it (scheme, host and port only). */
    static URI restUrl(String pncUrl) {
        URI uri = URI.create(pncUrl.trim());
        return URI.create(uri.getScheme() + "://" + uri.getRawAuthority() + "/pnc-rest/v2");
    }

    Path downloadArchive(String buildId, Path targetFile) {
        URI uri = buildUri(buildId, "/scm-archive/content");
        HttpResponse<InputStream> response = get(uri, "application/gzip");
        Path temporaryFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
        try (InputStream in = response.body()) {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            Files.copy(in, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            BuildSourcesDownloader.assertGzipArchive(temporaryFile, uri);
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download the sources of build " + buildId + " from " + uri, e);
        } finally {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    Diff alignmentDiff(String buildId) {
        URI uri = buildUri(buildId, "/alignment-diff");
        HttpResponse<InputStream> response = get(uri, "text/x-diff");
        try (InputStream in = response.body()) {
            return new Diff(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    response.headers().firstValue(DIFF_BASE_HEADER).orElse(null),
                    response.headers().firstValue(DIFF_HEAD_HEADER).orElse(null),
                    response.headers().firstValue(DIFF_MODE_HEADER).orElse(null));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the alignment diff of build " + buildId + " from " + uri, e);
        }
    }

    private URI buildUri(String buildId, String suffix) {
        return URI.create(restUrl + "/builds/" + URLEncoder.encode(buildId, StandardCharsets.UTF_8) + suffix);
    }

    /**
     * A GET through {@link SafeHttp}: credentials only to PNC's origin, the URL rewrites, a response timeout. A 503
     * whose {@code Retry-After} is at most {@link #MAX_RETRY_WAIT} is retried once after that wait.
     */
    private HttpResponse<InputStream> get(URI uri, String accept) {
        Optional<String> auth = authorization.get();
        for (boolean retried = false;; retried = true) {
            HttpResponse<InputStream> response = send(uri, accept, auth);
            if (response.statusCode() == 200) {
                return response;
            }
            String body = preview(response.body());
            Optional<Duration> wait = response.statusCode() == 503 ? retryAfter(response) : Optional.empty();
            if (!retried && wait.isPresent() && wait.get().compareTo(MAX_RETRY_WAIT) <= 0) {
                log.info(
                        "PNC cannot serve {} now ({}), retrying in {} s",
                        uri,
                        errorMessage(body).orElse("HTTP 503"),
                        wait.get().toSeconds());
                try {
                    sleeper.sleep(wait.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted requesting " + uri, e);
                }
                continue;
            }
            throw new RuntimeException(failure(uri, response.statusCode(), body, wait, auth.isPresent()));
        }
    }

    private HttpResponse<InputStream> send(URI uri, String accept, Optional<String> auth) {
        try {
            return SafeHttp.get(
                    httpClient,
                    uri,
                    Map.of("Accept", accept + ", application/json", "User-Agent", "bacon"),
                    auth.map(value -> Map.of("Authorization", value)).orElse(Map.of()),
                    rewrites,
                    List.of(),
                    RESPONSE_TIMEOUT);
        } catch (IOException e) {
            throw new RuntimeException("I/O error requesting " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted requesting " + uri, e);
        }
    }

    /** The error message: PNC's own explanation when it gave one, and a hint for the usual causes. */
    static String failure(URI uri, int status, String body, Optional<Duration> wait, boolean authenticated) {
        StringBuilder message = new StringBuilder("HTTP status: ").append(status).append(" from ").append(uri);
        Optional<String> explanation = errorMessage(body);
        if (explanation.isPresent()) {
            message.append(": ").append(explanation.get());
        } else if (!body.isBlank()) {
            message.append(". Response: ").append(body.length() > 300 ? body.substring(0, 300) + "..." : body);
        }
        switch (status) {
            case 401:
                message.append(
                        authenticated ? ". PNC rejected the credentials of the Bacon profile."
                                : ". PNC needs a login to serve sources and diffs: configure keycloak or"
                                        + " ldapUsernamePassword in the Bacon profile.");
                break;
            case 403:
                message.append(
                        ". The account of the Bacon profile is not allowed to read the SCM content of builds: it needs"
                                + " a role that may read builds in PNC, which PNC checks again in reqour.");
                break;
            case 404:
                if (body.isBlank()) {
                    message.append(
                            ". The build has no internal SCM repository or revision, or this PNC does not provide the"
                                    + " endpoint yet.");
                }
                break;
            case 503:
                message.append(
                        wait.map(w -> ". Try again in " + w.toSeconds() + " s.").orElse(". Try again later."));
                break;
            default:
                break;
        }
        return message.toString();
    }

    /** {@code errorMessage} and {@code details} of a PNC or reqour {@code ErrorResponse}, if the body is one. */
    static Optional<String> errorMessage(String body) {
        if (body == null || !body.startsWith("{")) {
            return Optional.empty();
        }
        try {
            JsonNode error = JSON.readTree(body);
            String message = error.path("errorMessage").asText("");
            if (message.isBlank()) {
                return Optional.empty();
            }
            JsonNode details = error.path("details");
            if (details.isMissingNode() || details.isNull()) {
                return Optional.of(message);
            }
            String detail = details.isTextual() ? details.asText() : details.toString();
            // reqour's answer passed on by PNC is itself an ErrorResponse
            detail = errorMessage(detail.strip()).orElse(detail);
            return Optional.of(message + " (" + detail + ")");
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<Duration> retryAfter(HttpResponse<?> response) {
        try {
            return response.headers()
                    .firstValue("Retry-After")
                    .map(String::trim)
                    .map(Long::parseLong)
                    .filter(seconds -> seconds >= 0)
                    .map(Duration::ofSeconds);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String preview(InputStream body) {
        try (body) {
            return new String(body.readNBytes(PREVIEW_BYTES), StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            return "";
        }
    }
}
