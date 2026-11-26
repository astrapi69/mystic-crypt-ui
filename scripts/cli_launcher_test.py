#!/usr/bin/env python3
"""Tests for mystic-crypt and mystic-crypt.bat, the command-line starters of the "commandline" pack.

The starter is what a terminal user types: mystic-crypt hash --password-stdin, from any directory.
Every case here runs the real script in a directory laid out like an installation, with a PATH
that holds only stubs: a java that prints a version on standard error, as java -version does, and
records its arguments one by one and its standard input; a notify-send and a zenity that record
their calls.

Run: python3 scripts/cli_launcher_test.py
"""

import os
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
echo "options {name} ${{JDK_JAVA_OPTIONS-unset}}" >> "$STUB_LOG"
{{ printf 'run {name}'; printf ' [%s]' "$@"; echo; }} >> "$STUB_LOG"
if [ -n "${{STUB_STDIN:-}}" ]; then
  cat > "$STUB_STDIN"
fi
exit {exit_code}
"""

NOTIFIER_STUB = """#!/bin/bash
echo "{name} $*" >> "$STUB_LOG"
"""

# the readlink of a system without -f (macOS before 12.3 and BSD): -f is refused as there, every other
# call goes to the real one
READLINK_WITHOUT_F = """#!/bin/bash
if [ "$1" = "-f" ]; then
  echo "readlink: illegal option -- f" >&2
  echo "usage: readlink [-n] [file ...]" >&2
  exit 1
fi
exec {real} "$@"
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
        # resolved: on macOS the temporary directory is below /var, which is a link to /private/var, and the
        # starter reports the directory it resolved
        self.work = Path(tempfile.mkdtemp(prefix="cli-launcher-")).resolve()
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

    def readlink_without_f(self):
        link = self.tools / "readlink"
        real = link.resolve()
        link.unlink()
        write_executable(link, READLINK_WITHOUT_F.format(real=real))

    def link(self):
        """A symlink to the starter in a directory of its own, the way ~/.local/bin holds one"""
        link_directory = self.work / "link bin"
        link_directory.mkdir()
        (link_directory / "mystic-crypt").symlink_to(self.install / "mystic-crypt")
        return link_directory

    def run_starter(self, *arguments, command=None, path=None, standard_input=None,
                    stdin_file=None, extra_environment=None):
        environment = {"PATH": path or f"{self.bin}:{self.tools}", "HOME": str(self.work),
                       "STUB_LOG": str(self.log)}
        environment.update(extra_environment or {})
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

    def flags(self):
        """The JVM flags of find-java.sh as the stub records them, one bracket per argument"""
        return " ".join(f"[{argument}]" for argument in f"{ADD_OPENS} {UNSAFE_FLAG}".split())

    def expected_run(self, java, *arguments):
        return (f"run {java} {self.flags()} [-jar] [{self.install}/mystic-crypt-ui.jar] [--cli] "
                + " ".join(f"[{argument}]" for argument in arguments))

    def test_an_older_java_first_on_the_path_says_why_on_standard_error_and_fails(self):
        self.path_java("21.0.5")
        self.notifiers()
        completed = self.run_starter("--help")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("Java 25", completed.stderr)
        self.assertIn(f"{self.bin}/java: Java 21", completed.stderr)
        self.assertEqual([], self.runs())

    def test_without_any_java_it_says_why_on_standard_error_and_fails(self):
        (self.install / "installer-java-home").unlink()
        self.notifiers()
        completed = self.run_starter("--help")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("Java 25", completed.stderr)
        self.assertIn("no java found", completed.stderr)

    def test_a_failure_on_the_command_line_opens_no_dialog(self):
        self.path_java("21.0.5")
        self.notifiers()
        self.run_starter("--help")
        self.assertEqual([], self.dialogs())

    def test_the_installers_java_wins_over_the_path(self):
        self.installer_java("25.0.5")
        self.path_java("25.0.1")
        self.run_starter("--help")
        self.assertEqual([self.expected_run("installer", "--help")], self.runs())

    def test_the_path_java_is_used_when_the_installers_is_too_old(self):
        self.installer_java("21.0.5")
        self.path_java("26")
        self.run_starter("--help")
        self.assertEqual([self.expected_run("path", "--help")], self.runs())

    def test_through_a_symlink_from_another_directory_the_installation_is_found(self):
        self.installer_java("25.0.5")
        self.path_java("25.0.1")
        link_directory = self.link()
        completed = self.run_starter("--help", command="mystic-crypt",
                                     path=f"{link_directory}:{self.bin}:{self.tools}")
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual([self.expected_run("installer", "--help")], self.runs())

    def test_through_a_symlink_without_readlink_f_the_installation_is_found(self):
        self.installer_java("25.0.5")
        self.readlink_without_f()
        link_directory = self.link()
        completed = self.run_starter("--help", command="mystic-crypt",
                                     path=f"{link_directory}:{self.bin}:{self.tools}")
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual([self.expected_run("installer", "--help")], self.runs())

    def test_a_chain_of_relative_links_without_readlink_f_is_followed_to_the_starter(self):
        self.installer_java("25.0.5")
        self.readlink_without_f()
        (self.work / "first").mkdir()
        (self.work / "first" / "mystic-crypt").symlink_to("../second/mystic-crypt")
        (self.work / "second").mkdir()
        (self.work / "second" / "mystic-crypt").symlink_to("../install dir/mystic-crypt")
        completed = self.run_starter("--help", command="mystic-crypt",
                                     path=f"{self.work / 'first'}:{self.bin}:{self.tools}")
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual([self.expected_run("installer", "--help")], self.runs())

    def test_the_jvm_flags_reach_java_before_the_jar(self):
        self.path_java("25.0.1")
        self.run_starter("--help")
        self.assertEqual([self.expected_run("path", "--help")], self.runs())

    def test_without_its_search_file_the_starter_names_it_and_fails(self):
        (self.install / "find-java.sh").unlink()
        self.path_java("25.0.1")
        completed = self.run_starter("--help")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("find-java.sh is missing", completed.stderr)
        self.assertEqual([], self.runs())

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

    def options(self):
        return [call for call in self.calls() if call.startswith("options ")]

    def test_jdk_java_options_reach_java_unchanged_through_the_starter(self):
        self.path_java("25.0.1")
        given = "-Dmystic.crypt.ui.plugins.dir=/a dir/with space -Dother=\"x y\""
        self.run_starter("--help", extra_environment={"JDK_JAVA_OPTIONS": given})
        self.assertEqual([f"options path {given}"], self.options())

    def test_without_jdk_java_options_the_starter_sets_none(self):
        self.path_java("25.0.1")
        self.run_starter("--help")
        self.assertEqual(["options path unset"], self.options())

    def test_the_exit_code_of_the_command_is_the_starters(self):
        self.path_java("25.0.1", exit_code=3)
        self.assertEqual(3, self.run_starter("verify-signature").returncode)


class CliLauncherFilesTest(unittest.TestCase):
    def test_the_windows_starter_carries_the_flags_of_start_bat_before_the_jar(self):
        command = [line for line in MYSTIC_CRYPT_BAT.read_text().splitlines()
                   if line.lstrip().startswith("java ")]
        self.assertEqual(1, len(command), command)
        self.assertIn(ADD_OPENS, command[0])
        self.assertIn(UNSAFE_FLAG, command[0])
        self.assertLess(command[0].index(ADD_OPENS), command[0].index("-jar"))
        self.assertLess(command[0].index(UNSAFE_FLAG), command[0].index("-jar"))

    def test_the_windows_starter_takes_the_command_line_path_of_the_application(self):
        command = [line for line in MYSTIC_CRYPT_BAT.read_text().splitlines()
                   if line.lstrip().startswith("java ")]
        self.assertIn('-jar "%~dp0mystic-crypt-ui.jar" --cli %*', command[0])
        self.assertNotIn(CLI_CLASS, command[0])

    def test_the_windows_starter_ends_with_the_exit_code_of_the_command(self):
        lines = [line.strip() for line in MYSTIC_CRYPT_BAT.read_text().splitlines() if line.strip()]
        self.assertEqual("EXIT /B %ERRORLEVEL%", lines[-1])

    def test_the_java_search_is_written_once_and_both_starters_source_it(self):
        for name in ("start.sh", "mystic-crypt"):
            with self.subTest(starter=name):
                text = (DOC / name).read_text()
                self.assertIn('. "$DIR/find-java.sh"', text)
                self.assertNotIn("MINIMUM_JAVA=", text)
                self.assertNotIn("java_major()", text)
                self.assertNotIn("--add-opens", text)
        search = (DOC / "find-java.sh").read_text()
        self.assertIn("MINIMUM_JAVA=25", search)
        self.assertIn(ADD_OPENS, search)

    def test_the_required_pack_installs_the_search_beside_the_starters(self):
        root = ElementTree.parse(INSTALL_XML).getroot()
        pack = next(pack for pack in root.iter("pack") if pack.get("name") == "executables")
        self.assertEqual("yes", pack.get("required"))
        sources = [(element.get("src"), element.get("targetdir")) for element in pack.iter("file")]
        self.assertIn(("resources/main/doc/find-java.sh", "${INSTALL_PATH}"), sources)


LINK_SCRIPT = DOC / "mystic-crypt-link.sh"


class LinkScriptTest(unittest.TestCase):
    """mystic-crypt-link.sh, which the installer runs to put the starter on the PATH and the
    uninstaller to take it off again"""

    def setUp(self):
        self.work = Path(tempfile.mkdtemp(prefix="cli-link-"))
        self.addCleanup(shutil.rmtree, self.work, ignore_errors=True)
        self.install = self.work / "install dir"
        self.install.mkdir()
        lay_out_installation(self.install)
        self.starter = self.install / "mystic-crypt"
        self.link = self.work / "home" / ".local" / "bin" / "mystic-crypt"

    def run_script(self, *arguments):
        script = self.install / "mystic-crypt-link.sh"
        script.chmod(0o755)
        return subprocess.run([str(script), *arguments], capture_output=True, text=True,
                              timeout=60)

    def link_it(self):
        return self.run_script("link", str(self.starter), str(self.link))

    def unlink_it(self):
        return self.run_script("unlink", str(self.starter), str(self.link))

    def other_installation(self):
        other = self.work / "other install"
        other.mkdir()
        (other / "mystic-crypt").write_text("#!/bin/bash\n")
        (other / "mystic-crypt-ui.jar").write_bytes(b"PK")
        return other / "mystic-crypt"

    def test_link_creates_the_directory_and_the_link_to_the_starter(self):
        completed = self.link_it()
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertTrue(self.link.is_symlink())
        self.assertEqual(str(self.starter), os.readlink(self.link))

    def test_link_a_second_time_keeps_the_link(self):
        self.link_it()
        completed = self.link_it()
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertEqual(str(self.starter), os.readlink(self.link))

    def test_link_replaces_a_link_whose_target_is_gone_or_another_installation(self):
        cases = {"a target that is gone": self.work / "removed install" / "mystic-crypt",
                 "another installation": self.other_installation()}
        for name, target in cases.items():
            with self.subTest(name):
                self.link.parent.mkdir(parents=True, exist_ok=True)
                self.link.unlink(missing_ok=True)
                self.link.symlink_to(target)
                completed = self.link_it()
                self.assertEqual(0, completed.returncode, completed.stderr)
                self.assertEqual(str(self.starter), os.readlink(self.link))

    def test_link_leaves_a_foreign_file_of_the_same_name_alone(self):
        foreign_target = self.work / "elsewhere" / "mystic-crypt"
        foreign_target.parent.mkdir()
        foreign_target.write_text("#!/bin/bash\necho a tool of its own\n")
        cases = {"a file": lambda: self.link.write_text("#!/bin/bash\necho a tool of its own\n"),
                 "a link to another program": lambda: self.link.symlink_to(foreign_target)}
        for name, make in cases.items():
            with self.subTest(name):
                self.link.parent.mkdir(parents=True, exist_ok=True)
                self.link.unlink(missing_ok=True)
                make()
                before = (self.link.is_symlink(), self.link.read_text())
                completed = self.link_it()
                self.assertEqual(0, completed.returncode, completed.stderr)
                self.assertIn("left as it is", completed.stderr)
                self.assertEqual(before, (self.link.is_symlink(), self.link.read_text()))

    def test_unlink_removes_the_link_to_this_starter(self):
        self.link_it()
        completed = self.unlink_it()
        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertFalse(self.link.is_symlink())
        self.assertFalse(self.link.exists())

    def test_unlink_leaves_what_is_not_its_link_alone_and_succeeds(self):
        self.link.parent.mkdir(parents=True)
        cases = {"a file": lambda: self.link.write_text("a tool of its own\n"),
                 "a link to another installation": lambda: self.link.symlink_to(
                     self.other_installation())}
        for name, make in cases.items():
            with self.subTest(name):
                self.link.unlink(missing_ok=True)
                make()
                completed = self.unlink_it()
                self.assertEqual(0, completed.returncode, completed.stderr)
                self.assertTrue(self.link.is_symlink() or self.link.is_file())

    def test_unlink_without_a_link_succeeds(self):
        self.assertEqual(0, self.unlink_it().returncode)

    def test_wrong_arguments_exit_2(self):
        for arguments in ((), ("link",), ("link", str(self.starter)),
                          ("move", str(self.starter), str(self.link))):
            with self.subTest(arguments=arguments):
                self.assertEqual(2, self.run_script(*arguments).returncode)


class InstallerLinkTest(unittest.TestCase):
    """The commandline pack of install.xml runs mystic-crypt-link.sh after the installation and
    before the uninstallation, on Linux and macOS"""

    def setUp(self):
        root = ElementTree.parse(INSTALL_XML).getroot()
        self.pack = next(pack for pack in root.iter("pack") if pack.get("name") == "commandline")
        self.executables = [element for element in self.pack.iter("executable")
                            if element.get("targetfile") == "${INSTALL_PATH}/mystic-crypt-link.sh"]

    def stage(self, name):
        found = [element for element in self.executables if element.get("stage") == name]
        self.assertEqual(1, len(found), [element.attrib for element in self.executables])
        return found[0]

    def arguments(self, element):
        return [argument.get("value") for argument in element.iter("arg")]

    def families(self, element):
        return sorted(os_element.get("family") for os_element in element.iter("os"))

    def test_the_pack_installs_the_link_script(self):
        sources = [(element.get("src"), element.get("targetdir")) for element in self.pack.iter("file")]
        self.assertIn(("resources/main/doc/mystic-crypt-link.sh", "${INSTALL_PATH}"), sources)

    def test_the_installation_links_the_starter_into_the_users_local_bin(self):
        executable = self.stage("postinstall")
        self.assertEqual(["link", "${INSTALL_PATH}/mystic-crypt", "${USER_HOME}/.local/bin/mystic-crypt"],
                         self.arguments(executable))
        self.assertEqual("true", executable.get("keep"))
        self.assertEqual(["mac", "unix"], self.families(executable))

    def test_the_uninstallation_removes_the_link_and_never_stops_on_it(self):
        executable = self.stage("uninstall")
        self.assertEqual(["unlink", "${INSTALL_PATH}/mystic-crypt", "${USER_HOME}/.local/bin/mystic-crypt"],
                         self.arguments(executable))
        self.assertEqual("ignore", executable.get("failure"))
        self.assertEqual(["mac", "unix"], self.families(executable))


if __name__ == "__main__":
    unittest.main()
