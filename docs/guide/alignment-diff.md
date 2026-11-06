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
| `*-1-build.diff` | yes | The alignment itself: dependency, plugin and property changes, generated files, anything not recognized |
| `*-2-rerun-state.diff` | yes for Gradle/sbt | GME/SMEG files that apply the alignment at build time (see below) |
| `*-3-reproduce.diff` | no | Project version rewrites and removed repositories, to match the PNC build exactly |
| `*-4-noise.diff` | no | PME marker, reports and backups, formatting-only changes: never affects the build |

Every part applies on its own on top of the upstream sources; applying all of them, in any order, reproduces the
complete alignment. Applying them in numeric order is recommended:

```bash
git apply BRJO2LNIL6QAA-alignment-1-build.diff BRJO2LNIL6QAA-alignment-2-rerun-state.diff
```

Each file starts with comments describing its content, when it can be skipped and the project version change. A
file grouping several categories has one commented section per category.

# How the manipulators apply the alignment

The manipulators apply the alignment in two different ways, which is why the rerun-state part mentions GME and SMEG
but not PME:

* **PME (Maven) rewrites the `pom.xml` files directly.** The aligned dependency, plugin and property versions are
  plain edits of the POMs, so they land in the build part; the project version rewrite lands in the reproduce part.
  PME leaves nothing behind that the build needs: its leftovers are the marker
  (`target/pom-manip-ext-marker.txt`, which makes a later PME run skip the alignment) and the
  `target/manipulation.json` report, both in the noise part.
* **GME (Gradle) and SMEG (sbt) mostly do not rewrite the build files.** GME writes `manipulation.json`, copies
  `gme.gradle` (and `gme-pluginconfigs.gradle`, `gradle/gme-repos.gradle`) and injects `apply from` lines into the
  build files to load them; at build time the GME plugin reads the aligned versions from `manipulation.json`. SMEG
  does the same through `manipulations.sbt`, which sbt loads automatically. For these, the rerun-state part
  effectively *is* the alignment.

So PME changes never go to the rerun-state part: it is required for Gradle/sbt builds and simply absent for Maven
builds. Leave it out only when running GME/SMEG again yourself, as they must start from the upstream state.

# Project version

The project version change is reported in the same way whatever the manipulator, on a
`# Project version: <original> -> <aligned> (<files>)` line at the top of every part, and in the command output. It
is read from:

| Manipulator | Source | Part holding the change |
|-------------|--------|-------------------------|
| PME | the version rewritten in the `pom.xml` files | reproduce |
| GME | `originalVersion` and `version` in `manipulation.json` | rerun-state |
| SMEG | `version :=` in `manipulations.sbt` (the original version is not recorded) | rerun-state |

The GME and SMEG files are kept whole, as the build needs them as a whole, so only the PME version change can be
skipped on its own.
