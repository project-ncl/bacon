---
title: "Changelog"
---

* Contents
{:toc}
{::options toc_levels="1,2"/}


## Introduction
All notable changes to this project will be documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

A template is
```
## [Version] - date
### Added
### Removed
### Changed
### Deprecated
### Fixed
```
## [2.1.5] - UNRELEASED
### Added
- [NCL-6110] Print status of brew push on stdout

### Fixed
### Changed

## [2.1.4] - 2020-10-13
### Added
- [NCL-5980] Check for >0 build configs and builds when generating maven repo
- [CPAAS-976] Allow Bacon to be installed in an arbitrary location
### Fixed
- [NCL-6170] Bacon list command for builds is ignoring some arguments
### Changed
- [CPAAS-976] Output version of Bacon

## [2.1.3] - 2020-10-08
### Added
- Offliner manifest file generation from @akoufoudakis

### Fixed
- [NCL-6155] Provide alternative impl of ConsoleLogger so that all logs are controlled by slf4j

### Changed
- [NCLSUP-161] Default max download attempts is now 5, with a max exponential backoff of 30 seconds

## [2.1.2] - 2020-10-05
### Added
- Enable tailtip widget in Bacon console
- [NCL-5982] validation of build-config.yaml (pig)

### Fixed
- Brew push fix where it could only push 2 builds to Brew at a time
- [NCL-6060] NullPointerException when generating the addons with bacon pig
- Set default javadoc generation to IGNORE
- [NCL-6061] Log warning instead of throwing exception when filling brew data
- [NCL-6097] bacon is missing possibility to set a milestone as current
- Fix newlines in picocli output
- [NCL-6054] Close PNC clients used by PiG

### Removed

## [2.1.1] - 2020-09-17
### Added
- [NCL-5974] Ported from aesh CLI framework to picocli

### Fixed
- [NCLSUP-119] Fix bug in checking if a branch is modified during PiG run phase. We were grabbing the scm revision commit id instead of the tag
- [NCL-6033] Fix list operations
- Maven repo generation and quarkus dependency analyzer fixes
- Improved handling of errors and more tests

### Removed
- [NCL-6048] Remove --skipRepo from bacon pig

## [2.0.2] - 2020-09-07
### Added
- `bacon pnc whoami` command
- `bacon pnc group-config show-latest-build` command

### Fixed
- [NCLSUP-109] Fix Bacon/PiG stuck at trying to check if branch updated
- [NCL-6018] Download logs even when build successful to make addons happy again
- Fix how we grab the Git reference for a tag in PiG during branch modification check
- [NCL-6022] Rename executionRoot to 'brewBuildName', the new name in PNC 2.0
- Lots of code cleanups / closing resources by @dwalluck :)
- Fix how we regenerate the access token in Bacon using refresh token. If the refresh token is expiring soon, get a new set of access/refresh token instead of trying to refresh

## [2.0.1] - 2020-08-28
### Added
- Ability to install specific version of bacon

### Fixed
- Bacon install script with cygwin should now handle paths better
- [PiG] Bug fix for nvr generation
- [PiG] Fix link to build url in generated README

## [2.0.0] - 2020-08-27
### Added
- Most commands from pnc-cli 1.x are ported to bacon 2.0.0
- PiG integration in Bacon
