#!/usr/bin/env bash
#
# Measures the vault format across releases with real entries (#757): every release from 7.2 on, and this
# build, writes a vault through its own save path - protected by a password, by a key file alone, and by both -
# with a root group and a group "mykeys" and one entry in each, kept in the group's element as the application
# keeps them; then readers open those vaults and their entries are compared with what was written, line by line
# (scripts/fixtures/EntryLines.java: every text field, the expiry date, the custom property with its value, the
# attachment by the sha256 of its content).
#
#   scripts/compat/measure-vault-format.sh <empty work directory> [<this build's all-dependencies jar>]
#
# Readers: 8.12, 8.13 and this build for every vault; besides, 8.6 to 8.11 for the password vault 8.12 wrote,
# which is what the FORMAT section of 8.12 states. The application jars come from the installers on the
# release pages, checked against their published sha256 (scripts/fetch-release-jar.sh); 7.2, 7.3, 8 and 8.1.1
# publish none and are taken without a check, which the table says. Without a jar named for this build, it is
# built here (./gradlew withAllDependendiesJar). The key is an RSA key made for the run; nothing of a real
# vault or key is read. Prints one table row per writer, protection and reader, and writes the lines to the
# work directory.
set -uo pipefail

work="${1:?usage: scripts/compat/measure-vault-format.sh <empty work directory> [<this build all-dependencies jar>]}"
repository="$(git rev-parse --show-toplevel)"
fixtures="$repository/scripts/fixtures"
java="${JAVA_HOME:-$HOME/.sdkman/candidates/java/current}/bin/java"
checked="${MYSTIC_CRYPT_UI_RELEASE_JARS:-$HOME/.cache/mystic-crypt-ui/release-jars}"
unchecked="${MYSTIC_CRYPT_UI_RELEASE_JARS_UNVERIFIED:-$HOME/.cache/mystic-crypt-ui/release-jars-unverified}"
without_checksum="7.2 7.3 8 8.1.1"
writers="7.2 7.3 8 8.1.1 8.2 8.3 8.4 8.5 8.5.1 8.6 8.7 8.8 8.9 8.10 8.11 8.12 8.13 this"
readers="8.12 8.13 this"
readers_of_812="8.6 8.7 8.8 8.9 8.10 8.11"

die() { printf 'measure-vault-format: %s\n' "$*" >&2; exit 1; }
mkdir -p "$work"
[ -z "$(ls -A "$work")" ] || die "$work is not empty"
command -v openssl > /dev/null || die "openssl makes the key and is not installed"

jar_of() {
  case " $without_checksum " in
    *" $1 "*) printf '%s/%s/mystic-crypt-ui-%s-app.jar' "$unchecked" "$1" "$1" ;;
    *) printf '%s/%s/mystic-crypt-ui-%s-app.jar' "$checked" "$1" "$1" ;;
  esac
}

this_jar="${2:-}"
if [ -z "$this_jar" ]; then
  (cd "$repository" && ./gradlew -q withAllDependendiesJar -x test > "$work/this-build.log" 2>&1) || die "this build's jar was not built, see $work/this-build.log"
  this_jar="$repository/build/libs/mystic-crypt-ui-$(sed -n 's/^projectVersion=//p' "$repository/gradle.properties")-all.jar"
fi
[ -f "$this_jar" ] || die "no jar for this build: $this_jar"

for release in $writers $readers_of_812; do
  [ "$release" = this ] && continue
  case " $without_checksum " in
    *" $release "*) "$repository/scripts/fetch-release-jar.sh" "$release" --without-published-checksum > /dev/null ;;
    *) "$repository/scripts/fetch-release-jar.sh" "$release" > /dev/null ;;
  esac || die "no application jar for $release"
done

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$work/key.pem" 2> /dev/null || die "no key"
password="measure-$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')"

jar() { if [ "$1" = this ]; then printf '%s' "$this_jar"; else jar_of "$1"; fi; }
options() {
  case "$1" in
    key) printf -- '--key-file\n%s\n' "$work/key.pem" ;;
    both) printf -- '--password-and-key-file\n%s\n' "$work/key.pem" ;;
  esac
}

write() {
  local release=$1 protection=$2 vault="$work/vault-$1-$2.mcrdb"
  mapfile -t extra < <(options "$protection")
  printf '%s' "$password" | "$java" -cp "$(jar "$release")" "$fixtures/WriteWithAnOldRelease.java" "$vault" "${extra[@]}" \
    > "$work/written-$release-$protection.txt" 2> "$work/written-$release-$protection.err"
}

read_with() {
  local reader=$1 writer=$2 protection=$3 vault="$work/vault-$2-$3.mcrdb" out="$work/read-$1-from-$2-$3.txt"
  mapfile -t extra < <(options "$protection")
  printf '%s' "$password" | "$java" -cp "$(jar "$reader")" "$fixtures/OpenWithAnOldRelease.java" "$vault" "${extra[@]}" \
    > "$out" 2> "${out%.txt}.err"
  if grep -q '^REFUSED' "$out"; then
    printf 'refused: %s' "$(grep '^REFUSED' "$out" | cut -c9- | cut -c1-110)"
  elif ! grep -q '^OPENED' "$out"; then
    printf 'failed: %s' "$(grep -vE '^WARNING|^\s+at ' "${out%.txt}.err" | tail -1 | cut -c1-110)"
  elif diff <(grep '^ENTRY' "$work/written-$writer-$protection.txt") <(grep '^ENTRY' "$out") > /dev/null; then
    printf 'opens, every entry as written (%s)' "$(grep -c '^ENTRY' "$out")"
  else
    printf 'opens, entries DIFFER: %s written, %s read' "$(grep -c '^ENTRY' "$work/written-$writer-$protection.txt")" "$(grep -c '^ENTRY' "$out")"
  fi
}

note() { case " $without_checksum " in *" $1 "*) printf ' (no published checksum)' ;; esac; }

printf '| written by | protection | read by | result |\n|---|---|---|---|\n'
for writer in $writers; do
  for protection in password key both; do
    if ! write "$writer" "$protection" || ! grep -q '^WRITTEN' "$work/written-$writer-$protection.txt"; then
      printf '| %s%s | %s | - | not written: %s |\n' "$writer" "$(note "$writer")" "$protection" \
        "$(grep -vE '^WARNING|^\s+at ' "$work/written-$writer-$protection.err" | tail -1 | cut -c1-110)"
      continue
    fi
    for reader in $readers; do
      printf '| %s%s | %s | %s | %s |\n' "$writer" "$(note "$writer")" "$protection" "$reader" "$(read_with "$reader" "$writer" "$protection")"
    done
  done
done
for reader in $readers_of_812; do
  printf '| 8.12 | password | %s | %s |\n' "$reader" "$(read_with "$reader" 8.12 password)"
done
