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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.jboss.bacon.da.rest.endpoint.ListingsApi;
import org.jboss.bacon.da.rest.endpoint.LookupApi;
import org.jboss.bacon.da.rest.endpoint.ReportsApi;
import org.jboss.da.listings.model.rest.RestArtifact;
import org.jboss.da.lookup.model.MavenResult;
import org.jboss.da.lookup.model.NPMResult;
import org.jboss.da.model.rest.GAV;
import org.jboss.da.model.rest.NPMPackage;
import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.bacon.common.CustomRestHeaderFilter;
import org.jboss.pnc.bacon.common.TokenAuthenticator;
import org.jboss.pnc.bacon.common.Utils;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.DaConfig;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.otel.OTelCLIHelper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper methods for DA stuff
 */
@Slf4j
public class DaHelper {

    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MILLIS = 200;
    private static final long MAX_BACKOFF_MILLIS = 5_000;
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(408, 429, 500, 502, 503, 504);
    private static final long CONNECT_TIMEOUT_SECONDS = 30;
    private static final long READ_TIMEOUT_SECONDS = 60;
    private final static String DA_PATH = "/rest/v-1";

    private static ResteasyClientBuilder builder;
    private static String daUrl;

    private static ResteasyWebTarget getClient() {

        if (builder == null) {
            builder = new ResteasyClientBuilder();
            builder.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            builder.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
            builder.providerFactory(factory);
            ResteasyProviderFactory.setRegisterBuiltinByDefault(true);
            RegisterBuiltin.register(factory);

            DaConfig daConfig = Config.instance().getActiveProfile().getDa();
            daUrl = Utils.generateUrlPath(daConfig.getUrl(), DA_PATH);
        }
        ResteasyClient resteasyClient = builder.build();
        if (OTelCLIHelper.otelEnabled()) {
            resteasyClient.register(new CustomRestHeaderFilter(Span.current().getSpanContext()));
        }
        return resteasyClient.target(daUrl);
    }

    private static ResteasyWebTarget getAuthenticatedClient() {

        ResteasyWebTarget target = getClient();
        Configuration pncConfiguration = PncClientHelper.getPncConfiguration();
        target.register(new TokenAuthenticator(pncConfiguration.getBearerTokenSupplier()));
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
    public static <T extends MavenResult> List<T> orderMavenResult(Iterable<GAV> gavSet, Set<T> result) {
        Map<GAV, T> gavToResult = result.stream().collect(Collectors.toMap(MavenResult::getGav, x -> x));

        List<T> ordered = new ArrayList<>();

        for (GAV gav : gavSet) {
            ordered.add(gavToResult.get(gav));
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
    public static <T extends NPMResult> List<T> orderNPMResult(
            Iterable<NPMPackage> npmPackageSet,
            Set<T> result) {
        Map<NPMPackage, T> gavToNPMResult = result.stream().collect(Collectors.toMap(NPMResult::getNpmPackage, x -> x));

        List<T> ordered = new ArrayList<>();

        for (NPMPackage npmPackage : npmPackageSet) {
            ordered.add(gavToNPMResult.get(npmPackage));
        }
        return ordered;
    }

    public static <T> T executeWithRetry(Supplier<T> supplier, String operationDescription) {
        return executeWithRetry(supplier, operationDescription, DEFAULT_MAX_RETRIES);
    }

    public static <T> T executeWithRetry(Supplier<T> supplier, String operationDescription, int maxRetries) {
        int retries = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (ProcessingException e) {
                if (hasCause(e, SSLHandshakeException.class)) {
                    throw new FatalException(
                            "Cannot reach the DA server because of missing TLS certificates",
                            e.getCause());
                }
                retries++;
                if (retries > maxRetries) {
                    throw new FatalException(
                            "DA call failed after " + maxRetries + " retries: " + operationDescription,
                            e);
                }
                logRetryAttempt(retries, maxRetries, operationDescription, e);
                sleepWithBackoff(retries);
            } catch (WebApplicationException e) {
                int statusCode = e.getResponse().getStatus();
                e.getResponse().close();
                if (RETRYABLE_STATUS_CODES.contains(statusCode)) {
                    retries++;
                    if (retries > maxRetries) {
                        throw new FatalException(
                                "DA call failed after " + maxRetries + " retries: " + operationDescription,
                                e);
                    }
                    logRetryAttempt(retries, maxRetries, operationDescription, e);
                    sleepWithBackoff(retries);
                } else {
                    throw e;
                }
            }
        }
    }

    private static boolean hasCause(Throwable e, Class<? extends Throwable> causeType) {
        Throwable current = e;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void logRetryAttempt(int retries, int maxRetries, String operationDescription, Exception e) {
        if (retries == maxRetries / 2) {
            log.warn("Having difficulty reaching DA server for {}. Retrying...", operationDescription);
        }
        log.debug("DA Retry {}/{} for {}: {}", retries, maxRetries, operationDescription, e.getMessage());
    }

    private static void sleepWithBackoff(int retries) {
        long exponentialDelay = calculateExponentialBackoffMillis(retries);
        long amountOfSleep = applyJitter(exponentialDelay);
        log.debug("Sleeping for {} seconds", String.format("%.1f", amountOfSleep / 1000.0));
        try {
            Thread.sleep(amountOfSleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static long calculateExponentialBackoffMillis(int retries) {
        long delay = INITIAL_BACKOFF_MILLIS;
        for (int retry = 1; retry < retries; retry++) {
            if (delay >= MAX_BACKOFF_MILLIS / 2) {
                return MAX_BACKOFF_MILLIS;
            }
            delay *= 2;
        }
        return Math.min(delay, MAX_BACKOFF_MILLIS);
    }

    private static long applyJitter(long delayMillis) {
        long lowerBound = delayMillis / 2;
        long upperBoundExclusive = delayMillis + 1;
        return ThreadLocalRandom.current().nextLong(lowerBound, upperBoundExclusive);
    }
}
