# GITHUB-ISSUE-PFLICHT

A bug or small improvement found here is fixed in the session that finds it, in a PR of its
own (family rule NW-015). It gets a GitHub issue first only when the fix needs the
maintainer's decision, depends on something external or is too big for the session
(NW-016); such an issue follows the workflow below. Applies to ALL agents.

## Workflow

1. **Search first.** `gh issue list --search "<keywords>" --state all`. If a closed
   issue matches and the bug recurred, REOPEN it rather than filing a duplicate.
2. **File it where NW-016 asks for one.** Then create it (`bug` label, enough context
   that the fix is actionable without follow-up) before touching code. A NEW bug
   discovered while working on another one is fixed in a PR of its own, with an issue
   only under NW-016.
3. **When there is an issue, cite it.** A commit subject on the branch names it as
   `(#NN)` (AG-068). The squash commit on develop does not: under the squash pilot
   (NW-026) its subject is the PR title with the PR's number, so the title carries no
   issue number (the maintainer's decision of 2026-11-11, #902). `make merge-pr` builds
   that subject and refuses a title that contains `(#`. Auto-merge checks nothing in the
   title and gives that subject only with `--subject "<title> (#<pr>)"`: the repository's
   squash default takes the subject of a PR's only commit, issue number included. The
   issue goes into the PR body instead: `Closes #NN` when the PR completes the fix, or
   `Refs #NN` and "#NN stays open" when it does not (AG-069, lessons-learned.md).
4. **Verify the premise before filing.** If a pre-implementation check shows the
   reported defect does not exist, surface that finding instead - a false issue is worse
   than no issue.

## Lifecycle

- Issues are closed by the fix, not by hand: `Closes #NN` / `Fixes #NN` in the PR body,
  which GitHub reads when the PR is merged into develop, the default branch. `make
  merge-pr` also makes the PR body the body of the squash commit; auto-merge does so only
  when `--body-file` is given the PR body, otherwise GitHub writes the messages of the
  branch commits. A PR into `hotfix/X.Y.Z` closes nothing when it merges, since GitHub acts
  on the keywords for the default branch only: look at the issue again once the hotfix
  branch is merged into develop.
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
condition in writing (#301 waited for the second plugin that opts into the public state).
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
crashes; then the rest - smallest scope first within a tier. For each: the regression test
first, green on today's behaviour, then the fix that turns it, two commits (RL-002), each
citing the issue, and a PR. Report status after each; do not wait for confirmation between
issues.
