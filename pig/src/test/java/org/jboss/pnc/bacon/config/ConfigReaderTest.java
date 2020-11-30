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
package org.jboss.pnc.bacon.config;

import com.google.common.collect.ImmutableMap;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.config.JavadocGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
class ConfigReaderTest {
    @Test
    void shouldDeserializeJavadocGenerationStrategy() {
        PigConfiguration config = loadBuildConfig("/build-config-VarUsage.yaml");
        assertNotNull(config);
        JavadocGenerationStrategy strategy = config.getFlow().getJavadocGeneration().getStrategy();
        assertThat(strategy).isEqualTo(JavadocGenerationStrategy.IGNORE);
    }

    private PigConfiguration loadBuildConfig(String buildConfig) {
        return loadBuildConfig(buildConfig, Collections.emptyMap());
    }

    private PigConfiguration loadBuildConfig(String buildConfig, Map<String, String> buildVarOverrides) {
        InputStream configStream = PigConfiguration.class.getResourceAsStream(buildConfig);
        return PigConfiguration.load(configStream, buildVarOverrides);
    }

    @Test
    void shouldExpandVariables() {
        PigConfiguration config = loadBuildConfig("/build-config-VarUsage.yaml");
        assertNotNull(config);
        if (!config.getVersion().equals("3.5.1")) {
            fail("Version does not match predefined variable");
        }
    }

    @Test
    void shouldNotOverrideEnvironmentNameWithId() {
        PigConfiguration config = loadBuildConfig("/build-config-environment-override.yaml");
        assertNotNull(config);
        for (BuildConfig build : config.getBuilds()) {
            if (build.getName().equals("profile-injection-1.0.0")) {
                if (build.getRawEnvironmentId() != null && build.getEnvironmentName().equals("OpenJDK 1.8")) {
                    fail("The envId from defaultBuildParameters should not override if environmentName is declared");
                }
            }
        }
    }

    @Test
    void shouldExpandAndOverrideVariables() {
        Map<String, String> overrides = ImmutableMap.of("productName", "vertx", "milestone", "ER2");
        PigConfiguration config = loadBuildConfig("/build-config-VarUsage.yaml", overrides);
        assertNotNull(config);
        if (!config.getVersion().equals("3.5.1")) {
            fail("Version does not match predefined variable");
        }

        if (!config.getMilestone().equals("ER2")) {
            fail("Milestone does not match overridden value");
        }

        if (!config.getProduct().getName().equals("vertx")) {
            fail("Product name does not match overridden value");
        }
    }

    @Test
    void shouldFailOnUnkownVariable() {
        boolean thrown = false;
        try {
            loadBuildConfig("/build-config-UnknownVar.yaml");
        } catch (RuntimeException e) {
            if (e.getMessage().startsWith("No variable definition for")) {
                thrown = true;
            }
        }
        assertTrue(thrown);
    }
}
