#!/usr/bin/env bash
#
# Measures what a JVM flag does to the gate and to the application (#593), in GitHub Actions (AG-021). A measuring
# branch, not develop: the workflows call it only when started by hand with the input jvm-flags or measure.
#
#   measure-jvm.sh flags "<flags>"             adds the flags to the Gradle daemon (gradle.properties, which the
#                                               plugin builds take through PLUGIN_DAEMON) and to every test JVM of
#                                               the host and the plugin builds (gradle/test-jvms.gradle), in this
#                                               checkout only
#   measure-jvm.sh sample-start <file>         samples, every second, the summed resident memory of all java
#                                               processes of the runner, the highest value into <file>
#   measure-jvm.sh sample-stop <file> <label>  stops the sampling and prints the peak as a notice
#   measure-jvm.sh app-start <jar> <runs> "<flags>"
#                                               starts the application <runs> times on an Xvfb of its own and prints,
#                                               per start, the seconds until its first window and its peak resident
#                                               memory (VmHWM) 15 seconds after that window
#
# A notice is what this session reads of a run: the logs themselves are not reachable from it.
set -euo pipefail

notice() {
  echo "::notice title=$1::$2"
  echo "$1: $2"
}

apply_flags() {
  local flags="$1"
  [ -n "$flags" ] || { echo "no flags given" >&2; exit 2; }
  sed -i "s|^org.gradle.jvmargs=.*|& $flags|" gradle.properties
  local jvm_args
  jvm_args=$(printf "'%s', " $flags)
  sed -i "s|^    maxHeapSize = '512m'|&\n    jvmArgs ${jvm_args%, }|" gradle/test-jvms.gradle
  grep -q "jvmArgs ${jvm_args%, }" gradle/test-jvms.gradle || { echo "test-jvms.gradle not changed" >&2; exit 2; }
  notice "daemon arguments" "$(sed -n 's/^org.gradle.jvmargs=//p' gradle.properties)"
  notice "test JVM arguments" "${jvm_args%, }"
}

java_rss_kb() {
  local total=0 pid rss
  for pid in $(pgrep -x java || true); do
    rss=$(awk '/^VmRSS:/ {print $2}' "/proc/$pid/status" 2>/dev/null || echo 0)
    total=$((total + ${rss:-0}))
  done
  echo "$total"
}

sample_start() {
  local file="$1"
  echo 0 > "$file"
  (
    while [ -f "$file" ]; do
      now=$(java_rss_kb)
      [ "$now" -gt "$(cat "$file" 2>/dev/null || echo 0)" ] && echo "$now" > "$file.tmp" && mv "$file.tmp" "$file"
      sleep 1
    done
  ) &
  echo $! > "$file.pid"
}

sample_stop() {
  local file="$1" label="$2" peak
  peak=$(cat "$file")
  kill "$(cat "$file.pid")" 2>/dev/null || true
  rm -f "$file" "$file.pid"
  notice "$label" "java peak $((peak / 1024)) MB (sum of VmRSS of all java processes, sampled every second)"
}

app_start() {
  local jar="$1" runs="$2" flags="$3" display=:93 run
  [ -f "$jar" ] || { echo "no jar at $jar" >&2; exit 2; }
  Xvfb "$display" -screen 0 1600x1200x24 > /dev/null 2>&1 &
  local xvfb=$!
  sleep 2
  DISPLAY="$display" fluxbox > /dev/null 2>&1 &
  local wm=$!
  sleep 2
  local home
  for run in $(seq 1 "$runs"); do
    home=$(mktemp -d)
    local before started window pid hwm
    before=$(DISPLAY="$display" wmctrl -l | wc -l)
    started=$(date +%s%N)
    # shellcheck disable=SC2086
    DISPLAY="$display" java $flags --add-opens java.base/java.time=ALL-UNNAMED \
      --sun-misc-unsafe-memory-access=allow -Duser.home="$home" -jar "$jar" > "$home/out.log" 2>&1 &
    pid=$!
    window=""
    for _ in $(seq 1 600); do
      if [ "$(DISPLAY="$display" wmctrl -l | wc -l)" -gt "$before" ]; then
        window=$(date +%s%N)
        break
      fi
      sleep 0.1
    done
    if [ -z "$window" ]; then
      kill "$pid" 2>/dev/null || true
      tail -20 "$home/out.log" >&2
      echo "no window within 60 seconds" >&2
      exit 1
    fi
    sleep 15
    hwm=$(awk '/^VmHWM:/ {print $2}' "/proc/$pid/status")
    kill "$pid"
    wait "$pid" 2>/dev/null || true
    notice "application start $run" \
      "$(awk -v n="$((window - started))" 'BEGIN {printf "%.2f", n / 1e9}') s to the first window, VmHWM $((hwm / 1024)) MB"
    rm -rf "$home"
  done
  kill "$wm" "$xvfb" 2>/dev/null || true
}

case "${1:-}" in
  flags) apply_flags "${2:-}" ;;
  sample-start) sample_start "$2" ;;
  sample-stop) sample_stop "$2" "$3" ;;
  app-start) app_start "$2" "$3" "${4:-}" ;;
  *) echo "usage: $0 flags|sample-start|sample-stop|app-start ..." >&2; exit 2 ;;
esac
