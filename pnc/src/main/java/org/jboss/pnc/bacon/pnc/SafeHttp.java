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

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP GETs to SCM hosts and repository managers that carry credentials.
 * <p>
 * {@link HttpClient.Redirect#NORMAL} forwards explicit headers such as {@code Authorization} or {@code PRIVATE-TOKEN}
 * to whatever host a redirect points to. {@link #get} follows redirects the same way (never from https to http) but
 * sends the credential headers only to the origin (scheme, host and port) of the first URL. Clients from
 * {@link #newClient()} also bound the time to establish a connection, so an unreachable host fails instead of blocking
 * until the operating system gives up.
 */
final class SafeHttp {

    /** Same default as the PNC REST client's connect timeout. */
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    static final int MAX_REDIRECTS = 10;

    private SafeHttp() {
    }

    /** A client that does not follow redirects itself (see {@link #get}) and has {@link #CONNECT_TIMEOUT}. */
    static HttpClient newClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Sends a GET to {@code uri}, following redirects like {@link HttpClient.Redirect#NORMAL}: a redirect without a
     * {@code Location}, or from https to http, is returned as the response.
     *
     * @param headers headers sent with every request
     * @param credentialHeaders headers sent only to the origin of {@code uri}
     * @return the first response that is not a followed redirect; the caller reads and closes its body
     * @throws IOException on I/O errors, including more than {@link #MAX_REDIRECTS} redirects
     */
    static HttpResponse<InputStream> get(
            HttpClient client,
            URI uri,
            Map<String, String> headers,
            Map<String, String> credentialHeaders) throws IOException, InterruptedException {
        URI current = uri;
        for (int redirects = 0;; redirects++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(current).GET();
            headers.forEach(request::header);
            if (sameOrigin(uri, current)) {
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
            URI next = current.resolve(location.get());
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

    /**
     * Whether {@code failure} (or one of its causes) means the host could not be reached at all: unknown host,
     * connection refused or unreachable, or {@link #CONNECT_TIMEOUT} exceeded. Further requests to the same host fail
     * the same way, so callers stop trying it.
     */
    static boolean isConnectFailure(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof HttpConnectTimeoutException || t instanceof ConnectException
                    || t instanceof UnknownHostException || t instanceof NoRouteToHostException) {
                return true;
            }
        }
        return false;
    }

    /** Whether both URIs have the same scheme, host and (default or explicit) port. */
    static boolean sameOrigin(URI a, URI b) {
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
