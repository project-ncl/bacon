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

    @Test
    void shouldIgnoreSomePaths() {

        String originalHash = HashUtils.hashDirectory(testDirs.resolve("original"));
        String copyIgnoreFileHash = HashUtils.hashDirectory(testDirs.resolve("copy-with-file-to-be-ignored"), path -> {
            return path.normalize().endsWith("ignore-me");
        });
    }

}
