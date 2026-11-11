package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

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

    private static final URI PUBLIC_URL = URI.create(
            "https://artifactory-pnc.example.com/artifactory/pnc-builds/org/acme/demo/1.0.0/demo-1.0.0.jar");

    @Test
    void shouldNotParseAnArtifactBaseUrlWhenAbsent() {
        assertNull(BuildOutputDownloader.parseArtifactBaseUrl(null));
        assertNull(BuildOutputDownloader.parseArtifactBaseUrl(""));
        assertNull(BuildOutputDownloader.parseArtifactBaseUrl("   "));
    }

    @Test
    void shouldParseArtifactBaseUrlWithoutTrailingSlashes() {
        assertEquals(
                URI.create("https://repo.example.com"),
                BuildOutputDownloader.parseArtifactBaseUrl(" https://repo.example.com/ "));
        assertEquals(
                URI.create("http://repo.example.com:8081/proxy"),
                BuildOutputDownloader.parseArtifactBaseUrl("http://repo.example.com:8081/proxy//"));
    }

    @Test
    void shouldRejectInvalidArtifactBaseUrls() {
        for (String invalid : List.of(
                "repo.example.com",
                "/artifactory",
                "ftp://repo.example.com",
                "https://user:secret@repo.example.com",
                "https://repo.example.com?x=1",
                "https://repo.example.com#top",
                "https://repo example.com")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> BuildOutputDownloader.parseArtifactBaseUrl(invalid),
                    invalid);
        }
    }

    @Test
    void shouldNotParseALogBaseUrlWhenAbsent() {
        assertNull(BuildOutputDownloader.parseLogBaseUrl(null));
        assertNull(BuildOutputDownloader.parseLogBaseUrl(" "));
    }

    @Test
    void shouldParseLogBaseUrlWithoutPath() {
        assertEquals(
                URI.create("https://bifrost.example.com:8443"),
                BuildOutputDownloader.parseLogBaseUrl("https://bifrost.example.com:8443/"));
    }

    @Test
    void shouldRejectLogBaseUrlWithPathOrInvalidValue() {
        for (String invalid : List.of(
                "https://bifrost.example.com/bifrost",
                "bifrost.example.com",
                "https://user:secret@bifrost.example.com",
                "https://bifrost.example.com?x=1")) {
            assertThrows(IllegalArgumentException.class, () -> BuildOutputDownloader.parseLogBaseUrl(invalid), invalid);
        }
    }

    @Test
    void shouldKeepPublicUrlWithoutArtifactBaseUrl() {
        assertSame(PUBLIC_URL, BuildOutputDownloader.rebase(PUBLIC_URL, null));
    }

    @Test
    void shouldReplaceSchemeAndHostOfPublicUrl() {
        assertEquals(
                URI.create(
                        "http://other-route.example.com:8080/artifactory/pnc-builds/org/acme/demo/1.0.0/demo-1.0.0.jar"),
                BuildOutputDownloader.rebase(
                        PUBLIC_URL,
                        BuildOutputDownloader.parseArtifactBaseUrl("http://other-route.example.com:8080/")));
    }

    @Test
    void shouldPrefixPublicUrlPathWithArtifactBaseUrlPath() {
        assertEquals(
                URI.create("https://proxy.example.com/ibm/artifactory/pnc-builds/org/acme/demo/1.0.0/demo-1.0.0.jar"),
                BuildOutputDownloader
                        .rebase(
                                PUBLIC_URL,
                                BuildOutputDownloader.parseArtifactBaseUrl("https://proxy.example.com/ibm")));
    }

    @Test
    void shouldKeepEncodedPathAndQueryOfPublicUrl() {
        URI publicUrl = URI.create("https://artifactory-pnc.example.com/repo/a%20b/demo.jar?download=true");

        assertEquals(
                URI.create("https://repo.example.com/repo/a%20b/demo.jar?download=true"),
                BuildOutputDownloader
                        .rebase(publicUrl, BuildOutputDownloader.parseArtifactBaseUrl("https://repo.example.com")));
    }

    @Test
    void shouldCreateChecksumSidecarPathNextToArtifact() {
        Path artifact = Path.of("repo", "org", "acme", "demo", "1.0.0", "demo-1.0.0.jar");

        assertEquals(
                Path.of("repo", "org", "acme", "demo", "1.0.0", "demo-1.0.0.jar.sha256"),
                BuildOutputDownloader.checksumPath(artifact, "sha256"));
    }

}
