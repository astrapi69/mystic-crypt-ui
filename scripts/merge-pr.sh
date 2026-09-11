#!/usr/bin/env bash
#
# Merges a pull request the way the rules describe, in order, stopping at the first step that fails.
#
# The sequence is not new; typing it by hand is what went wrong. Two pull requests were closed
# rather than merged because a branch deletion chained with ';' ran after a failed merge (#299,
# #312), and one was merged while its build check was still running (#317). This runs the same
# steps with 'set -e', so a failure stops what comes after it instead of being stepped over.
#
# Usage: scripts/merge-pr.sh <pr-number> [target-branch]
set -euo pipefail

PR="${1:?usage: scripts/merge-pr.sh <pr-number> [target-branch]}"
TARGET="${2:-develop}"
WAIT_SECONDS="${MERGE_PR_WAIT_SECONDS:-2400}"
POLL_SECONDS=20

say() { printf '%s\n' "$*"; }
die() { printf 'merge-pr: %s\n' "$*" >&2; exit 1; }

# A dirty tree is somebody's work in progress, and this script used to be able to move the branch
# under it. It no longer does, but a merge started from a tree with uncommitted changes is still a
# sign that two things are happening at once, and the cost of saying so is one line (#340)
if [ -n "$(git status --porcelain)" ]; then
  say "note: this worktree has uncommitted changes. They are not touched - the merge no longer"
  say "      changes the checked-out branch - but check that you meant to merge right now"
fi

state="$(gh pr view "$PR" --json state -q .state)"
[ "$state" = "OPEN" ] || die "pull request #$PR is $state, not OPEN"

say "waiting for the checks on #$PR (up to $((WAIT_SECONDS / 60)) minutes)"
waited=0
while :; do
  pending="$(gh pr view "$PR" --json statusCheckRollup \
    -q '[.statusCheckRollup[] | select(.status != "COMPLETED")] | length')"
  [ "$pending" = "0" ] && break
  [ "$waited" -ge "$WAIT_SECONDS" ] && die "still $pending check(s) running after ${waited}s - not merging"
  sleep "$POLL_SECONDS"
  waited=$((waited + POLL_SECONDS))
done

failed="$(gh pr view "$PR" --json statusCheckRollup \
  -q '[.statusCheckRollup[] | select(.conclusion != "SUCCESS")] | map(.name) | join(", ")')"
if [ -n "$failed" ]; then
  gh pr checks "$PR" || true
  die "these checks are not SUCCESS: $failed"
fi

total="$(gh pr view "$PR" --json statusCheckRollup -q '.statusCheckRollup | length')"
[ "$total" -gt 0 ] || die "#$PR reports no checks at all - a pull request nothing ran on is not green"
say "all $total checks green"

gh pr merge "$PR" --merge --delete-branch

# The worktree is left exactly where it was (#340). This used to end with
# 'git checkout develop && git pull --ff-only', which is the right sequence and the wrong place:
# the script can run for as long as CI takes, and while it does, whoever is working in this
# checkout is on their own branch. Changing HEAD under them puts their next commit on the target
# branch - measured today, caught before a push, and the next time the caught step might be the
# push.
#
# gh pr merge is server-side, so nothing here needs the target checked out. This fast-forwards the
# local branch from the remote without touching HEAD; when the target IS checked out, that form is
# refused by git, so the ordinary pull is used for that case alone.
git fetch --quiet origin
if [ "$(git rev-parse --abbrev-ref HEAD)" = "$TARGET" ]; then
  git merge --ff-only "origin/$TARGET"
else
  git fetch --quiet origin "$TARGET:$TARGET"
fi
say "merged #$PR - $TARGET is now $(git rev-parse --short "$TARGET"): $(git log -1 --format=%s "$TARGET")"
