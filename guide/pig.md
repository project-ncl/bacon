---
title: "PiG"
---

* Contents
{:toc}

# Introduction

PiG allows users to setup PNC with the product, build configurations, and much more via a YAML file. This makes it much easier to configure PNC for new releases as well as making the configuration portable across different PNC servers.

# Usage
PiG is run in multiple phases:

- configure
- build
- repo
- licenses
- javadocs
- sources
- shared-content
- docs
- scripts
- addons

The all-in-one phase that combines all the phases is:

- run

The `configure` phase pushes all the settings in the YAML file to PNC, and the `build` phase tells PNC to build everything.

```bash
bacon pig <phase> ...
```


# Configuration

The application is configured via the `build-config.yaml` file.

A PiG `build-config.yaml` looks like [this](https://github.com/project-ncl/bacon/blob/master/example-pig-config.yaml). See [this](build-config.html) for a detailed guide of `build-config.yaml`

<table bgcolor="#ffff00">
<tr>
<td>
    <b>TODO</b> add a command that allows users to specify variables via cli
</td>
</tr>
</table>


# Operation

In all of the operations above, you can override the variables in your `build-config.yaml` by specifying the `-e` or `--env` flag. More details [here](build-config.html#usage-of-yaml-variables)

## Configure PNC Entities

Usage:
```bash
bacon pig configure <directrory containing build-config.yaml>
```

## Building

Usage:
```bash
bacon pig build <directrory containing build-config.yaml>
```

You can specify if you want temporary builds or not with the `-t` flag.

## Deliverables

This section covers the required deliverables for a product release:

- licenses
- maven-repository
- src zip
- javadoc zip

<table bgcolor="#ffff00">
<tr>
<td>
    <b>TODO</b> describe each section more
</td>
</tr>
</table>

### Maven Repository

You can generate a repository by adding a `repositoryGeneration` section to your `build-config.yaml`. The intended generation behaviour can be configured using the appropriate strategy. You can also optionally explicitly include or exclude artifacts based on a pattern.

A simple example configuration is:

```
repositoryGeneration:
  strategy: BUILD_GROUP
```

The above will build a repository based on the PNC build group.

#### Strategies


<table bgcolor="#ffff00">
<tr>
<td>
    <b>TODO</b> Add details about additional strategies and options.
</td>
</tr>
</table>

PiG supports various strategies for generating a maven repository:

- **BUILD_GROUP**
You can use the `BUILD_GROUP` strategy to generate a repository based on the PNC group config. This produces a repository with build and runtime dependencies that are based on an amalgamated list of dependencies retrieved from the builds in a group build.

- **IGNORE**
Setting the strategy to `IGNORE` will disable repository generation. This is particularly useful to temporarily disable generation as you can leave the rest of your configuration in place.

#### Excluding Artifacts

You can exclude artifacts based on a pattern added to an `excludeArtifacts` section, an example configuration utilising this to exclude war and zip artifacts:

```
repositoryGeneration:
  strategy: BUILD_GROUP
  excludeArtifacts:
    - '.*:war:.*'
    - '.*:zip.*'
```

#### Including Additional Artifacts

You can include artifacts by adding an `externalAdditionalArtifacts` section and specifying them in the format of `groupId:artifact:artifactId:type:classifier`, an example configuration to add an additional artifact:

```
repositoryGeneration:
  strategy: BUILD_GROUP
  externalAdditionalArtifacts:
    - 'com.google.guava:guava-parent:pom:18.0.0.redhat-1'
```


## Add ons

### Offliner Manifest

PiG is able to generate an offliner manifest in plain text compatible with the offliner tool. You can find more information about the tool [here](https://release-engineering.github.io/offliner/). This also takes into account any explicit inclusions or exclusions you may have specified as part of your `repositoryGeneration` step.

Adding the following to the bottom of your `build-config.yaml` will generate `offliner.txt` inside the target directory.

```
addons:
  offlineManifestGenerator:
    offlineManifestFileName: offliner.txt

```
