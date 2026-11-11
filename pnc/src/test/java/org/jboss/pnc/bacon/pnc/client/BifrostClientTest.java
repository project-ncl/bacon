package org.jboss.pnc.bacon.pnc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

class BifrostClientTest {

    @TempDir
    static Path configDir;

    private HttpServer server;
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeAll
    static void configureLdapProfile() throws IOException {
        Files.writeString(
                configDir.resolve("config.yaml"),
                "profile:\n" + "- name: \"default\"\n" + "  pnc:\n" + "    url: \"http://orch.example.com\"\n"
                        + "    bifrostBaseurl: \"http://bifrost.example.com\"\n"
                        + "  ldapUsernamePassword: \"user:secret\"\n");
        Config.configure(configDir.toString(), "config.yaml", "default");
        Config.initialize();
    }

    @AfterAll
    static void restoreTestConfig() throws IOException {
        File file = new File(BifrostClientTest.class.getClassLoader().getResource("config.yaml").getFile());
        Config.configure(file.getParent(), "config.yaml", "default");
        Config.initialize();
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            boolean found = exchange.getRequestURI().getRawPath().equals("/final-log/B123/alignment-log");
            byte[] body = (found ? "line 1\nline 2\n" : "not found").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(found ? 200 : 404, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private URI serverUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void shouldResolveFinalLogFromBifrostRoot() {
        assertEquals(
                URI.create("https://bifrost.example.com:8443/final-log/BSM5YUBO6KQAA/alignment-log"),
                BifrostClient.finalLogUri(
                        URI.create("https://bifrost.example.com:8443"),
                        "BSM5YUBO6KQAA",
                        "alignment-log"));
    }

    @Test
    void shouldEncodeFinalLogPathSegments() {
        assertEquals(
                URI.create("https://bifrost.example.com/final-log/a%2Fb/alignment-log"),
                BifrostClient.finalLogUri(URI.create("https://bifrost.example.com"), "a/b", "alignment-log"));
    }

    @Test
    void shouldReadFinalLogWithProfileCredentials() throws IOException {
        List<String> lines = new ArrayList<>();

        new BifrostClient(serverUri()).writeFinalLog("B123", "alignment-log", lines::add);

        assertEquals(List.of("line 1", "line 2"), lines);
        assertEquals("/final-log/B123/alignment-log", requestPath.get());
        assertEquals(
                "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8)),
                authorization.get());
    }

    @Test
    void shouldFailOnUnsuccessfulStatus() {
        IOException e = assertThrows(
                IOException.class,
                () -> new BifrostClient(serverUri()).writeFinalLog("missing", "alignment-log", line -> {}));

        assertTrue(e.getMessage().contains("HTTP status 404"), e.getMessage());
    }
}
