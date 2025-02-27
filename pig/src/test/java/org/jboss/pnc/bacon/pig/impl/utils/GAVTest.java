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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GAVTest {
    @Test
    void shouldGetGavFromFileName() {
        String absolutePath = "C:\\USERS\\GUEST\\APPDATA\\LOCAL\\TEMP\\deliverable-generation-01234567890123456789\\cpaas-1.0.0.GA-maven-repository\\maven-repository\\org\\apache\\commons\\commons-lang3\\3.6.0.redhat-1\\commons-lang3-3.6.0.redhat-1.jar";
        String repoRootName = "maven-repository/";
        GAV gav = GAV.fromFileName(absolutePath, repoRootName);

        assertThat(gav.getGroupId()).isEqualTo("org.apache.commons");
        assertThat(gav.getArtifactId()).isEqualTo("commons-lang3");
        assertThat(gav.getVersion()).isEqualTo("3.6.0.redhat-1");
        assertThat(gav.getPackaging()).isEqualTo("jar");
        assertThat(gav.getScope()).isNull();
        assertThat(gav.getClassifier()).isNull();
    }

    @Test
    void testGetGavFromPath() {
        // Artifact with classifier
        String path = "org/kie/kogito/kogito-ddl-runtimes/1.32.0.Final-redhat-00003/kogito-ddl-runtimes-1.32.0.Final-redhat-00003-db-scripts.zip";
        GAV gav = new GAV(path);

        assertThat(gav.getGroupId()).isEqualTo("org.kie.kogito");
        assertThat(gav.getArtifactId()).isEqualTo("kogito-ddl-runtimes");
        assertThat(gav.getVersion()).isEqualTo("1.32.0.Final-redhat-00003");
        assertThat(gav.getPackaging()).isEqualTo("zip");
        assertThat(gav.getClassifier()).isEqualTo("db-scripts");

        // Artifact without classifier
        path = "com/fasterxml/jackson/core/jackson-databind/2.13.4.2-redhat-00001/jackson-databind-2.13.4.2-redhat-00001.jar";
        gav = new GAV(path);

        assertThat(gav.getGroupId()).isEqualTo("com.fasterxml.jackson.core");
        assertThat(gav.getArtifactId()).isEqualTo("jackson-databind");
        assertThat(gav.getVersion()).isEqualTo("2.13.4.2-redhat-00001");
        assertThat(gav.getPackaging()).isEqualTo("jar");
        assertThat(gav.getClassifier()).isNull();

        // Artifact with classifier and extension with a dot (tar.gz)
        path = "org/kie/kogito/kogito-runtimes/1.32.0.Final-redhat-00003/kogito-runtimes-1.32.0.Final-redhat-00003-project-sources.tar.gz";
        gav = new GAV(path);

        assertThat(gav.getGroupId()).isEqualTo("org.kie.kogito");
        assertThat(gav.getArtifactId()).isEqualTo("kogito-runtimes");
        assertThat(gav.getVersion()).isEqualTo("1.32.0.Final-redhat-00003");
        assertThat(gav.getPackaging()).isEqualTo("tar.gz");
        assertThat(gav.getClassifier()).isEqualTo("project-sources");

        // Artifact with classifier matching the version
        path = "io/quarkus/quarkus-bom-quarkus-platform-descriptor/3.8.5.redhat-00004/quarkus-bom-quarkus-platform-descriptor-3.8.5.redhat-00004-3.8.5.redhat-00004.json";
        gav = new GAV(path);

        assertThat(gav.getGroupId()).isEqualTo("io.quarkus");
        assertThat(gav.getArtifactId()).isEqualTo("quarkus-bom-quarkus-platform-descriptor");
        assertThat(gav.getVersion()).isEqualTo("3.8.5.redhat-00004");
        assertThat(gav.getPackaging()).isEqualTo("json");
        assertThat(gav.getClassifier()).isEqualTo("3.8.5.redhat-00004");
    }

    @Test
    void testGetGapvc() {
        // Artifact without classifier
        String gapvc = "com.fasterxml.jackson.core:jackson-databin:jar:2.13.4.2-redhat-00001";
        String[] sections = gapvc.split(":");
        GAV gav = new GAV(sections[0], sections[1], sections[3], sections[2]);

        assertThat(gav.toGapvc()).isEqualTo(gapvc);

        // Artifact with classifier
        gapvc = "org.kie.kogito:kogito-ddl-runtime:zip:1.32.0.Final-redhat-00003:db-scripts";
        sections = gapvc.split(":");
        gav = new GAV(sections[0], sections[1], sections[3], sections[2], sections[4]);

        assertThat(gav.toGapvc()).isEqualTo(gapvc);
    }

    /**
     * This test is related to NCL-7238: when an artifact has no packaging, we just set the packaging to string "empty"
     * to generate proper PURL and to avoid artifact duplicates. Unfortunately this affects how the GAV uri is generated
     * <p/>
     * This is a test to make sure we handle it properly
     */
    @Test
    void testEmptyPackagingUri() {
        String gapvc = "com.fasterxml.jackson.core:jackson-databin:pom:2.13.4";
        GAV gav = GAV.fromColonSeparatedGAPV(gapvc);
        assertThat(gav.toFileName()).isEqualTo("jackson-databin-2.13.4.pom");
        assertThat(gav.toUri())
                .isEqualTo("com/fasterxml/jackson/core/jackson-databin/2.13.4/jackson-databin-2.13.4.pom");

        // with empty packaging, there should be no extension in the filename or url
        String gapvcEmpty = "com.fasterxml.jackson.core:jackson-databin:empty:2.13.4";
        GAV gavEmpty = GAV.fromColonSeparatedGAPV(gapvcEmpty);
        assertThat(gavEmpty.toFileName()).isEqualTo("jackson-databin-2.13.4");
        assertThat(gavEmpty.toUri())
                .isEqualTo("com/fasterxml/jackson/core/jackson-databin/2.13.4/jackson-databin-2.13.4");
    }
}
