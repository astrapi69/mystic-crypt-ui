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

## Priority follows damage, not effort

`P0` a released version is losing or exposing data right now: fix and release today,
everything else stops. `P1` data loss, security, or a gate that cannot report - the next
release carries it. `P2` important, scheduled next. `P3` small or cosmetic: it confuses,
it destroys nothing. `P4` worth doing and nobody is waiting: picked up when something else
touches the area, not scheduled on its own. `P5` recorded so it is not rediscovered, and
not planned - the honest form of `wontfix`, with the reason in the issue.

A one-line fix for a defect that loses data is P1; a week of work on a confusing dialog is
P3. Sorting by effort is what makes the colour a negotiation again at the next issue.

The line that matters most is P0 against P1, because it is the only one that interrupts
work in progress: P1 is "the next release does not go out without this", P0 is "a release
goes out today because of this".

The two at the bottom earn their place only by meaning something an empty label does not:
P4 says somebody will do it while passing, P5 says nobody will and here is why. If either
becomes a place things go to be forgotten quietly, it is doing `wontfix`'s job badly and
should be dropped.

Something that waits for a NAMED condition is not a stage: it gets `blocked` and the
condition in writing (#301 waits for the second plugin that opts into the public state).
A condition carried as P3 turns "not yet" into "some day".

## A decision an issue reserves is taken in the issue

When an issue names a question as open - "to be decided", "not to implement blindly", a
choice listed for the maintainer - that decision is made in the issue, in writing, before
the code that depends on it. It is not made inside the commit that implements it, however
reasonable the choice is: an issue with zero comments and an answer buried in a commit
body leaves nobody able to see that a question was ever asked (#272).

If it was already decided in a commit: the commit stands, nothing is reverted. The
decision is then made again, visibly, in the issue - what was built, what the
alternatives were, and the question put to the maintainer.

## Issues as a work queue

On "weiter" / "work through the bugs": treat `gh issue list --label bug --state open` as
the queue. Priority: explicit P-labels; then data-loss / security; then reproducible
crashes; then the rest — smallest scope first within a tier. For each: fix + regression
test in the same commit, conventional commit citing the issue, PR. Report status after
each; do not wait for confirmation between issues.
