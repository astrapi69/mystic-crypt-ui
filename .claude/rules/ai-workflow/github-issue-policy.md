# GITHUB-ISSUE-PFLICHT

Every bug and every issue MUST have a GitHub issue BEFORE the fix begins. Mandatory,
not advisory. Applies to ALL agents.

## Workflow

1. **Search first.** `gh issue list --search "<keywords>" --state all`. If a closed
   issue matches and the bug recurred, REOPEN it rather than filing a duplicate.
2. **No fix without an issue.** If none exists, create one (`bug` label, enough context
   that the fix is actionable without follow-up) BEFORE touching code. Applies
   retroactively: a NEW bug discovered while working on another one gets its own issue
   before its fix.
3. **No commit without an issue reference.** Commit subject and PR cite the number —
   `(#NN)` or `(fixes #NN)`.
4. **Verify the premise before filing.** If a pre-implementation check shows the
   reported defect does not exist, surface that finding instead — a false issue is worse
   than no issue.

## Lifecycle

- Issues are closed by the fix, not by hand: `Closes #NN` / `Fixes #NN` in the commit or
  PR body so merging auto-closes.
- Sub-issue of an umbrella: `Closes #<sub-issue>` (auto-close) plus `Refs #<umbrella>`
  (traceability). `Refs` alone does not close. The umbrella stays open until all
  sub-issues are closed.
- After a merge, verify BOTH: the issue is actually closed AND the expected files are on
  the target branch (see lessons-learned.md "After a merge").

## Issues as a work queue

On "weiter" / "work through the bugs": treat `gh issue list --label bug --state open` as
the queue. Priority: explicit P-labels; then data-loss / security; then reproducible
crashes; then the rest — smallest scope first within a tier. For each: fix + regression
test in the same commit, conventional commit citing the issue, PR. Report status after
each; do not wait for confirmation between issues.
