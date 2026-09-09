# Release Workflow

Prompt triggers: "release new version", "new release".

## Ground rules

- Version source: `gradle.properties` (`projectVersion`) — the ONLY hand-edited version
  field. Nothing else carries an independent version literal; if one appears, fix the
  derivation, not the literal.
- Tests must be green: red tests block the release, no exceptions, no "disable the test
  for this release".
- Every release is a logical boundary — do not release mid-feature.
- SemVer: `feat:` = minor, `fix:`/`refactor:` = patch, breaking change = major (called
  out prominently in the CHANGELOG).
- Gitflow: release is prepared from `develop`, tagged, and `master` carries releases.
  Never hand-tag mid-state.

## Steps

1. **Capture state**: `git tag --sort=-creatordate | head -5`, commits since last tag
   (`git log $(git describe --tags --abbrev=0)..HEAD --oneline --no-merges`). Show the
   summary and wait for confirmation.
2. **Pick the version** per SemVer, propose with rationale, wait for OK.
3. **CHANGELOG.md**: grouped entry (Breaking / Added / Changed / Fixed / Security),
   summarized for humans. Commit `docs: changelog for vX.Y.Z`.
4. **Bump** `projectVersion` in `gradle.properties`.
5. **Full gate** (ALL mandatory; a red result aborts the release):
   - `make build-full`, under the Xvfb harness — it runs the packaging, EVERY test
     including the UI e2e suite, spotless and the license check. `make test` and
     `make test-e2e` are not run beside it: the `test` task filters nothing
     (`gradle/testing.gradle`), so `test-e2e` is a strict subset of `test` and both are
     contained in `build-full`. Running the e2e suite three times measures nothing new.
   - the gate reports what it measured: read the test XML afterwards — class count, test
     count, and that the UI e2e classes are among them. A `:test FROM-CACHE` restores
     results without running anything, and an empty result set is not a green gate.
   - **package from the state that will be tagged.** The release PR is merged FIRST; the
     installer and its checksums are built from the merged branch, and the tag names
     exactly that commit. Building beforehand from the release branch ties the release to
     a commit the tag never names — unless the merge happens to be a fast-forward, which
     is luck, not a property (2026-09-09: it was, so 8.4 kept its installer; a squash or a
     merge commit would have forced a rebuild and a second verification).
   - `make izpack-installer` (the plugin set in `install.xml` must match the Makefile
     `plugins:` list)
   - **verify the installer AFTER the checksums are written**, against the file the
     checksums describe: size (a plausible floor, ~50 MB — below it is an abort, not a
     warning), it opens as an archive, and `sha256sum -c` / `sha512sum -c` pass.
     Verifying before the checksums verifies a file that is then replaced:
     `checksumInstaller` depends on `izPackCreateInstaller`, which re-runs and rewrites
     the jar (measured 2026-09-07: 81743356 bytes verified, 81743359 bytes shipped).
   - dependency currency check: `make dependency-updates` — routine patch/minor bumps as
     part of the release; major bumps get their own session, never bundled in. A change
     to the build or the release path itself is NOT bundled into a security release.
6. **Tag + push**: `make tag-release`, which is where the tag's SHAPE is decided -
   `gradle/tagging.gradle` builds it as `RELEASE-${projectVersion}`, so a release is
   `RELEASE-8.4` and never `v8.4`. Manually it is `git tag -a RELEASE-X.Y.Z` + push, but
   prefer the target: a shape retyped from prose is a shape that drifts, and this line said
   `vX.Y.Z` through three releases while every tag in the repository said otherwise (#291).
   **A pushed tag is never deleted or moved.** If the publishing workflow it triggers
   fails, the fix goes on the branch and the workflow is re-run on the SAME tag. A deleted
   or moved tag rewrites what a version means for everyone who already fetched it, to
   repair a run that can simply be repeated. Before a release depends on a publishing
   workflow that has changed, run it once manually against a snapshot target: a workflow
   that has never executed is a hypothesis (crypt-api lost a first tag run to exactly
   this).
7. **Move `master` onto the release.** Merge (or fast-forward) the tagged commit into `master`
   and push it. `master` holds releases - that rule was written down and then not followed for
   8.1.1, 8.2, 8.3 and 8.4, so master stood at the 2024-06 release while claiming to be the
   release branch, and a reader could not tell which release it corresponded to without checking
   the tags (#248). It is a numbered step here rather than a habit for exactly that reason. Never
   the other direction: master carries nothing develop does not.
8. **GitHub release** from the CHANGELOG entry (`gh release create RELEASE-X.Y.Z`), with the installer
   AND the `.sha256`/`.sha512` files `make izpack-installer` writes next to it. A release without
   them gives a downloader no way to tell a tampered file from the real one.
9. **Open the next cycle**: set `projectVersion` to the next version with a `-SNAPSHOT`
   suffix (`8.3` released -> `8.4-SNAPSHOT`), in the same session as the tag. Without it
   the repository carries the released number while being something else, and every
   local build produces artifacts that name themselves after a release they are not.
   The `releaseVersion` flag in `gradle/packaging.gradle` reads exactly this suffix, so
   a develop that permanently looks like a release version also signs and packages in
   release mode on every developer build.
10. **Post-release**: CHANGELOG link check, CLAUDE.md update if architecture changed,
   lessons-learned.md entry if anything noteworthy happened during the release.

## Troubleshooting

Tests fail right before release: abort, fix in its own commit, restart from step 1.
Checklist items touching safety (tests green, build successful, correct version) are
never skipped, not even on instruction — postpone rather than ship broken.
