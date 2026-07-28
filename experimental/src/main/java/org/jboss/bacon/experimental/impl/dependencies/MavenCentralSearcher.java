package org.jboss.bacon.experimental.impl.dependencies;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.pnc.bacon.common.exception.FatalException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MavenCentralSearcher {

    private static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    private static final int TIMEOUT = 120_000;
    private static final int MAX_ROWS = 200;

    private final CloseableHttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(
                    RequestConfig.custom()
                            .setConnectTimeout(TIMEOUT)
                            .setSocketTimeout(TIMEOUT)
                            .build())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Set<String> findArtifacts(String groupId, String version) {
        String query = "g:" + groupId + " AND v:" + version;
        String url = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&rows=" + MAX_ROWS + "&wt=json";

        log.info("Searching Maven Central for artifacts matching {}:*:{}", groupId, version);
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 300) {
                throw new FatalException(
                        "Maven Central search failed with HTTP " + statusCode + ": "
                                + response.getStatusLine().getReasonPhrase());
            }
            HttpEntity entity = response.getEntity();
            String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return parseResponse(body);
        } catch (IOException e) {
            throw new FatalException("Failed to query Maven Central search API.", e);
        }
    }

    Set<String> parseResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode docs = root.path("response").path("docs");
        if (!docs.isArray()) {
            throw new FatalException("Unexpected Maven Central search response format.");
        }
        Set<String> artifactIds = new LinkedHashSet<>();
        for (JsonNode doc : docs) {
            String artifactId = doc.path("a").asText(null);
            if (artifactId != null) {
                artifactIds.add(artifactId);
            }
        }
        return artifactIds;
    }
}
