#!/usr/bin/env bash
#
# Judges a pull request's check rollup: exit 0 means the merge may proceed, exit 1 names why not.
#
# The rules (#867, acceptance criteria in the issue, decided by the maintainer on 2026-11-11):
# - every REQUIRED context (from branch protection) has to have reported SUCCESS; one that never
#   reported is not green (AG-077), one that reported anything else refuses the merge.
# - a NON-REQUIRED check with conclusion NEUTRAL or SKIPPED is informational: named, not blocking.
#   Socket Security reports NEUTRAL on a pull request without dependency changes (#863).
# - a NON-REQUIRED check with conclusion FAILURE, ERROR, TIMED_OUT, ACTION_REQUIRED, CANCELLED,
#   STARTUP_FAILURE or STALE refuses the merge by default: GitGuardian is not a required check
#   here, and a red GitGuardian can mean a secret in the commits (AG-090). The explicit opt-out
#   MERGE_PR_ALLOW_OPTIONAL_FAILURE=1 downgrades these to loud warnings, for the case the issue's
#   criteria recommend ("branch protection defines the required merge gate").
#
# Extracted from merge-pr.sh so that the judgement is testable against fixtures without GitHub.
#
# Usage: merge-pr-judge.sh <required-contexts, comma-separated>
# stdin: one line per rollup entry: <name> TAB <reported: true|false> TAB <conclusion>
#        where <conclusion> is the raw check-run conclusion (SUCCESS, FAILURE, NEUTRAL, SKIPPED,
#        CANCELLED, TIMED_OUT, ACTION_REQUIRED, STARTUP_FAILURE, STALE) or commit-status state
#        (SUCCESS, FAILURE, ERROR, PENDING, EXPECTED) - both sources feed the same rollup.
set -euo pipefail

required_csv="${1:?usage: merge-pr-judge.sh <required-contexts, comma-separated>}"
allow_optional_failure="${MERGE_PR_ALLOW_OPTIONAL_FAILURE:-0}"

say() { printf '%s\n' "$*"; }
fail() { printf 'merge-pr: %s\n' "$*" >&2; exit 1; }

lines="$(cat)"
total="$(printf '%s\n' "$lines" | awk -F'\t' '$1 != ""' | grep -c . || true)"
[ "$total" -gt 0 ] || fail "no checks at all - a pull request nothing ran on is not green"

is_required() {
  local context
  IFS=',' read -ra required_contexts <<< "$required_csv"
  for context in "${required_contexts[@]}"; do
    [ "$1" = "$context" ] && return 0
  done
  return 1
}

# a required context that never reported is not green (AG-077), whatever the rest says
unreported=""
IFS=',' read -ra required_contexts <<< "$required_csv"
for context in "${required_contexts[@]}"; do
  [ -n "$context" ] || continue
  printf '%s\n' "$lines" | awk -F'\t' -v c="$context" '$1 == c && $2 == "true" {found=1} END {exit !found}' \
    || unreported="${unreported:+$unreported, }$context"
done
[ -z "$unreported" ] || fail "required but not reported: $unreported"

failed_required=""
failed_optional=""
informational=""
while IFS=$'\t' read -r name reported conclusion; do
  [ -n "$name" ] || continue
  if is_required "$name"; then
    [ "$conclusion" = "SUCCESS" ] || failed_required="${failed_required:+$failed_required, }$name ($conclusion)"
  else
    case "$conclusion" in
      SUCCESS) ;;
      NEUTRAL|SKIPPED) informational="${informational:+$informational, }$name ($conclusion)" ;;
      *) failed_optional="${failed_optional:+$failed_optional, }$name ($conclusion)" ;;
    esac
  fi
done <<< "$lines"

[ -z "$informational" ] || say "non-required, no verdict (not blocking): $informational"
[ -z "$failed_required" ] || fail "required checks not SUCCESS: $failed_required"
if [ -n "$failed_optional" ]; then
  if [ "$allow_optional_failure" = "1" ]; then
    say "WARNING: non-required checks failed and MERGE_PR_ALLOW_OPTIONAL_FAILURE=1 overrides them: $failed_optional"
  else
    fail "non-required checks failed: $failed_optional - a red GitGuardian can mean a secret (AG-090); MERGE_PR_ALLOW_OPTIONAL_FAILURE=1 overrides deliberately"
  fi
fi

say "every required check is SUCCESS ($required_csv)"
