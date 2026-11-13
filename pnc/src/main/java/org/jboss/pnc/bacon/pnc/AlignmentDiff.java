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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Splits a unified alignment diff into {@link Category categories}, so that the changes which actually influence the
 * build can be told apart from the state and files the manipulators (PME, GME, SMEG, Ant manipulator) and Reqour
 * leave in the aligned tree.
 *
 * <p>
 * Whole files are categorized by name first (manipulator state, reports, repository backups). The remaining files are
 * categorized hunk by hunk, because a single {@code pom.xml} or {@code build.gradle} can mix real alignment changes
 * with repository removals, injected GME wiring or pure re-formatting. A file whose hunks fall into
 * several categories is emitted in each of those parts with the same file header and only its own hunks; the new-side
 * line numbers of the hunk headers are recomputed so that every part applies on its own on top of the pre-alignment
 * sources.
 *
 * <p>
 * Every hunk lands in exactly one part, so applying all the parts, in any order, reproduces the complete alignment.
 * The categories are declared in apply order, from the one always needed to the ones that can always be skipped, and
 * grouped into the {@link Part files} written for the reproducer; applying them in that order keeps the line offsets
 * of the most important parts smallest. Every part other
 * than {@link Category#BUILD} can be left out (or reverse-applied on the aligned tree) independently of the others:
 * anything not recognized stays in {@link Category#BUILD}, and recognized GME state that is still referenced from the
 * build part is kept there too (see {@link #warnings()}). A renamed file is never split, so that the rename is applied
 * only once; a mode change repeated in several parts is harmless.
 */
final class AlignmentDiff {

    /**
     * The categories of an alignment diff, each written to its own file, declared in priority order: the order in
     * which a reproducer applies the parts on top of the pre-alignment sources, from the one always needed to the ones
     * that can always be skipped. Whole files are categorized by name; the hunks of the other files are tried in the
     * order {@link #FORMATTING}, {@link #RERUN_STATE} (GME injections), {@link #METADATA} (PME stamp),
     * {@link #REPOSITORIES}, else {@link #BUILD}.
     */
    enum Category {
        /**
         * Changes that influence the build: the alignment itself (dependency, plugin and property changes, the
         * project's own version rewritten to the aligned version, files the alignment generated such as a
         * {@code pom.xml} for a Maven 1 project) and anything not recognized as another category. The project version
         * rewrites are kept here, as properties holding the project version (e.g. referencing sibling modules) are
         * aligned together with the other properties, and the GME/SMEG version is in their state files anyway.
         */
        BUILD(
                "build",
                true,
                "The alignment itself: dependency, plugin and property versions changed by the manipulator, the "
                        + "project's own version rewritten to the aligned version, generated files, and every "
                        + "change no other part recognized.",
                "Building the aligned project.",
                "never: this is the alignment"),
        /**
         * State a later run of the manipulator reads back or re-creates: GME's {@code manipulation.json} (reused
         * versions and version base) together with its {@code gme*.gradle} scripts and the {@code apply from} lines
         * injected to load them, and SMEG's {@code manipulations.sbt} (loaded by sbt, so SMEG starts from the aligned
         * version). They are what applies the GME/SMEG alignment at build time, so they are kept or removed as a
         * whole, and must be removed for a clean re-run.
         */
        RERUN_STATE(
                "rerun-state",
                true,
                "Files GME (Gradle) or SMEG (sbt) left in the project and reads back: manipulation.json, the "
                        + "gme*.gradle scripts and the 'apply from' lines loading them, manipulations.sbt.",
                "Building an aligned Gradle/sbt project: these files apply the alignment at build time.",
                "only when running GME/SMEG again yourself, which must start from the upstream state"),
        /**
         * Repositories removed from a {@code pom.xml} ({@code <repository>} / {@code <pluginRepository>}, or their
         * whole container). A build of the modified sources outside PNC may need them back; they are listed in the
         * manifest.
         */
        REPOSITORIES(
                "repositories",
                false,
                "Maven repositories removed from the pom.xml files, so that PNC resolves from its own repository "
                        + "only. Listed in the manifest.",
                "Reproducing the PNC build exactly.",
                "when building outside PNC, where the removed repositories are still needed to resolve dependencies"),
        /**
         * Files whose presence prevents a later run of the manipulator: the PME marker makes PME skip ("Skipping
         * manipulation as previous execution found."), so the next build ships unaligned.
         */
        RERUN_BLOCKER(
                "rerun-blocker",
                false,
                "The PME marker file (target/pom-manip-ext-marker.txt).",
                "Nothing at build time; while present, a new PME run skips the alignment.",
                "when running PME again: the marker makes PME skip the alignment"),
        /**
         * Informative files not read by the build: alignment reports, repository backups, the PME provenance stamp,
         * the npm manipulator result written by Reqour.
         */
        METADATA(
                "metadata",
                false,
                "Reports and backups of the manipulators and Reqour (alignmentReport.json, "
                        + "target/manipulation.json, repositories-backup.xml, ...) and the PME stamp in the pom.xml "
                        + "files.",
                "Reviewing what the alignment did; the build does not read them.",
                "always: reports and backups not read by the build"),
        /**
         * Hunks that only change whitespace, line endings or the order of the attributes of an XML tag, typically the
         * XML re-serialization done by PME.
         */
        FORMATTING(
                "formatting",
                false,
                "Whitespace, line ending and XML attribute order changes, typically PME re-serializing the pom.xml "
                        + "files.",
                "Sources byte-identical to the ones PNC built; the build is not affected.",
                "always: whitespace and XML attribute order only");

        private final String id;
        private final boolean required;
        private final String contents;
        private final String neededFor;
        private final String skip;

        Category(String id, boolean required, String contents, String neededFor, String skip) {
            this.id = id;
            this.required = required;
            this.contents = contents;
            this.neededFor = neededFor;
            this.skip = skip;
        }

        /** The name of the category in the manifest. */
        String id() {
            return id;
        }

        /** The file the category is written to. */
        Part file() {
            return Arrays.stream(Part.values())
                    .filter(part -> part.categories().contains(this))
                    .findFirst()
                    .orElseThrow();
        }

        /** Whether a reproduction of the PNC build normally needs the part. */
        boolean required() {
            return required;
        }

        /** What the part contains. */
        String contents() {
            return contents;
        }

        /** What applying the part is needed for. */
        String neededFor() {
            return neededFor;
        }

        /** When the part can be left out. */
        String skip() {
            return skip;
        }
    }

    /**
     * The files the categories are written to, numbered in apply order: the two parts a build of the aligned project
     * needs, then the optional ones grouped by purpose. Within a file, each category is a separate section.
     */
    enum Part {
        BUILD("build", true, "the alignment", Category.BUILD),
        RERUN_STATE("rerun-state", true, "GME/SMEG files applying the alignment at build time", Category.RERUN_STATE),
        REPRODUCE(
                "reproduce",
                false,
                "makes the build match the PNC one exactly",
                Category.REPOSITORIES),
        NOISE(
                "noise",
                false,
                "never affects the build",
                Category.RERUN_BLOCKER,
                Category.METADATA,
                Category.FORMATTING);

        private final String id;
        private final boolean required;
        private final String summary;
        private final List<Category> categories;

        Part(String id, boolean required, String summary, Category... categories) {
            this.id = id;
            this.required = required;
            this.summary = summary;
            this.categories = List.of(categories);
        }

        String id() {
            return id;
        }

        /** The position of the file in the apply order, starting at 1. */
        int number() {
            return ordinal() + 1;
        }

        /** The file name suffix, e.g. {@code 3-reproduce}: sorted by name, the files are in apply order. */
        String suffix() {
            return number() + "-" + id;
        }

        /** Whether a build of the aligned project needs the file. */
        boolean required() {
            return required;
        }

        /** What the file is about, in a few words. */
        String summary() {
            return summary;
        }

        /** The categories written to the file, in apply order. */
        List<Category> categories() {
            return categories;
        }
    }

    /** The category given to a whole file or to one hunk, and the name of the rule that chose it. */
    record Change(Category category, String rule) {
    }

    /** One file of the diff and the categories of its content: one change for the whole file, or one per hunk. */
    record FileChange(String path, String status, List<Change> changes) {
    }

    /**
     * A repository removed from a build file. {@code kind} is {@code repository} or {@code pluginRepository};
     * {@code id} or {@code url} may be null when not part of the removal.
     */
    record Repository(String kind, String id, String url, String file) {
    }

    /**
     * A change of the project's own version, whatever the manipulator: {@code original} may be null when the
     * manipulator does not record it (SMEG). {@code files} are where the change was found.
     */
    record ProjectVersion(String original, String aligned, List<String> files) {
    }

    /**
     * A module of the project aligned by the manipulator, read from the alignment reports in the diff. The group and
     * artifact are null when the report does not record them (SMEG), and so is {@code originalVersion}.
     */
    record AlignedProject(String groupId, String artifactId, String originalVersion, String alignedVersion) {
    }

    /**
     * A dependency or plugin aligned by the manipulator, read from the alignment reports in the diff, with the project
     * modules where it was aligned. {@code type} is {@code dependency}, {@code plugin}, {@code managed-dependency} or
     * {@code managed-plugin}.
     */
    record AlignedDependency(
            String type,
            String groupId,
            String artifactId,
            String originalVersion,
            String alignedVersion,
            List<String> modules) {
    }

    // Rule names, reported in the manifest.
    static final String RULE_PME_MARKER = "pme-marker";
    static final String RULE_GME_MODEL = "gme-manipulation-model";
    static final String RULE_GME_WIRING = "gme-wiring";
    static final String RULE_GME_INJECTION = "gme-injection";
    static final String RULE_SMEG_OVERRIDES = "smeg-overrides";
    static final String RULE_ALIGNMENT_REPORT = "alignment-report";
    static final String RULE_PME_MODEL = "pme-manipulation-model";
    static final String RULE_SMEG_REPORT = "smeg-report";
    static final String RULE_ANT_DISCOVERY_REPORT = "ant-discovery-report";
    static final String RULE_REPOSITORIES_BACKUP = "repositories-backup";
    static final String RULE_REQOUR_NPM_RESULTS = "reqour-npm-results";
    static final String RULE_PME_STAMP = "pme-stamp";
    static final String RULE_REMOVED_REPOSITORIES = "removed-repositories";
    static final String RULE_ALIGNED_VERSION = "aligned-version";
    static final String RULE_WHITESPACE = "whitespace";
    static final String RULE_XML_ATTRIBUTE_ORDER = "xml-attribute-order";
    static final String RULE_ALIGNMENT = "alignment";
    static final String RULE_UNRECOGNIZED_ADDED = "unrecognized-added-file";

    // PME: the marker whose presence makes PME skip. Reqour commits with `git add --all`, so it is part of the diff
    // when the project does not ignore target/.
    static final String PME_MARKER = "pom-manip-ext-marker.txt";
    // GME writes manipulation.json in the project root and reads it back on the next run; the GME plugin also reads it
    // at build time. PME writes target/manipulation.json, a report.
    static final String MANIPULATION_JSON = "manipulation.json";
    // GME: scripts copied into the project and wired into the build. gme-repos.gradle is committed under gradle/,
    // matched here by file name.
    static final String GME_GRADLE = "gme.gradle";
    static final String GME_PLUGINCONFIGS_GRADLE = "gme-pluginconfigs.gradle";
    static final String GME_REPOS_GRADLE = "gme-repos.gradle";
    // SMEG: version/setting overrides, auto-loaded by sbt as a build file.
    static final String MANIPULATIONS_SBT = "manipulations.sbt";

    // PME/GME/Ant manipulator alignment report, SMEG report and Ant manipulator diagnostic report.
    static final String ALIGNMENT_REPORT_JSON = "alignmentReport.json";
    static final String MANIPULATIONS_JSON = "manipulations.json";
    static final String DISCOVERY_REPORT_JSON = "discovery-report.json";
    // PME (removed repositories, profile removed-by-pme) and GME (repositories found, profile generated-by-gme) backup,
    // the -DrepoRemovalBackup value used by PNC. With -DrepoRemovalBackup=settings.xml PME writes it there.
    static final String REPOSITORIES_BACKUP = "repositories-backup.xml";
    static final String SETTINGS_XML = "settings.xml";
    static final String PME_REMOVED_REPOSITORIES_PROFILE = "removed-by-pme";
    // Reqour creates the npm manipulator result file in the repository root (NpmProvider RESULTS_FILENAME).
    static final String REQOUR_NPM_RESULTS = "results";

    private static final Set<String> GME_SCRIPTS = Set.of(GME_GRADLE, GME_PLUGINCONFIGS_GRADLE, GME_REPOS_GRADLE);
    private static final Set<String> GME_RULES = Set.of(RULE_GME_MODEL, RULE_GME_WIRING, RULE_GME_INJECTION);

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    // The statements GME injects into the Gradle build files (AlignmentTask APPLY_GME_REPOS, INJECT_GME_START/END and
    // their Kotlin variants), without whitespace. INJECT_GME_START is "buildscript { apply from: \"gme.gradle\"" which
    // either extends an existing "buildscript {" line or opens a new block, so only the apply part is listed.
    // APPLY_GME_REPOS is a complete block and is removed as a whole.
    private static final List<String> GME_INJECTIONS = Stream
            .of(
                    "buildscript { apply from: new File((gradle.getParent() == null ? gradle : "
                            + "gradle.getParent()).startParameter.getCurrentDir(), \"gradle/gme-repos.gradle\"),"
                            + " to: buildscript }",
                    "project.apply { from(\"gme.gradle\") }",
                    "project.apply { from(\"gme-pluginconfigs.gradle\") }",
                    "apply from: \"gme.gradle\"",
                    "apply from: \"gme-pluginconfigs.gradle\"")
            .map(AlignmentDiff::withoutWhitespace)
            .collect(Collectors.toList());
    private static final String EMPTY_BUILDSCRIPT = "buildscript{}";
    // Any reference to a GME script, to check that none is left in the build part when the scripts are categorized.
    private static final Pattern GME_SCRIPT_REFERENCE = Pattern.compile("gme(-pluginconfigs|-repos)?\\.gradle");
    // PME appends "<!--\nModified by POM Manipulation Extension for Maven 5.10 ( SHA: ... )\n-->" to the POMs.
    private static final Pattern PME_STAMP = Pattern.compile("<!--ModifiedbyPOMManipulationExtension[^>]*-->");

    // Matches the per-file header that starts every section of a `git diff`, capturing the b/ (new) path in group 2.
    private static final Pattern DIFF_GIT_HEADER = Pattern.compile("^diff --git a/(.*) b/(.*)$");
    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -(\\d+(?:,\\d+)?) \\+(\\d+)(?:,(\\d+))? @@(.*)$");

    // An XML start tag with its attributes (group 2), possibly spanning lines, and the attributes in it.
    private static final Pattern XML_START_TAG = Pattern
            .compile("<([\\w:.-]+)((?:\\s+[\\w:.-]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'))+)\\s*(/?>)");
    private static final Pattern XML_ATTRIBUTE = Pattern.compile("[\\w:.-]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*')");

    private static final Pattern REPOSITORY_CONTAINER_OPEN = Pattern.compile("<(repositories|pluginRepositories)>");
    private static final Pattern REPOSITORY_CONTAINER_CLOSE = Pattern.compile("</(repositories|pluginRepositories)>");
    // <distributionManagement> also contains a <repository>, the deploy target, which is not one to resolve from.
    private static final Pattern OTHER_CONTAINER_OPEN = Pattern.compile("<distributionManagement>");
    private static final Pattern REPOSITORY_OPEN = Pattern.compile("<(repository|pluginRepository)>");
    private static final Pattern REPOSITORY_CLOSE = Pattern.compile("</(repository|pluginRepository)>");
    private static final Pattern XML_ID = Pattern.compile("<id>\\s*([^<]*?)\\s*</id>");
    private static final Pattern XML_URL = Pattern.compile("<url>\\s*([^<]*?)\\s*</url>");

    // The words of a line compared when looking for version-only changes: versions, property references, names.
    private static final Pattern VERSION_TOKEN = Pattern.compile("[\\w.${}+-]+");
    // Aligned versions carry a PNC suffix (redhat-00001, temporary-redhat-00001, rhlw-00000-n-00012, ibm-00001, ...);
    // a candidate without one is ignored, so an unchanged version is never mistaken for a rewrite.
    private static final Pattern ALIGNED_VERSION_SUFFIX = Pattern.compile("[.-](redhat|rhlw|ibm)(-[a-z]+)*-\\d");
    private static final Pattern SBT_VERSION = Pattern.compile("version\\s*:=\\s*\"([^\"]+)\"");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<Category, String> parts;
    private final List<FileChange> files;
    private final List<Repository> removedRepositories;
    private final Set<String> alignedVersions;
    private final String project;
    private final List<AlignedProject> alignedProjects;
    private final List<AlignedDependency> alignedDependencies;
    private final List<ProjectVersion> projectVersions;
    private final List<String> warnings;

    private AlignmentDiff(
            Map<Category, String> parts,
            List<FileChange> files,
            List<Repository> removedRepositories,
            Set<String> alignedVersions,
            String project,
            List<AlignedProject> alignedProjects,
            List<AlignedDependency> alignedDependencies,
            List<ProjectVersion> projectVersions,
            List<String> warnings) {
        this.parts = parts;
        this.files = files;
        this.removedRepositories = removedRepositories;
        this.alignedVersions = alignedVersions;
        this.project = project;
        this.alignedProjects = alignedProjects;
        this.alignedDependencies = alignedDependencies;
        this.projectVersions = projectVersions;
        this.warnings = warnings;
    }

    /**
     * The changes of the project's own version, found the same way whatever the manipulator: PME rewrites of the
     * version in the build files ({@link #RULE_ALIGNED_VERSION} hunks, in {@link Category#BUILD}), the
     * {@code originalVersion}/{@code version} of GME's
     * {@code manipulation.json} and the {@code version :=} of SMEG's {@code manipulations.sbt}.
     */
    List<ProjectVersion> projectVersions() {
        return projectVersions;
    }

    /** The content of the given part, or an empty string when the diff has no changes in that category. */
    String part(Category category) {
        return parts.getOrDefault(category, "");
    }

    boolean has(Category category) {
        return !part(category).isEmpty();
    }

    /** Every file of the diff, in diff order, with the categories of its content. */
    List<FileChange> files() {
        return files;
    }

    /** The repositories removed from the build files, in diff order. */
    List<Repository> removedRepositories() {
        return removedRepositories;
    }

    /**
     * The aligned versions of the project's own modules, used to recognize the {@link #RULE_ALIGNED_VERSION} changes:
     * the ones of {@link #alignedProjects()} and the ones given by the caller. The versions of the aligned dependencies
     * are not included, so that a dependency rewrite is never taken for the project version.
     */
    Set<String> alignedVersions() {
        return alignedVersions;
    }

    /**
     * The identifier of the project, {@code groupId:artifactId} of its root module: the PME execution root or the GME
     * root project recorded by the alignment reports in the diff, else the one given by the caller. Null when unknown.
     */
    String project() {
        return project;
    }

    /** The project modules recorded by the alignment reports in the diff, in diff order. */
    List<AlignedProject> alignedProjects() {
        return alignedProjects;
    }

    /** The dependencies and plugins recorded as aligned by the alignment reports in the diff, in diff order. */
    List<AlignedDependency> alignedDependencies() {
        return alignedDependencies;
    }

    /** Files the alignment added that no rule recognized; they stay in {@link Category#BUILD}. */
    List<String> unrecognizedAddedFiles() {
        return files.stream()
                .filter(file -> file.changes().stream().anyMatch(c -> RULE_UNRECOGNIZED_ADDED.equals(c.rule())))
                .map(FileChange::path)
                .collect(Collectors.toList());
    }

    /** Recognized content that could not be categorized safely and was kept in {@link Category#BUILD}. */
    List<String> warnings() {
        return warnings;
    }

    /**
     * The manifest of the split: aligned versions, per-file categories with the matching rules, removed repositories,
     * unrecognized added files and warnings.
     */
    Map<String, Object> manifest() {
        return manifest(category -> null);
    }

    /**
     * The manifest of the split, with the file each category is written to.
     *
     * @param fileName the file name of each part, or null when not known
     */
    Map<String, Object> manifest(Function<Part, String> fileName) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("project", project);
        manifest.put("alignedVersions", alignedVersions);
        manifest.put("alignedProjects", alignedProjects.stream().map(project -> {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("groupId", project.groupId());
            entry.put("artifactId", project.artifactId());
            entry.put("originalVersion", project.originalVersion());
            entry.put("alignedVersion", project.alignedVersion());
            return entry;
        }).collect(Collectors.toList()));
        manifest.put("alignedDependencies", alignedDependencies.stream().map(dependency -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", dependency.type());
            entry.put("groupId", dependency.groupId());
            entry.put("artifactId", dependency.artifactId());
            entry.put("originalVersion", dependency.originalVersion());
            entry.put("alignedVersion", dependency.alignedVersion());
            entry.put("modules", dependency.modules());
            return entry;
        }).collect(Collectors.toList()));

        // The categories in apply order, with what a reproducer may skip.
        manifest.put("parts", Arrays.stream(Category.values()).map(category -> {
            Map<String, Object> part = new LinkedHashMap<>();
            part.put("category", category.id());
            part.put("part", category.file().number());
            String file = fileName.apply(category.file());
            if (file != null) {
                part.put("file", file);
            }
            part.put("present", has(category));
            part.put("required", category.required());
            part.put("contents", category.contents());
            part.put("neededFor", category.neededFor());
            part.put("skipWhen", category.skip());
            return part;
        }).collect(Collectors.toList()));

        Map<String, Object> summary = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            List<FileChange> inCategory = files.stream()
                    .filter(file -> file.changes().stream().anyMatch(c -> c.category() == category))
                    .collect(Collectors.toList());
            long changes = inCategory.stream()
                    .flatMap(file -> file.changes().stream())
                    .filter(c -> c.category() == category)
                    .count();
            summary.put(category.id(), Map.of("files", inCategory.size(), "changes", changes));
        }
        manifest.put("summary", summary);

        manifest.put("files", files.stream().map(file -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("path", file.path());
            entry.put("status", file.status());
            entry.put("changes", file.changes().stream().map(c -> {
                Map<String, String> change = new LinkedHashMap<>();
                change.put("category", c.category().id());
                change.put("rule", c.rule());
                return change;
            }).collect(Collectors.toList()));
            return entry;
        }).collect(Collectors.toList()));

        manifest.put("removedRepositories", removedRepositories.stream().map(repository -> {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("kind", repository.kind());
            entry.put("id", repository.id());
            entry.put("url", repository.url());
            entry.put("file", repository.file());
            return entry;
        }).collect(Collectors.toList()));
        manifest.put("projectVersions", projectVersions.stream().map(version -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("original", version.original());
            entry.put("aligned", version.aligned());
            entry.put("files", version.files());
            return entry;
        }).collect(Collectors.toList()));
        manifest.put("unrecognizedAddedFiles", unrecognizedAddedFiles());
        manifest.put("warnings", warnings);
        return manifest;
    }

    /**
     * Applies {@code mask} (credential masking) only to the sections of whole metadata files: the reports and backups
     * the manipulators and Reqour write ({@link Category#METADATA} by file, e.g. {@code alignmentReport.json},
     * {@code target/manipulation.json}, {@code repositories-backup.xml}), which are not needed to reproduce the build.
     * Every other section, and the text outside sections, is kept byte for byte, so that the diff still recreates the
     * aligned sources (a masked line in a source or build file would no longer apply, e.g. Java code like
     * {@code token=t;}). Sections carrying a {@code GIT binary patch} are never masked: their payload is encoded file
     * content.
     */
    static String maskSecretsInMetadata(String unifiedDiff, UnaryOperator<String> mask) {
        StringBuilder out = new StringBuilder(unifiedDiff.length());
        int start = 0;
        while (start < unifiedDiff.length()) {
            int next = unifiedDiff.indexOf("\ndiff --git ", start);
            int end = next < 0 ? unifiedDiff.length() : next + 1;
            String chunk = unifiedDiff.substring(start, end);
            out.append(isMaskableMetadata(chunk) ? mask.apply(chunk) : chunk);
            start = end;
        }
        return out.toString();
    }

    private static boolean isMaskableMetadata(String chunk) {
        if (!chunk.startsWith("diff --git ") || chunk.contains("\nGIT binary patch\n")) {
            return false;
        }
        Section section = new Section(Arrays.asList(chunk.split("\n", -1)));
        Change change = categorizeFile(section);
        return change != null && change.category() == Category.METADATA;
    }

    /** Splits the given unified diff, taking the aligned versions from the alignment reports it contains. */
    static AlignmentDiff split(String unifiedDiff) {
        return split(unifiedDiff, Set.of());
    }

    /**
     * Routes each file section, or each hunk of a file section, of the given unified diff to its category.
     *
     * @param unifiedDiff the unified diff, may be null
     * @param knownAlignedVersions aligned project versions known by the caller (e.g. from the PNC build), in addition
     *        to the ones read from the alignment reports in the diff; candidates without a PNC suffix are ignored
     * @return the split result; every part is an empty string when the input is null or empty
     */
    static AlignmentDiff split(String unifiedDiff, Collection<String> knownAlignedVersions) {
        return split(unifiedDiff, knownAlignedVersions, null);
    }

    /**
     * Routes each file section, or each hunk of a file section, of the given unified diff to its category.
     *
     * @param unifiedDiff the unified diff, may be null
     * @param knownAlignedVersions aligned project versions known by the caller (e.g. from the PNC build), in addition
     *        to the ones read from the alignment reports in the diff; candidates without a PNC suffix are ignored
     * @param knownProject the project identifier known by the caller (e.g. from the PNC build), used when the
     *        alignment reports in the diff do not record it; may be null
     * @return the split result; every part is an empty string when the input is null or empty
     */
    static AlignmentDiff split(String unifiedDiff, Collection<String> knownAlignedVersions, String knownProject) {
        List<Section> sections = parse(unifiedDiff);

        Set<AlignedProject> projects = new LinkedHashSet<>();
        Map<List<String>, Set<String>> dependencies = new LinkedHashMap<>();
        for (Section section : sections) {
            collectReport(section, projects, dependencies);
        }

        Set<String> alignedVersions = new TreeSet<>();
        Stream.concat(knownAlignedVersions.stream(), projects.stream().map(AlignedProject::alignedVersion))
                .filter(version -> version != null && ALIGNED_VERSION_SUFFIX.matcher(version).find())
                .forEach(alignedVersions::add);

        for (Section section : sections) {
            categorize(section, alignedVersions);
        }

        List<String> warnings = new ArrayList<>();
        keepReferencedGmeStateInBuild(sections, warnings);

        Map<Category, List<String>> lines = new EnumMap<>(Category.class);
        List<FileChange> files = new ArrayList<>();
        List<Repository> removedRepositories = new ArrayList<>();
        for (Section section : sections) {
            emit(section, lines);
            if (section.path != null) {
                files.add(new FileChange(section.path, section.status, List.copyOf(section.changes())));
            }
            // Repository removals are recognized per hunk only.
            for (int i = 0; i < section.hunkChanges.size(); i++) {
                if (section.hunkChanges.get(i).category() == Category.REPOSITORIES) {
                    removedRepositories.addAll(removedRepositories(section.path, section.hunks.get(i)));
                }
            }
        }

        Map<Category, String> parts = new EnumMap<>(Category.class);
        lines.forEach((category, partLines) -> parts.put(category, String.join("\n", partLines) + "\n"));
        return new AlignmentDiff(
                parts,
                Collections.unmodifiableList(files),
                Collections.unmodifiableList(removedRepositories),
                Collections.unmodifiableSet(alignedVersions),
                // The PME execution root and the GME root project are recorded first.
                projects.stream()
                        .filter(project -> project.groupId() != null && project.artifactId() != null)
                        .map(project -> project.groupId() + ":" + project.artifactId())
                        .findFirst()
                        .orElse(knownProject != null && !knownProject.isBlank() ? knownProject : null),
                List.copyOf(projects),
                dependencies.entrySet()
                        .stream()
                        .map(
                                entry -> new AlignedDependency(
                                        entry.getKey().get(0),
                                        entry.getKey().get(1),
                                        entry.getKey().get(2),
                                        entry.getKey().get(3),
                                        entry.getKey().get(4),
                                        List.copyOf(entry.getValue())))
                        .collect(Collectors.toUnmodifiableList()),
                projectVersions(sections, alignedVersions),
                Collections.unmodifiableList(warnings));
    }

    /** See {@link #projectVersions()}; the same change found in several files is reported once. */
    private static List<ProjectVersion> projectVersions(List<Section> sections, Set<String> alignedVersions) {
        // original -> aligned -> files; LinkedHashMap keys allow a null original.
        Map<String, Map<String, Set<String>>> changes = new LinkedHashMap<>();
        for (Section section : sections) {
            if (section.path == null) {
                continue;
            }
            String fileName = section.fileName();
            boolean inTarget = List.of(section.path.split("/")).contains("target");
            if (MANIPULATION_JSON.equals(fileName) && !inTarget) {
                // GME: the root project and its children.
                try {
                    JsonNode model = JSON.readTree(section.addedText());
                    if (model != null) {
                        collectGmeVersions(model, section.path, changes);
                    }
                } catch (Exception e) {
                    // Only modified lines in the diff: nothing to learn from it.
                }
            } else if (MANIPULATIONS_SBT.equals(fileName)) {
                // SMEG records the aligned version only, the original one may be on a removed line.
                Matcher aligned = SBT_VERSION.matcher(section.addedText());
                if (aligned.find()) {
                    Matcher original = SBT_VERSION.matcher(section.removedText());
                    addVersion(changes, original.find() ? original.group(1) : null, aligned.group(1), section.path);
                }
            }
            // PME (and any build file rewritten the same way): the words changed by the version hunks.
            for (int i = 0; i < section.hunkChanges.size(); i++) {
                if (RULE_ALIGNED_VERSION.equals(section.hunkChanges.get(i).rule())) {
                    List<String> hunk = section.hunks.get(i);
                    List<String> removed = tokens(side(hunk, '-'));
                    List<String> added = tokens(side(hunk, '+'));
                    for (int t = 0; t < added.size() && t < removed.size(); t++) {
                        // A ${project.version} reference resolved to the aligned version is not another original.
                        if (!removed.get(t).equals(added.get(t)) && alignedVersions.contains(added.get(t))
                                && !removed.get(t).contains("${")) {
                            addVersion(changes, removed.get(t), added.get(t), section.path);
                        }
                    }
                }
            }
        }

        List<ProjectVersion> versions = new ArrayList<>();
        changes.forEach(
                (original, byAligned) -> byAligned.forEach(
                        (aligned, files) -> versions.add(new ProjectVersion(original, aligned, List.copyOf(files)))));
        return Collections.unmodifiableList(versions);
    }

    private static void collectGmeVersions(JsonNode model, String path, Map<String, Map<String, Set<String>>> changes) {
        JsonNode version = model.get("version");
        JsonNode original = model.get("originalVersion");
        if (version != null && version.isTextual() && original != null && original.isTextual()
                && !version.asText().equals(original.asText())) {
            addVersion(changes, original.asText(), version.asText(), path);
        }
        JsonNode children = model.get("children");
        if (children != null) {
            children.forEach(child -> collectGmeVersions(child, path, changes));
        }
    }

    private static void addVersion(
            Map<String, Map<String, Set<String>>> changes,
            String original,
            String aligned,
            String file) {
        changes.computeIfAbsent(original, o -> new LinkedHashMap<>())
                .computeIfAbsent(aligned, a -> new LinkedHashSet<>())
                .add(file);
    }

    /** The text of the removed ('-') or added ('+') lines of a hunk, without the marker. */
    private static String side(List<String> hunk, char marker) {
        StringBuilder text = new StringBuilder();
        for (String line : hunk.subList(1, hunk.size())) {
            if (line.charAt(0) == marker) {
                text.append(line, 1, line.length()).append('\n');
            }
        }
        return text.toString();
    }

    /** A file section of the diff, or the content before the first file header (with a null path). */
    private static final class Section {
        private final List<String> lines;
        private final String path;
        private final String status;
        private final List<String> header;
        private final List<List<String>> hunks = new ArrayList<>();
        // Set either for the whole file or per hunk.
        private Change fileChange;
        private final List<Change> hunkChanges = new ArrayList<>();

        private Section(List<String> lines) {
            this.lines = lines;
            Matcher matcher = DIFF_GIT_HEADER.matcher(lines.get(0));
            this.path = matcher.matches() ? matcher.group(2) : null;

            int firstHunk = 0;
            while (firstHunk < lines.size() && !lines.get(firstHunk).startsWith("@@")) {
                firstHunk++;
            }
            this.header = lines.subList(0, firstHunk);
            for (String line : lines.subList(firstHunk, lines.size())) {
                if (line.startsWith("@@")) {
                    hunks.add(new ArrayList<>());
                }
                hunks.get(hunks.size() - 1).add(line);
            }

            if (header.stream().anyMatch(line -> line.startsWith("new file mode"))) {
                status = "added";
            } else if (header.stream().anyMatch(line -> line.startsWith("deleted file mode"))) {
                status = "deleted";
            } else if (header.stream().anyMatch(line -> line.startsWith("rename from"))) {
                status = "renamed";
            } else {
                status = "modified";
            }
        }

        private String fileName() {
            return fileName(path);
        }

        private List<Change> changes() {
            return fileChange != null ? List.of(fileChange) : hunkChanges;
        }

        /** The text removed by the section, without the leading '-'. */
        private String removedText() {
            return hunks.stream()
                    .flatMap(hunk -> hunk.subList(1, hunk.size()).stream())
                    .filter(line -> line.startsWith("-"))
                    .map(line -> line.substring(1))
                    .collect(Collectors.joining("\n"));
        }

        /** The text added by the section, without the leading '+'. */
        private String addedText() {
            return hunks.stream()
                    .flatMap(List::stream)
                    .filter(line -> line.startsWith("+"))
                    .map(line -> line.substring(1))
                    .collect(Collectors.joining("\n"));
        }

        private static String fileName(String path) {
            int lastSlash = path.lastIndexOf('/');
            return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
    }

    private static List<Section> parse(String unifiedDiff) {
        List<Section> sections = new ArrayList<>();
        if (unifiedDiff == null || unifiedDiff.isEmpty()) {
            return sections;
        }
        List<String> input = new ArrayList<>(List.of(unifiedDiff.split("\n", -1)));
        if (input.get(input.size() - 1).isEmpty()) {
            // The terminating newline; every non-empty part gets its own when emitted.
            input.remove(input.size() - 1);
        }

        List<String> current = new ArrayList<>();
        for (String line : input) {
            if (DIFF_GIT_HEADER.matcher(line).matches() && !current.isEmpty()) {
                sections.add(new Section(current));
                current = new ArrayList<>();
            }
            current.add(line);
        }
        if (!current.isEmpty()) {
            sections.add(new Section(current));
        }
        return sections;
    }

    private static void categorize(Section section, Set<String> alignedVersions) {
        if (section.path == null) {
            // Content before the first header (there usually is none for a GitHub compare diff).
            section.fileChange = new Change(Category.BUILD, RULE_ALIGNMENT);
            return;
        }
        Change fileChange = categorizeFile(section);
        if (fileChange != null) {
            section.fileChange = fileChange;
            return;
        }
        if (section.hunks.isEmpty() || "added".equals(section.status) || "renamed".equals(section.status)) {
            // Nothing to look into (binary, mode change only, empty file), a whole new file, or a renamed file whose
            // rename must be applied only once (a second "rename from" fails, the old path no longer exists).
            section.fileChange = new Change(
                    Category.BUILD,
                    "added".equals(section.status) ? RULE_UNRECOGNIZED_ADDED : RULE_ALIGNMENT);
            return;
        }
        for (List<String> hunk : section.hunks) {
            section.hunkChanges.add(categorizeHunk(section.fileName(), hunk, alignedVersions));
        }
    }

    private static Change categorizeFile(Section section) {
        String fileName = section.fileName();
        boolean inTarget = List.of(section.path.split("/")).contains("target");
        if (PME_MARKER.equals(fileName)) {
            return new Change(Category.RERUN_BLOCKER, RULE_PME_MARKER);
        }
        if (MANIPULATION_JSON.equals(fileName)) {
            return inTarget ? new Change(Category.METADATA, RULE_PME_MODEL)
                    : new Change(Category.RERUN_STATE, RULE_GME_MODEL);
        }
        if (GME_SCRIPTS.contains(fileName)) {
            return new Change(Category.RERUN_STATE, RULE_GME_WIRING);
        }
        if (MANIPULATIONS_SBT.equals(fileName)) {
            return new Change(Category.RERUN_STATE, RULE_SMEG_OVERRIDES);
        }
        if (ALIGNMENT_REPORT_JSON.equals(fileName)) {
            return new Change(Category.METADATA, RULE_ALIGNMENT_REPORT);
        }
        if (MANIPULATIONS_JSON.equals(fileName)) {
            return new Change(Category.METADATA, RULE_SMEG_REPORT);
        }
        if (DISCOVERY_REPORT_JSON.equals(fileName)) {
            return new Change(Category.METADATA, RULE_ANT_DISCOVERY_REPORT);
        }
        if (REPOSITORIES_BACKUP.equals(fileName) || (SETTINGS_XML.equals(fileName)
                && section.lines.stream().anyMatch(line -> line.contains(PME_REMOVED_REPOSITORIES_PROFILE)))) {
            return new Change(Category.METADATA, RULE_REPOSITORIES_BACKUP);
        }
        if (REQOUR_NPM_RESULTS.equals(section.path) && "added".equals(section.status)) {
            return new Change(Category.METADATA, RULE_REQOUR_NPM_RESULTS);
        }
        return null;
    }

    private static Change categorizeHunk(String fileName, List<String> hunk, Set<String> alignedVersions) {
        StringBuilder removed = new StringBuilder();
        StringBuilder added = new StringBuilder();
        for (String line : hunk.subList(1, hunk.size())) {
            if (line.startsWith("-")) {
                removed.append(line, 1, line.length()).append('\n');
            } else if (line.startsWith("+")) {
                added.append(line, 1, line.length()).append('\n');
            }
        }
        String removedCode = withoutWhitespace(removed);
        String addedCode = withoutWhitespace(added);

        if (removedCode.equals(addedCode)) {
            return new Change(Category.FORMATTING, RULE_WHITESPACE);
        }
        if (isXmlFile(fileName) && withoutWhitespace(sortingAttributes(removed))
                .equals(withoutWhitespace(sortingAttributes(added)))) {
            return new Change(Category.FORMATTING, RULE_XML_ATTRIBUTE_ORDER);
        }
        if (isGradleFile(fileName) && isGmeInjection(removedCode, addedCode)) {
            return new Change(Category.RERUN_STATE, RULE_GME_INJECTION);
        }
        if (removedCode.isEmpty() && PME_STAMP.matcher(addedCode).matches()) {
            return new Change(Category.METADATA, RULE_PME_STAMP);
        }
        if ("pom.xml".equals(fileName) && removesRepositories(hunk)) {
            return new Change(Category.REPOSITORIES, RULE_REMOVED_REPOSITORIES);
        }
        if (isVersionRewrite(removed, added, alignedVersions)) {
            return new Change(Category.BUILD, RULE_ALIGNED_VERSION);
        }
        return new Change(Category.BUILD, RULE_ALIGNMENT);
    }

    /**
     * Keeps the GME state in {@link Category#BUILD} when the build part still references a GME script: the
     * {@code apply from} injection was not recognized (e.g. a GME version injecting another statement), so leaving the
     * scripts out would break the build. GME state is all or nothing, as the scripts load the plugin that reads
     * {@code manipulation.json}.
     */
    private static void keepReferencedGmeStateInBuild(List<Section> sections, List<String> warnings) {
        // A known GME injection git put in the same hunk as other changes, or a reference not recognized at all.
        Set<String> mixedInjections = new LinkedHashSet<>();
        Set<String> references = new LinkedHashSet<>();
        for (Section section : sections) {
            if (section.path == null || !isGradleFile(section.fileName())) {
                continue;
            }
            for (int i = 0; i < section.hunks.size(); i++) {
                Change change = section.fileChange != null ? section.fileChange : section.hunkChanges.get(i);
                if (change.category() != Category.BUILD) {
                    continue;
                }
                List<String> referencing = section.hunks.get(i)
                        .stream()
                        .filter(line -> line.startsWith("+") && GME_SCRIPT_REFERENCE.matcher(line).find())
                        .collect(Collectors.toList());
                if (referencing.isEmpty()) {
                    continue;
                }
                boolean injections = referencing.stream()
                        .map(line -> withoutWhitespace(line.substring(1)))
                        .allMatch(line -> GME_INJECTIONS.stream().anyMatch(line::contains));
                (injections ? mixedInjections : references).add(section.path);
            }
        }
        if (mixedInjections.isEmpty() && references.isEmpty()) {
            return;
        }

        boolean demoted = false;
        for (Section section : sections) {
            if (section.fileChange != null && GME_RULES.contains(section.fileChange.rule())) {
                section.fileChange = new Change(Category.BUILD, section.fileChange.rule());
                demoted = true;
            }
            for (int i = 0; i < section.hunkChanges.size(); i++) {
                if (GME_RULES.contains(section.hunkChanges.get(i).rule())) {
                    section.hunkChanges.set(i, new Change(Category.BUILD, section.hunkChanges.get(i).rule()));
                    demoted = true;
                }
            }
        }
        if (demoted && !mixedInjections.isEmpty()) {
            warnings.add(
                    "GME state kept in the build part: the GME injection shares a hunk with other changes in "
                            + String.join(", ", mixedInjections)
                            + ", so the injection cannot be left out on its own");
        }
        if (demoted && !references.isEmpty()) {
            warnings.add(
                    "GME state kept in the build part: unrecognized references to the GME scripts in "
                            + String.join(", ", references));
        }
    }

    /** Writes each section, or each hunk of a section, to the lines of its category. */
    private static void emit(Section section, Map<Category, List<String>> lines) {
        if (section.fileChange != null) {
            lines.computeIfAbsent(section.fileChange.category(), c -> new ArrayList<>()).addAll(section.lines);
            return;
        }

        Map<Category, List<String>> sectionParts = new EnumMap<>(Category.class);
        // Line count difference (new - old) of all the previous hunks, and of the previous hunks of each part.
        int shift = 0;
        Map<Category, Integer> partShift = new EnumMap<>(Category.class);
        for (int i = 0; i < section.hunks.size(); i++) {
            List<String> hunk = section.hunks.get(i);
            Category category = section.hunkChanges.get(i).category();
            List<String> part = sectionParts.computeIfAbsent(category, c -> new ArrayList<>(section.header));

            Matcher hunkHeader = HUNK_HEADER.matcher(hunk.get(0));
            if (!hunkHeader.matches()) {
                part.addAll(hunk);
                continue;
            }
            int oldCount = count(hunkHeader.group(1).contains(",") ? hunkHeader.group(1).split(",")[1] : null);
            int newCount = count(hunkHeader.group(3));
            int newStart = Integer.parseInt(hunkHeader.group(2));
            // The hunks of this file left out of the part no longer shift the new-side lines.
            int partNewStart = newStart - (shift - partShift.getOrDefault(category, 0));

            part.add(
                    "@@ -" + hunkHeader.group(1) + " +" + partNewStart
                            + (hunkHeader.group(3) != null ? "," + newCount : "") + " @@" + hunkHeader.group(4));
            part.addAll(hunk.subList(1, hunk.size()));

            shift += newCount - oldCount;
            partShift.merge(category, newCount - oldCount, Integer::sum);
        }

        sectionParts.forEach(
                (category, partLines) -> lines.computeIfAbsent(category, c -> new ArrayList<>()).addAll(partLines));
    }

    /**
     * Collects the project modules and the aligned dependencies recorded by the alignment report added by the section,
     * recognized by its content: the PME/Ant manipulator format ({@code executionRoot} and {@code modules}, in
     * {@code alignmentReport.json} or {@code target/manipulation.json}), the GME format ({@code group},
     * {@code originalVersion}, {@code children} and {@code alignedDependencies}, in {@code manipulation.json}), and the
     * SMEG {@code manipulations.sbt} / {@code manipulations.json}, recording the aligned version only.
     *
     * @param dependencies type, groupId, artifactId, original and aligned version -> modules
     */
    private static void collectReport(
            Section section,
            Set<AlignedProject> projects,
            Map<List<String>, Set<String>> dependencies) {
        if (section.path == null) {
            return;
        }
        String fileName = section.fileName();
        if (MANIPULATIONS_SBT.equals(fileName)) {
            Matcher matcher = SBT_VERSION.matcher(section.addedText());
            while (matcher.find()) {
                projects.add(new AlignedProject(null, null, null, matcher.group(1)));
            }
            return;
        }
        if (!ALIGNMENT_REPORT_JSON.equals(fileName) && !MANIPULATION_JSON.equals(fileName)
                && !MANIPULATIONS_JSON.equals(fileName)) {
            return;
        }
        JsonNode report;
        try {
            report = JSON.readTree(section.addedText());
        } catch (Exception e) {
            // Not a complete report (e.g. only modified lines in the diff): nothing to learn from it.
            return;
        }
        if (report == null || !report.isObject()) {
            return;
        }
        if (report.has("executionRoot") || report.has("modules")) {
            collectPmeReport(report, projects, dependencies);
        } else if (report.has("group") || report.has("children") || report.has("alignedDependencies")) {
            collectGmeReport(report, projects, dependencies);
        } else if (report.path("version").isTextual()) {
            projects.add(new AlignedProject(null, null, null, report.get("version").asText()));
        }
    }

    private static void collectPmeReport(
            JsonNode report,
            Set<AlignedProject> projects,
            Map<List<String>, Set<String>> dependencies) {
        addPmeProject(report.get("executionRoot"), projects);
        for (JsonNode module : report.path("modules")) {
            JsonNode gav = module.get("gav");
            addPmeProject(gav, projects);
            String name = gav != null ? gav.path("artifactId").asText(null) : null;
            collectPmeModule(module, name, dependencies);
            for (JsonNode profile : module.path("profiles")) {
                collectPmeModule(profile, name, dependencies);
            }
        }
    }

    /** The aligned dependencies and plugins of a PME module or profile. */
    private static void collectPmeModule(JsonNode module, String name, Map<List<String>, Set<String>> dependencies) {
        addDependencies("dependency", module.get("dependencies"), name, dependencies);
        addDependencies("plugin", module.get("plugins"), name, dependencies);
        // A ManagedDependenciesItem / ManagedPluginsItem in a module, a list of them in a profile.
        for (JsonNode managed : asList(module.get("managedDependencies"))) {
            addDependencies("managed-dependency", managed.get("dependencies"), name, dependencies);
        }
        for (JsonNode managed : asList(module.get("managedPlugins"))) {
            addDependencies("managed-plugin", managed.get("plugins"), name, dependencies);
        }
    }

    private static void addPmeProject(JsonNode gav, Set<AlignedProject> projects) {
        if (gav == null || !gav.path("version").isTextual()) {
            return;
        }
        String groupId = gav.path("groupId").asText(null);
        String artifactId = gav.path("artifactId").asText(null);
        String originalGav = gav.path("originalGAV").asText(null);
        projects.add(
                new AlignedProject(
                        groupId,
                        artifactId,
                        originalGav != null ? versionOf(originalGav, groupId, artifactId) : null,
                        gav.get("version").asText()));
    }

    private static void collectGmeReport(
            JsonNode model,
            Set<AlignedProject> projects,
            Map<List<String>, Set<String>> dependencies) {
        String name = model.path("name").asText(null);
        if (model.path("version").isTextual()) {
            projects.add(
                    new AlignedProject(
                            model.path("group").asText(null),
                            name,
                            model.path("originalVersion").asText(null),
                            model.get("version").asText()));
        }
        addDependencies("dependency", model.get("alignedDependencies"), name, dependencies);
        for (JsonNode child : model.path("children")) {
            collectGmeReport(child, projects, dependencies);
        }
    }

    /**
     * Adds the entries of a PME/GME map of aligned dependencies, keyed by the original
     * {@code groupId:artifactId:version}
     * with the aligned {@code groupId}, {@code artifactId} and {@code version} as value.
     */
    private static void addDependencies(
            String type,
            JsonNode aligned,
            String module,
            Map<List<String>, Set<String>> dependencies) {
        if (aligned == null || !aligned.isObject()) {
            return;
        }
        aligned.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (!value.path("version").isTextual()) {
                return;
            }
            String groupId = value.path("groupId").asText(null);
            String artifactId = value.path("artifactId").asText(null);
            Set<String> modules = dependencies.computeIfAbsent(
                    Arrays.asList(
                            type,
                            groupId,
                            artifactId,
                            versionOf(entry.getKey(), groupId, artifactId),
                            value.get("version").asText()),
                    key -> new LinkedHashSet<>());
            if (module != null) {
                modules.add(module);
            }
        });
    }

    /** The version of a {@code groupId:artifactId:version} reference, the last segment when it is not of that GA. */
    private static String versionOf(String gav, String groupId, String artifactId) {
        String prefix = groupId + ":" + artifactId + ":";
        if (gav.startsWith(prefix)) {
            return gav.substring(prefix.length());
        }
        int colon = gav.lastIndexOf(':');
        return colon >= 0 ? gav.substring(colon + 1) : gav;
    }

    private static List<JsonNode> asList(JsonNode node) {
        if (node == null) {
            return List.of();
        }
        if (node.isArray()) {
            List<JsonNode> items = new ArrayList<>();
            node.forEach(items::add);
            return items;
        }
        return List.of(node);
    }

    /**
     * Whether the only change of the hunk is the project version rewritten to an aligned version: the words of the old
     * and new side are the same except for words that became one of the aligned versions.
     */
    private static boolean isVersionRewrite(CharSequence removed, CharSequence added, Set<String> alignedVersions) {
        if (alignedVersions.isEmpty()) {
            return false;
        }
        List<String> removedTokens = tokens(removed);
        List<String> addedTokens = tokens(added);
        if (removedTokens.size() != addedTokens.size()) {
            return false;
        }
        boolean rewritten = false;
        for (int i = 0; i < addedTokens.size(); i++) {
            if (!removedTokens.get(i).equals(addedTokens.get(i))) {
                if (!alignedVersions.contains(addedTokens.get(i))) {
                    return false;
                }
                rewritten = true;
            }
        }
        return rewritten;
    }

    private static List<String> tokens(CharSequence text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = VERSION_TOKEN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /**
     * Whether the only change of the hunk is adding the GME statements: removing them from the new side must leave the
     * old side.
     */
    private static boolean isGmeInjection(String removedCode, String addedCode) {
        String withoutInjections = addedCode;
        for (String injection : GME_INJECTIONS) {
            withoutInjections = withoutInjections.replace(injection, "");
        }
        if (withoutInjections.equals(addedCode)) {
            return false;
        }
        return withoutInjections.replace(EMPTY_BUILDSCRIPT, "").equals(removedCode.replace(EMPTY_BUILDSCRIPT, ""));
    }

    /**
     * Whether the hunk removes a {@code <repositories>}/{@code <pluginRepositories>} block, or a repository inside
     * one. Only the old side is followed, to know which container a removed {@code <repository>} belongs to.
     */
    private static boolean removesRepositories(List<String> hunk) {
        boolean inRepositories = false;
        for (String line : hunk.subList(1, hunk.size())) {
            if (line.startsWith("+") || line.startsWith("\\")) {
                continue;
            }
            boolean removed = line.startsWith("-");
            if (REPOSITORY_CONTAINER_OPEN.matcher(line).find()) {
                if (removed) {
                    return true;
                }
                inRepositories = true;
            }
            if (OTHER_CONTAINER_OPEN.matcher(line).find()) {
                inRepositories = false;
            }
            if (removed && inRepositories && REPOSITORY_OPEN.matcher(line).find()) {
                return true;
            }
            if (REPOSITORY_CONTAINER_CLOSE.matcher(line).find()) {
                inRepositories = false;
            }
        }
        return false;
    }

    /** The id and URL of every repository whose definition the hunk removes. */
    private static List<Repository> removedRepositories(String path, List<String> hunk) {
        List<Repository> repositories = new ArrayList<>();
        boolean inRepository = false;
        String kind = null;
        String id = null;
        String url = null;
        for (String line : hunk.subList(1, hunk.size())) {
            if (line.startsWith("+") || line.startsWith("\\")) {
                continue;
            }
            boolean removed = line.startsWith("-");
            Matcher open = REPOSITORY_OPEN.matcher(line);
            if (open.find()) {
                inRepository = true;
                kind = open.group(1);
                id = null;
                url = null;
            }
            if (inRepository && removed) {
                Matcher idMatcher = XML_ID.matcher(line);
                if (idMatcher.find()) {
                    id = idMatcher.group(1);
                }
                Matcher urlMatcher = XML_URL.matcher(line);
                if (urlMatcher.find()) {
                    url = urlMatcher.group(1);
                }
            }
            if (inRepository && REPOSITORY_CLOSE.matcher(line).find()) {
                if (id != null || url != null) {
                    repositories.add(new Repository(kind, id, url, path));
                }
                inRepository = false;
            }
        }
        if (inRepository && (id != null || url != null)) {
            // The hunk ends inside the removed definition.
            repositories.add(new Repository(kind, id, url, path));
        }
        return repositories;
    }

    /**
     * Rewrites every complete XML start tag of the text with its attributes sorted, so that re-serializing a tag with
     * its attributes in another order (as PME does with the {@code <project>} element) compares equal.
     */
    private static String sortingAttributes(CharSequence text) {
        Matcher tag = XML_START_TAG.matcher(text);
        StringBuilder sorted = new StringBuilder();
        while (tag.find()) {
            List<String> attributes = new ArrayList<>();
            Matcher attribute = XML_ATTRIBUTE.matcher(tag.group(2));
            while (attribute.find()) {
                attributes.add(attribute.group());
            }
            Collections.sort(attributes);
            tag.appendReplacement(
                    sorted,
                    Matcher.quoteReplacement("<" + tag.group(1) + " " + String.join(" ", attributes) + tag.group(3)));
        }
        tag.appendTail(sorted);
        return sorted.toString();
    }

    private static boolean isXmlFile(String fileName) {
        return fileName.endsWith(".xml") || fileName.endsWith(".pom") || fileName.endsWith(".xsd");
    }

    private static boolean isGradleFile(String fileName) {
        return fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts");
    }

    private static int count(String count) {
        // An omitted hunk line count means 1.
        return count == null ? 1 : Integer.parseInt(count);
    }

    private static String withoutWhitespace(CharSequence text) {
        return WHITESPACE.matcher(text).replaceAll("");
    }
}
