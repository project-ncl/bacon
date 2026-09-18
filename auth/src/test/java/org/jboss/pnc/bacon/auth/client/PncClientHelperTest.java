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
package org.jboss.pnc.bacon.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PncClientHelperTest {

    private static final String SOURCE = "pnc.client.readTimeoutMillis/PNC_CLIENT_READ_TIMEOUT_MILLIS";

    @Test
    void parseTimeoutMillisReturnsPositiveValue() {
        assertEquals(300000L, PncClientHelper.parseTimeoutMillis("300000", SOURCE));
    }

    @Test
    void parseTimeoutMillisTrimsWhitespace() {
        assertEquals(60000L, PncClientHelper.parseTimeoutMillis("  60000  ", SOURCE));
    }

    @Test
    void parseTimeoutMillisReturnsNullForNull() {
        assertNull(PncClientHelper.parseTimeoutMillis(null, SOURCE));
    }

    @Test
    void parseTimeoutMillisReturnsNullForBlank() {
        assertNull(PncClientHelper.parseTimeoutMillis("   ", SOURCE));
    }

    @Test
    void parseTimeoutMillisReturnsNullForNonNumeric() {
        assertNull(PncClientHelper.parseTimeoutMillis("not-a-number", SOURCE));
    }

    @Test
    void parseTimeoutMillisReturnsNullForZero() {
        assertNull(PncClientHelper.parseTimeoutMillis("0", SOURCE));
    }

    @Test
    void parseTimeoutMillisReturnsNullForNegative() {
        assertNull(PncClientHelper.parseTimeoutMillis("-1", SOURCE));
    }

    @Test
    void resolveTimeoutOverrideReadsSystemProperty() {
        String property = "test.pnc.client.readTimeoutMillis";
        String previous = System.getProperty(property);
        System.setProperty(property, "120000");
        try {
            assertEquals(120000L, PncClientHelper.resolveTimeoutOverride(property, "TEST_PNC_UNSET_ENV_VAR"));
        } finally {
            restoreProperty(property, previous);
        }
    }

    @Test
    void resolveTimeoutOverrideReturnsNullWhenUnset() {
        // Neither the system property nor the environment variable are expected to be set in CI.
        assertNull(
                PncClientHelper
                        .resolveTimeoutOverride("test.pnc.client.unsetTimeoutMillis", "TEST_PNC_UNSET_ENV_VAR"));
    }

    @Test
    void resolveTimeoutOverrideIgnoresInvalidSystemProperty() {
        String property = "test.pnc.client.invalidTimeoutMillis";
        String previous = System.getProperty(property);
        System.setProperty(property, "bogus");
        try {
            assertNull(PncClientHelper.resolveTimeoutOverride(property, "TEST_PNC_UNSET_ENV_VAR"));
        } finally {
            restoreProperty(property, previous);
        }
    }

    private static void restoreProperty(String property, String previous) {
        if (previous == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, previous);
        }
    }
}
