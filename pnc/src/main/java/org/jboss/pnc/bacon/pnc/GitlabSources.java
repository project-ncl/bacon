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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Access to an internal GitLab repository through the GitLab REST API v4: source archive URL, and the alignment diff
 * rebuilt as a unified diff from the JSON diffs of the compare/commit APIs.
 */
@Slf4j
final class GitlabSources {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int PREVIEW_BYTES = 300;
    private static final int PER_PAGE = 100;

    /**
     * A token sent in one header (GitLab accepts PRIVATE-TOKEN and Authorization: Bearer), or no token at all when
     * {@link #header} is null.
     */
    static final class Credential {
        final String header;
        final String value;
        final String name;

        Credential(String header, String value, String name) {
            this.header = header;
            this.value = value;
            this.name = name;
        }
    }

    static final Credential NO_TOKEN = new Credential(null, null, "no token");

    /** HTTP error from GitLab. */
    static final class GitlabApiException extends RuntimeException {
        final int status;

        GitlabApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    /** A rebuilt unified diff, with the files GitLab did not return as text (binary or too large). */
    static final class UnifiedDiff {
        final String content;
        final List<String> incompleteFiles;

        UnifiedDiff(String content, List<String> incompleteFiles) {
            this.content = content;
            this.incompleteFiles = incompleteFiles;
        }
    }

    /** Reads the content of a file at a revision; returns null when it cannot be read. */
    interface ContentFetcher {
        byte[] fetch(String path, String ref);
    }

    private final String apiBaseUrl;
    private final String projectPath;
    private final HttpClient httpClient;

    /**
     * @param webBaseUrl e.g. {@code https://gitlab.cee.redhat.com}
     * @param projectPath full project path, e.g. {@code pnc-workspace/airlift/airbase}
     */
    GitlabSources(String webBaseUrl, String projectPath, HttpClient httpClient) {
        this.apiBaseUrl = stripTrailingSlash(webBaseUrl) + "/api/v4";
        this.projectPath = projectPath;
        this.httpClient = httpClient;
    }

    static boolean isGitlabHost(String host) {
        return host != null && host.toLowerCase().contains("gitlab");
    }

    /** The credentials to try for a token, most common first. */
    static List<Credential> credentials(String token) {
        List<Credential> credentials = new ArrayList<>();
        if (token != null && !token.isBlank()) {
            credentials.add(new Credential("PRIVATE-TOKEN", token, "PRIVATE-TOKEN"));
            credentials.add(new Credential("Authorization", "Bearer " + token, "Bearer"));
        }
        // internal/public projects can be read without a token
        credentials.add(NO_TOKEN);
        return credentials;
    }

    URI archiveUri(String revision) {
        return URI.create(projectUrl() + "/repository/archive.tar.gz?sha=" + encode(revision));
    }

    URI projectUri() {
        return URI.create(projectUrl());
    }

    /**
     * Whether {@code base} is an ancestor of {@code head} (their merge base is {@code base}).
     */
    boolean isAncestor(String base, String head, Credential credential) {
        JsonNode mergeBase = getJson(
                URI.create(
                        projectUrl() + "/repository/merge_base?refs%5B%5D=" + encode(base) + "&refs%5B%5D="
                                + encode(head)),
                credential);
        String mergeBaseId = mergeBase.path("id").asText("");
        return !mergeBaseId.isEmpty() && (mergeBaseId.equals(base) || mergeBaseId.startsWith(base));
    }

    /**
     * The diff from {@code base} to {@code head} (their trees compared directly, like {@code git diff base head}).
     */
    UnifiedDiff compare(String base, String head, Credential credential) {
        JsonNode compare = getJson(
                URI.create(
                        projectUrl() + "/repository/compare?from=" + encode(base) + "&to=" + encode(head)
                                + "&straight=true"),
                credential);
        if (compare.path("compare_timeout").asBoolean(false)) {
            log.warn("GitLab timed out computing the comparison {}..{}; the diff may be incomplete", base, head);
        }
        return toUnifiedDiff(compare.path("diffs"), contentFetcher(credential), base, head);
    }

    /**
     * The diff of the commit {@code ref} against its parent.
     */
    UnifiedDiff commitDiff(String ref, Credential credential) {
        List<JsonNode> diffs = new ArrayList<>();
        for (int page = 1;; page++) {
            JsonNode pageDiffs = getJson(
                    URI.create(
                            projectUrl() + "/repository/commits/" + encode(ref) + "/diff?per_page=" + PER_PAGE
                                    + "&page=" + page),
                    credential);
            pageDiffs.forEach(diffs::add);
            if (pageDiffs.size() < PER_PAGE) {
                break;
            }
        }
        JsonNode commit = getJson(URI.create(projectUrl() + "/repository/commits/" + encode(ref)), credential);
        String parent = commit.path("parent_ids").path(0).asText(null);
        return toUnifiedDiff(JSON.valueToTree(diffs), contentFetcher(credential), parent, ref);
    }

    /**
     * Rebuilds a git unified diff from GitLab's JSON diff entries ({@code old_path}, {@code new_path}, {@code a_mode},
     * {@code b_mode}, {@code new_file}, {@code deleted_file}, {@code renamed_file}, {@code diff}). Binary files
     * (GitLab returns "Binary files ... differ") and files GitLab left out ({@code too_large}: patch over the
     * instance's
     * diff size limit, 200 KiB by default) are kept as "Binary files ... differ" sections and reported as incomplete.
     * Empty new or deleted files are complete with their header only.
     */
    static UnifiedDiff toUnifiedDiff(JsonNode diffs) {
        return toUnifiedDiff(diffs, null, null, null);
    }

    /**
     * Like {@link #toUnifiedDiff(JsonNode)}, but the content GitLab left out is read with {@code fetcher} (at
     * {@code fromRef} for the old side, {@code toRef} for the new side) and written in full: a new text file as a
     * regular
     * hunk, anything else as a {@code GIT binary patch} with literal contents. Only files that cannot be read stay
     * incomplete.
     */
    static UnifiedDiff toUnifiedDiff(JsonNode diffs, ContentFetcher fetcher, String fromRef, String toRef) {
        StringBuilder out = new StringBuilder();
        List<String> incomplete = new ArrayList<>();
        for (JsonNode entry : diffs) {
            String oldPath = entry.path("old_path").asText();
            String newPath = entry.path("new_path").asText(oldPath);
            String aMode = entry.path("a_mode").asText("");
            String bMode = entry.path("b_mode").asText("");
            boolean newFile = entry.path("new_file").asBoolean(false);
            boolean deletedFile = entry.path("deleted_file").asBoolean(false);
            boolean renamedFile = entry.path("renamed_file").asBoolean(false);
            String diff = entry.path("diff").asText("");

            out.append("diff --git a/").append(oldPath).append(" b/").append(newPath).append('\n');
            if (newFile) {
                out.append("new file mode ").append(bMode).append('\n');
            } else if (deletedFile) {
                out.append("deleted file mode ").append(aMode).append('\n');
            } else if (!aMode.isEmpty() && !bMode.isEmpty() && !aMode.equals(bMode)) {
                out.append("old mode ").append(aMode).append('\n');
                out.append("new mode ").append(bMode).append('\n');
            }
            if (renamedFile) {
                out.append("rename from ").append(oldPath).append('\n');
                out.append("rename to ").append(newPath).append('\n');
            }

            if (diff.startsWith("@@")) {
                out.append("--- ").append(newFile ? "/dev/null" : "a/" + oldPath).append('\n');
                out.append("+++ ").append(deletedFile ? "/dev/null" : "b/" + newPath).append('\n');
                out.append(diff);
                if (!diff.endsWith("\n")) {
                    out.append('\n');
                }
                continue;
            }

            boolean truncated = entry.path("too_large").asBoolean(false) || entry.path("collapsed").asBoolean(false);
            // GitLab writes "Binary files ... differ" for binary files. An empty diff is complete for an empty
            // new/deleted file (e.g. PME's marker file), a pure rename and a pure mode change; otherwise GitLab left
            // the content out (file over its diff size limit).
            boolean contentMissing = !diff.isBlank() || truncated
                    || (!newFile && !deletedFile && !renamedFile && aMode.equals(bMode));
            if (!contentMissing) {
                continue;
            }

            String filled = fetcher == null || isSubmodule(aMode) || isSubmodule(bMode) ? null
                    : fullContent(fetcher, fromRef, toRef, oldPath, newPath, newFile, deletedFile);
            if (filled != null) {
                out.append(filled);
            } else {
                out.append("Binary files ")
                        .append(newFile ? "/dev/null" : "a/" + oldPath)
                        .append(" and ")
                        .append(deletedFile ? "/dev/null" : "b/" + newPath)
                        .append(" differ\n");
                incomplete.add(newPath);
            }
        }
        return new UnifiedDiff(out.toString(), incomplete);
    }

    /** The full content of a file GitLab left out, in patch form, or null when it cannot be read. */
    private static String fullContent(
            ContentFetcher fetcher,
            String fromRef,
            String toRef,
            String oldPath,
            String newPath,
            boolean newFile,
            boolean deletedFile) {
        byte[] oldContent = null;
        if (!newFile) {
            oldContent = fromRef == null ? null : fetcher.fetch(oldPath, fromRef);
            if (oldContent == null) {
                return null;
            }
        }
        byte[] newContent = null;
        if (!deletedFile) {
            newContent = toRef == null ? null : fetcher.fetch(newPath, toRef);
            if (newContent == null) {
                return null;
            }
        }
        if (newFile && GitPatchContent.isText(newContent)) {
            return GitPatchContent.newTextFileHunk(newPath, newContent);
        }
        return GitPatchContent.binaryPatch(oldContent, newContent);
    }

    private static boolean isSubmodule(String mode) {
        return "160000".equals(mode);
    }

    /**
     * Reads files with the credential the diff was obtained with, and without a token when that fails: GitLab answers
     * some endpoints (e.g. compare) for a token it rejects on others (e.g. raw files), as happens with a token that is
     * not allowed to access this project, while the project itself can be read without a token.
     */
    private ContentFetcher contentFetcher(Credential credential) {
        return (path, ref) -> {
            URI uri = URI.create(projectUrl() + "/repository/files/" + encode(path) + "/raw?ref=" + encode(ref));
            try {
                return getBytes(uri, credential);
            } catch (RuntimeException e) {
                if (credential.header == null) {
                    log.warn("Cannot read {} at {} from GitLab: {}", path, ref, e.getMessage());
                    return null;
                }
                log.debug("Cannot read {} at {} with {}, trying without a token: {}", path, ref, credential.name, e);
            }
            try {
                return getBytes(uri, NO_TOKEN);
            } catch (RuntimeException e) {
                log.warn("Cannot read {} at {} from GitLab: {}", path, ref, e.getMessage());
                return null;
            }
        };
    }

    private JsonNode getJson(URI uri, Credential credential) {
        try {
            return JSON.readTree(getBytes(uri, credential));
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON from " + uri, e);
        }
    }

    private byte[] getBytes(URI uri, Credential credential) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .header("Accept", "application/json, */*")
                    .header("User-Agent", "bacon");
            if (credential.header != null) {
                request.header(credential.header, credential.value);
            }
            HttpResponse<InputStream> response = httpClient
                    .send(request.GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                byte[] bytes = body.readAllBytes();
                if (response.statusCode() != 200) {
                    String preview = new String(bytes, 0, Math.min(bytes.length, PREVIEW_BYTES), StandardCharsets.UTF_8)
                            .replaceAll("\\s+", " ");
                    throw new GitlabApiException(
                            response.statusCode(),
                            "HTTP status: " + response.statusCode() + " from " + uri
                                    + (preview.isBlank() ? "" : ". Response preview: " + preview));
                }
                return bytes;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while requesting " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while requesting " + uri, e);
        }
    }

    private String projectUrl() {
        return apiBaseUrl + "/projects/" + encode(projectPath);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
