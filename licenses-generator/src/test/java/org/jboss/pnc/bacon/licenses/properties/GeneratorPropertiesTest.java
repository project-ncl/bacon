/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.licenses.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class GeneratorPropertiesTest {

    @Test
    public void shouldGetDefaultProperties() {
        GeneratorProperties properties = new GeneratorProperties("test_properties/empty.properties");
        assertThat(properties.getRepositories()).containsOnlyKeys("Maven Central")
                .containsValues("https://repo1.maven.org/maven2");
        assertThat(properties.getLicenseServiceUrl()).isEmpty();
    }

    @Test
    public void shouldGetModifiedProperties() {
        GeneratorProperties properties = new GeneratorProperties("test_properties/modified.properties");
        assertThat(properties.getRepositories()).containsOnlyKeys("testRepositoryName1", "testRepositoryName2")
                .containsValues("testRepositoryUrl1", "testRepositoryUrl2");
        assertThat(properties.getLicenseServiceUrl()).contains("http://10.10.10.10/find-license-check-record");
    }

    @Test
    public void shouldFailToGetWrongRepositories() {
        Assertions.assertThrows(GeneratorPropertiesException.class, () -> {
            GeneratorProperties properties = new GeneratorProperties("test_properties/wrong-repositories.properties");
            properties.getRepositories();
        });
    }

    @Test
    public void shouldGetAliasesFilePath() {
        GeneratorProperties properties = new GeneratorProperties();
        assertThat(properties.getAliasesFilePath()).isEqualTo("rh-license-names.json");

        properties.setAliasesFilePath("another-license-names.json");
        assertThat(properties.getAliasesFilePath()).isEqualTo("another-license-names.json");
    }

    @Test
    public void shouldGetExceptionsFilePath() {
        GeneratorProperties properties = new GeneratorProperties();
        assertThat(properties.getExceptionsFilePath()).isEqualTo("rh-license-exceptions.json");

        properties.setExceptionsFilePath("another-license-exceptions.json");
        assertThat(properties.getExceptionsFilePath()).isEqualTo("another-license-exceptions.json");
    }

}
