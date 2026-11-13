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

/** The origin (scheme, host and port) of URLs, which credentials are tied to. */
public final class Origins {

    private Origins() {
    }

    /** Whether both URIs have the same scheme, host and (default or explicit) port. */
    public static boolean same(URI a, URI b) {
        return a.getScheme() != null && a.getScheme().equalsIgnoreCase(b.getScheme()) && a.getHost() != null
                && a.getHost().equalsIgnoreCase(b.getHost())
                && port(a.getScheme(), a.getPort()) == port(b.getScheme(), b.getPort());
    }

    /** The explicit port, else the default port of http and https, else -1. */
    public static int port(String scheme, int port) {
        if (port != -1) {
            return port;
        }
        return "https".equalsIgnoreCase(scheme) ? 443 : "http".equalsIgnoreCase(scheme) ? 80 : -1;
    }
}
