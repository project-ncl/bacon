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

import org.jboss.da.model.rest.GAV;
import org.jboss.da.model.rest.NPMPackage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DaHelperTest {

    @Test
    void getModeTest() {

        assertEquals("SERVICE", DaHelper.getMode(false, true));
        assertEquals("SERVICE_TEMPORARY", DaHelper.getMode(true, true));
        assertEquals("PERSISTENT", DaHelper.getMode(false, false));
        assertEquals("TEMPORARY", DaHelper.getMode(true, false));
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
}
