#!/usr/bin/env bash
#
# Prints the "[on-screen]" reports of failed end-to-end tests into the CI log (#575).
#
# WhatWasOnScreen writes its report to the standard output of the test JVM. Gradle keeps that in the
# result XML and does not print it, so a red run's log carried the stack trace and nothing of what
# was on screen: #575 read that as "the report did not print for this class", while it sat in an
# artifact. A forwarder inside the build printed nothing under the configuration cache (05cd11c3),
# so the reports are read back out of the XML after the build instead.
#
#   scripts/print-on-screen-reports.sh build/test-results/e2eTest [more result directories ...]
#
# A directory that does not exist is skipped - a suite that never ran wrote nothing. No result file
# in any of them is a failure: then nothing was read, which is not the same as nothing to report.
set -euo pipefail

tag='[on-screen] '
read_files=0
with_report=0

for directory in "$@"; do
  [ -d "$directory" ] || continue
  for file in "$directory"/TEST-*.xml; do
    [ -f "$file" ] || continue
    read_files=$((read_files + 1))
    if grep -qF "$tag" "$file"; then
      with_report=$((with_report + 1))
      printf '==> %s\n' "$file"
      # every report line starts with the tag, but the first line of the JVM's output still
      # carries the XML that opens it: <system-out><![CDATA[
      sed -n -e 's/^.*<!\[CDATA\[//' -e '/^\[on-screen\] /p' "$file"
    fi
  done
done

if [ "$read_files" -eq 0 ]; then
  printf 'print-on-screen-reports: no result file in %s - nothing was read\n' "$*" >&2
  exit 1
fi
printf '==> %s of %s result files carried a report\n' "$with_report" "$read_files"
