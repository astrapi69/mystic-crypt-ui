#!/usr/bin/env bash
#
# Writes a file and a text with the file encryption window of a published release, through that release's
# own FileCryptSupport (scripts/fixtures/WriteFileCryptWithAnOldRelease.java), for the tests that today's
# window and --cli decrypt open what an older window wrote (#877). Meant for 8.2 to 8.6, the releases
# before #490 moved the window onto the library's PassphraseEnvelope "byte for byte the same layout".
#
#   scripts/fixtures/write-file-crypt-of-an-old-release.sh <version> <empty output directory>
#
# "The release" is the file users downloaded: the installer from GitHub Releases, checked against the
# sha256 published next to it, and from its packs the application jar (scripts/installer-pack.py) and the
# classes of the file-crypt plugin. Nothing of this checkout is on the class path. The passphrase is
# TestPasswords.FILE_CRYPT_WINDOW_FIXTURE, read from there, so it stands in one place. Each run writes
# different files: the salt is new every time.
set -euo pipefail

version="${1:?usage: scripts/fixtures/write-file-crypt-of-an-old-release.sh <version> <empty output directory>}"
out="${2:?usage: scripts/fixtures/write-file-crypt-of-an-old-release.sh <version> <empty output directory>}"
repository="$(git rev-parse --show-toplevel)"
java="${JAVA_HOME:-$HOME/.sdkman/candidates/java/current}/bin/java"
installer="mystic-crypt-ui-$version-installer.jar"
base_url="https://github.com/astrapi69/mystic-crypt-ui/releases/download/RELEASE-$version"

die() { printf 'write-file-crypt-of-an-old-release: %s\n' "$*" >&2; exit 1; }

mkdir -p "$out"
[ -z "$(ls -A "$out")" ] || die "$out is not empty"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
passphrase="$(sed -n 's/.*FILE_CRYPT_WINDOW_FIXTURE = "\([^"]*\)";.*/\1/p' \
  "$repository/src/testSupport/java/io/github/astrapi69/mystic/crypt/TestPasswords.java")"
[ -n "$passphrase" ] || die "no FILE_CRYPT_WINDOW_FIXTURE in TestPasswords.java"

curl -fsSL --retry 3 -o "$work/$installer" "$base_url/$installer" || die "could not download $base_url/$installer"
curl -fsSL --retry 3 -o "$work/$installer.sha256" "$base_url/$installer.sha256" \
  || die "could not download $base_url/$installer.sha256"
expected="$(cut -c1-64 "$work/$installer.sha256")"
actual="$(sha256sum "$work/$installer" | cut -c1-64)"
[ "$expected" = "$actual" ] || die "$installer is $actual, its published sha256 says $expected"
printf '==> %s, sha256 %s, as RELEASE-%s publishes it\n' "$installer" "$actual" "$version"

python3 -I "$repository/scripts/installer-pack.py" application-jar "$work/$installer" "$work/app.jar" "$version"
python3 -I - "$repository/scripts/installer-pack.py" "$work/$installer" "$work/plugin" <<'PY'
import importlib.util, io, os, sys, zipfile
spec = importlib.util.spec_from_file_location("installer_pack", sys.argv[1])
pack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pack)
window = "classes/io/github/astrapi69/mystic/crypt/plugin/filecrypt/FileCryptSupport.class"
for chunk in pack.archives(sys.argv[2], "pack-plugins"):
    with zipfile.ZipFile(io.BytesIO(chunk)) as plugin:
        if window not in plugin.namelist():
            continue
        names = [n for n in plugin.namelist() if n.startswith(("classes/", "lib/")) and not n.endswith("/")]
        for name in names:
            target = os.path.join(sys.argv[3], name)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "wb") as written:
                written.write(plugin.read(name))
        print(f"==> extracted the file-crypt plugin, {len(names)} files")
        sys.exit(0)
sys.exit("no plugin zip with FileCryptSupport in resources/packs/pack-plugins")
PY

classpath="$work/app.jar:$work/plugin/classes"
for library in "$work"/plugin/lib/*.jar; do [ -e "$library" ] && classpath="$classpath:$library"; done
printf '%s' "$passphrase" | "$java" -cp "$classpath" \
  "$repository/scripts/fixtures/WriteFileCryptWithAnOldRelease.java" "$version" "$out"
(cd "$out" && sha256sum -- *)
