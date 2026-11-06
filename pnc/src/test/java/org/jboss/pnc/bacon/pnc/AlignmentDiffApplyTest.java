/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pnc;

import static org.jboss.pnc.bacon.pnc.AlignmentDiff.Category.BUILD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jboss.pnc.bacon.pnc.AlignmentDiff.Category;
import org.jboss.pnc.bacon.pnc.AlignmentDiff.Part;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Applies the parts of a real {@code git diff} of an alignment with {@code git apply}, to check that they rebuild the
 * aligned tree in any order and that every part but the build part can be skipped.
 */
class AlignmentDiffApplyTest {

    private static final String UPSTREAM = "upstream";
    private static final String ALIGNED = "aligned";

    @TempDir
    static Path workDir;

    private static Path repo;
    private static AlignmentDiff diff;

    @BeforeAll
    static void alignRepository() throws Exception {
        assumeTrue(gitAvailable(), "git is required to apply the diff parts");
        repo = Files.createDirectory(workDir.resolve("repo"));
        git("init", "-q");

        write("pom.xml", upstreamPom());
        write("build.gradle", gradle(false));
        write("README.md", lines("# Widget", padding("readme", 8), "Build with Maven.  "));
        write("script.sh", lines("#!/bin/sh", "echo start  ", padding("script", 8), "mvn install"));
        write("old-name.txt", lines("first line  ", padding("rename", 10), "upstream"));
        git("add", "-A");
        git("commit", "-q", "-m", UPSTREAM);
        git("tag", UPSTREAM);

        write("pom.xml", alignedPom());
        // GME: the injected apply line, the scripts and the model it reads back.
        write("build.gradle", gradle(true));
        write("gme.gradle", lines("// GME plugin wiring"));
        write("manipulation.json", lines("{\"version\": \"1.4.0.redhat-00002\"}"));
        write("target/pom-manip-ext-marker.txt", "");
        write("target/alignmentReport.json", lines("{\"executionRoot\": {\"version\": \"1.4.0.redhat-00002\"}}"));
        // Whitespace only.
        write("README.md", lines("# Widget", padding("readme", 8), "Build with Maven."));
        // A mode change with a formatting hunk and a build hunk: the mode header is repeated in both parts.
        write("script.sh", lines("#!/bin/sh", "echo start", padding("script", 8), "mvn install -Dpnc"));
        git("add", "-A");
        git("update-index", "--chmod=+x", "script.sh");
        // A rename with a formatting hunk and a build hunk.
        git("mv", "old-name.txt", "new-name.txt");
        write("new-name.txt", lines("first line", padding("rename", 10), "aligned"));
        git("add", "-A");
        git("commit", "-q", "-m", ALIGNED);
        git("tag", ALIGNED);

        diff = AlignmentDiff.split(git("diff", "--full-index", "-M", UPSTREAM, ALIGNED));
    }

    @Test
    void shouldProduceEveryCategory() {
        for (Category category : Category.values()) {
            assertTrue(diff.has(category), category + " part missing:\n" + diff.manifest());
        }
        assertTrue(diff.warnings().isEmpty(), diff.warnings().toString());
    }

    @Test
    void shouldRebuildTheAlignedTreeApplyingThePartFilesSortedByName() throws Exception {
        Path out = Files.createDirectories(workDir.resolve("out"));
        BuildSourcesDownloader.writeAlignmentDiffParts(diff, out.resolve("alignment.diff"), false);

        // Only the diff parts, no manifest.
        List<Path> parts;
        try (Stream<Path> files = Files.list(out)) {
            parts = files.sorted().collect(Collectors.toList());
        }
        assertEquals(
                List.of(
                        "alignment-1-build.diff",
                        "alignment-2-rerun-state.diff",
                        "alignment-3-reproduce.diff",
                        "alignment-4-noise.diff"),
                parts.stream().map(part -> part.getFileName().toString()).collect(Collectors.toList()));

        for (Part part : Part.values()) {
            String content = Files.readString(parts.get(part.ordinal()));
            assertTrue(content.startsWith("# PNC alignment diff, part " + part.number() + " of 4: "), content);
            assertTrue(
                    content.contains("\n# Project: unknown\n# Project version: 1.4.0 -> 1.4.0.redhat-00002\n"),
                    content);
            for (Category category : part.categories()) {
                assertTrue(content.contains("\n# --- " + category.id() + " ---\n"), content);
            }
        }

        // git apply ignores the comments, and applies a file present in several sections of a part (pom.xml in the
        // metadata and formatting sections) section by section.
        resetToUpstream();
        for (Path part : parts) {
            git("apply", part.toString());
        }
        assertAligned("sorted by name");
    }

    @Test
    void shouldRebuildTheAlignedTreeInAnyOrder() throws Exception {
        List<List<Category>> orders = new ArrayList<>();
        orders.add(Arrays.asList(Category.values()));
        List<Category> reversed = new ArrayList<>(Arrays.asList(Category.values()));
        Collections.reverse(reversed);
        orders.add(reversed);
        Random random = new Random(42);
        for (int i = 0; i < 20; i++) {
            List<Category> shuffled = new ArrayList<>(Arrays.asList(Category.values()));
            Collections.shuffle(shuffled, random);
            orders.add(shuffled);
        }

        for (List<Category> order : orders) {
            resetToUpstream();
            for (Category category : order) {
                apply(category);
            }
            assertAligned(order.toString());
        }
    }

    @Test
    void shouldApplyEverySubsetThatContainsTheBuildPart() throws Exception {
        List<Category> optional = Arrays.stream(Category.values())
                .filter(category -> category != BUILD)
                .collect(Collectors.toList());
        for (int mask = 0; mask < 1 << optional.size(); mask++) {
            int subset = mask;
            List<Category> applied = new ArrayList<>(List.of(BUILD));
            IntStream.range(0, optional.size())
                    .filter(i -> (subset & 1 << i) != 0)
                    .forEach(i -> applied.add(optional.get(i)));

            resetToUpstream();
            for (Category category : applied) {
                apply(category);
            }
        }
    }

    @Test
    void shouldListThePartsInApplyOrderInTheManifest() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) diff
                .manifest(part -> "alignment-" + part.suffix() + ".diff")
                .get("parts");

        assertEquals(
                List.of(
                        "build",
                        "rerun-state",
                        "repositories",
                        "rerun-blocker",
                        "metadata",
                        "formatting"),
                parts.stream().map(part -> part.get("category")).collect(Collectors.toList()));
        assertEquals(
                Map.of(
                        "category",
                        "rerun-state",
                        "part",
                        2,
                        "file",
                        "alignment-2-rerun-state.diff",
                        "present",
                        true,
                        "required",
                        true,
                        "contents",
                        Category.RERUN_STATE.contents(),
                        "neededFor",
                        Category.RERUN_STATE.neededFor(),
                        "skipWhen",
                        Category.RERUN_STATE.skip()),
                parts.get(1));
        assertEquals(
                List.of(true, true, false, false, false, false),
                parts.stream().map(part -> part.get("required")).collect(Collectors.toList()));
    }

    private static void apply(Category category) throws Exception {
        Path part = workDir.resolve("part-" + category.id() + ".diff");
        Files.writeString(part, diff.part(category), StandardCharsets.UTF_8);
        git("apply", part.toString());
    }

    private static void resetToUpstream() throws Exception {
        git("checkout", "-q", "-f", UPSTREAM);
        git("clean", "-q", "-f", "-d", "-x");
    }

    private static void assertAligned(String order) throws Exception {
        git("add", "-A");
        String remaining = git("diff", "--cached", "--stat", ALIGNED);
        assertEquals("", remaining, "Applying the parts in the order " + order + " differs from the aligned tree");
    }

    private static String upstreamPom() {
        return lines(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">",
                padding("pom-top", 8),
                "  <modelVersion>4.0.0</modelVersion>",
                "  <groupId>org.example</groupId>",
                "  <artifactId>widget</artifactId>",
                "  <version>1.4.0</version>",
                padding("pom-header", 8),
                "  <repositories>",
                "    <repository>",
                "      <id>jcenter</id>",
                "      <url>https://jcenter.bintray.com</url>",
                "    </repository>",
                "  </repositories>",
                "  <dependencies>") + dependencies(false) + lines("  </dependencies>", "</project>");
    }

    private static String alignedPom() {
        return lines(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xmlns=\"http://maven.apache.org/POM/4.0.0\">",
                padding("pom-top", 8),
                "  <modelVersion>4.0.0</modelVersion>",
                "  <groupId>org.example</groupId>",
                "  <artifactId>widget</artifactId>",
                "  <version>1.4.0.redhat-00002</version>",
                padding("pom-header", 8),
                "  <dependencies>") + dependencies(true)
                + lines(
                        "  </dependencies>",
                        "</project>",
                        "<!--",
                        "Modified by POM Manipulation Extension for Maven 5.10 ( SHA: abcdef12 )",
                        "-->");
    }

    /** Similar dependency blocks, so that the build hunk after the removed repositories has to be found by offset. */
    private static String dependencies(boolean aligned) {
        StringBuilder dependencies = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            dependencies.append(
                    lines(
                            "    <dependency>",
                            "      <groupId>org.example</groupId>",
                            "      <artifactId>dep-" + i + "</artifactId>",
                            "      <version>" + (aligned && i == 6 ? "2.0.redhat-00001" : "2.0") + "</version>",
                            "    </dependency>"));
        }
        return dependencies.toString();
    }

    private static String gradle(boolean aligned) {
        return lines("plugins {", "    id 'java'", "}") + padding("gradle", 1, 4)
                + (aligned ? lines("apply from: \"gme.gradle\"") : "") + padding("gradle", 5, 16)
                + lines(
                        "dependencies {",
                        "    implementation 'org.foo:bar:1.0" + (aligned ? ".redhat-00003'" : "'"),
                        "}");
    }

    private static String padding(String name, int count) {
        return padding(name, 1, count);
    }

    private static String padding(String name, int from, int to) {
        return IntStream.rangeClosed(from, to)
                .mapToObj(i -> "// " + name + " " + i + "\n")
                .collect(Collectors.joining());
    }

    private static String lines(String... lines) {
        return Arrays.stream(lines)
                .map(line -> line.endsWith("\n") ? line : line + "\n")
                .collect(Collectors.joining());
    }

    private static void write(String path, String content) throws IOException {
        Path file = repo.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static boolean gitAvailable() {
        try {
            return new ProcessBuilder("git", "--version").start().waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static String git(String... args) throws Exception {
        List<String> command = new ArrayList<>(
                List.of(
                        "git",
                        "-c",
                        "user.name=Test",
                        "-c",
                        "user.email=test@example.com",
                        "-c",
                        "commit.gpgsign=false",
                        "-c",
                        "tag.gpgsign=false",
                        "-c",
                        "core.autocrlf=false",
                        "-c",
                        "core.fileMode=true"));
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder = new ProcessBuilder(command).directory(repo.toFile()).redirectErrorStream(true);
        builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
        builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new AssertionError(String.join(" ", args) + " failed (" + exit + "):\n" + output);
        }
        return output;
    }
}
