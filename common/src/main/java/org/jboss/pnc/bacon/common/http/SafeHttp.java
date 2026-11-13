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
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP GETs that may carry credentials.
 * <p>
 * {@link HttpClient.Redirect#NORMAL} forwards explicit headers such as {@code Authorization} to whatever host a
 * redirect points to. {@link #get} follows redirects the same way (never from https to http) but sends the credential
 * headers only to the origin (scheme, host and port) of the first URL and of origins the caller trusts, and applies
 * the {@link UrlRewrites} to the first URL and to every redirect. Clients from {@link #newClient()} also bound the time
 * to establish a connection.
 */
public final class SafeHttp {

    /** Same default as the PNC REST client's connect timeout. */
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    public static final int MAX_REDIRECTS = 10;

    /**
     * A default for {@code responseTimeout}: how long a server may take to start answering (status and headers). The
     * body is not limited, so large downloads and long logs still work.
     */
    public static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(2);

    private SafeHttp() {
    }

    /** A client that does not follow redirects itself (see {@link #get}) and has {@link #CONNECT_TIMEOUT}. */
    public static HttpClient newClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /** {@link #get(HttpClient, URI, Map, Map, UrlRewrites, List)} without other credential origins. */
    public static HttpResponse<InputStream> get(
            HttpClient client,
            URI uri,
            Map<String, String> headers,
            Map<String, String> credentialHeaders,
            UrlRewrites rewrites) throws IOException, InterruptedException {
        return get(client, uri, headers, credentialHeaders, rewrites, List.of());
    }

    /**
     * Sends a GET to {@code uri}, following redirects like {@link HttpClient.Redirect#NORMAL}: a redirect without a
     * {@code Location}, or from https to http, is returned as the response. The https to http check is made on the
     * URLs before rewriting (as the server sent them): a rule may rewrite to plain http on purpose.
     *
     * @param headers headers sent with every request
     * @param credentialHeaders headers sent only to the origin of the (rewritten) {@code uri} and to
     *        {@code credentialOrigins}
     * @param rewrites applied to {@code uri}, to every redirect and to {@code credentialOrigins}
     * @param credentialOrigins other URLs whose origin is trusted with the credentials, e.g. a service the first URL
     *        redirects to and that bacon already sends them to
     * @return the first response that is not a followed redirect; the caller reads and closes its body
     * @throws IOException on I/O errors, including more than {@link #MAX_REDIRECTS} redirects
     */
    public static HttpResponse<InputStream> get(
            HttpClient client,
            URI uri,
            Map<String, String> headers,
            Map<String, String> credentialHeaders,
            UrlRewrites rewrites,
            List<URI> credentialOrigins) throws IOException, InterruptedException {
        return get(client, uri, headers, credentialHeaders, rewrites, credentialOrigins, null);
    }

    /**
     * {@link #get(HttpClient, URI, Map, Map, UrlRewrites, List)} where each request fails with
     * {@link java.net.http.HttpTimeoutException} when its response (status and headers) does not arrive within
     * {@code responseTimeout}; {@code null} for no limit. Reading the body is not limited.
     */
    public static HttpResponse<InputStream> get(
            HttpClient client,
            URI uri,
            Map<String, String> headers,
            Map<String, String> credentialHeaders,
            UrlRewrites rewrites,
            List<URI> credentialOrigins,
            Duration responseTimeout) throws IOException, InterruptedException {
        List<URI> trusted = new ArrayList<>();
        trusted.add(rewrites.apply(uri));
        credentialOrigins.forEach(origin -> trusted.add(rewrites.apply(origin)));

        URI requested = uri;
        URI current = trusted.get(0);
        for (int redirects = 0;; redirects++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(current).GET();
            if (responseTimeout != null) {
                request.timeout(responseTimeout);
            }
            headers.forEach(request::header);
            URI target = current;
            if (trusted.stream().anyMatch(origin -> Origins.same(origin, target))) {
                credentialHeaders.forEach(request::header);
            }

            HttpResponse<InputStream> response = client
                    .send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            boolean returned = false;
            try {
                Optional<String> location = response.headers().firstValue("Location");
                Optional<Redirect> next = isRedirect(response.statusCode()) && location.isPresent()
                        ? redirect(requested, current, location.get(), rewrites)
                        : Optional.empty();
                if (next.isEmpty()) {
                    returned = true;
                    return response;
                }
                if (redirects >= MAX_REDIRECTS) {
                    throw new IOException("Too many redirects from " + uri + " (maximum " + MAX_REDIRECTS + ")");
                }
                requested = next.get().requested;
                current = next.get().target;
            } finally {
                // the body of a response that is not returned is closed here, also on errors
                if (!returned) {
                    closeQuietly(response.body());
                }
            }
        }
    }

    /** A redirect: the URL as the server named it (before rewriting) and the URL to request. */
    public static final class Redirect {
        public final URI requested;
        public final URI target;

        Redirect(URI requested, URI target) {
            this.requested = requested;
            this.target = target;
        }
    }

    /**
     * Whether {@code failure} (or one of its causes) means the host could not be reached at all: unknown host,
     * connection refused or unreachable, or {@link #CONNECT_TIMEOUT} exceeded. Further requests to the same host fail
     * the same way, so callers stop trying it. A TLS handshake failure is not one: the host is reachable.
     */
    public static boolean isConnectFailure(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof HttpConnectTimeoutException || t instanceof ConnectException
                    || t instanceof UnknownHostException || t instanceof NoRouteToHostException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Where a redirect goes, or empty when it must not be followed (from https to http).
     * <ul>
     * <li>A {@code Location} with a host (absolute, or protocol-relative {@code //host/path}) is resolved against
     * {@code requested}, the URL as the server knows it, checked for an https to http downgrade there, and then
     * rewritten: a rule may rewrite to plain http on purpose.</li>
     * <li>A {@code Location} with a path only stays on the host actually requested ({@code current}, already
     * rewritten) and is not rewritten again.</li>
     * <li>The fragment is dropped: it is not sent in requests.</li>
     * </ul>
     *
     * @param requested the URL of the response before rewriting
     * @param current the URL of the response as requested (after rewriting)
     * @param location the {@code Location} header
     */
    public static Optional<Redirect> redirect(URI requested, URI current, String location, UrlRewrites rewrites) {
        URI target = URI.create(location.trim());
        if (target.getRawAuthority() != null) {
            URI sent = withoutFragment(requested.resolve(target));
            if ("https".equalsIgnoreCase(requested.getScheme()) && "http".equalsIgnoreCase(sent.getScheme())) {
                return Optional.empty();
            }
            return Optional.of(new Redirect(sent, rewrites.apply(sent)));
        }
        return Optional.of(
                new Redirect(withoutFragment(requested.resolve(target)), withoutFragment(current.resolve(target))));
    }

    private static URI withoutFragment(URI uri) {
        String value = uri.toString();
        int hash = value.indexOf('#');
        return hash < 0 ? uri : URI.create(value.substring(0, hash));
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException e) {
            // nothing to do: the response is discarded
        }
    }

    /** Whether both URIs have the same scheme, host and (default or explicit) port; see {@link Origins#same}. */
    public static boolean sameOrigin(URI a, URI b) {
        return Origins.same(a, b);
    }

    static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }
}
