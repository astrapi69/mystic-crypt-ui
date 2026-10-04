#!/usr/bin/env bash
#
# Starts the development build with the internal plugins built from this checkout, in a profile of its
# own (#498).
#
# The application finds its configuration directory - and the plugins directory under it - through the
# user.home system property. The installed release keeps its plugins in ~/.config/mystic-crypt-ui/plugins,
# built for that release; plugins built from develop do not belong there, and the release must not pick
# them up (three of them read key files through a crypt-data the release does not ship). Pointed at a
# profile of its own, the development build reads and writes nothing under the release's directory.
#
# Usage: scripts/run-isolated.sh <isolated-home> [java options...]
set -euo pipefail

ISOLATED_HOME="${1:?usage: scripts/run-isolated.sh <isolated-home> [java options...]}"
shift

say() { printf '%s\n' "$*"; }
die() { printf 'run-isolated: %s\n' "$*" >&2; exit 1; }

# resolved without creating anything: the checks below come before the first write, or a refused
# profile inside the release's directory would already have been created there
isolated="$(realpath -m "$ISOLATED_HOME")"
real_home="$(realpath -m "$HOME")"

# fail closed: a profile that is the real home, or contains it, isolates nothing; one inside the
# release's configuration directory would share it
[ "$isolated" != "/" ] || die "'/' is not a profile"
case "$real_home/" in
  "$isolated"/*) die "'$isolated' is or contains the real home '$real_home' - that isolates nothing" ;;
esac
case "$isolated/" in
  "$real_home/.config/mystic-crypt-ui/"*) die "'$isolated' lies inside the installed release's configuration directory" ;;
esac

mkdir -p "$isolated"
jar="$(find build/libs -maxdepth 1 -name '*-all.jar' -print -quit)"
[ -n "$jar" ] || die "no *-all.jar in build/libs - 'make run-isolated' builds it first"

plugins="$isolated/.config/mystic-crypt-ui/plugins"
mkdir -p "$plugins"
# the profile's own plugins are replaced on every start: pf4j keeps the directory it unpacked a zip
# into, and an older unpacked plugin next to a newer zip is how a stale plugin ran before
find "$plugins" -mindepth 1 -maxdepth 1 \( -name '*-plugin-*.zip' -o \( -type d -name '*-plugin-*' \) \) \
  -exec rm -rf {} +
count=0
for zip in plugins/*/build/plugin-dist/*.zip; do
  [ -f "$zip" ] || continue
  cp "$zip" "$plugins/"
  count=$((count + 1))
done
[ "$count" -gt 0 ] || die "no plugin zips under plugins/*/build/plugin-dist - 'make run-isolated' builds them first"

say "==> $count plugins into $plugins"
say "==> starting $jar with user.home=$isolated"
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -Duser.home="$isolated" --sun-misc-unsafe-memory-access=allow \
  "$@" -jar "$jar"
