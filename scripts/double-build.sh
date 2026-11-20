#!/usr/bin/env bash
# Builds what mystic-crypt-ui ships twice from one commit and compares it by SHA-256, the second build
# in another language, time zone and directory (the method of check-reproducible.sh, RL-094, adapted to
# an application: the host jars, every plugin zip, the signed jar and the installer), each step as
# make izpack-installer runs it: createAllDependendiesJar -PcreateIzPackInstaller=true, then checksumInstaller.
#
# Usage: double-build.sh <repository> <commit> <java home> <keystore> <alias> [archives|installer]
# archives: the host jars and the plugin zips only, no signing and no izpack
# The keystore is a throwaway one; its two passwords come from MYSTIC_CRYPT_STORE_PASSWORD and
# MYSTIC_CRYPT_KEY_PASSWORD in the environment, as scripts/with-signing-passwords.sh hands them over.
set -uo pipefail
repo=$1; commit=$2; java=$3; keystore=$4; alias=$5; mode=${6:-installer}
# offline in the archives mode: everything it needs is in the Gradle cache, and Central throttles
offline=""; [ "$mode" = archives ] && offline=--offline
# an init script for the measurement, as DOUBLE_BUILD_INIT; empty for none
init=""; [ -n "${DOUBLE_BUILD_INIT:-}" ] && init="-I $DOUBLE_BUILD_INIT"
work=$(mktemp -d "${TMPDIR:-/tmp}/double-build.XXXXXX")
for l in de_DE.UTF-8 en_US.UTF-8; do
	locale -a | grep -qiE "^${l/UTF-8/utf8}$" || { echo "locale $l is missing" >&2; exit 2; }
done

build()
{
	local n=$1 lang=$2 tz=$3
	local dir="$work/tree$n"
	git -C "$repo" worktree add -q --detach "$dir" "$commit" || exit 2
	echo "build $n in $dir ($lang, $tz)"
	(cd "$dir" && export JAVA_HOME="$java" LANG="$lang" LC_ALL="$lang" TZ="$tz" &&
		./gradlew --no-daemon --no-build-cache $offline $init -q publishToMavenLocal -x test &&
		for plugin in plugins/*/; do
			./gradlew --no-daemon --no-build-cache $offline $init -q -p "$plugin" pluginZip -x test || exit 1
		done &&
		if [ "$mode" = archives ]; then
			./gradlew --no-daemon --no-build-cache $offline $init -q jar withAllDependendiesJar
		else
			./gradlew --no-daemon --no-build-cache $offline $init -q createAllDependendiesJar -PcreateIzPackInstaller=true \
				-Prelease.mystic-crypt.store.file="$keystore" -Prelease.mystic-crypt.store.type=PKCS12 \
				-Prelease.mystic-crypt.key.alias="$alias" &&
			./gradlew --no-daemon --no-build-cache $offline $init -q checksumInstaller
		fi) > "$work/build$n.log" 2>&1 \
		|| { echo "build $n failed:" >&2; tail -30 "$work/build$n.log" >&2; exit 2; }
	mkdir -p "$work/a$n"
	cp "$dir"/build/libs/mystic-crypt-ui-*[0-9T].jar "$dir"/build/libs/mystic-crypt-ui-*-all.jar "$work/a$n/"
	if [ "$mode" != archives ]; then
		for signed in "$dir"/build/signed/*.jar; do cp "$signed" "$work/a$n/signed-$(basename "$signed")"; done
		cp "$dir"/build/distributions/*-installer.jar "$work/a$n/"
	fi
	cp "$dir"/plugins/*/build/plugin-dist/*.zip "$work/a$n/"
	(cd "$work/a$n" && sha256sum -- * | sort -k2 > "$work/sha$n.txt")
}

build 1 de_DE.UTF-8 Europe/Berlin
build 2 en_US.UTF-8 Pacific/Auckland
echo "artifacts: $(wc -l < "$work/sha1.txt")"
join -j 2 "$work/sha1.txt" "$work/sha2.txt" | awk '{printf "%-58s %s %s %s\n", $1, substr($2,1,16), substr($3,1,16), ($2==$3 ? "identical" : "DIFFERENT")}'
join -j 2 "$work/sha1.txt" "$work/sha2.txt" | awk '$2 != $3 {print $1}' | while read -r file; do
	python3 -I - "$work/a1/$file" "$work/a2/$file" <<'PY'
import sys, zipfile
a, b = zipfile.ZipFile(sys.argv[1]), zipfile.ZipFile(sys.argv[2])
na, nb = [i.filename for i in a.infolist()], [i.filename for i in b.infolist()]
print(f"  {sys.argv[1].rsplit('/',1)[1]}: {len(na)} and {len(nb)} entries, same names in the same order: {na == nb}")
times = sum(1 for x, y in zip(a.infolist(), b.infolist()) if x.date_time != y.date_time)
print(f"    entries whose time differs: {times}; first times: {a.infolist()[0].date_time} / {b.infolist()[0].date_time}")
differing = [n for n in na if n in nb and a.read(n) != b.read(n)]
print(f"    entries whose content differs: {len(differing)}: {differing[:12]}")
PY
done
echo "work directory: $work"
