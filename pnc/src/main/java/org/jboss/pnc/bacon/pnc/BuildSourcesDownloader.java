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
import java.net.URLEncoder;
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
import java.util.regex.Pattern;

import org.jboss.pnc.common.log.LogSanitizer;
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

    private static final Pattern COMMIT_SHA = Pattern.compile("[0-9a-fA-F]{40}");

    private static final String ACCEPT_ARCHIVE = "application/x-gzip, application/gzip, application/x-tar, application/octet-stream";
    private static final String ACCEPT_GITHUB_API = "application/json";
    private static final String ACCEPT_DIFF = "application/vnd.github.diff";

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
            List<String> scmRevisions = selectScmRevisions(build);

            if (scmUrl == null || scmUrl.isBlank()) {
                throw new RuntimeException("Build " + build.getId() + " has no SCM URL.");
            }
            if (scmRevisions.isEmpty()) {
                throw new RuntimeException("Build " + build.getId() + " has no SCM revision.");
            }

            List<SourceArchiveCandidate> candidates = sourceArchiveCandidates(scmUrl, scmRevisions);
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

    /**
     * Downloads the diff of the changes PNC/Reqour committed on top of the original SCM revision during alignment.
     *
     * <p>
     * Reqour clones the upstream sources into the internal SCM repository and commits the alignment changes on top of
     * the original revision, pushing and tagging the result. Both the pre-alignment revision and the post-alignment
     * commit are therefore present in the internal repository, so the alignment changes can be captured as a single
     * GitHub compare diff ({@code <pre-alignment>...<post-alignment>}). The resulting unified diff applies cleanly on
     * top of the original upstream sources (for example with {@code git apply} or {@code patch -p1}).
     *
     * <p>
     * The diff is sanitized of leaked credentials ({@link LogSanitizer}) and split ({@link AlignmentDiff}) so that
     * non-build-affecting metadata ({@value AlignmentDiff#REPOSITORIES_BACKUP},
     * {@value AlignmentDiff#MANIPULATION_JSON})
     * is written to a sibling {@code *-metadata.diff} file instead of polluting the main diff.
     *
     * @param build the build to compute the alignment diff for
     * @param mainDiffFile the file to write the build-affecting diff to
     * @return the files written: the main diff, plus the metadata diff when the build contains such metadata
     */
    public List<Path> downloadAlignmentDiff(Build build, Path mainDiffFile) {
        String githubToken = getGithubTokenFromActiveProfile();
        if (githubToken == null || githubToken.isBlank()) {
            throw new RuntimeException(
                    "Failed to download alignment diff for build " + build.getId()
                            + ". Please specify a valid 'githubToken' in your Bacon config.yaml active profile.");
        }

        String internalUrl = selectInternalScmUrl(build);
        if (internalUrl == null || internalUrl.isBlank()) {
            throw new RuntimeException(
                    "Build " + build.getId() + " has no internal SCM URL; cannot compute the alignment diff.");
        }

        NormalizedGithubRepository repository = normalizeGithubRepository(internalUrl);
        if (repository == null) {
            throw new RuntimeException(
                    "Could not parse the internal SCM URL for build " + build.getId() + ": " + internalUrl);
        }

        List<String> bases = preAlignmentRevisions(build);
        if (bases.isEmpty()) {
            throw new RuntimeException(
                    "Build " + build.getId()
                            + " has no pre-alignment SCM revision; cannot compute the alignment diff.");
        }

        List<String> heads = postAlignmentRevisions(build);
        if (heads.isEmpty()) {
            throw new RuntimeException(
                    "Build " + build.getId()
                            + " has no post-alignment SCM revision/tag; cannot compute the alignment diff.");
        }

        String rawDiff = null;
        List<String> failures = new ArrayList<>();
        search: for (String base : bases) {
            for (String head : heads) {
                URI compareUri = compareDiffUri(repository, base, head);
                for (String authorizationHeader : authorizationHeaders(githubToken)) {
                    try {
                        log.debug(
                                "Downloading alignment diff for build {} from {} ({}...{})",
                                build.getId(),
                                compareUri,
                                base,
                                head);

                        rawDiff = fetchDiff(compareUri, authorizationHeader);
                        break search;
                    } catch (RuntimeException e) {
                        failures.add(
                                compareUri + " using " + authSchemeName(authorizationHeader) + ": " + e.getMessage());
                        log.debug(
                                "Alignment diff candidate failed for build {}: {}",
                                build.getId(),
                                failures.get(failures.size() - 1));
                    }
                }
            }
        }

        if (rawDiff == null) {
            // Compare failed for every base/head. Distinguish "token cannot access the repository" (GitHub Enterprise
            // masks a missing SSO authorization as 404) from "the two commits have no common ancestor" (the three-dot
            // compare returns 404 for unrelated histories, which happens with some import/alignment repositories).
            String firstAuth = authorizationHeaders(githubToken).get(0);
            URI repoUri = repoApiUri(repository);
            int repoStatus = probeStatus(repoUri, firstAuth);
            if (repoStatus == 401 || repoStatus == 403 || repoStatus == 404) {
                throw new RuntimeException(
                        "Failed to download alignment diff for build " + build.getId()
                                + ". The internal repository " + internalUrl
                                + " is not accessible with the configured githubToken (HTTP " + repoStatus + " for "
                                + repoUri
                                + "). On GitHub Enterprise with SAML SSO the token must be authorized for the '"
                                + repository.owner + "' organization.");
            }

            // The repository is reachable, so compare most likely failed because the recorded pre-alignment revision is
            // not an ancestor of the post-alignment commit. Fall back to the diff of the post-alignment commit itself,
            // which captures the alignment change when Reqour committed it as a single commit on top of the import.
            for (String head : heads) {
                URI commitUri = commitDiffUri(repository, head);
                for (String authorizationHeader : authorizationHeaders(githubToken)) {
                    try {
                        log.debug(
                                "Falling back to the post-alignment commit diff for build {} from {}",
                                build.getId(),
                                commitUri);

                        rawDiff = fetchDiff(commitUri, authorizationHeader);
                        log.warn(
                                "Alignment compare had no common ancestor for build {}; used the post-alignment commit"
                                        + " diff ({}) instead. If alignment produced more than one commit this captures"
                                        + " only the last one.",
                                build.getId(),
                                head);
                        break;
                    } catch (RuntimeException e) {
                        failures.add(
                                commitUri + " using " + authSchemeName(authorizationHeader) + ": " + e.getMessage());
                        log.debug(
                                "Post-alignment commit diff candidate failed for build {}: {}",
                                build.getId(),
                                failures.get(failures.size() - 1));
                    }
                }
                if (rawDiff != null) {
                    break;
                }
            }
        }

        if (rawDiff == null) {
            throw new RuntimeException(
                    "Failed to download alignment diff for build " + build.getId()
                            + ". The pre-alignment revision(s) " + bases
                            + " must be reachable in the internal repository " + internalUrl
                            + ". Tried candidates:\n - " + String.join("\n - ", failures));
        }

        return writeAlignmentDiff(build, mainDiffFile, rawDiff);
    }

    private int probeStatus(URI uri, String authorizationHeader) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", ACCEPT_GITHUB_API)
                    .header("User-Agent", "bacon")
                    .header("Authorization", authorizationHeader)
                    .GET()
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private List<Path> writeAlignmentDiff(Build build, Path mainDiffFile, String rawDiff) {
        AlignmentDiff diff = AlignmentDiff.split(LogSanitizer.maskKnownSecrets(rawDiff));
        try {
            if (mainDiffFile.getParent() != null) {
                Files.createDirectories(mainDiffFile.getParent());
            }

            List<Path> written = new ArrayList<>();
            Files.writeString(mainDiffFile, diff.buildAffecting(), StandardCharsets.UTF_8);
            written.add(mainDiffFile);

            if (diff.hasMetadata()) {
                Path metadataFile = metadataSibling(mainDiffFile);
                Files.writeString(metadataFile, diff.metadata(), StandardCharsets.UTF_8);
                written.add(metadataFile);
            }

            return written;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write alignment diff for build " + build.getId(), e);
        }
    }

    /**
     * Derives the metadata diff path from the main diff path, e.g. {@code alignment.diff -> alignment-metadata.diff}.
     */
    static Path metadataSibling(Path mainDiffFile) {
        String name = mainDiffFile.getFileName().toString();
        String baseName = name.endsWith(".diff") ? name.substring(0, name.length() - ".diff".length()) : name;
        return mainDiffFile.resolveSibling(baseName + "-metadata.diff");
    }

    private String fetchDiff(URI uri, String authorizationHeader) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", ACCEPT_DIFF)
                    .header("User-Agent", "bacon")
                    .header("Authorization", authorizationHeader)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            try (InputStream in = response.body()) {
                byte[] body = in.readAllBytes();
                if (statusCode != HttpResponseCodes.SC_OK) {
                    String responsePreview = new String(
                            body,
                            0,
                            Math.min(body.length, PREVIEW_BYTES),
                            StandardCharsets.UTF_8).replaceAll("\\s+", " ");
                    throw new RuntimeException(
                            "HTTP status: " + statusCode
                                    + (responsePreview.isBlank() ? "" : ". Response preview: " + responsePreview));
                }
                return new String(body, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while downloading " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading " + uri, e);
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
        return sourceArchiveCandidates(scmUrl, List.of(scmRevision));
    }

    static List<SourceArchiveCandidate> sourceArchiveCandidates(String scmUrl, List<String> scmRevisions) {
        NormalizedGithubRepository repository = normalizeGithubRepository(scmUrl);
        if (repository == null) {
            return List.of();
        }

        Set<SourceArchiveCandidate> candidates = new LinkedHashSet<>();

        // Prefer the authenticated REST endpoint. Browser archive URLs on GitHub Enterprise commonly redirect to
        // SAML/login pages and return HTML or 406 instead of an archive.
        for (String scmRevision : scmRevisions) {
            for (String revisionCandidate : revisionCandidates(scmRevision)) {
                candidates.add(
                        new SourceArchiveCandidate(
                                apiTarballUri(repository, revisionCandidate),
                                ACCEPT_GITHUB_API));
            }
        }

        // Keep the browser archive endpoint as a fallback for installations where the REST endpoint is unavailable.
        for (String scmRevision : scmRevisions) {
            for (String revisionCandidate : revisionCandidates(scmRevision)) {
                candidates.add(
                        new SourceArchiveCandidate(
                                webArchiveUri(repository, revisionCandidate),
                                ACCEPT_ARCHIVE));
            }
        }

        return List.copyOf(candidates);
    }

    private static List<String> revisionCandidates(String scmRevision) {
        if (scmRevision == null || scmRevision.isBlank()) {
            return List.of();
        }

        String revision = scmRevision.trim();
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(revision);

        // GitHub returns HTTP 300 when an unqualified name exists as both a branch and a tag. Explicitly retry both
        // namespaces. Commit SHAs and already-qualified refs do not need these alternatives.
        if (!revision.startsWith("refs/") && !COMMIT_SHA.matcher(revision).matches()) {
            candidates.add("refs/tags/" + revision);
            candidates.add("refs/heads/" + revision);
        }

        return List.copyOf(candidates);
    }

    private static URI apiTarballUri(NormalizedGithubRepository repository, String revision) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository) + "/tarball/" + encodePath(revision));
    }

    /**
     * Builds the GitHub (Enterprise) compare URL that yields the diff between {@code base} and {@code head} in the
     * given
     * repository. Both refs must be resolvable within that single repository.
     */
    static URI compareDiffUri(NormalizedGithubRepository repository, String base, String head) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository) + "/compare/" + encodePath(base) + "..."
                        + encodePath(head));
    }

    /**
     * Builds the GitHub (Enterprise) URL for a single commit. With the diff media type this yields the diff introduced
     * by that commit relative to its parent.
     */
    static URI commitDiffUri(NormalizedGithubRepository repository, String ref) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository) + "/commits/" + encodePath(ref));
    }

    /**
     * Builds the GitHub (Enterprise) URL of the repository itself, used to probe whether the token can access it.
     */
    static URI repoApiUri(NormalizedGithubRepository repository) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository));
    }

    private static String apiBaseUrl(NormalizedGithubRepository repository) {
        return "github.com".equalsIgnoreCase(repository.host)
                ? "https://api.github.com"
                : repository.webBaseUrl + "/api/v3";
    }

    private static URI webArchiveUri(NormalizedGithubRepository repository, String revision) {
        return URI.create(
                repository.webBaseUrl + "/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository) + "/archive/" + encodePath(revision) + ".tar.gz");
    }

    private static String encodePath(String value) {
        String[] segments = value.split("/", -1);
        StringBuilder encoded = new StringBuilder();

        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }

        return encoded.toString();
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

    static List<String> selectScmRevisions(Build build) {
        Set<String> revisions = new LinkedHashSet<>();

        // The build-level revision is normally the immutable revision actually used for the build, so prefer it over
        // the configured branch/tag when both are present.
        if (build.getScmRevision() != null && !build.getScmRevision().isBlank()) {
            revisions.add(build.getScmRevision());
        }

        BuildConfigurationRevisionRef revision = build.getBuildConfigRevision();
        if (revision != null && revision.getScmRevision() != null && !revision.getScmRevision().isBlank()) {
            revisions.add(revision.getScmRevision());
        }

        return List.copyOf(revisions);
    }

    static String selectInternalScmUrl(Build build) {
        SCMRepository repository = build.getScmRepository();
        return repository == null ? null : repository.getInternalUrl();
    }

    /**
     * The pre-alignment revision(s) to use as the {@code base} of the alignment diff, most reliable first.
     *
     * <p>
     * {@code scmBuildConfigRevision} is preferred: it is the resolved commit (the exact pre-alignment revision Reqour
     * based the alignment on) that Reqour mirrored into the internal repository, so it is reachable there. The
     * configured {@code buildConfigRevision.scmRevision} is only a fallback because it is frequently a branch/tag name
     * on the external SCM that does not resolve in the internal repository.
     */
    static List<String> preAlignmentRevisions(Build build) {
        Set<String> revisions = new LinkedHashSet<>();

        if (build.getScmBuildConfigRevision() != null && !build.getScmBuildConfigRevision().isBlank()) {
            revisions.add(build.getScmBuildConfigRevision());
        }

        BuildConfigurationRevisionRef revision = build.getBuildConfigRevision();
        if (revision != null && revision.getScmRevision() != null && !revision.getScmRevision().isBlank()) {
            revisions.add(revision.getScmRevision());
        }

        return List.copyOf(revisions);
    }

    /**
     * The post-alignment revision(s) pushed by Reqour into the internal repository, preferring the immutable commit
     * over
     * the tag. These are the {@code head} candidates of the alignment diff.
     */
    static List<String> postAlignmentRevisions(Build build) {
        Set<String> revisions = new LinkedHashSet<>();

        if (build.getScmRevision() != null && !build.getScmRevision().isBlank()) {
            revisions.add(build.getScmRevision());
        }
        if (build.getScmTag() != null && !build.getScmTag().isBlank()) {
            revisions.add(build.getScmTag());
        }

        return List.copyOf(revisions);
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

        URI uri() {
            return uri;
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
