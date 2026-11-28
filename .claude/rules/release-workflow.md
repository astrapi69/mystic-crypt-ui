# Release Workflow

Prompt triggers: "release new version", "new release".

## Ground rules

- A release is built with the JDK `.sdkmanrc` names, which the Makefile pins and `sdk env` sets the
  shell to. A newer patch release of it is taken on deliberately, in a small pull request of its own
  that passes the gate, never automatically (#586).
- Version source: `gradle.properties` (`projectVersion`), the ONLY hand-edited version
  field. Nothing else carries an independent version literal; if one appears, fix the
  derivation, not the literal.
- Tests must be green: red tests block the release, no exceptions, no "disable the test
  for this release".
- Every release is a logical boundary; do not release mid-feature.
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
3. **Dependency currency check, then CHANGELOG.md.** The check runs in GitHub Actions (AG-021):
   `gh workflow run gradle.yml --ref develop -f dependency-updates=true`; the log counts the updates,
   the artifact `dependency-updates-report` lists them. Patch and minor bumps go into the release,
   and so do major bumps of test and build dependencies, which reach no consumer and no published
   POM; a major bump of a runtime dependency gets its own session, never bundled in (#749). A change
   to the build or the release path itself is NOT bundled into a security release. The bump pull
   requests and their `changelog.d/` fragments land before the release pull request, so a release
   needs one gate (the maintainer's decision of 2026-11-24).
   Since the squash pilot (2026-11-11) the entries live as fragments in
   `changelog.d/`, one per pull request, brought along by the change itself (see
   `changelog.d/README.md`). The release pull request runs
   `python3 scripts/assemble_changelog.py X.Y.Z`, which inserts the grouped version block in
   the file's own shape and deletes the fragments; commit both as `docs: changelog for vX.Y.Z`,
   write the intro sentence under the version heading by hand. For an advisory published with
   this release the release pull request carries one neutral SECURITY line, without the
   advisory's id and without a description of the defect (AG-032, AG-117); the script refuses a
   fragment that names an advisory not yet published. The ids and the descriptions appear first
   in the advisories, published after the tag. Once an advisory is published, a docs pull
   request adds only its id to that neutral line in the version block of the release that fixed
   it and changes nothing else; the description stays in the advisory (DC-007, #1021). Nothing
   is collected privately for this repository any more, and CHANGELOG.md is never edited outside
   the release pull request and that docs pull request. The old failure this replaces: a
   hand-kept draft and the cut-time entry carried the same version twice (#356).
4. **Bump** `projectVersion` in `gradle.properties`.
   **The bump is a CI-visible change, not a local one.** Dropping `-SNAPSHOT` flips every
   release switch at once, and since #333 CI publishes the host locally to build the plugins:
   on a release version that pulled in `signMavenJavaPublication`, which has no key on a runner
   (the signing keys are on the maintainer's machine by decision, which is why signing is local,
   AG-021). The 8.6 bump died there before the first test ran, and the repair had to go in
   ahead of the release: a LOCAL publish without a key signs nothing and says so (#431); there
   is no remote publish since #453. So a bump that reddens CI in a signing, publishing or packaging task is
   this switch, not the release being wrong.
5. **Full gate**, once, on the merged state that will be tagged (ALL mandatory; a red result
   aborts the release).
   - **the gate runs in GitHub Actions**, never on the maintainer's machine (AG-021, RL-072, #1156):
     `gh workflow run gradle.yml --ref <ref> -f force=true` and the same for `e2e.yml`, `<ref>` the
     branch to tag; both runs' `headSha` must be the commit to be tagged. Forced, gradle.yml
     builds every plugin with its tests and the script test suites, packages the runnable jar
     unsigned (`createAllDependendiesJar`; the installer refuses to be built unsigned), runs
     `test --rerun` and Spotless (license header since #686, plugin builds since #666); e2e.yml
     runs `e2eTest --rerun e2eLockTest --rerun e2eKdbxTest --rerun`. The plugins are in the gate
     since #333: `./gradlew build` never built them, so the 54 end-to-end tests that
     install one skipped while the gate read green. Since #319 the four suites are DISJOINT source
     sets, each with its own cache key. Of the tasks the plain `./gradlew build` of the former local
     gate ran, only `jacocoTestReport` (a report without a floor) runs in neither forced run; javadoc
     runs in both, through the `publishToMavenLocal` every plugin build needs.
   - the gate reports what it measured (#306): one line per suite,
     `<suite>: N classes, M tests, F failures, E errors, S skipped`, and
     `<suite>: executed in this build`, ABSENT when the task only restored its results; then
     `scripts/check-forced-run-results.sh` prints `<suite>: N result files, all from the forced run`
     and fails on a missing or an older file (#630: the final `build` of the former local gate once
     restored an older run's results over the forced ones while the gate read green). Read these
     lines in the logs, and the XML in the artifacts `test-reports` and `e2e-test-reports` when a
     number is in doubt; kept 14 days, they go with the release's evidence.
     An empty result set is not a green gate.
   - force the TEST TASKS only, each by name with its own `--rerun` (RL-123): the option binds to
     the task named right before it, and written once at the end it forced only the last task,
     measured in the #409 gate as `> Task :test UP-TO-DATE` (#410). Re-running the unit suite and
     letting the UI suites come back from the cache inverts the reason for the step (#330). Not
     `--rerun-tasks` either, which also throws away the compile, the jar, the javadoc, spotless and
     the packaging, none of which has an input Gradle cannot see. The UI suite does: a display and
     a window manager are in no cache key, and both produced a green-looking result that said
     nothing about the machine it ran on (8.5). A run on a push or pull request builds without
     `--rerun`, so an unchanged suite comes back from the cache there (#319).
   - **package from the state that will be tagged.** The release PR is merged FIRST; the installer
     and its checksums are built from exactly the commit the gate found green, and the tag names
     that commit. Building beforehand from the release branch ties the release to a commit the tag
     never names, unless the merge happens to be a fast-forward, which is luck, not a property
     (2026-09-09: it was, so 8.4 kept its installer).
   - **signing, packaging and verify-installer stay local** (AG-021), in a clean worktree whose
     `git rev-parse HEAD` is both runs' `headSha`, without the tests the gate ran there, from a
     normal terminal:
     `make without-slot TARGET=izpack-installer PLUGIN_TESTS= PLUGIN_ZIP_TESTS= SCRIPT_TESTS=no`
     (the plugin set in `install.xml` must match the Makefile `plugins:` list).
   - **verify the installer AFTER the checksums are written**, against the file the checksums
     describe: it opens as an archive, and
     `sha256sum -c` / `sha512sum -c` pass. Verifying before the checksums verifies a file that is
     then replaced: `checksumInstaller` depends on `izPackCreateInstaller`, which re-runs and
     rewrites the jar (measured 2026-09-07: 81743356 bytes verified, 81743359 bytes shipped).
     `make without-slot TARGET=verify-installer PREVIOUS=<last release>` runs these checks and the
     ones 8.14 added (signer as the previous release's, class major, mystic-crypt against Central,
     plugins, the jar tests, a start); run it again on the installer downloaded from the draft release
     (step 8, #793).
   - First release this way: 8.16, gate runs 45600604264 (gradle.yml) and 45600609197 (e2e.yml) on
     `0031c500`, which `RELEASE-8.16` names.
   - **the maintainer runs the release smoke by hand**: the `release smoke` subset of
     `docs/manual-tests/testplan-mystic-crypt-ui.md` (the plan gives its duration), against the BUILT
     installer, after the checksums are written. The tag waits for his go after this hand test
     (#948).
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
8. **GitHub release, a draft until every station is green** (RL-083). From the CHANGELOG entry:
   `gh release create RELEASE-X.Y.Z --draft --verify-tag --title "mystic-crypt-ui X.Y.Z"
   --notes-file <notes> <installer> <installer>.sha256 <installer>.sha512`, the installer AND the
   checksum files `make izpack-installer` writes next to it. A release without them gives a
   downloader no way to tell a tampered file from the real one. `--verify-tag` refuses when the tag
   of step 6 is not on GitHub; without it `gh` creates the tag from the head of the default branch,
   develop. The draft is published (`gh release edit RELEASE-X.Y.Z --draft=false`) only when
   - every run of the tagged commit has concluded with success
     (`gh run list --commit <sha> --json workflowName,status,conclusion`): at 8.16 the release was
     published at 12:58:54 UTC while three runs of the push to `master` were still running, the last
     until 13:06:20 UTC;
   - `gh release download RELEASE-X.Y.Z -D <a new empty directory>` gives exactly the three files,
     each identical to the local one (`cmp`), so the set is the expected one and not a rebuild;
   - `make without-slot TARGET=verify-installer PREVIOUS=<last release>
     INSTALLER=<that directory>/mystic-crypt-ui-X.Y.Z-installer.jar` passed (the second run of step 5).
   `gh` 2.46.0 finds a draft by its tag for `view`, `download` and `edit`, and a draft creates no
   tag (measured 2026-11-26 on a throwaway draft in a private repository). The release text names
   the JDK it was built with, as `java -version` of the Makefile's JAVA_HOME prints it (#586).
   The release text names an advisory id only once that advisory is published (AG-117); until then it
   carries the neutral SECURITY line of step 3.
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
never skipped, not even on instruction; postpone rather than ship broken.
