---
title: "Configuration"
---

* Contents
{:toc}

# Configuring Bacon

An example configuration is present [here](https://github.com/project-ncl/bacon/blob/main/config.yaml)

The default configuration used by `bacon` should be located in folder: `~/.config/pnc-bacon/` with name `config.yaml`. We can specify a different folder for the location of the `config.yaml` via the `-p` flag:

```bash
bacon pnc build cancel -p <alternate folder containing config.yaml> 1000
```

We can specify several profiles in the configuration file. This is useful when dealing with different PNC servers and/or configurations. We can use the `--profile` flag to choose the non-default one:

```bash
bacon pnc build cancel --profile coolProfile 1000
```

# Authentication

To authenticate to PNC Authentication servers, add this to your `config.yaml`:

```yaml
keycloak:
    url: "http://keycloak.com"
    realm: ""
    username: ""

    # if regular user
    clientId: ""

    # if service account
    # clientSecret: ""
```

We also support LDAP authentication as an alternative to keycloak if the PNC
servers are configured to accept LDAP requests:

```yaml
ldapUsernamePassword: username:password
```

# Internal SCM repositories (GitHub and GitLab)

The git provider is chosen from the repository URL: GitLab when the host contains `gitlab`, GitHub otherwise.

```yaml
githubToken: "github-token"   # GitHub / GitHub Enterprise, 'repo' scope
gitlabToken: "gitlab-token"   # GitLab, 'read_api' scope (or the GITLAB_TOKEN environment variable)
```

Without a token, public (and on GitLab, internal) repositories are read anonymously. Tokens are only sent to the
repository's own host, never to a host it redirects to.

# Network timeouts

Connections to SCM hosts, repository managers and bifrost time out after 30 seconds. A host that cannot be reached is
not retried with other revisions or credentials, and a token a host rejects is not tried again on it.

# PNC client HTTP timeouts

The PNC REST client used by `bacon` defaults to a **30 second connect timeout**
and a **60 second read timeout**. Some operations can take longer than the read
default to respond — for example generating redacted provenance during
`bacon pnc build download-build-outputs <id> --redacted` — and will otherwise
fail with `java.net.SocketTimeoutException: Read timed out`.

Both timeouts can be overridden without changing `config.yaml`. Each accepts a
value in **milliseconds** and can be set via either a Java system property or an
environment variable:

| Timeout | System property                     | Environment variable                | Default    |
|---------|-------------------------------------|-------------------------------------|------------|
| Read    | `pnc.client.readTimeoutMillis`      | `PNC_CLIENT_READ_TIMEOUT_MILLIS`    | `60000`    |
| Connect | `pnc.client.connectTimeoutMillis`   | `PNC_CLIENT_CONNECT_TIMEOUT_MILLIS` | `30000`    |

Precedence for each timeout is: system property first, then environment
variable, then the built-in default. Values that are missing, blank,
non-numeric, or not strictly positive are ignored (a warning is logged) and the
default is used.

Set the read timeout to 5 minutes via an environment variable:

```bash
export PNC_CLIENT_READ_TIMEOUT_MILLIS=300000
bacon pnc build download-build-outputs <id> --redacted
```

Or via a system property (system properties reach `bacon` through
`JAVA_TOOL_OPTIONS`):

```bash
JAVA_TOOL_OPTIONS="-Dpnc.client.readTimeoutMillis=300000" \
  bacon pnc build download-build-outputs <id> --redacted
```

# Rewriting download URLs

Some URLs that PNC hands out may not be reachable from where `bacon` runs, while
the same service is reachable at another address: the `publicUrl` of built
artifacts, or the bifrost host PNC redirects the alignment log to. URL rewrites
replace the start of such URLs, like git's `url.<base>.insteadOf`:

```yaml
urlRewrites:
  "https://bifrost.example.com": "https://bifrost.internal.example.com:8443"
  "https://indy.example.com/api/content": "https://mirror.example.com/indy"
```

or for a single run, with the global `--url-rewrite <from>=<to>` option
(repeatable, added to the profile's rules):

```bash
bacon pnc build download-build-outputs <id> \
  --url-rewrite https://bifrost.example.com=https://bifrost.internal.example.com:8443
```

- `<from>` and `<to>` are absolute `http(s)` URLs, optionally with a path, and
  without user info, query or fragment.
- A URL matches when its scheme, host and port are the same as `<from>`'s and
  its path starts with `<from>`'s path at a `/` boundary. The matching start is
  replaced by `<to>`; the rest of the path and the query are kept.
- The longest matching `<from>` wins; with the same `<from>`, the command line
  rule wins over the profile's.
- Rewrites apply to the downloads bacon makes itself, and to their redirects:
  built artifacts, PiG downloads, source archives and alignment diffs, bifrost
  logs and, when rules are configured, the alignment log (both the request to
  PNC, so a rule matching the PNC host changes where it is asked, and PNC's
  redirect to bifrost). The PNC REST client itself uses the profile's PNC URL.
- Credentials (e.g. `ldapUsernamePassword`) are sent only to the origin of the
  first, rewritten URL, and for the alignment log to the profile's
  `bifrostBaseurl` (rewritten too), never to another host a redirect points to.
- A redirect from `https` to `http` sent by a server is not followed; a rule
  may still rewrite to plain `http`, and a warning is logged when it does.
