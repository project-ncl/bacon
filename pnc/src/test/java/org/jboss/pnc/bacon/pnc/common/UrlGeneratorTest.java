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
package org.jboss.pnc.bacon.pnc.common;

import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stupid tests that check for accidental modifications in UrlGenerator
 */
class UrlGeneratorTest {

    @BeforeAll
    static void init() throws IOException {
        File file = new File(UrlGeneratorTest.class.getClassLoader().getResource("config.yaml").getFile());
        Config.configure(file.getParent(), "config.yaml", "default");
        Config.initialize();
    }

    @Test
    void testGenerateBuildUrl() {
        String url = UrlGenerator.generateBuildUrl("abcd");
        assertEquals(Config.instance().getActiveProfile().getPnc().getUrl() + "/pnc-web/#/builds/abcd", url);
    }

    @Test
    void testGenerateGroupBuildUrl() {
        String url = UrlGenerator.generateGroupBuildUrl("abcd");
        assertEquals(Config.instance().getActiveProfile().getPnc().getUrl() + "/pnc-web/#/group-builds/abcd", url);
    }

    @Test
    void testGenerateGroupConfigUrl() {
        String url = UrlGenerator.generateGroupConfigUrl("abcd");
        assertEquals(Config.instance().getActiveProfile().getPnc().getUrl() + "/pnc-web/#/group-configs/abcd", url);
    }
}
