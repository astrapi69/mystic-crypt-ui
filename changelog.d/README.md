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
- **Exception**: a fix for an unpublished security advisory brings no fragment that names the
  advisory or describes the hole (AG-032, AG-117). The release pull request that publishes the
  advisory carries one neutral line in the `security` section instead, for example "Fixes a
  security issue; its advisory follows after the release is published." `assemble_changelog.py`
  refuses a fragment naming an advisory id that is not published. How CHANGELOG.md takes the id
  and the description afterwards is open until #1021 closes.
