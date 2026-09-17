#!/usr/bin/env bash
#
# Fetches the application jar of a published release, so a test can open a vault this build writes
# with the release a user may still be running (#402).
#
# "The release" means the file users downloaded, not a rebuild from the tag: the installer is taken
# from GitHub Releases, its sha256 is checked against the checksum file published next to it, and
# the application jar is extracted from the installer's pack. A jar built from the tag would prove
# what the tag compiles to today, with today's dependency cache - not what is installed.
#
# The installer packs its files as zlib streams one after another in
# resources/packs/pack-executables; the application jar is the stream that is a zip holding
# StartMysticCryptApplication, and its manifest has to name the version asked for.
#
# The result is cached per version under $MYSTIC_CRYPT_UI_RELEASE_JARS (default
# ~/.cache/mystic-crypt-ui/release-jars), which is also where CI's cache step points. A cached jar is
# used as it is: a published release does not change, and the checksum was checked when it was
# written.
#
# Usage: scripts/fetch-release-jar.sh <version>        e.g. 8.5.1
# Exits non-zero with the reason when anything cannot be done - no network, a checksum that does not
# match, an installer without the jar. Whether that fails a build is the caller's decision.

set -euo pipefail

version="${1:?usage: scripts/fetch-release-jar.sh <version>, e.g. 8.5.1}"
cache_root="${MYSTIC_CRYPT_UI_RELEASE_JARS:-$HOME/.cache/mystic-crypt-ui/release-jars}"
target_dir="$cache_root/$version"
target="$target_dir/mystic-crypt-ui-$version-app.jar"
installer_name="mystic-crypt-ui-$version-installer.jar"
base_url="https://github.com/astrapi69/mystic-crypt-ui/releases/download/RELEASE-$version"

say() { printf '==> %s\n' "$*"; }
die() { printf 'fetch-release-jar: %s\n' "$*" >&2; exit 1; }

if [ -f "$target" ]; then
  say "release jar $version already cached: $target"
  exit 0
fi

command -v curl >/dev/null 2>&1 || die "curl is not installed"
command -v python3 >/dev/null 2>&1 || die "python3 is not installed, it extracts the jar from the installer pack"
command -v sha256sum >/dev/null 2>&1 || die "sha256sum is not installed"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

say "downloading $installer_name and its published checksum from RELEASE-$version"
curl -fsSL --retry 3 -o "$work/$installer_name" "$base_url/$installer_name" \
  || die "could not download $base_url/$installer_name (no network, or no such release)"
curl -fsSL --retry 3 -o "$work/$installer_name.sha256" "$base_url/$installer_name.sha256" \
  || die "could not download the published checksum $base_url/$installer_name.sha256"

(cd "$work" && sha256sum -c "$installer_name.sha256") \
  || die "the installer does not match the checksum published with RELEASE-$version - not using it"

mkdir -p "$target_dir"
python3 - "$work/$installer_name" "$target.partial" "$version" <<'PY'
import io, sys, zipfile, zlib

installer, target, version = sys.argv[1], sys.argv[2], sys.argv[3]
with zipfile.ZipFile(installer) as outer:
    pack = outer.read("resources/packs/pack-executables")

position = 0
while position < len(pack):
    stream = zlib.decompressobj()
    try:
        chunk = stream.decompress(pack[position:])
    except zlib.error:
        position += 1
        continue
    if not stream.eof:
        break
    position = len(pack) - len(stream.unused_data)
    if chunk[:2] != b"PK":
        continue
    with zipfile.ZipFile(io.BytesIO(chunk)) as candidate:
        names = set(candidate.namelist())
        if "io/github/astrapi69/mystic/crypt/StartMysticCryptApplication.class" not in names:
            continue
        manifest = candidate.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    declared = [line.split(":", 1)[1].strip() for line in manifest.splitlines()
                if line.startswith("Implementation-Version:")]
    if declared != [version]:
        sys.exit(f"the application jar in the installer declares Implementation-Version {declared}, not {version}")
    with open(target, "wb") as out:
        out.write(chunk)
    print(f"==> extracted the application jar, {len(chunk)} bytes, Implementation-Version {version}")
    sys.exit(0)

sys.exit("no application jar (a zip holding StartMysticCryptApplication) in resources/packs/pack-executables")
PY
mv "$target.partial" "$target"
say "cached: $target"
