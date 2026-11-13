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
package org.jboss.pnc.bacon.pnc.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class BifrostClientTest {

    private HttpServer bifrost;
    private HttpServer other;
    private final Map<String, String> authorizations = new ConcurrentHashMap<>();
    private ConfigProfile profile;

    @BeforeEach
    void start() throws IOException {
        bifrost = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        other = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        bifrost.createContext("/text", exchange -> {
            authorizations.put("bifrost", String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            if (exchange.getRequestURI().getQuery().contains("follow=true")) {
                // the "follow" flag stands in for a bifrost that redirects to another host
                exchange.getResponseHeaders().add("Location", "http://localhost:" + port(other) + "/text");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            lines(exchange);
        });
        other.createContext("/text", exchange -> {
            authorizations.put("other", String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            lines(exchange);
        });
        bifrost.start();
        other.start();

        profile = new ConfigProfile();
        profile.setName("default");
        profile.setLdapUsernamePassword("user:password");
        Config config = new Config();
        config.setActiveProfile(profile);
        Config.setInstance(config);
    }

    private static void lines(HttpExchange exchange) throws IOException {
        byte[] body = "line 1\nline 2\n".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static int port(HttpServer server) {
        return server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        bifrost.stop(0);
        other.stop(0);
        Config.setInstance(null);
        UrlRewrites.reset();
    }

    @Test
    void sendsCredentialsToBifrostButNotToAnotherHostItRedirectsTo() throws IOException {
        BifrostClient client = new BifrostClient(URI.create("http://127.0.0.1:" + port(bifrost)));

        assertThat(client.getLog("B1", BifrostClient.LogType.BUILD)).containsExactly("line 1", "line 2");
        assertThat(authorizations.get("bifrost")).startsWith("Basic ");

        client.writeLog("B1", true, line -> {}, BifrostClient.LogType.BUILD);
        assertThat(authorizations.get("other")).isEqualTo("null");
    }

    @Test
    void sendsCredentialsToARewrittenBifrost() throws IOException {
        profile.setUrlRewrites(Map.of("https://bifrost.unreachable.invalid", "http://127.0.0.1:" + port(bifrost)));

        List<String> lines = new BifrostClient(URI.create("https://bifrost.unreachable.invalid"))
                .getLog("B1", BifrostClient.LogType.BUILD);

        assertThat(lines).containsExactly("line 1", "line 2");
        assertThat(authorizations.get("bifrost")).startsWith("Basic ");
    }
}
