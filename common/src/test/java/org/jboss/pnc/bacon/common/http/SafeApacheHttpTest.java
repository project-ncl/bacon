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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class SafeApacheHttpTest {

    private HttpServer first;
    private HttpServer second;
    private final Map<String, String> authorizations = new ConcurrentHashMap<>();

    @BeforeEach
    void start() throws IOException {
        first = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        second = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        first.createContext("/start", exchange -> {
            authorizations.put("first", String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            // a host that is not reachable here: only a rewrite makes the redirect work
            exchange.getResponseHeaders().add("Location", "https://redirected.invalid/end?q=1");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        second.createContext("/end", exchange -> {
            authorizations.put("second", String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            byte[] body = "content".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        first.start();
        second.start();
    }

    @AfterEach
    void stop() {
        first.stop(0);
        second.stop(0);
    }

    private String base(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private String get(List<URI> credentialOrigins) throws IOException {
        UrlRewrites rewrites = new UrlRewrites(
                List.of(
                        UrlRewrites.parse("https://first.invalid=" + base(first)),
                        UrlRewrites.parse("https://redirected.invalid=" + base(second))));
        URI original = URI.create("https://first.invalid/start");
        try (CloseableHttpClient client = SafeApacheHttp.builder(() -> rewrites, () -> credentialOrigins).build()) {
            HttpGet request = new HttpGet(rewrites.apply(original));
            request.setHeader("Authorization", "Basic secret");
            try (CloseableHttpResponse response = client.execute(request, SafeApacheHttp.context(original))) {
                return response.getStatusLine().getStatusCode() + " " + EntityUtils.toString(response.getEntity());
            }
        }
    }

    @Test
    void rewritesRedirectsAndSendsCredentialsOnlyToTrustedOrigins() throws IOException {
        assertEquals("200 content", get(List.of(URI.create("https://first.invalid"))));

        assertEquals("Basic secret", authorizations.get("first"));
        assertEquals("null", authorizations.get("second"));
    }

    @Test
    void sendsCredentialsToARedirectTargetThatIsTrusted() throws IOException {
        assertEquals(
                "200 content",
                get(List.of(URI.create("https://first.invalid"), URI.create("https://redirected.invalid"))));

        assertEquals("Basic secret", authorizations.get("second"));
    }
}
