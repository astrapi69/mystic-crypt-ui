[![Java CI with Gradle](https://github.com/astrapi69/mystic-crypt-ui/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/astrapi69/mystic-crypt-ui/actions/workflows/gradle.yml)

# mystic-crypt-ui

A Swing-based desktop password manager: store credentials with file attachments in a searchable
tree, generate and manage key pairs, verify file checksums, obfuscate text, and now import or
export data to and from real KeePass `.kdbx` databases — extensible through a plugin system.

## Table of Contents

- [Features](#features)
- [Install](#install)
- [Build from source](#build-from-source)
- [Plugins](#plugins)
- [KeePass import/export](#keepass-importexport)
- [Installer tool](#installer-tool)
- [License](#license)
- [Want to help and improve it?](#want-to-help-and-improve-it)
- [Contacting the developer](#contacting-the-developer)
- [Donate](#donate)

## Features

- Save passwords with file attachments in a tree manner
- Check the checksum of downloaded files with the most common algorithms
- Creation of private and public keys with 1024, 2048 and 4096 bit length
- Save the created private and public keys
- Obfuscate text with a specified map that can be exported and imported
- Entries in the existing obfuscation map can be edited, deleted, and new entries can be added
- Import an existing KeePass `.kdbx` database (password, key file, or both) into your own
  encrypted database
- Export your database to a `.kdbx` file usable by real KeePass
- Extend the application with plugins — third-party `.zip` plugins are picked up automatically,
  no installation step required beyond dropping the file in

## Install

Windows, Linux and Mac users can download and install it with the
[izpack installer](https://sourceforge.net/projects/mysticcrypt/files/5.1/installer.jar/download).

> Unix users: don't forget to set the execute bit before running the jar file.

[![Download mystic-crypt](https://a.fsdn.com/con/app/sf-download-button)](https://sourceforge.net/projects/mysticcrypt/files/latest/download)
[![Download mystic-crypt](https://img.shields.io/sourceforge/dm/mysticcrypt.svg)](https://sourceforge.net/projects/mysticcrypt/files/latest/download)

## Build from source

Clone the repository. It's a Gradle project, so you'll need a JDK installation (the project
targets Java 21) — the bundled Gradle wrapper takes care of the rest.

The fastest way to build and launch the app in one step is the bundled `Makefile`:

```bash
make          # or: make all
```

This runs a fast build (compile + package, tests skipped) and then launches the freshly built
jar directly — no separate IDE run configuration needed, and no risk of running against stale
compiled classes.

Other useful targets:

```bash
make build        # fast build only (skips tests, spotless, license checks)
make build-full   # full build: tests, spotless, license checks, then package
make run          # (re-)launch the most recently built jar
make test         # run the test suite only
make clean        # clean build outputs
```

Prefer plain Gradle? That works too:

```bash
./gradlew build
```

The runnable jar (with all dependencies bundled) is generated at
`build/libs/mystic-crypt-ui-<version>-all.jar`.

## Plugins

The application loads plugins via [pf4j](https://github.com/pf4j/pf4j). A plugin is a `.zip` file
dropped into the app's `plugins` directory (created automatically on first launch, under the
application's own configuration directory) — no separate install step, it's picked up on next
launch.

A plugin author implements the `PluginMenuContribution` extension point to add items to the
app's "Plugins" menu:

```java
@Extension
public class HelloMenuContribution implements PluginMenuContribution
{
    @Override
    public List<JMenuItem> getMenuItems()
    {
        JMenuItem item = new JMenuItem("Hello from Plugin");
        item.addActionListener(e -> JOptionPane.showMessageDialog(null, "Hello!"));
        return List.of(item);
    }
}
```

A menu item's action can do anything a normal Swing action can — including opening a full
internal frame panel, the same way every built-in feature of the app does.

See [`examples/hello-plugin`](examples/hello-plugin) for a complete, buildable template project —
including the `plugin.properties` descriptor and the Gradle task that packages the plugin `.zip`
in the exact layout the app expects.

## KeePass import/export

**File → Import from KeePass...** reads an existing `.kdbx` file (password, key file, or both)
and adds its entries into your currently open database, under a new group named after the
imported file — your existing entries are left untouched.

**File → Export to KeePass...** writes your currently open database out to a `.kdbx` file that
can be opened in any standard KeePass client.

Both dialogs remember the last file and key file used, across restarts.

## Installer tool

Here is the installer tool used to package the final application:

- [izpack](http://izpack.org/) — a widely used tool for packaging applications on the Java™
  platform.

For more information on creating the izpack installer, see the
[wiki izpack section](https://github.com/astrapi69/mystic-crypt-ui/wiki/How-to-create-izpack-installer-with-gradle).

## License

The source code comes under the liberal MIT License.

## Want to help and improve it?

The source code for mystic-crypt-ui is on GitHub. Please feel free to fork and send pull
requests!

Create your own fork of [astrapi69/mystic-crypt-ui/fork](https://github.com/astrapi69/mystic-crypt-ui/fork).

To share your changes, [submit a pull request](https://github.com/astrapi69/mystic-crypt-ui/pull/new/develop).

Don't forget to add new unit tests for your changes.

## Contacting the developer

Do not hesitate to contact the mystic-crypt-ui developers with your questions, concerns,
comments, bug reports, or feature requests.

- Feature requests, questions and bug reports can be reported on the
  [issues page](https://github.com/astrapi69/mystic-crypt-ui/issues).

> No animals were harmed in the making of this application.

## Donate

If you like this application, please consider a donation through

<a href="https://flattr.com/submit/auto?fid=r7vp62&url=https%3A%2F%2Fgithub.com%2Flightblueseas%2Fmystic-crypt-ui" target="_blank">
<img src="http://button.flattr.com/flattr-badge-large.png" alt="Flattr this" title="Flattr this" border="0">
</a>
