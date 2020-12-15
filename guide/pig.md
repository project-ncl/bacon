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

## Add ons

<table bgcolor="#ffff00">
<tr>
<td>
    <b>TODO</b>
</td>
</tr>
</table>
