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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

/**
 * {@link SafeHttp} for Apache HttpClient: clients from {@link #builder} follow redirects through
 * {@link SafeHttp#redirect} (URL rewrites, never from https to http), send {@code Authorization} only to the trusted
 * origins, and give up connecting after {@link SafeHttp#CONNECT_TIMEOUT}. Requests are made with
 * {@link #context(URI)} so that redirects are resolved against the URL as the server knows it.
 */
public final class SafeApacheHttp {

    private static final String REQUESTED = SafeApacheHttp.class.getName() + ".requested";

    private SafeApacheHttp() {
    }

    /**
     * @param rewrites the rules applied to redirects (the caller rewrites the first URL)
     * @param credentialOrigins the URLs whose origin may receive {@code Authorization}, before rewriting
     */
    public static HttpClientBuilder builder(Supplier<UrlRewrites> rewrites, Supplier<List<URI>> credentialOrigins) {
        int timeout = (int) SafeHttp.CONNECT_TIMEOUT.toMillis();
        return HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build())
                .setRedirectStrategy(new RewritingRedirectStrategy(rewrites))
                .addInterceptorLast(dropAuthorizationOutside(rewrites, credentialOrigins));
    }

    /** A context for a request to {@code original}, the URL before rewriting. */
    public static HttpClientContext context(URI original) {
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute(REQUESTED, original);
        return context;
    }

    static HttpRequestInterceptor dropAuthorizationOutside(
            Supplier<UrlRewrites> rewrites,
            Supplier<List<URI>> credentialOrigins) {
        return (request, context) -> {
            HttpHost target = HttpClientContext.adapt(context).getTargetHost();
            if (target == null) {
                return;
            }
            URI targetUri = URI.create(target.toURI());
            UrlRewrites rules = rewrites.get();
            boolean trusted = credentialOrigins.get()
                    .stream()
                    .anyMatch(origin -> Origins.same(rules.apply(origin), targetUri));
            if (!trusted) {
                request.removeHeaders("Authorization");
            }
        };
    }

    private static final class RewritingRedirectStrategy extends DefaultRedirectStrategy {
        private final Supplier<UrlRewrites> rewrites;

        RewritingRedirectStrategy(Supplier<UrlRewrites> rewrites) {
            this.rewrites = rewrites;
        }

        @Override
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
            return super.isRedirected(request, response, context) && next(request, response, context).isPresent();
        }

        @Override
        public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
            SafeHttp.Redirect redirect = next(request, response, context)
                    .orElseThrow(() -> new ProtocolException("Redirect from https to http refused"));
            context.setAttribute(REQUESTED, redirect.requested);
            return redirect.target;
        }

        private Optional<SafeHttp.Redirect> next(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
            Header location = response.getFirstHeader("Location");
            if (location == null) {
                return Optional.empty();
            }
            URI current = current(request, context);
            Object requested = context.getAttribute(REQUESTED);
            try {
                return SafeHttp.redirect(
                        requested instanceof URI ? (URI) requested : current,
                        current,
                        location.getValue(),
                        rewrites.get());
            } catch (IllegalArgumentException e) {
                throw new ProtocolException("Invalid redirect location: " + location.getValue(), e);
            }
        }

        private static URI current(HttpRequest request, HttpContext context) throws ProtocolException {
            try {
                URI uri = request instanceof HttpRequestBase ? ((HttpRequestBase) request).getURI()
                        : new URI(request.getRequestLine().getUri());
                if (uri.isAbsolute()) {
                    return uri;
                }
                HttpHost target = HttpClientContext.adapt(context).getTargetHost();
                return URI.create(target.toURI()).resolve(uri);
            } catch (URISyntaxException e) {
                throw new ProtocolException("Invalid request URI", e);
            }
        }
    }
}
