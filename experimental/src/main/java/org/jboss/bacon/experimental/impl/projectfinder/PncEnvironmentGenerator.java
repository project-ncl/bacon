package org.jboss.bacon.experimental.impl.projectfinder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Fetches PNC build environments and writes them to src/main/resources/pnc-environments.json.
 *
 * Usage: java -cp ... org.jboss.bacon.experimental.impl.projectfinder.PncEnvironmentGenerator [--all] [pnc-base-url]
 *
 * By default, only active (non-deprecated) environments are included.
 * Pass --all to include deprecated environments as well.
 */
public class PncEnvironmentGenerator {

    private static final String DEFAULT_BASE_URL = "https://orch.pnc.engineering.redhat.com";
    private static final String OUTPUT_PATH = "src/main/resources/pnc-environments.json";

    public static void main(String[] args) throws Exception {
        boolean includeAll = false;
        String baseUrl = DEFAULT_BASE_URL;

        for (String arg : args) {
            if ("--all".equals(arg)) {
                includeAll = true;
            } else {
                baseUrl = arg;
            }
        }

        String apiBase = baseUrl + "/pnc-rest/v2";

        System.out.println("Fetching " + (includeAll ? "all" : "active only") + " environments from " + apiBase);

        HttpClient httpClient = buildHttpClient();
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ArrayNode output = mapper.createArrayNode();

        int pageIndex = 0;
        int totalPages = 1;
        int totalHits = 0;

        while (pageIndex < totalPages) {
            String url = apiBase + "/environments?pageSize=200&pageIndex=" + pageIndex;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("HTTP " + response.statusCode() + " from " + url);
                System.exit(1);
            }

            JsonNode root = mapper.readTree(response.body());
            if (root.has("totalPages")) {
                totalPages = root.get("totalPages").asInt();
            }

            JsonNode content = root.has("content") ? root.get("content") : root;
            if (content.isArray()) {
                for (JsonNode env : content) {
                    boolean deprecated = env.path("deprecated").asBoolean(false);
                    if (!includeAll && deprecated) {
                        continue;
                    }

                    ObjectNode entry = mapper.createObjectNode();
                    entry.put("id", env.path("id").asText());
                    entry.put("name", env.path("name").asText());
                    entry.put("hidden", env.path("hidden").asBoolean(false));
                    entry.put("deprecated", deprecated);

                    ObjectNode attrs = mapper.createObjectNode();
                    if (env.has("attributes") && env.get("attributes").isObject()) {
                        env.get("attributes")
                                .fields()
                                .forEachRemaining(e -> attrs.put(e.getKey(), e.getValue().asText()));
                    }
                    entry.set("attributes", attrs);

                    output.add(entry);
                    totalHits++;
                }
            }

            pageIndex++;
        }

        Path outputFile = Path.of(OUTPUT_PATH);
        mapper.writeValue(outputFile.toFile(), output);
        System.out.println("Wrote " + totalHits + " environments to " + outputFile.toAbsolutePath());
    }

    private static HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL);
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String type) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String type) {
                }
            } }, new SecureRandom());
            builder.sslContext(sslContext);
        } catch (Exception e) {
            // fallback to default SSL
        }
        return builder.build();
    }
}
