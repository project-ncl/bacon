---
title: "Experimental features"
---

* Contents
{:toc}

# Experimental Features
These are experimental Bacon features. These commands are provided "as is" and they can be changed or removed in the future. To enable use of experimental commands, you need to acknowledge this warning and add `enableExperimental=true` to your configuration profile.

# Dependency generator

Automated dependency Build Config generator, also known as Autobuilder.

## Running the generator

To run it analyzing artifacts released to public repositories use this:
```shell
bacon experimental dependency-generator generate <config>
```
To run it analyzing local project, add the `--project-dir=<path>` parameter pointing to the location of Maven project.

The `generate` command will output content that can be used in [PiG](pig.html) `build-config.yaml` file. You can then use `bacon pig` commands with the file to run the builds in PNC.

## Config file
Example of config file:
```yaml
dependencyResolutionConfig: # Configuration for the dependency resolution by Domino
    analyzeArtifacts: # List of toplevel artifacts that should be analyzed and built
        - foo:bar:1.0.0
    analyzeBOM: "foo:bar-bom:1.0.0" # Bom that should be analyzed and which artifacts should be built
    excludeArtifacts: # List of artifacts that should be excluded from the analysis
        # The matching artifact and it's dependencies will be ommited from the resulting build tree
        # The format is G:A:[C:T:]V where every part can be replaced with '*'
        - org.apache.maven:*:*
    includeArtifacts: # List of artifacts that should not be excluded from the analysis
        # The format is G:A:[C:T:]V where every part can be replaced with '*'
        - foo:*:*
    includeOptionalDependencies: true # Wheather optional dependencies should be included in the analysis. Defaults to true

buildConfigGeneratorConfig: # Configuration for how the new build configs should be created
    defaultValues: # Default values to be used when generating build config
            environmentName: "OpenJDK 11.0; Mvn 3.5.4" # Name of the environment to be used when generating new build config. Required.
            buildScript: "mvn deploy" # Build script to be used when generating new build config. Defaults to given example.
            scmUrl: "https://github.com/michalszynkiewicz/empty.git" # Placeholder SCM URL
            # to be used when no SCM url was found for the project. We need it to generate valid build-config. Defaults to given example.
            scmRevision: "master" # Placeholder SCM revision to be used when using placeholder URL. Defaults to given example.
    scmReplaceWithPlaceholder: # SCM URLs which contain one of the string listed will be replaced by the default placeholder scmUrl
         - "svn.example.com"
    scmPattern: # SCM URLs will have the listed key substring replaced with the value
        "git@github.com:": "https://github.com/" # "git@github.com:foo/bar.git" -> "https://github.com/foo/bar.git"
    scmMapping: # SCM URLs containing the listed key are wholy replaced by the value
        "svn.example.com/foo-bar": "https://git.example.com/foo/bar.git" # "https://svn.example.com/foo-bar/branch" -> "https://git.example.com/foo/bar.git"
    failGeneratedBuildScript: true # When generating new or copying and changing existing build config, will part to build script to make sure the build fails
    # This can be helpful to make sure the configs are manually reviewed
    pigTemplate: # Template of the PiG build-config.yaml to use
        product:
            name: Autobuild Example
            abbreviation: ABX
            stage: Alpha
            issueTrackerUrl: https://issues.example.com/projects/ABX
        version: 0.0.1
        milestone: DR1
        group: autobuild-example
        builds:
            -
        flow:
            licensesGeneration:
                strategy: IGNORE
            repositoryGeneration:
                strategy: IGNORE
            javadocGeneration:
                strategy: IGNORE
            sourcesGeneration:
                strategy: IGNORE
        outputPrefixes:
            releaseFile: ab-example
            releaseDir: ab-example
        addons:
            notYetAlignedFromDependencyTree:
```

## Generation strategy
The strategy for creating Build Configs goes like this:
- For each project identified by the dependency analysis, we try to find if it was already built in PNC in the same or simmilar version.
- If we found that a project version was already build we create a copy of the build config with some changes:
    - strip any `-DdependencyOverride` from the alignment parameters.
    - **Note:** It may be that the original build config did not have dependecies setup correctly so we want a copy that has the depenencies right. Even if the original had correct dependencies, for autobuilder we now want to use one "static" or "stable" build config per project version as we may not have the influence over the original which could be updated to build different version.
 - If we find that a project was build in different version, we find closest built version and create a copy of the build config and doing some changes:
     - changing the SCM revision
     - strip any `-DdependencyOverride` from the alignment parameters.
 - If we found that given artifact was not built at all, we generate new build config

We expect there could be multiple build config generation strategies, but it's something that we need to distill from prod engineers experience. We expect lots of feature request from you.

### Why strip `dependencyOverride`?
Autobuilder generates the whole dependency tree (unless excluded) of build configs and the goal is to build all the dependecies. The automatic alignment then aligns dependencies to the artifacts produced by builds down the tree. Having `dependencyOverride` would break the alignment on the dependencies introduced by autobuilder.

## Examples
### Analysis of BOM project
```shell
bacon experimental dependency-generator generate vertx-autobuilder.yaml > vertx/build-config.yaml
```
`vertx-autobuilder.yaml`:
```yaml
dependencyResolutionConfig:
    analyzeBOM: io.vertx:vertx-dependencies:4.3.7
    excludeArtifacts:
        - com.sun:tools:*
buildConfigGeneratorConfig:
    defaultValues:
        environmentName: "OpenJDK 11.0; Mvn 3.5.4"
    pigTemplate:
        product:
            name: Autobuild Vert.x Example
            abbreviation: ABX-VX
            stage: Alpha
            issueTrackerUrl: https://issues.example.com/projects/ABX-VX
        version: 4.3.7
        milestone: CR1
        group: autobuild-example-vertx
        flow:
            licensesGeneration:
                strategy: IGNORE
            repositoryGeneration:
                strategy: IGNORE
            javadocGeneration:
                strategy: IGNORE
            sourcesGeneration:
                strategy: IGNORE
        outputPrefixes:
            releaseFile: ab-example
            releaseDir: ab-example
        addons:
            notYetAlignedFromDependencyTree:
```
### Analysis of artifacts
```shell
bacon experimental dependency-generator generate bacon-autobuilder.yaml > bacon/build-config.yaml
```
`bacon-autobuilder.yaml`:
```yaml
dependencyResolutionConfig:
    analyzeArtifacts:
        - org.jboss.pnc.bacon:parent:2.4.1
        - org.jboss.pnc.bacon:cli:2.4.1
    excludeArtifacts:
        - org.apache.maven:*:*
    includeArtifacts:
        - org.apache.maven:maven-settings-builder:*
buildConfigGeneratorConfig:
    defaultValues:
        environmentName: "OpenJDK 11.0; Mvn 3.5.4"
    pigTemplate:
        product:
            name: Autobuild Bacon Example
            abbreviation: ABX-B
            stage: Alpha
            issueTrackerUrl: https://issues.example.com/projects/ABX-B
        version: 2.4.1
        milestone: CR1
        group: autobuild-example-bacon
        flow:
            licensesGeneration:
                strategy: IGNORE
            repositoryGeneration:
                strategy: IGNORE
            javadocGeneration:
                strategy: IGNORE
            sourcesGeneration:
                strategy: IGNORE
        outputPrefixes:
            releaseFile: ab-example
            releaseDir: ab-example
        addons:
            notYetAlignedFromDependencyTree:
```
### Analysis of local project
```shell
bacon experimental dependency-generator generate --project-dir=~/projcets/example/ local-autobuilder.yaml > local/build-config.yaml
```
`local-autobuilder.yaml`:
```yaml
buildConfigGeneratorConfig:
    defaultValues:
        environmentName: "OpenJDK 11.0; Mvn 3.5.4"
    pigTemplate:
        product:
            name: Autobuild Local Example
            abbreviation: ABX-L
            stage: Alpha
            issueTrackerUrl: https://issues.example.com/projects/ABX-L
        version: 1.0.0
        milestone: CR1
        group: autobuild-example-bacon
        flow:
            licensesGeneration:
                strategy: IGNORE
            repositoryGeneration:
                strategy: IGNORE
            javadocGeneration:
                strategy: IGNORE
            sourcesGeneration:
                strategy: IGNORE
        outputPrefixes:
            releaseFile: ab-example
            releaseDir: ab-example
        addons:
            notYetAlignedFromDependencyTree:
```
