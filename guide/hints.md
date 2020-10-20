---
title: "Hints"
---

* Contents
{:toc}

# Introduction

<table bgcolor="#ffff00">
<tr>
<td>
    <b>TODO</b>
</td>
</tr>
</table>


# Use a specific brew tag prefix
This is done by specifying in your `build-config.yaml`:
```yaml
...
version: 1.0.0
group: test boo
brewTagPrefix: fb-1.0-pnc
builds:
  - ...
```

If the `brewTagPrefix` is null, the product version is not updated with
the brew tag prefix. Note that the product version has a brew tag prefix
generated automatically by default. This provides a way to override that
value.

# Portability of build-config.yaml between different PNC environments
To be able to re-use the same `build-config.yaml` between different PNC environments (i.e. staging/production):
- Use `systemimageid` instead of `environmentId` to specify your build environment. The `systemimageid` of the environment will be the same across different PNC environments, however the ids will be different.
