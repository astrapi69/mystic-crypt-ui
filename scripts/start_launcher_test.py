#!/usr/bin/env python3
"""Tests for start.sh, the launcher the desktop entry runs (#1009), with stub java executables.

The menu start used to run whatever java came first on the session PATH, and with a default
older than 25 it died without a word. start.sh finds a Java 25 or newer, the installer's first,
then the PATH's, and otherwise says why it cannot start. Every case here runs the real script in
a directory laid out like an installation, with a PATH that holds only stubs: a java that prints
a version on standard error, as java -version does, and a notify-send and a zenity that record
their calls.

Run: python3 scripts/start_launcher_test.py
"""

import os
import shutil
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path

DOC = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "doc"
START_SH = DOC / "start.sh"
START_BAT = DOC / "start.bat"
DESKTOP_ENTRY = DOC / "mystic-crypt-ui.desktop"
INSTALLER_JAVA_HOME = DOC / "installer-java-home"

ADD_OPENS = "--add-opens java.base/java.time=ALL-UNNAMED"
UNSAFE_FLAG = "--sun-misc-unsafe-memory-access=allow"

JAVA_STUB = """#!/bin/bash
if [ "$1" = "-version" ]; then
  echo 'Picked up JAVA_TOOL_OPTIONS: -Dstub=true' >&2
  echo 'openjdk version "{version}" 2025-01-01' >&2
  echo 'OpenJDK Runtime Environment (stub)' >&2
  exit 0
fi
echo "run {name} $*" >> "$STUB_LOG"
exit {exit_code}
"""

NOTIFIER_STUB = """#!/bin/bash
echo "{name} $*" >> "$STUB_LOG"
"""


def write_executable(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)
    path.chmod(path.stat().st_mode | stat.S_IXUSR)


class StartLauncherTest(unittest.TestCase):
    def setUp(self):
        self.work = Path(tempfile.mkdtemp(prefix="start-launcher-"))
        self.addCleanup(shutil.rmtree, self.work, ignore_errors=True)
        self.install = self.work / "install dir"
        self.bin = self.work / "bin"
        self.log = self.work / "calls.log"
        self.bin.mkdir()
        self.install.mkdir()
        shutil.copy(START_SH, self.install / "start.sh")
        (self.install / "start.sh").chmod(0o755)

    def stub_java(self, directory, name, version, exit_code=0):
        write_executable(directory / "java", JAVA_STUB.format(
            version=version, name=name, exit_code=exit_code))

    def installer_java(self, version, exit_code=0):
        home = self.work / "installer-jdk"
        self.stub_java(home / "bin", "installer", version, exit_code)
        (self.install / "installer-java-home").write_text(f"{home}\n")

    def path_java(self, version, exit_code=0):
        self.stub_java(self.bin, "path", version, exit_code)

    def notifiers(self, *names):
        for name in names:
            write_executable(self.bin / name, NOTIFIER_STUB.format(name=name))

    def start(self, *arguments):
        environment = {"PATH": str(self.bin), "HOME": str(self.work), "STUB_LOG": str(self.log)}
        return subprocess.run([str(self.install / "start.sh"), *arguments], env=environment,
                              capture_output=True, text=True, timeout=60)

    def calls(self):
        return self.log.read_text().splitlines() if self.log.exists() else []

    def test_installers_java_25_wins_over_the_path(self):
        self.installer_java("25.0.5")
        self.path_java("21.0.5")
        completed = self.start()
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual(1, len(self.calls()), self.calls())
        self.assertTrue(self.calls()[0].startswith("run installer "), self.calls())

    def test_installers_java_is_taken_even_when_the_path_has_a_25_too(self):
        self.installer_java("25.0.5")
        self.path_java("25.0.1")
        self.start()
        self.assertTrue(self.calls()[0].startswith("run installer "), self.calls())

    def test_without_an_installer_home_and_java_21_on_the_path_it_says_why_and_fails(self):
        self.path_java("21.0.5")
        self.notifiers("zenity", "notify-send")
        completed = self.start()
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("Java 25", completed.stderr)
        self.assertIn("21", completed.stderr)
        self.assertEqual([], [call for call in self.calls() if call.startswith("run ")])
        shown = [call for call in self.calls() if call.startswith(("zenity", "notify-send"))]
        self.assertEqual(1, len(shown), self.calls())
        self.assertIn("Java 25", shown[0])

    def test_the_message_reaches_notify_send_when_there_is_no_zenity(self):
        self.path_java("21.0.5")
        self.notifiers("notify-send")
        completed = self.start()
        self.assertNotEqual(0, completed.returncode)
        self.assertTrue(self.calls()[0].startswith("notify-send "), self.calls())
        self.assertIn("Java 25", self.calls()[0])

    def test_the_message_is_on_standard_error_when_there_is_no_notifier_at_all(self):
        completed = self.start()
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("Java 25", completed.stderr)
        self.assertEqual([], self.calls())

    def test_the_path_java_25_is_used_when_the_installers_java_is_gone(self):
        self.installer_java("25.0.5")
        shutil.rmtree(self.work / "installer-jdk")
        self.path_java("25.0.1")
        completed = self.start()
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertTrue(self.calls()[0].startswith("run path "), self.calls())

    def test_the_path_java_25_is_used_when_the_installers_java_is_too_old(self):
        self.installer_java("21.0.5")
        self.path_java("26")
        self.start()
        self.assertTrue(self.calls()[0].startswith("run path "), self.calls())

    def test_an_unexpanded_installer_placeholder_is_ignored(self):
        (self.install / "installer-java-home").write_text("${JAVA_HOME}\n")
        self.path_java("25.0.1")
        self.start()
        self.assertTrue(self.calls()[0].startswith("run path "), self.calls())

    def test_the_flags_and_the_jar_reach_java_and_the_arguments_follow(self):
        self.installer_java("25.0.5")
        self.start("--extra", "value")
        expected = f"run installer {ADD_OPENS} {UNSAFE_FLAG} -jar {self.install}/mystic-crypt-ui.jar" \
                   " --extra value"
        self.assertEqual([expected], self.calls())

    def test_the_exit_code_of_the_application_is_the_scripts(self):
        self.installer_java("25.0.5", exit_code=3)
        self.assertEqual(3, self.start().returncode)

    def test_the_script_does_not_depend_on_the_working_directory(self):
        self.installer_java("25.0.5")
        elsewhere = self.work / "elsewhere"
        elsewhere.mkdir()
        environment = {"PATH": str(self.bin), "HOME": str(self.work), "STUB_LOG": str(self.log)}
        subprocess.run([str(self.install / "start.sh")], env=environment, cwd=elsewhere, check=True)
        self.assertIn(f"-jar {self.install}/mystic-crypt-ui.jar", self.calls()[0])


class LauncherFilesTest(unittest.TestCase):
    def test_the_desktop_entry_runs_the_shipped_start_script_and_carries_no_flags(self):
        exec_lines = [line for line in DESKTOP_ENTRY.read_text().splitlines()
                      if line.startswith("Exec=")]
        self.assertEqual(1, len(exec_lines), exec_lines)
        self.assertIn("${INSTALL_PATH}/start.sh", exec_lines[0])
        self.assertNotIn("java", exec_lines[0])
        self.assertNotIn("--", exec_lines[0])

    def test_start_bat_carries_the_same_flags_before_the_jar(self):
        command = [line for line in START_BAT.read_text().splitlines() if "javaw" in line]
        self.assertEqual(1, len(command), command)
        self.assertIn(ADD_OPENS, command[0])
        self.assertIn(UNSAFE_FLAG, command[0])
        self.assertLess(command[0].index(UNSAFE_FLAG), command[0].index("-jar"))
        self.assertLess(command[0].index(ADD_OPENS), command[0].index("-jar"))

    def test_the_installer_java_home_file_holds_only_the_installers_variable(self):
        self.assertEqual("${JAVA_HOME}", INSTALLER_JAVA_HOME.read_text().strip())


if __name__ == "__main__":
    unittest.main()
