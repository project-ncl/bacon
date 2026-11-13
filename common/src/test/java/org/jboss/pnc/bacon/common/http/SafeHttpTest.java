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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class SafeHttpTest {

    private HttpServer first;
    private HttpServer second;
    private final Map<String, String> authorizations = new ConcurrentHashMap<>();
    // a host that is not reachable here: only a rewrite makes the redirect work
    private volatile String location = "http://redirected.invalid/end?q=1";

    @BeforeEach
    void start() throws IOException {
        first = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        second = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        first.createContext("/start", exchange -> {
            record(exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        });
        second.createContext("/end", exchange -> {
            record(exchange.getRequestURI().toString(), exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "content".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        first.start();
        second.start();
    }

    private void record(String key, String authorization) {
        authorizations.put(key, authorization == null ? "<none>" : authorization);
    }

    @AfterEach
    void stop() {
        first.stop(0);
        second.stop(0);
    }

    private String base(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void rewritesTheUrlAndRedirectsAndSendsCredentialsOnlyToTheFirstOrigin() throws Exception {
        UrlRewrites rewrites = new UrlRewrites(
                List.of(
                        UrlRewrites.parse("http://unreachable.invalid=" + base(first)),
                        UrlRewrites.parse("http://redirected.invalid=" + base(second))));

        HttpResponse<InputStream> response = SafeHttp.get(
                SafeHttp.newClient(),
                URI.create("http://unreachable.invalid/start"),
                Map.of(),
                Map.of("Authorization", "Bearer t"),
                rewrites);

        try (InputStream body = response.body()) {
            assertEquals(200, response.statusCode());
            assertEquals("content", new String(body.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(URI.create(base(second) + "/end?q=1"), response.uri());
        assertEquals("Bearer t", authorizations.get("/start"));
        assertEquals("<none>", authorizations.get("/end?q=1"));
    }

    private static String target(String requested, String current, String location, String... rules) {
        UrlRewrites rewrites = new UrlRewrites(java.util.Arrays.stream(rules).map(UrlRewrites::parse).toList());
        return SafeHttp.redirect(URI.create(requested), URI.create(current), location, rewrites)
                .map(r -> r.requested + " -> " + r.target)
                .orElse("refused");
    }

    @Test
    void aPathLocationStaysOnTheHostRequested() {
        // the response came from the rewritten host: a path stays there and is not rewritten again
        assertEquals(
                "https://pnc.example.com/final-log/B1 -> http://127.0.0.1:8080/final-log/B1",
                target(
                        "https://pnc.example.com/logs/B1",
                        "http://127.0.0.1:8080/logs/B1",
                        "/final-log/B1",
                        "https://pnc.example.com=http://127.0.0.1:8080"));
        assertEquals(
                "https://pnc.example.com/logs/x -> http://127.0.0.1:8080/logs/x",
                target("https://pnc.example.com/logs/B1", "http://127.0.0.1:8080/logs/B1", "x"));
    }

    @Test
    void aProtocolRelativeLocationNamesAHostAndIsRewritten() {
        assertEquals(
                "https://bifrost.example.com/final-log/B1 -> http://127.0.0.1:9090/final-log/B1",
                target(
                        "https://pnc.example.com/logs/B1",
                        "https://pnc.example.com/logs/B1",
                        "//bifrost.example.com/final-log/B1",
                        "https://bifrost.example.com=http://127.0.0.1:9090"));
    }

    @Test
    void theFragmentOfALocationIsDropped() {
        assertEquals(
                "https://bifrost.example.com/a?q=1 -> https://bifrost.example.com/a?q=1",
                target(
                        "https://pnc.example.com/x",
                        "https://pnc.example.com/x",
                        "https://bifrost.example.com/a?q=1#f"));
        assertEquals(
                "https://pnc.example.com/b -> https://pnc.example.com/b",
                target("https://pnc.example.com/x", "https://pnc.example.com/x", "/b#f"));
    }

    @Test
    void onlyAnHttpLocationSentForAnHttpsRequestIsADowngrade() {
        // what the server sent decides, even if a rule then rewrites the https target to plain http
        assertEquals(
                "https://bifrost.example.com/a -> http://127.0.0.1:9090/a",
                target(
                        "https://pnc.example.com/x",
                        "https://pnc.example.com/x",
                        "https://bifrost.example.com/a",
                        "https://bifrost.example.com=http://127.0.0.1:9090"));
        assertEquals(
                "refused",
                target("https://pnc.example.com/x", "https://pnc.example.com/x", "http://bifrost.example.com/a"));
        // a path or protocol-relative Location keeps the scheme
        assertEquals(
                "https://pnc.example.com/a -> http://127.0.0.1:8080/a",
                target("https://pnc.example.com/x", "http://127.0.0.1:8080/x", "/a"));
        assertEquals(
                "http://bifrost.example.com/a -> http://bifrost.example.com/a",
                target("http://pnc.example.com/x", "http://pnc.example.com/x", "http://bifrost.example.com/a"));
    }

    @Test
    void followsAnHttpsRedirectThatARuleRewritesToPlainHttp() throws Exception {
        location = "https://bifrost.invalid/end?q=1";
        UrlRewrites rewrites = new UrlRewrites(
                List.of(
                        UrlRewrites.parse("https://pnc.invalid=" + base(first)),
                        UrlRewrites.parse("https://bifrost.invalid=" + base(second))));

        HttpResponse<InputStream> response = SafeHttp.get(
                SafeHttp.newClient(),
                URI.create("https://pnc.invalid/start"),
                Map.of(),
                Map.of(),
                rewrites);

        try (InputStream body = response.body()) {
            assertEquals(200, response.statusCode());
        }
        assertEquals(URI.create(base(second) + "/end?q=1"), response.uri());
    }

    @Test
    void doesNotFollowARedirectFromHttpsToHttpSentByTheServer() throws Exception {
        location = "http://bifrost.invalid/end?q=1";
        UrlRewrites rewrites = new UrlRewrites(
                List.of(
                        UrlRewrites.parse("https://pnc.invalid=" + base(first)),
                        UrlRewrites.parse("http://bifrost.invalid=" + base(second))));

        HttpResponse<InputStream> response = SafeHttp.get(
                SafeHttp.newClient(),
                URI.create("https://pnc.invalid/start"),
                Map.of(),
                Map.of(),
                rewrites);

        try (InputStream body = response.body()) {
            assertEquals(307, response.statusCode());
        }
        assertNull(authorizations.get("/end?q=1"));
    }

    @Test
    void sendsCredentialsToTrustedOriginsAfterARedirect() throws Exception {
        UrlRewrites rewrites = new UrlRewrites(
                List.of(
                        UrlRewrites.parse("http://unreachable.invalid=" + base(first)),
                        UrlRewrites.parse("http://redirected.invalid=" + base(second))));

        HttpResponse<InputStream> response = SafeHttp.get(
                SafeHttp.newClient(),
                URI.create("http://unreachable.invalid/start"),
                Map.of(),
                Map.of("Authorization", "Bearer t"),
                rewrites,
                List.of(URI.create("http://redirected.invalid")));

        try (InputStream body = response.body()) {
            assertEquals(200, response.statusCode());
        }
        assertEquals("Bearer t", authorizations.get("/start"));
        assertEquals("Bearer t", authorizations.get("/end?q=1"));
    }

    @Test
    void withoutRewritesAnUnfollowableRedirectFailsToConnect() {
        UrlRewrites none = new UrlRewrites(List.of());
        try {
            SafeHttp.get(SafeHttp.newClient(), URI.create(base(first) + "/start"), Map.of(), Map.of(), none);
        } catch (IOException | InterruptedException expected) {
            assertNull(authorizations.get("/end?q=1"));
            return;
        }
        throw new AssertionError("redirect to an unknown host should fail");
    }
}
