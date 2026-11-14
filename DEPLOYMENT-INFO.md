# Overview

A release follows `.claude/rules/release-workflow.md`, the binding sequence; `docs/build-and-release.md`
explains the build behind it. Two things that used to be hand work are not any more:

* The version is edited in one place only: `projectVersion` in `gradle.properties`. The info dialog
  reads it from the running jar's manifest (`Implementation-Version`), so nothing else carries a
  version to be changed.
* `CHANGELOG.md` is assembled by the release pull request from the fragments under `changelog.d/`
  (`python3 scripts/assemble_changelog.py X.Y.Z`, see `changelog.d/README.md`).

For the IzPack installer, `make izpack-installer` builds it with its checksums; the walkthrough is in the
[wiki izpack section](https://github.com/astrapi69/mystic-crypt-ui/wiki/How-to-create-izpack-installer-with-gradle).
