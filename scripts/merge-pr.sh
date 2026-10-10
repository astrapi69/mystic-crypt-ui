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

# The target is where this script updates the local branch afterwards, and 'gh pr merge' merges
# into whatever the pull request's base is. When the two differ - a hotfix pull request opened
# against develop by habit, or TARGET given for a pull request that targets develop - the merge
# would land somewhere other than where the caller is looking. So the two have to agree (#395)
base="$(gh pr view "$PR" --json baseRefName -q .baseRefName)"
[ "$base" = "$TARGET" ] || die "pull request #$PR targets '$base', but the merge target given here is
  '$TARGET' - one of the two is wrong, and merging would put the change where nobody is looking"

# What a merge into TARGET waits for: the required checks of its branch protection, each until it
# has REPORTED. Waiting only for the checks already in the rollup missed the ones not created yet -
# codecov posts after the build, and astrapi69/mystic-crypt#183 was merged before its codecov result
# existed (#493). A target without required checks is refused: nothing then says what green means.
required_checks() {
  gh api "repos/{owner}/{repo}/branches/$1/protection/required_status_checks" \
    -q '.contexts[]' 2>/dev/null || true
}
required="$(required_checks "$TARGET")"
if [ -z "$required" ] && [[ "$TARGET" == hotfix/* ]]; then
  required="$(required_checks develop)"
  [ -n "$required" ] && say "note: $TARGET has no protection of its own - waiting for develop's required checks"
fi
[ -n "$required" ] || die "'$TARGET' has no required checks in its branch protection - nothing says
  what green means for a merge there, so this does not merge (#493)"
say "required on $TARGET: $(printf '%s\n' "$required" | paste -sd, - | sed 's/,/, /g')"

# One line per check in the rollup: name, whether it is done, whether it succeeded. Check runs carry
# name/status/conclusion, commit statuses context/state - both are read, or a status would look
# pending forever
rollup() {
  gh pr view "$PR" --json statusCheckRollup -q '.statusCheckRollup[] |
    [(.name // .context),
     (if .status then (.status == "COMPLETED") else ((.state // "PENDING") | IN("PENDING", "EXPECTED") | not) end),
     ((.conclusion // .state) == "SUCCESS")] | @tsv'
}

say "waiting for the checks on #$PR (up to $((WAIT_SECONDS / 60)) minutes)"
waited=0
while :; do
  lines="$(rollup)"
  running="$(printf '%s\n' "$lines" | awk -F'\t' '$1 != "" && $2 == "false" {print $1}' | paste -sd, -)"
  unreported=""
  while IFS= read -r check; do
    [ -n "$check" ] || continue
    printf '%s\n' "$lines" | awk -F'\t' -v c="$check" '$1 == c && $2 == "true" {found=1} END {exit !found}' \
      || unreported="${unreported:+$unreported, }$check"
  done <<< "$required"
  [ -z "$running" ] && [ -z "$unreported" ] && break
  [ "$waited" -ge "$WAIT_SECONDS" ] && die "after ${waited}s - still running: ${running:-none};
  required but not reported: ${unreported:-none} - not merging"
  sleep "$POLL_SECONDS"
  waited=$((waited + POLL_SECONDS))
done

failed="$(printf '%s\n' "$lines" | awk -F'\t' '$1 != "" && $3 == "false" {print $1}' | paste -sd, -)"
if [ -n "$failed" ]; then
  gh pr checks "$PR" || true
  die "these checks are not SUCCESS: $failed"
fi

total="$(printf '%s\n' "$lines" | awk -F'\t' '$1 != ""' | wc -l)"
[ "$total" -gt 0 ] || die "#$PR reports no checks at all - a pull request nothing ran on is not green"
say "all $total checks green, the required ones among them"

# Merged without --delete-branch: when the local branch it deletes is the one checked out, gh switches
# to the base branch and pulls it - HEAD moved, and the running script file was replaced under bash in
# the middle of the run (#496). The remote head branch goes through the API instead, which touches no
# checkout; the local branch is left alone, because deleting it is the person's decision and leaving
# it costs nothing. This repository also deletes head branches on merge by itself, so the branch may
# be gone already, or go while this runs - what is reported is what the API says afterwards.
head="$(gh pr view "$PR" --json headRefName -q .headRefName)"
cross="$(gh pr view "$PR" --json isCrossRepository -q .isCrossRepository)"
gh pr merge "$PR" --merge
if [ "$cross" = "true" ]; then
  say "the head branch '$head' lives in a fork - not deleted from here"
else
  gh api -X DELETE "repos/{owner}/{repo}/git/refs/heads/$head" --silent 2>/dev/null || true
  if gh api "repos/{owner}/{repo}/branches/$head" --silent 2>/dev/null; then
    say "note: the remote branch '$head' is still there - delete it by hand if it is not needed"
  else
    say "the remote branch '$head' is gone; the local one is left as it is"
  fi
fi

# The local copy of the target follows the merge without moving HEAD and without touching
# uncommitted work, wherever the target is checked out (#340, #496, #543). It is a script of its own
# so that it can be tested against real repositories; its exit status is 0 also when it leaves the
# branch as it was, because the merge above happened either way.
git fetch --quiet origin
say "merged #$PR - origin/$TARGET is now $(git rev-parse --short "origin/$TARGET"): $(git log -1 --format=%s "origin/$TARGET")"
"${BASH_SOURCE[0]%/*}/update-local-branch.sh" "$TARGET"
