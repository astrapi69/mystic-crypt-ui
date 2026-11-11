
# Overview

The installer of mystic-crypt-ui is an [IzPack](https://izpack.org) 5 installer jar, built by
Gradle through the [Gradle IzPack plugin](https://github.com/bmuschko/gradle-izpack-plugin)
(`com.bmuschko.izpack`) with IzPack's `izpack-ant`; both versions are in
`gradle/libs.versions.toml`. The installer packs the signed jar of the application with all its
dependencies, so building one needs the release keystore (see *Signing* below). The whole build
and release path is described in
[`docs/build-and-release.md`](https://github.com/astrapi69/mystic-crypt-ui/blob/develop/docs/build-and-release.md).

From the repository root, one command builds it:

```bash
make izpack-installer
```

It runs three steps:

1. `make plugins` builds every internal plugin zip. The installer packs them, and IzPack fails on
   a file that is missing.
2. `scripts/with-signing-passwords.sh ./gradlew --no-daemon createAllDependendiesJar -PcreateIzPackInstaller=true`
   runs `clean`, then `withAllDependendiesJar`, which writes the jar with every dependency,
   `build/libs/mystic-crypt-ui-<version>-all.jar`, then `jar`, whose last step signs every jar in
   `build/libs` into `build/signed`. The script hands the keystore's two passwords to that one
   command; `-PcreateIzPackInstaller=true` is what asks for signing on a `-SNAPSHOT` version.
3. `./gradlew checksumInstaller` runs `izPackCreateInstaller`, which writes the installer, and
   then writes a SHA-256 and a SHA-512 file next to it.

The result, in `build/distributions`, with `<version>` the `projectVersion` of `gradle.properties`:

```
mystic-crypt-ui-<version>-installer.jar
mystic-crypt-ui-<version>-installer.jar.sha256
mystic-crypt-ui-<version>-installer.jar.sha512
```

Each checksum file holds one line, `<hash>  <file name>`, the form `sha256sum` and `sha512sum`
read and write, and the form this application's own **Verify Checksum** tool reads. A download
is checked with:

```bash
sha256sum -c mystic-crypt-ui-<version>-installer.jar.sha256
sha512sum -c mystic-crypt-ui-<version>-installer.jar.sha512
```

Check the installer only after `make izpack-installer` has finished, against the file the
checksums describe: `checksumInstaller` runs `izPackCreateInstaller` again, which writes the jar
anew. `make verify-installer PREVIOUS=<last release>` runs the checks of a release on it: the
size, that it opens as an archive, both checksum files, a signer equal to the previous release's,
and more. With `INSTALLER=<file>` it checks another file, such as the one downloaded from the
release.

Two more targets build an installer without the checksum files: `make izpack-installer-signed`
runs `createIzPackInstallerFromSignedJar` (`clean`, `withAllDependendiesJar`, `jar` and
`izPackCreateInstaller`, in that order) and passes no `-PcreateIzPackInstaller=true`, so on a
`-SNAPSHOT` version it signs only with `createIzPackInstaller=true` in `gradle.properties`; and
`make clean-build-installer` runs
`createAllDependendiesJar -PcreateIzPackInstaller=true` and then `build izPackCreateInstaller`,
the full build with every test included.

# The Gradle side

- `gradle/izpack.gradle` configures the plugin: the base directory is `build/`, the installer
  definition is `src/main/izpack/install.xml`, the output file is
  `build/distributions/mystic-crypt-ui-<version>-installer.jar`, compressed with `deflate` at
  level 9. It hands `install.xml` the values it uses as `@{...}`: `app.group`, `app.name`,
  `app.title`, `app.version`, `app.java.version`, `app.author`, `app.author.email`, `app.url` and
  `app.subpath`. The same file holds `checksumInstaller` and `discardBrokenInstaller`, which runs
  after `izPackCreateInstaller` and deletes an output smaller than a megabyte that a failed run
  left behind.
- `gradle/packaging.gradle` holds `jar` with the manifest and the signing,
  `withAllDependendiesJar`, `createAllDependendiesJar` and `createIzPackInstallerFromSignedJar`.
  The task names are spelled `Dependendies`, as written here.
- `gradle.properties` sets `createIzPackInstaller=false`; the Make targets pass
  `-PcreateIzPackInstaller=true` where they need it. A version without `-SNAPSHOT` asks for
  signing in any case.

# The installer definition

`src/main/izpack/install.xml` is read relative to the base directory `build/`, which is why it
names the jar as `signed/@{app.name}-@{app.version}-all.jar` and the plugins as
`../plugins/<name>-plugin/build/plugin-dist/<name>-plugin-1.0.0.zip`. The installer shows the
panels `HelloPanel`, `HTMLInfoPanel` (with `src/main/izpack/installation.html`), `LicencePanel`,
`TargetPanel`, `PacksPanel`, `InstallPanel` and `SimpleFinishPanel`, and offers three packs:

| Pack | Required | What it installs |
|---|---|---|
| `executables` | yes | The signed jar as `mystic-crypt-ui.jar`, `start.sh` and `start.bat`, `CHANGELOG.md`, `LICENSE`, `installation.html`, the icon, and a desktop entry in `~/.local/share/applications`. |
| `commandline` | no, preselected | The `mystic-crypt` and `mystic-crypt.bat` launchers of the command line, see [[The-Command-Line]]. |
| `plugins` | no, preselected | The zip of every internal plugin into `~/.config/mystic-crypt-ui/plugins`, and the licences of the libraries the Lethenon plugin bundles. |

The plugin set of the `plugins` pack is the Makefile's `plugins:` list without the Menu Designer,
which is development tooling and is not shipped (see [[Plugin-Menu-Designer]]). A plugin added to
the pack is also named in the pack's description; `InstallerPackDescriptionTest` checks that the
description names as many tools as the pack installs.

# Signing

The jar is signed with Ant's `signjar` in the last step of the `jar` task. It needs the release
keystore, which lives on the maintainer's machine and never in the repository:

- The project's `gradle.properties` names the property keys to read: `mysticCryptStoreFile`,
  `mysticCryptStoreType`, `mysticCryptKeyAlias`, `mysticCryptStorePassword` and
  `mysticCryptKeyPassword` point at `release.mystic-crypt.store.file`,
  `release.mystic-crypt.store.type`, `release.mystic-crypt.key.alias`,
  `release.mystic-crypt.store.password` and `release.mystic-crypt.key.password`.
- The keystore's file, type and alias are set under those `release.mystic-crypt.*` names in
  `~/.gradle/gradle.properties`, following the pattern described in this
  [article](https://asterios-raptis.medium.com/protect-sensible-data-in-your-build-gradle-file-27c908293711).
- The two passwords come from the desktop's keyring: `scripts/with-signing-passwords.sh` reads
  them with `secret-tool` and hands them to the one Gradle command that signs, as the environment
  variables `MYSTIC_CRYPT_STORE_PASSWORD` and `MYSTIC_CRYPT_KEY_PASSWORD`. They are stored once,
  each typed at the prompt `secret-tool` shows:

  ```bash
  secret-tool store --label='mystic-crypt-ui release keystore' service mystic-crypt-ui purpose store-password
  secret-tool store --label='mystic-crypt-ui release key' service mystic-crypt-ui purpose key-password
  ```

  Without `secret-tool` or without those two entries, the build falls back to the two password
  properties in `~/.gradle/gradle.properties`. The build says which source it used, never the
  value.

# Troubleshooting

If the build stops with

```
not signed: an installer build needs the release keystore - the release.mystic-crypt.* properties for its file, type and alias, and its two passwords from the keyring through scripts/with-signing-passwords.sh (make izpack-installer does that) or from the properties (#579)
```

the keystore properties or the passwords are missing. Set them as described under *Signing*.

If it prints

```
Created jar file is not signed.
IzPackInstaller can not be created because it depends on signed jar.
```

signing was not asked for: the version ends in `-SNAPSHOT` and `-PcreateIzPackInstaller=true` was
not passed, so `izPackCreateInstaller` then fails on the missing signed jar. Use
`make izpack-installer`, which passes it.

If IzPack fails on a missing `plugin-dist` zip, the internal plugins were not built; `make plugins`
builds them, and `make izpack-installer` runs it first.

```
> jarsigner returned: 1
```

can occur if the path to the keystore in `~/.gradle/gradle.properties` is not right, or a
password does not match.

A failed IzPack run leaves an empty installer file behind; `discardBrokenInstaller` removes it and
says `removed mystic-crypt-ui-<version>-installer.jar: <n> bytes cannot be an installer`. No
checksum file is written for a build that failed.
