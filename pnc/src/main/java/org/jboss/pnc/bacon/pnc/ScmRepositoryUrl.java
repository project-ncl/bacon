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

/** The host and path of an internal SCM repository URL, and whether it is on GitLab. */
final class ScmRepositoryUrl {

    final String host;
    final String webBaseUrl;
    final String owner;
    final String repository;
    /** The whole path of the repository (GitLab projects can be in nested groups). */
    final String path;
    final boolean gitlab;

    private ScmRepositoryUrl(
            String host,
            String webBaseUrl,
            String owner,
            String repository,
            String path) {
        this.host = host;
        this.webBaseUrl = webBaseUrl;
        this.owner = owner;
        this.repository = repository;
        this.path = path;
        this.gitlab = GitlabSources.isGitlabHost(host);
    }

    boolean isGitlab() {
        return gitlab;
    }

    /**
     * The repository of an SCM URL (scp-like {@code git@host:path}, {@code ssh://} or {@code http(s)://}), or null
     * when it is not one.
     */
    static ScmRepositoryUrl parse(String scmUrl) {
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

    private static ScmRepositoryUrl fromHostAndPath(String host, String path) {
        if (host == null || host.isBlank() || path == null || path.isBlank()) {
            return null;
        }

        String[] parts = path.split("/");
        if (parts.length < 2) {
            return null;
        }

        return new ScmRepositoryUrl(host, "https://" + host, parts[0], parts[1], path);
    }

    private static String stripGitSuffix(String value) {
        return value != null && value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }

    private static String stripLeadingSlash(String value) {
        return value != null && value.startsWith("/") ? value.substring(1) : value;
    }
}
