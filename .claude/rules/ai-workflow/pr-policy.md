# PR-PFLICHT

After ANY code change that is committed and pushed to a branch, a pull request against
`develop` MUST be opened — whether or not the task asked for one. Mandatory, not
advisory.

## Core rules

- **"No PR, wasn't requested" is NOT a valid completion report.** A pushed branch with
  no PR is unfinished work.
- **Opening the PR is the last step of the change**, in the same turn as the push.
- The PR is the hand-off surface: the diff, testing evidence, the `Closes #NN`
  auto-close.

## Exceptions

1. **No code change** — pure analysis/status tasks that push nothing. (A task that DOES
   change committed files — including docs and `.claude/rules/` — gets a PR.)
2. **Release freeze** (see vibe-coding.md). Exception within the exception: a P0 hotfix
   blocking the release.
3. **The user explicitly said "push only, no PR"** for this task. Absence of an explicit
   opt-in is never a reason to skip.

## Conventions

- Against `develop` (gitflow; `master` holds releases only).
- Body cites the issue with a closing keyword (GITHUB-ISSUE-PFLICHT).
- One concern per PR; docs-only changesets get their own PR, not mixed into features.
