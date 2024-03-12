/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.jboss.bacon.experimental.impl.generator;

import org.jboss.bacon.experimental.impl.dependencies.Project;
import org.jboss.da.model.rest.GAV;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectNameGeneratorTest {

    private ProjectNameGenerator png = new ProjectNameGenerator();

    @Test
    public void shouldGenerateName() {
        Project nonRedhat = new Project();
        nonRedhat.setGavs(Collections.singleton(new GAV("org.slf4j", "slf4j-api", "1.7.25")));
        nonRedhat.setDependencies(Set.of());
        png.nameProjects(Set.of(nonRedhat));

        assertThat(nonRedhat.getName()).isEqualTo("org.slf4j-slf4j-api-1.7.25-AUTOBUILD");

        Project redhat = new Project();
        redhat.setGavs(Collections.singleton(new GAV("org.slf4j", "slf4j-api", "1.7.25.redhat-00001")));
        redhat.setDependencies(Set.of());
        png.nameProjects(Set.of(redhat));

        assertThat(redhat.getName()).isEqualTo("org.slf4j-slf4j-api-1.7.25-AUTOBUILD");

        Project temporaryRedhat = new Project();
        temporaryRedhat
                .setGavs(Collections.singleton(new GAV("org.slf4j", "slf4j-api", "1.7.25.temporary-redhat-00001")));
        temporaryRedhat.setDependencies(Set.of());
        png.nameProjects(Set.of(temporaryRedhat));

        assertThat(temporaryRedhat.getName()).isEqualTo("org.slf4j-slf4j-api-1.7.25-AUTOBUILD");
    }

    @Test
    public void shouldDetectDuplicate() {
        Project nonRedhat = new Project();
        nonRedhat.setGavs(Collections.singleton(new GAV("org.slf4j", "slf4j-api", "1.7.25")));
        nonRedhat.setDependencies(Set.of());
        Project redhat = new Project();
        redhat.setGavs(Collections.singleton(new GAV("org.slf4j", "slf4j-api", "1.7.25.redhat-00001")));
        redhat.setDependencies(Set.of());

        png.nameProjects(Set.of(nonRedhat, redhat));

        assertThat(nonRedhat.isConflictingName()).isTrue();
        assertThat(nonRedhat.getName()).isEqualTo("org.slf4j-slf4j-api-1.7.25-AUTOBUILD");
        assertThat(redhat.isConflictingName()).isTrue();
        assertThat(redhat.getName()).isEqualTo("org.slf4j-slf4j-api-1.7.25.redhat-00001-AUTOBUILD");
    }

    @Test
    public void shouldGetShorterName() {
        GAV longer = new GAV("org.jboss", "jboss-parent-mr-jar", "32");
        GAV shorter = new GAV("org.jboss", "jboss-parent", "32");

        Project nonRedhat = new Project();
        nonRedhat.setGavs(Set.of(longer, shorter));
        nonRedhat.setDependencies(Set.of());

        png.nameProjects(Set.of(nonRedhat));

        assertThat(nonRedhat.getName()).isEqualTo("org.jboss-jboss-parent-32-AUTOBUILD");
    }

    @Test
    public void shouldDetectDuplicateWithTemporary() {
        Project nonRedhat = new Project();
        nonRedhat.setGavs(Collections.singleton(new GAV("org.slf4j", "slf4j-api", "1.7.25")));
        nonRedhat.setDependencies(Set.of());
        Project temporaryRedhat = new Project();
        temporaryRedhat
                .setGavs(Collections.singleton(new GAV("org.slf4j", "slf4j-api", "1.7.25.temporary-redhat-00001")));
        temporaryRedhat.setDependencies(Set.of());

        png.nameProjects(Set.of(nonRedhat, temporaryRedhat));

        assertThat(nonRedhat.isConflictingName()).isTrue();
        assertThat(nonRedhat.getName()).isEqualTo("org.slf4j-slf4j-api-1.7.25-AUTOBUILD");
        assertThat(temporaryRedhat.isConflictingName()).isTrue();
        assertThat(temporaryRedhat.getName()).isEqualTo("org.slf4j-slf4j-api-1.7.25.temporary-redhat-00001-AUTOBUILD");
    }
}
