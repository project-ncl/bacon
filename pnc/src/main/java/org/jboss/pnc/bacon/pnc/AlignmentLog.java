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
package org.jboss.pnc.bacon.pnc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.bacon.common.http.SafeHttp;
import org.jboss.pnc.bacon.common.http.UrlRewrites;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.RemoteResourceException;

/**
 * Reads the alignment log of a build. PNC answers {@code /builds/<id>/logs/align} with a redirect to the log in
 * bifrost; with {@link UrlRewrites} the request and that redirect are made by bacon, so both can be rewritten.
 */
final class AlignmentLog {

    private AlignmentLog() {
    }

    /** Passes each line of the alignment log to {@code onLine}; nothing when the build has no alignment log. */
    static void forEachLine(String buildId, Consumer<String> onLine)
            throws IOException, InterruptedException, RemoteResourceException {
        UrlRewrites rewrites = UrlRewrites.current();
        if (rewrites.isEmpty()) {
            try (BuildClient buildClient = new BuildClient(
                    PncClientHelper.getPncConfiguration(PncClientHelper.credentialsAvailable()))) {
                Optional<InputStream> log = buildClient.getAlignLogs(buildId);
                if (log.isPresent()) {
                    readLines(log.get(), onLine);
                }
            }
            return;
        }

        Map<String, String> credentials = PncClientHelper.authorizationHeader()
                .map(header -> Map.of("Authorization", header))
                .orElse(Map.of());
        HttpResponse<InputStream> response = SafeHttp
                .get(SafeHttp.newClient(), alignLogUri(buildId), Map.of(), credentials, rewrites);
        try (InputStream body = response.body()) {
            if (response.statusCode() == 404) {
                return;
            }
            if (response.statusCode() != 200) {
                throw new IOException(
                        "Failed to read the alignment log of build " + buildId + " from " + response.uri()
                                + ": HTTP status " + response.statusCode());
            }
            readLines(body, onLine);
        }
    }

    /** The PNC REST URL of the alignment log, on the host of the profile's PNC URL like the PNC REST client. */
    static URI alignLogUri(String buildId) {
        URI pnc = URI.create(Config.instance().getActiveProfile().getPnc().getUrl());
        try {
            return new URI(
                    pnc.getScheme(),
                    null,
                    pnc.getHost(),
                    pnc.getPort(),
                    "/pnc-rest/v2/builds/" + buildId + "/logs/align",
                    null,
                    null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid build id '" + buildId + "'", e);
        }
    }

    private static void readLines(InputStream in, Consumer<String> onLine) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            reader.lines().forEach(onLine);
        }
    }
}
