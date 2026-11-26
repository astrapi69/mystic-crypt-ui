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


if __name__ == "__main__":
    unittest.main()
