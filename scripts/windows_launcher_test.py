#!/usr/bin/env python3
"""Tests for start.bat and mystic-crypt.bat, the Windows starters, and find-java.bat, the Java search
both call (#1195).

On Linux and macOS start.sh and mystic-crypt take their Java from find-java.sh: the Java the
installer ran on, then the PATH's, 25 or newer. The batch files used to run whatever java came first
on the PATH. A batch file cannot run on the Linux gate, so these cases run on a Windows runner
(.github/workflows/windows-launchers.yml): each runs the real starter in a directory laid out like an
installation, with a PATH that holds only stubs. A stub is a small real executable, compiled with the
csc.exe every Windows carries: java.exe and javaw.exe that print a version on standard error as
java -version does, and a powershell.exe that records the dialog start.bat asks for. The cases that
need Windows are skipped elsewhere; the file checks run everywhere.

Run (on Windows): python scripts/windows_launcher_test.py
"""

import os
import re
import shutil
import subprocess
import tempfile
import time
import unittest
import xml.etree.ElementTree as ElementTree
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOC = ROOT / "src" / "main" / "resources" / "doc"
FIND_JAVA_SH = DOC / "find-java.sh"
INSTALL_XML = ROOT / "src" / "main" / "izpack" / "install.xml"
LAUNCHER_FILES = ("start.bat", "mystic-crypt.bat", "find-java.bat")

JVM_FLAGS = ["--add-opens", "java.base/java.time=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"]

ON_WINDOWS = os.name == "nt"

STUB_SOURCE = r"""
using System;
using System.IO;
using System.Reflection;
using System.Text;

class Stub
{
    static int Main(string[] args)
    {
        string exe = Assembly.GetExecutingAssembly().Location;
        string dir = Path.GetDirectoryName(exe);
        string name = Path.GetFileNameWithoutExtension(exe);
        string label = "stub";
        string version = "25.0.1";
        string stdinFile = "";
        int exitCode = 0;
        string config = Path.Combine(dir, "stub.txt");
        if (File.Exists(config))
        {
            foreach (string line in File.ReadAllLines(config))
            {
                int eq = line.IndexOf('=');
                if (eq < 0) continue;
                string key = line.Substring(0, eq);
                string value = line.Substring(eq + 1);
                if (key == "label") label = value;
                else if (key == "version") version = value;
                else if (key == "exit") exitCode = int.Parse(value);
                else if (key == "stdin") stdinFile = value;
            }
        }
        if (args.Length > 0 && args[0] == "-version")
        {
            Console.Error.WriteLine("Picked up JAVA_TOOL_OPTIONS: -Dstub=true");
            Console.Error.WriteLine("openjdk version \"" + version + "\" 2025-01-01");
            Console.Error.WriteLine("OpenJDK Runtime Environment (stub)");
            return 0;
        }
        if (stdinFile.Length > 0)
        {
            using (Stream input = Console.OpenStandardInput())
            using (FileStream output = File.Create(stdinFile))
            {
                input.CopyTo(output);
            }
        }
        StringBuilder call = new StringBuilder("run " + label + " " + name);
        foreach (string arg in args) call.Append(" [" + arg + "]");
        string message = Environment.GetEnvironmentVariable("MYSTIC_CRYPT_MESSAGE");
        if (message != null) call.Append(" message=" + message);
        File.AppendAllText(Environment.GetEnvironmentVariable("STUB_LOG"), call + "\n");
        return exitCode;
    }
}
"""

STUB_EXE = None
STUB_BUILD = None


def setUpModule():
    global STUB_EXE, STUB_BUILD
    if not ON_WINDOWS:
        return
    STUB_BUILD = Path(tempfile.mkdtemp(prefix="win-launcher-stub-"))
    source = STUB_BUILD / "stub.cs"
    source.write_text(STUB_SOURCE)
    compiler = Path(os.environ["WINDIR"]) / "Microsoft.NET" / "Framework64" / "v4.0.30319" / "csc.exe"
    STUB_EXE = STUB_BUILD / "stub.exe"
    subprocess.run([str(compiler), "/nologo", f"/out:{STUB_EXE}", str(source)], check=True)


def tearDownModule():
    if STUB_BUILD is not None:
        shutil.rmtree(STUB_BUILD, ignore_errors=True)


def find_java_flags():
    """The flags find-java.sh passes, the ones the batch files have to carry too."""
    match = re.search(r"JVM_FLAGS=\((.*?)\)", FIND_JAVA_SH.read_text())
    return match.group(1).split()


@unittest.skipUnless(ON_WINDOWS, "runs the batch files, which needs Windows")
class WindowsLauncherCase(unittest.TestCase):
    def setUp(self):
        self.work = Path(tempfile.mkdtemp(prefix="win-launcher-"))
        self.addCleanup(shutil.rmtree, self.work, ignore_errors=True)
        # a name with a space and a parenthesis, as "C:\Program Files (x86)\..." has
        self.install = self.work / "install (x86) dir"
        self.bin = self.work / "bin dir"
        self.tools = self.work / "tools"
        self.log = self.work / "calls.log"
        for directory in (self.install, self.bin, self.tools):
            directory.mkdir()
        for name in LAUNCHER_FILES:
            if (DOC / name).exists():
                shutil.copy(DOC / name, self.install / name)

    def stub(self, directory, executables, label, version="25.0.1", exit_code=0, stdin_file=None):
        directory.mkdir(parents=True, exist_ok=True)
        for executable in executables:
            shutil.copy(STUB_EXE, directory / executable)
        lines = [f"label={label}", f"version={version}", f"exit={exit_code}"]
        if stdin_file is not None:
            lines.append(f"stdin={stdin_file}")
        (directory / "stub.txt").write_text("\n".join(lines) + "\n")

    def path_java(self, version, exit_code=0, stdin_file=None):
        self.stub(self.bin, ("java.exe", "javaw.exe"), "path", version, exit_code, stdin_file)

    def installer_java(self, version, exit_code=0, stdin_file=None, line_end="\r\n"):
        home = self.work / "installer jdk (x64)"
        self.stub(home / "bin", ("java.exe", "javaw.exe"), "installer", version, exit_code, stdin_file)
        (self.install / "installer-java-home").write_bytes(f"{home}{line_end}".encode())

    def dialog(self):
        self.stub(self.tools, ("powershell.exe",), "dialog")

    def environment(self):
        system_root = os.environ["SystemRoot"]
        return {"PATH": f"{self.bin};{self.tools};{system_root}\\System32", "SystemRoot": system_root,
                "ComSpec": os.environ["ComSpec"], "PATHEXT": ".COM;.EXE;.BAT;.CMD",
                "TEMP": str(self.work), "TMP": str(self.work), "STUB_LOG": str(self.log)}

    def run_starter(self, name, *arguments, standard_input=b"", cwd=None):
        completed = subprocess.run([str(self.install / name), *arguments], env=self.environment(),
                                   input=standard_input, capture_output=True, timeout=120,
                                   cwd=cwd or self.work)
        completed.out = completed.stdout.decode("utf-8", "replace")
        completed.err = completed.stderr.decode("utf-8", "replace")
        return completed

    def calls(self):
        return self.log.read_text(encoding="utf-8").splitlines() if self.log.exists() else []

    def calls_after_start(self, count=1):
        """start.bat does not wait for the application, so its stub writes its line a moment later."""
        deadline = time.time() + 30
        while len(self.calls()) < count and time.time() < deadline:
            time.sleep(0.25)
        return self.calls()

    def expected_flags(self):
        return " ".join(f"[{flag}]" for flag in JVM_FLAGS)


class PinnedBehaviourTest(WindowsLauncherCase):
    """What the starters do with the Java they run, which stays as it is."""

    def test_mystic_crypt_bat_passes_the_flags_the_jar_and_every_argument_to_java(self):
        self.path_java("25.0.1")
        completed = self.run_starter("mystic-crypt.bat", "hash", "--password-stdin", "two words")
        self.assertEqual(0, completed.returncode, completed.err)
        self.assertEqual(
            [f"run path java {self.expected_flags()} [-jar] [{self.install}\\mystic-crypt-ui.jar] [--cli]"
             " [hash] [--password-stdin] [two words]"], self.calls())

    def test_mystic_crypt_bat_passes_standard_input_through(self):
        received = self.work / "received.bin"
        self.path_java("25.0.1", stdin_file=received)
        self.run_starter("mystic-crypt.bat", "hash", standard_input=b"a line\r\nanother\r\n")
        self.assertEqual(b"a line\r\nanother\r\n", received.read_bytes())

    def test_mystic_crypt_bat_ends_with_the_exit_code_of_the_command(self):
        self.path_java("25.0.1", exit_code=3)
        self.assertEqual(3, self.run_starter("mystic-crypt.bat", "verify-signature").returncode)

    def test_mystic_crypt_bat_does_not_depend_on_the_working_directory(self):
        self.path_java("25.0.1")
        elsewhere = self.work / "elsewhere"
        elsewhere.mkdir()
        self.run_starter("mystic-crypt.bat", "hash", cwd=elsewhere)
        self.assertIn(f"[{self.install}\\mystic-crypt-ui.jar]", self.calls()[0])

    def test_start_bat_starts_the_application_with_the_flags_and_the_jar(self):
        self.path_java("25.0.1")
        completed = self.run_starter("start.bat")
        self.assertEqual(0, completed.returncode, completed.err)
        self.assertEqual(
            [f"run path javaw {self.expected_flags()} [-jar] [{self.install}\\mystic-crypt-ui.jar]"],
            self.calls_after_start())

    def test_start_bat_returns_at_once_whatever_the_application_exits_with(self):
        self.path_java("25.0.1", exit_code=3)
        self.assertEqual(0, self.run_starter("start.bat").returncode)
        self.calls_after_start()


class LauncherFilesTest(unittest.TestCase):
    """Checks over the files, which need no Windows."""

    def test_every_batch_file_has_windows_line_endings(self):
        for path in sorted(DOC.glob("*.bat")):
            with self.subTest(file=path.name):
                content = path.read_bytes()
                self.assertGreater(content.count(b"\r\n"), 0)
                self.assertEqual(content.count(b"\n"), content.count(b"\r\n"))

    def test_both_starters_carry_the_flags_of_find_java_sh_before_the_jar(self):
        flags = " ".join(find_java_flags())
        self.assertEqual(JVM_FLAGS, find_java_flags())
        for name, marker in (("start.bat", "javaw"), ("mystic-crypt.bat", "java ")):
            with self.subTest(starter=name):
                command = [line for line in (DOC / name).read_text().splitlines()
                           if line.lstrip().lower().startswith(("start ", "java ")) and marker in line]
                self.assertEqual(1, len(command), command)
                self.assertIn(flags, command[0])
                self.assertLess(command[0].index(flags), command[0].index("-jar"))

    def test_the_search_is_written_once_and_both_starters_call_it(self):
        search = (DOC / "find-java.bat").read_text()
        self.assertIn('SET "MINIMUM_JAVA=25"', search)
        self.assertIn("MINIMUM_JAVA=25", FIND_JAVA_SH.read_text())
        for name in ("start.bat", "mystic-crypt.bat"):
            with self.subTest(starter=name):
                text = (DOC / name).read_text()
                self.assertIn('CALL "%~dp0find-java.bat"', text)
                self.assertNotIn("MINIMUM_JAVA", text)
                self.assertNotIn("-version", text)

    def test_the_required_pack_installs_the_search_beside_the_starters(self):
        root = ElementTree.parse(INSTALL_XML).getroot()
        pack = next(pack for pack in root.iter("pack") if pack.get("name") == "executables")
        self.assertEqual("yes", pack.get("required"))
        sources = [(element.get("src"), element.get("targetdir")) for element in pack.iter("file")]
        self.assertIn(("resources/main/doc/find-java.bat", "${INSTALL_PATH}"), sources)


class JavaSearchTest(WindowsLauncherCase):
    """The order and the minimum of find-java.sh, for both starters."""

    STARTERS = ("start.bat", "mystic-crypt.bat")

    def started_with(self, starter):
        """The first run the starter made, after waiting for it where the starter does not wait."""
        calls = self.calls_after_start() if starter == "start.bat" else self.calls()
        return calls[0] if calls else ""

    def test_the_installers_java_wins_over_the_paths_java_older_or_not(self):
        for starter in self.STARTERS:
            for path_version in ("21.0.5", "25.0.1"):
                with self.subTest(starter=starter, path_java=path_version):
                    self.log.unlink(missing_ok=True)
                    self.installer_java("25.0.5")
                    self.path_java(path_version)
                    completed = self.run_starter(starter)
                    self.assertEqual(0, completed.returncode, completed.err)
                    self.assertTrue(self.started_with(starter).startswith("run installer "),
                                    self.calls())

    def test_the_installers_java_reads_with_unix_line_ends_too(self):
        self.installer_java("25.0.5", line_end="\n")
        self.path_java("21.0.5")
        self.run_starter("mystic-crypt.bat")
        self.assertTrue(self.calls()[0].startswith("run installer "), self.calls())

    def test_the_path_java_is_used_when_the_installers_is_gone_too_old_or_a_placeholder(self):
        def gone():
            self.installer_java("25.0.5")
            shutil.rmtree(self.work / "installer jdk (x64)")

        def too_old():
            self.installer_java("21.0.5")

        def placeholder():
            (self.install / "installer-java-home").write_bytes(b"${JAVA_HOME}\r\n")

        for case in (gone, too_old, placeholder):
            for starter in self.STARTERS:
                with self.subTest(installer=case.__name__, starter=starter):
                    self.log.unlink(missing_ok=True)
                    case()
                    self.path_java("26")
                    self.assertEqual(0, self.run_starter(starter).returncode)
                    self.assertTrue(self.started_with(starter).startswith("run path "), self.calls())

    def test_the_version_reads_from_every_form_java_prints(self):
        accepted = {"25": True, "25.0.1": True, "25-ea": True, "26": True, "24.0.2": False,
                    "21.0.5": False, "17": False, "1.8.0_391": False}
        for version, fits in accepted.items():
            with self.subTest(version=version):
                self.log.unlink(missing_ok=True)
                self.path_java(version)
                completed = self.run_starter("mystic-crypt.bat")
                self.assertEqual(0 if fits else 1, completed.returncode, completed.err)
                self.assertEqual(1 if fits else 0, len(self.calls()), self.calls())

    def test_mystic_crypt_bat_says_why_on_standard_error_only_and_exits_non_zero(self):
        self.path_java("21.0.5")
        self.dialog()
        completed = self.run_starter("mystic-crypt.bat", "hash")
        self.assertEqual(1, completed.returncode)
        self.assertIn("Java 25", completed.err)
        self.assertIn("Java 21", completed.err)
        self.assertIn(str(self.bin), completed.err)
        self.assertEqual("", completed.out.strip())
        self.assertEqual([], self.calls())

    def test_the_message_names_the_installers_java_and_the_paths_when_none_fits(self):
        self.installer_java("21.0.5")
        self.path_java("17.0.2")
        completed = self.run_starter("mystic-crypt.bat")
        self.assertEqual(1, completed.returncode)
        self.assertIn("installer jdk (x64)", completed.err)
        self.assertIn("Java 21", completed.err)
        self.assertIn("Java 17", completed.err)

    def test_the_message_says_so_when_there_is_no_java_at_all(self):
        completed = self.run_starter("mystic-crypt.bat")
        self.assertEqual(1, completed.returncode)
        self.assertIn("no java found", completed.err)

    def test_start_bat_shows_why_in_a_dialog_and_on_standard_error_and_exits_non_zero(self):
        self.path_java("21.0.5")
        self.dialog()
        completed = self.run_starter("start.bat")
        self.assertEqual(1, completed.returncode)
        self.assertIn("Java 25", completed.err)
        shown = self.calls()
        self.assertEqual(1, len(shown), shown)
        self.assertTrue(shown[0].startswith("run dialog powershell "), shown)
        self.assertIn("message=mystic-crypt-ui needs Java 25 or newer", shown[0])
        self.assertIn("Java 21", shown[0])
        self.assertNotIn("javaw", shown[0].split("message=")[0])

    def test_the_flags_the_jar_and_the_arguments_reach_the_installers_java(self):
        self.installer_java("25.0.5")
        self.run_starter("mystic-crypt.bat", "kem", "--algorithm", "hybrid")
        self.assertEqual(
            [f"run installer java {self.expected_flags()} [-jar] [{self.install}\\mystic-crypt-ui.jar]"
             " [--cli] [kem] [--algorithm] [hybrid]"], self.calls())

    def test_start_bat_starts_the_installers_javaw_without_a_console(self):
        self.installer_java("25.0.5")
        self.path_java("21.0.5")
        self.assertEqual(0, self.run_starter("start.bat").returncode)
        self.assertEqual(
            [f"run installer javaw {self.expected_flags()} [-jar] [{self.install}\\mystic-crypt-ui.jar]"],
            self.calls_after_start())

    def test_standard_input_and_the_exit_code_pass_through_the_installers_java(self):
        received = self.work / "received.bin"
        self.installer_java("25.0.5", exit_code=3, stdin_file=received)
        completed = self.run_starter("mystic-crypt.bat", "hash", standard_input=b"a line\r\n")
        self.assertEqual(3, completed.returncode)
        self.assertEqual(b"a line\r\n", received.read_bytes())

    def test_the_search_does_not_depend_on_the_working_directory(self):
        self.installer_java("25.0.5")
        elsewhere = self.work / "elsewhere"
        elsewhere.mkdir()
        self.assertEqual(0, self.run_starter("mystic-crypt.bat", cwd=elsewhere).returncode)
        self.assertTrue(self.calls()[0].startswith("run installer "), self.calls())


if __name__ == "__main__":
    unittest.main()
