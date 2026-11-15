/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.SCMRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Downloads through PNC (orch) with {@code scmSource} orch, and as the fallback of auto when the git provider cannot
 * be reached.
 */
class ScmSourceFallbackTest {

    /** A rule sends this host to a closed local port: the direct download fails at once. */
    private static final String UNREACHABLE_SCM = "https://scm.unreachable.example.com/pnc-workspace/repo.git";

    @TempDir
    Path tmp;

    private HttpServer orchServer;
    private final List<String> orchRequests = new ArrayList<>();
    private final List<String> authorizations = new ArrayList<>();

    @BeforeEach
    void configure() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        Config config = new Config();
        config.setActiveProfile(new ConfigProfile());
        Config.setInstance(config);
        UrlRewrites.setCommandLineRules(List.of("https://scm.unreachable.example.com=http://127.0.0.1:" + closedPort));
    }

    @AfterEach
    void tearDown() {
        if (orchServer != null) {
            orchServer.stop(0);
        }
        UrlRewrites.reset();
        Config.setInstance(null);
    }

    @Test
    void scmSourceValues() {
        assertThat(ScmSource.parse(null)).isEqualTo(ScmSource.AUTO);
        assertThat(ScmSource.parse(" Orch ")).isEqualTo(ScmSource.ORCH);
        assertThat(ScmSource.parse("direct")).isEqualTo(ScmSource.DIRECT);
        assertThatThrownBy(() -> ScmSource.parse("reqour")).hasMessageContaining("auto, direct or orch");
    }

    @Test
    void restUrlKeepsOnlySchemeHostAndPort() {
        assertThat(OrchScmContent.restUrl("https://orch.example.com:8443/some/path/"))
                .isEqualTo(URI.create("https://orch.example.com:8443/pnc-rest/v2"));
    }

    @Test
    void orchOnlyDownloadsTheArchiveThroughPnc() throws Exception {
        byte[] archive = gzip("sources");
        orch(exchange -> send(exchange, 200, archive, "application/gzip"));

        Path target = downloader(ScmSource.ORCH)
                .downloadSources("B1", List.of(UNREACHABLE_SCM), List.of("abc"), tmp.resolve("src.tar.gz"));

        assertThat(Files.readAllBytes(target)).isEqualTo(archive);
        assertThat(orchRequests).containsExactly("/pnc-rest/v2/builds/B1/scm-archive/content");
        assertThat(authorizations).containsExactly("Bearer token");
    }

    @Test
    void autoFallsBackToPncWhenTheGitProviderIsUnreachable() throws Exception {
        byte[] archive = gzip("sources");
        orch(exchange -> send(exchange, 200, archive, "application/gzip"));

        Path target = downloader(ScmSource.AUTO)
                .downloadSources("B1", List.of(UNREACHABLE_SCM), List.of("abc"), tmp.resolve("src.tar.gz"));

        assertThat(Files.readAllBytes(target)).isEqualTo(archive);
    }

    @Test
    void autoReportsBothFailures() throws Exception {
        orch(
                exchange -> send(
                        exchange,
                        404,
                        "{\"errorMessage\":\"no revision\"}".getBytes(StandardCharsets.UTF_8),
                        "application/json"));

        assertThatThrownBy(
                () -> downloader(ScmSource.AUTO)
                        .downloadSources("B1", List.of(UNREACHABLE_SCM), List.of("abc"), tmp.resolve("src.tar.gz")))
                .hasMessageContaining("from the git provider and through PNC")
                .hasMessageContaining("Git provider:")
                .hasMessageContaining("PNC: HTTP status: 404")
                .hasMessageContaining("no revision");
        assertThat(tmp.resolve("src.tar.gz")).doesNotExist();
    }

    @Test
    void directOnlyNeverCallsPnc() throws Exception {
        orch(exchange -> send(exchange, 200, gzip("x"), "application/gzip"));

        assertThatThrownBy(
                () -> downloader(ScmSource.DIRECT)
                        .downloadSources("B1", List.of(UNREACHABLE_SCM), List.of("abc"), tmp.resolve("src.tar.gz")))
                .hasMessageContaining("Failed to download sources for build B1");
        assertThat(orchRequests).isEmpty();
    }

    @Test
    void directTriesEverySourceUrlAndReportsThemAll() throws Exception {
        orch(exchange -> send(exchange, 200, gzip("x"), "application/gzip"));

        assertThatThrownBy(
                () -> downloader(ScmSource.DIRECT).downloadSources(
                        "B1",
                        List.of(UNREACHABLE_SCM, "https://scm.unreachable.example.com/pnc-workspace/moved.git"),
                        List.of("abc"),
                        tmp.resolve("src.tar.gz")))
                .hasMessageContaining("from any of its SCM URLs")
                .hasMessageContaining(UNREACHABLE_SCM)
                .hasMessageContaining("moved.git");
        assertThat(orchRequests).isEmpty();
    }

    @Test
    void notAGzipIsRejected() throws Exception {
        orch(exchange -> send(exchange, 200, "<html>login</html>".getBytes(StandardCharsets.UTF_8), "text/html"));

        assertThatThrownBy(
                () -> downloader(ScmSource.ORCH)
                        .downloadSources("B1", List.of(UNREACHABLE_SCM), List.of("abc"), tmp.resolve("src.tar.gz")))
                .isInstanceOf(RuntimeException.class);
        assertThat(tmp.resolve("src.tar.gz")).doesNotExist();
    }

    @Test
    void aTruncatedArchiveIsRejected() throws Exception {
        byte[] archive = gzip("x".repeat(10_000));
        byte[] truncated = java.util.Arrays.copyOf(archive, archive.length - 8); // CRC and length trailer cut off
        orch(exchange -> send(exchange, 200, truncated, "application/gzip"));

        assertThatThrownBy(
                () -> downloader(ScmSource.ORCH)
                        .downloadSources("B1", List.of(UNREACHABLE_SCM), List.of("abc"), tmp.resolve("src.tar.gz")))
                .hasMessageContaining("incomplete or corrupted gzip archive");
        assertThat(tmp.resolve("src.tar.gz")).doesNotExist();
    }

    @Test
    void alignmentDiffFromPncIsMaskedAndSplit() throws Exception {
        String diff = "diff --git a/pom.xml b/pom.xml\n--- a/pom.xml\n+++ b/pom.xml\n@@ -1 +1 @@\n"
                + "-<version>1.0</version>\n+<version>1.0.0.redhat-00001</version>\n";
        orch(exchange -> {
            exchange.getResponseHeaders().add(OrchScmContent.DIFF_MODE_HEADER, "commit");
            exchange.getResponseHeaders().add(OrchScmContent.DIFF_HEAD_HEADER, "aligned");
            send(exchange, 200, diff.getBytes(StandardCharsets.UTF_8), "text/x-diff");
        });
        Build build = Build.builder()
                .id("B1")
                .scmTag("1.0.0.redhat-00001")
                .scmRepository(SCMRepository.builder().internalUrl(UNREACHABLE_SCM).build())
                .scmRevision("aligned")
                .scmBuildConfigRevision("upstream")
                .build();

        List<Path> parts = downloader(ScmSource.AUTO).downloadAlignmentDiff(build, tmp.resolve("alignment.diff"), true);

        assertThat(orchRequests).containsExactly("/pnc-rest/v2/builds/B1/alignment-diff");
        assertThat(parts).anySatisfy(part -> assertThat(part.getFileName().toString()).endsWith("-manifest.json"));
        String all = String.join("", parts.stream().map(ScmSourceFallbackTest::read).toList());
        assertThat(all).contains("+<version>1.0.0.redhat-00001</version>");
    }

    @Test
    void missingCredentialsGetAHint() throws Exception {
        orch(exchange -> send(exchange, 401, new byte[0], "application/json"));
        OrchScmContent orch = new OrchScmContent(
                OrchScmContent.restUrl("http://127.0.0.1:" + orchServer.getAddress().getPort()),
                Optional::empty,
                HttpClient.newHttpClient(),
                UrlRewrites.none());

        assertThatThrownBy(() -> orch.alignmentDiff("B1")).hasMessageContaining("HTTP status: 401")
                .hasMessageContaining("configure keycloak or ldapUsernamePassword");
    }

    @Test
    void aForbiddenCallerSeesTheExplanationsOfPncAndReqour() throws Exception {
        String reqour = "{\"errorType\":\"FORBIDDEN\",\"errorMessage\":\"missing role\",\"details\":null}";
        String pnc = "{\"errorType\":\"ForbiddenException\",\"errorMessage\":\"Reqour does not allow the caller to"
                + " read SCM content (HTTP 403).\",\"details\":" + new ObjectMapper().writeValueAsString(reqour) + "}";
        orch(exchange -> send(exchange, 403, pnc.getBytes(StandardCharsets.UTF_8), "application/json"));

        assertThatThrownBy(() -> content(sleeps()).alignmentDiff("B1"))
                .hasMessageContaining("HTTP status: 403")
                .hasMessageContaining("Reqour does not allow the caller to read SCM content (HTTP 403). (missing role)")
                .hasMessageContaining("needs a role that may read builds");
    }

    @Test
    void anEmptyNotFoundGetsAHint() throws Exception {
        orch(exchange -> send(exchange, 404, new byte[0], "application/json"));

        assertThatThrownBy(() -> content(sleeps()).alignmentDiff("B1"))
                .hasMessageContaining("no internal SCM repository or revision, or this PNC does not provide");
    }

    @Test
    void aShortRetryAfterIsRetriedOnce() throws Exception {
        byte[] archive = gzip("sources");
        orch(exchange -> {
            if (orchRequests.size() == 1) {
                exchange.getResponseHeaders().add("Retry-After", "30");
                send(
                        exchange,
                        503,
                        "{\"errorMessage\":\"Too many concurrent source and diff downloads.\"}"
                                .getBytes(StandardCharsets.UTF_8),
                        "application/json");
            } else {
                send(exchange, 200, archive, "application/gzip");
            }
        });
        List<Duration> sleeps = sleeps();

        Path target = content(sleeps).downloadArchive("B1", tmp.resolve("src.tar.gz"));

        assertThat(Files.readAllBytes(target)).isEqualTo(archive);
        assertThat(orchRequests).hasSize(2);
        assertThat(sleeps).containsExactly(Duration.ofSeconds(30));
    }

    @Test
    void aSecond503IsReported() throws Exception {
        orch(exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "5");
            send(exchange, 503, "{\"errorMessage\":\"busy\"}".getBytes(StandardCharsets.UTF_8), "application/json");
        });
        List<Duration> sleeps = sleeps();

        assertThatThrownBy(() -> content(sleeps).alignmentDiff("B1")).hasMessageContaining("HTTP status: 503")
                .hasMessageContaining("busy")
                .hasMessageContaining("Try again in 5 s");
        assertThat(orchRequests).hasSize(2);
        assertThat(sleeps).hasSize(1);
    }

    @Test
    void aLongRetryAfterIsReportedWithoutWaiting() throws Exception {
        orch(exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "600");
            send(
                    exchange,
                    503,
                    "{\"errorMessage\":\"The git provider rate-limited reqour (HTTP 403)\"}"
                            .getBytes(StandardCharsets.UTF_8),
                    "application/json");
        });
        List<Duration> sleeps = sleeps();

        assertThatThrownBy(() -> content(sleeps).alignmentDiff("B1"))
                .hasMessageContaining("The git provider rate-limited reqour (HTTP 403)")
                .hasMessageContaining("Try again in 600 s");
        assertThat(orchRequests).hasSize(1);
        assertThat(sleeps).isEmpty();
    }

    private static List<Duration> sleeps() {
        return new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    private OrchScmContent content(List<Duration> sleeps) {
        return new OrchScmContent(
                OrchScmContent.restUrl("http://127.0.0.1:" + orchServer.getAddress().getPort()),
                () -> Optional.of("Bearer token"),
                HttpClient.newHttpClient(),
                UrlRewrites.none(),
                sleeps::add);
    }

    @Test
    void binaryOnlyFilesAreListed() {
        assertThat(
                BuildSourcesDownloader.binaryOnlyFiles(
                        "diff --git a/x.bin b/x.bin\nBinary files a/x.bin and b/x.bin differ\n"))
                .containsExactly("a/x.bin and b/x.bin");
    }

    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private void orch(Handler handler) throws IOException {
        orchServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        orchServer.createContext("/", exchange -> {
            orchRequests.add(exchange.getRequestURI().getRawPath());
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            handler.handle(exchange);
        });
        orchServer.start();
    }

    private BuildSourcesDownloader downloader(ScmSource source) {
        return new BuildSourcesDownloader(
                () -> source,
                () -> new OrchScmContent(
                        OrchScmContent.restUrl("http://127.0.0.1:" + orchServer.getAddress().getPort()),
                        () -> Optional.of("Bearer token"),
                        HttpClient.newHttpClient(),
                        UrlRewrites.none()));
    }

    private static void send(HttpExchange exchange, int status, byte[] body, String contentType) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] gzip(String content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
