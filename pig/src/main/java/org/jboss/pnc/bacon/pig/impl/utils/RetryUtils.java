/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2026 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pig.impl.utils;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries PNC REST calls that fail with a transient error (e.g. connection reset / no response, or a 5xx / 408 / 429
 * status). Attempt count and initial backoff are configurable from the CLI, see {@link #configure(int, long)}.
 */
public class RetryUtils {

    private static final Logger log = LoggerFactory.getLogger(RetryUtils.class);

    public static final int DEFAULT_MAX_ATTEMPTS = 5;
    public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 500;

    private static final long MAX_BACKOFF_MILLIS = 10_000;

    // status -1 is used by the PNC client when there was no HTTP response at all (e.g. connection reset, timeout)
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(-1, 408, 429, 500, 502, 503, 504);

    private static int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private static long initialBackoffMillis = DEFAULT_INITIAL_BACKOFF_MILLIS;

    private RetryUtils() {
    }

    /**
     * @param maxAttempts number of retries after the initial call fails, 0 disables retrying
     * @param initialBackoffMillis backoff before the first retry, doubled on each subsequent retry up to a cap, with
     *        jitter applied
     */
    public static void configure(int maxAttempts, long initialBackoffMillis) {
        RetryUtils.maxAttempts = maxAttempts;
        RetryUtils.initialBackoffMillis = initialBackoffMillis;
    }

    @FunctionalInterface
    public interface PncCall<T> {
        T call() throws ClientException;
    }

    public static <T> T withRetry(PncCall<T> call, String operationDescription) throws ClientException {
        int attempt = 0;
        while (true) {
            try {
                return call.call();
            } catch (ClientException e) {
                if (!isRetryable(e) || attempt >= maxAttempts) {
                    throw e;
                }
                attempt++;
                long backoff = backoffMillis(attempt);
                log.warn(
                        "PNC call failed (attempt {}/{}) for '{}': {}. Retrying in {} ms",
                        attempt,
                        maxAttempts,
                        operationDescription,
                        e.getMessage(),
                        backoff);
                sleep(backoff);
            }
        }
    }

    private static boolean isRetryable(ClientException e) {
        return e instanceof RemoteResourceException
                && RETRYABLE_STATUS_CODES.contains(((RemoteResourceException) e).getStatus());
    }

    private static long backoffMillis(int attempt) {
        long exponential = initialBackoffMillis;
        for (int i = 1; i < attempt; i++) {
            if (exponential >= MAX_BACKOFF_MILLIS / 2) {
                exponential = MAX_BACKOFF_MILLIS;
                break;
            }
            exponential *= 2;
        }
        exponential = Math.min(exponential, MAX_BACKOFF_MILLIS);
        long lowerBound = exponential / 2;
        return ThreadLocalRandom.current().nextLong(lowerBound, exponential + 1);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
