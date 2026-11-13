package org.jboss.pnc.bacon.pnc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Rebuilds an alignment diff the way bacon does from GitLab's API, with the content GitLab leaves out (binary files,
 * files over its diff size limit) filled in, and checks with {@code git apply} that it recreates the aligned tree
 * exactly, also after splitting it into parts.
 */
class GitlabFilledDiffTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    /** gitlab.cee leaves out per-file patches over ~200 KiB. */
    private static final int GITLAB_PATCH_LIMIT = 200 * 1024;

    @TempDir
    Path tmp;

    private Path repo;
    private String base;
    private String head;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(gitAvailable(), "git is required");
        repo = tmp.resolve("repo");
        Files.createDirectories(repo);
        git(repo, "init", "-q", "-b", "main");

        // upstream sources
        write("build.gradle", "plugins { id 'java' }\nversion = '1.70'\n");
        write("pom.xml", "<project>\n<version>1.70</version>\n</project>\n");
        write("big-modified.txt", lines("upstream line ", 9000));
        Files.write(repo.resolve("lib/old.jar"), randomBytes(3000, 1), createParents("lib/old.jar"));
        Files.write(repo.resolve("lib/changed.bin"), randomBytes(2000, 2));
        write("obsolete.txt", "to delete\n");
        commit("upstream");
        base = git(repo, "rev-parse", "HEAD");

        // what alignment and the groovy manipulators add or change
        write("build.gradle", "plugins { id 'java' }\nversion = '1.70.0.redhat-00001'\napply from: 'gme.gradle'\n");
        write("gme.gradle", "// GME\nallprojects { }\n");
        write("manipulation.json", "{\n  \"version\": \"1.70.0.redhat-00001\"\n}\n");
        write(
                "groovy/fix-stampversion-gme.groovy",
                "@InvocationPoint(invocationPoint = InvocationStage.LAST)\nprintln 'x'");
        write("groovy/allow-insecure-repos-gme.groovy", "println 'é ü – unicode'\r\nprintln 'crlf'\r\n");
        write("target/pom-manip-ext-marker.txt", "");
        // file names git writes quoted (non-ASCII) or that contain a space
        write("docs/na\u00efve.txt", "na\u00efve\n");
        write("docs/with space.txt", "spaced\n");
        write("generated/SqlLexer.java", lines("// generated lexer line ", 12000)); // > 200 KiB, new text file
        write("big-modified.txt", lines("aligned line ", 9000)); // > 200 KiB patch, modified text file
        Files.write(repo.resolve("lib/new.jar"), randomBytes(5000, 3)); // new binary
        Files.write(repo.resolve("lib/changed.bin"), randomBytes(2100, 4)); // modified binary
        Files.delete(repo.resolve("lib/old.jar")); // deleted binary
        Files.delete(repo.resolve("obsolete.txt"));
        commit("Reqour");
        head = git(repo, "rev-parse", "HEAD");
    }

    @Test
    void filledDiffRecreatesTheAlignedTree() throws Exception {
        GitlabSources.UnifiedDiff diff = GitlabSources
                .toUnifiedDiff(gitlabLikeDiffs(), this::fetch, base, head);

        assertThat(diff.incompleteFiles).isEmpty();
        assertThat(diff.content).contains("GIT binary patch")
                .contains(
                        "diff --git a/target/pom-manip-ext-marker.txt b/target/pom-manip-ext-marker.txt\nnew file mode")
                .contains("+@InvocationPoint(invocationPoint = InvocationStage.LAST)")
                .contains("+// generated lexer line 11999");
        assertAppliesToAlignedTree(List.of(diff.content));
    }

    @Test
    void filledDiffSurvivesMaskingAndSplitting() throws Exception {
        GitlabSources.UnifiedDiff diff = GitlabSources
                .toUnifiedDiff(gitlabLikeDiffs(), this::fetch, base, head);

        String masked = BuildSourcesDownloader.maskSecrets(diff.content);
        List<Path> parts = BuildSourcesDownloader.writeAlignmentDiffParts(
                AlignmentDiff.split(masked, List.of("1.70.0.redhat-00001"), "org.bouncycastle:bcprov"),
                tmp.resolve("out/alignment.diff"),
                false);

        List<String> contents = new ArrayList<>();
        for (Path part : parts) {
            contents.add(Files.readString(part));
        }
        assertAppliesToAlignedTree(contents);
    }

    @Test
    void withoutFetcherTheLeftOutFilesAreReported() throws Exception {
        GitlabSources.UnifiedDiff diff = GitlabSources.toUnifiedDiff(gitlabLikeDiffs());

        assertThat(diff.incompleteFiles).containsExactlyInAnyOrder(
                "generated/SqlLexer.java",
                "big-modified.txt",
                "lib/new.jar",
                "lib/changed.bin",
                "lib/old.jar");
    }

    @Test
    void literalsMatchGit() {
        // as written by 'git diff --binary'
        assertThat(GitPatchContent.literal(new byte[0])).isEqualTo("literal 0\nHcmV?d00001\n");
        assertThat(GitPatchContent.literal(new byte[] { 0, 1, 2 })).isEqualTo("literal 3\nKcmZQzWC8#H2LJ>B\n");
    }

    @Test
    void pathsAreQuotedLikeGit() throws Exception {
        for (String path : git(repo, "-c", "core.quotePath=false", "diff", "--name-only", base, head).split("\n")) {
            String quotedByGit = git(repo, "diff", "--name-only", base, head, "--", path);
            assertThat(GitPatchContent.quotePath(path)).as(path).isEqualTo(quotedByGit);
        }
        assertThat(GitPatchContent.quotePath("a/docs/na\u00efve.txt")).isEqualTo("\"a/docs/na\\303\\257ve.txt\"");
        assertThat(GitPatchContent.quotePath("b/docs/with space.txt")).isEqualTo("b/docs/with space.txt");
    }

    @Test
    void blobIdMatchesGit() throws Exception {
        byte[] content = randomBytes(777, 9);
        Files.write(tmp.resolve("blob"), content);
        assertThat(GitPatchContent.blobId(content)).isEqualTo(git(tmp, "hash-object", tmp.resolve("blob").toString()));
    }

    /** Applies the diff parts in order on the upstream sources and compares the result with the aligned tree. */
    private void assertAppliesToAlignedTree(List<String> parts) throws Exception {
        Path work = tmp.resolve("apply");
        git(tmp, "clone", "-q", repo.toString(), work.toString());
        git(work, "checkout", "-q", base);
        for (int i = 0; i < parts.size(); i++) {
            Path patch = tmp.resolve("part-" + i + ".diff");
            Files.writeString(patch, parts.get(i));
            git(work, "apply", "--whitespace=nowarn", patch.toString());
        }
        git(work, "add", "-A");
        assertThat(git(work, "write-tree")).isEqualTo(git(repo, "rev-parse", head + "^{tree}"));
    }

    /**
     * The diff entries as gitlab.cee returns them: hunks for text files under the size limit, "Binary files ... differ"
     * for binary files, and an empty diff with too_large for larger patches.
     */
    private ArrayNode gitlabLikeDiffs() throws Exception {
        ArrayNode diffs = JSON.createArrayNode();
        // raw names, as in GitLab's JSON
        for (String line : git(repo, "-c", "core.quotePath=false", "diff", "--no-renames", "--name-status", base, head)
                .split("\n")) {
            String status = line.substring(0, 1);
            String path = line.substring(line.indexOf('\t') + 1);
            ObjectNode entry = diffs.addObject();
            entry.put("old_path", path);
            entry.put("new_path", path);
            entry.put("new_file", status.equals("A"));
            entry.put("deleted_file", status.equals("D"));
            entry.put("renamed_file", false);
            entry.put("a_mode", status.equals("A") ? "0" : "100644");
            entry.put("b_mode", status.equals("D") ? "0" : "100644");

            String patch = git(repo, "-c", "core.quotePath=false", "diff", "--no-renames", base, head, "--", path)
                    + "\n";
            int hunk = patch.indexOf("\n@@");
            boolean binary = patch.contains("\nBinary files ");
            if (binary) {
                entry.put("diff", patch.substring(patch.indexOf("Binary files ")));
                entry.put("too_large", false);
            } else if (hunk < 0) {
                entry.put("diff", "");
                entry.put("too_large", false);
            } else if (patch.length() - hunk > GITLAB_PATCH_LIMIT) {
                entry.put("diff", "");
                entry.put("too_large", true);
            } else {
                entry.put("diff", patch.substring(hunk + 1));
                entry.put("too_large", false);
            }
        }
        return diffs;
    }

    private byte[] fetch(String path, String ref) {
        try {
            Process process = new ProcessBuilder("git", "show", ref + ":" + path).directory(repo.toFile()).start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = process.getInputStream()) {
                in.transferTo(out);
            }
            return process.waitFor() == 0 ? out.toByteArray() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void write(String path, String content) throws Exception {
        Path file = repo.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private java.nio.file.OpenOption createParents(String path) throws Exception {
        Files.createDirectories(repo.resolve(path).getParent());
        return java.nio.file.StandardOpenOption.CREATE;
    }

    private void commit(String message) throws Exception {
        git(repo, "add", "-A");
        git(repo, "-c", "user.name=t", "-c", "user.email=t@example.com", "commit", "-q", "-m", message);
    }

    private static String lines(String prefix, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append(prefix).append(i).append('\n');
        }
        return text.toString();
    }

    private static byte[] randomBytes(int size, long seed) {
        byte[] bytes = new byte[size];
        new Random(seed).nextBytes(bytes);
        bytes[0] = 0; // make sure git sees it as binary
        return bytes;
    }

    private static boolean gitAvailable() {
        try {
            return new ProcessBuilder("git", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String git(Path dir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed: " + output);
        }
        return output.endsWith("\n") ? output.substring(0, output.length() - 1) : output;
    }
}
