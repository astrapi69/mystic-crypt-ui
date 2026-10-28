#!/usr/bin/env bash
#
# Checks that the result files the release gate leaves behind come from its forced phase (#630).
#
# The gate forces every test task (`test --rerun e2eTest --rerun ...`) and then runs `./gradlew build`.
# When that last build got another build cache key for a test task than the forced phase had, it was
# not up to date and restored an older run's results over the forced ones: at 8.12 every XML in the
# four suites came from the gate run before it, while the gate read green. The counts the build
# prints were right; the files a reader goes to when a number is in doubt were not.
#
#   scripts/check-forced-run-results.sh <start of the forced phase, epoch seconds> <result dir>...
#
# Every directory named has to exist and hold at least one TEST-*.xml - the forced phase runs every
# suite, so an empty one means nothing was checked. Every file's testsuite timestamp has to lie in
# or after the second the forced phase started. Anything else fails, and so does a file whose
# timestamp cannot be read.
set -euo pipefail

usage() {
  printf 'usage: scripts/check-forced-run-results.sh <start, epoch seconds> <result directory>...\n' >&2
  exit 2
}

[ "$#" -ge 2 ] || usage
started="$1"
shift
[[ "$started" =~ ^[0-9]+$ ]] || usage

failed=0
for directory in "$@"; do
  suite="$(basename "$directory")"
  files=0
  older=0
  if [ -d "$directory" ]; then
    for file in "$directory"/TEST-*.xml; do
      [ -f "$file" ] || continue
      files=$((files + 1))
      timestamp="$(grep -m1 -oE '<testsuite [^>]*timestamp="[^"]+"' "$file" | sed -E 's/.*timestamp="([^"]+)"/\1/' || true)"
      seconds="$( [ -n "$timestamp" ] && date -u -d "$timestamp" +%s 2>/dev/null || true)"
      if [ -z "$seconds" ]; then
        printf 'check-forced-run-results: no readable timestamp in %s\n' "$file"
        older=$((older + 1))
      elif [ "$seconds" -lt "$started" ]; then
        printf 'check-forced-run-results: OLDER THAN THE FORCED RUN: %s (%s)\n' "$file" "$timestamp"
        older=$((older + 1))
      fi
    done
  fi
  if [ "$files" -eq 0 ]; then
    printf 'check-forced-run-results: no result file in %s - the forced run did not write it\n' "$directory"
    failed=1
  elif [ "$older" -gt 0 ]; then
    printf '%s: %s of %s result files are not from the forced run\n' "$suite" "$older" "$files"
    failed=1
  else
    printf '%s: %s result files, all from the forced run\n' "$suite" "$files"
  fi
done

exit "$failed"
