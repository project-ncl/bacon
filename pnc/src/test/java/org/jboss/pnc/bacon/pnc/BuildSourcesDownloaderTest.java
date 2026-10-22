package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.dto.SCMRepository;
import org.junit.jupiter.api.Test;

class BuildSourcesDownloaderTest {

    @Test
    void shouldPreferBuildConfigRevisionScmRevision() {
        Build build = mock(Build.class);
        BuildConfigurationRevisionRef revision = mock(BuildConfigurationRevisionRef.class);

        when(build.getScmRevision()).thenReturn("build-revision");
        when(build.getBuildConfigRevision()).thenReturn(revision);
        when(revision.getScmRevision()).thenReturn("config-revision");

        assertEquals("config-revision", BuildSourcesDownloader.selectScmRevision(build));
    }

    @Test
    void shouldFallbackToBuildScmRevision() {
        Build build = mock(Build.class);
        BuildConfigurationRevisionRef revision = mock(BuildConfigurationRevisionRef.class);

        when(build.getScmRevision()).thenReturn("build-revision");
        when(build.getBuildConfigRevision()).thenReturn(revision);
        when(revision.getScmRevision()).thenReturn(null);

        assertEquals("build-revision", BuildSourcesDownloader.selectScmRevision(build));
    }

    @Test
    void shouldRecognizeGzipMagicBytes() {
        assertTrue(BuildSourcesDownloader.isGzip(new byte[] { (byte) 0x1f, (byte) 0x8b, 0x08 }));
        assertFalse(BuildSourcesDownloader.isGzip("<!DOCTYPE html>".getBytes()));
    }

    @Test
    void shouldPreferImmutableBuildRevisionAndRetainConfiguredRevisionAsFallback() {
        Build build = mock(Build.class);
        BuildConfigurationRevisionRef revision = mock(BuildConfigurationRevisionRef.class);

        when(build.getScmRevision()).thenReturn("0123456789abcdef0123456789abcdef01234567");
        when(build.getBuildConfigRevision()).thenReturn(revision);
        when(revision.getScmRevision()).thenReturn("commons-2_6");

        assertEquals(
                List.of("0123456789abcdef0123456789abcdef01234567", "commons-2_6"),
                BuildSourcesDownloader.selectScmRevisions(build));
    }

    @Test
    void shouldQualifyAmbiguousGithubEnterpriseBranchAndTagNames() {
        List<BuildSourcesDownloader.SourceArchiveCandidate> candidates = BuildSourcesDownloader
                .sourceArchiveCandidates(
                        "https://github.ibm.com/pnc-prod/apache-xmlgraphics-commons.git",
                        "commons-2_6");

        assertEquals(
                List.of(
                        URI.create(
                                "https://github.ibm.com/api/v3/repos/pnc-prod/apache-xmlgraphics-commons/tarball/commons-2_6"),
                        URI.create(
                                "https://github.ibm.com/api/v3/repos/pnc-prod/apache-xmlgraphics-commons/tarball/refs/tags/commons-2_6"),
                        URI.create(
                                "https://github.ibm.com/api/v3/repos/pnc-prod/apache-xmlgraphics-commons/tarball/refs/heads/commons-2_6"),
                        URI.create(
                                "https://github.ibm.com/pnc-prod/apache-xmlgraphics-commons/archive/commons-2_6.tar.gz"),
                        URI.create(
                                "https://github.ibm.com/pnc-prod/apache-xmlgraphics-commons/archive/refs/tags/commons-2_6.tar.gz"),
                        URI.create(
                                "https://github.ibm.com/pnc-prod/apache-xmlgraphics-commons/archive/refs/heads/commons-2_6.tar.gz")),
                candidates.stream().map(BuildSourcesDownloader.SourceArchiveCandidate::uri).toList());
    }

    @Test
    void shouldNotAddBranchAndTagAlternativesForCommitSha() {
        List<BuildSourcesDownloader.SourceArchiveCandidate> candidates = BuildSourcesDownloader
                .sourceArchiveCandidates(
                        "git@github.com:project-ncl/bacon.git",
                        "0123456789abcdef0123456789abcdef01234567");

        assertEquals(2, candidates.size());
        assertEquals(
                URI.create(
                        "https://api.github.com/repos/project-ncl/bacon/tarball/0123456789abcdef0123456789abcdef01234567"),
                candidates.get(0).uri());
    }

    @Test
    void shouldEncodeSpecialCharactersWithoutDroppingRefSlashes() {
        List<BuildSourcesDownloader.SourceArchiveCandidate> candidates = BuildSourcesDownloader
                .sourceArchiveCandidates(
                        "https://github.ibm.com/pnc-prod/example.git",
                        "release#1");

        assertEquals(
                URI.create("https://github.ibm.com/api/v3/repos/pnc-prod/example/tarball/release%231"),
                candidates.get(0).uri());
        assertEquals(
                URI.create("https://github.ibm.com/api/v3/repos/pnc-prod/example/tarball/refs/tags/release%231"),
                candidates.get(1).uri());
    }

    @Test
    void shouldBuildCompareDiffUriForGithubEnterprise() {
        BuildSourcesDownloader.NormalizedGithubRepository repository = BuildSourcesDownloader
                .normalizeGithubRepository("https://github.ibm.com/pnc-prod/apache-xmlgraphics-commons.git");

        assertEquals(
                URI.create(
                        "https://github.ibm.com/api/v3/repos/pnc-prod/apache-xmlgraphics-commons/compare/"
                                + "upstream-sha...aligned-sha"),
                BuildSourcesDownloader.compareDiffUri(repository, "upstream-sha", "aligned-sha"));
    }

    @Test
    void shouldBuildCompareDiffUriForPublicGithub() {
        BuildSourcesDownloader.NormalizedGithubRepository repository = BuildSourcesDownloader
                .normalizeGithubRepository("git@github.com:project-ncl/bacon.git");

        assertEquals(
                URI.create("https://api.github.com/repos/project-ncl/bacon/compare/base...head"),
                BuildSourcesDownloader.compareDiffUri(repository, "base", "head"));
    }

    @Test
    void shouldBuildCommitDiffAndRepoUrisForGithubEnterprise() {
        BuildSourcesDownloader.NormalizedGithubRepository repository = BuildSourcesDownloader
                .normalizeGithubRepository("git@github.ibm.com:pnc-prod/example.git");

        assertEquals(
                URI.create("https://github.ibm.com/api/v3/repos/pnc-prod/example/commits/aligned-sha"),
                BuildSourcesDownloader.commitDiffUri(repository, "aligned-sha"));
        assertEquals(
                URI.create("https://github.ibm.com/api/v3/repos/pnc-prod/example"),
                BuildSourcesDownloader.repoApiUri(repository));
    }

    @Test
    void shouldSelectInternalScmUrl() {
        Build build = mock(Build.class);
        SCMRepository repository = mock(SCMRepository.class);

        when(build.getScmRepository()).thenReturn(repository);
        when(repository.getInternalUrl()).thenReturn("https://github.ibm.com/pnc-prod/example.git");

        assertEquals(
                "https://github.ibm.com/pnc-prod/example.git",
                BuildSourcesDownloader.selectInternalScmUrl(build));
    }

    @Test
    void shouldPreferResolvedScmBuildConfigRevisionAsPreAlignmentBase() {
        Build build = mock(Build.class);
        BuildConfigurationRevisionRef revision = mock(BuildConfigurationRevisionRef.class);

        // scmBuildConfigRevision is the resolved commit reachable in the internal repository; the configured
        // buildConfigRevision.scmRevision is only an external branch/tag name fallback.
        when(build.getScmBuildConfigRevision()).thenReturn("4a066dcf4348b05c33566643f4db085eba211c10");
        when(build.getBuildConfigRevision()).thenReturn(revision);
        when(revision.getScmRevision()).thenReturn("LW-2026-9864/1.0M10/pipeline-18641434");

        assertEquals(
                List.of("4a066dcf4348b05c33566643f4db085eba211c10", "LW-2026-9864/1.0M10/pipeline-18641434"),
                BuildSourcesDownloader.preAlignmentRevisions(build));
    }

    @Test
    void shouldFallBackToConfiguredScmRevisionWhenNoResolvedBuildConfigRevision() {
        Build build = mock(Build.class);
        BuildConfigurationRevisionRef revision = mock(BuildConfigurationRevisionRef.class);

        when(build.getScmBuildConfigRevision()).thenReturn(null);
        when(build.getBuildConfigRevision()).thenReturn(revision);
        when(revision.getScmRevision()).thenReturn("upstream-revision");

        assertEquals(List.of("upstream-revision"), BuildSourcesDownloader.preAlignmentRevisions(build));
    }

    @Test
    void shouldSelectPostAlignmentRevisionsPreferringCommitOverTag() {
        Build build = mock(Build.class);

        when(build.getScmRevision()).thenReturn("aligned-sha");
        when(build.getScmTag()).thenReturn("the-tag");

        assertEquals(List.of("aligned-sha", "the-tag"), BuildSourcesDownloader.postAlignmentRevisions(build));
    }

    @Test
    void shouldFallBackToTagWhenPostAlignmentCommitMissing() {
        Build build = mock(Build.class);

        when(build.getScmRevision()).thenReturn(null);
        when(build.getScmTag()).thenReturn("the-tag");

        assertEquals(List.of("the-tag"), BuildSourcesDownloader.postAlignmentRevisions(build));
    }

    @Test
    void shouldDeriveMetadataSiblingFromMainDiffFile() {
        assertEquals(
                Path.of("/tmp/out/alignment-metadata.diff"),
                BuildSourcesDownloader.metadataSibling(Path.of("/tmp/out/alignment.diff")));
        assertEquals(
                Path.of("B123-alignment-metadata.diff"),
                BuildSourcesDownloader.metadataSibling(Path.of("B123-alignment.diff")));
    }
}
