#!/usr/bin/env bash
#
# Writes a test network chain file the way mystic-crypt-ui 8.8 writes one, for the end-to-end test
# of the conversion into lethenon 0.6.0's database (#581). The file is written by 8.8's own code:
# the lethenon plugin from the 8.8 installer, with the lethenon 0.4.0 it carries, and the 8.8
# application jar - nothing of this checkout is on the class path.
#
#   scripts/fixtures/write-a-chain-file-with-8.8.sh <empty output directory>
#
# The installer is checked against the checksum published with RELEASE-8.8 before anything is taken
# out of it. Each run writes a different file: the wallet and the proof of work are new every time.
set -euo pipefail

out="${1:?usage: scripts/fixtures/write-a-chain-file-with-8.8.sh <empty output directory>}"
repository="$(git rev-parse --show-toplevel)"
version=8.8
installer_name="mystic-crypt-ui-$version-installer.jar"
base_url="https://github.com/astrapi69/mystic-crypt-ui/releases/download/RELEASE-$version"
# 2026-10-16T12:00:00Z: in the past, so that no block is from the future
start_millis=1792152000000

die() { printf 'write-a-chain-file-with-8.8: %s\n' "$*" >&2; exit 1; }

mkdir -p "$out"
[ -z "$(ls -A "$out")" ] || die "$out is not empty"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

"$repository/scripts/fetch-release-jar.sh" "$version"
application_jar="${MYSTIC_CRYPT_UI_RELEASE_JARS:-$HOME/.cache/mystic-crypt-ui/release-jars}/$version/mystic-crypt-ui-$version-app.jar"

curl -fsSL --retry 3 -o "$work/$installer_name" "$base_url/$installer_name" \
  || die "could not download $base_url/$installer_name"
curl -fsSL --retry 3 -o "$work/$installer_name.sha256" "$base_url/$installer_name.sha256" \
  || die "could not download $base_url/$installer_name.sha256"
(cd "$work" && sha256sum -c "$installer_name.sha256") \
  || die "the installer does not match the checksum published with RELEASE-$version"

python3 -I - "$work/$installer_name" "$work/lethenon-plugin.zip" <<'PY'
import io, sys, zipfile, zlib
installer, target = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(installer) as outer:
    pack = outer.read("resources/packs/pack-plugins")
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
        if "plugin.properties" not in candidate.namelist():
            continue
        properties = candidate.read("plugin.properties").decode("utf-8")
    if "plugin.id=lethenon-plugin" not in properties.splitlines():
        continue
    with open(target, "wb") as written:
        written.write(chunk)
    print(f"==> extracted the lethenon plugin, {len(chunk)} bytes")
    sys.exit(0)
sys.exit("no lethenon plugin zip in resources/packs/pack-plugins")
PY

mkdir "$work/plugin"
unzip -q "$work/lethenon-plugin.zip" -d "$work/plugin"
lethenon_jar="$(ls "$work"/plugin/lib/lethenon-*.jar)"
printf '==> the plugin carries %s\n' "$(basename "$lethenon_jar")"

java --enable-native-access=ALL-UNNAMED \
  -cp "$work/plugin/classes:$lethenon_jar:$application_jar" \
  "$repository/scripts/fixtures/WriteAChainFileWith88.java" \
  "$out/written-by-8.8.lethenon" "$work/wallet-8.8.lethenon-wallet" fixture "$start_millis"
sha256sum "$out/written-by-8.8.lethenon"
