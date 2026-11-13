/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2026 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.common.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.pnc.bacon.config.Config;

import lombok.extern.slf4j.Slf4j;

/**
 * Rewrites the URLs bacon downloads from, like git's {@code url.<base>.insteadOf}: a URL that starts with a rule's
 * {@code from} prefix gets that prefix replaced by the rule's {@code to}. For hosts that PNC hands out (artifact
 * {@code publicUrl}s, bifrost, redirects) but that are reachable from where bacon runs only at another address.
 * <p>
 * Rules come from the {@code --url-rewrite <from>=<to>} option, then the profile's {@code urlRewrites} map. A prefix
 * matches whole path segments; the longest matching prefix wins, a command line rule over a profile rule with the same
 * prefix.
 */
@Slf4j
public final class UrlRewrites {

    // set once per command, like Config's profile; see reset() for tests
    private static volatile List<String> commandLineRules = List.of();

    private static volatile Cached cached;

    private static final Set<URI> WARNED_PLAIN_HTTP = ConcurrentHashMap.newKeySet();

    private final List<Rule> rules;

    UrlRewrites(List<Rule> rules) {
        List<Rule> sorted = new ArrayList<>(rules);
        // longest prefix first; the sort is stable, so the earlier (command line) rule wins on equal prefixes
        sorted.sort(Comparator.comparingInt((Rule r) -> r.from.getRawPath().length()).reversed());
        this.rules = List.copyOf(sorted);
    }

    /**
     * Sets the {@code --url-rewrite} values.
     *
     * @throws IllegalArgumentException if a value is not {@code <from>=<to>} with absolute http(s) URLs
     */
    public static void setCommandLineRules(Collection<String> values) {
        List<String> copy = values == null ? List.of() : List.copyOf(values);
        copy.forEach(UrlRewrites::parse);
        commandLineRules = copy;
    }

    /** Clears the command line rules, for tests. */
    public static void reset() {
        commandLineRules = List.of();
        cached = null;
    }

    /**
     * The rules of the command line and the active profile, parsed once for as long as both stay the same.
     *
     * @throws RuntimeException if the configuration cannot be loaded, like {@link Config#instance()}
     */
    public static UrlRewrites current() {
        List<String> commandLine = commandLineRules;
        Map<String, String> profileRules = Config.instance().getActiveProfile().getUrlRewrites();
        Cached last = cached;
        if (last != null && last.commandLine == commandLine && last.profile == profileRules) {
            return last.rewrites;
        }
        List<Rule> rules = new ArrayList<>();
        commandLine.forEach(value -> rules.add(parse(value)));
        if (profileRules != null) {
            profileRules.forEach((from, to) -> rules.add(rule(from, to, "urlRewrites entry '" + from + "'")));
        }
        UrlRewrites rewrites = new UrlRewrites(rules);
        cached = new Cached(commandLine, profileRules, rewrites);
        return rewrites;
    }

    private static final class Cached {
        final List<String> commandLine;
        final Map<String, String> profile;
        final UrlRewrites rewrites;

        Cached(List<String> commandLine, Map<String, String> profile, UrlRewrites rewrites) {
            this.commandLine = commandLine;
            this.profile = profile;
            this.rewrites = rewrites;
        }
    }

    /** {@code uri} with the longest matching prefix replaced, or {@code uri} itself when no rule matches. */
    public URI apply(URI uri) {
        for (Rule rule : rules) {
            Optional<URI> rewritten = rule.apply(uri);
            if (rewritten.isPresent()) {
                log.debug("Rewrote {} to {}", uri, rewritten.get());
                if ("http".equalsIgnoreCase(rule.to.getScheme()) && WARNED_PLAIN_HTTP.add(rule.to)) {
                    log.warn("Rewriting URLs to plain http {}: credentials sent to it are not encrypted", rule.to);
                }
                return rewritten.get();
            }
        }
        return uri;
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    static Rule parse(String value) {
        int separator = value == null ? -1 : value.indexOf('=');
        if (separator < 0) {
            throw new IllegalArgumentException("Invalid URL rewrite '" + value + "': expected <from>=<to>");
        }
        return rule(value.substring(0, separator), value.substring(separator + 1), "URL rewrite '" + value + "'");
    }

    private static Rule rule(String from, String to, String what) {
        return new Rule(base(from, what), base(to, what));
    }

    /** An absolute http(s) URL without user info, query or fragment, and without a trailing slash. */
    private static URI base(String value, String what) {
        URI uri;
        try {
            uri = new URI(value == null ? "" : value.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid " + what + ": " + e.getMessage(), e);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme) || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "Invalid " + what + ": expected absolute http(s) URLs such as https://host[:port][/path]");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Invalid " + what + ": user info, query and fragment are not allowed");
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return URI.create(scheme.toLowerCase() + "://" + uri.getRawAuthority() + path);
    }

    static final class Rule {
        final URI from;
        final URI to;

        Rule(URI from, URI to) {
            this.from = from;
            this.to = to;
        }

        Optional<URI> apply(URI uri) {
            if (!SafeHttp.sameOrigin(from, uri)) {
                return Optional.empty();
            }
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String prefix = from.getRawPath();
            if (!path.startsWith(prefix) || path.length() > prefix.length() && path.charAt(prefix.length()) != '/') {
                return Optional.empty();
            }
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            return Optional.of(URI.create(to + path.substring(prefix.length()) + query));
        }
    }
}
