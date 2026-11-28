#!/usr/bin/env bash
#
# Writes the vaults of 7.2, 7.3, 8 and 8.1.1 that src/testSupport/resources/compat/vaults holds for #864, the eight
# release and protection combinations that #624 and #650 did not write: each one by that release's own application
# jar, through the save path of its protection (scripts/fixtures/WriteWithAnOldRelease.java), with the two groups and
# entries the other vaults there hold. Then each is opened again by the release that wrote it
# (scripts/fixtures/OpenWithAnOldRelease.java), and its entries have to read back as they were written. Nothing of this
# checkout is on the class path. It runs in GitHub Actions, write-vaults-of-old-releases.yml (AG-021):
#
#   scripts/fixtures/write-vaults-of-every-variant.sh <empty output directory>
#
# None of these releases publishes a checksum, so every installer is downloaded anew and checked, with the application
# jar taken out of it, against the sha256 sums measured on 2026-11-05 and recorded in README.md next to the vaults; a
# sum that does not match stops the run. The key is the repository's RSA test key src/test/resources/pem/private.pem,
# the one the vaults of 8 with a key file have; the password is the one TestPasswords holds as
# VAULTS_OF_EVERY_VARIANT_FIXTURE, read from there, so it stands in one place. Each run writes different files: the
# salt and the entry ids are new every time. The output directory gets the vaults, SHA256SUMS and one log per vault.
set -euo pipefail

out="${1:?usage: scripts/fixtures/write-vaults-of-every-variant.sh <empty output directory>}"
repository="$(git rev-parse --show-toplevel)"
fixtures="$repository/scripts/fixtures"
key="$repository/src/test/resources/pem/private.pem"
java="${JAVA_HOME:-$HOME/.sdkman/candidates/java/current}/bin/java"

die() { printf 'write-vaults-of-every-variant: %s\n' "$*" >&2; exit 1; }

# release, protection: the eight combinations of 7.2, 7.3, 8 and 8.1.1 with password, key-file and
# password-and-key-file that have no vault in src/testSupport/resources/compat/vaults yet
combinations="7.2:key-file 7.2:password-and-key-file 7.3:key-file 7.3:password-and-key-file 8:password
8.1.1:password 8.1.1:key-file 8.1.1:password-and-key-file"

installer_sha256() {
  case "$1" in
    7.2) printf 47c60fc2475c6d7d2411eed6dc7d557af36c3798beec2ea3adb83c837401a1b0 ;;
    7.3) printf 04312d7a474d2df8445ca81da192b26229203b9e92ac3846b1eb15cd46f88702 ;;
    8) printf 31275e12a3473340aeca8d98a12615b6f5b6ba2e281a7e12c00763a5f52c3c13 ;;
    8.1.1) printf 1f9df0fac7dcc513995da5d8c5de011542fdc91bb09f1bf58fdee0df213e551f ;;
    *) die "no installer sha256 recorded for $1" ;;
  esac
}

jar_sha256() {
  case "$1" in
    7.2) printf 699bc85b9b8a98dba765e59958b5babaf485bd3e6f3ce199f5992a04fe38eba5 ;;
    7.3) printf 39ce2c67ca3e833b14c04e4c356225de0d7ea9113f77dc0dc0c9fb19ac84ec9d ;;
    8) printf f093d7261faf5bd8ae5aa2fe717fa5025ce089c264debdef06cdf01b91881985 ;;
    8.1.1) printf 112869045778e3cc90c7688112f35d93111e684ba9d133bb262b0c645dce6f4b ;;
    *) die "no application jar sha256 recorded for $1" ;;
  esac
}

vault_name() {
  if [ "$2" = password ]; then printf 'vault-%s.mcrdb' "$1"; else printf 'vault-%s-%s.mcrdb' "$1" "$2"; fi
}

mkdir -p "$out"
[ -z "$(ls -A "$out")" ] || die "$out is not empty"
[ -f "$key" ] || die "no test key at $key"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
# a cache of this run's own, so every jar comes from an installer downloaded and checked in this run
export MYSTIC_CRYPT_UI_RELEASE_JARS_UNVERIFIED="$work/release-jars"
password="$(sed -n 's/.*VAULTS_OF_EVERY_VARIANT_FIXTURE = "\([^"]*\)";.*/\1/p' \
  "$repository/src/testSupport/java/io/github/astrapi69/mystic/crypt/TestPasswords.java")"
[ -n "$password" ] || die "no VAULTS_OF_EVERY_VARIANT_FIXTURE in TestPasswords.java"

for version in 7.2 7.3 8 8.1.1; do
  "$repository/scripts/fetch-release-jar.sh" "$version" --without-published-checksum > "$work/fetch-$version.log" \
    || { cat "$work/fetch-$version.log" >&2; die "no application jar for $version"; }
  cat "$work/fetch-$version.log"
  grep -qxF "$(installer_sha256 "$version")  mystic-crypt-ui-$version-installer.jar" "$work/fetch-$version.log" \
    || die "the installer of $version does not have the sha256 README.md records"
  jar="$MYSTIC_CRYPT_UI_RELEASE_JARS_UNVERIFIED/$version/mystic-crypt-ui-$version-app.jar"
  printf '%s  %s\n' "$(jar_sha256 "$version")" "$jar" | sha256sum -c - \
    || die "the application jar of $version does not have the sha256 README.md records"
done

for combination in $combinations; do
  version="${combination%%:*}"
  protection="${combination#*:}"
  vault="$out/$(vault_name "$version" "$protection")"
  jar="$MYSTIC_CRYPT_UI_RELEASE_JARS_UNVERIFIED/$version/mystic-crypt-ui-$version-app.jar"
  options=()
  [ "$protection" = password ] || options=("--$protection" "$key")
  log="$out/$(vault_name "$version" "$protection").log"
  printf '%s' "$password" | "$java" -cp "$jar" "$fixtures/WriteWithAnOldRelease.java" "$vault" "${options[@]}" \
    > "$work/written.txt" 2> "$work/written.err" \
    || { cat "$work/written.err" >&2; die "$version could not write its vault with $protection"; }
  grep -qx WRITTEN "$work/written.txt" || die "$version did not say WRITTEN for $protection"
  printf '%s' "$password" | "$java" -cp "$jar" "$fixtures/OpenWithAnOldRelease.java" "$vault" "${options[@]}" \
    > "$work/opened.txt" 2> "$work/opened.err" \
    || { cat "$work/opened.txt" "$work/opened.err" >&2; die "$version could not open the vault it wrote with $protection"; }
  { printf '== written by %s with %s\n' "$version" "$protection"; cat "$work/written.txt"
    printf '== opened by %s with %s\n' "$version" "$protection"; cat "$work/opened.txt"; } > "$log"
  diff <(grep '^ENTRY ' "$work/written.txt") <(grep '^ENTRY ' "$work/opened.txt") > /dev/null \
    || { cat "$log" >&2; die "$version read back other entries than it wrote with $protection"; }
  [ "$(grep -c '^ENTRY ' "$work/opened.txt")" = 2 ] || { cat "$log" >&2; die "$version did not write two entries"; }
  printf '==> %s with %s: %s\n' "$version" "$protection" "$(grep '^OPENED' "$work/opened.txt")"
done

(cd "$out" && sha256sum -- *.mcrdb > SHA256SUMS && cat SHA256SUMS)
