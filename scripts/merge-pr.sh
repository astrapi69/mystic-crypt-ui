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
