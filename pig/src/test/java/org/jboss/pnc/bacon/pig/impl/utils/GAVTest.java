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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GAVTest {
    @Test
    void shouldGetGavFromFileName() {
        String absolutePath = "C:\\USERS\\GUEST\\APPDATA\\LOCAL\\TEMP\\deliverable-generation01234567890123456789\\cpaas-1.0.0.GA-maven-repository\\maven-repository\\org\\apache\\commons\\commons-lang3\\3.6.0.redhat-1\\commons-lang3-3.6.0.redhat-1.jar";
        String repoRootName = "maven-repository/";
        GAV gav = GAV.fromFileName(absolutePath, repoRootName);

        assertThat(gav.getGroupId()).isEqualTo("org.apache.commons");
        assertThat(gav.getArtifactId()).isEqualTo("commons-lang3");
        assertThat(gav.getVersion()).isEqualTo("3.6.0.redhat-1");
        assertThat(gav.getPackaging()).isEqualTo("jar");
        assertThat(gav.getScope()).isNull();
        assertThat(gav.getClassifier()).isNull();
    }
}
