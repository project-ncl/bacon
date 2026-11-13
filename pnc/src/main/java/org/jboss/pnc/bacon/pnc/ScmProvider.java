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

import java.nio.file.Path;
import java.util.List;

import org.jboss.pnc.dto.Build;

/**
 * What differs between the git providers of internal repositories: credentials, source archive URLs and the alignment
 * diff. {@link BuildSourcesDownloader} keeps what they share (URL fallback, the archive download loop, writing the
 * diff).
 */
interface ScmProvider {

    /** The provider of a repository; GitHub when the URL is not one bacon can parse. */
    static ScmProvider of(ScmRepositoryUrl repository) {
        return repository != null && repository.isGitlab() ? GitlabProvider.INSTANCE : GithubProvider.INSTANCE;
    }

    /**
     * The credentials to try, in order, with no token last on purpose: public (and on GitLab internal) repositories
     * are readable without one, so sources still download with a missing or wrong token.
     */
    List<GitlabSources.Credential> credentials();

    /** A hint to configure the provider's token, when none is configured; "" otherwise. */
    String tokenHintIfMissing();

    /** The URLs to try for the source archive of the revisions, most precise first. */
    List<BuildSourcesDownloader.SourceArchiveCandidate> archiveCandidates(
            ScmRepositoryUrl repository,
            List<String> scmRevisions);

    /** The alignment diff of the build from the repository at {@code internalUrl}, written with {@code downloader}. */
    List<Path> alignmentDiff(
            BuildSourcesDownloader downloader,
            Build build,
            ScmRepositoryUrl repository,
            String internalUrl,
            Path diffFile,
            boolean manifest);
}
