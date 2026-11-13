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

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.pnc.bacon.common.http.SafeHttp;
import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.dto.Build;

import lombok.extern.slf4j.Slf4j;

/** {@link ScmProvider} for GitLab: REST API v4 archives, compare and commit diffs (see {@link GitlabSources}). */
@Slf4j
final class GitlabProvider implements ScmProvider {

    static final GitlabProvider INSTANCE = new GitlabProvider();

    private GitlabProvider() {
    }

    @Override
    public List<GitlabSources.Credential> credentials() {
        return GitlabSources.credentials(ScmCredentials.gitlabToken());
    }

    @Override
    public String tokenHintIfMissing() {
        return ScmCredentials.gitlabToken() == null
                ? "\nIf the repository is not readable without a token, specify a valid 'gitlabToken' (read_api "
                        + "scope) in your Bacon config.yaml active profile, or set the GITLAB_TOKEN environment "
                        + "variable."
                : "";
    }

    @Override
    public List<BuildSourcesDownloader.SourceArchiveCandidate> archiveCandidates(
            ScmRepositoryUrl repository,
            List<String> scmRevisions) {
        Set<BuildSourcesDownloader.SourceArchiveCandidate> candidates = new LinkedHashSet<>();
        // GitLab API v4; 'sha' accepts a commit, branch or tag, and the API (unlike the web archive route) accepts
        // the token
        GitlabSources gitlab = gitlabSources(repository, UrlRewrites.none());
        for (String scmRevision : scmRevisions) {
            if (scmRevision != null && !scmRevision.isBlank()) {
                candidates.add(
                        new BuildSourcesDownloader.SourceArchiveCandidate(
                                gitlab.archiveUri(scmRevision.trim()),
                                BuildSourcesDownloader.ACCEPT_ARCHIVE));
            }
        }
        return List.copyOf(candidates);
    }

    /**
     * Alignment diff from an internal GitLab repository. Like the GitHub path, it is the diff from the pre-alignment
     * revision to the post-alignment one when the former is an ancestor of the latter, else the diff of the
     * post-alignment commit against its parent. GitLab returns per-file JSON diffs, which are rebuilt into a unified
     * diff; files without text diff (binary, or too large for GitLab) are listed in a warning.
     */
    @Override
    public List<Path> alignmentDiff(
            BuildSourcesDownloader downloader,
            Build build,
            ScmRepositoryUrl repository,
            String internalUrl,
            Path diffFile,
            boolean manifest) {
        List<GitlabSources.Credential> credentials = credentials();
        List<String> bases = BuildSourcesDownloader.preAlignmentRevisions(build);
        List<String> heads = BuildSourcesDownloader.postAlignmentRevisions(build);
        if (heads.isEmpty()) {
            throw new RuntimeException(
                    "Build " + build.getId()
                            + " has no post-alignment SCM revision/tag; cannot compute the alignment diff.");
        }

        GitlabSources gitlab = gitlabSources(repository, UrlRewrites.current());
        List<String> failures = new ArrayList<>();
        credentials: for (GitlabSources.Credential credential : credentials) {
            for (String head : heads) {
                for (String base : bases) {
                    try {
                        if (gitlab.isAncestor(base, head, credential)) {
                            log.debug("Alignment diff of build {} from GitLab: {}..{}", build.getId(), base, head);
                            return writeGitlabDiff(
                                    downloader,
                                    build,
                                    diffFile,
                                    gitlab.compare(base, head, credential),
                                    manifest);
                        }
                        failures.add(base + " is not an ancestor of " + head);
                    } catch (RuntimeException e) {
                        BuildSourcesDownloader.throwIfUnreachable(build, URI.create(repository.webBaseUrl), e);
                        failures.add(base + ".." + head + " using " + credential.name() + ": " + e.getMessage());
                        if (credential.header() != null && e instanceof GitlabSources.GitlabApiException
                                && BuildSourcesDownloader
                                        .isAuthenticationFailure(((GitlabSources.GitlabApiException) e).status)) {
                            // the token is rejected: every other request with it would be too
                            continue credentials;
                        }
                    }
                }
                try {
                    GitlabSources.UnifiedDiff diff = gitlab.commitDiff(head, credential);
                    log.warn(
                            "No pre-alignment revision {} of build {} is an ancestor of {}; used the post-alignment"
                                    + " commit diff instead. If alignment produced more than one commit this captures"
                                    + " only the last one.",
                            bases,
                            build.getId(),
                            head);
                    return writeGitlabDiff(downloader, build, diffFile, diff, manifest);
                } catch (RuntimeException e) {
                    BuildSourcesDownloader.throwIfUnreachable(build, URI.create(repository.webBaseUrl), e);
                    failures.add("commit " + head + " using " + credential.name() + ": " + e.getMessage());
                    if (credential.header() != null && e instanceof GitlabSources.GitlabApiException
                            && BuildSourcesDownloader
                                    .isAuthenticationFailure(((GitlabSources.GitlabApiException) e).status)) {
                        continue credentials;
                    }
                }
            }
        }
        throw new RuntimeException(
                "Failed to download alignment diff for build " + build.getId() + " from the repository "
                        + internalUrl + ". Tried:\n - " + String.join("\n - ", failures)
                        + tokenHintIfMissing());
    }

    private static GitlabSources gitlabSources(ScmRepositoryUrl repository, UrlRewrites rewrites) {
        return new GitlabSources(
                repository.webBaseUrl,
                repository.path,
                SafeHttp.newClient(),
                rewrites);
    }

    private List<Path> writeGitlabDiff(
            BuildSourcesDownloader downloader,
            Build build,
            Path diffFile,
            GitlabSources.UnifiedDiff diff,
            boolean manifest) {
        if (!diff.incompleteFiles.isEmpty()) {
            log.warn(
                    "GitLab did not return the content changes of {} file(s) (binary, or too large); they are in the"
                            + " diff as 'Binary files ... differ' only and cannot be applied from it: {}",
                    diff.incompleteFiles.size(),
                    diff.incompleteFiles);
        }
        return downloader.writeAlignmentDiff(build, diffFile, diff.content, manifest);
    }
}
