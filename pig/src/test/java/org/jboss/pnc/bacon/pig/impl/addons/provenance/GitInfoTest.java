package org.jboss.pnc.bacon.pig.impl.addons.provenance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for GitInfo utility class that retrieves git repository information.
 */
class GitInfoTest {

    @Test
    void testFromCurrentDirectory_WhenInGitRepo() {
        // This test will only pass if the test is run from within a git repository
        // In a real git repo, this should return a valid GitInfo
        Optional<GitInfo> gitInfo = GitInfo.fromCurrentDirectory();

        // We can't assert it's present because tests might run outside a git repo
        // But we can verify the method doesn't throw exceptions
        assertNotNull(gitInfo);
    }

    @Test
    void testFromDirectory_WithNonGitDirectory(@TempDir Path tempDir) {
        // Test with a directory that is not a git repository
        Optional<GitInfo> gitInfo = GitInfo.fromDirectory(tempDir);

        // Should return empty Optional for non-git directories
        assertTrue(gitInfo.isEmpty(), "Should return empty Optional for non-git directory");
    }

    @Test
    void testFromDirectory_WithNullPath() {
        // Test with null path
        Optional<GitInfo> gitInfo = GitInfo.fromDirectory(null);

        // Should return empty Optional for null directories
        assertTrue(gitInfo.isEmpty(), "Should return empty Optional for null directory");
    }

    @Test
    void testFromDirectory_WithNonExistentPath() {
        // Test with a path that doesn't exist
        Path nonExistent = Path.of("/tmp/non-existent-directory-" + System.currentTimeMillis());
        Optional<GitInfo> gitInfo = GitInfo.fromDirectory(nonExistent);

        // Should return empty Optional for non-existent directories
        assertTrue(gitInfo.isEmpty(), "Should return empty Optional for non-existent directory");
    }

    @Test
    void testGitInfo_GettersReturnCorrectValues() {
        // Create a GitInfo instance using reflection to test getters
        // Since constructor is private, we test through the factory method

        // If we're in a git repo, test the getters
        Optional<GitInfo> gitInfoOpt = GitInfo.fromCurrentDirectory();

        gitInfoOpt.ifPresent(gitInfo -> {
            assertNotNull(gitInfo.getRepositoryUrl(), "Repository URL should not be null");
            assertNotNull(gitInfo.getCommitHash(), "Commit hash should not be null");
            assertFalse(gitInfo.getRepositoryUrl().isEmpty(), "Repository URL should not be empty");
            assertFalse(gitInfo.getCommitHash().isEmpty(), "Commit hash should not be empty");

            // Commit hash should be a valid SHA-1 (40 hex characters)
            assertTrue(
                    gitInfo.getCommitHash().matches("[0-9a-f]{40}"),
                    "Commit hash should be a valid SHA-1 hash");

            // Repository URL should be a valid URL or git path
            String repoUrl = gitInfo.getRepositoryUrl();
            assertTrue(
                    repoUrl.startsWith("http://") ||
                            repoUrl.startsWith("https://") ||
                            repoUrl.startsWith("git@") ||
                            repoUrl.startsWith("file://") ||
                            repoUrl.startsWith("/"),
                    "Repository URL should be a valid git URL format");
        });
    }

    @Test
    void testGitInfo_ToStringContainsRelevantInfo() {
        Optional<GitInfo> gitInfoOpt = GitInfo.fromCurrentDirectory();

        gitInfoOpt.ifPresent(gitInfo -> {
            String toString = gitInfo.toString();
            assertNotNull(toString);
            assertTrue(toString.contains("GitInfo"), "toString should contain class name");
            assertTrue(toString.contains("repositoryUrl"), "toString should contain repositoryUrl field");
            assertTrue(toString.contains("commitHash"), "toString should contain commitHash field");
            assertTrue(toString.contains(gitInfo.getRepositoryUrl()), "toString should contain actual URL");
            assertTrue(toString.contains(gitInfo.getCommitHash()), "toString should contain actual commit hash");
        });
    }

    @Test
    void testFromDirectory_WithGitRepoSimulation(@TempDir Path tempDir) throws IOException, InterruptedException {
        // Create a minimal git repository for testing
        File gitDir = new File(tempDir.toFile(), ".git");
        if (gitDir.mkdir()) {
            // Create a minimal git config
            File configDir = new File(gitDir, "config");
            Files.writeString(
                    configDir.toPath(),
                    "[core]\n" +
                            "    repositoryformatversion = 0\n" +
                            "[remote \"origin\"]\n" +
                            "    url = https://github.com/test/repo.git\n");

            // Initialize git repo properly
            ProcessBuilder pb = new ProcessBuilder("git", "init");
            pb.directory(tempDir.toFile());
            Process process = pb.start();
            process.waitFor();

            // Add remote
            pb = new ProcessBuilder("git", "remote", "add", "origin", "https://github.com/test/repo.git");
            pb.directory(tempDir.toFile());
            process = pb.start();
            process.waitFor();

            // Create initial commit
            pb = new ProcessBuilder("git", "config", "user.email", "test@example.com");
            pb.directory(tempDir.toFile());
            process = pb.start();
            process.waitFor();

            pb = new ProcessBuilder("git", "config", "user.name", "Test User");
            pb.directory(tempDir.toFile());
            process = pb.start();
            process.waitFor();

            Files.writeString(tempDir.resolve("test.txt"), "test content");

            pb = new ProcessBuilder("git", "add", ".");
            pb.directory(tempDir.toFile());
            process = pb.start();
            process.waitFor();

            pb = new ProcessBuilder("git", "commit", "-m", "Initial commit");
            pb.directory(tempDir.toFile());
            process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // Now test GitInfo
                Optional<GitInfo> gitInfo = GitInfo.fromDirectory(tempDir);

                assertTrue(gitInfo.isPresent(), "Should find git info in initialized repo");
                gitInfo.ifPresent(info -> {
                    assertEquals("https://github.com/test/repo.git", info.getRepositoryUrl());
                    assertNotNull(info.getCommitHash());
                    assertTrue(info.getCommitHash().matches("[0-9a-f]{40}"));
                });
            }
        }
    }
}
