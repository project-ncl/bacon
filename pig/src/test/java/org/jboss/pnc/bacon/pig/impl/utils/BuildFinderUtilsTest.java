/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.utils;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.build.finder.core.BuildConfig;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildFinderUtilsTest {
    @Test
    void getKojiBuildFinderConfig() {
        File configFile = new File(
                new File(BuildFinderUtilsTest.class.getResource("/").getFile()).getParentFile()
                        .getParentFile()
                        .getParentFile(),
                "config.yaml");
        Config.configure(configFile.getParent(), configFile.getName(), "default");
        Config.instance().getActiveProfile().getPig().setKojiHubUrl("https://127.0.0.1");

        BuildConfig bc = BuildFinderUtils.getKojiBuildFinderConfig();
        assertTrue(bc.getKojiHubURL().toString().contains("127.0.0.1"));
    }
}
