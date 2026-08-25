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
   - `make test`
   - `make test-e2e` (Xvfb harness, see lessons-learned.md)
   - `make build-full`
   - `make izpack-installer` (installer smoke — the plugin set in `install.xml` must
     match the Makefile `plugins:` list)
   - dependency currency check: `make dependency-updates` — routine patch/minor bumps as
     part of the release; major bumps get their own session, never bundled in.
6. **Tag + push**: `make tag-release` (or the manual `git tag -a vX.Y.Z` + push).
7. **GitHub release** from the CHANGELOG entry (`gh release create vX.Y.Z`).
8. **Post-release**: CHANGELOG link check, CLAUDE.md update if architecture changed,
   lessons-learned.md entry if anything noteworthy happened during the release.

## Troubleshooting

Tests fail right before release: abort, fix in its own commit, restart from step 1.
Checklist items touching safety (tests green, build successful, correct version) are
never skipped, not even on instruction — postpone rather than ship broken.
