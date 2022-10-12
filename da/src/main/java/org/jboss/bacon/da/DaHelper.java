/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.bacon.da;

import org.jboss.bacon.da.rest.endpoint.ListingsApi;
import org.jboss.bacon.da.rest.endpoint.LookupApi;
import org.jboss.bacon.da.rest.endpoint.ReportsApi;
import org.jboss.da.listings.model.rest.RestArtifact;
import org.jboss.da.lookup.model.MavenLatestResult;
import org.jboss.da.lookup.model.MavenLookupResult;
import org.jboss.da.lookup.model.NPMLookupResult;
import org.jboss.da.model.rest.GAV;
import org.jboss.da.model.rest.NPMPackage;
import org.jboss.pnc.bacon.common.Utils;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.DaConfig;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.Configuration;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper methods for DA stuff
 */
public class DaHelper {
    private final static String DA_PATH = "/da/rest/v-1";

    private static ResteasyClientBuilder builder;
    private static String daUrl;

    private static ResteasyWebTarget getClient() {

        if (builder == null) {
            builder = new ResteasyClientBuilder();
            ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
            builder.providerFactory(factory);
            ResteasyProviderFactory.setRegisterBuiltinByDefault(true);
            RegisterBuiltin.register(factory);

            DaConfig daConfig = Config.instance().getActiveProfile().getDa();
            daUrl = Utils.generateUrlPath(daConfig.getUrl(), DA_PATH);
        }

        return builder.build().target(daUrl);
    }

    private static ResteasyWebTarget getAuthenticatedClient() {

        ResteasyWebTarget target = getClient();
        Configuration pncConfiguration = PncClientHelper.getPncConfiguration();
        target.register(new TokenAuthenticator(pncConfiguration.getBearerToken()));
        return target;
    }

    public static ReportsApi createReportsApi() {
        return getClient().proxy(ReportsApi.class);
    }

    public static ListingsApi createListingsApi() {
        return getClient().proxy(ListingsApi.class);
    }

    public static ListingsApi createAuthenticatedListingsApi() {
        return getAuthenticatedClient().proxy(ListingsApi.class);
    }

    public static LookupApi createLookupApi() {
        return getClient().proxy(LookupApi.class);
    }

    /**
     * Get the appropriate mode to query DA for an artifact
     *
     * @param temporary whether the artifact is a temporary one
     * @param managedService whether the artifact is targetting a managed service
     * @param mode explicitly specify the mode to use
     *
     * @return appropriate mode
     */
    public static String getMode(boolean temporary, boolean managedService, String mode) {
        if (mode == null) {
            if (managedService) {
                if (temporary) {
                    return "SERVICE_TEMPORARY";
                } else {
                    return "SERVICE";
                }
            } else {
                if (temporary) {
                    return "TEMPORARY";
                } else {
                    return "PERSISTENT";
                }
            }
        } else {
            if (temporary || managedService) {
                throw new IllegalArgumentException(
                        "Don't specify temporary or managed service when specifying mode explicitly.");
            }
            return mode;
        }
    }

    /**
     * Transforms a string in format group:artifact:version to a GAV If the string is not properly formatted, a
     * RuntimeException is thrown
     *
     * @param gav String to transform
     *
     * @return GAV object
     */
    public static GAV toGAV(String gav) {
        String[] pieces = gav.split(":");

        if (pieces.length != 3) {
            throw new RuntimeException("GAV " + gav + " cannot be parsed into groupid:artifactid:version");
        }

        return new GAV(pieces[0], pieces[1], pieces[2]);
    }

    /**
     * Transforms a string in format group:artifact:version to a RestArtifact If the string is not properly formatted, a
     * RuntimeException is thrown
     *
     * @param gav String to transform
     *
     * @return RestArtifact object
     */
    public static RestArtifact toRestArtifact(String gav) {
        String[] pieces = gav.split(":");

        if (pieces.length != 3) {
            throw new RuntimeException("GAV " + gav + " cannot be parsed into groupid:artifactid:version");
        }
        RestArtifact artifact = new RestArtifact();
        artifact.setGroupId(pieces[0]);
        artifact.setArtifactId(pieces[1]);
        artifact.setVersion(pieces[2]);

        return artifact;
    }

    /**
     * Transforms a string in format package:version to an NPMPackage If the string is not properly formatted, a
     * RuntimeException is thrown
     *
     * @param nameVersion String to transform
     *
     * @return NPMPackage object
     */
    public static NPMPackage toNPMPackage(String nameVersion) {
        String[] pieces = nameVersion.split(":");

        if (pieces.length != 2) {
            throw new RuntimeException("NPM " + nameVersion + " cannot be parsed into name:version");
        }

        return new NPMPackage(pieces[0], pieces[1]);
    }

    /**
     * Based on the order in the gavSet, order the 'result' the same way based on the GAV, and return as a list
     *
     * @param gavSet order of gav
     * @param result set of result to order
     *
     * @return list of the ordered result
     */
    public static List<MavenLatestResult> orderedMavenLatestResult(
            Iterable<GAV> gavSet,
            Set<MavenLatestResult> result) {

        Map<GAV, MavenLatestResult> gavToMavenLatestResult = result.stream()
                .collect(Collectors.toMap(MavenLatestResult::getGav, x -> x));

        List<MavenLatestResult> ordered = new ArrayList<>();

        for (GAV gav : gavSet) {
            ordered.add(gavToMavenLatestResult.get(gav));
        }

        return ordered;
    }

    /**
     * Based on the order in the gavSet, order the 'result' the same way based on the GAV, and return as a list
     *
     * @param gavSet order of gav
     * @param result set of result to order
     *
     * @return list of the ordered result
     */
    public static List<MavenLookupResult> orderedMavenLookupResult(
            Iterable<GAV> gavSet,
            Set<MavenLookupResult> result) {

        Map<GAV, MavenLookupResult> gavToMavenLookupResult = result.stream()
                .collect(Collectors.toMap(MavenLookupResult::getGav, x -> x));

        List<MavenLookupResult> ordered = new ArrayList<>();

        for (GAV gav : gavSet) {
            ordered.add(gavToMavenLookupResult.get(gav));
        }

        return ordered;
    }

    /**
     * Based on the order in the npmPackageSet, order the 'result' the same way based on the npm package, and return as
     * a list
     *
     * @param npmPackageSet order of npm package
     * @param result set of result to order
     *
     * @return list of the ordered result
     */
    public static List<NPMLookupResult> orderedNPMLookupResult(
            Iterable<NPMPackage> npmPackageSet,
            Set<NPMLookupResult> result) {

        Map<NPMPackage, NPMLookupResult> gavToNPMLookupResult = result.stream()
                .collect(Collectors.toMap(NPMLookupResult::getNpmPackage, x -> x));

        List<NPMLookupResult> ordered = new ArrayList<>();

        for (NPMPackage npmPackage : npmPackageSet) {
            ordered.add(gavToNPMLookupResult.get(npmPackage));
        }

        return ordered;
    }

    private static class TokenAuthenticator implements ClientRequestFilter {

        private final String token;

        TokenAuthenticator(String token) {
            this.token = token;
        }

        public void filter(ClientRequestContext requestContext) throws IOException {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            headers.add("Authorization", "Bearer " + this.token);
        }
    }
}
