#!/usr/bin/env python3
"""Tests for mystic-crypt and mystic-crypt.bat, the command-line starters of the "commandline" pack.

The starter is what a terminal user types: mystic-crypt hash --password-stdin, from any directory.
Every case here runs the real script in a directory laid out like an installation, with a PATH
that holds only stubs: a java that prints a version on standard error, as java -version does, and
records its arguments one by one and its standard input; a notify-send and a zenity that record
their calls.

Run: python3 scripts/cli_launcher_test.py
"""

import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ElementTree
from pathlib import Path

from start_launcher_test import ADD_OPENS, DOC, INSTALL_XML, UNSAFE_FLAG, write_executable

MYSTIC_CRYPT_BAT = DOC / "mystic-crypt.bat"
CLI_CLASS = "io.github.astrapi69.mystic.crypt.cli.MysticCryptCli"

JAVA_STUB = """#!/bin/bash
if [ "$1" = "-version" ]; then
  echo 'openjdk version "{version}" 2025-01-01' >&2
  echo 'OpenJDK Runtime Environment (stub)' >&2
  exit 0
fi
{{ printf 'run {name}'; printf ' [%s]' "$@"; echo; }} >> "$STUB_LOG"
if [ -n "${{STUB_STDIN:-}}" ]; then
  cat > "$STUB_STDIN"
fi
exit {exit_code}
"""

NOTIFIER_STUB = """#!/bin/bash
echo "{name} $*" >> "$STUB_LOG"
"""

# the programs a starter and the stubs call besides java, linked one by one so that the PATH of a
# case holds no real java: a runner's /usr/bin may have one
TOOLS = ("dirname", "readlink", "cat")


def lay_out_installation(directory):
    """Copies into the given directory what install.xml writes into ${INSTALL_PATH} from
    src/main/resources/doc, so that a case runs the starter beside exactly the files an
    installation gives it. A source named with an @{...} property of the IzPack compiler is left
    out: it is the desktop entry, which no starter reads."""
    for element in ElementTree.parse(INSTALL_XML).getroot().iter("file"):
        source = element.get("src")
        if element.get("targetdir") != "${INSTALL_PATH}" or "@{" in source \
                or not source.startswith("resources/main/doc/"):
            continue
        name = source.rsplit("/", 1)[-1]
        shutil.copy(DOC / name, directory / name)
    (directory / "mystic-crypt").chmod(0o755)


class CliLauncherTest(unittest.TestCase):
    def setUp(self):
        self.work = Path(tempfile.mkdtemp(prefix="cli-launcher-"))
        self.addCleanup(shutil.rmtree, self.work, ignore_errors=True)
        self.install = self.work / "install dir"
        self.bin = self.work / "bin"
        self.tools = self.work / "tools"
        self.elsewhere = self.work / "elsewhere"
        self.log = self.work / "calls.log"
        for directory in (self.install, self.bin, self.tools, self.elsewhere):
            directory.mkdir()
        for tool in TOOLS:
            (self.tools / tool).symlink_to(shutil.which(tool))
        lay_out_installation(self.install)

    def stub_java(self, directory, name, version, exit_code=0):
        write_executable(directory / "java", JAVA_STUB.format(
            version=version, name=name, exit_code=exit_code))

    def installer_java(self, version, exit_code=0):
        home = self.work / "installer-jdk"
        self.stub_java(home / "bin", "installer", version, exit_code)
        (self.install / "installer-java-home").write_text(f"{home}\n")

    def path_java(self, version, exit_code=0):
        self.stub_java(self.bin, "path", version, exit_code)

    def notifiers(self):
        for name in ("zenity", "notify-send"):
            write_executable(self.bin / name, NOTIFIER_STUB.format(name=name))

    def link(self):
        """A symlink to the starter in a directory of its own, the way ~/.local/bin holds one"""
        link_directory = self.work / "link bin"
        link_directory.mkdir()
        (link_directory / "mystic-crypt").symlink_to(self.install / "mystic-crypt")
        return link_directory

    def run_starter(self, *arguments, command=None, path=None, standard_input=None,
                    stdin_file=None):
        environment = {"PATH": path or f"{self.bin}:{self.tools}", "HOME": str(self.work),
                       "STUB_LOG": str(self.log)}
        if stdin_file:
            environment["STUB_STDIN"] = str(stdin_file)
        return subprocess.run([command or str(self.install / "mystic-crypt"), *arguments],
                              env=environment, cwd=self.elsewhere, input=standard_input,
                              capture_output=True, text=True, timeout=60)

    def calls(self):
        return self.log.read_text().splitlines() if self.log.exists() else []

    def runs(self):
        return [call for call in self.calls() if call.startswith("run ")]

    def dialogs(self):
        return [call for call in self.calls() if call.startswith(("zenity", "notify-send"))]

    def test_an_older_java_first_on_the_path_is_run_without_a_check(self):
        self.path_java("21.0.5")
        self.notifiers()
        completed = self.run_starter("--help")
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual(1, len(self.runs()), self.calls())
        self.assertTrue(self.runs()[0].startswith("run path "), self.calls())
        self.assertEqual([], self.dialogs())

    def test_without_any_java_the_shell_reports_a_missing_command(self):
        self.notifiers()
        completed = self.run_starter("--help")
        self.assertEqual(127, completed.returncode, completed.stderr)
        self.assertIn("java", completed.stderr)
        self.assertNotIn("Java 25", completed.stderr)
        self.assertEqual([], self.dialogs())

    def test_the_installers_java_is_not_looked_at(self):
        self.installer_java("25.0.5")
        self.path_java("25.0.1")
        self.run_starter("--help")
        self.assertTrue(self.runs()[0].startswith("run path "), self.calls())

    def test_through_a_symlink_from_another_directory_the_jar_is_looked_for_beside_the_link(self):
        self.path_java("25.0.1")
        link_directory = self.link()
        self.run_starter("--help", command="mystic-crypt",
                         path=f"{link_directory}:{self.bin}:{self.tools}")
        self.assertEqual(
            [f"run path [-cp] [{link_directory}/mystic-crypt-ui.jar] [{CLI_CLASS}] [--help]"],
            self.runs())

    def test_no_jvm_flags_reach_java(self):
        self.path_java("25.0.1")
        self.run_starter("--help")
        self.assertEqual(
            [f"run path [-cp] [{self.install}/mystic-crypt-ui.jar] [{CLI_CLASS}] [--help]"],
            self.runs())

    def test_arguments_with_spaces_reach_the_command_unchanged(self):
        self.path_java("25.0.1")
        self.run_starter("checksum", "--file", "a file with spaces.txt", "", "--x=a b")
        self.assertTrue(
            self.runs()[0].endswith(" [checksum] [--file] [a file with spaces.txt] [] [--x=a b]"),
            self.runs())

    def test_standard_input_reaches_the_command(self):
        self.path_java("25.0.1")
        received = self.work / "stdin.txt"
        self.run_starter("hash", "--password-stdin", standard_input="a line\nanother\n",
                         stdin_file=received)
        self.assertEqual("a line\nanother\n", received.read_text())

    def test_the_exit_code_of_the_command_is_the_starters(self):
        self.path_java("25.0.1", exit_code=3)
        self.assertEqual(3, self.run_starter("verify-signature").returncode)


class CliLauncherFilesTest(unittest.TestCase):
    def test_the_windows_starter_runs_java_without_the_flags_of_start_bat(self):
        command = [line for line in MYSTIC_CRYPT_BAT.read_text().splitlines()
                   if line.lstrip().startswith("java ")]
        self.assertEqual(1, len(command), command)
        self.assertNotIn(ADD_OPENS, command[0])
        self.assertNotIn(UNSAFE_FLAG, command[0])


if __name__ == "__main__":
    unittest.main()
