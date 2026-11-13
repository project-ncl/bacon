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

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * Picks the git provider of an SCM URL (GitLab when the host contains "gitlab", GitHub otherwise) and the matching
 * credentials from the active Bacon profile, so that the same code works whether a repository is on GitHub or GitLab:
 * <ul>
 * <li>GitHub: the {@code githubToken} profile setting,</li>
 * <li>GitLab: the {@code gitlabToken} profile setting, else the {@code GITLAB_TOKEN} environment variable;
 * internal/public projects also work without a token.</li>
 * </ul>
 */
@Slf4j
public final class ScmCredentials {

    /** Username and password (token) for git over HTTPS. */
    public record UsernamePassword(String username, String password) {
    }

    private ScmCredentials() {
    }

    /** Whether the SCM URL (scp-like, ssh or http(s)) is on a GitLab instance. */
    public static boolean isGitlab(String scmUrl) {
        BuildSourcesDownloader.NormalizedGithubRepository repository = BuildSourcesDownloader
                .normalizeGithubRepository(scmUrl);
        return repository != null && repository.isGitlab();
    }

    /**
     * Credentials for cloning the repository over HTTPS with the token of its git provider, if one is configured.
     */
    public static Optional<UsernamePassword> forGitClone(String scmUrl) {
        if (isGitlab(scmUrl)) {
            return Optional.ofNullable(gitlabToken()).map(t -> new UsernamePassword("oauth2", t));
        }
        return Optional.ofNullable(githubToken()).map(t -> new UsernamePassword("x-access-token", t));
    }

    /** The {@code githubToken} of the active profile, or null. */
    static String githubToken() {
        return blankToNull(profileProperty("getGithubToken"));
    }

    /** The {@code gitlabToken} of the active profile, else the {@code GITLAB_TOKEN} environment variable, or null. */
    static String gitlabToken() {
        String token = blankToNull(profileProperty("getGitlabToken"));
        return token != null ? token : blankToNull(System.getenv("GITLAB_TOKEN"));
    }

    private static String profileProperty(String getter) {
        try {
            Class<?> configClass = Class.forName("org.jboss.pnc.bacon.config.Config");
            Object config = configClass.getMethod("instance").invoke(null);
            Object activeProfile = config.getClass().getMethod("getActiveProfile").invoke(config);
            Object value = activeProfile.getClass().getMethod(getter).invoke(activeProfile);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.debug("Cannot read {} from the active Bacon profile", getter, e);
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
