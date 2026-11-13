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
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.pnc.bacon.common.http.SafeHttp;
import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.dto.Build;
import org.jboss.resteasy.util.HttpResponseCodes;

import lombok.extern.slf4j.Slf4j;

/** {@link ScmProvider} for GitHub and GitHub Enterprise: REST API v3 tarballs and compare diffs. */
@Slf4j
final class GithubProvider implements ScmProvider {

    static final GithubProvider INSTANCE = new GithubProvider();

    private static final Pattern COMMIT_SHA = Pattern.compile("[0-9a-fA-F]{40}");

    private GithubProvider() {
    }

    @Override
    public List<GitlabSources.Credential> credentials() {
        List<GitlabSources.Credential> credentials = new ArrayList<>();
        String githubToken = ScmCredentials.githubToken();
        if (githubToken != null && !githubToken.isBlank()) {
            for (String authorizationHeader : authorizationHeaders(githubToken)) {
                credentials.add(
                        new GitlabSources.Credential(
                                "Authorization",
                                authorizationHeader,
                                authSchemeName(authorizationHeader)));
            }
        }
        // public repositories can be read without a token
        credentials.add(GitlabSources.NO_TOKEN);
        return credentials;
    }

    @Override
    public String tokenHintIfMissing() {
        return ScmCredentials.githubToken() == null
                ? "\nPlease specify a valid 'githubToken' in your Bacon config.yaml active profile."
                : "";
    }

    @Override
    public List<BuildSourcesDownloader.SourceArchiveCandidate> archiveCandidates(
            ScmRepositoryUrl repository,
            List<String> scmRevisions) {
        Set<BuildSourcesDownloader.SourceArchiveCandidate> candidates = new LinkedHashSet<>();

        // Prefer the authenticated REST endpoint. Browser archive URLs on GitHub Enterprise commonly redirect to
        // SAML/login pages and return HTML or 406 instead of an archive.
        for (String scmRevision : scmRevisions) {
            for (String revisionCandidate : revisionCandidates(scmRevision)) {
                candidates.add(
                        new BuildSourcesDownloader.SourceArchiveCandidate(
                                apiTarballUri(repository, revisionCandidate),
                                BuildSourcesDownloader.ACCEPT_GITHUB_API));
            }
        }

        // Keep the browser archive endpoint as a fallback for installations where the REST endpoint is unavailable.
        for (String scmRevision : scmRevisions) {
            for (String revisionCandidate : revisionCandidates(scmRevision)) {
                candidates.add(
                        new BuildSourcesDownloader.SourceArchiveCandidate(
                                webArchiveUri(repository, revisionCandidate),
                                BuildSourcesDownloader.ACCEPT_ARCHIVE));
            }
        }

        return List.copyOf(candidates);
    }

    /**
     * The diff of the GitHub compare API from the pre-alignment revision to the post-alignment one, else the diff of
     * the post-alignment commit when the repository is readable but the compare fails.
     */
    @Override
    public List<Path> alignmentDiff(
            BuildSourcesDownloader downloader,
            Build build,
            ScmRepositoryUrl repository,
            String internalUrl,
            Path diffFile,
            boolean manifest) {
        String githubToken = ScmCredentials.githubToken();
        if (githubToken == null || githubToken.isBlank()) {
            throw new RuntimeException(
                    "Failed to download alignment diff for build " + build.getId()
                            + ". Please specify a valid 'githubToken' in your Bacon config.yaml active profile.");
        }

        List<String> bases = BuildSourcesDownloader.preAlignmentRevisions(build);
        if (bases.isEmpty()) {
            throw new RuntimeException(
                    "Build " + build.getId()
                            + " has no pre-alignment SCM revision; cannot compute the alignment diff.");
        }

        List<String> heads = BuildSourcesDownloader.postAlignmentRevisions(build);
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
                        BuildSourcesDownloader.throwIfUnreachable(build, compareUri, e);
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
                                + ". The repository " + internalUrl
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
                        BuildSourcesDownloader.throwIfUnreachable(build, commitUri, e);
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
                            + " must be reachable in the repository " + internalUrl
                            + ". Tried candidates:\n - " + String.join("\n - ", failures));
        }

        return downloader.writeAlignmentDiff(build, diffFile, rawDiff, manifest);
    }

    private static List<String> authorizationHeaders(String githubToken) {
        return List.of("Bearer " + githubToken, "token " + githubToken);
    }

    private static String authSchemeName(String authorizationHeader) {
        int spaceIndex = authorizationHeader.indexOf(' ');
        return spaceIndex > 0 ? authorizationHeader.substring(0, spaceIndex) : "unknown";
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

    private static URI apiTarballUri(ScmRepositoryUrl repository, String revision) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository) + "/tarball/" + encodePath(revision));
    }

    /**
     * Builds the GitHub (Enterprise) compare URL that yields the diff between {@code base} and {@code head} in the
     * given
     * repository. Both refs must be resolvable within that single repository.
     */
    static URI compareDiffUri(ScmRepositoryUrl repository, String base, String head) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository) + "/compare/" + encodePath(base) + "..."
                        + encodePath(head));
    }

    /**
     * Builds the GitHub (Enterprise) URL for a single commit. With the diff media type this yields the diff introduced
     * by that commit relative to its parent.
     */
    static URI commitDiffUri(ScmRepositoryUrl repository, String ref) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository) + "/commits/" + encodePath(ref));
    }

    /**
     * Builds the GitHub (Enterprise) URL of the repository itself, used to probe whether the token can access it.
     */
    static URI repoApiUri(ScmRepositoryUrl repository) {
        return URI.create(
                apiBaseUrl(repository) + "/repos/" + encodePath(repository.owner) + "/"
                        + encodePath(repository.repository));
    }

    private static String apiBaseUrl(ScmRepositoryUrl repository) {
        return "github.com".equalsIgnoreCase(repository.host)
                ? "https://api.github.com"
                : repository.webBaseUrl + "/api/v3";
    }

    private static URI webArchiveUri(ScmRepositoryUrl repository, String revision) {
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

    private String fetchDiff(URI uri, String authorizationHeader) {
        try {
            HttpResponse<InputStream> response = SafeHttp.get(
                    SafeHttp.newClient(),
                    uri,
                    Map.of("Accept", BuildSourcesDownloader.ACCEPT_DIFF, "User-Agent", "bacon"),
                    Map.of("Authorization", authorizationHeader),
                    UrlRewrites.current());
            int statusCode = response.statusCode();
            try (InputStream in = response.body()) {
                byte[] body = in.readAllBytes();
                if (statusCode != HttpResponseCodes.SC_OK) {
                    String responsePreview = new String(
                            body,
                            0,
                            Math.min(body.length, BuildSourcesDownloader.PREVIEW_BYTES),
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

    private int probeStatus(URI uri, String authorizationHeader) {
        try {
            HttpResponse<InputStream> response = SafeHttp.get(
                    SafeHttp.newClient(),
                    uri,
                    Map.of("Accept", BuildSourcesDownloader.ACCEPT_GITHUB_API, "User-Agent", "bacon"),
                    Map.of("Authorization", authorizationHeader),
                    UrlRewrites.current());
            BuildSourcesDownloader.closeQuietly(response.body());
            return response.statusCode();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
