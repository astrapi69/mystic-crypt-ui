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

    def test_installer_mode_today_only_the_third_call_of_a_build_names_the_keystore(self):
        calls = self.calls_of("installer")
        self.assertEqual(len(calls), 8, "per build: publish, one plugin zip, createAllDependendiesJar, checksumInstaller")
        with_properties = [index for index, call in enumerate(calls)
                           if any(argument.startswith("-Prelease.mystic-crypt.") for argument in call["args"])]
        self.assertEqual(with_properties, [2, 6])

    def test_installer_mode_today_every_call_runs_in_the_gradle_user_home_it_inherited(self):
        calls = self.calls_of("installer")
        self.assertEqual({call["home"] for call in calls}, {str(self.inherited)})

    def test_no_call_has_a_password_among_its_arguments(self):
        calls = self.calls_of("installer")
        for call in calls:
            for argument in call["args"]:
                self.assertNotIn(self.store_password, argument)
                self.assertNotIn(self.key_password, argument)


if __name__ == "__main__":
    unittest.main()
