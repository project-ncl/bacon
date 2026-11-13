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
package org.jboss.pnc.bacon.pnc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import org.jboss.pnc.bacon.common.http.SafeHttp;
import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** The source archive download loop: redirects, credentials and URL rewrites. */
class SourceArchiveRedirectTest {

    private static final String REVISION = "0123456789abcdef0123456789abcdef01234567";
    private static final String SCM_URL = "https://github.example.com/org/repo.git";

    @TempDir
    Path tmp;

    private HttpServer github;
    private HttpServer other;
    private volatile String location;
    private final Map<String, String> authorizations = new ConcurrentHashMap<>();
    private final List<String> otherRequests = new ArrayList<>();

    @BeforeEach
    void start() throws IOException {
        github = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        other = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        github.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/codeload/")) {
                authorizations.put("github-codeload", header(exchange));
                archive(exchange);
                return;
            }
            authorizations.putIfAbsent("github", header(exchange));
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        other.createContext("/", exchange -> {
            otherRequests.add(exchange.getRequestURI().getPath());
            authorizations.put("other", header(exchange));
            archive(exchange);
        });
        github.start();
        other.start();

        ConfigProfile profile = new ConfigProfile();
        profile.setGithubToken("token");
        Config config = new Config();
        config.setActiveProfile(profile);
        Config.setInstance(config);
        UrlRewrites.setCommandLineRules(
                List.of(
                        "https://github.example.com=" + base(github),
                        "https://codeload.example.com=" + base(other)));
    }

    @AfterEach
    void stop() {
        github.stop(0);
        other.stop(0);
        UrlRewrites.reset();
        Config.setInstance(null);
    }

    private static String header(HttpExchange exchange) {
        return String.valueOf(exchange.getRequestHeaders().getFirst("Authorization"));
    }

    private static void archive(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream gzip = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(gzip)) {
            out.write("sources".getBytes());
        }
        byte[] body = gzip.toByteArray();
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String base(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private Path download() {
        return new BuildSourcesDownloader()
                .downloadSources("B1", SCM_URL, List.of(REVISION), tmp.resolve("sources.tar.gz"));
    }

    @Test
    void sendsNoCredentialsToAnotherOrigin() throws IOException {
        // another origin (after the rewrite too): the redirect is followed without the token
        location = "https://codeload.example.com/x.tar.gz";

        assertThat(Files.size(download())).isPositive();
        assertThat(authorizations.get("github")).isEqualTo("Bearer token");
        assertThat(authorizations.get("other")).isEqualTo("null");
    }

    @Test
    void keepsCredentialsOnARelativeRedirect() {
        location = "/codeload/x.tar.gz";

        download();
        assertThat(authorizations.get("github-codeload")).isEqualTo("Bearer token");
    }

    @Test
    void rewritesAnAbsoluteRedirect() {
        location = "https://codeload.example.com/x.tar.gz";

        download();
        assertThat(otherRequests).containsExactly("/x.tar.gz");
        assertThat(authorizations.get("other")).isEqualTo("null");
    }

    @Test
    void refusesARedirectFromHttpsToHttp() {
        location = "http://codeload.example.com/x.tar.gz";

        assertThatThrownBy(this::download).hasMessageContaining("Refused redirect from https to http");
        assertThat(otherRequests).isEmpty();
    }

    @Test
    void stopsAfterTooManyRedirects() {
        location = "/loop";

        assertThatThrownBy(this::download)
                .hasMessageContaining("Too many redirects; maximum allowed redirects: " + SafeHttp.MAX_REDIRECTS);
    }
}
