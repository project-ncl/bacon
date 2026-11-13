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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void onlyAnHttpLocationSentForAnHttpsRequestIsADowngrade() {
        URI https = URI.create("https://pnc.example.com/builds/B1/logs/align");
        // what the server sent decides, even if a rule then rewrites the https target to plain http
        assertFalse(SafeHttp.isDowngrade(https, "https://bifrost.example.com/final-log/B1/alignment-log"));
        assertTrue(SafeHttp.isDowngrade(https, "http://bifrost.example.com/final-log/B1/alignment-log"));
        assertFalse(SafeHttp.isDowngrade(https, "/final-log/B1/alignment-log"));
        assertFalse(SafeHttp.isDowngrade(URI.create("http://pnc.example.com/x"), "http://bifrost.example.com/y"));
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
