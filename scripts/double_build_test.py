#!/usr/bin/env python3
"""Tests for double-build.sh against a throwaway git repository whose gradlew is a stub (#1000).

The stub records the arguments and the GRADLE_USER_HOME of every call and leaves the files the script
copies, so no real build runs and no real keystore is involved.

Run: python3 scripts/double_build_test.py
"""

import os
import secrets
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "double-build.sh"
ALIAS = "throwaway-alias"
INHERITED_HOME = "inherited-home"

STUB_GRADLEW = """#!/bin/sh
{
  printf 'CALL\\n'
  printf 'HOME=%s\\n' "${GRADLE_USER_HOME-unset}"
  printf 'RO=%s\\n' "${GRADLE_RO_DEP_CACHE-unset}"
  for argument in "$@"; do printf 'ARG=%s\\n' "$argument"; done
} >> "$STUB_LOG"
mkdir -p build/libs build/signed build/distributions
echo jar > build/libs/mystic-crypt-ui-1.jar
echo all > build/libs/mystic-crypt-ui-1-all.jar
echo signed > build/signed/mystic-crypt-ui-1.jar
echo installer > build/distributions/mystic-crypt-ui-1-installer.jar
for plugin in plugins/*/; do
  mkdir -p "${plugin}build/plugin-dist"
  echo zip > "${plugin}build/plugin-dist/$(basename "$plugin").zip"
done
"""


def locales_present():
    names = subprocess.run(["locale", "-a"], capture_output=True, text=True).stdout.lower()
    return "de_de.utf8" in names and "en_us.utf8" in names


def git(repo, *arguments):
    subprocess.run(["git", "-c", "user.name=test", "-c", "user.email=test@example.org", "-C", str(repo),
                    *arguments], check=True, capture_output=True)


def parse_calls(log):
    calls = []
    for line in log.read_text().splitlines():
        if line == "CALL":
            calls.append({"home": None, "ro": None, "args": []})
        elif line.startswith("HOME="):
            calls[-1]["home"] = line[len("HOME="):]
        elif line.startswith("RO="):
            calls[-1]["ro"] = line[len("RO="):]
        elif line.startswith("ARG="):
            calls[-1]["args"].append(line[len("ARG="):])
    return calls


@unittest.skipUnless(locales_present(), "needs the de_DE.utf8 and en_US.utf8 locales the script requires")
class DoubleBuildTest(unittest.TestCase):

    def setUp(self):
        self.root = Path(tempfile.mkdtemp(prefix="double-build-test."))
        self.addCleanup(shutil.rmtree, self.root, True)
        self.repo = self.root / "repo"
        (self.repo / "plugins" / "alpha-plugin").mkdir(parents=True)
        (self.repo / "plugins" / "alpha-plugin" / "plugin.properties").write_text("id=alpha\n")
        gradlew = self.repo / "gradlew"
        gradlew.write_text(STUB_GRADLEW)
        gradlew.chmod(0o755)
        git(self.repo.parent, "init", "-q", str(self.repo))
        git(self.repo, "add", ".")
        git(self.repo, "commit", "-q", "-m", "stub")
        self.commit = subprocess.run(["git", "-C", str(self.repo), "rev-parse", "HEAD"], check=True,
                                     capture_output=True, text=True).stdout.strip()
        self.inherited = self.root / INHERITED_HOME
        (self.inherited / "wrapper").mkdir(parents=True)
        (self.root / "tmp").mkdir()
        self.store_password = secrets.token_hex(16)
        self.key_password = secrets.token_hex(16)
        self.keystore = self.root / "throwaway.p12"
        self.stub_log = self.root / "stub.log"

    def run_script(self, mode, extra_env=None):
        env = dict(os.environ, STUB_LOG=str(self.stub_log), GRADLE_USER_HOME=str(self.inherited),
                   TMPDIR=str(self.root / "tmp"), MYSTIC_CRYPT_STORE_PASSWORD=self.store_password,
                   MYSTIC_CRYPT_KEY_PASSWORD=self.key_password)
        env.pop("GRADLE_RO_DEP_CACHE", None)
        if extra_env:
            env.update(extra_env)
        return subprocess.run(["bash", str(SCRIPT), str(self.repo), self.commit, "/java", str(self.keystore),
                               ALIAS, mode], env=env, capture_output=True, text=True, timeout=120)

    def calls_of(self, mode):
        result = self.run_script(mode)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        return parse_calls(self.stub_log)

    def assert_every_call_names_the_throwaway_keystore(self, calls):
        expected = {f"-Prelease.mystic-crypt.store.file={self.keystore}", "-Prelease.mystic-crypt.store.type=PKCS12",
                    f"-Prelease.mystic-crypt.key.alias={ALIAS}"}
        for call in calls:
            self.assertTrue(expected <= set(call["args"]), f"a call lacks the keystore properties: {call['args']}")

    def test_installer_mode_every_call_names_the_throwaway_keystore(self):
        calls = self.calls_of("installer")
        self.assertEqual(len(calls), 8, "per build: publish, one plugin zip, createAllDependendiesJar, checksumInstaller")
        self.assert_every_call_names_the_throwaway_keystore(calls)

    def test_archives_mode_every_call_names_the_throwaway_keystore(self):
        calls = self.calls_of("archives")
        self.assertEqual(len(calls), 6, "per build: publish, one plugin zip, jar withAllDependendiesJar")
        self.assert_every_call_names_the_throwaway_keystore(calls)

    def test_each_build_has_a_gradle_user_home_of_its_own_and_not_the_inherited_one(self):
        calls = self.calls_of("installer")
        first, second = {call["home"] for call in calls[:4]}, {call["home"] for call in calls[4:]}
        self.assertEqual(len(first), 1, "one home for all calls of build 1")
        self.assertEqual(len(second), 1, "one home for all calls of build 2")
        self.assertNotEqual(first, second)
        self.assertNotIn(str(self.inherited), first | second)

    def test_the_dependencies_come_read_only_from_the_inherited_cache(self):
        calls = self.calls_of("installer")
        self.assertEqual({call["ro"] for call in calls}, {str(self.inherited / "caches")})

    def test_a_given_read_only_cache_wins_over_the_default(self):
        cache = str(self.root / "given-cache")
        result = self.run_script("archives", {"GRADLE_RO_DEP_CACHE": cache})
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual({call["ro"] for call in parse_calls(self.stub_log)}, {cache})

    def test_the_wrapper_distribution_is_linked_into_each_home(self):
        calls = self.calls_of("archives")
        for home in {call["home"] for call in calls}:
            self.assertEqual(os.path.realpath(Path(home) / "wrapper"), os.path.realpath(self.inherited / "wrapper"))

    def test_gradle_slot_runner_runs_every_call_through_the_wrapper_it_names(self):
        slot = self.root / "slot.sh"
        slot_log = self.root / "slot.log"
        slot.write_text('#!/bin/sh\ntree=$1; log=$2; shift 2\nprintf \'%s\\n\' "$tree" >> "$SLOT_LOG"\n'
                        '(cd "$tree" && ./gradlew "$@") > "$log" 2>&1\n')
        slot.chmod(0o755)
        result = self.run_script("installer", {"GRADLE_SLOT_RUNNER": str(slot), "SLOT_LOG": str(slot_log)})
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        trees = slot_log.read_text().split()
        self.assertEqual(len(trees), 8)
        self.assertEqual(len(set(trees)), 2, "the slot gets the tree of each build")
        self.assert_every_call_names_the_throwaway_keystore(parse_calls(self.stub_log))

    def test_a_failing_call_through_the_slot_stops_the_double_build(self):
        slot = self.root / "slot.sh"
        slot.write_text("#!/bin/sh\necho refused > \"$2\"\nexit 3\n")
        slot.chmod(0o755)
        result = self.run_script("installer", {"GRADLE_SLOT_RUNNER": str(slot)})
        self.assertEqual(result.returncode, 2)
        self.assertIn("build 1 failed", result.stderr)
        self.assertIn("refused", result.stderr)

    def test_the_slot_number_a_slot_sets_is_not_taken_for_a_wrapper(self):
        result = self.run_script("installer", {"GRADLE_SLOT": "1"})
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assert_every_call_names_the_throwaway_keystore(parse_calls(self.stub_log))

    def test_no_call_has_a_password_among_its_arguments(self):
        calls = self.calls_of("installer")
        for call in calls:
            for argument in call["args"]:
                self.assertNotIn(self.store_password, argument)
                self.assertNotIn(self.key_password, argument)


if __name__ == "__main__":
    unittest.main()
