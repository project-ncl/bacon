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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

class UrlRewritesTest {

    private static UrlRewrites rewrites(String... rules) {
        return new UrlRewrites(List.of(rules).stream().map(UrlRewrites::parse).toList());
    }

    private static String apply(UrlRewrites rewrites, String uri) {
        return rewrites.apply(URI.create(uri)).toString();
    }

    @Test
    void replacesTheOriginAndKeepsPathAndQuery() {
        UrlRewrites rewrites = rewrites("https://bifrost.example.com=https://bifrost.internal:8443");
        assertEquals(
                "https://bifrost.internal:8443/final-log/B1/alignment-log?x=a%20b",
                apply(rewrites, "https://bifrost.example.com/final-log/B1/alignment-log?x=a%20b"));
    }

    @Test
    void replacesAPathPrefixWithAnotherPath() {
        UrlRewrites rewrites = rewrites("https://indy.example.com/api/content=https://mirror.example.com/indy/");
        assertEquals(
                "https://mirror.example.com/indy/maven/g/a/1/a-1.jar",
                apply(rewrites, "https://indy.example.com/api/content/maven/g/a/1/a-1.jar"));
    }

    @Test
    void matchesWholePathSegmentsOnly() {
        UrlRewrites rewrites = rewrites("https://host.example.com/api=https://other.example.com");
        assertEquals("https://other.example.com", apply(rewrites, "https://host.example.com/api"));
        assertEquals("https://host.example.com/apix/1", apply(rewrites, "https://host.example.com/apix/1"));
    }

    @Test
    void matchesSchemeHostAndPortExactly() {
        UrlRewrites rewrites = rewrites("https://host.example.com=https://other.example.com");
        assertEquals("https://other.example.com/a", apply(rewrites, "HTTPS://HOST.example.com:443/a"));
        assertEquals("http://host.example.com/a", apply(rewrites, "http://host.example.com/a"));
        assertEquals("https://host.example.com:8443/a", apply(rewrites, "https://host.example.com:8443/a"));
        assertEquals("https://sub.host.example.com/a", apply(rewrites, "https://sub.host.example.com/a"));
    }

    @Test
    void longestPrefixWinsThenTheEarlierRule() {
        UrlRewrites rewrites = rewrites(
                "https://h.example.com=https://short.example.com",
                "https://h.example.com/api/content=https://long.example.com",
                "https://h.example.com/api/content=https://later.example.com");
        assertEquals("https://long.example.com/x", apply(rewrites, "https://h.example.com/api/content/x"));
        assertEquals("https://short.example.com/api/other", apply(rewrites, "https://h.example.com/api/other"));
    }

    @Test
    void withoutRulesUrlsAreUnchanged() {
        UrlRewrites rewrites = rewrites();
        assertTrue(rewrites.isEmpty());
        assertEquals("https://h.example.com/x", apply(rewrites, "https://h.example.com/x"));
    }

    @Test
    void rejectsInvalidRules() {
        for (String rule : new String[] {
                "https://h.example.com",
                "h.example.com=https://o.example.com",
                "ftp://h.example.com=https://o.example.com",
                "https://h.example.com=https://user:secret@o.example.com",
                "https://h.example.com?q=1=https://o.example.com",
                "https://h.example.com=https://o.example.com#f" }) {
            assertThrows(IllegalArgumentException.class, () -> UrlRewrites.parse(rule), rule);
        }
    }
}
