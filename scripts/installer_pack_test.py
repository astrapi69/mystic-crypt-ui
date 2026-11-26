#!/usr/bin/env python3
"""Tests for installer-pack.py, which reads the application jar and the plugin zips out of an IzPack installer.

Every installer here is built in a temporary directory with zipfile and zlib: its packs hold zlib streams one after
another, as the real one does. The script has a hyphen in its name, so the walk over the streams is called through
importlib and the exit codes and standard error are read from a subprocess run with python3 -I.

What VerifyInstallerTest already covers is not repeated: the application jar found, one of another version refused, the
plugin ids listed, a stream that is no zip skipped, a zip without the start class skipped.

Run: python3 scripts/installer_pack_test.py
"""

import importlib.util
import io
import subprocess
import sys
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "installer-pack.py"
APPLICATION_CLASS = "io/github/astrapi69/mystic/crypt/StartMysticCryptApplication.class"
JUNK = b"\x00\x01\x02\x03"

_spec = importlib.util.spec_from_file_location("installer_pack", SCRIPT)
installer_pack = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(installer_pack)


def zip_of(entries):
    """A zip archive holding the given name to text entries"""
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for name, content in entries.items():
            archive.writestr(name, content)
    return buffer.getvalue()


def application_jar(manifest="Manifest-Version: 1.0\nImplementation-Version: 9.9\n"):
    return zip_of({APPLICATION_CLASS: "class", "META-INF/MANIFEST.MF": manifest})


def plugin_zip(properties):
    return zip_of({"plugin.properties": properties})


def streams(*files):
    """The given files as zlib streams one after another, the way a pack holds them"""
    return b"".join(zlib.compress(file) for file in files)


class InstallerCase(unittest.TestCase):
    """A temporary directory per test, and the means to build an installer in it and run the script on it"""

    def setUp(self):
        self._directory = tempfile.TemporaryDirectory()
        self.addCleanup(self._directory.cleanup)
        self.directory = Path(self._directory.name)

    def installer(self, executables=b"", plugins=b"", name="installer.jar"):
        """An installer whose two packs hold the given bytes as they are"""
        path = self.directory / name
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("resources/packs/pack-executables", executables)
            archive.writestr("resources/packs/pack-plugins", plugins)
        return path

    def pack_of(self, content):
        """The archives the walk finds in a pack that holds the given bytes"""
        return list(installer_pack.archives(self.installer(plugins=content), "pack-plugins"))

    def run_script(self, *arguments):
        return subprocess.run([sys.executable, "-I", str(SCRIPT), *arguments], capture_output=True, text=True,
                              timeout=60)

    def plugins_of(self, plugins):
        return self.run_script("plugins", str(self.installer(plugins=plugins)))

    def application_jar_of(self, executables, version="9.9"):
        target = self.directory / "app.jar"
        return self.run_script("application-jar", str(self.installer(executables=executables)), str(target),
                               version), target


class TestWalkOverTheStreams(InstallerCase):

    def test_archives_findsEveryZip_whenTheStreamsFollowEachOther(self):
        first, second = zip_of({"a": "1"}), zip_of({"b": "2"})

        self.assertEqual([first, second], self.pack_of(streams(first, second)))

    def test_archives_skipsBytesThatAreNoZlibStream_andFindsTheZipsAround(self):
        first, second = zip_of({"a": "1"}), zip_of({"b": "2"})

        found = self.pack_of(JUNK + streams(first) + JUNK + streams(second))

        self.assertEqual([first, second], found)

    def test_archives_findsNothing_whenThePackHoldsOnlyBytesThatAreNoZlibStream(self):
        self.assertEqual([], self.pack_of(JUNK * 8))

    def test_archives_findsNothing_whenThePackIsEmpty(self):
        self.assertEqual([], self.pack_of(b""))

    def test_archives_endsTheWalkAtATruncatedLastStream_andKeepsTheZipsBefore(self):
        first, second = zip_of({"a": "1"}), zip_of({"b": "2"})

        found = self.pack_of(streams(first) + zlib.compress(second)[:-5])

        self.assertEqual([first], found)

    def test_archives_findsTheLastStream_whenItIsComplete(self):
        first, second = zip_of({"a": "1"}), zip_of({"b": "2"})

        found = self.pack_of(streams(first) + zlib.compress(second))

        self.assertEqual([first, second], found)


class TestApplicationJar(InstallerCase):

    def test_applicationJar_isRefused_whenNoZipHoldsTheStartClass(self):
        run, target = self.application_jar_of(streams(zip_of({"lib/other.txt": "x"}), b"a launcher script"))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("no application jar", run.stderr)
        self.assertIn("pack-executables", run.stderr)
        self.assertFalse(target.exists())

    def test_applicationJar_isRefused_whenThePackHoldsNothing(self):
        run, target = self.application_jar_of(b"")

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("no application jar", run.stderr)
        self.assertFalse(target.exists())

    def test_applicationJar_isRefused_whenTheManifestDeclaresNoVersion(self):
        run, target = self.application_jar_of(streams(application_jar("Manifest-Version: 1.0\n")))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("Implementation-Version []", run.stderr)
        self.assertFalse(target.exists())

    def test_applicationJar_isRefused_whenTheManifestDeclaresTheVersionTwice(self):
        manifest = "Implementation-Version: 9.9\nImplementation-Version: 9.9\n"

        run, target = self.application_jar_of(streams(application_jar(manifest)))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("['9.9', '9.9']", run.stderr)
        self.assertFalse(target.exists())

    def test_applicationJar_isWritten_whenTheManifestDeclaresTheVersionOnce(self):
        jar = application_jar()

        run, target = self.application_jar_of(streams(jar))

        self.assertEqual(0, run.returncode, run.stderr)
        self.assertEqual(jar, target.read_bytes())

    def test_applicationJar_isFound_behindBytesThatAreNoZlibStream(self):
        jar = application_jar()

        run, target = self.application_jar_of(JUNK + streams(b"a launcher script") + JUNK + streams(jar))

        self.assertEqual(0, run.returncode, run.stderr)
        self.assertEqual(jar, target.read_bytes())


class TestPlugins(InstallerCase):

    def test_plugins_areRefused_whenThePackHoldsNoZip(self):
        run = self.plugins_of(streams(b"readme"))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("no plugin zip", run.stderr)
        self.assertEqual("", run.stdout)

    def test_plugins_areRefused_whenTheOnlyZipHoldsNoPluginProperties(self):
        run = self.plugins_of(streams(zip_of({"lib/other.txt": "x"})))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("no plugin zip", run.stderr)

    def test_plugins_skipAZipWithoutPluginProperties_andNameTheOthers(self):
        run = self.plugins_of(streams(zip_of({"lib/other.txt": "x"}), plugin_zip("plugin.id=keygen-plugin\n")))

        self.assertEqual(0, run.returncode, run.stderr)
        self.assertEqual("keygen-plugin\n", run.stdout)

    def test_plugins_areRefused_whenAPluginZipNamesNoPluginId(self):
        run = self.plugins_of(streams(plugin_zip("plugin.version=1.0.0\n")))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("names 0 plugin.id", run.stderr)
        self.assertEqual("", run.stdout)

    def test_plugins_areRefused_whenAPluginZipNamesTwoPluginIds(self):
        run = self.plugins_of(streams(plugin_zip("plugin.id=a-plugin\nplugin.id=b-plugin\n")))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("names 2 plugin.id", run.stderr)
        self.assertEqual("", run.stdout)

    def test_plugins_areRefused_whenOneOfTwoPluginZipsNamesNoPluginId(self):
        run = self.plugins_of(streams(plugin_zip("plugin.id=a-plugin\n"), plugin_zip("plugin.version=1\n")))

        self.assertEqual(1, run.returncode, run.stderr)
        self.assertIn("names 0 plugin.id", run.stderr)
        self.assertEqual("", run.stdout)

    def test_plugins_areNamed_whenTheyLieBehindBytesThatAreNoZlibStream(self):
        run = self.plugins_of(JUNK + streams(plugin_zip("plugin.id=a-plugin\n")) + JUNK
                              + streams(plugin_zip("plugin.id=b-plugin\n")))

        self.assertEqual(0, run.returncode, run.stderr)
        self.assertEqual("a-plugin\nb-plugin\n", run.stdout)


class TestArguments(InstallerCase):

    def test_usage_isPrintedOnStandardError_forArgumentsThatFitNoCommand(self):
        installer = str(self.installer())
        cases = {
            "no arguments": [],
            "an unknown command": ["list", installer],
            "application-jar without the version": ["application-jar", installer, "app.jar"],
            "application-jar with one argument more": ["application-jar", installer, "app.jar", "9.9", "x"],
            "plugins without the installer": ["plugins"],
            "plugins with one argument more": ["plugins", installer, "x"],
        }
        for label, arguments in cases.items():
            with self.subTest(label):
                run = self.run_script(*arguments)

                self.assertEqual(1, run.returncode, run.stderr)
                self.assertTrue(run.stderr.startswith("usage: installer-pack.py"), run.stderr)
                self.assertEqual("", run.stdout)

    def test_usage_isNotPrinted_forTheTwoCommandsWithTheirArguments(self):
        installer = str(self.installer(executables=streams(application_jar()),
                                       plugins=streams(plugin_zip("plugin.id=a-plugin\n"))))
        cases = {
            "application-jar": ["application-jar", installer, str(self.directory / "app.jar"), "9.9"],
            "plugins": ["plugins", installer],
        }
        for label, arguments in cases.items():
            with self.subTest(label):
                run = self.run_script(*arguments)

                self.assertEqual(0, run.returncode, run.stderr)
                self.assertEqual("", run.stderr)


class TestAnInstallerThatIsNotWhatItShouldBe(InstallerCase):
    """The docstring of the script promises the reason on standard error: one line, no traceback."""

    def assertRefusedWithOneLine(self, run, *named):
        lines = run.stderr.strip().splitlines()
        self.assertEqual(1, run.returncode, run.stderr)
        self.assertNotIn("Traceback", run.stderr)
        self.assertEqual(1, len(lines), run.stderr)
        for text in named:
            self.assertIn(text, lines[0])

    def test_refused_whenTheInstallerIsNotAZip(self):
        installer = self.directory / "installer.jar"
        installer.write_text("not a zip", encoding="utf-8")

        for arguments in (["plugins", str(installer)],
                          ["application-jar", str(installer), str(self.directory / "app.jar"), "9.9"]):
            with self.subTest(arguments[0]):
                self.assertRefusedWithOneLine(self.run_script(*arguments), "installer.jar")

    def test_refused_whenTheInstallerDoesNotExist(self):
        missing = self.directory / "missing.jar"

        self.assertRefusedWithOneLine(self.run_script("plugins", str(missing)), "missing.jar")

    def test_refused_whenThePackOfThePluginsIsMissing(self):
        installer = self.directory / "installer.jar"
        with zipfile.ZipFile(installer, "w") as archive:
            archive.writestr("resources/packs/pack-executables", b"")

        self.assertRefusedWithOneLine(self.run_script("plugins", str(installer)), "pack-plugins")

    def test_refused_whenThePackOfTheExecutablesIsMissing(self):
        installer = self.directory / "installer.jar"
        with zipfile.ZipFile(installer, "w") as archive:
            archive.writestr("resources/packs/pack-plugins", b"")

        run = self.run_script("application-jar", str(installer), str(self.directory / "app.jar"), "9.9")

        self.assertRefusedWithOneLine(run, "pack-executables")

    def test_refused_whenTheApplicationJarHasNoManifest(self):
        jar = zip_of({APPLICATION_CLASS: "class"})

        run, target = self.application_jar_of(streams(jar))

        self.assertRefusedWithOneLine(run, "MANIFEST.MF")
        self.assertFalse(target.exists())

    def test_refused_whenAStreamStartsLikeAZipButIsNone(self):
        run = self.plugins_of(streams(b"PK\x03\x04 this is no zip"))

        self.assertRefusedWithOneLine(run, "pack-plugins")


if __name__ == "__main__":
    unittest.main()
