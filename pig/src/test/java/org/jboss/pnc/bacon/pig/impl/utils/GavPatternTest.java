/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GavPatternTest {

    @Test
    public void groupId() {
        final GavPattern p = GavPattern.of("org.my-group");
        Assertions.assertTrue(p.matches("org.my-group", "foo", "jar", "", "1.2.3"));
        Assertions.assertTrue(p.matches("org.my-group", "foo", "jar", null, "1.2.3"));
        Assertions.assertFalse(p.matches("org.foo", "foo", "jar", null, "1.2.3"));
    }

    @Test
    public void groupIdPrefix() {
        final GavPattern p = GavPattern.of("org.my-group*");
        Assertions.assertTrue(p.matches("org.my-group", "foo", "jar", null, "1.2.3"));
        Assertions.assertTrue(p.matches("org.my-group-bar", "foo", "jar", null, "1.2.3"));
        Assertions.assertFalse(p.matches("org.foo", "foo", "jar", null, "1.2.3"));
    }

    @Test
    public void artifactId() {
        final GavPattern p = GavPattern.of("org.my-group:my-artifact");
        Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.3"));
        Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.4"));
        Assertions.assertFalse(p.matches("org.my-group", "foo", "jar", null, "1.2.3"));
    }

    @Test
    public void gav() {
        final GavPattern p = GavPattern.of("org.my-group:my-artifact:1.2.3");
        Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.3"));
        Assertions.assertFalse(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.4"));
    }

    @Test
    public void gatcv() {
        {
            final GavPattern p = GavPattern.of("org.my-group:my-artifact:*:*:1.2.3");
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "jar", "foo", "1.2.3"));
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "jar", "", "1.2.3"));
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.4"));
        }
        {
            final GavPattern p = GavPattern.of("org.my-group:my-artifact:*:foo:1.2.3");
            Assertions.assertTrue(p.matches("org.my-group", "my-artifact", "jar", "foo", "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", "jar", "", "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.3"));
            Assertions.assertFalse(p.matches("org.my-group", "my-artifact", "jar", null, "1.2.4"));
        }
    }

    @Test
    public void gatv() {
        try {
            GavPattern.of("org.my-group:my-artifact:jar:1.2.3");
            Assertions.fail("Expected IllegalStateException for 'org.my-group:my-artifact:jar:1.2.3'");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void ofToString() {
        assertOfToString("org.my-group");
        assertOfToString("org.my-group*");
        assertOfToString("org.my-group:my-artifact");
        assertOfToString("org.my-group:my-artifact:1.2.3");
        Assertions.assertEquals(
                "org.my-group:my-artifact:1.2.3",
                GavPattern.of("org.my-group:my-artifact:*:*:1.2.3").toString());
        try {
            assertOfToString("org.my-group:my-artifact:foo:1.2.3");
            Assertions.fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
        Assertions.assertEquals(
                "org.my-group:my-artifact:1.2.3",
                GavPattern.of("org.my-group:my-artifact:*:*:1.2.3").toString());
        assertOfToString("org.my-group:my-artifact:jar:foo:1.2.3");
    }

    static void assertOfToString(String pattern) {
        Assertions.assertEquals(pattern, GavPattern.of(pattern).toString());
    }

}
