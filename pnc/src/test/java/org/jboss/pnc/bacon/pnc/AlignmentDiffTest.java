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
import static org.jboss.pnc.bacon.pnc.AlignmentDiff.Category.FORMATTING;
import static org.jboss.pnc.bacon.pnc.AlignmentDiff.Category.METADATA;
import static org.jboss.pnc.bacon.pnc.AlignmentDiff.Category.REPOSITORIES;
import static org.jboss.pnc.bacon.pnc.AlignmentDiff.Category.RERUN_BLOCKER;
import static org.jboss.pnc.bacon.pnc.AlignmentDiff.Category.RERUN_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class AlignmentDiffTest {

    private static final String POM_SECTION = lines(
            "diff --git a/pom.xml b/pom.xml",
            "index 1111111..2222222 100644",
            "--- a/pom.xml",
            "+++ b/pom.xml",
            "@@ -1,1 +1,1 @@",
            "-<version>1.0.0</version>",
            "+<version>1.0.0.redhat-00001</version>");

    // A pom.xml as PME rewrites it: re-serialized project header, version alignment, removed repositories.
    private static final String POM_HEADER = lines(
            "diff --git a/core/pom.xml b/core/pom.xml",
            "index 32cdeff..c283fc0 100644",
            "--- a/core/pom.xml",
            "+++ b/core/pom.xml");
    private static final String FORMATTING_HUNK = lines(
            "@@ -18,8 +18,7 @@",
            "   under the License.",
            "   -->",
            " ",
            "-<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "-         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">",
            "+<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                    + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">",
            " ",
            "   <modelVersion>4.0.0</modelVersion>",
            "   <parent>");
    private static final String VERSION_HUNK = lines(
            "@@ -30,3 +29,3 @@",
            "   <artifactId>widget</artifactId>",
            "-  <version>1.4.0</version>",
            "+  <version>1.4.0.redhat-00002</version>",
            "   <packaging>jar</packaging>");
    private static final String REPOSITORIES_HUNK = lines(
            "@@ -50,7 +49,1 @@",
            "   </build>",
            "-  <repositories>",
            "-    <repository>",
            "-      <id>jcenter</id>",
            "-      <url>https://jcenter.bintray.com</url>",
            "-    </repository>",
            "-  </repositories>");

    @Test
    void shouldReturnEmptyPartsForNullOrEmpty() {
        for (AlignmentDiff.Category category : AlignmentDiff.Category.values()) {
            assertEquals("", AlignmentDiff.split(null).part(category));
            assertFalse(AlignmentDiff.split("").has(category));
        }
    }

    @Test
    void shouldKeepEverythingAsBuildAffectingWhenNothingElseIsFound() {
        AlignmentDiff diff = AlignmentDiff.split(POM_SECTION);

        assertEquals(POM_SECTION, diff.part(BUILD));
        assertEquals(List.of(BUILD), presentCategories(diff));
    }

    @Test
    void shouldRoutePmeMarkerToRerunBlocker() {
        assertEquals(
                List.of(RERUN_BLOCKER),
                presentCategories(AlignmentDiff.split(newFile("target/pom-manip-ext-marker.txt", "+x"))));
    }

    @Test
    void shouldRouteManipulatorStateToRerunState() {
        for (String path : List.of(
                "manipulation.json",
                "gme.gradle",
                "gme-pluginconfigs.gradle",
                "gradle/gme-repos.gradle",
                "manipulations.sbt")) {
            assertEquals(List.of(RERUN_STATE), presentCategories(AlignmentDiff.split(newFile(path, "+x"))), path);
        }
    }

    @Test
    void shouldRouteReportsAndBackupsToMetadata() {
        for (String path : List.of(
                // PME writes its manipulation model under target/; only GME reads one back.
                "target/manipulation.json",
                "target/alignmentReport.json",
                "alignmentReport.json",
                "manipulations.json",
                "discovery-report.json",
                "repositories-backup.xml",
                // Written by Reqour for npm builds.
                "results")) {
            assertEquals(List.of(METADATA), presentCategories(AlignmentDiff.split(newFile(path, "+{}"))), path);
        }
        assertEquals(
                List.of(METADATA),
                presentCategories(AlignmentDiff.split(newFile("settings.xml", "+<id>removed-by-pme</id>"))));
        // A settings.xml without the PME backup profile is not a backup, nor a "results" file below the root.
        assertEquals(
                List.of(BUILD),
                presentCategories(AlignmentDiff.split(newFile("settings.xml", "+<id>central</id>"))));
        assertEquals(List.of(BUILD), presentCategories(AlignmentDiff.split(newFile("src/test/results", "+x"))));
    }

    @Test
    void shouldKeepEmptyMarkerFileSectionIntact() {
        // The marker is an empty file: no hunk at all.
        String marker = lines(
                "diff --git a/dom4j/target/pom-manip-ext-marker.txt b/dom4j/target/pom-manip-ext-marker.txt",
                "new file mode 100644",
                "index 00000000..e69de29b");

        AlignmentDiff diff = AlignmentDiff.split(POM_SECTION + marker);

        assertEquals(POM_SECTION, diff.part(BUILD));
        assertEquals(marker, diff.part(RERUN_BLOCKER));
    }

    @Test
    void shouldSplitHunksOfOneFileAndRecomputeTheirNewStart() {
        AlignmentDiff diff = AlignmentDiff.split(POM_HEADER + FORMATTING_HUNK + VERSION_HUNK + REPOSITORIES_HUNK);

        assertEquals(List.of(BUILD, REPOSITORIES, FORMATTING), presentCategories(diff));
        // The first hunk does not move.
        assertEquals(POM_HEADER + FORMATTING_HUNK, diff.part(FORMATTING));
        // Without the formatting hunk (-1 line) before them, the other hunks start on the same line in old and new.
        assertEquals(POM_HEADER + VERSION_HUNK.replace("+29,3", "+30,3"), diff.part(BUILD));
        assertEquals(POM_HEADER + REPOSITORIES_HUNK.replace("+49,1", "+50,1"), diff.part(REPOSITORIES));
    }

    @Test
    void shouldKeepShiftOfPreviousHunksInTheSamePart() {
        String repositories = lines(
                "@@ -10,7 +10,1 @@",
                "   </build>",
                "-  <repositories>",
                "-    <repository>",
                "-      <id>jcenter</id>",
                "-      <url>https://jcenter.bintray.com</url>",
                "-    </repository>",
                "-  </repositories>");
        String addedDependency = lines(
                "@@ -30,3 +24,4 @@",
                "   <dependencies>",
                "+    <dependency><groupId>g</groupId><artifactId>a</artifactId></dependency>",
                "     <dependency>",
                "       <groupId>x</groupId>");
        String version = lines(
                "@@ -60,3 +55,3 @@",
                "   <dependency>",
                "-    <version>2.0</version>",
                "+    <version>2.0.redhat-00001</version>");

        AlignmentDiff diff = AlignmentDiff.split(POM_HEADER + repositories + addedDependency + version);

        // The repositories hunk (-6 lines) no longer shifts the build hunks, the added dependency (+1 line) still does.
        assertEquals(
                POM_HEADER + addedDependency.replace("+24,4", "+30,4") + version.replace("+55,3", "+61,3"),
                diff.part(BUILD));
        assertEquals(POM_HEADER + repositories, diff.part(REPOSITORIES));
    }

    @Test
    void shouldNotTreatDeployOrScmRepositoryAsRemovedRepository() {
        String distributionManagement = lines(
                "@@ -70,8 +70,4 @@",
                "   <distributionManagement>",
                "-    <repository>",
                "-      <id>releases</id>",
                "-      <url>https://example.com/releases</url>",
                "-    </repository>",
                "     <snapshotRepository>",
                "       <id>snapshots</id>",
                "     </snapshotRepository>");
        // Maven 1 project.xml: <repository> holds the SCM coordinates.
        String mavenOneScm = lines(
                "diff --git a/project.xml b/project.xml",
                "--- a/project.xml",
                "+++ b/project.xml",
                "@@ -10,4 +10,0 @@",
                "-  <repository>",
                "-    <connection>scm:cvs:pserver:anonymous@cvs.example.com:/cvsroot/x</connection>",
                "-  </repository>",
                "-");

        AlignmentDiff diff = AlignmentDiff.split(POM_HEADER + distributionManagement + mavenOneScm);

        assertEquals(List.of(BUILD), presentCategories(diff));
    }

    @Test
    void shouldRouteRemovedPluginRepositoryWithinContextToRepositories() {
        String hunk = lines(
                "@@ -80,6 +80,2 @@",
                "   <pluginRepositories>",
                "-    <pluginRepository>",
                "-      <id>old</id>",
                "-      <url>https://example.com/old</url>",
                "-    </pluginRepository>",
                "   </pluginRepositories>");

        assertEquals(List.of(REPOSITORIES), presentCategories(AlignmentDiff.split(POM_HEADER + hunk)));
    }

    @Test
    void shouldRouteGmeInjectionsToRerunState() {
        String buildGradle = lines(
                "diff --git a/build.gradle b/build.gradle",
                "--- a/build.gradle",
                "+++ b/build.gradle",
                "@@ -1,3 +1,3 @@",
                "-buildscript {",
                "+buildscript { apply from: \"gme.gradle\"",
                "     repositories {",
                "         mavenCentral()",
                "@@ -40,2 +40,3 @@",
                " }",
                " ",
                "+apply from: \"gme-pluginconfigs.gradle\"");
        String buildGradleKts = lines(
                "diff --git a/build.gradle.kts b/build.gradle.kts",
                "--- a/build.gradle.kts",
                "+++ b/build.gradle.kts",
                "@@ -0,0 +1 @@",
                "+buildscript { project.apply { from(\"gme.gradle\") } }");
        String extraGradle = lines(
                "diff --git a/gradle/publishing.gradle b/gradle/publishing.gradle",
                "--- a/gradle/publishing.gradle",
                "+++ b/gradle/publishing.gradle",
                "@@ -1,2 +1,3 @@",
                "+buildscript { apply from: new File((gradle.getParent() == null ? gradle : gradle.getParent())"
                        + ".startParameter.getCurrentDir(), \"gradle/gme-repos.gradle\"), to: buildscript }",
                " apply plugin: 'maven-publish'",
                " ");

        AlignmentDiff diff = AlignmentDiff.split(
                buildGradle + buildGradleKts + extraGradle + newFile("gme.gradle", "+// gme")
                        + newFile("manipulation.json", "+{}"));

        assertEquals(List.of(RERUN_STATE), presentCategories(diff));
        assertEquals(List.of(), diff.warnings());
    }

    @Test
    void shouldKeepAllGmeStateInBuildWhenAnInjectionIsNotRecognized() {
        String buildGradle = lines(
                "diff --git a/build.gradle b/build.gradle",
                "--- a/build.gradle",
                "+++ b/build.gradle",
                "@@ -1,2 +1,2 @@",
                "-buildscript { ext.kotlinVersion = '1.9.0'",
                "+buildscript { apply from: \"gme.gradle\" ext.kotlinVersion = '1.9.0.redhat-00001'",
                "     repositories {");

        AlignmentDiff diff = AlignmentDiff
                .split(buildGradle + newFile("gme.gradle", "+// gme") + newFile("manipulation.json", "+{}"));

        // Leaving gme.gradle or manipulation.json out would break a build still applying gme.gradle.
        assertEquals(List.of(BUILD), presentCategories(diff));
        assertEquals(1, diff.warnings().size());
        assertTrue(diff.warnings().get(0).contains("build.gradle"));
        assertEquals(
                List.of(new AlignmentDiff.Change(BUILD, AlignmentDiff.RULE_GME_WIRING)),
                diff.files().get(1).changes());
    }

    @Test
    void shouldRouteWhitespaceOnlyHunkToFormatting() {
        String hunk = lines("@@ -86,3 +86,3 @@", "   </mailingLists>", "-  <build/>", "+  <build />", " ");

        assertEquals(List.of(FORMATTING), presentCategories(AlignmentDiff.split(POM_HEADER + hunk)));
    }

    @Test
    void shouldRouteReorderedXmlAttributesToFormatting() {
        String reordered = lines(
                "@@ -2,4 +2,2 @@",
                "-<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
                "-         xmlns=\"http://maven.apache.org/POM/4.0.0\"",
                "-         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
                "+<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
                "   <modelVersion>4.0.0</modelVersion>");
        String changedValue = lines(
                "@@ -40,1 +38,1 @@",
                "-  <plugin combine.self=\"override\" inherited=\"true\">",
                "+  <plugin inherited=\"false\" combine.self=\"override\">");

        AlignmentDiff diff = AlignmentDiff.split(POM_HEADER + reordered + changedValue);

        assertEquals(POM_HEADER + reordered, diff.part(FORMATTING));
        assertEquals(POM_HEADER + changedValue.replace("+38,1", "+40,1"), diff.part(BUILD));
    }

    @Test
    void shouldKeepProjectVersionRewritesInBuildUsingTheAlignmentReport() {
        String aligned = "1.0.0.M10-rhlw-00000-n-00012";
        String rootVersion = lines(
                "@@ -25,3 +25,3 @@",
                "     <artifactId>woden</artifactId>",
                "-    <version>1.0M10</version>",
                "+    <version>" + aligned + "</version>",
                "     <packaging>pom</packaging>");
        String moduleReference = lines(
                "@@ -140,5 +140,5 @@",
                "         <dependency>",
                "             <artifactId>woden-core</artifactId>",
                "-            <version>${project.version}</version>",
                "+            <version>" + aligned + "</version>",
                "         </dependency>");
        String dependencyOverride = lines(
                "@@ -300,1 +300,1 @@",
                "-    <version.org.freemarker>2.3.34</version.org.freemarker>",
                "+    <version.org.freemarker>2.3.32</version.org.freemarker>");
        String report = newFile(
                "target/alignmentReport.json",
                "+{ \"executionRoot\" : { \"version\" : \"" + aligned + "\", \"originalGAV\" : \"g:a:1.0M10\" } }");

        AlignmentDiff diff = AlignmentDiff
                .split(POM_HEADER + rootVersion + moduleReference + dependencyOverride + report);

        assertEquals(java.util.Set.of(aligned), diff.alignedVersions());
        // The project version rewrites are part of the alignment, recognized by their rule only.
        assertEquals(POM_HEADER + rootVersion + moduleReference + dependencyOverride, diff.part(BUILD));
        assertEquals(
                List.of(
                        AlignmentDiff.RULE_ALIGNED_VERSION,
                        AlignmentDiff.RULE_ALIGNED_VERSION,
                        AlignmentDiff.RULE_ALIGNMENT),
                diff.files().get(0).changes().stream().map(AlignmentDiff.Change::rule).collect(Collectors.toList()));
        assertEquals(report, diff.part(METADATA));
        assertEquals(
                List.of(new AlignmentDiff.ProjectVersion("1.0M10", aligned, List.of("core/pom.xml"))),
                diff.projectVersions());
    }

    @Test
    void shouldKeepPropertiesHoldingTheProjectVersionWithTheirVersionRewrites() {
        // A property referencing the sibling modules is aligned with the other properties: applying the build part
        // alone must build the modules with the version they reference.
        String aligned = "7.123.1.redhat-00003";
        String parentVersion = lines(
                "@@ -17,3 +17,3 @@",
                "     <parent>",
                "-        <version>7.123.1-SNAPSHOT</version>",
                "+        <version>" + aligned + "</version>",
                "     </parent>");
        String properties = lines(
                "@@ -120,4 +120,4 @@",
                "-        <io.vertx.version>4.5.30</io.vertx.version>",
                "-        <che.version>7.123.1-SNAPSHOT</che.version>",
                "+        <io.vertx.version>4.5.30.redhat-00001</io.vertx.version>",
                "+        <che.version>" + aligned + "</che.version>");

        AlignmentDiff diff = AlignmentDiff.split(POM_HEADER + parentVersion + properties, List.of(aligned));

        assertEquals(List.of(BUILD), presentCategories(diff));
        assertEquals(POM_HEADER + parentVersion + properties, diff.part(BUILD));
        assertEquals(
                List.of(new AlignmentDiff.ProjectVersion("7.123.1-SNAPSHOT", aligned, List.of("core/pom.xml"))),
                diff.projectVersions());
    }

    @Test
    void shouldUseKnownAlignedVersionsWithPncSuffixOnly() {
        assertEquals(
                1,
                AlignmentDiff.split(POM_HEADER + VERSION_HUNK, List.of("1.4.0.redhat-00002")).projectVersions().size());
        // Without a report or a known version the rewrite is not recognized.
        assertEquals(List.of(), AlignmentDiff.split(POM_HEADER + VERSION_HUNK).projectVersions());

        String unsuffixed = lines("@@ -30,1 +30,1 @@", "-  <version>1.4.0</version>", "+  <version>1.4.1</version>");
        assertEquals(List.of(), AlignmentDiff.split(POM_HEADER + unsuffixed, List.of("1.4.1")).projectVersions());
    }

    @Test
    void shouldFindTheProjectVersionChangeOfPme() {
        AlignmentDiff diff = AlignmentDiff.split(
                POM_HEADER + VERSION_HUNK + POM_HEADER.replace("core/pom.xml", "api/pom.xml") + VERSION_HUNK,
                List.of("1.4.0.redhat-00002"));

        assertEquals(
                List.of(
                        new AlignmentDiff.ProjectVersion(
                                "1.4.0",
                                "1.4.0.redhat-00002",
                                List.of("core/pom.xml", "api/pom.xml"))),
                diff.projectVersions());
    }

    @Test
    void shouldFindTheProjectVersionChangeOfGme() {
        String model = newFile(
                "manipulation.json",
                "+{\"version\": \"1.85.0.redhat-00001\", \"originalVersion\": \"1.85\", \"children\": "
                        + "{\"core\": {\"version\": \"1.85.0.redhat-00001\", \"originalVersion\": \"1.85\"}}}");
        // An unchanged version is no change.
        String unchanged = newFile(
                "sub/manipulation.json",
                "+{\"version\": \"2.0\", \"originalVersion\": \"2.0\"}");

        assertEquals(
                List.of(new AlignmentDiff.ProjectVersion("1.85", "1.85.0.redhat-00001", List.of("manipulation.json"))),
                AlignmentDiff.split(model + unchanged).projectVersions());
    }

    @Test
    void shouldFindTheProjectVersionChangeOfSmeg() {
        assertEquals(
                List.of(new AlignmentDiff.ProjectVersion(null, "3.1.0.redhat-00004", List.of("manipulations.sbt"))),
                AlignmentDiff.split(newFile("manipulations.sbt", "+version := \"3.1.0.redhat-00004\""))
                        .projectVersions());
    }

    @Test
    void shouldRoutePmeStampToMetadata() {
        String stamp = lines(
                "@@ -272,3 +257,6 @@",
                "         <apache.ant.version>1.7.1</apache.ant.version>",
                "     </properties>",
                " </project>",
                "+<!--",
                "+Modified by POM Manipulation Extension for Maven 5.10 ( SHA: ff6cf971c8a1da22fc5bc801f3ee439ae300adc3 )",
                "+-->");

        AlignmentDiff diff = AlignmentDiff.split(POM_HEADER + stamp);

        assertEquals(List.of(METADATA), presentCategories(diff));
        assertEquals(AlignmentDiff.RULE_PME_STAMP, diff.files().get(0).changes().get(0).rule());
    }

    @Test
    void shouldListRemovedRepositories() {
        // PME may remove only the repositories and keep their (now empty) container.
        String keptContainer = lines(
                "@@ -200,14 +200,7 @@",
                "     <repositories>",
                "-        <repository>",
                "-            <id>apache.snapshots</id>",
                "-            <url>http://repository.apache.org/snapshots</url>",
                "-        </repository>",
                "+        ",
                "     </repositories>");

        AlignmentDiff diff = AlignmentDiff
                .split(POM_HEADER + keptContainer + REPOSITORIES_HUNK.replace("-50,7", "-250,7"));

        assertEquals(
                List.of(
                        new AlignmentDiff.Repository(
                                "apache.snapshots",
                                "http://repository.apache.org/snapshots",
                                "core/pom.xml"),
                        new AlignmentDiff.Repository("jcenter", "https://jcenter.bintray.com", "core/pom.xml")),
                diff.removedRepositories());
    }

    @Test
    void shouldDescribeTheSplitInTheManifest() {
        // dom4j: the alignment generated a pom.xml for a Maven 1 project, which is needed by the build.
        String generatedPom = newFile("dom4j/pom.xml", "+<project/>");
        String marker = lines(
                "diff --git a/dom4j/target/pom-manip-ext-marker.txt b/dom4j/target/pom-manip-ext-marker.txt",
                "new file mode 100644",
                "index 00000000..e69de29b");

        AlignmentDiff diff = AlignmentDiff.split(generatedPom + marker + POM_HEADER + FORMATTING_HUNK + VERSION_HUNK);

        assertEquals(List.of("dom4j/pom.xml"), diff.unrecognizedAddedFiles());
        java.util.Map<String, Object> manifest = diff.manifest();
        assertEquals(
                List.of(
                        "alignedVersions",
                        "parts",
                        "summary",
                        "files",
                        "removedRepositories",
                        "projectVersions",
                        "unrecognizedAddedFiles",
                        "warnings"),
                List.copyOf(manifest.keySet()));
        List<?> files = (List<?>) manifest.get("files");
        assertEquals(3, files.size());
        assertEquals(
                java.util.Map.of(
                        "path",
                        "core/pom.xml",
                        "status",
                        "modified",
                        "changes",
                        List.of(
                                java.util.Map.of("category", "formatting", "rule", AlignmentDiff.RULE_WHITESPACE),
                                java.util.Map.of("category", "build", "rule", AlignmentDiff.RULE_ALIGNMENT))),
                files.get(2));
        assertEquals(
                java.util.Map.of("files", 1, "changes", 1L),
                ((java.util.Map<?, ?>) manifest.get("summary")).get("rerun-blocker"));
    }

    @Test
    void shouldKeepEveryLineInExactlyOnePart() {
        String unifiedDiff = POM_HEADER + FORMATTING_HUNK + VERSION_HUNK + REPOSITORIES_HUNK
                + newFile("target/manipulation.json", "+{}") + newFile("alignmentReport.json", "+{}") + POM_SECTION;

        AlignmentDiff diff = AlignmentDiff.split(unifiedDiff);

        List<String> changed = changedLines(unifiedDiff);
        List<String> changedInParts = Arrays.stream(AlignmentDiff.Category.values())
                .flatMap(category -> changedLines(diff.part(category)).stream())
                .collect(Collectors.toList());
        assertEquals(changed.size(), changedInParts.size());
        assertTrue(changedInParts.containsAll(changed));
    }

    private static List<AlignmentDiff.Category> presentCategories(AlignmentDiff diff) {
        return Arrays.stream(AlignmentDiff.Category.values()).filter(diff::has).collect(Collectors.toList());
    }

    private static List<String> changedLines(String diff) {
        return Arrays.stream(diff.split("\n"))
                .filter(
                        line -> (line.startsWith("+") || line.startsWith("-")) && !line.startsWith("+++")
                                && !line.startsWith("---"))
                .collect(Collectors.toList());
    }

    private static String newFile(String path, String addedLine) {
        return lines(
                "diff --git a/" + path + " b/" + path,
                "new file mode 100644",
                "index 0000000..3333333",
                "--- /dev/null",
                "+++ b/" + path,
                "@@ -0,0 +1 @@",
                addedLine);
    }

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }
}
