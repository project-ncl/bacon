---
title: "Build Config Files"
---

* Contents
{:toc}

## Overview

**_Unless specified otherwise, all the keys specified in the yaml are required._**

The build-config.yaml is broken down into 2 big categories: the PNC part and the generation part. The PNC part consists of the configuration on how your builds are performed. Once the builds are done, that information is then fed into the generation part to produce maven repository, javadoc, sources, license zips.

build-config.yaml example file with comments: [build-config.yaml](https://github.com/project-ncl/bacon/blob/main/example-pig-config.yaml)

The basic structure of a build-config.yaml looks as follows:

```yaml
# PNC part
# PNC product part
product:
  name: test-product
  abbreviation: tp
  stage: GA
  issueTrackerUrl: http://issues.jboss.org/browse/TEST
version: 1.0.0
milestone: DR1
brewTagPrefix: fb-1.0-pnc # Optional
group: Test Product 1.0-all
releaseStorageUrl: http://testurl.com/releaseStorage
temporaryBuildAlignmentPreference: PERSISTENT

# PNC builds part
defaultBuildParameters: # Optional: shared between all builds
  project: test-project
  environmentId: 1
  buildScript: mvn clean deploy -DskipTests
builds:
  - name: empty
    scmUrl: https://github.com/michalszynkiewicz/empty.git
    scmRevision: 1.1

# Generation part
outputPrefixes:
  releaseFile: rhempty
  releaseDir: rhempty
flow:
  repositoryGeneration:
    strategy: IGNORE
  licensesGeneration:
    strategy: IGNORE
  javadocGeneration:
    strategy: IGNORE
  sourcesGeneration: # Optional
    strategy: GENERATE
```

## PNC part
### Product, version, milestone, brewTagPrefix, group
The product section describes the product that the builds belong. One example of a product is EAP.

```
product:
  name: test-product
  abbreviation: tp
  stage: GA
  issueTrackerUrl: http://issues.jboss.org/browse/TEST
version: 1.0.0
milestone: DR1
brewTagPrefix: fb-1.0-pnc # Optional
group: Test Product 1.0-all
releaseStorageUrl: http://testurl.com/registry
temporaryBuildAlignmentPreference: TEMPORARY
```
The `version` specifies the product version we are building for, and the `milestone` represents the 'step' at which we are in the version. A typical milestone value is DR1, DR2, etc or ER1, ER2 etc. The combination of the version and milestone forms the full release name of the product: e.g 1.0.0.CR1.

The `releaseStorageUrl` is location of the release storage, typically on rcm-guest staging. This is the url to be used in the 'upload to candidates script' when running the `pig release` command. Using `*` in the milestone means that this property may also be used for automatic increment of milestone value. For example `ER*` as milestone, during `pig run`, the command will check the configured release storage and increment the version to next available milestone version. It is possible to configure it either within the `build-config.yaml` or via the CLI using the flag `--releaseStorageUrl=<...>` with the former taking precedence.

The `brewTagPrefix` is an **optional** key that you can use to override the default generated brew tag prefix associated with a product version. The key is used to specify to which Brew tag all the builds should go when the milestone is closed.

The `temporaryBuildAlignmentPreference` is an option to specify alignment preference for temporary build to be run with: PERSISTENT or TEMPORARY(default). This property will override --tempBuildAlignment=PERSISTENT/TEMPORARY flag passed in as command line option.

Finally the `group` is name of the group config where all the builds defined are grouped together. It is highly recommended to make the group name unique to the version of product / separated projects being built. For example, if the `version` is 1.0.0, then the group name is: `my-beautiful-product-1.0.0`. To make this easier, consider using [YAML variables](#usage-of-yaml-variables). The group config is automatically linked to the product version.

<table bgcolor="#ffff00">
<tr>
<td>
    It is very important to keep <code> group </code> name unique across all your build-config.yaml files in case you're building multiple build-config.yaml configurations in parallel within e.g. CPaaS (Continuous Productization As A Service).
</td>
</tr>
</table>

```yaml
...
version: 1.0.0
milestone: DR1
group: my-beautiful-product-1.0.0
...
```

### defaultBuildParameters and builds
The builds section describes the list of build configurations to use to perform builds. If most / all build configurations share the same attributes (e.g environmentId, builder pod size etc), then you can specify those values in the `defaultBuildParameters` section to reduce repetition.

```yaml
...
builds:
- name: <name of build config>
  project: <project name>
  description: <description> # Optional

  buildScript: <build script: mvn clean deploy>
  buildType: MVN # Specify whether it's MVN, GRADLE, NPM

  scmUrl: <scm URL> # Specify scmUrl

  scmRevision: <scm revision>

  brewPullActive: false #This flag allows the user to specify whether to activate the brew pull for specific build configs. Number of dependencies in Brew used in current PNC builds is pretty low and the feature slows down both alignment and the build itself.

  # Either specify environmentId, environmentName or systemImageId.
  # If more than one is specified, the priority will be: environmentId, systemImageId, environmentName
  environmentId: <id> # The specific id of a build environment, e.g. 20
  environmentName: <env-name> # The name of the builder image, e.g. OpenJDK 1.8; Mvn 3.6.0. As this will pick the latest up-to-date version of the image, it's the recommended option
  systemImageId: <system image id> # The specific builder image name associated with a build environment, e.g. builder-rhel-7-j8-mvn3.6.0:1.0.0

  dependencies: # Optional: specify which builds need to be done before this one. The build must also be part of the 'builds' list in the YAML
  - <dependency 1 by name>
  - <dependency 2 by name>

  buildPodMemory: <number> # Optional: use if you want to override the default memory inside a builder pod. e.g buildPodMemory: 6
  alignmentParameters: <alignment parameters> # Optional: if you want to add parameters to the alignment invocation
  extraRepositories: # Optional: use if you want your build to connect to more Maven repositories
  - http://custom.repository

  buildCategory: STANDARD # Optional: used for managed services builds, specify STANDARD or SERVICE, defaults to STANDARD

  brewBuildName: <brew build name> # Optional: if you wish to push the build to an alternate brew tag
...
```

As you noticed, there's quite a lot of key/values to specify! If most of your builds use, say, the same `buildType`, you can just specify it in the `defaultBuildParameters` section and not in any of the builds:
```yaml
...
defaultBuildParameters:
  buildType: MVN
builds:
- name: awesome-build
  project: awesome-project
```

If another build specifies its own `buildType`, then this overrides the default. Any key/value that you specify for a build can also be specified in the `defaultBuildParameters` section.

#### scmUrl
Nowadays the `scmUrl` can be used to describe both the external, and the internal source of the build.

If the `scmUrl` points to an internal url, it should be in the format:
```yaml
builds:
- name: something
  scmUrl: git+ssh://<internal git server>/<repo>.git
...
```

If the `scmUrl` points to an external url, it should simply use the anonymous Git link:
```yaml
builds:
- name: something
  scmUrl: https://<external git server>/<repo>.git
```

#### environmentId v/s environmentName v/s systemImageId

The build environment used for builds has a unique id, name and system image id.

We allow the user to specify the build environment using either `environmentId`, `environmentName` or the `systemImageId`.

The `environmentId` refers to the exact id of the build environment as stored in the PNC database. This is what has been traditionally used, but it can be problematic since the ids are different between PNC servers (Devel, Stage, Prod), which makes the `build-config.yaml` not very portable.

To overcome the portability issue, it is possible to specify the `systemImageId` instead, which is the same across different PNC servers. Both `environmentId` and `systemImageId` **preserve the docker-image version** of the environment, which means that after some time the version may become deprecated and users may need to update the value every once in a while. (**NOTE**: `systemImageIds` with `:latest` suffix will **no longer be available** in PNC).

The `environmentName` is as well the same between different PNC servers and thus portability across different PNC servers is not a concern. However, the big difference is that environments specified by `environmentName` are always using **the latest docker-image version**. This is important because there will be changes to your build configurations in PNC from time to time, which will cause **rebuilds** of the affected components.

To get the `environmentId`, `environmentName` or the `systemImageId` of the build image, simply run:

```
bacon pnc environment list
```

### Best practices for the naming of builds
We recommend that each build configuration's name uses the format "\<project name\>-\<version being built\>" or "\<GroupId\>-\<ArtifactId\>-\<version\>". This allows you to more easily build different versions of the project without having to edit the same build configuration.

## Generation part
The generation section produces maven repository, javadocs, sources zips. The path where the zips are produced in in `target/<product>-<fullVersion>/`.

### Output prefixes
```yaml
outputPrefixes:
  releaseFile: releasefile
  releaseDir: releasedir
```
The `releaseFile` is used as the prefix for the zips produced in the generation part. Using the example above, the maven repository zip location will be `target/<product>-<fullVersion>/releasefile-<fullVersion>-maven-repository.zip`.

When unzipped, the content of the data will be under the folder name: `releasedir-<fullVersion>-maven-repository`. The `releaseDir` specifies the name of the folder prefix inside the zip.

#### Output Suffix
The `outputSuffix` key can be used to specify a suffix in the generation of zip files and folders. For example, the mavne repository zip name will then be `releasefile-<fullVersion>-<suffix>-maven-repository.zip`, and the folder inside named `releasedir-<fullVersion>-<suffix>-maven-repository`.

`outputSuffix` is optional.

Example:
```yaml
outputSuffix: picard
```


### Flow
```yaml
flow:
  repositoryGeneration:
    strategy: IGNORE
  licensesGeneration: # Optional: default is IGNORE
    strategy: IGNORE
    sourceBuild: <build name> # Optional: used when strategy is DOWNLOAD: same name as specified in the build-config.yaml
    sourceArtifact: <regex filename of license from <build name> # Optional: used when strategy is DOWNLOAD
  javadocGeneration:
    strategy: IGNORE
    sourceBuild: <build name> # Optional: used when strategy is DOWNLOAD: same name as specified in the build-config.yaml
    sourceArtifact: <regex filename of license from <build name> # Optional: used when strategy is DOWNLOAD
  sourcesGeneration: # Optional: default is GENERATE
    strategy: GENERATE
```
The flows describe the implementation on how the license, repository, javadoc, and sources generation are to be performed. The choice of the implementation depends on the requirements of your product.

The `sourcesGeneration` by default is set to `GENERATE`.

#### Maven Repository generation
Strategy options are:
- IGNORE
- GENERATE
- DOWNLOAD
- BUILD_CONFIGS
- BUILD_GROUP
- MILESTONE
- RESOLVE_ONLY

The `IGNORE` strategy won't produce any repository. This is particularly useful to temporarily disable generation as you can leave the rest of your configuration in place.

The `GENERATE` strategy generates repository based on build. You need to specify `sourceArtifact` and `sourceBuild`. `sourceArtifact` is a regular expression pattern for the file name of a BOM that is used for repository generation, `sourceBuild` is name of build configuration with build for BOM.

`Optional parameters:`
- `additionalRepo` - used to specify additonal maven repositories, e.g. for Quarkus build from master where we do a fake release to repository.engineering.redhat.com; used only for temp builds
- `ignored` - set of artifact ids (dependencies in the BOM) to ignore, i.e. not copy from BOM to the generated project's pom


The `DOWNLOAD` strategy generates repository based on artifact. You need to specify `sourceArtifact` and `sourceBuild`. Specified artifact is downloaded and repackaged.

The `BUILD_CONFIGS` strategy generates repository based on specified build configurations. You need to specify `sourceBuilds`. Redhat artifacts are exctracted from last successful build of specified build configurations, parent poms, `additionalArtifacts`, missing sources, checksums are added and packaged into repository.

The `BUILD_GROUP` strategy generates repository for builds included in pnc build group. This produces a repository with build and runtime dependencies that are based on an amalgamated list of dependencies retrieved from the builds in a group build.

You need to specify `group`. Redhat artifacts are then sorted from builds included in `group`, parent poms, `additionalArtifacts`, missing sources, checksums are added and packaged into repository.

`Optional parameters:`
- `excludeSourceBuilds` - used to specify the list of build configurations that you don't want to be included in the generated repository.

The `MILESTONE` - not implemented yet

The `RESOLVE_ONLY` strategy resolves a list of artifacts against a BOM. The resolution is transitive, i.e. direct (except optional) and transitive dependencies of the listed artifacts are included in the repo zip as well.

 **Necessary configuration**

In order to RESOLVE_ONLY strategy to work, three things need be defined:

1. `sourceBuild` - the PNC build to take the `sourceArtifact` from
1. `sourceArtifact` - the BOM against which the artifacts should be resolved
2. The list of artifacts to resolve against the BOM via one or more of the following options:

    - `parameters.extensionsListUrl` - a URL referring to a text file containing the list of artifacts in the format `groupId:artifactId:version:type:classifier` (type and classifier are optional)
    - `parameters.resolveIncludes` and `parameters.resolveExcludes` - these are comma separated lists of artifact patterns to filter out from the given BOM (that is transformed to an effective pom before the evaluation). The patterns are of the form `groupIdPattern:[artifactIdPattern:[[typePatter:classifierIdPattern]:versionPattern]]`. The subpatterns can contain string literals combined with wildcard `*`.

It is possible to set both `parameters.extensionsListUrl` and `parameters.resolveIncludes`. In such a case the union of both sets will be used.

`build-config.yaml` example:

 ```yaml
   repositoryGeneration:
     strategy: RESOLVE_ONLY
     sourceBuild: io.quarkus-quarkus-platform-{{productVersion}}
     sourceArtifact: 'quarkus-product-bom-[\d]+.*.pom'
     parameters:
       extensionsListUrl: "http://link.to.txt.file"
       resolveIncludes: "*:*:*redhat-*" # get all artifacts from the BOM that have redhat- substring in their versions
       resolveExcludes: "io.netty:*:*:linux-aarch_64:*,io.netty:*:*:osx-*:*" # but ignore all netty artifacts with linux-aarch_64 and osx-* classifiers
 ```

`extensionsListUrl` example:

 ```
 io.quarkus:quarkus-bom-quarkus-platform-properties:1.11.6.Final:properties
 io.quarkus:quarkus-logging-json-deployment:1.11.6.Final
 io.quarkus:quarkus-smallrye-opentracing-deployment:1.11.6.Final
 ```


##### Parameters available in all strategies

- `additionalArtifacts` - artifacts to be specifically added to the repository from specific builds. Should use regular expressions to match any incremental suffixes.
- `externalAdditionalArtifacts` - artifacts from other builds, not within this product's build group. This list should contain artifact identifiers, as shown in the PNC UI.

  Example:
  ```
  repositoryGeneration:
    strategy: BUILD_GROUP
    externalAdditionalArtifacts:
      - 'com.google.guava:guava-parent:pom:18.0.0.redhat-1'
  ```

- `excludeArtifacts` - exclude artifacts based on a pattern

  Example:
  ```
  repositoryGeneration:
    strategy: BUILD_GROUP
    excludeArtifacts:
      - '.*:war:.*'
      - '.*:zip.*'
  ```

The artifact information is specified in the format of `groupId:artifact:artifactId:type:classifier`.

##### Additional filter available in BUILD_CONFIGS and BUILD_GROUP strategies

When using BUILD_CONFIGS and BUILD_GROUP strategies additional filter `filterArtifacts` is available. You can use it to reduce amount of artifacts in your repo e.g. filter artifacts and download only ones you're interested in. If activated (by adding some artifacts into array) only artifacts matching filter are included in repo generation process.

Artifact information/wildcards should be specified in the format of `groupId:artifact:artifactId:type:classifier`.

#### licenses generation
Strategy options are:
- IGNORE (default)
- GENERATE
- DOWNLOAD

The `IGNORE` strategy won't produce any license zips.

The `GENERATE` strategy generates the license information based on the artifacts in the builds. It requires that the repository generation is **not** ignored.

The `DOWNLOAD` strategy assumes that the license information for all the builds is already provided inside an artifact produced by a specific build. To use it, you also need to specify the `sourceBuild` and the `sourceArtifact`. The artifact will be repackaged into the final zip. The `sourceBuild` is actually the name of one of the build configs listed in the `build-config.yaml`. The `sourceArtifact` is the **regex** for the ***filename*** of the artifact containing the license information. A partial match for the `sourceArtifact` is sufficient.

Example:
```yaml
flow:
  licensesGeneration:
    strategy: DOWNLOAD
    sourceBuild: empty-1.0.0
    sourceArtifact: empty-licenses-1.0.0
```

#### Javadoc generation
Strategy options are:
- IGNORE
- GENERATE
- DOWNLOAD

The `IGNORE` strategy won't produce any javadoc

The `GENERATE` strategy generates javadocs based on sources artifact in the builds. It requires that the repository generation is **not** ignored.

The `DOWNLOAD` strategy is identical to that of the license generation, except that we specify the artifact which contains all the javadocs.

#### Sources generation
Strategy options are:
- IGNORE
- GENERATE (default)
- GENERATE_EXTENDED
- GENERATE_SELECTED

The `IGNORE` strategy won't produce any sources zip.

The `GENERATE` strategy get sources from builds.

The `GENERATE_EXTENDED` strategy get same result as `GENERATE` and add sources of unreleased dependencies.

The `GENERATE_SELECTED` strategy get sources from selected `sourceBuild`.

All `GENERATE*` strategies require that the repository generation is **not** ignored.

## Usage of YAML variables
You can define variables in your YAML file for injection at various points. This is useful when there are few changes between version releases (e.g tags). To define a variable, use this format:
```yaml
#!variable=value
#!variable_next={{variable}} value2
#milestone=ER5
...
group: product-x-all
milestone: {{milestone}}
...
```
From the above, we can also define the variable `variable_next` using another variable. To use the variable in the file, put in your yaml: `{{variable}}`.

You can also define or override the value of the variable in the YAML by using the `bacon pig` option '-e' or '--env'.

```bash
bacon pig run -e variable="alternate value"  --env variable2=value3 ...
```

To test if the variables are being substituted / overridden properly, `bacon pig` provides a command to print the final build-config.yaml. The command is:
```
bacon pig pre-process-yaml <folder to build-config.yaml>

// if you are defining the variable value in the CLI options:
bacon pig pre-process-yaml -e variable1=value1 <folder to build-config.yaml>
```
