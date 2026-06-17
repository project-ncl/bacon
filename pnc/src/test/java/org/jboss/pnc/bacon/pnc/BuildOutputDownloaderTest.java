package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BuildOutputDownloaderTest {

    @Test
    void shouldCreateMavenPathFromFourPartIdentifier() {
        Path path = BuildOutputDownloader.mavenPathFromIdentifier("org.acme:demo:jar:1.0.0");

        assertEquals(Path.of("org", "acme", "demo", "1.0.0", "demo-1.0.0.jar"), path);
    }

    @Test
    void shouldCreateMavenPathFromFivePartIdentifier() {
        Path path = BuildOutputDownloader.mavenPathFromIdentifier("org.acme:demo:jar:sources:1.0.0");

        assertEquals(Path.of("org", "acme", "demo", "1.0.0", "demo-1.0.0-sources.jar"), path);
    }

    @Test
    void shouldReturnNullForUnsupportedIdentifier() {
        assertEquals(null, BuildOutputDownloader.mavenPathFromIdentifier("not-a-maven-identifier"));
    }

    @Test
    void shouldRejectPathTraversalWhenResolvingArtifactPath() {
        assertThrows(
                RuntimeException.class,
                () -> BuildOutputDownloader.safeResolve(Path.of("/tmp/root"), Path.of("..", "evil.jar")));
    }

    @Test
    void shouldCreateChecksumSidecarPathNextToArtifact() {
        Path artifact = Path.of("repo", "org", "acme", "demo", "1.0.0", "demo-1.0.0.jar");

        assertEquals(
                Path.of("repo", "org", "acme", "demo", "1.0.0", "demo-1.0.0.jar.sha256"),
                BuildOutputDownloader.checksumPath(artifact, "sha256"));
    }

}
