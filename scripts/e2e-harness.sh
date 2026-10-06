#!/usr/bin/env bash
#
# Runs the end-to-end UI suite on a display of its own, behind a window manager that is verified to
# be answering, and refuses to start when it is not.
#
# A display of its own (#504). The harness used to run the suite on whatever DISPLAY it inherited.
# A shell started from a desktop session carries that session's DISPLAY - on a Wayland desktop it is
# XWayland on :0 - and the robot then clicked and typed on the person's real screen. So by default
# the harness starts an Xvfb, lets the server pick a free display number (-displayfd, no race over
# lock files), exports it, and stops it again on exit. It refuses to go on when DISPLAY is not that
# number afterwards. The display a caller already has is used only on request:
# E2E_USE_CURRENT_DISPLAY=1, which 'make test-e2e-demo' sets because a demo run is meant to be
# watched, and which no other target sets.
#
# A window manager that answers (#322). The line this once replaced was 'fluxbox >/dev/null 2>&1 &
# sleep 2; ./gradlew ...'. Without a window manager the suite does not fail, it waits: one run sat in
# ComponentDriver.focus for 40 minutes with no output. wmctrl answering is the evidence: it asks the
# display for a window manager and fails when none owns the selection, which 'pgrep fluxbox' cannot
# tell apart from a process that started and died.
#
# The command runs as a child, not through exec, so that the trap below stops what this script
# started even when the command fails; its exit code is passed through unchanged.
#
# Usage: scripts/e2e-harness.sh [gradle arguments...]   (default: e2eTest e2eLockTest e2eKdbxTest)
# Environment:
#   E2E_USE_CURRENT_DISPLAY=1  run on the inherited DISPLAY instead of an own Xvfb
#   E2E_RUN_COMMAND            the command to run, ./gradlew by default (the seam the harness's own
#                              test uses to run a stub instead of the suite)
#   E2E_SCREEN                 the own Xvfb's screen, 1920x1200x24 by default
#   E2E_WM_WAIT_SECONDS        how long to wait for the window manager, 15 by default
set -euo pipefail

WM_WAIT_SECONDS="${E2E_WM_WAIT_SECONDS:-15}"
XVFB_WAIT_SECONDS="${E2E_XVFB_WAIT_SECONDS:-15}"
SCREEN="${E2E_SCREEN:-1920x1200x24}"
RUN_COMMAND="${E2E_RUN_COMMAND:-./gradlew}"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/mystic-crypt-ui-e2e.XXXXXX")"
WM_LOG="$WORK_DIR/fluxbox.log"
XVFB_LOG="$WORK_DIR/xvfb.log"

xvfb_pid=""
fluxbox_pid=""

say() { printf '==> %s\n' "$*"; }
die() { printf 'e2e-harness: %s\n' "$*" >&2; exit 1; }

stop_started() {
  local pid
  for pid in "$fluxbox_pid" "$xvfb_pid"; do
    [ -n "$pid" ] || continue
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  done
  rm -rf "$WORK_DIR"
}
trap stop_started EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

require() {
  command -v "$1" >/dev/null 2>&1 || die "$1 is not installed. $2"
}

start_own_display() {
  require Xvfb "Install it (sudo apt-get install -y xvfb), or run on a display you already have
  with E2E_USE_CURRENT_DISPLAY=1."
  local number_file="$WORK_DIR/display-number" waited=0 number=""
  Xvfb -displayfd 3 -nolisten tcp -screen 0 "$SCREEN" 3>"$number_file" >"$XVFB_LOG" 2>&1 &
  xvfb_pid=$!
  until number="$(head -n 1 "$number_file" 2>/dev/null)" && [ -n "$number" ]; do
    if ! kill -0 "$xvfb_pid" 2>/dev/null; then
      printf 'e2e-harness: Xvfb exited while starting up. Its output:\n' >&2
      cat "$XVFB_LOG" >&2 || true
      exit 1
    fi
    [ "$waited" -ge "$((XVFB_WAIT_SECONDS * 10))" ] \
      && die "Xvfb reported no display number within ${XVFB_WAIT_SECONDS}s"
    sleep 0.1
    waited=$((waited + 1))
  done
  say "started Xvfb (pid $xvfb_pid) on :$number"
  export DISPLAY=":$number"
  [ "$DISPLAY" = ":$number" ] \
    || die "DISPLAY is '$DISPLAY' after starting Xvfb on :$number - not running on a display that is not this harness's own"
}

if [ "${E2E_USE_CURRENT_DISPLAY:-}" = "1" ]; then
  [ -n "${DISPLAY:-}" ] || die "E2E_USE_CURRENT_DISPLAY=1, but DISPLAY is not set"
  say "using the current display $DISPLAY (E2E_USE_CURRENT_DISPLAY=1)"
else
  start_own_display
fi

require wmctrl "Whether a window manager is running cannot be measured without it, and this script
  will not fall back to sleeping and hoping (#322). Install it: sudo apt-get install -y wmctrl"
require xdpyinfo "Install it: sudo apt-get install -y x11-utils"

xdpyinfo -display "$DISPLAY" >/dev/null 2>&1 \
  || die "no X display answers on $DISPLAY - nothing is listening there"

if wm="$(wmctrl -m 2>/dev/null | sed -n 's/^Name: //p')" && [ -n "$wm" ]; then
  say "window manager already running on $DISPLAY: $wm"
else
  require fluxbox "No window manager answers on $DISPLAY. Install it (sudo apt-get install -y
  fluxbox) or start your own before running this."
  # the log is a file, not /dev/null: when fluxbox refuses to start, its reason is the only thing
  # that explains the abort below
  fluxbox >"$WM_LOG" 2>&1 &
  fluxbox_pid=$!
  say "started fluxbox (pid $fluxbox_pid) on $DISPLAY (log: $WM_LOG)"
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

if [ "$#" -eq 0 ]; then set -- e2eTest e2eLockTest e2eKdbxTest; fi
say "running on $DISPLAY: $RUN_COMMAND $*"
set +e
"$RUN_COMMAND" "$@"
status=$?
set -e
exit "$status"
