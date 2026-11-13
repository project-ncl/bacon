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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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

    @BeforeEach
    void start() throws IOException {
        first = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        second = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        first.createContext("/start", exchange -> {
            record(exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().add("Location", base(second) + "/end?q=1");
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
    void followsRedirectsAndSendsCredentialsOnlyToTheFirstOrigin() throws Exception {
        HttpResponse<InputStream> response = SafeHttp.get(
                SafeHttp.newClient(),
                URI.create(base(first) + "/start"),
                Map.of(),
                Map.of("Authorization", "Bearer t"));

        try (InputStream body = response.body()) {
            assertEquals(200, response.statusCode());
            assertEquals("content", new String(body.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(URI.create(base(second) + "/end?q=1"), response.uri());
        assertEquals("Bearer t", authorizations.get("/start"));
        assertEquals("<none>", authorizations.get("/end?q=1"));
    }
}
