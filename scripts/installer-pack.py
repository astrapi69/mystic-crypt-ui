"""Reads what an IzPack installer of this application packs (#793, shared with fetch-release-jar.sh since #402).

The installer keeps the files of a pack as zlib streams one after another in resources/packs/<pack>. This reads them in
order and takes those that are zip archives:

  installer-pack.py application-jar <installer> <target> <version>
      writes the application jar, the zip in pack-executables that holds StartMysticCryptApplication, to <target>,
      and fails unless its manifest names Implementation-Version <version>
  installer-pack.py plugins <installer>
      prints the plugin.id of every plugin zip in pack-plugins, one per line, and fails when there is none

Run it with python3 -I. It exits with the reason on standard error when the installer is not what it should be.
"""
import io
import sys
import zipfile
import zlib

APPLICATION_CLASS = "io/github/astrapi69/mystic/crypt/StartMysticCryptApplication.class"


def archives(installer, pack):
    """Every zip archive among the zlib streams of the given pack, in the order the installer holds them"""
    with zipfile.ZipFile(installer) as outer:
        data = outer.read("resources/packs/" + pack)
    position = 0
    while position < len(data):
        stream = zlib.decompressobj()
        try:
            chunk = stream.decompress(data[position:])
        except zlib.error:
            position += 1
            continue
        if not stream.eof:
            break
        position = len(data) - len(stream.unused_data)
        if chunk[:2] == b"PK":
            yield chunk


def application_jar(installer, target, version):
    for chunk in archives(installer, "pack-executables"):
        with zipfile.ZipFile(io.BytesIO(chunk)) as candidate:
            if APPLICATION_CLASS not in candidate.namelist():
                continue
            manifest = candidate.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
        declared = [line.split(":", 1)[1].strip() for line in manifest.splitlines()
                    if line.startswith("Implementation-Version:")]
        if declared != [version]:
            sys.exit(f"the application jar in the installer declares Implementation-Version {declared}, not {version}")
        with open(target, "wb") as out:
            out.write(chunk)
        print(f"==> extracted the application jar, {len(chunk)} bytes, Implementation-Version {version}")
        return
    sys.exit("no application jar (a zip holding StartMysticCryptApplication) in resources/packs/pack-executables")


def plugins(installer):
    found = []
    for chunk in archives(installer, "pack-plugins"):
        with zipfile.ZipFile(io.BytesIO(chunk)) as candidate:
            if "plugin.properties" not in candidate.namelist():
                continue
            properties = candidate.read("plugin.properties").decode("utf-8", "replace")
        ids = [line.split("=", 1)[1].strip() for line in properties.splitlines() if line.startswith("plugin.id=")]
        if len(ids) != 1:
            sys.exit(f"a plugin zip in pack-plugins names {len(ids)} plugin.id")
        found.append(ids[0])
    if not found:
        sys.exit("no plugin zip (a zip holding plugin.properties) in resources/packs/pack-plugins")
    for plugin in found:
        print(plugin)


def main(arguments):
    if len(arguments) == 4 and arguments[0] == "application-jar":
        application_jar(*arguments[1:])
    elif len(arguments) == 2 and arguments[0] == "plugins":
        plugins(arguments[1])
    else:
        sys.exit("usage: installer-pack.py application-jar <installer> <target> <version> | plugins <installer>")


if __name__ == "__main__":
    main(sys.argv[1:])
