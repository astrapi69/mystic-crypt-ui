# Changelog fragments

Every user-visible change brings its CHANGELOG entry along in the same pull request, as one file
here, so the entry lands in the same squash commit as the change (squash pilot, 2026-11-11).
CHANGELOG.md itself is never edited directly (DC-007): the release pull request assembles these
fragments under the new version with `scripts/assemble_changelog.py` and deletes them.

- **Name**: `<pull request number>.<section>.md`, for example `899.fixed.md`. The sections are the
  ones CHANGELOG.md already uses: `security`, `added`, `changed`, `fixed`, `deprecated`, `removed`.
- **Content**: one bullet (starting with `- `), English (DC-001), written for a human reader in the
  style of the existing entries - what changed and what it means for the user, with the issue
  number, never the raw commit message. Continuation lines are indented two spaces, as in
  CHANGELOG.md.
- **Who needs one**: a pull request whose title is `feat`, `fix` or `perf`, and every breaking
  change (`!` in the title). `chore`, `test`, `refactor`, `ci`, `build` and `docs` need none.
  `make merge-pr` refuses a pull request that is missing a required fragment and names the
  expected file.
- **Exception**: a fix for an unpublished security advisory gets its fragment only in the release
  pull request that publishes the advisory (AG-032) - until then the merge is run with the
  fragment check consciously answered by a `security` fragment whose wording does not name the
  hole, or the fix is held back with the release.
