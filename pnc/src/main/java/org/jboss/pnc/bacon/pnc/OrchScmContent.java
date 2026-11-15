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

/**
 * Build sources and alignment diffs through PNC (orch), which reads the internal repository through reqour: for
 * environments that reach PNC but not the git providers.
 */
final class OrchScmContent {

    static final String REVISION_HEADER = "Reqour-Revision";
    static final String DIFF_BASE_HEADER = "Reqour-Diff-Base";
    static final String DIFF_HEAD_HEADER = "Reqour-Diff-Head";
    static final String DIFF_MODE_HEADER = "Reqour-Diff-Mode";
    static final String DIFF_MODE_COMMIT = "commit";
    /** PNC asks reqour, which may clone the repository first: answers can take minutes. */
    static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(10);
    private static final int PREVIEW_BYTES = 300;

    /** A complete alignment diff and how reqour obtained it. */
    record Diff(String content, String base, String head, String mode) {
    }

    private final URI restUrl;
    private final Supplier<Optional<String>> authorization;
    private final HttpClient httpClient;
    private final UrlRewrites rewrites;

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
        this.restUrl = restUrl;
        this.authorization = authorization;
        this.httpClient = httpClient;
        this.rewrites = rewrites;
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

    /** A GET through {@link SafeHttp}: credentials only to PNC's origin, the URL rewrites, a response timeout. */
    private HttpResponse<InputStream> get(URI uri, String accept) {
        Optional<String> auth = authorization.get();
        HttpResponse<InputStream> response;
        try {
            response = SafeHttp.get(
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
        if (response.statusCode() == 200) {
            return response;
        }
        String preview = preview(response.body());
        String hint = "";
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            hint = auth.isEmpty() ? ". Configure keycloak or ldapUsernamePassword in the Bacon profile."
                    : ". The configured credentials are not allowed to read builds.";
        } else if (response.statusCode() == 404 && preview.isBlank()) {
            hint = ". This PNC may not provide the endpoint yet.";
        }
        throw new RuntimeException(
                "HTTP status: " + response.statusCode() + " from " + uri
                        + (preview.isBlank() ? "" : ". Response preview: " + preview) + hint);
    }

    private static String preview(InputStream body) {
        try (body) {
            return new String(body.readNBytes(PREVIEW_BYTES), StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            return "";
        }
    }
}
