#!/usr/bin/env bash
#
# Verifies the installer of a release after its checksums were written, against the file they describe
# (release-workflow.md step 5, #793). Run it on the installer built from the commit that will be tagged, and again
# on the file downloaded from the release, if it was uploaded.
#
# Usage: scripts/verify-installer.sh <version> [--previous <version>] [--installer <file>]
#        make verify-installer [PREVIOUS=<version>]
#
#   --installer   the installer to check; default build/distributions/mystic-crypt-ui-<version>-installer.jar, with
#                 its .sha256 and .sha512 next to it
#   --previous    a published release whose signer certificate the installer's must equal; its application jar is
#                 fetched with scripts/fetch-release-jar.sh
#
# Every check prints "ok <check>: <what it measured>" or "FAILED <check>: <why>". A check that cannot run, because
# what it reads is missing, is FAILED too, never skipped. The script exits non-zero when any check failed. The
# application jar and what the checks write go to build/verify-installer/<version>/. The JDK is $JAVA_HOME, or the
# one .sdkmanrc names, which is what the release is built with.

set -uo pipefail

version="${1:?usage: scripts/verify-installer.sh <version> [--previous <version>] [--installer <file>]}"
shift
previous=""
installer="build/distributions/mystic-crypt-ui-$version-installer.jar"
while [ $# -gt 0 ]; do
  case "$1" in
    --previous) previous="${2:?--previous needs a version}"; shift 2 ;;
    --installer) installer="${2:?--installer needs a file}"; shift 2 ;;
    *) printf 'verify-installer: unknown option %s\n' "$1" >&2; exit 2 ;;
  esac
done

case "$installer" in /*) ;; *) installer="$PWD/$installer" ;; esac
root="$(git rev-parse --show-toplevel)" || { printf 'verify-installer: not inside the repository\n' >&2; exit 2; }
cd "$root" || exit 2
scripts="$root/scripts"
out="$root/build/verify-installer/$version"
app="$out/mystic-crypt-ui-$version-app.jar"
if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME="$HOME/.sdkman/candidates/java/$(sed -n 's/^java=//p' .sdkmanrc)"
fi
bin="$JAVA_HOME/bin"

failed=0
ok() { printf 'ok %s: %s\n' "$1" "$2"; }
fail() { printf 'FAILED %s: %s\n' "$1" "$2"; failed=$((failed + 1)); }

if [ ! -f "$installer" ]; then
  printf 'verify-installer: no installer at %s\n' "$installer" >&2
  exit 1
fi
rm -rf "$out" && mkdir -p "$out" || { printf 'verify-installer: cannot write %s\n' "$out" >&2; exit 1; }

# ~50 MB: an installer below it is missing its application jar or its plugins (release-workflow.md step 5)
floor=50000000
size=$(stat -c %s "$installer")
if [ "$size" -ge "$floor" ]; then ok size "$size bytes, at least $floor"; else fail size "$size bytes, below $floor"; fi

if unzip -tq "$installer" > "$out/archive.txt" 2>&1; then ok archive "$(tail -1 "$out/archive.txt")"
else fail archive "$(tail -1 "$out/archive.txt")"; fi

dir="$(dirname "$installer")"; name="$(basename "$installer")"
for sum in sha256 sha512; do
  if [ ! -f "$dir/$name.$sum" ]; then fail "$sum" "no $name.$sum next to the installer"
  elif (cd "$dir" && "${sum}sum" -c "$name.$sum") > "$out/$sum.txt" 2>&1; then ok "$sum" "$(cat "$out/$sum.txt")"
  else fail "$sum" "$(head -1 "$out/$sum.txt")"; fi
done

if python3 -I "$scripts/installer-pack.py" application-jar "$installer" "$app" "$version" > "$out/application-jar.txt" 2>&1
then ok "application jar" "$(stat -c %s "$app") bytes, Implementation-Version $version"
else fail "application jar" "$(tail -1 "$out/application-jar.txt")"; rm -f "$app"; fi

needs_app() { [ -f "$app" ] || { fail "$1" "not run, there is no application jar"; return 1; }; }

if needs_app signature; then
  "$bin/jarsigner" -J-Duser.language=en -verify "$app" > "$out/jarsigner.txt" 2>&1
  if grep -qx 'jar verified.' "$out/jarsigner.txt"; then
    ok signature "$(grep -m1 -oE 'X\.509, [^]]+' <("$bin/jarsigner" -J-Duser.language=en -verify -verbose -certs "$app" 2>/dev/null) || echo 'jar verified')"
  else fail signature "$(grep -m1 -E '^jar ' "$out/jarsigner.txt" || tail -1 "$out/jarsigner.txt")"; fi
fi

certificate() { unzip -p "$1" 'META-INF/*.RSA' 'META-INF/*.EC' 'META-INF/*.DSA' 2>/dev/null | "$bin/keytool" -printcert 2>/dev/null | sed -n 's/^.*SHA256: //p' | head -1; }
if [ -n "$previous" ] && needs_app "signer"; then
  if "$scripts/fetch-release-jar.sh" "$previous" > "$out/previous.txt" 2>&1; then
    mine="$(certificate "$app")"
    theirs="$(certificate "${MYSTIC_CRYPT_UI_RELEASE_JARS:-$HOME/.cache/mystic-crypt-ui/release-jars}/$previous/mystic-crypt-ui-$previous-app.jar")"
    if [ -n "$mine" ] && [ "$mine" = "$theirs" ]; then ok signer "the certificate of $previous, SHA256 $mine"
    else fail signer "certificate SHA256 '${mine:-none}', $previous has '${theirs:-none}'"; fi
  else fail signer "the application jar of $previous cannot be fetched: $(tail -1 "$out/previous.txt")"; fi
fi

if needs_app "class major"; then
  majors="$(python3 -I -c '
import collections, sys, zipfile
z = zipfile.ZipFile(sys.argv[1]); c = collections.Counter()
for n in z.namelist():
    if n.startswith("io/github/astrapi69/mystic/crypt/") and n.endswith(".class"): c[z.read(n)[7]] += 1
print(" ".join(f"{major}:{count}" for major, count in sorted(c.items())))' "$app")"
  expected="$("$bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.class.version = \([0-9]*\).*/\1/p')"
  case "$majors" in
    "$expected":*) [ "${majors#* }" = "$majors" ] && ok "class major" "$majors, the JDK's $expected" || fail "class major" "$majors, more than one" ;;
    *) fail "class major" "${majors:-no class}, the JDK's is $expected" ;;
  esac
fi

if needs_app mystic-crypt; then
  lib="$(sed -n 's/^mystic-crypt-version *= *"\(.*\)"/\1/p' gradle/libs.versions.toml)"
  url="https://repo1.maven.org/maven2/io/github/astrapi69/mystic-crypt/$lib/mystic-crypt-$lib.jar"
  if [ -n "$lib" ] && curl -fsSL --retry 3 -o "$out/mystic-crypt-$lib.jar" "$url" && curl -fsSL --retry 3 -o "$out/mystic-crypt-$lib.jar.sha1" "$url.sha1" \
    && [ "$(cut -c1-40 "$out/mystic-crypt-$lib.jar.sha1")" = "$(sha1sum "$out/mystic-crypt-$lib.jar" | cut -c1-40)" ]; then
    # module-info.class is left out of the all-dependencies jar on purpose (#771)
    verdict="$(python3 -I -c '
import sys, zipfile
lib, app = zipfile.ZipFile(sys.argv[1]), zipfile.ZipFile(sys.argv[2]); names = set(app.namelist())
classes = [n for n in lib.namelist() if n.endswith(".class") and n != "module-info.class"]
missing = [n for n in classes if n not in names]; differ = [n for n in classes if n in names and lib.read(n) != app.read(n)]
print(f"{len(classes) - len(missing) - len(differ)} of {len(classes)} classes byte-equal, missing {missing[:3]}, different {differ[:3]}")
sys.exit(1 if missing or differ else 0)' "$out/mystic-crypt-$lib.jar" "$app")"
    if [ $? -eq 0 ]; then ok mystic-crypt "$lib from Maven Central, $verdict"; else fail mystic-crypt "$lib from Maven Central, $verdict"; fi
  else fail mystic-crypt "mystic-crypt ${lib:-of unknown version} cannot be fetched from Maven Central with a matching sha1"; fi
fi

# the menu designer is development tooling, built but not shipped (architecture.md)
makefile_plugins="$(sed -n 's/^plugins: //p' Makefile | tr ' ' '\n' | sed -n 's/^plugin-\(.*\)$/\1-plugin/p' | grep -vx 'menu-designer-plugin' | sort)"
install_plugins="$(grep -oE 'plugins/[a-z-]+-plugin/build/plugin-dist/[a-z-]+-plugin-[0-9.]+\.zip' src/main/izpack/install.xml | sed -E 's#plugins/([a-z-]+)/.*#\1#' | sort)"
if packed="$(python3 -I "$scripts/installer-pack.py" plugins "$installer" 2> "$out/plugins.txt" | sort)"; then
  if [ "$install_plugins" = "$makefile_plugins" ] && [ "$packed" = "$install_plugins" ]; then
    ok plugins "$(printf '%s\n' "$packed" | wc -l) in the pack, the ones install.xml and the Makefile name"
  else fail plugins "pack [$(echo $packed)], install.xml [$(echo $install_plugins)], Makefile [$(echo $makefile_plugins)]"; fi
else fail plugins "$(tail -1 "$out/plugins.txt")"; fi

if needs_app "jar tests"; then
  JAVA_HOME="$JAVA_HOME" ./gradlew test --rerun --tests '*.RuntimeDependenciesTest' --tests '*.SharedLibraryFilesTest' \
    -PapplicationJarUnderTest="$app" > "$out/jar-tests.txt" 2>&1
  gradle=$?
  verdict="$(python3 -I -c '
import glob, sys, xml.etree.ElementTree as ET
parts = []; bad = False
for name in ("RuntimeDependenciesTest", "SharedLibraryFilesTest"):
    files = glob.glob(f"build/test-results/test/TEST-*.{name}.xml")
    if not files: parts.append(f"{name}: no result"); bad = True; continue
    r = ET.parse(files[0]).getroot(); t, f, e, s = (int(r.get(k)) for k in ("tests", "failures", "errors", "skipped"))
    parts.append(f"{name} {t} tests, {f} failures, {e} errors, {s} skipped"); bad |= t == 0 or f + e + s > 0
print("; ".join(parts)); sys.exit(1 if bad else 0)')"
  if [ $? -eq 0 ] && [ $gradle -eq 0 ]; then ok "jar tests" "$verdict"; else fail "jar tests" "gradle exit $gradle; $verdict"; fi
fi

if needs_app start; then
  if timeout 120 "$bin/java" -Djava.awt.headless=true -jar "$app" --cli --version > "$out/start.txt" 2>&1
  then ok start "--cli --version exit 0"; else fail start "--cli --version exit $?, $(tail -1 "$out/start.txt")"; fi
fi

if [ "$failed" -gt 0 ]; then
  printf 'verify-installer: %s failed for %s\n' "$failed" "$installer" >&2
  exit 1
fi
printf 'verify-installer: every check passed for %s\n' "$installer"
