
# Introduction

Bacon is a new Java CLI for [ProjectNCL](https://github.com/project-ncl/pnc) 2.0 combining features of old PNC, DA CLI and PiG tooling.

# Documentation

User documentation is available [here](https://project-ncl.github.io/bacon). This includes guidance on the `build-config.yaml` files and the features available.

Contributions are welcome! Please see the [Developer's guide](https://github.com/project-ncl/bacon/blob/master/DEVELOPING.md) to get up and running with developing in Bacon.

Finally, our [changelog](https://project-ncl.github.io/bacon/changelog.html) lists the changes that happened in releases.

---
# Installation via JBang

[JBang](https://www.jbang.dev) lets you run or install bacon directly from Maven Central with no manual download. Requires Java 17+.

## Run directly

```shell
jbang bacon@project-ncl pnc build list
jbang pnc@project-ncl build list
jbang da@project-ncl
jbang pig@project-ncl
```

## Install as a local command

```shell
jbang app install bacon@project-ncl
jbang app install pnc@project-ncl
jbang app install da@project-ncl
jbang app install pig@project-ncl
```

After installing, `bacon`, `pnc`, `da`, and `pig` are available on your `PATH`.

## Snapshot builds

Append `-snapshot` to use the latest snapshot from Maven Central:

```shell
jbang bacon-snapshot@project-ncl pnc build list
jbang app install bacon-snapshot@project-ncl
```

---
# Reporting an issue
To report an issue, please use the NCLSUP/NCL projects in the internal JIRA instance. The Github issues usage is now disabled.
