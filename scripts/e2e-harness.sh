#!/usr/bin/env bash
#
# Runs the end-to-end UI suite on a display of its own, behind a window manager that is verified to
# be answering, and refuses to start when it is not.
#
# A display of its own (#504). The harness used to run the suite on whatever DISPLAY it inherited.
# A shell started from a desktop session carries that session's DISPLAY - on a Wayland desktop it is
# XWayland on :0 - and the robot then clicked and typed on the person's real screen. So by default
# the harness starts an Xvfb on a display number of its own, exports it, and stops it again on exit.
# It refuses to go on when DISPLAY is not that number afterwards.
#
# Not the lowest free number (#528). It used to let Xvfb pick the lowest free one (-displayfd), and
# builds that follow each other - fourteen plugin builds in 'make plugins', the test tasks of one
# build - then took the number the build before had just given up, while that server was still on
# its way out: fluxbox could not connect to the new one ("Couldn't connect to XServer:0"), three
# times on CI. Now the harness picks a random number from 100 to 999 that has neither a lock file
# nor a socket, accepts the server only when the lock file names its own Xvfb, the socket exists and
# xdpyinfo answers, and starts again on another number when the server or the window manager does
# not come up, three attempts at most. Stopping waits until the number's lock file and socket are
# gone, so the number is free when the harness ends.
#
# The display a caller already has is used only on request: E2E_USE_CURRENT_DISPLAY=1, which
# 'make test-e2e-demo' sets because a demo run is meant to be watched, and which no other target
# sets.
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
# Every Gradle build that runs tests comes here too (#505): gradle/own-display.gradle runs this
# script with scripts/hold-display.sh as the command, takes the display it reports, and gives it to
# every test task. So './gradlew build', 'check', 'jacocoTestReport' and each plugin build run their
# tests on a display of their own as well, not only the targets that call this script by name.
#
# Usage: scripts/e2e-harness.sh [gradle arguments...]   (default: e2eTest e2eLockTest e2eKdbxTest)
# Environment:
#   E2E_USE_CURRENT_DISPLAY=1  run on the inherited DISPLAY instead of an own Xvfb. The harness sets
#                              it for the command it runs, whose display is then settled
#   E2E_RUN_COMMAND            the command to run, ./gradlew by default. The build runs
#                              scripts/hold-display.sh through it, the harness's own test a stub
#   E2E_SCREEN                 the own Xvfb's screen, 1920x1200x24 by default
#   E2E_WM_WAIT_SECONDS        how long to wait for the window manager, 15 by default
#   E2E_DISPLAY_ATTEMPTS       how often an own display is started before giving up, 3 by default
set -euo pipefail

WM_WAIT_SECONDS="${E2E_WM_WAIT_SECONDS:-15}"
XVFB_WAIT_SECONDS="${E2E_XVFB_WAIT_SECONDS:-15}"
DISPLAY_ATTEMPTS="${E2E_DISPLAY_ATTEMPTS:-3}"
SCREEN="${E2E_SCREEN:-1920x1200x24}"
RUN_COMMAND="${E2E_RUN_COMMAND:-./gradlew}"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/mystic-crypt-ui-e2e.XXXXXX")"
WM_LOG="$WORK_DIR/fluxbox.log"
XVFB_LOG="$WORK_DIR/xvfb.log"

xvfb_pid=""
fluxbox_pid=""
display_number=""
reason=""

say() { printf '==> %s\n' "$*"; }
die() { printf 'e2e-harness: %s\n' "$*" >&2; exit 1; }

lock_pid() { tr -d ' \n' < "/tmp/.X$1-lock" 2>/dev/null || true; }

# stops what this harness started on its display, and waits until the display's lock file and socket
# are gone, so that the number is free again when this returns (#528)
stop_display() {
  local pid waited=0
  for pid in "$fluxbox_pid" "$xvfb_pid"; do
    [ -n "$pid" ] || continue
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  done
  if [ -n "$display_number" ] && [ -n "$xvfb_pid" ]; then
    while [ "$waited" -lt 50 ] && { [ "$(lock_pid "$display_number")" = "$xvfb_pid" ] \
      || { [ -e "/tmp/.X11-unix/X$display_number" ] && [ ! -e "/tmp/.X$display_number-lock" ]; }; }; do
      sleep 0.1
      waited=$((waited + 1))
    done
  fi
  fluxbox_pid=""
  xvfb_pid=""
  display_number=""
}

stop_started() {
  stop_display
  rm -rf "$WORK_DIR"
}
trap stop_started EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

require() {
  command -v "$1" >/dev/null 2>&1 || die "$1 is not installed. $2"
}

# a random display number from 100 to 999 without a lock file or a socket: never the lowest free
# number, which is the one a build that has just ended gave up (#528)
free_display_number() {
  local tries=0 number
  while [ "$tries" -lt 200 ]; do
    number=$((100 + RANDOM % 900))
    if [ ! -e "/tmp/.X$number-lock" ] && [ ! -e "/tmp/.X11-unix/X$number" ]; then
      printf '%s' "$number"
      return 0
    fi
    tries=$((tries + 1))
  done
  return 1
}

# starts an Xvfb on the given number and returns once it is this harness's own server: the lock file
# names its pid, the socket exists, and a client gets through. Fails with the reason otherwise
start_xvfb() {
  local number="$1" waited=0
  Xvfb ":$number" -nolisten tcp -screen 0 "$SCREEN" >"$XVFB_LOG" 2>&1 &
  xvfb_pid=$!
  display_number="$number"
  until [ "$(lock_pid "$number")" = "$xvfb_pid" ] && [ -S "/tmp/.X11-unix/X$number" ] \
    && xdpyinfo -display ":$number" >/dev/null 2>&1; do
    if ! kill -0 "$xvfb_pid" 2>/dev/null; then
      reason="Xvfb exited while starting up on :$number. Its output: $(cat "$XVFB_LOG" 2>/dev/null)"
      return 1
    fi
    if [ "$waited" -ge "$((XVFB_WAIT_SECONDS * 10))" ]; then
      reason="Xvfb on :$number did not come up as this harness's own server within ${XVFB_WAIT_SECONDS}s"
      return 1
    fi
    sleep 0.1
    waited=$((waited + 1))
  done
}

# a window manager that answers on DISPLAY, started when none does (#322); fails with the reason
start_window_manager() {
  local wm waited=0
  if wm="$(wmctrl -m 2>/dev/null | sed -n 's/^Name: //p')" && [ -n "$wm" ]; then
    say "window manager already running on $DISPLAY: $wm"
    return 0
  fi
  require fluxbox "No window manager answers on $DISPLAY. Install it (sudo apt-get install -y
  fluxbox) or start your own before running this."
  # the log is a file, not /dev/null: when fluxbox refuses to start, its reason is the only thing
  # that explains the abort below
  fluxbox >"$WM_LOG" 2>&1 &
  fluxbox_pid=$!
  say "started fluxbox (pid $fluxbox_pid) on $DISPLAY (log: $WM_LOG)"
  until wm="$(wmctrl -m 2>/dev/null | sed -n 's/^Name: //p')" && [ -n "$wm" ]; do
    if ! kill -0 "$fluxbox_pid" 2>/dev/null; then
      reason="fluxbox exited while starting up. Its output: $(cat "$WM_LOG" 2>/dev/null)"
      return 1
    fi
    if [ "$waited" -ge "$WM_WAIT_SECONDS" ]; then
      reason="fluxbox did not answer wmctrl within ${WM_WAIT_SECONDS}s. Its output is in $WM_LOG.
  Not starting the tests: without a window manager the sign-in dialog never takes focus and the
  suite hangs instead of failing (#322)."
      return 1
    fi
    sleep 1
    waited=$((waited + 1))
  done
  say "window manager answering after ${waited}s: $wm"
}

start_own_display() {
  require Xvfb "Install it (sudo apt-get install -y xvfb), or run on a display you already have
  with E2E_USE_CURRENT_DISPLAY=1."
  local attempt=1 number
  while :; do
    number="$(free_display_number)" || die "no free display number between 100 and 999"
    if start_xvfb "$number"; then
      say "started Xvfb (pid $xvfb_pid) on :$number"
      export DISPLAY=":$number"
      [ "$DISPLAY" = ":$number" ] \
        || die "DISPLAY is '$DISPLAY' after starting Xvfb on :$number - not running on a display that is not this harness's own"
      if start_window_manager; then
        return 0
      fi
    fi
    printf 'e2e-harness: attempt %s of %s on :%s failed: %s\n' "$attempt" "$DISPLAY_ATTEMPTS" \
      "$number" "$reason" >&2
    stop_display
    [ "$attempt" -lt "$DISPLAY_ATTEMPTS" ] \
      || die "no display of its own came up in $DISPLAY_ATTEMPTS attempts; the last reason is above"
    attempt=$((attempt + 1))
  done
}

require wmctrl "Whether a window manager is running cannot be measured without it, and this script
  will not fall back to sleeping and hoping (#322). Install it: sudo apt-get install -y wmctrl"
require xdpyinfo "Install it: sudo apt-get install -y x11-utils"

if [ "${E2E_USE_CURRENT_DISPLAY:-}" = "1" ]; then
  [ -n "${DISPLAY:-}" ] || die "E2E_USE_CURRENT_DISPLAY=1, but DISPLAY is not set"
  say "using the current display $DISPLAY (E2E_USE_CURRENT_DISPLAY=1)"
  xdpyinfo -display "$DISPLAY" >/dev/null 2>&1 \
    || die "no X display answers on $DISPLAY - nothing is listening there"
  start_window_manager || die "$reason"
else
  start_own_display
fi

if [ "$#" -eq 0 ]; then set -- e2eTest e2eLockTest e2eKdbxTest; fi
# The command runs on a display this harness chose and checked - its own, or the one it was told to
# use. It is told so: a Gradle build under the harness asks the harness once more for a display for
# its test tasks (gradle/own-display.gradle, #505), and without this it would start a second Xvfb
# next to this one instead of using it
export E2E_USE_CURRENT_DISPLAY=1
say "running on $DISPLAY: $RUN_COMMAND $*"
set +e
"$RUN_COMMAND" "$@"
status=$?
set -e
exit "$status"
