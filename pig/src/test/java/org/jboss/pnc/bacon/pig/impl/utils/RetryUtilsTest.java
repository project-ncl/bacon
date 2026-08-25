package org.jboss.pnc.bacon.pig.impl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RetryUtilsTest {

    @BeforeEach
    void setup() {
        RetryUtils.configure(RetryUtils.DEFAULT_MAX_ATTEMPTS, 1);
    }

    @Test
    void succeedsFirstAttempt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String result = RetryUtils.withRetry(() -> {
            calls.incrementAndGet();
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(1, calls.get());
    }

    @Test
    void retriesOnNoResponseThenSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String result = RetryUtils.withRetry(() -> {
            if (calls.incrementAndGet() <= 2) {
                throw new RemoteResourceException(new RuntimeException("connection reset"));
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    void retriesOn503ThenSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String result = RetryUtils.withRetry(() -> {
            if (calls.incrementAndGet() <= 1) {
                throw new RemoteResourceException("service unavailable", 503);
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void retriesOn408RequestTimeout() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String result = RetryUtils.withRetry(() -> {
            if (calls.incrementAndGet() <= 1) {
                throw new RemoteResourceException("request timeout", 408);
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void retriesOn429TooManyRequests() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String result = RetryUtils.withRetry(() -> {
            if (calls.incrementAndGet() <= 1) {
                throw new RemoteResourceException("too many requests", 429);
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void failsImmediatelyOn4xx() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(RemoteResourceException.class, () -> RetryUtils.withRetry(() -> {
            calls.incrementAndGet();
            throw new RemoteResourceException("bad request", 400);
        }, "test"));
        assertEquals(1, calls.get());
    }

    @Test
    void failsImmediatelyOnPlainClientException() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(ClientException.class, () -> RetryUtils.withRetry(() -> {
            calls.incrementAndGet();
            throw new ClientException("not a remote resource exception");
        }, "test"));
        assertEquals(1, calls.get());
    }

    @Test
    void throwsAfterMaxAttemptsExhausted() {
        RetryUtils.configure(2, 1);
        AtomicInteger calls = new AtomicInteger();
        assertThrows(RemoteResourceException.class, () -> RetryUtils.withRetry(() -> {
            calls.incrementAndGet();
            throw new RemoteResourceException("always fails", 500);
        }, "test"));
        assertEquals(3, calls.get());
    }

    @Test
    void zeroMaxAttemptsDisablesRetrying() {
        RetryUtils.configure(0, 1);
        AtomicInteger calls = new AtomicInteger();
        assertThrows(RemoteResourceException.class, () -> RetryUtils.withRetry(() -> {
            calls.incrementAndGet();
            throw new RemoteResourceException("always fails", 500);
        }, "test"));
        assertEquals(1, calls.get());
    }
}
