#!/usr/bin/env bash
#
# The command a Gradle build runs under scripts/e2e-harness.sh to borrow the harness's display (#505).
#
# It reports the display on one line, E2E_DISPLAY=:N, and then holds it until its input ends. The
# build closes that input when it finishes; this script then exits, and the harness's trap stops the
# Xvfb and the window manager it started. Nothing else runs here, so the display is used by the
# build's test JVMs only. When the build dies without closing anything, its end of the pipe goes with
# it, and the input ends all the same.
#
# Usage: E2E_RUN_COMMAND=scripts/hold-display.sh scripts/e2e-harness.sh hold
set -euo pipefail

[ -n "${DISPLAY:-}" ] || { printf 'hold-display: no DISPLAY to hand out\n' >&2; exit 1; }
printf 'E2E_DISPLAY=%s\n' "$DISPLAY"
cat >/dev/null
