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
package org.jboss.pnc.bacon.common.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP GETs that may carry credentials.
 * <p>
 * {@link HttpClient.Redirect#NORMAL} forwards explicit headers such as {@code Authorization} to whatever host a
 * redirect points to. {@link #get} follows redirects the same way (never from https to http) but sends the credential
 * headers only to the origin (scheme, host and port) of the first URL, and applies the {@link UrlRewrites} to the
 * first URL and to every redirect. Clients from {@link #newClient()} also bound the time to establish a connection.
 */
public final class SafeHttp {

    /** Same default as the PNC REST client's connect timeout. */
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    static final int MAX_REDIRECTS = 10;

    private SafeHttp() {
    }

    /** A client that does not follow redirects itself (see {@link #get}) and has {@link #CONNECT_TIMEOUT}. */
    public static HttpClient newClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /** {@link #get(HttpClient, URI, Map, Map, UrlRewrites)} with {@link UrlRewrites#current()}. */
    public static HttpResponse<InputStream> get(
            HttpClient client,
            URI uri,
            Map<String, String> headers,
            Map<String, String> credentialHeaders) throws IOException, InterruptedException {
        return get(client, uri, headers, credentialHeaders, UrlRewrites.current());
    }

    /**
     * Sends a GET to {@code uri}, following redirects like {@link HttpClient.Redirect#NORMAL}: a redirect without a
     * {@code Location}, or from https to http, is returned as the response.
     *
     * @param headers headers sent with every request
     * @param credentialHeaders headers sent only to the origin of the (rewritten) {@code uri}
     * @param rewrites applied to {@code uri} and to every redirect
     * @return the first response that is not a followed redirect; the caller reads and closes its body
     * @throws IOException on I/O errors, including more than {@link #MAX_REDIRECTS} redirects
     */
    public static HttpResponse<InputStream> get(
            HttpClient client,
            URI uri,
            Map<String, String> headers,
            Map<String, String> credentialHeaders,
            UrlRewrites rewrites) throws IOException, InterruptedException {
        URI first = rewrites.apply(uri);
        URI current = first;
        for (int redirects = 0;; redirects++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(current).GET();
            headers.forEach(request::header);
            if (sameOrigin(first, current)) {
                credentialHeaders.forEach(request::header);
            }

            HttpResponse<InputStream> response = client
                    .send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (!isRedirect(response.statusCode())) {
                return response;
            }
            Optional<String> location = response.headers().firstValue("Location");
            if (location.isEmpty()) {
                return response;
            }
            URI next = rewrites.apply(current.resolve(location.get()));
            if ("https".equalsIgnoreCase(current.getScheme()) && "http".equalsIgnoreCase(next.getScheme())) {
                return response;
            }
            response.body().close();
            if (redirects >= MAX_REDIRECTS) {
                throw new IOException("Too many redirects from " + uri + " (maximum " + MAX_REDIRECTS + ")");
            }
            current = next;
        }
    }

    /** Whether both URIs have the same scheme, host and (default or explicit) port. */
    public static boolean sameOrigin(URI a, URI b) {
        return a.getScheme() != null && a.getScheme().equalsIgnoreCase(b.getScheme()) && a.getHost() != null
                && a.getHost().equalsIgnoreCase(b.getHost()) && port(a) == port(b);
    }

    static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static int port(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : "http".equalsIgnoreCase(uri.getScheme()) ? 80 : -1;
    }
}
