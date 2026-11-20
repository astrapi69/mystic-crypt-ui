#!/usr/bin/env python3
"""Tests for the Makefile's build slot guard: which targets start Gradle, and what they do without a slot.

The targets come from make's own database and are sorted by a dry run, never from a hand list: a target
starts Gradle when its dry run (the prerequisites and sub-makes included) prints gradlew, or calls a file
under scripts/ that mentions it. A test that finds no targets, or no Gradle target, fails.

Run: python3 scripts/gradle_slot_guard_test.py
"""

import os
import re
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE = re.compile(r"\bgradlew\b")
SCRIPT = re.compile(r"scripts/[A-Za-z0-9_./-]+")
TARGET = re.compile(r"^([A-Za-z0-9][A-Za-z0-9_-]*):(?!=)")
# make is started by make in CI and by this suite's callers: nothing of the outer run may reach the inner one
OUTER_RUN = ("GRADLE_SLOT", "GITHUB_ACTIONS", "MAKEFLAGS", "MFLAGS", "MAKELEVEL", "MAKEOVERRIDES")


def environment(**extra):
    env = {name: value for name, value in os.environ.items() if name not in OUTER_RUN}
    env["LC_ALL"] = "C"
    env.update(extra)
    return env


def make(arguments, **extra):
    return subprocess.run(["make", *arguments], cwd=ROOT, env=environment(**extra),
                          capture_output=True, text=True, check=False)


def declared_targets():
    """Every target of the Makefile as make's database lists it, files that are no target left out"""
    database = make(["-pRrq"], GRADLE_SLOT="1").stdout.splitlines()
    names = set()
    for before, line in zip([""] + database, database):
        found = TARGET.match(line)
        if found and before != "# Not a target:":
            names.add(found.group(1))
    return sorted(names)


def mentions_gradle(script, seen):
    """True when the file under scripts/ runs gradlew, or a script it calls does; a comment that names one runs nothing"""
    path = ROOT / script
    if script in seen or not path.is_file():
        return False
    seen.add(script)
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    text = "\n".join(line for line in lines if not line.lstrip().startswith("#"))
    return bool(GRADLE.search(text)) or any(mentions_gradle(name, seen) for name in SCRIPT.findall(text))


def starts_gradle(target):
    """Decided with a slot set, so that the answer does not depend on the guard under test"""
    output = make(["-n", target], GRADLE_SLOT="1").stdout
    return bool(GRADLE.search(output)) or any(mentions_gradle(name, set()) for name in SCRIPT.findall(output))


def sorted_targets():
    targets = declared_targets()
    gradle = [target for target in targets if starts_gradle(target)]
    return gradle, [target for target in targets if target not in gradle]


GRADLE_TARGETS, FREE_TARGETS = sorted_targets()


class TheTargetsAreFoundTest(unittest.TestCase):

    def test_the_makefile_has_targets_and_some_of_them_start_gradle(self):
        self.assertTrue(FREE_TARGETS + GRADLE_TARGETS, "no target found: the discovery is broken")
        self.assertTrue(GRADLE_TARGETS, "no target that starts Gradle found: the discovery is broken")
        self.assertIn("build", GRADLE_TARGETS)
        self.assertIn("bump-check", GRADLE_TARGETS)
        self.assertIn("help", FREE_TARGETS)


class WithoutASlotTodayTest(unittest.TestCase):
    """Pins today's behaviour: nothing asks for a slot"""

    def test_a_gradle_target_runs_without_a_slot(self):
        for target in GRADLE_TARGETS:
            with self.subTest(target=target):
                result = make(["-n", target])
                self.assertEqual(0, result.returncode, result.stderr)

    def test_a_free_target_runs_without_a_slot(self):
        for target in FREE_TARGETS:
            with self.subTest(target=target):
                result = make(["-n", target])
                self.assertEqual(0, result.returncode, result.stderr)


if __name__ == "__main__":
    unittest.main()
