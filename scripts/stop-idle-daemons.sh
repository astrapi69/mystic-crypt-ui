#!/usr/bin/env bash
#
# Stops the Gradle daemons before the release gate, but only once none of them is building (#580).
#
# The 8.9 gate died of memory next to daemons left over from earlier builds in other worktrees;
# stopping them first is what let the second attempt pass. A daemon that is building - another
# worktree's build, the IDE's - is not killed in the middle: while ./gradlew --status shows one that
# is not IDLE, the script waits and asks again, and it stops the daemons only when every one is idle.
# A daemon still working when the wait is over leaves everything running, and the script fails, so
# that the gate does not start next to it.
#
#   scripts/stop-idle-daemons.sh
#
# --status and --stop reach only the daemons of this build's Gradle version; others are not seen.
# Settings: GRADLEW (default ./gradlew), STOP_IDLE_DAEMONS_INTERVAL seconds between two looks
# (default 30), STOP_IDLE_DAEMONS_WAIT seconds to wait at most (default 1800).
set -euo pipefail

gradlew="${GRADLEW:-./gradlew}"
interval="${STOP_IDLE_DAEMONS_INTERVAL:-30}"
wait_at_most="${STOP_IDLE_DAEMONS_WAIT:-1800}"

say() { printf '==> %s\n' "$*"; }
die() { printf 'stop-idle-daemons: %s\n' "$*" >&2; exit 1; }

started=$SECONDS
while true; do
  status="$("$gradlew" --status)" || die "$gradlew --status failed"
  # every daemon line whose status is neither IDLE nor STOPPED counts as working, an unknown one too
  working="$(printf '%s\n' "$status" \
    | awk '$1 ~ /^[0-9]+$/ && $2 != "IDLE" && $2 != "STOPPED" { printf "%s%s %s", sep, $1, $2; sep = ", " }')"
  idle="$(printf '%s\n' "$status" | awk '$1 ~ /^[0-9]+$/ && $2 == "IDLE"' | wc -l)"
  if [ -z "$working" ]; then
    say "no daemon is working, ${idle} idle: stopping them"
    "$gradlew" --stop
    exit 0
  fi
  if [ $((SECONDS - started)) -ge "$wait_at_most" ]; then
    die "still working after ${wait_at_most}s: ${working} - nothing was stopped, the gate does not start"
  fi
  say "waiting for ${working} (${idle} idle), asking again in ${interval}s"
  sleep "$interval"
done
