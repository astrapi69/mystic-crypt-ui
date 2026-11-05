#!/usr/bin/env bash
#
# Writes the vaults of 7.2 and 7.3 that src/testSupport/resources/compat/vaults holds (#624): each one by
# that release's own application jar, through its own password save path, with a root group and a
# "mykeys" group from the release's own tree factory and one entry in each
# (scripts/fixtures/WriteWithAnOldRelease.java). Nothing of this checkout is on the class path.
#
#   scripts/fixtures/write-vaults-of-7x.sh <empty output directory>
#
# 7.x publishes no checksum, so the installers cannot be checked: fetch-release-jar.sh prints their
# sha256, and README.md next to the vaults records it. The password is the one TestPasswords
# holds as VAULTS_OF_7X_FIXTURE, read from there, so it stands in one place. Each run writes
# different files: the salt and the entry ids are new every time.
set -euo pipefail

out="${1:?usage: scripts/fixtures/write-vaults-of-7x.sh <empty output directory>}"
repository="$(git rev-parse --show-toplevel)"
java="${JAVA_HOME:-$HOME/.sdkman/candidates/java/current}/bin/java"

die() { printf 'write-vaults-of-7x: %s\n' "$*" >&2; exit 1; }

mkdir -p "$out"
[ -z "$(ls -A "$out")" ] || die "$out is not empty"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
password="$(sed -n 's/.*VAULTS_OF_7X_FIXTURE = "\([^"]*\)";.*/\1/p' \
  "$repository/src/testSupport/java/io/github/astrapi69/mystic/crypt/TestPasswords.java")"
[ -n "$password" ] || die "no VAULTS_OF_7X_FIXTURE in TestPasswords.java"

for version in 7.2 7.3; do
  "$repository/scripts/fetch-release-jar.sh" "$version" --without-published-checksum
  jar="${MYSTIC_CRYPT_UI_RELEASE_JARS_UNVERIFIED:-$HOME/.cache/mystic-crypt-ui/release-jars-unverified}/$version/mystic-crypt-ui-$version-app.jar"
  printf '%s' "$password" | "$java" -cp "$jar" "$repository/scripts/fixtures/WriteWithAnOldRelease.java" \
    "$out/vault-$version.mcrdb" 2> "$work/write-$version.log" \
    || { cat "$work/write-$version.log" >&2; die "$version could not write its vault"; }
  printf '==> %s: %s\n' "$version" "$(sha256sum "$out/vault-$version.mcrdb")"
done
