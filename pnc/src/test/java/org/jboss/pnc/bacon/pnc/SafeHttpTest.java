package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class SafeHttpTest {

    private static final Map<String, String> CREDENTIALS = Map
            .of("Authorization", "Basic secret", "PRIVATE-TOKEN", "token");

    private final List<HttpServer> servers = new CopyOnWriteArrayList<>();
    /** "port path Authorization PRIVATE-TOKEN" of every request received. */
    private final List<String> received = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(server -> server.stop(0));
    }

    private interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }

    /** Starts a server that records each request and answers it with {@code responder}; returns its /start URI. */
    private URI server(Responder responder) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            received.add(
                    exchange.getLocalAddress().getPort() + " " + exchange.getRequestURI().getPath() + " "
                            + exchange.getRequestHeaders().getFirst("Authorization") + " "
                            + exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN"));
            responder.respond(exchange);
            exchange.close();
        });
        server.start();
        servers.add(server);
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/start");
    }

    private static void ok(HttpExchange exchange) throws IOException {
        byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static Responder redirectTo(String location) {
        return exchange -> {
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
        };
    }

    private static String body(HttpResponse<InputStream> response) throws IOException {
        try (InputStream in = response.body()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void shouldSendCredentialsAcrossSameOriginRedirects() throws Exception {
        URI uri = server(exchange -> {
            if (exchange.getRequestURI().getPath().equals("/start")) {
                redirectTo("/final").respond(exchange);
            } else {
                ok(exchange);
            }
        });

        HttpResponse<InputStream> response = SafeHttp.get(SafeHttp.newClient(), uri, Map.of(), CREDENTIALS);

        assertEquals("ok", body(response));
        int port = uri.getPort();
        assertEquals(List.of(port + " /start Basic secret token", port + " /final Basic secret token"), received);
    }

    @Test
    void shouldNotForwardCredentialsToAnotherOrigin() throws Exception {
        URI target = server(SafeHttpTest::ok);
        URI redirecting = server(redirectTo(target.toString()));

        HttpResponse<InputStream> response = SafeHttp
                .get(SafeHttp.newClient(), redirecting, Map.of("User-Agent", "bacon"), CREDENTIALS);

        assertEquals("ok", body(response));
        assertEquals(
                List.of(redirecting.getPort() + " /start Basic secret token", target.getPort() + " /start null null"),
                received);
    }

    @Test
    void shouldReturnRedirectWithoutLocation() throws Exception {
        URI uri = server(exchange -> exchange.sendResponseHeaders(302, -1));

        HttpResponse<InputStream> response = SafeHttp.get(SafeHttp.newClient(), uri, Map.of(), Map.of());

        assertEquals(302, response.statusCode());
        body(response);
    }

    @Test
    void shouldFailAfterTooManyRedirects() throws Exception {
        URI uri = server(redirectTo("/start"));

        IOException e = assertThrows(
                IOException.class,
                () -> SafeHttp.get(SafeHttp.newClient(), uri, Map.of(), Map.of()));

        assertTrue(e.getMessage().contains("Too many redirects"), e.getMessage());
        assertEquals(SafeHttp.MAX_REDIRECTS + 1, received.size());
    }

    @Test
    void shouldCreateClientsWithConnectTimeoutThatDoNotFollowRedirects() {
        HttpClient client = SafeHttp.newClient();

        assertEquals(SafeHttp.CONNECT_TIMEOUT, client.connectTimeout().orElse(null));
        assertEquals(HttpClient.Redirect.NEVER, client.followRedirects());
    }

    @Test
    void shouldCompareOrigins() {
        assertTrue(sameOrigin("https://Repo.example.com/a", "https://repo.example.com:443/b"));
        assertTrue(sameOrigin("http://repo.example.com/a", "http://repo.example.com:80/b"));
        assertFalse(sameOrigin("https://repo.example.com/a", "http://repo.example.com/a"));
        assertFalse(sameOrigin("https://repo.example.com/a", "https://repo.example.com:8443/a"));
        assertFalse(sameOrigin("https://repo.example.com/a", "https://cdn.example.com/a"));
    }

    private static boolean sameOrigin(String a, String b) {
        return SafeHttp.sameOrigin(URI.create(a), URI.create(b));
    }

    @Test
    void shouldRecognizeConnectFailuresInTheCauseChain() {
        assertTrue(SafeHttp.isConnectFailure(new RuntimeException(new ConnectException("refused"))));
        assertTrue(
                SafeHttp.isConnectFailure(new RuntimeException("io", new IOException(new UnknownHostException("x")))));
        assertTrue(SafeHttp.isConnectFailure(new HttpConnectTimeoutException("timed out")));
        assertFalse(SafeHttp.isConnectFailure(new RuntimeException(new IOException("connection reset"))));
        assertFalse(SafeHttp.isConnectFailure(new RuntimeException("HTTP status: 401")));
    }
}
