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

## Other PiG commands

The following `pig` subcommands are available but not tied to any specific phase:

- cachi2lockfile

# Configuration

The application is configured via the `build-config.yaml` file.

A PiG `build-config.yaml` looks like [this](https://github.com/project-ncl/bacon/blob/main/example-pig-config.yaml). See [this](build-config.html) for a detailed guide of `build-config.yaml`

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


## Add ons

### Skipping some addons via the command line
To skip addon(s) on the `bacon pig run` or `bacon pig addons` command, use the `--skipAddon=<addon>` option:

```bash
bacon pig run ... --skipAddon=a
```

To specify more than one addon to skip, repeat the `--skipAddon` option again:

```bash
bacon pig run ... --skipAddon=a --skipAddon=b
```


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

### Build From Source Statistics

The `runtimeDependenciesToAlignTree` add-on produces statistics on the percentage of dependencies that are build-from-source compliant and provides a reverse-mapping of dependencies to all of the parents in the tree which brings that dependency in.

```
addons:
   runtimeDependenciesToAlignTree:

```

The `runtimeDependenciesToAlignTree` has optional logging for sorted lists by project of the unique dependencies of that project.    To generate these:

```
addons:
   runtimeDependenciesToAlignTree:
       printProjectLogs: true
```

For each project in the build configuration, a log will be generated within the extras folder.


### Extra Deliverables Downloader

The `extraDeliverablesDownloader` add-on downloads artifacts from one or more PiG build configurations and uploads it to your projects staging directory. Each build configuration accepts two yaml variables: `matching` and `suffix` which should be provided together as a list. 

`matching` is a regex of an artifact that is to be downloaded and `suffix` is the name of the artifact you want to give it.
The final name is created by concatenating `{{project-name}}-{{version}}.{{milestone}}-{{suffix}}`

Make sure to give all artifacts distinct names because two artifacts with the same name can't be uploaded.

An example configuration is:
```
addons:
  extraDeliverablesDownloader:
    {{keycloakParentBuildConfigName}}:
        - matching: 'keycloak-server-dist.*\.zip'
          suffix: 'server-dist.zip'
        - matching: 'keycloak-api-docs.*\.zip'
          suffix: 'api-docs.zip'
    {{keycloakConnectBuildConfigName}}:
        - matching: 'keycloak-connect.*\.zip'
          suffix: 'nodejs-adapter-dist.zip'
```

### Post Build Product Security Scanning as a Service (PSSaaS)
{% raw %}

The `postBuildScanService` addon provides a way to trigger code scans for builds within the PiG context (successful builds in the current `build-config.yaml`), as well as on brew builds and git repositories using the PSSaaS service.

```
addons:
  postBuildScanService:
    productID: 2151 #Defined in product pages (String)
    serviceUrl: {{ pssaas_scan_service_url }} #URI of the PSSaaS service
    serviceSecretKey: {{ pssaas_secret_key }} #The key for the PSSaaS service
    serviceSecretValue: {{ pssaas_secret_value }} #The token for the PSSaaS service
```

Currently PSSaaS supports three use cases:

##### PNC build

Passes PNC build ID (String) to the scan service, the repository and revision associated with the build will be checked out and scanned. By default this will be successful builds within the current PiG context and no further addon configuration is required as shown above.

##### Brew build

Passes brew/koji build ID (Integer) to the scan service, The repository and revision will be checked out and scanned,
the brew builds currently need to be manually specified in the addon configuration.

```
brewBuilds:
    - 1000
    - 2000
```

##### SCM URL and Revision

The repository and revision of product which is not productized (not built in PNC or Brew), this will be checked out
verbatim and scanned, repositories need to be specified in the addon configuration.

```
extraScmUrls:
      - scmUrl: https://github.com/randomurl
        scmRevision: 10.0.0
      - scmUrl: https://github.com/anotherrandomurl
        scmRevision: foobar
```


The following command relies on mandatory configuration parameters to be defined directly in the `build-config.yaml` file (otherwise, the bacon addons run will fail due to lack of mandatory parameters).
```
bacon pig run --skipBuilds --skipJavadoc --skipLicenses --skipPncUpdate --skipSharedContent --skipSources --verbose .
```

#### Hiding Sensitive Information

In order to avoid any sensitive information from being exposed or checked into SCM, we'd recommend you invoke bacon command the following way:
```
bacon pig run --skipBuilds --skipJavadoc --skipLicenses --skipSharedContent --skipSources \
-e pssaas_scan_service_url=${scan_service_url}                                            \
-e pssaas_secret_key=${scan_service_secret_key}                                           \
-e pssaas_secret_value=${scan_service_secret_val}                                         \
--verbose .
```
Make sure all involved environment variables have been defined before running the above command.
```
export scan_service_url=<token1>
export scan_service_secret_key=<token2>
export scan_service_secret_value=<token3>
```
Use parameters passed via '-e' bacon CLI option inside the build-config.yaml file:
```
serviceUrl: {{ pssaas_scan_service_url }}
serviceSecretKey: {{ pssaas_secret_key }}
serviceSecretValue: {{ pssaas_secret_value }}
```

Using environment variables means build-configurations can be shared easily. Supplying parameters via command line option '-e' protects sensitive information by hiding actual values in the environment variables. This technique can be used to conceal secret parameter values and assign them to so-called 'bridge' variables which are passed-in to the build-config.yaml where they get interpreted and assigned to the ultimate addon configuration parameters (in the above example - serviceUrl, serviceSecretKey, serviceSecretValue).

There is no need to do the same for non-sensitive parameters such as productID, eventID, etc. They can be defined directly in the build-config.yaml (see examples below).

_Example 1: Default scan (Successful builds in the PiG context)_
```
addons:
  postBuildScanService:
    productID: 2151
    serviceUrl: {{ pssaas_scan_service_url }}
    serviceSecretKey: {{ pssaas_secret_key }}
    serviceSecretValue: {{ pssaas_secret_value }}
```

_Example 2: PNC, Brew and SCMURL based scans and other values supported_
```
addons:
  postBuildScanService:
    productID: 2151
    eventId: 1
    isManagedService: True
    cpaasVersion: 99.0.0
    jobUrl: https://somejenkinspipeline.com/run999
    serviceUrl: {{ pssaas_scan_service_url }}
    serviceSecretKey: {{ pssaas_secret_key }}
    serviceSecretValue: {{ pssaas_secret_value }}
    extraScmUrls:
      - scmUrl: https://github.com/randomurl
        scmRevision: 10.0.0
      - scmUrl: https://github.com/anotherrandomurl
        scmRevision: foobar
    brewBuilds:
      - 1000
      - 2000
```

#### Using Defaults
To use default value for an optional parameter leave that parameter undefined in the addons section of your build-config.yaml file.

### Cachi2 Lock File Generator

This add-on generates a Cachi2 Lock File for the produced Maven repository ZIP (regardless which strategy was used to generate it). Cachi2 Lock Files can be used to prefetch Maven content to Konflux pipelines for container image and MRRC release processes. 

The add-on can be configured in the following way:

```
addons:
  cachi2LockFile:
    filename: mrrc-cachi2-lockfile.yaml
```

The `filename` parameter is optional and defaults to `cachi2lockfile.yaml` if not configured. The file will be stored under the `extras` directory.

{% endraw %}

## cachi2lockfile command

This `pig cachi2lockfile` CLI command allows generating a Cachi2 lockfile for a given Maven repository. The command will not perform any Maven artifact resolution, it will simply describe the content of a Maven repository in the Cachi2 lock file format.

The command accepts ZIP files and/or directories that include Maven repository content under `maven-repository` subdirectory (to be compliant with the way Bacon generates Maven repository ZIPs). For example:

```
bacon pig cachi2lockfile rh-quarkus-platform-3.15.2.CR1-maven-repository.zip
```

Multiple repository paths (including a mix of directories and ZIPs) can be provided using comma as a separator.

By default, the generated lock file will be named `artifacts.lock.yaml`, since this is what Cachi2 to will expect by default. A different file name can be specified by adding the `--output` argument with the desired value.

Other command options include:

* `--maven-repository-url` - the desired value of the Maven repository URL to record for each artifact. The default value will match the one configured in the local Bacon config;
* `--preferred-checksum-alg` - the lock file format expects a single checksum. By default, the tool will use the highest version of the `SHA` checksum available in the Maven repository. This option allows to specify an alternative checksum algorithm (assuming it's available in the repository), if the default is not good enough. 

## PiG Export Feature

### Build Configuration Export
PiG allows you to export an existing build-configuration into YAML format that can then be included into the builds section in a `build-config.yaml`.

The command to generate the YAML output is:
```
$ bacon pig export build-config <build-config-id>
```

