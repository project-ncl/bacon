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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.da.model.rest.GAV;
import org.jboss.da.model.rest.NPMPackage;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.junit.jupiter.api.Test;

class DaHelperTest {

    @Test
    void getModeTest() {

        assertEquals("SERVICE", DaHelper.getMode(false, true, null));
        assertEquals("SERVICE_TEMPORARY", DaHelper.getMode(true, true, null));
        assertEquals("PERSISTENT", DaHelper.getMode(false, false, null));
        assertEquals("TEMPORARY", DaHelper.getMode(true, false, null));
        assertEquals("FOO_BAR", DaHelper.getMode(false, false, "FOO_BAR"));

        try {
            DaHelper.getMode(true, false, "foo");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // ok
        }
        try {
            DaHelper.getMode(false, true, "foo");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    void toGAVTest() {

        String gavString = "org.jboss:test:1.2.3";
        GAV gav = DaHelper.toGAV(gavString);
        assertEquals("org.jboss", gav.getGroupId());
        assertEquals("test", gav.getArtifactId());
        assertEquals("1.2.3", gav.getVersion());

        String gavWrong = "org:haha";
        assertThrows(RuntimeException.class, () -> {
            DaHelper.toGAV(gavWrong);
        });

        String gavWrongAgain = "org:haha:1.2:pom";
        assertThrows(RuntimeException.class, () -> {
            DaHelper.toGAV(gavWrongAgain);
        });
    }

    @Test
    void toNPMPackage() {
        String npmVersionString = "vandijk:4";
        NPMPackage pkg = DaHelper.toNPMPackage(npmVersionString);
        assertEquals("vandijk", pkg.getName());
        assertEquals("4", pkg.getVersion());

        String npmPackageWrong = "org:haha:1.2";
        assertThrows(RuntimeException.class, () -> {
            DaHelper.toNPMPackage(npmPackageWrong);
        });
    }

    @Test
    void executeWithRetrySucceedsFirstAttempt() {
        AtomicInteger calls = new AtomicInteger();
        String result = DaHelper.executeWithRetry(() -> {
            calls.incrementAndGet();
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(1, calls.get());
    }

    @Test
    void executeWithRetrySucceedsAfterTransientFailures() {
        AtomicInteger calls = new AtomicInteger();
        String result = DaHelper.executeWithRetry(() -> {
            if (calls.incrementAndGet() <= 2) {
                throw new ProcessingException("connection reset");
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    void executeWithRetrySucceedsAfter5xxFailure() {
        AtomicInteger calls = new AtomicInteger();
        String result = DaHelper.executeWithRetry(() -> {
            if (calls.incrementAndGet() <= 1) {
                throw new WebApplicationException(Response.status(503).build());
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void executeWithRetryFailsImmediatelyOnSSLHandshake() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(FatalException.class, () -> {
            DaHelper.executeWithRetry(() -> {
                calls.incrementAndGet();
                throw new ProcessingException(new SSLHandshakeException("cert error"));
            }, "test");
        });
        assertEquals(1, calls.get());
    }

    @Test
    void executeWithRetryFailsImmediatelyOn4xx() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(WebApplicationException.class, () -> {
            DaHelper.executeWithRetry(() -> {
                calls.incrementAndGet();
                throw new WebApplicationException(Response.status(400).build());
            }, "test");
        });
        assertEquals(1, calls.get());
    }

    @Test
    void executeWithRetryRetriesOn408RequestTimeout() {
        AtomicInteger calls = new AtomicInteger();
        String result = DaHelper.executeWithRetry(() -> {
            if (calls.incrementAndGet() <= 1) {
                throw new WebApplicationException(Response.status(408).build());
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void executeWithRetryRetriesOn429TooManyRequests() {
        AtomicInteger calls = new AtomicInteger();
        String result = DaHelper.executeWithRetry(() -> {
            if (calls.incrementAndGet() <= 1) {
                throw new WebApplicationException(Response.status(429).build());
            }
            return "ok";
        }, "test");
        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    void executeWithRetryFailsImmediatelyOn501NotImplemented() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(WebApplicationException.class, () -> {
            DaHelper.executeWithRetry(() -> {
                calls.incrementAndGet();
                throw new WebApplicationException(Response.status(501).build());
            }, "test");
        });
        assertEquals(1, calls.get());
    }

    @Test
    void executeWithRetryThrowsAfterMaxRetries() {
        AtomicInteger calls = new AtomicInteger();
        FatalException ex = assertThrows(FatalException.class, () -> {
            DaHelper.executeWithRetry(() -> {
                calls.incrementAndGet();
                throw new ProcessingException("always fails");
            }, "testOp");
        });
        assertEquals(6, calls.get()); // 1 initial + 5 retries
        assertTrue(ex.getMessage().contains("testOp"));
    }

    @Test
    void executeWithRetryRespectsCustomMaxRetries() {
        AtomicInteger calls = new AtomicInteger();
        FatalException ex = assertThrows(FatalException.class, () -> {
            DaHelper.executeWithRetry(() -> {
                calls.incrementAndGet();
                throw new ProcessingException("always fails");
            }, "testOp", 2);
        });
        assertEquals(3, calls.get()); // 1 initial + 2 retries
        assertTrue(ex.getMessage().contains("2 retries"));
    }

    @Test
    void executeWithRetryCustomMaxRetriesSucceedsWithinLimit() {
        AtomicInteger calls = new AtomicInteger();
        String result = DaHelper.executeWithRetry(() -> {
            if (calls.incrementAndGet() <= 3) {
                throw new ProcessingException("transient failure");
            }
            return "ok";
        }, "test", 5);
        assertEquals("ok", result);
        assertEquals(4, calls.get());
    }
}
