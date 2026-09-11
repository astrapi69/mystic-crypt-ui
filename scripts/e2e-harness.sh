#!/usr/bin/env bash
#
# Runs the end-to-end UI suite behind a window manager that is verified to be answering, and
# refuses to start when it is not (#322).
#
# The line this replaces was 'fluxbox >/dev/null 2>&1 & sleep 2; ./gradlew ...'. It sent the window
# manager's own error message to /dev/null, waited two seconds whether or not anything came up, and
# started the tests either way. What a missing window manager then looks like is not a failure: a
# focus wait that never returns. One run sat in ComponentDriver.focus for 40 minutes with no output.
#
# So the check is not a courtesy here - it is the difference between a named abort in a second and a
# gate that hangs. wmctrl answering is the evidence: it asks the display for a window manager and
# fails when none owns the selection, which 'pgrep fluxbox' cannot tell apart from a process that
# started and died.
#
# Usage: scripts/e2e-harness.sh [gradle arguments...]   (default: e2eTest)
set -euo pipefail

WM_WAIT_SECONDS="${E2E_WM_WAIT_SECONDS:-15}"
WM_LOG="${TMPDIR:-/tmp}/mystic-crypt-ui-e2e-fluxbox.log"

say() { printf '==> %s\n' "$*"; }
die() { printf 'e2e-harness: %s\n' "$*" >&2; exit 1; }

[ -n "${DISPLAY:-}" ] || die "DISPLAY is not set - the UI suite needs a display. Start one (Xvfb
  :99 & export DISPLAY=:99) or run the suite on GitHub Actions, which provides its own."

command -v wmctrl >/dev/null 2>&1 \
  || die "wmctrl is not installed, so whether a window manager is running cannot be measured. This
  script will not fall back to sleeping and hoping - that is the failure mode it exists to remove.
  Install it: sudo apt-get install -y wmctrl"

xdpyinfo -display "$DISPLAY" >/dev/null 2>&1 \
  || die "no X display answers on $DISPLAY - nothing is listening there"

if wm="$(wmctrl -m 2>/dev/null | sed -n 's/^Name: //p')" && [ -n "$wm" ]; then
  say "window manager already running on $DISPLAY: $wm"
else
  command -v fluxbox >/dev/null 2>&1 \
    || die "no window manager answers on $DISPLAY and fluxbox is not installed. Install it
  (sudo apt-get install -y fluxbox) or start your own before running this."
  say "no window manager on $DISPLAY, starting fluxbox (log: $WM_LOG)"
  # the log is a file, not /dev/null: when fluxbox refuses to start, its reason is the only thing
  # that explains the abort below
  fluxbox >"$WM_LOG" 2>&1 &
  fluxbox_pid=$!
  waited=0
  until wm="$(wmctrl -m 2>/dev/null | sed -n 's/^Name: //p')" && [ -n "$wm" ]; do
    if ! kill -0 "$fluxbox_pid" 2>/dev/null; then
      printf 'e2e-harness: fluxbox exited while starting up. Its output:\n' >&2
      cat "$WM_LOG" >&2 || true
      exit 1
    fi
    [ "$waited" -ge "$WM_WAIT_SECONDS" ] && die "fluxbox did not answer wmctrl within
  ${WM_WAIT_SECONDS}s. Its output is in $WM_LOG. Not starting the tests: without a window manager
  the sign-in dialog never takes focus and the suite hangs instead of failing (#322)."
    sleep 1
    waited=$((waited + 1))
  done
  say "window manager answering after ${waited}s: $wm"
fi

say "running: ./gradlew ${*:-e2eTest}"
exec ./gradlew "${@:-e2eTest}"
