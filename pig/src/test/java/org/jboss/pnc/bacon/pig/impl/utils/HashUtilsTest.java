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

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {
    static final Path testDirs = Paths.get("src", "test", "resources", "hash");

    @Test
    void shouldGiveTheSameHashForUnmodified() {
        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String exactCopyHash = HashUtils.hashDirectory(testDirs.resolve("copy"));
        assertThat(originalHash).isEqualTo(exactCopyHash);
    }

    @Test
    void shouldGiveTheSameHashTwice() {
        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String secondRunHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        assertThat(originalHash).isEqualTo(secondRunHash);
    }

    @Test
    void shouldGiveDifferentHashForAdditionalNewline() {
        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String exactCopyHash = HashUtils.hashDirectory(testDirs.resolve("additional-newline"));
        assertThat(originalHash).isNotEqualTo(exactCopyHash);
    }

    @Test
    void shouldGiveDifferentHashForMovedFile() {
        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String exactCopyHash = HashUtils.hashDirectory(testDirs.resolve("moved-file"));
        assertThat(originalHash).isNotEqualTo(exactCopyHash);
    }

    @Test
    void shouldGiveDifferentHashForRenamedFile() {
        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String exactCopyHash = HashUtils.hashDirectory(testDirs.resolve("renamed-file"));
        assertThat(originalHash).isNotEqualTo(exactCopyHash);
    }

    @Test
    void shouldGiveDifferentHashForRemovedFile() {
        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String exactCopyHash = HashUtils.hashDirectory(testDirs.resolve("removed-file"));
        assertThat(originalHash).isNotEqualTo(exactCopyHash);
    }

    @Test
    void shouldGiveDifferentHashForAddedFile() {
        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String exactCopyHash = HashUtils.hashDirectory(testDirs.resolve("additional-file"));
        assertThat(originalHash).isNotEqualTo(exactCopyHash);
    }

}
