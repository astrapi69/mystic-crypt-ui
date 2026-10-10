#!/usr/bin/env bash
#
# Brings the local copy of a branch up to the remote after a merge, the last step of
# scripts/merge-pr.sh, without moving HEAD and without touching uncommitted work.
#
# The merge itself is server-side (gh pr merge), so nothing here needs the branch checked out, and
# nothing here may move the HEAD of the worktree it runs in: the script it ends can run for as long
# as CI takes, and whoever works in that checkout meanwhile is on their own branch (#340, #496).
#
# Where the branch is checked out decides how it is updated, because git refuses
# 'git fetch origin <branch>:<branch>' while the branch is checked out in ANY worktree, not only in
# this one. That refusal ended merge-pr with exit 128 after a merge that had happened, whenever it
# was started from a linked worktree while the main checkout stood on develop (#543):
#   checked out nowhere           the ref is fast-forwarded by the fetch
#   checked out in a worktree     that worktree is fast-forwarded with merge --ff-only, here or
#                                 elsewhere, when it is clean; with uncommitted work it is left as
#                                 it is, and the command that brings it up is printed
# A branch that cannot be fast-forwarded - it carries a commit of its own - is not updated either.
#
# The exit status is 0 in every one of those cases: the merge happened, and a red status after it
# reads as a failed merge and stops a chain written with '&&'. What was not updated is said.
#
# Usage: scripts/update-local-branch.sh <branch>
set -euo pipefail

BRANCH="${1:?usage: scripts/update-local-branch.sh <branch>}"

say() { printf '%s\n' "$*"; }

# the worktree the branch is checked out in, or nothing
checked_out_in() {
  git worktree list --porcelain | awk -v ref="refs/heads/$1" '
    /^worktree / { tree = substr($0, 10) }
    $0 == "branch " ref { print tree; exit }'
}

git fetch --quiet origin
tree="$(checked_out_in "$BRANCH")"

if [ -z "$tree" ]; then
  if git fetch --quiet origin "$BRANCH:$BRANCH" 2>/dev/null; then
    say "local $BRANCH is now $(git rev-parse --short "$BRANCH")"
  else
    say "note: local $BRANCH was not updated - it is not behind origin/$BRANCH alone, it has a"
    say "      commit of its own. Look at it with: git log --oneline origin/$BRANCH...$BRANCH"
  fi
elif [ -n "$(git -C "$tree" status --porcelain)" ]; then
  say "note: $BRANCH is checked out in $tree with uncommitted changes - left as it is, $BRANCH was"
  say "      not updated. Bring it up there when the work allows: git merge --ff-only origin/$BRANCH"
elif git -C "$tree" merge --quiet --ff-only "origin/$BRANCH" >/dev/null 2>&1; then
  say "local $BRANCH is now $(git rev-parse --short "$BRANCH") (checked out in $tree)"
else
  say "note: $BRANCH, checked out in $tree, was not updated - it has a commit of its own. Look at"
  say "      it with: git log --oneline origin/$BRANCH...$BRANCH"
fi
