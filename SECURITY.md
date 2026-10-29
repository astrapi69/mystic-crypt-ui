# Security Policy

`mystic-crypt-ui` is a desktop password manager. It holds a vault of credentials
behind a master password, puts secrets on the system clipboard, and loads
plugins from a directory on the user's machine. A defect here can expose data
its owner believes is locked away, so the reporting path below is deliberately
not the public issue tracker.

## Supported Versions

Only the current minor line receives security fixes. This is a single-maintainer
project, and supporting several lines at once is a promise it could not keep.

The table names **lines, not releases**. A fix always lands in a new release of
the supported line, so a patch release does not change what is supported here -
`8.2` and `8.3` are the same row. The table is revised when a line ends, not
when a version number moves.

| Version | Supported          | Required JDK                             |
|---------|--------------------|------------------------------------------|
| 8.x     | :white_check_mark: | 25 and above (8.0 was still built for 17) |
| 7.x     | :x:                | 17 and above                             |
| 6.1.x   | :x:                | 11 and above                             |
| <= 5.4  | :x:                | 8 and above                              |

If you are on an older line, the fix for a reported vulnerability will be an
upgrade to the current one rather than a backport.

## Reporting a Vulnerability

**Do not open a public issue.** Use GitHub's private vulnerability reporting,
which is enabled on this repository:

[Report a vulnerability](https://github.com/astrapi69/mystic-crypt-ui/security/advisories/new)

That keeps the report between you and the maintainer until a fix exists, and it
creates the draft advisory a CVE can later be issued from.

Please include, as far as you can:

- the version you found it in, and whether the current release is affected
- whether it is reachable through the graphical application, the `--cli` mode, or
  an installed plugin
- what an attacker gains - reading a vault without the master password, keeping
  access after the workspace is locked, recovering a secret from the clipboard or
  from a file the application wrote, and so on
- a way to reproduce it: a failing test is ideal, since this project fixes bugs
  test-first and yours would become the regression guard

Never attach a real vault, a real keystore, or a real password to a report. If a
sample is needed, create one with throwaway data.

### What happens next

This is a spare-time project, so no response time is promised that could not be
met. What is promised instead:

- a reply acknowledging the report, and whether it is reproducible
- if it is: a fix on the current line, a release, and an advisory crediting you
  unless you prefer otherwise
- if it is not, or it turns out to be intended behaviour: an explanation of why,
  rather than silence

### Defects found by the maintainer

A security defect that is found from the inside - during development, a review,
or an audit of this project's own code - is tracked in a public issue like any
other bug, because that is where its fix, its regression test and its discussion
already live. What the private path exists for is a report from outside, which
must not be public before a fix exists.

One internal finding takes the private path as well: one whose details would
describe a path that is still open in every published release. A public issue
would hand that path to anyone before a single user could update. Such a finding
is tracked in a draft advisory, fixed in the advisory's private fork, and becomes
public with the release that carries the fix.

Both paths end in the same place: once the fixed release is out, the defect gets
a published advisory naming the affected versions and the fixed one. The
advisory follows the release, never precedes it.

The first defect handled this way is
[#237](https://github.com/astrapi69/mystic-crypt-ui/issues/237) (locking the
workspace left the decrypted vault on screen), found internally on 2026-09-07.
The second is [#617](https://github.com/astrapi69/mystic-crypt-ui/issues/617):
every installer from 7 to 8.10 carried commons-beanutils 1.9.4 (CVE-2025-48734),
fixed in 8.11, advisory
[GHSA-9m97-3xjj-4gwm](https://github.com/astrapi69/mystic-crypt-ui/security/advisories/GHSA-9m97-3xjj-4gwm).
The first internal finding kept private is
[GHSA-w87j-p568-fr6v](https://github.com/astrapi69/mystic-crypt-ui/security/advisories/GHSA-w87j-p568-fr6v):
up to 8.11, opening a vault and importing an obfuscation rule table built every
type of this project's libraries that the file named. It was found on
2026-10-22 while #617 was examined, and fixed in 8.12.
The second is
[GHSA-xp4c-rpcx-hw5c](https://github.com/astrapi69/mystic-crypt-ui/security/advisories/GHSA-xp4c-rpcx-hw5c):
up to 8.12, opening a vault with a key file read the vault file with Java
serialization, without a filter, in the library mystic-crypt
([GHSA-87j9-xjf5-f387](https://github.com/astrapi69/mystic-crypt/security/advisories/GHSA-87j9-xjf5-f387)).
Both drafts were opened on 2026-10-29, and the fix shipped the same day in
mystic-crypt 13.8 and in 8.13.

## Two things specific to this application

### Plugins run with the application's own rights

Features ship as internal plugins, and the application loads them from a plugin
directory on the user's machine. A plugin runs in the same process as the vault:
there is no sandbox between them. Anything that lets an attacker place a file in
that directory is therefore as serious as a defect in the application itself,
and a report about plugin loading is in scope here.

### The installer is published with checksums

Each release publishes `mystic-crypt-ui-<version>-installer.jar` together with a
`.sha256` and a `.sha512` file. Verify the download against them before running
it; a mismatch is worth a report on its own.

## What belongs elsewhere

This application implements no cryptographic primitives. Ciphers, hashes, key
derivation, signing and random generation all come from the JDK, from Bouncy
Castle, or from the libraries underneath - `mystic-crypt`, `crypt-data` and
`crypt-api`, each with its own reporting path in its own repository. A flaw in a
primitive belongs to whoever implements it.

A flaw in *how this application uses* one of them - a vault written with weaker
parameters than configured, a secret left readable after locking, a password
reaching a log file or an error dialog - is squarely in scope here.

So is a library the installer ships in a version with a published vulnerability,
even one this application never declares itself: which version ends up in the
installer is this project's choice, and a user can only update the application.

## What this project already does

So that a report can start from what is known rather than from zero:

- **CodeQL** runs on every push and pull request to `master` and `develop`, plus
  weekly ([`codeql.yml`](.github/workflows/codeql.yml)).
- **Mutation testing** runs weekly over the headless logic, and on the pull
  requests that touch it ([`mutation.yml`](.github/workflows/mutation.yml)), so a
  test that passes without actually catching the bug is visible rather than
  assumed.
- Vault changes are accepted only with a real round trip through the running
  application, not with unit tests alone (`.claude/rules/quality-checks.md`).
- Test key material and test vaults are generated at test runtime and never
  committed, so nothing in this repository is a real secret.
- Libraries that arrive only through others, which Dependabot does not see, are
  held to a floor by `RuntimeDependenciesTest` (commons-beanutils 1.11.0, jackson
  2.22.4), and every release runs it against the application jar taken from the
  published installer itself (#617).
