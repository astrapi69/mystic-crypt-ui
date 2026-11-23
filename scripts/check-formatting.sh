#!/usr/bin/env bash
#
# Refuses a commit whose files Spotless would format (#592). The pre-commit hook runs it; CI checks
# the same with spotlessCheck in the build, which is where a forgotten spotlessApply used to surface,
# a gate run or a CI run later.
#
# Only what Spotless covers in this build counts, and only when the commit stages it:
#   src/**/*.java                          spotlessJavaCheck
#   *.gradle, *.md, .gitignore at the top  spotlessMiscCheck
# The plugin builds have no Spotless of their own, and a commit that touches nothing of the above
# costs no Gradle run. With a daemon running, a check takes under a second (measured 0.5 to 0.9 s on
# 2026-10-17; 5.3 s without one).
#
# Spotless checks the files as they are in the working tree, so a file staged in one form and edited
# further is checked in its edited form.
#
# Gradle runs only inside the build slot (#1116): see run_gradle below.
#
#   scripts/check-formatting.sh            GRADLEW overrides the Gradle wrapper, for the test
set -euo pipefail

root="$(git rev-parse --show-toplevel)"
gradlew="${GRADLEW:-$root/gradlew}"

java=false
misc=false
while IFS= read -r -d '' path; do
  case "$path" in
    src/*.java) java=true ;;
    */*) ;;
    *.gradle | *.md | .gitignore) misc=true ;;
  esac
done < <(git -C "$root" diff --cached --name-only --diff-filter=ACMR -z)

tasks=()
[ "$java" = true ] && tasks+=(spotlessJavaCheck)
[ "$misc" = true ] && tasks+=(spotlessMiscCheck)
[ "${#tasks[@]}" -gt 0 ] || exit 0

# Gradle only inside the build slot, as the Makefile asks of every target that starts it: inside a slot or
# on a CI runner the check runs Gradle itself, with GRADLE_SLOT_RUNNER it queues through that slot script
# (<runner> <tree> <log file> <gradle arguments...>), and otherwise it refuses before Gradle starts.
run_gradle()
{
  if [ "${GITHUB_ACTIONS:-}" = true ] || [ -n "${GRADLE_SLOT:-}" ]; then
    "$gradlew" -p "$root" "$@" 2>&1
    return
  fi
  local log status
  log="$(mktemp "${TMPDIR:-/tmp}/check-formatting.XXXXXX")"
  "$GRADLE_SLOT_RUNNER" "$root" "$log" "$@" > /dev/null 2>&1
  status=$?
  cat "$log"
  rm -f "$log"
  return "$status"
}

if [ "${GITHUB_ACTIONS:-}" != true ] && [ -z "${GRADLE_SLOT:-}" ] && [ -z "${GRADLE_SLOT_RUNNER:-}" ]; then
  {
    echo "==> commit refused: the formatting check starts Gradle, and Gradle runs only inside the build slot"
    echo "    (GRADLE_SLOT and GRADLE_SLOT_RUNNER are empty)"
    echo "    through the slot:           GRADLE_SLOT_RUNNER=<the slot script> git commit ..."
    echo "    deliberately without slot:  GRADLE_SLOT=none git commit ..."
  } >&2
  exit 1
fi

if output="$(run_gradle "${tasks[@]}" -q --console=plain)"; then
  exit 0
fi
{
  printf '%s\n' "$output"
  echo
  echo "==> commit refused: Spotless would format the files named above (#592)"
  echo "    format them:      ./gradlew spotlessApply     (or make spotless-java / make spotless-misc)"
  echo "    then stage them again with git add, and commit again"
} >&2
exit 1
