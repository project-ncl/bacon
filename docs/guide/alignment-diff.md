---
title: "Alignment Diff"
---

* Contents
{:toc}

# Introduction

During alignment PNC (through Reqour) runs a manipulator on the upstream sources and commits the result on top of
the original revision. `bacon pnc build get-alignment-diff` captures those changes as a diff that applies on top of
the original upstream sources, so that the PNC build can be reproduced or reviewed elsewhere.

```bash
bacon pnc build get-alignment-diff BRJO2LNIL6QAA
```

# The parts

The diff is split into up to four files, numbered in apply order. Only the files with changes are written.

| File | Required | Content |
|------|----------|---------|
| `*-1-build.diff` | yes | The alignment itself: dependency, plugin and property changes, project version rewrites, generated files, anything not recognized |
| `*-2-rerun-state.diff` | yes for Gradle/sbt | GME/SMEG files that apply the alignment at build time (see below) |
| `*-3-reproduce.diff` | no | Removed repositories, to match the PNC build exactly |
| `*-4-noise.diff` | no | PME marker, PME stamp, reports and backups, formatting-only changes: never affects the build |

Every part applies on its own on top of the upstream sources; applying all of them, in any order, reproduces the
complete alignment. Applying them in numeric order is recommended:

```bash
git apply BRJO2LNIL6QAA-alignment-1-build.diff BRJO2LNIL6QAA-alignment-2-rerun-state.diff
```

Each file starts with comments describing its content, when it can be skipped, the project and its version change. A
file grouping several categories has one commented section per category. The comments also list the removed
repositories, the added files no rule recognized (kept in the build part) and any warning; `git apply` and `patch`
ignore them.

The reproduce part only holds the repositories PNC removed from the `pom.xml` files, so that the build resolves from
the PNC repository only. Apply it to reproduce the PNC build exactly; skip it when building outside PNC, where the
removed repositories may still be needed to resolve the dependencies.

# Internal repositories

The diff is read from the build's internal repository on GitHub or GitLab (see
[the configuration guide](configuration.html#internal-scm-repositories-github-and-gitlab) for the tokens). GitLab does
not return binary files, nor patches over its size limit, in a diff: bacon reads those files through the raw file API
and writes them as text hunks or `GIT binary patch` blocks, so the parts still apply. Files that cannot be read, larger
than 100 MiB, or read after 512 MiB of such files for the same diff, stay as `Binary files ... differ` and are listed in
a warning; they cannot be applied from the diff.

# Secrets in the diff

Known credentials are masked only in the metadata files the manipulators write (reports such as
`alignmentReport.json` and `manipulation.json`, and their backups), so that the source and build files stay
byte-exact and the diff applies. **A secret committed in a source or build file of the repository appears in the diff
as it is.** Review the diff before sharing it outside the people who can read the internal repository.

# Options

* `--output <file>`: the base name of the part files, `<build ID>-alignment.diff` by default. The parts are named
  after it, e.g. `--output out/widget.diff` writes `out/widget-1-build.diff`, `out/widget-3-reproduce.diff`, ...
* `--manifest`: also writes `<base name>-manifest.json`, describing the split for review or tooling. It is not
  needed to apply the parts. It contains:
  * `project`: the project identifier, as on the `# Project:` line (see below);
  * `alignedVersions`: the aligned versions of the project's own modules, used to recognize the project version
    rewrites, read from the alignment reports in the diff or from the PNC build;
  * `alignedProjects`: the modules of the project recorded by the alignment reports, with their `groupId`,
    `artifactId`, `originalVersion` and `alignedVersion` (SMEG only records the aligned version);
  * `alignedDependencies`: the dependencies and plugins recorded as aligned by the PME and GME reports, with their
    `type` (`dependency`, `plugin`, `managed-dependency` or `managed-plugin`), `groupId`, `artifactId`,
    `originalVersion`, `alignedVersion` and the `modules` where they were aligned. PME writes its report under
    `target/`, so it is only in the diff, and in this list, when the project does not ignore `target/`;
  * `parts`: per category, the part number and file, whether it is present and required, and the same descriptions
    as the comments;
  * `summary`: the number of files and changes per category;
  * `files`: every changed file with its status and, for the whole file or for each hunk, the category and the rule
    that chose it (e.g. `aligned-version` for a project version rewrite and `alignment` for the other changes, both
    in the build part);
  * `removedRepositories`: the kind (`repository` or `pluginRepository`), id, url and file of every removed
    repository;
  * `projectVersions`: the project version changes, with the files holding them;
  * `unrecognizedAddedFiles` and `warnings`.

# How the manipulators apply the alignment

The manipulators apply the alignment in two different ways, which is why the rerun-state part mentions GME and SMEG
but not PME:

* **PME (Maven) rewrites the `pom.xml` files directly.** The aligned dependency, plugin and property versions are
  plain edits of the POMs, and so is the project version rewrite: they all land in the build part. PME leaves
  nothing behind that the build needs: its leftovers are the marker (`target/pom-manip-ext-marker.txt`, which makes
  a later PME run skip the alignment), the stamp comment appended to the POMs and the `target/manipulation.json`
  report, all in the noise part.
* **GME (Gradle) and SMEG (sbt) mostly do not rewrite the build files.** GME writes `manipulation.json`, copies
  `gme.gradle` (and `gme-pluginconfigs.gradle`, `gradle/gme-repos.gradle`) and injects `apply from` lines into the
  build files to load them; at build time the GME plugin reads the aligned versions from `manipulation.json`. SMEG
  does the same through `manipulations.sbt`, which sbt loads automatically. For these, the rerun-state part
  effectively *is* the alignment.

So PME changes never go to the rerun-state part: it is required for Gradle/sbt builds and simply absent for Maven
builds. Leave it out only when running GME/SMEG again yourself, as they must start from the upstream state.

# Project version

The project version change is reported in the same way whatever the manipulator, on a
`# Project version: <original> -> <aligned>` line at the top of every part, and in the command output. The files
holding it are listed in the manifest (`--manifest`). It is preceded by a `# Project: <groupId>:<artifactId>` line
identifying the project: the root module recorded by the alignment report in the diff (the PME execution root, the
GME root project), else the name PNC gave the build (its `BREW_BUILD_NAME` attribute), else `unknown`. The project
version is read from:

| Manipulator | Source | Part holding the change |
|-------------|--------|-------------------------|
| PME | the version rewritten in the `pom.xml` files | build |
| GME | `originalVersion` and `version` in `manipulation.json` | rerun-state |
| SMEG | `version :=` in `manipulations.sbt` (the original version is not recorded) | rerun-state |

The project version change is part of the alignment whatever the manipulator, and is never optional: a project often
uses its own version elsewhere (e.g. a property referencing its sibling modules), which the manipulator aligns
together with the other properties, so the build part does not build without the version rewrite.
