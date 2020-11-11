---
title: "Build Config Files"
---

* Contents
{:toc}

## Overview

**_Unless specified otherwise, all the keys specified in the yaml are required._**

The build-config.yaml is broken down into 2 big categories: the PNC part and the generation part. The PNC part consists of the configuration on how your builds are performed. Once the builds are done, that information is then fed into the generation part to produce maven repository, javadoc, sources, license zips.

The basic structure of a build-config.yaml looks as follows:

```yaml
# PNC part
# PNC product part
product:
  name: test-product
  abbreviation: tp
version: 1.0.0
milestone: DR1
brewTagPrefix: fb-1.0-pnc # Optional
group: Test Product 1.0-all

# PNC builds part
defaultBuildParameters: # Optional: shared between all builds
  project: test-project
  environmentId: 1
  buildScript: mvn clean deploy -DskipTests
builds:
  - name: empty
    externalScmUrl: https://github.com/michalszynkiewicz/empty.git
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
  name: my-beautiful-product
  abbreviation: mbp
```
The `version` specifies the product version we are building for, and the `milestone` represents the 'step' at which we are in the version. A typical milestone value is DR1, DR2, etc or ER1, ER2 etc. The combination of the version and milestone forms the full release name of the product: e.g 1.0.0.CR1.

**TODO**: talk about automatic generation of milestone versions with `--releaseStorageUrl`

The `brewTagPrefix` is an **optional** key that you can use to override the default generated brew tag prefix associated with a product version. The key is used to specify to which Brew tag all the builds should go when the milestone is closed.

Finally the `group` is the group config where all the builds defined are grouped together. It is highly recommended to make the group name unique to the version being built. For example, if the `version` is 1.0.0, then the group name is: `my-beautiful-product-1.0.0`. To make this easier, consider using [YAML variables](#usage-of-yaml-variables). The group config is automatically linked to the product version.

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

  scmUrl: <scm URL> # Either specify scmUrl or externalScmUrl
  externalScmUrl: <external Scm URL> # Either specify scmUrl or externalScmUrl

  scmRevision: <scm revision>
  
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

#### scmUrl v/s externalScmUrl
You'll notice that in the YAML above, you can specify either `scmUrl`, or `externalScmUrl`. For most (all?) cases, you'd just need to specify the `scmUrl` (We may deprecate `externalScmUrl` in the future). The `externalScmUrl` describes the external source of the build (e.g the Github link of the project) whereas nowadays the `scmUrl` can be used to describe both the external, and the internal source of the build.

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

We allow the user to specify the build environment using either `environmentId`, `environmentName` or the `systemImageId`. `environmentId` refers to the exact id of the build environment as stored in the PNC database. This is what has been traditionally used, but it is problematic since the ids are different between PNC servers (Devel, Stage, Prod), which makes the `build-config.yaml` not very portable between PNC servers. Moreover, an environment may become deprecated over time, thus the environmentId should be kept up-to-date, which is not optimal.

The `environmentName` in contrast, is the same between different PNC servers and should be used if portability is a concern. Environments specified by `environmentName` are always using **the latest updates**. This is important because there will be changes to your build configurations in PNC, therefore, they may be **rebuilt**. 

The `systemImageId` is also the same between different PNC servers, the difference to `environmentName` is that the `systemImageId` **preserves** the docker-image version of the environment, which means that after some time, the version may become deprecated, and you may need to update from time to time. (**NOTE**: `systemImageIds` with `:latest` suffix will **no longer be available** in PNC)  

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

The `DOWNLOAD` strategy is identical to that of the license generation, except that we specify the artifact which contains all the javadocs.

#### Sources generation
Strategy options are:
- IGNORE
- GENERATE (default)
- GENERATE_EXTENDED
- GENERATE_SELECTED

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