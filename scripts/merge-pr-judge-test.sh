#!/usr/bin/env bash
#
# Tests for merge-pr-judge.sh against fixtures, no GitHub involved (#867). Run: the script itself.
#
# This commit pins TODAY'S behaviour: a non-required check whose conclusion is NEUTRAL or SKIPPED
# blocks the merge, which is the defect of #867 (measured on #863: Socket Security reported NEUTRAL
# on a docs-only pull request and the script refused). The next commit turns the pinned assertions.
set -euo pipefail

judge="${BASH_SOURCE[0]%/*}/merge-pr-judge.sh"
failures=0

expect() {
  description="$1" expected_exit="$2" required="$3" fixture="$4"
  shift 4
  set +e
  output="$(printf '%s' "$fixture" | env "$@" "$judge" "$required" 2>&1)"
  actual_exit=$?
  set -e
  if [ "$actual_exit" -eq "$expected_exit" ]; then
    printf 'ok: %s\n' "$description"
  else
    printf 'FAIL: %s - expected exit %s, got %s\n%s\n' "$description" "$expected_exit" "$actual_exit" "$output"
    failures=$((failures + 1))
  fi
}

required="build,closing-references,release-freeze"
green_required=$'build\ttrue\tSUCCESS\nclosing-references\ttrue\tSUCCESS\nrelease-freeze\ttrue\tSUCCESS'

# case 1 (#867): required green, one non-required NEUTRAL - today the script refuses
expect "required green + non-required NEUTRAL blocks (today's defect)" 1 "$required" \
  "$green_required"$'\nSocket Security: Pull Request Alerts\ttrue\tNEUTRAL'

# case 2: required green, one non-required SKIPPED - today the script refuses
expect "required green + non-required SKIPPED blocks (today's defect)" 1 "$required" \
  "$green_required"$'\nSocket Security: Pull Request Alerts\ttrue\tSKIPPED'

# case 3: required green, one non-required FAILURE - refused
expect "required green + non-required FAILURE blocks" 1 "$required" \
  "$green_required"$'\nGitGuardian Security Checks\ttrue\tFAILURE'

# case 3b: the same with the opt-out - today there is no opt-out, still refused
expect "non-required FAILURE with MERGE_PR_ALLOW_OPTIONAL_FAILURE=1 blocks (no opt-out today)" 1 "$required" \
  "$green_required"$'\nGitGuardian Security Checks\ttrue\tFAILURE' MERGE_PR_ALLOW_OPTIONAL_FAILURE=1

# case 4: a required check FAILURE - refused
expect "required FAILURE blocks" 1 "$required" \
  $'build\ttrue\tFAILURE\nclosing-references\ttrue\tSUCCESS\nrelease-freeze\ttrue\tSUCCESS'

# case 5: a required check that never reported - refused
expect "required check missing from the rollup blocks" 1 "$required" \
  $'build\ttrue\tSUCCESS\nclosing-references\ttrue\tSUCCESS'

# a stale check run no longer describes the head - refused
expect "non-required STALE blocks" 1 "$required" \
  "$green_required"$'\nGitGuardian Security Checks\ttrue\tSTALE'

# a commit status ERROR is the statuses-API spelling of failure - refused
expect "non-required status ERROR blocks" 1 "$required" \
  "$green_required"$'\ncodecov/patch\ttrue\tERROR'

# all green, required among them - merges
expect "required green + non-required SUCCESS merges" 0 "$required" \
  "$green_required"$'\nGitGuardian Security Checks\ttrue\tSUCCESS'

# an empty rollup is not green
expect "no checks at all blocks" 1 "$required" ""

if [ "$failures" -gt 0 ]; then
  printf '%s test(s) failed\n' "$failures"
  exit 1
fi
printf 'all tests passed\n'
