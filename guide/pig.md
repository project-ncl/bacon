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

The above will build a repository based on the PNC build group. For a more detailed explanation of the various strategies for the maven repository generation, see [here](build-config.html#maven-repository-generation)

## Repository Packaging Strategies

- ### RESOLVE ONLY
    This strategy takes the entry points of the artifacts/extensions of of the product which are supposed to be packaged in repository, resolves the artifact, resolves artifacts managed dependencies. It also takes care of the transitives and package them as well.
  
    **Necessary configuration**
    
    In order to RESOLVE_ONLY strategy to work it requires a link to txt file which contains the artifact or extensions in the format of
    `groupId:artifactId:version:type:classifier` (type and classifier are optional)
  
    *For example*
  
    In build-config.yaml
  
    ```
      repositoryGeneration:
        strategy: RESOLVE_BOM_ONLY
        parameters:
          extensionsListUrl: "http://link.to.txt.file"
    ``` 
    *Text file sample*
    ```
    io.quarkus:quarkus-bom-quarkus-platform-properties:1.11.6.Final:properties
    io.quarkus:quarkus-logging-json-deployment:1.11.6.Final
    io.quarkus:quarkus-smallrye-opentracing-deployment:1.11.6.Final
    
    ```
    **Additional Information**
    Resolve artifacts/extensions in the scope of a bom file
    In order to resolve the artifacts/extensions in scope of your product bom file, fill `sourceBuild`  and `sourceArtifact` and RESOLVE_ONLY strategy will resolve in the scope of your product bom
    
    For example
    ```
      repositoryGeneration:
        strategy: RESOLVE_BOM_ONLY
        sourceBuild: io.quarkus-quarkus-platform-{{productVersion}}
        sourceArtifact: 'quarkus-product-bom-[\d]+.*.pom'
        parameters:
          extensionsListUrl: "https://gitlab.cee.redhat.com/rmaity/build-configurations/-/raw/resolvePlay/Quarkus/extensionArtifactList.txt"
    ```


## Add ons

### Offliner Manifest

PiG is able to generate an offliner manifest in plain text compatible with the offliner tool. 
You can find more information about the tool [here](https://release-engineering.github.io/offliner/). 
This also takes into account any explicit inclusions or exclusions you may have specified as part of your `repositoryGeneration` step.
When the repository generation is set to BUILD_GROUP or to IGNORE then the `excludeSourceBuilds` optional parameter will be used, 
if it is defined, to exclude the builds specified with it.

Adding the following to the bottom of your `build-config.yaml` will generate `offliner.txt` inside the target directory.

```
addons:
  offlineManifestGenerator:
    offlineManifestFileName: offliner.txt

```

### Vertx Artifact Finder

This Add-on automates the process of creating vertx artifacts list which are used in Quarkus. 

Now, in order to create a list of vertx artifacts, you don't need to manually search for it in the project, this addon will do the work for you.

Adding the following to the bottom of your `build-config.yaml` will generate `vertxList.txt` inside the target directory.

```
addons:
   vertxArtifactFinder:

```