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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;
import org.jboss.pnc.bacon.config.PncConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class AlignmentLogTest {

    private HttpServer pnc;
    private HttpServer bifrost;
    private final Map<String, String> authorizations = new ConcurrentHashMap<>();

    @BeforeEach
    void start() throws IOException {
        pnc = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        bifrost = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        pnc.createContext("/pnc-rest/v2/builds/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            authorizations.put(path, String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            if (path.equals("/pnc-rest/v2/builds/B1/logs/align")) {
                // the bifrost host PNC is configured with, not reachable from here
                exchange.getResponseHeaders()
                        .add("Location", "https://bifrost.unreachable.invalid/final-log/B1/alignment-log");
                exchange.sendResponseHeaders(307, -1);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
            exchange.close();
        });
        bifrost.createContext("/final-log/B1/alignment-log", exchange -> {
            authorizations.put("bifrost", String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            byte[] body = "line 1\nline 2\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        pnc.start();
        bifrost.start();

        PncConfig pncConfig = new PncConfig();
        pncConfig.setUrl("http://127.0.0.1:" + pnc.getAddress().getPort());
        pncConfig.setBifrostBaseurl("https://bifrost.unreachable.invalid");
        ConfigProfile profile = new ConfigProfile();
        profile.setName("default");
        profile.setPnc(pncConfig);
        profile.setLdapUsernamePassword("user:password");
        profile.setUrlRewrites(
                Map.of("https://bifrost.unreachable.invalid", "http://127.0.0.1:" + bifrost.getAddress().getPort()));
        Config config = new Config();
        config.setActiveProfile(profile);
        Config.setInstance(config);
    }

    @AfterEach
    void stop() {
        pnc.stop(0);
        bifrost.stop(0);
        Config.setInstance(null);
        UrlRewrites.reset();
    }

    @Test
    void followsPncsRedirectToTheRewrittenBifrostWithTheCredentials() throws Exception {
        List<String> lines = new ArrayList<>();
        AlignmentLog.forEachLine("B1", lines::add);

        assertThat(lines).containsExactly("line 1", "line 2");
        assertThat(authorizations.get("/pnc-rest/v2/builds/B1/logs/align")).startsWith("Basic ");
        // the profile's bifrost gets the credentials, as for the build log
        assertThat(authorizations.get("bifrost")).startsWith("Basic ");
    }

    @Test
    void aRedirectToAnotherHostGetsNoCredentials() throws Exception {
        Config.instance().getActiveProfile().getPnc().setBifrostBaseurl("https://other.invalid");
        List<String> lines = new ArrayList<>();
        AlignmentLog.forEachLine("B1", lines::add);

        assertThat(lines).containsExactly("line 1", "line 2");
        assertThat(authorizations.get("bifrost")).isEqualTo("null");
    }

    @Test
    void aBuildWithoutAlignmentLogHasNoLines() throws Exception {
        List<String> lines = new ArrayList<>();
        AlignmentLog.forEachLine("B2", lines::add);

        assertThat(lines).isEmpty();
    }
}
