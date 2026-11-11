package org.jboss.pnc.bacon.pnc.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BifrostClient {

    public enum LogType {
        COMPLETE, BUILD, ALIGNMENT
    }

    private static final Logger log = LoggerFactory.getLogger(BifrostClient.class);

    private final URI baseUrl;
    private final HttpClient client;

    /** Same default as the PNC REST client's connect timeout. No socket timeout: logs can be followed live. */
    static final int CONNECT_TIMEOUT_MILLIS = 30_000;

    public BifrostClient(URI baseUrl) {
        this.baseUrl = baseUrl;
        client = HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
                                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MILLIS)
                                .build())
                .addInterceptorLast(dropAuthorizationOutside(baseUrl))
                .build();
    }

    /**
     * Removes the {@code Authorization} header from requests to another origin than {@code baseUrl}: on a redirect the
     * client copies the original request's headers to the new target.
     */
    static HttpRequestInterceptor dropAuthorizationOutside(URI baseUrl) {
        return (request, context) -> {
            HttpHost target = HttpClientContext.adapt(context).getTargetHost();
            if (target != null && !sameOrigin(baseUrl, target)) {
                request.removeHeaders("Authorization");
            }
        };
    }

    static boolean sameOrigin(URI baseUrl, HttpHost target) {
        return baseUrl.getScheme() != null && baseUrl.getScheme().equalsIgnoreCase(target.getSchemeName())
                && baseUrl.getHost() != null && baseUrl.getHost().equalsIgnoreCase(target.getHostName())
                && port(baseUrl.getScheme(), baseUrl.getPort()) == port(target.getSchemeName(), target.getPort());
    }

    private static int port(String scheme, int port) {
        if (port != -1) {
            return port;
        }
        return "https".equalsIgnoreCase(scheme) ? 443 : "http".equalsIgnoreCase(scheme) ? 80 : -1;
    }

    public List<String> getLog(String buildId, LogType logType) throws IOException {
        List<String> logs = new ArrayList<>();
        writeLog(buildId, false, logs::add, logType);
        return logs;
    }

    public void writeLog(String id, boolean follow, Consumer<String> onLine, LogType logType) throws IOException {

        String query;

        switch (logType) {
            case BUILD:
                query = MessageFormat.format(
                        "direction=ASC&matchFilters=mdc.processContext:build-{0},loggerName:org.jboss.pnc._userlog_.build-log&batchSize=5000&batchDelay=500&format=LEVEL",
                        id);
                break;
            case ALIGNMENT:
                query = MessageFormat.format(
                        "direction=ASC&matchFilters=mdc.processContext:build-{0},loggerName:org.jboss.pnc._userlog_.alignment-log&batchSize=5000&batchDelay=500&format=LEVEL",
                        id);
                break;
            default:
                query = MessageFormat.format(
                        "direction=ASC&matchFilters=mdc.processContext:build-{0}&prefixFilters=loggerName:org.jboss.pnc._userlog_&batchSize=500&batchDelay=500&format=TIMESTAMP",
                        id);
        }

        if (follow) {
            query += "&follow=true";
        } else {
            query += "&follow=false";
        }

        URI logsUrl = baseUrl.resolve(URI.create("/text?" + query));
        log.debug("Reading logs from {}", logsUrl);

        HttpUriRequest httpGet = new HttpGet(logsUrl);
        addAuthorization(httpGet);
        HttpResponse response = client.execute(httpGet);

        try (InputStream is = response.getEntity().getContent();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br.lines().forEach(onLine);
        }
    }

    /**
     * Writes a final (stored) log of a build, e.g. {@code alignment-log}, from bifrost's
     * {@code /final-log/<build id>/<log name>} endpoint: the one PNC redirects {@code /builds/<id>/logs/align} to.
     */
    public void writeFinalLog(String buildId, String logName, Consumer<String> onLine) throws IOException {
        URI logUrl = finalLogUri(baseUrl, buildId, logName);
        log.debug("Reading final log from {}", logUrl);

        HttpUriRequest httpGet = new HttpGet(logUrl);
        addAuthorization(httpGet);
        HttpResponse response = client.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            EntityUtils.consumeQuietly(response.getEntity());
            throw new IOException("Failed to read " + logUrl + ": HTTP status " + status);
        }

        try (InputStream is = response.getEntity().getContent();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br.lines().forEach(onLine);
        }
    }

    /** {@code <bifrost>/final-log/<build id>/<log name>}, resolved like the other bifrost endpoints. */
    static URI finalLogUri(URI baseUrl, String buildId, String logName) {
        return baseUrl.resolve(
                URI.create(
                        "/final-log/" + URLEncoder.encode(buildId, StandardCharsets.UTF_8) + "/"
                                + URLEncoder.encode(logName, StandardCharsets.UTF_8)));
    }

    private void addAuthorization(HttpUriRequest request) {
        PncClientHelper.authorizationHeader().ifPresent(header -> request.setHeader("Authorization", header));
    }
}
