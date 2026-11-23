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
import tempfile
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
    """The bypass is named in its own tests: it starts Gradle only for the target it is given"""
    targets = [target for target in declared_targets() if target != "without-slot"]
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


class TheGuardTest(unittest.TestCase):

    def test_a_gradle_target_refuses_without_a_slot(self):
        for target in GRADLE_TARGETS:
            with self.subTest(target=target):
                result = make(["-n", target])
                self.assertNotEqual(0, result.returncode, "ran without a slot: " + target)
                self.assertIn(target, result.stderr)
                self.assertIn("GRADLE_SLOT", result.stderr)
                self.assertIn("make without-slot TARGET=" + target, result.stderr)

    def test_an_empty_slot_counts_as_none_and_a_runner_that_is_not_ci_gets_no_pass(self):
        for name, extra in (("empty slot", {"GRADLE_SLOT": ""}), ("GITHUB_ACTIONS=false", {"GITHUB_ACTIONS": "false"})):
            with self.subTest(case=name):
                self.assertNotEqual(0, make(["-n", "build"], **extra).returncode)

    def test_a_gradle_target_runs_inside_a_slot(self):
        for target in GRADLE_TARGETS:
            with self.subTest(target=target):
                result = make(["-n", target], GRADLE_SLOT="1")
                self.assertEqual(0, result.returncode, result.stderr)

    def test_a_gradle_target_runs_on_a_ci_runner_which_has_no_slot(self):
        for target in GRADLE_TARGETS:
            with self.subTest(target=target):
                result = make(["-n", target], GITHUB_ACTIONS="true")
                self.assertEqual(0, result.returncode, result.stderr)

    def test_one_of_several_goals_that_starts_gradle_refuses_them_all(self):
        result = make(["-n", "help", "build"])
        self.assertNotEqual(0, result.returncode)
        self.assertIn("build", result.stderr)

    def test_a_free_target_runs_without_a_slot(self):
        for target in FREE_TARGETS:
            with self.subTest(target=target):
                result = make(["-n", target])
                self.assertEqual(0, result.returncode, result.stderr)


class WithoutSlotTest(unittest.TestCase):

    def test_without_slot_runs_the_target_it_is_given(self):
        for target in GRADLE_TARGETS:
            with self.subTest(target=target):
                result = make(["-n", "without-slot", "TARGET=" + target])
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertIn("make " + target + " GRADLE_SLOT=none", result.stdout)

    def test_without_slot_refuses_when_it_is_given_no_target(self):
        for case in ([], ["TARGET="]):
            with self.subTest(arguments=case):
                result = make(["-n", "without-slot", *case])
                self.assertNotEqual(0, result.returncode)
                self.assertIn("TARGET", result.stderr)

    def test_without_slot_is_no_gradle_target_itself(self):
        self.assertNotIn("without-slot", GRADLE_TARGETS)

    def test_the_target_given_to_without_slot_reaches_the_recipes_below_it(self):
        with tempfile.TemporaryDirectory() as folder:
            probe = Path(folder) / "probe.mk"
            probe.write_text('show-target:\n\t@echo "TARGET=[$$TARGET]"\n', encoding="utf-8")
            result = make(["without-slot", "TARGET=show-target"], MAKEFILES=str(probe))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("TARGET=[show-target]", result.stdout)


if __name__ == "__main__":
    unittest.main()
