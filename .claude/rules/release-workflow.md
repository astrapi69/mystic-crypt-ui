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
- **A hotfix is a patch release built from the release it patches, not from `develop`.**
  When `develop` already carries a `feat` since the last release, a release from it is a minor
  by SemVer, and a patch that has to go out carries only fixes: branch `hotfix/X.Y.Z` from
  `RELEASE-X.Y`, pull requests against that branch (`make merge-pr PR=n TARGET=hotfix/X.Y.Z`;
  CI runs on `hotfix/**`), the steps below run on the branch, the tag names its head and
  `master` moves onto the tag. Afterwards the WHOLE branch is merged into `develop` - never
  cherry-picked - with the conflicts resolved there under the full gate, and the branch is
  deleted. Nothing on a hotfix branch changes the build, the release path or a persisted
  format: those go to `develop` and wait for the minor. First case: 8.5.1 (#395), where
  `develop` held three `feat` commits and the three P1 fixes had to reach 8.5 users alone.

## Steps

1. **Capture state**: `git tag --sort=-creatordate | head -5`, commits since last tag
   (`git log $(git describe --tags --abbrev=0)..HEAD --oneline --no-merges`). Show the
   summary and wait for confirmation.
2. **Pick the version** per SemVer, propose with rationale, wait for OK.
3. **CHANGELOG.md**: grouped entry (Breaking / Added / Changed / Fixed / Security),
   summarized for humans. Commit `docs: changelog for vX.Y.Z`. **The release entry REPLACES
   the unreleased draft, it is not written above it.** Both carried a `Version X.Y (unreleased)`
   heading for the same cycle at 8.5 - the draft that had accumulated day by day, and the
   release entry written at cut time - and neither was deleted, so the file carried the same
   version twice with different wording until a later audit merged them by hand (#356). Fold
   whatever the draft said that the release entry does not, keep the release entry's wording
   where both describe the same change, and delete the draft heading in the same commit that
   adds the release entry.
4. **Bump** `projectVersion` in `gradle.properties`.
   **The bump is a CI-visible change, not a local one.** Dropping `-SNAPSHOT` flips every
   release switch at once, and since #333 CI publishes the host locally to build the plugins:
   on a release version that pulled in `signMavenJavaPublication`, which has no key on a runner
   (the signing keys are on the maintainer's machine by decision, which is why a release is cut
   locally). The 8.6 bump died there before the first test ran, and the repair had to go in
   ahead of the release: a LOCAL publish without a key signs nothing and says so, a REMOTE one
   still fails (#431). So a bump that reddens CI in a signing, publishing or packaging task is
   this switch, not the release being wrong.
5. **Full gate** (ALL mandatory; a red result aborts the release):
   - `make build-full`, under the Xvfb harness — it runs the plugin builds, the packaging,
     EVERY test including the UI e2e suite, spotless and the license check. The plugins are
     part of it since #333: `./gradlew build` never built them, so the 54 end-to-end tests
     that install one skipped, and a release could be cut with every plugin feature
     unverified while the gate read green. `make test` and
     `make test-e2e` are not run beside it, but not because one contains the other: since
     #319 the suites are DISJOINT source sets - `test` (unit), and the end-to-end tasks
     `e2eTest`, `e2eLockTest` and `e2eKdbxTest`, each with its own classes and its own cache
     key. What puts all of them inside `build-full` is `check.dependsOn` on each
     (`gradle/testing.gradle`). Running the e2e suites three times measures nothing new.
   - the gate reports what it measured: since #306 the build prints it itself, one line
     per suite, plus an `executed in this build` line that is ABSENT when the task was
     UP-TO-DATE or FROM-CACHE and only restored its results. Read it, and read the XML in
     `build/test-results/test`, `e2eTest`, `e2eLockTest` and `e2eKdbxTest` when a number is in
     doubt.
     An empty result set is not a green gate.
   - force the TEST TASKS only, each one: `./gradlew test --rerun e2eTest --rerun
     e2eLockTest --rerun e2eKdbxTest --rerun`, then `./gradlew build`. `--rerun` is a task
     option and binds to the task named right before it - written once at the end it forced
     only the last task, measured in the #409 gate as `> Task :test UP-TO-DATE` (#410).
     Re-running the unit suite and letting the UI suites come back from the cache inverts the
     whole reason for the step (#330). This is the release gate only: CI builds without
     `--rerun`, so an unchanged suite comes back from the cache there (#319). Not `--rerun-tasks` either, which also throws away the compile, the jar, the
     javadoc, spotless and the packaging - none of which has an input Gradle cannot see.
     The UI suite does: a display and a window manager are in no cache key, and both
     produced a green-looking result that said nothing about this machine (8.5).
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
7. **Move `master` onto the release.** Push the tagged COMMIT onto `master`:
   `git push origin RELEASE-X.Y.Z^{}:master`. The `^{}` is not decoration - it dereferences the
   annotated tag to the commit it points at, and without it the push is rejected, because a branch
   must point at a commit and `RELEASE-X.Y.Z` is a tag object (measured at 8.5, where the rule said
   "the tagged commit" and the hand typed the tag). `master` holds releases - that rule was written down and then not followed for
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
