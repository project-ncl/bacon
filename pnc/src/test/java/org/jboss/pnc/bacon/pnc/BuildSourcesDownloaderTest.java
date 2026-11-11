package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.jboss.pnc.dto.SCMRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        BuildSourcesDownloader.NormalizedScmRepository repository = BuildSourcesDownloader
                .normalizeScmRepository("https://github.ibm.com/pnc-prod/apache-xmlgraphics-commons.git");

        assertEquals(
                URI.create(
                        "https://github.ibm.com/api/v3/repos/pnc-prod/apache-xmlgraphics-commons/compare/"
                                + "upstream-sha...aligned-sha"),
                BuildSourcesDownloader.compareDiffUri(repository, "upstream-sha", "aligned-sha"));
    }

    @Test
    void shouldBuildCompareDiffUriForPublicGithub() {
        BuildSourcesDownloader.NormalizedScmRepository repository = BuildSourcesDownloader
                .normalizeScmRepository("git@github.com:project-ncl/bacon.git");

        assertEquals(
                URI.create("https://api.github.com/repos/project-ncl/bacon/compare/base...head"),
                BuildSourcesDownloader.compareDiffUri(repository, "base", "head"));
    }

    @Test
    void shouldBuildCommitDiffAndRepoUrisForGithubEnterprise() {
        BuildSourcesDownloader.NormalizedScmRepository repository = BuildSourcesDownloader
                .normalizeScmRepository("git@github.ibm.com:pnc-prod/example.git");

        assertEquals(
                URI.create("https://github.ibm.com/api/v3/repos/pnc-prod/example/commits/aligned-sha"),
                BuildSourcesDownloader.commitDiffUri(repository, "aligned-sha"));
        assertEquals(
                URI.create("https://github.ibm.com/api/v3/repos/pnc-prod/example"),
                BuildSourcesDownloader.repoApiUri(repository));
    }

    @Test
    void shouldTryTheBuildUrlThenTheCurrentInternalUrl() {
        Build build = mock(Build.class);
        SCMRepository repository = mock(SCMRepository.class);

        when(build.getScmUrl()).thenReturn("https://github.ibm.com/pnc-prod/example.git");
        when(build.getScmRepository()).thenReturn(repository);
        when(repository.getInternalUrl()).thenReturn("git@gitlab.cee.redhat.com:pnc-workspace/example.git");
        when(repository.getExternalUrl()).thenReturn("https://github.com/upstream/example.git");

        // the external repository does not have the aligned commits and tags
        assertEquals(
                List.of(
                        "https://github.ibm.com/pnc-prod/example.git",
                        "git@gitlab.cee.redhat.com:pnc-workspace/example.git"),
                BuildSourcesDownloader.scmUrlCandidates(build));

        when(repository.getInternalUrl()).thenReturn("https://github.ibm.com/pnc-prod/example.git");
        assertEquals(
                List.of("https://github.ibm.com/pnc-prod/example.git"),
                BuildSourcesDownloader.scmUrlCandidates(build));

        when(build.getScmUrl()).thenReturn(null);
        when(repository.getInternalUrl()).thenReturn(" ");
        assertEquals(List.of(), BuildSourcesDownloader.scmUrlCandidates(build));
    }

    @Test
    void shouldReportEveryScmUrlWhenNoneWorks(@TempDir Path tempDir) {
        // ".invalid" never resolves (RFC 6761)
        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> new BuildSourcesDownloader().downloadSources(
                        "B123",
                        List.of("https://first.invalid/org/repo.git", "https://second.invalid/org/repo.git"),
                        List.of("0123456789abcdef0123456789abcdef01234567"),
                        tempDir.resolve("sources.tar.gz")));

        assertTrue(e.getMessage().contains("from any of its SCM URLs"), e.getMessage());
        assertTrue(e.getMessage().contains("first.invalid"), e.getMessage());
        assertTrue(e.getMessage().contains("second.invalid"), e.getMessage());
        assertFalse(Files.exists(tempDir.resolve("sources.tar.gz")));
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
    void shouldDerivePartFilesNumberedInApplyOrder() {
        assertEquals(
                Path.of("/tmp/out/alignment-1-build.diff"),
                BuildSourcesDownloader.partFile(Path.of("/tmp/out/alignment.diff"), AlignmentDiff.Part.BUILD));
        assertEquals(
                Path.of("B123-alignment-4-noise.diff"),
                BuildSourcesDownloader.partFile(Path.of("B123-alignment.diff"), AlignmentDiff.Part.NOISE));
        assertEquals(
                Path.of("B123-alignment-manifest.json"),
                BuildSourcesDownloader.manifestFile(Path.of("B123-alignment.diff")));
    }

    private static final String POM_REMOVING_REPOSITORY = String.join(
            "\n",
            "diff --git a/pom.xml b/pom.xml",
            "index 1111111..2222222 100644",
            "--- a/pom.xml",
            "+++ b/pom.xml",
            "@@ -10,6 +10,1 @@",
            "   </build>",
            "-  <repositories>",
            "-    <repository>",
            "-      <id>jcenter</id>",
            "-      <url>https://jcenter.bintray.com</url>",
            "-    </repository>",
            "-  </repositories>",
            "diff --git a/extra.txt b/extra.txt",
            "new file mode 100644",
            "index 0000000..3333333",
            "--- /dev/null",
            "+++ b/extra.txt",
            "@@ -0,0 +1 @@",
            "+extra",
            "");

    @Test
    void shouldDescribeThePartAndItsCategoriesInComments() {
        AlignmentDiff diff = AlignmentDiff.split(POM_REMOVING_REPOSITORY);

        assertEquals(
                "# PNC alignment diff, part 3 of 4: reproduce (optional, makes the build match the PNC one exactly)\n"
                        + "# Project: unknown\n"
                        + "# Project version: no change found\n"
                        + "# Apply on top of the upstream sources in numeric order, alone or together.\n#\n",
                BuildSourcesDownloader.partHeader(AlignmentDiff.Part.REPRODUCE, diff));

        String header = BuildSourcesDownloader.categoryHeader(AlignmentDiff.Category.REPOSITORIES, diff);
        assertTrue(header.startsWith("# --- repositories ---\n# Contains:   Maven repositories removed"), header);
        assertTrue(header.contains("\n# Skip when:  when building outside PNC"), header);
        assertTrue(
                header.contains("\n# Removed:    jcenter https://jcenter.bintray.com (pom.xml, repository)\n"),
                header);
        assertTrue(header.endsWith("\n#\n"), header);
        for (String line : header.split("\n")) {
            assertTrue(line.startsWith("#") && line.length() <= 100, line);
        }

        String buildHeader = BuildSourcesDownloader.categoryHeader(AlignmentDiff.Category.BUILD, diff);
        assertTrue(buildHeader.contains("\n# Unknown:    extra.txt (added file not recognized"), buildHeader);
        assertFalse(buildHeader.contains("Removed:"), buildHeader);
    }

    @Test
    void shouldListTheProjectVersionChanges() {
        AlignmentDiff diff = AlignmentDiff.split(
                String.join(
                        "\n",
                        "diff --git a/manipulations.sbt b/manipulations.sbt",
                        "new file mode 100644",
                        "index 0000000..3333333",
                        "--- /dev/null",
                        "+++ b/manipulations.sbt",
                        "@@ -0,0 +1 @@",
                        "+version := \"3.1.0.redhat-00004\"",
                        ""));

        assertEquals(
                List.of("Project: unknown", "Project version: ? -> 3.1.0.redhat-00004"),
                BuildSourcesDownloader.projectLines(diff));
        // SMEG does not record the project: the one PNC named the build after is used.
        String diffText = String.join(
                "\n",
                "diff --git a/manipulations.sbt b/manipulations.sbt",
                "new file mode 100644",
                "index 0000000..3333333",
                "--- /dev/null",
                "+++ b/manipulations.sbt",
                "@@ -0,0 +1 @@",
                "+version := \"3.1.0.redhat-00004\"",
                "");
        assertEquals(
                List.of("Project: org.acme:widget_2.13", "Project version: ? -> 3.1.0.redhat-00004"),
                BuildSourcesDownloader
                        .projectLines(AlignmentDiff.split(diffText, List.of(), "org.acme:widget_2.13")));
    }

    @Test
    void shouldWriteOnlyThePartsWithChangesAndTheManifestWhenRequested(@TempDir Path dir) throws Exception {
        AlignmentDiff diff = AlignmentDiff.split(POM_REMOVING_REPOSITORY);

        assertEquals(
                List.of(dir.resolve("a-1-build.diff"), dir.resolve("a-3-reproduce.diff")),
                BuildSourcesDownloader.writeAlignmentDiffParts(diff, dir.resolve("a.diff"), false));
        assertFalse(Files.exists(dir.resolve("a-manifest.json")));

        assertEquals(
                dir.resolve("b-manifest.json"),
                BuildSourcesDownloader.writeAlignmentDiffParts(diff, dir.resolve("b.diff"), true).get(2));
        assertTrue(Files.readString(dir.resolve("b-manifest.json")).contains("\"file\" : \"b-3-reproduce.diff\""));

        assertEquals(
                List.of(),
                BuildSourcesDownloader.writeAlignmentDiffParts(AlignmentDiff.split(""), dir.resolve("c.diff"), false));
    }

    @Test
    void shouldDeriveAlignedVersionsFromScmTag() {
        assertEquals(
                List.of("1.0.0.M10-rhlw-00000-n-00012"),
                BuildSourcesDownloader.alignedVersionsFromTag("1.0.0.M10-rhlw-00000-n-00012"));
        // Reqour appends the commit when a tag with the version name already exists.
        assertEquals(
                List.of("1.70-f1459fdb", "1.70"),
                BuildSourcesDownloader.alignedVersionsFromTag("1.70-f1459fdb"));
        assertEquals(List.of(), BuildSourcesDownloader.alignedVersionsFromTag(null));
    }

    @Test
    void shouldStopTryingASourceHostThatCannotBeReached(@TempDir Path tempDir) {
        // ".invalid" never resolves (RFC 6761): the first candidate fails, the others are not attempted
        RuntimeException e = assertThrows(
                RuntimeException.class,
                () -> new BuildSourcesDownloader().downloadSources(
                        "B123",
                        "https://bacon-test.invalid/org/repo.git",
                        List.of("0123456789abcdef0123456789abcdef01234567"),
                        tempDir.resolve("sources.tar.gz")));

        assertTrue(e.getMessage().contains("skipped, bacon-test.invalid is not reachable"), e.getMessage());
        assertFalse(Files.exists(tempDir.resolve("sources.tar.gz")));
    }
}
