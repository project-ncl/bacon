---
---
don't merge me

* Contents
{:toc}

### Overview

Bacon is a new Java CLI for [ProjectNCL](https://github.com/project-ncl/pnc) 2.0 combining features of the older PNC CLI, [DependencyAnalysis](https://github.com/project-ncl/dependency-analysis) CLI and PiG tooling.

### Release Notes

For a list of changes please see [here](https://github.com/project-ncl/bacon/wiki/Changelog)

### Installation and Usage

Compile:
```bash
mvn clean install
```

Run:
```bash
java -jar cli/target/bacon.jar
```

To install the latest ***released*** version:
```bash
curl -fsSL https://raw.github.com/project-ncl/bacon/master/bacon_install.py | python3 -
```

To install a specific ***released*** version:
```bash
curl -fsSL https://raw.github.com/project-ncl/bacon/master/bacon_install.py | python3 - 2.0.1
```

To install the latest ***snapshot*** (no need to compile):
```bash
curl -fsSL https://raw.github.com/project-ncl/bacon/master/bacon_install.py | python3 - snapshot
```

This will install the `bacon`, `pig`, `da` and `pnc` commands in the `~/bin`
directory, which on Linux will make it available in your shell.

To update the installed version:
```bash
# latest released version
bacon update

# specifc released version
bacon update 2.0.1

# snapshot version
bacon update snapshot
```

### Feature Guide

Below are links to more specific information about configuring sets of features in PME:

* [Configuration Files](guide/configuration.html)
* [Build Config Files](guide/build-config.html)
* [Hints](guide/hints.html)
* [PiG](guide/pig.html)
