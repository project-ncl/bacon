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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.pnc.bacon.common.http.Origins;
import org.jboss.pnc.bacon.common.http.SafeHttp;
import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.common.log.LogSanitizer;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.resteasy.util.HttpResponseCodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.extern.slf4j.Slf4j;

/**
 * Downloads the sources and the alignment diff of a PNC build from its internal repository, on GitHub (GitHub
 * Enterprise) or GitLab: the {@link ScmProvider} of the repository URL ({@link ScmRepositoryUrl}) gives what differs
 * between them, this class what they share.
 *
 * This intentionally does not use /builds/{id}/scm-archive because that endpoint can fail with 406 via the generated
 * REST client, or return browser/SAML HTML instead of a tarball.
 */
@Slf4j
public class BuildSourcesDownloader {

    static final int PREVIEW_BYTES = 300;

    private static final Pattern SCM_TAG_COMMIT_SUFFIX = Pattern.compile("-[0-9a-f]{8}$");
    private static final int HEADER_WIDTH = 100;
    // The build attribute holding the groupId:artifactId PNC names the build after (Attributes.BUILD_BREW_NAME).
    private static final String BREW_BUILD_NAME = "BREW_BUILD_NAME";
    private static final ObjectWriter MANIFEST_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    static final String ACCEPT_ARCHIVE = "application/x-gzip, application/gzip, application/x-tar, application/octet-stream";
    static final String ACCEPT_GITHUB_API = "application/json";
    static final String ACCEPT_DIFF = "application/vnd.github.diff";

    private static final int HTTP_MOVED_PERMANENTLY = 301;
    private static final int HTTP_FOUND = 302;
    private static final int HTTP_SEE_OTHER = 303;
    private static final int HTTP_TEMPORARY_REDIRECT = 307;
    private static final int HTTP_PERMANENT_REDIRECT = 308;

    public Path downloadSources(Build build, Path targetFile) {
        return downloadSources(build.getId(), scmUrlCandidates(build), selectScmRevisions(build), targetFile);
    }

    /**
     * Like {@link #downloadSources(String, String, List, Path)}, trying the SCM URLs in order (see
     * {@link #scmUrlCandidates(Build)}).
     */
    public Path downloadSources(String buildId, List<String> scmUrls, List<String> scmRevisions, Path targetFile) {
        return firstSuccessful(
                "sources",
                buildId,
                scmUrls,
                url -> downloadSources(buildId, url, scmRevisions, targetFile),
                BuildSourcesDownloader::tokenHintIfMissing);
    }

    /**
     * Downloads the tar.gz archive of the sources of a build from its SCM repository, on GitHub or GitLab (see
     * {@link ScmCredentials}).
     *
     * @param buildId the build, for messages
     * @param scmUrl the repository the build was built from
     * @param scmRevisions the revisions to try, most precise first
     * @param targetFile where to write the archive
     * @return {@code targetFile}
     */
    public Path downloadSources(String buildId, String scmUrl, List<String> scmRevisions, Path targetFile) {
        try {
            if (scmUrl == null || scmUrl.isBlank()) {
                throw new RuntimeException("Build " + buildId + " has no SCM URL.");
            }
            if (scmRevisions == null || scmRevisions.isEmpty()) {
                throw new RuntimeException("Build " + buildId + " has no SCM revision.");
            }

            List<GitlabSources.Credential> credentials = ScmProvider.of(ScmRepositoryUrl.parse(scmUrl)).credentials();

            List<SourceArchiveCandidate> candidates = sourceArchiveCandidates(scmUrl, scmRevisions);
            if (candidates.isEmpty()) {
                throw new RuntimeException(
                        "Could not derive any source archive URL for build " + buildId
                                + " from SCM URL: " + scmUrl);
            }

            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }

            Path temporaryFile = targetFile.resolveSibling(targetFile.getFileName().toString() + ".tmp");
            List<String> failures = new ArrayList<>();

            // A host that cannot be reached, or a token it rejects, fails the same way for every other candidate:
            // skip them instead of waiting for each one.
            Set<String> unreachableHosts = new LinkedHashSet<>();
            Set<String> rejectedCredentials = new LinkedHashSet<>();
            try {
                for (SourceArchiveCandidate candidate : candidates) {
                    String host = String.valueOf(candidate.uri.getHost()).toLowerCase(Locale.ROOT);
                    if (unreachableHosts.contains(host)) {
                        failures.add(candidate.uri + ": skipped, " + host + " is not reachable");
                        continue;
                    }
                    for (GitlabSources.Credential credential : credentials) {
                        if (rejectedCredentials.contains(host + " " + credential.name())) {
                            continue;
                        }
                        try {
                            log.debug(
                                    "Downloading sources for build {} from {} to {}",
                                    buildId,
                                    candidate.uri,
                                    targetFile);

                            downloadWithRedirects(buildId, candidate, credential, temporaryFile);
                            assertGzipArchive(temporaryFile, candidate.uri);

                            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                            return targetFile;
                        } catch (RuntimeException e) {
                            Files.deleteIfExists(temporaryFile);
                            failures.add(candidate.uri + " using " + credential.name() + ": " + e.getMessage());
                            log.debug(
                                    "Source archive candidate failed for build {}: {}",
                                    buildId,
                                    failures.get(failures.size() - 1));
                            if (SafeHttp.isConnectFailure(e)) {
                                unreachableHosts.add(host);
                                break;
                            }
                            if (credential.header() != null && e instanceof HttpStatusException
                                    && isAuthenticationFailure(((HttpStatusException) e).status)) {
                                rejectedCredentials.add(host + " " + credential.name());
                            }
                        }
                    }
                }
            } finally {
                Files.deleteIfExists(temporaryFile);
            }

            throw new RuntimeException(
                    "Failed to download sources for build " + buildId + ". Tried candidates:\n - "
                            + String.join("\n - ", failures) + tokenHintIfMissing(scmUrl));
        } catch (IOException e) {
            throw new RuntimeException("Failed to download sources for build " + buildId, e);
        }
    }

    /**
     * Downloads the diff of the changes PNC/Reqour committed on top of the original SCM revision during alignment.
     *
     * <p>
     * Reqour clones the upstream sources into the internal SCM repository and commits the alignment changes on top of
     * the original revision, pushing and tagging the result. Both the pre-alignment revision and the post-alignment
     * commit are therefore present in the internal repository, so the alignment changes can be captured as a single
     * compare diff ({@code <pre-alignment>...<post-alignment>}) of GitHub or GitLab. The resulting unified diff applies
     * cleanly on top of the original upstream sources (for example with {@code git apply} or {@code patch -p1}).
     *
     * <p>
     * Leaked credentials are masked in the metadata files only ({@link #maskSecrets}), and the diff is split into
     * categories
     * ({@link AlignmentDiff}), grouped into files written next to {@code diffFile} as {@code <name>-<n>-<part>.diff}
     * (see {@link #partFile}) when present, each with a comment header saying what can be skipped. Each part applies on
     * its own; applying all of them reproduces the complete alignment.
     *
     * @param build the build to compute the alignment diff for
     * @param diffFile the file name the part names are derived from; not written itself
     * @param manifest whether to also write {@code <name>-manifest.json}, describing the categorization of every file
     * @return the files written: one file per category present in the diff, and the manifest when requested
     */
    public List<Path> downloadAlignmentDiff(Build build, Path diffFile, boolean manifest) {
        return firstSuccessful(
                "alignment diff",
                build.getId(),
                scmUrlCandidates(build),
                url -> downloadAlignmentDiffFrom(build, url, diffFile, manifest),
                BuildSourcesDownloader::tokenHintIfMissing);
    }

    /**
     * The result for the first URL that works. With a single URL its failure is thrown as it is; with several, one
     * exception lists the failure of each URL, with the token hint of each provider once, the last failure as its cause
     * and all of them as suppressed exceptions.
     *
     * @param hintFor the hint {@code download} appends to its failure messages for a URL, or "" for none
     */
    static <T> T firstSuccessful(
            String what,
            String buildId,
            List<String> urls,
            Function<String, T> download,
            Function<String, String> hintFor) {
        if (urls.isEmpty()) {
            throw new RuntimeException("Build " + buildId + " has no SCM URL; cannot download the " + what + ".");
        }
        List<String> failures = new ArrayList<>();
        List<RuntimeException> exceptions = new ArrayList<>();
        Set<String> hints = new LinkedHashSet<>();
        for (String url : urls) {
            try {
                return download.apply(url);
            } catch (RuntimeException e) {
                if (urls.size() == 1) {
                    throw e;
                }
                log.debug("Cannot download the {} of build {} from {}: {}", what, buildId, url, e.getMessage());
                String message = String.valueOf(e.getMessage());
                String hint = hintFor.apply(url);
                if (!hint.isEmpty() && message.endsWith(hint)) {
                    message = message.substring(0, message.length() - hint.length());
                    hints.add(hint);
                }
                failures.add(url + ": " + message);
                exceptions.add(e);
            }
        }
        RuntimeException failure = new RuntimeException(
                "Failed to download the " + what + " of build " + buildId + " from any of its SCM URLs:\n - "
                        + String.join("\n - ", failures) + String.join("", hints),
                exceptions.get(exceptions.size() - 1));
        exceptions.forEach(failure::addSuppressed);
        throw failure;
    }

    /** The alignment diff read from one SCM URL (see {@link #downloadAlignmentDiff(Build, Path, boolean)}). */
    List<Path> downloadAlignmentDiffFrom(Build build, String internalUrl, Path diffFile, boolean manifest) {
        ScmRepositoryUrl repository = ScmRepositoryUrl.parse(internalUrl);
        if (repository == null) {
            throw new RuntimeException("Could not parse the SCM URL for build " + build.getId() + ": " + internalUrl);
        }
        return ScmProvider.of(repository).alignmentDiff(this, build, repository, internalUrl, diffFile, manifest);
    }

    /** A hint to configure the token of the repository's git provider, when none is configured. */
    static String tokenHintIfMissing(String scmUrl) {
        ScmRepositoryUrl repository;
        try {
            repository = ScmRepositoryUrl.parse(scmUrl);
        } catch (IllegalArgumentException e) {
            repository = null;
        }
        return ScmProvider.of(repository).tokenHintIfMissing();
    }

    List<Path> writeAlignmentDiff(Build build, Path diffFile, String rawDiff, boolean manifest) {
        AlignmentDiff diff = AlignmentDiff.split(
                maskSecrets(rawDiff),
                alignedVersionsFromTag(build.getScmTag()),
                build.getAttributes() != null ? build.getAttributes().get(BREW_BUILD_NAME) : null);
        try {
            return writeAlignmentDiffParts(diff, diffFile, manifest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write alignment diff for build " + build.getId(), e);
        }
    }

    /**
     * Masks leaked credentials ({@link LogSanitizer}) in the metadata files of the diff only (reports and backups the
     * manipulators write); sources and build files are kept byte-exact so that the diff reproduces the aligned sources.
     * See {@link AlignmentDiff#maskSecretsInMetadata}.
     */
    static String maskSecrets(String diff) {
        List<String> unmasked = new ArrayList<>();
        String masked = AlignmentDiff.maskSecretsInMetadata(diff, LogSanitizer::maskKnownSecrets, unmasked::add);
        if (!unmasked.isEmpty()) {
            log.warn(
                    "The alignment diff may contain credentials in {} source or build file(s), left unmasked so that"
                            + " the diff still applies; review it before sharing: {}",
                    unmasked.size(),
                    unmasked);
        }
        return masked;
    }

    /**
     * The aligned project version Reqour named the post-alignment tag after: the execution root version, with a
     * {@code -<8 hex>} commit suffix when a tag with that name already existed. Both forms are returned; the one that
     * is not a version simply never matches.
     */
    static List<String> alignedVersionsFromTag(String scmTag) {
        if (scmTag == null || scmTag.isBlank()) {
            return List.of();
        }
        String withoutCommit = SCM_TAG_COMMIT_SUFFIX.matcher(scmTag).replaceFirst("");
        return withoutCommit.equals(scmTag) ? List.of(scmTag) : List.of(scmTag, withoutCommit);
    }

    /**
     * Writes the parts of the diff present, in {@link AlignmentDiff.Category priority order}, to their {@link #partFile
     * part files}, and the {@link AlignmentDiff#manifest(java.util.function.Function) manifest} to
     * {@link #manifestFile(Path)} when requested.
     *
     * @param diffFile the file name the part names are derived from; not written itself
     * @param manifest whether to write the manifest
     * @return the files written, in apply order, followed by the manifest when written
     */
    static List<Path> writeAlignmentDiffParts(AlignmentDiff diff, Path diffFile, boolean manifest) throws IOException {
        if (diffFile.getParent() != null) {
            Files.createDirectories(diffFile.getParent());
        }

        List<Path> written = new ArrayList<>();
        for (AlignmentDiff.Part part : AlignmentDiff.Part.values()) {
            StringBuilder content = new StringBuilder();
            for (AlignmentDiff.Category category : part.categories()) {
                if (diff.has(category)) {
                    content.append(categoryHeader(category, diff)).append(diff.part(category));
                }
            }
            if (content.length() > 0) {
                Path partFile = partFile(diffFile, part);
                Files.writeString(partFile, partHeader(part, diff) + content, StandardCharsets.UTF_8);
                written.add(partFile);
            }
        }
        if (written.isEmpty()) {
            log.info("The alignment diff has no changes, no part written");
        }

        if (manifest) {
            Path manifestFile = manifestFile(diffFile);
            MANIFEST_WRITER.writeValue(
                    manifestFile.toFile(),
                    diff.manifest(
                            part -> part.categories().stream().anyMatch(diff::has)
                                    ? partFile(diffFile, part).getFileName().toString()
                                    : null));
            written.add(manifestFile);
        }

        projectLines(diff).forEach(line -> log.info(line));
        diff.warnings().forEach(warning -> log.warn("Alignment diff: {}", warning));
        return written;
    }

    /**
     * The comment written at the top of a part file, with the project version change repeated in every part, so that
     * it is found in the same place whatever the manipulator and whichever parts the alignment produced.
     */
    static String partHeader(AlignmentDiff.Part part, AlignmentDiff diff) {
        StringBuilder header = new StringBuilder();
        comment(
                header,
                "PNC alignment diff, part " + part.number() + " of " + AlignmentDiff.Part.values().length + ": "
                        + part.id() + " (" + (part.required() ? "required" : "optional") + ", " + part.summary()
                        + ")");
        projectLines(diff).forEach(line -> comment(header, line));
        comment(header, "Apply on top of the upstream sources in numeric order, alone or together.");
        header.append("#\n");
        return header.toString();
    }

    /**
     * The comment written before the changes of a category in its part file: what they are, what they are needed for
     * and when they can be skipped, plus the removed repositories for {@link AlignmentDiff.Category#REPOSITORIES} and
     * the unrecognized added files and warnings for {@link AlignmentDiff.Category#BUILD}. {@code git apply} and
     * {@code patch} ignore the text before and between the file sections.
     */
    static String categoryHeader(AlignmentDiff.Category category, AlignmentDiff diff) {
        StringBuilder header = new StringBuilder();
        comment(header, "--- " + category.id() + " ---");
        comment(header, "Contains:   ", category.contents());
        comment(header, "Needed for: ", category.neededFor());
        comment(header, "Skip when:  ", category.skip());
        if (category == AlignmentDiff.Category.REPOSITORIES) {
            for (AlignmentDiff.Repository repository : diff.removedRepositories()) {
                comment(
                        header,
                        "Removed:    ",
                        Stream.of(repository.id(), repository.url())
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining(" ")) + " (" + repository.file() + ", " + repository.kind()
                                + ")");
            }
        }
        if (category == AlignmentDiff.Category.BUILD) {
            for (String file : diff.unrecognizedAddedFiles()) {
                comment(header, "Unknown:    ", file + " (added file not recognized, kept here)");
            }
            for (String warning : diff.warnings()) {
                comment(header, "Warning:    ", warning);
            }
        }
        header.append("#\n");
        return header.toString();
    }

    /**
     * The project identifier, e.g. {@code Project: org.acme:widget}, followed by one line per project version change,
     * e.g. {@code Project version: 1.4.0 -> 1.4.0.redhat-00002}, or a single line saying none was found. The files
     * holding the change are listed in the manifest only.
     */
    static List<String> projectLines(AlignmentDiff diff) {
        List<String> lines = new ArrayList<>();
        lines.add("Project: " + (diff.project() != null ? diff.project() : "unknown"));
        if (diff.projectVersions().isEmpty()) {
            lines.add("Project version: no change found");
        }
        diff.projectVersions()
                .stream()
                .map(
                        version -> "Project version: " + (version.original() != null ? version.original() : "?")
                                + " -> " + version.aligned())
                .forEach(lines::add);
        return lines;
    }

    /** Appends the text as "# " comment lines wrapped at {@value #HEADER_WIDTH} characters. */
    private static void comment(StringBuilder header, String text) {
        comment(header, "", text);
    }

    /**
     * Appends the label and the text as "# " comment lines wrapped at {@value #HEADER_WIDTH} characters, the wrapped
     * lines indented under the start of the text.
     */
    private static void comment(StringBuilder header, String label, String text) {
        String indent = "# " + " ".repeat(label.length());
        StringBuilder line = new StringBuilder("# ").append(label);
        boolean empty = true;
        for (String word : text.split(" ")) {
            if (!empty && line.length() + 1 + word.length() > HEADER_WIDTH) {
                header.append(line).append('\n');
                line = new StringBuilder(indent);
                empty = true;
            }
            line.append(empty ? "" : " ").append(word);
            empty = false;
        }
        header.append(line).append('\n');
    }

    /**
     * Derives the path of a part from the diff file name, numbered by priority so that sorting the parts by name gives
     * the apply order, e.g. {@code alignment.diff -> alignment-1-build.diff, alignment-4-noise.diff}.
     */
    static Path partFile(Path diffFile, AlignmentDiff.Part part) {
        return diffFile.resolveSibling(baseName(diffFile) + "-" + part.suffix() + ".diff");
    }

    /** Derives the manifest path from the diff file name, e.g. {@code alignment.diff -> alignment-manifest.json}. */
    static Path manifestFile(Path diffFile) {
        return diffFile.resolveSibling(baseName(diffFile) + "-manifest.json");
    }

    private static String baseName(Path diffFile) {
        String name = diffFile.getFileName().toString();
        return name.endsWith(".diff") ? name.substring(0, name.length() - ".diff".length()) : name;
    }

    private void downloadWithRedirects(
            String buildId,
            SourceArchiveCandidate candidate,
            GitlabSources.Credential credential,
            Path targetFile) {
        try {
            HttpClient httpClient = SafeHttp.newClient();
            UrlRewrites rewrites = UrlRewrites.current();

            // the Accept header changes after the first request, so redirects are followed here, with SafeHttp's rules
            URI requestedUri = candidate.uri;
            URI originalUri = rewrites.apply(candidate.uri);
            URI currentUri = originalUri;

            for (int redirectCount = 0; redirectCount <= SafeHttp.MAX_REDIRECTS; redirectCount++) {
                boolean firstRequest = redirectCount == 0;
                String acceptHeader = firstRequest ? candidate.initialAcceptHeader : ACCEPT_ARCHIVE;

                HttpRequest request = newSourceArchiveRequest(
                        currentUri,
                        acceptHeader,
                        credential,
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

                    SafeHttp.Redirect redirect = SafeHttp
                            .redirect(requestedUri, previousUri, locationHeader.get(), rewrites)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Refused redirect from https to http: " + locationHeader.get()));
                    requestedUri = redirect.requested;
                    currentUri = redirect.target;

                    log.debug(
                            "Following source archive redirect for build {}: {} -> {}",
                            buildId,
                            previousUri,
                            currentUri);
                    continue;
                }

                if (statusCode != HttpResponseCodes.SC_OK) {
                    String responsePreview = responsePreview(response.body());
                    throw new HttpStatusException(
                            statusCode,
                            "HTTP status: " + statusCode + " from " + currentUri
                                    + (responsePreview.isBlank() ? "" : ". Response preview: " + responsePreview));
                }

                try (InputStream in = response.body()) {
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }

                return;
            }

            throw new RuntimeException("Too many redirects; maximum allowed redirects: " + SafeHttp.MAX_REDIRECTS);
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
            GitlabSources.Credential credential,
            boolean sendAuthorization) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(SafeHttp.RESPONSE_TIMEOUT)
                .header("Accept", acceptHeader)
                .header("User-Agent", "bacon")
                .GET();

        if (sendAuthorization && credential.header() != null) {
            builder.header(credential.header(), credential.value());
        }

        return builder.build();
    }

    private static boolean shouldSendAuthorization(URI originalUri, URI currentUri) {
        // Like curl -L: do not forward Authorization to another origin such as codeload.github.ibm.com.
        return Origins.same(originalUri, currentUri);
    }

    static List<SourceArchiveCandidate> sourceArchiveCandidates(String scmUrl, String scmRevision) {
        return sourceArchiveCandidates(scmUrl, List.of(scmRevision));
    }

    static List<SourceArchiveCandidate> sourceArchiveCandidates(String scmUrl, List<String> scmRevisions) {
        ScmRepositoryUrl repository = ScmRepositoryUrl.parse(scmUrl);
        if (repository == null) {
            return List.of();
        }
        return ScmProvider.of(repository).archiveCandidates(repository, scmRevisions);
    }

    /**
     * The SCM URLs to read a build's sources and alignment diff from, in order: the internal URL the build used
     * ({@code scmUrl}), then the repository's current internal URL (it may have moved since, e.g. to another git
     * provider). The external URL is not used: it does not have the aligned commits and tags.
     */
    public static List<String> scmUrlCandidates(Build build) {
        SCMRepository repository = build.getScmRepository();
        return scmUrlCandidates(build.getScmUrl(), repository == null ? null : repository.getInternalUrl());
    }

    /**
     * Like {@link #scmUrlCandidates(Build)} for the build's internal URL and the repository's current one. URLs of the
     * same repository (e.g. its scp-like and https forms) are tried once.
     */
    public static List<String> scmUrlCandidates(String buildUrl, String currentInternalUrl) {
        Map<String, String> urls = new LinkedHashMap<>();
        for (String url : new String[] { buildUrl, currentInternalUrl }) {
            if (url != null && !url.isBlank()) {
                ScmRepositoryUrl repository = ScmRepositoryUrl.parse(url);
                String key = repository == null ? url
                        : repository.host.toLowerCase(Locale.ROOT) + "/" + repository.path.toLowerCase(Locale.ROOT);
                urls.putIfAbsent(key, url);
            }
        }
        return List.copyOf(urls.values());
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
        byte[] firstBytes;
        try (InputStream in = Files.newInputStream(file)) {
            firstBytes = in.readNBytes(PREVIEW_BYTES);
        }
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

    /** Stops trying other revisions or credentials when the host cannot be reached at all. */
    static void throwIfUnreachable(Build build, URI uri, RuntimeException e) {
        if (SafeHttp.isConnectFailure(e)) {
            throw new RuntimeException(
                    "Failed to download alignment diff for build " + build.getId() + ": " + uri.getHost()
                            + " is not reachable (" + e.getMessage() + ")",
                    e);
        }
    }

    /** A non-successful HTTP response. */
    static final class HttpStatusException extends RuntimeException {
        final int status;

        HttpStatusException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    static boolean isAuthenticationFailure(int status) {
        return status == 401 || status == 403;
    }

    static void closeQuietly(InputStream inputStream) {
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

    static final class SourceArchiveCandidate {

        private final URI uri;
        private final String initialAcceptHeader;

        SourceArchiveCandidate(URI uri, String initialAcceptHeader) {
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
}
