#!/usr/bin/env bash
#
# Writes the files the file encryption window and the mystic-crypt command line hand each other,
# for the tests that each side reads what the other wrote (#877):
#
#   scripts/fixtures/write-file-crypt-formats.sh window|command-line <empty output directory> [mystic-crypt version]
#
# `window` writes through the window's FileCryptSupport of this checkout, `command-line` through
# `encrypt` and PassphraseCryptor of the published mystic-crypt release (default 13.10), resolved
# from Maven Central into a build of its own, so the version this checkout pins does not decide it.
# The passphrase is read from standard input. Each run writes different files: the salts are new.
set -euo pipefail

side="${1:?usage: scripts/fixtures/write-file-crypt-formats.sh window|command-line <empty output directory> [mystic-crypt version]}"
out="${2:?usage: scripts/fixtures/write-file-crypt-formats.sh window|command-line <empty output directory> [mystic-crypt version]}"
version="${3:-13.10}"
repository="$(git rev-parse --show-toplevel)"
window_source="$repository/plugins/file-crypt-plugin/src/main/java/io/github/astrapi69/mystic/crypt/plugin/filecrypt/FileCryptSupport.java"

die() { printf 'write-file-crypt-formats: %s\n' "$*" >&2; exit 1; }

case "$side" in window|command-line) ;; *) die "the side is window or command-line, not '$side'" ;; esac
mkdir -p "$out"
[ -z "$(ls -A "$out")" ] || die "$out is not empty"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

printf "rootProject.name = 'file-crypt-formats'\n" > "$work/settings.gradle"
cat > "$work/build.gradle" <<GRADLE
plugins { id 'java' }
repositories { mavenCentral() }
dependencies { implementation 'io.github.astrapi69:mystic-crypt:$version' }
tasks.register('printClasspath') { doLast { println configurations.runtimeClasspath.asPath } }
GRADLE
classpath="$("$repository/gradlew" -q --no-configuration-cache -p "$work" printClasspath | tail -n 1)"
case "$classpath" in *"mystic-crypt-$version.jar"*) ;; *) die "mystic-crypt $version is not on the resolved class path" ;; esac
printf '==> mystic-crypt %s, java %s\n' "$version" "$(java -version 2>&1 | grep -v JAVA_TOOL_OPTIONS | head -n 1)"

mkdir "$work/window"
javac -d "$work/window" -cp "$classpath" "$window_source"
printf '==> the window: %s at %s\n' "${window_source#"$repository/"}" "$(git -C "$repository" log -1 --format=%h -- "$window_source")"

java -cp "$work/window:$classpath" "$repository/scripts/fixtures/WriteFileCryptFormats.java" "$side" "$out"
(cd "$out" && sha256sum -- *)
