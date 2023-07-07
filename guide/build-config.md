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
    strategy: GENERATE_ADDITIONAL_SELECTED
    additionalExternalSources:
      - org.drools-drools-8.32.0.Final
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

The `temporaryBuildAlignmentPreference` is an option to specify alignment preference for temporary build to be run with: PERSISTENT or TEMPORARY(default). This property will override `--tempBuildAlignment=PERSISTENT|TEMPORARY` flag passed in as command line option.

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

  alignmentParameters: # Optional: if you want to add parameters to the alignment invocation - see https://github.com/project-ncl/project-manipulator/blob/master/README.md#usage for syntax and other flags
  # These 3 flags are the default behaviour if none are set. BREW_BUILD_VERSION in build-config will match x.y.z-redhat-00001
  # and Post-alignment SCM Tag will match x.y.z-redhat-00001-gitcommmitSHA
  - '-DversioningStrategy=HYPHENED'
  - '-DversionIncrementalSuffix=redhat'
  # PNC forces default padding=5
  - '-DversionIncrementalSuffixPadding=5'
  # can also set a package scope if applicable
  - '-DpackageScope=redhat'
  extraRepositories: # Optional: use if you want your build to connect to more Maven repositories
  - http://custom.repository

  buildCategory: STANDARD # Optional: used for managed services builds, specify STANDARD or SERVICE, defaults to STANDARD

  brewBuildName: <brew build name> # Optional: if you wish to push the build to an alternate brew tag

  parameters: #generic parameters 
    parameter: "value"
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

#### dependencies
```
builds:
- name: build-1
  ...
- name: build-2
  ...
  dependencies:
  - build-1  
```

You can specify which builds need to be done before the build dependency is specified for. Dependency build must also be part of the 'builds' list in the build-configuration.yaml

#### parameters

```
builds:
- name: build-1
  parameters:
     BPM_SERVER: "RH-DEV"
```

You can specify generic parameters without specific property to set them directly.

### Best practices for the naming of builds
We recommend that each build configuration's name uses the format "\<project name\>-\<version being built\>" or "\<GroupId\>-\<ArtifactId\>-\<version\>". This allows you to more easily build different versions of the project without having to edit the same build configuration.

## Generation part
The generation section produces maven repository, javadocs, sources zips. The path where the zips are produced in in `target/<product>-<fullVersion>/`.

`<product>` is generated by using the product's name from `build-config.yaml` to lower case, and replacing spaces with `-`.

`<fullVersion>` is generated by combining the version and the milestone in your `build-config.yaml`: `<version>.<milestone>`

Example:
```yaml
# build-config.yaml
product:
  name: Hello WORld
  abbreviation: tp
  stage: GA
  issueTrackerUrl: http://example.com
version: 1.0.0
milestone: DR1
group: Test Product 1.0-all
```
The `<product>` becomes: `hello-world`.

The `<fullVersion>` becomes: `1.0.0.DR1`

### Output prefixes
```yaml
outputPrefixes:
  releaseFile: releasefile
  releaseDir: releasedir
```
The `releaseFile` is used as the prefix for the zips produced in the generation part. Using the example above, the maven repository zip location will be `target/<product>-<fullVersion>/releasefile-<fullVersion>-maven-repository.zip`.

When unzipped, the content of the data will be under the folder name: `releasedir-<fullVersion>-maven-repository`. The `releaseDir` specifies the name of the folder prefix inside the zip.

#### Output Suffix
The `outputSuffix` key can be used to specify a suffix in the generation of zip files and folders. For example, the maven repository zip name will then be `releasefile-<fullVersion>-<suffix>-maven-repository.zip`, and the folder inside named `releasedir-<fullVersion>-<suffix>-maven-repository`.

`outputSuffix` is optional.

Example:
```yaml
outputSuffix: picard
```


### Flow
<table bgcolor="#ffff00">
<tr>
<td>
Flow part is mainly for maven type of project output, e.g. maven, gradle, scala builds. NPM Projects could set all strategies to IGNORE or modify generation to get empty repo to which they manually download needed packages through additional repo options.
</td>
</tr>
</table>

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

##### `IGNORE`

The `IGNORE` strategy won't produce any repository. This is particularly useful to temporarily disable generation as you can leave the rest of your configuration in place.

The `GENERATE` strategy generates repository based on build. You need to specify `sourceArtifact` and `sourceBuild`. `sourceArtifact` is a regular expression pattern for the file name of a BOM that is used for repository generation, `sourceBuild` is name of build configuration with build for BOM.

`Optional parameters:`
- `additionalRepo` - used to specify additonal maven repositories, e.g. for Quarkus build from master where we do a fake release to repository.engineering.redhat.com; used only for temp builds
- `ignored` - set of artifact ids (dependencies in the BOM) to ignore, i.e. not copy from BOM to the generated project's pom

##### `DOWNLOAD`

The `DOWNLOAD` strategy generates repository based on artifact. You need to specify `sourceArtifact` and `sourceBuild`. Specified artifact is downloaded and repackaged.

##### `BUILD_CONFIGS`

The `BUILD_CONFIGS` strategy generates repository based on specified build configurations. You need to specify `sourceBuilds`. Redhat artifacts are exctracted from last successful build of specified build configurations, parent poms, `additionalArtifacts`, missing sources, checksums are added and packaged into repository.

##### `BUILD_GROUP`

The `BUILD_GROUP` strategy generates repository for builds included in pnc build group. This produces a repository with build and runtime dependencies that are based on an amalgamated list of dependencies retrieved from the builds in a group build.

You need to specify `group`. Redhat artifacts are then sorted from builds included in `group`, parent poms, `additionalArtifacts`, missing sources, checksums are added and packaged into repository.

`Optional parameters:`
- `excludeSourceBuilds` - used to specify the list of build configurations that you don't want to be included in the generated repository.

##### `MILESTONE`

The `MILESTONE` - not implemented yet

##### `RESOLVE_ONLY`

The `RESOLVE_ONLY` strategy resolves a list of artifacts against a BOM. The resolution is transitive, i.e. direct (except optional) and transitive dependencies of the listed artifacts are included in the repo zip as well.

###### `RESOLVE_ONLY` configuration

For the `RESOLVE_ONLY` strategy to work, two main things need to be defined: 1. the BOM(s) to resolve against and 2.
the set of artifacts to resolve. Both 1. and 2. can be defined in more than one way.

###### 1. BOMs

Option A: using `sourceBuild` and `sourceArtifact` options

* `sourceBuild` - the PNC build to take the `sourceArtifact` from
* `sourceArtifact` - a regular expression to match against files in `sourceBuild`.
  It should select the BOM against which the artifacts should be resolved

Option B: using `bomGavs` parameter

* `bomGavs` - a comma or whitespace separated list of BOM artifact coordinates.
  You can either pass `groupId:artifactId:version` in which case `sourceBuild` is not needed
  or you can pass just `groupId:artifactId` in which case the BOM artifact will be looked up
  in the build referenced by `sourceBuild`.

Options A. and B. can be combined: if you use both `sourceArtifact` and `bomGavs` then a union of BOMs defined
through both options will get effective.

###### 2. Set of artifacts to resolve

The list of artifacts to resolve against the BOMs can be defined via one or more of the following options:

* `parameters.resolveIncludes` and `parameters.resolveExcludes` - these are comma or whitespace separated lists of artifact patterns to filter out from the given BOM (that is transformed to an effective pom before the evaluation). The patterns are of the form `groupIdPattern:[artifactIdPattern:[[typePatter:classifierIdPattern]:versionPattern]]`. The subpatterns can contain string literals combined with wildcard `*`.
* `parameters.resolveArtifacts` - a comma or whitespace separated list of artifacts in the format `groupId:artifactId:[type:[classifier:]]version`. Note that type and classifier are optional. No type means `jar` and no classifier means empty classifier. Also note that the ordering of the segments is the same like in `resolveIncludes` and `resolveExcludes` but different than in `extensionsListUrl`.
* `parameters.extensionsListUrl` - a URL referring to a text file containing the list of artifacts in the format `groupId:artifactId:version:type:classifier` (type and classifier are optional)

It is possible to use all of the three above parameters simultaneously. In such a case the union of all sets will be used.

When to use which? 

* The combination of `resolveIncludes` and `resolveExcludes` should cover the most of what you need because the pattern are flexible enough to cover many use cases. Keep in mind that the universe from which the patterns are selecting the artifacts is only defined by `bomGavs`. What is not managed in any of the BOMs, cannot be selected by any pattern.
* `resolveArtifacts` can be used for adding artifacts that are not managed in any of the `bomGavs`. This is typically the case for Maven plugins and similar artifacts. 
* `extensionsListUrl` is much like `resolveArtifacts`, but it is an external file. It may come in handy if you generate it by some external tool.

`build-config.yaml` example using `bomGavs`, `resolveIncludes`/`resolveExcludes` and `resolveArtifacts`:

 ```yaml
   repositoryGeneration:
     strategy: RESOLVE_ONLY
     parameters:
       bomGavs: >-
         io.quarkus:quarkus-bom:1.2.3.Final-redhat-00001
         org.apache.camel.quarkus:camel-quarkus-bom:2.3.4.redhat-00002
       resolveIncludes: >-
         *:*:*redhat-*" # get all artifacts from the BOM that have redhat- substring in their versions
       resolveExcludes: >-
         io.netty:*:*:linux-aarch_64:* # but ignore all netty artifacts with linux-aarch_64
         io.netty:*:*:osx-*:*          # and osx-* classifiers
       resolveArtifacts: >-
         io.quarkus:quarkus-maven-plugin:1.2.3.Final-redhat-00001 # this one is not managed in io.quarkus:quarkus-bom
 ```

`build-config.yaml` example using `sourceBuild`, `sourceArtifact` and `extensionsListUrl`:

{% raw %}
 ```yaml
   repositoryGeneration:
     strategy: RESOLVE_ONLY
     sourceBuild: io.quarkus-quarkus-platform-{{productVersion}}
     sourceArtifact: 'quarkus-product-bom-[\d]+.*.pom'
     parameters:
       extensionsListUrl: "http://link.to.txt.file"
 ```
{% endraw %}

`extensionsListUrl` example:

 ```
 io.quarkus:quarkus-bom-quarkus-platform-properties:1.11.6.Final:properties
 io.quarkus:quarkus-logging-json-deployment:1.11.6.Final
 io.quarkus:quarkus-smallrye-opentracing-deployment:1.11.6.Final
 ```

###### Miscelaneous `RESOLVE_ONLY` parameters

* `bannedArtifacts` - a comma or whitespace separated list of `groupId:artifactId` artifact coordinates.
      Handy for troubleshooting which BOM entry pulls some specific artifact. If any of the banned
      artifacts is spotted in the local Maven repo, then the triggering artifact is logged
      and the generation fails.
* `excludeTransitive` - a comma or whitespace separated list of `groupId:artifactId[:classifier:type]` artifact coordinates that should be excluded by the Maven artifact resolver when resolving dependencies of the root artifacts that should be added to the Maven repository ZIP. This parameter allows excluding artifacts that do not exist in Indy and that would fail to be resolved.

###### Multi step Maven repository generation

If a product includes multiple BOMs that could be imported in any combination in customer projects, to make sure the generated Maven repository includes all the artifacts to cover all the combinations, a Maven repository would have to be generated for each BOM individually after which all the Maven generated repositories would have to be merged together to form a single deliverable.
For these kind of scenarious, the repository generation configuration allows configuring steps. Here is an example of how it could look like for a Quarkus platform including the RHBQ quarkus-bom and the CEQ quarkus-camel-bom.

 ```yaml
   repositoryGeneration:
     strategy: RESOLVE_ONLY
     sourceBuild: io.quarkus-quarkus-platform-{{productVersion}}
     steps:
       -
         parameters:
           extensionsListUrl: "http://link-to-RHBQ-extra-artifacts-list"
       -
         parameters:
           bomGavs: 'com.redhat.quarkus.platform:quarkus-camel-bom:2.13.4.Final-redhat-00001'
           extensionsListUrl: "http://link-to-CEQ-extra-artifacts-list"
     parameters:
       resolveIncludes: *:*:*redhat-*
       bomGavs: 'com.redhat.quarkus.platform:quarkus-bom:2.13.4.Final-redhat-00001'
 ```

In this example, the generation of the Maven repository will consist of two steps:
* generating Maven repository content for the RHBQ using the `RESOLVE_ONLY` strategy applied to the `com.redhat.quarkus.platform:quarkus-bom:2.13.4.Final-redhat-00001` and the extra artifacts configured in `http://link-to-RHBQ-extra-artifacts-list`
* generating Maven repository content for the CEQ using the `RESOLVE_ONLY` strategy applied to the `com.redhat.quarkus.platform:quarkus-camel-bom:2.13.4.Final-redhat-00001` *and* `com.redhat.quarkus.platform:quarkus-bom:2.13.4.Final-redhat-00001` (given that the CEQ BOM will not be imported w/o the RHBQ BOM) and the extra artifacts configured in `http://link-to-RHBQ-extra-artifacts-list`

Configuration options configured outside steps are inherited by each configured step. Each step may augment the default configuration by providing any configuration option that is supported by `repositoryGeneration` with the exception of configuring nested steps or changing the strategy (in fact, the RESOLVE_ONLY strategy is only strategy currently supporting multi step Maven repository generation).

If the same configuration option is found directly under the `repositoryGeneration` and under a step, the effective value of the option will either be a merge of the two (in case a value represents a list or a map) or the one configured in a step will override the default one (in case a value has a simple type, such as string). For example, in the above example the effective value of the `bomGavs` for the second step will be `com.redhat.quarkus.platform:quarkus-bom:2.13.4.Final-redhat-00001,com.redhat.quarkus.platform:quarkus-camel-bom:2.13.4.Final-redhat-00001`.

##### Parameters available in all strategies

- `additionalArtifacts` - artifacts to be specifically added to the repository from specific builds. Should use regular expressions to match any incremental suffixes.
- `externalAdditionalArtifacts` - artifacts from other builds, not within this product's build group. This list should contain artifact identifiers, as shown in the PNC UI.
- `externalAdditionalConfigs` - built artifacts from other builds, you use exact name of build configuration, to add all built artifacts of this config. Depending on build type, it's checking for last successful temporary or persistent build.
  Example:
  ```
  repositoryGeneration:
    strategy: BUILD_GROUP
    externalAdditionalArtifacts:
      - 'com.google.guava:guava-parent:pom:18.0.0.redhat-1'
    externalAdditionalConfigs:
      - org.drools-drools-8.32.0.Final
  ```

- `excludeArtifacts` - exclude artifacts based on a pattern

  Example:
  ```
  repositoryGeneration:
    strategy: BUILD_GROUP
    excludeArtifacts:
      - '.*:.*:war:.*'
      - '.*:.*:zip:.*'
  ```

The artifact information is specified in the format of `groupId:artifactId:packaging:version[:classifier]`

##### Additional filter available in BUILD_CONFIGS and BUILD_GROUP strategies

When using BUILD_CONFIGS and BUILD_GROUP strategies additional filter `filterArtifacts` is available. You can use it to reduce amount of artifacts in your repo e.g. filter artifacts and download only ones you're interested in. If activated (by adding some artifacts into array) only artifacts matching filter are included in repo generation process.

Artifact information/wildcards should be specified in the format of `groupId:artifact:artifactId:type:classifier`.

#### Licenses generation

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
- GENERATE_ADDITIONAL_SELECTED
- GENERATE_REDHAT_DEPENDENCIES
- GENERATE_REDHAT_DEPENDENCIES_EXTENDED

The `IGNORE` strategy won't produce any sources zip.

The `GENERATE` strategy gets sources from builds.

The `GENERATE_EXTENDED` strategy gets same results as `GENERATE` and adds sources of unreleased dependencies.

The `GENERATE_SELECTED` strategy gets sources from selected `sourceBuild`.

The `GENERATE_ADDITIONAL_SELECTED` strategy gets same results as `GENERATE` and adds sources of builds external to current group. `additionalExternalSources` specifies Build Config names of builds, outside this group.

The `GENERATE_REDHAT_DEPENDENCIES` strategy gets sources from builds and adds sources of redhat dependencies used in these builds.

The `GENERATE_REDHAT_DEPENDENCIES_EXTENDED` strategy gets same result as `GENERATE_REDHAT_DEPENDENCIES` and add sources of unreleased dependencies.

All `GENERATE*` strategies require that the repository generation is **not** ignored.

## Usage of YAML variables
You can define variables in your YAML file for injection at various points. This is useful when there are few changes between version releases (e.g tags). To define a variable, use this format:
{% raw %}
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
{% endraw %}

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
