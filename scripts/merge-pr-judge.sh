#!/usr/bin/env bash
#
# Judges a pull request's check rollup: exit 0 means the merge may proceed, exit 1 names why not.
#
# Extracted from merge-pr.sh so that the judgement can be tested against fixtures without touching
# GitHub (#867). This commit carries today's behaviour unchanged: every check in the rollup has to
# be SUCCESS, required or not. The distinction between required and non-required checks is the next
# commit's change; the tests pin today's behaviour first so the change shows as turned assertions.
#
# Usage: merge-pr-judge.sh <required-contexts, comma-separated>
# stdin: one line per rollup entry: <name> TAB <reported: true|false> TAB <conclusion>
#        where <conclusion> is the raw check-run conclusion (SUCCESS, FAILURE, NEUTRAL, SKIPPED,
#        CANCELLED, TIMED_OUT, ACTION_REQUIRED, STARTUP_FAILURE, STALE) or commit-status state
#        (SUCCESS, FAILURE, ERROR, PENDING, EXPECTED) - both sources feed the same rollup.
set -euo pipefail

required_csv="${1:?usage: merge-pr-judge.sh <required-contexts, comma-separated>}"

say() { printf '%s\n' "$*"; }
fail() { printf 'merge-pr: %s\n' "$*" >&2; exit 1; }

lines="$(cat)"
total="$(printf '%s\n' "$lines" | awk -F'\t' '$1 != ""' | grep -c . || true)"
[ "$total" -gt 0 ] || fail "no checks at all - a pull request nothing ran on is not green"

# a required context that never reported is not green (AG-077), whatever the rest says
unreported=""
IFS=',' read -ra required_contexts <<< "$required_csv"
for context in "${required_contexts[@]}"; do
  [ -n "$context" ] || continue
  printf '%s\n' "$lines" | awk -F'\t' -v c="$context" '$1 == c && $2 == "true" {found=1} END {exit !found}' \
    || unreported="${unreported:+$unreported, }$context"
done
[ -z "$unreported" ] || fail "required but not reported: $unreported"

failed="$(printf '%s\n' "$lines" | awk -F'\t' '$1 != "" && $3 != "SUCCESS" {print $1}' | paste -sd, -)"
[ -z "$failed" ] || fail "these checks are not SUCCESS: $failed"

say "all $total checks green, the required ones among them"
