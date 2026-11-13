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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;
import org.jboss.pnc.dto.Build;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** The GitLab alignment diff loop: credentials, the ancestor check and the commit diff fallback. */
class GitlabAlignmentDiffTest {

    private static final String BASE = "65f00aeed880a0b8a34687b41faeee12459730a2";
    private static final String HEAD = "4e2c2cf0ee5033d0a5ea1ceb212715fbb7ffdff7";
    private static final String URL = "https://gitlab.example.com/group/repo.git";
    private static final String PROJECT = "/api/v4/projects/group%2Frepo/repository";
    private static final String DIFFS = "[{\"old_path\":\"pom.xml\",\"new_path\":\"pom.xml\",\"a_mode\":\"100644\","
            + "\"b_mode\":\"100644\",\"diff\":\"@@ -1 +1 @@\\n-<version>1</version>\\n+<version>%s</version>\\n\"}]";

    @TempDir
    Path tmp;

    private HttpServer gitlab;
    private volatile String mergeBase = BASE;
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void start() throws IOException {
        gitlab = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        gitlab.createContext("/", exchange -> {
            String token = exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN");
            String bearer = exchange.getRequestHeaders().getFirst("Authorization");
            String path = exchange.getRequestURI().getRawPath();
            requests.add((token != null ? "PRIVATE-TOKEN " : bearer != null ? "Bearer " : "none ") + path);
            if (token != null) {
                // this instance only accepts the token as a Bearer header
                send(exchange, 401, "{\"message\":\"401 Unauthorized\"}");
            } else if (path.equals(PROJECT + "/merge_base")) {
                send(exchange, 200, "{\"id\":\"" + mergeBase + "\"}");
            } else if (path.equals(PROJECT + "/compare")) {
                send(exchange, 200, "{\"diffs\":" + String.format(DIFFS, "compared") + "}");
            } else if (path.equals(PROJECT + "/commits/" + HEAD + "/diff")) {
                send(exchange, 200, String.format(DIFFS, "last-commit"));
            } else if (path.equals(PROJECT + "/commits/" + HEAD)) {
                send(exchange, 200, "{\"id\":\"" + HEAD + "\",\"parent_ids\":[\"" + BASE + "\"]}");
            } else {
                send(exchange, 404, "{}");
            }
        });
        gitlab.start();

        ConfigProfile profile = new ConfigProfile();
        profile.setGitlabToken("secret");
        Config config = new Config();
        config.setActiveProfile(profile);
        Config.setInstance(config);
        UrlRewrites.setCommandLineRules(
                List.of("https://gitlab.example.com=http://127.0.0.1:" + gitlab.getAddress().getPort()));
    }

    @AfterEach
    void stop() {
        gitlab.stop(0);
        UrlRewrites.reset();
        Config.setInstance(null);
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Build build() {
        Build build = mock(Build.class);
        when(build.getId()).thenReturn("B1");
        when(build.getScmBuildConfigRevision()).thenReturn(BASE);
        when(build.getScmRevision()).thenReturn(HEAD);
        return build;
    }

    private String diff() throws IOException {
        StringBuilder content = new StringBuilder();
        for (Path part : new BuildSourcesDownloader()
                .downloadAlignmentDiffFrom(build(), URL, tmp.resolve("alignment.diff"), false)) {
            content.append(Files.readString(part));
        }
        return content.toString();
    }

    @Test
    void stopsUsingARejectedTokenAndComparesWithTheNextCredential() throws IOException {
        assertThat(diff()).contains("+<version>compared</version>");

        // the PRIVATE-TOKEN header is rejected once, then Bearer is used for every request
        assertThat(requests).first().asString().startsWith("PRIVATE-TOKEN " + PROJECT + "/merge_base");
        assertThat(requests.subList(1, requests.size())).allMatch(request -> request.startsWith("Bearer "));
        assertThat(requests).filteredOn(request -> request.startsWith("PRIVATE-TOKEN")).hasSize(1);
    }

    @Test
    void fallsBackToTheCommitDiffWhenTheBaseIsNotAnAncestor() throws IOException {
        mergeBase = "1111111111111111111111111111111111111111";

        assertThat(diff()).contains("+<version>last-commit</version>").doesNotContain("compared");
    }

    @Test
    void stopsAtOnceWhenGitLabIsNotReachable() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        UrlRewrites.setCommandLineRules(List.of("https://gitlab.example.com=http://127.0.0.1:" + port));

        assertThatThrownBy(this::diff).hasMessageContaining("gitlab.example.com is not reachable");
        assertThat(requests).isEmpty();
    }
}
