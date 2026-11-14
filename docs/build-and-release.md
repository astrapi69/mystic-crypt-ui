# Building, packaging and releasing

Developer documentation for `mystic-crypt-ui`. Everything below is derived from the build
files in this repository: `Makefile`, `build.gradle`, `gradle.properties`, the include
files under `gradle/`, `src/main/izpack/install.xml` and `.claude/rules/release-workflow.md`.

## Prerequisites

- **JDK 25.** `build.gradle` sets both `sourceCompatibility` and the Java toolchain from
  the `projectSourceCompatibility` property in `gradle.properties`, which is `25`.
- **A pinned `JAVA_HOME`.** The `Makefile` pins it with `:=`, not `?=`:

  ```makefile
  JAVA_HOME := /home/astrapi69/.sdkman/candidates/java/25-tem
  ```

  The comment above it states the reason: the shell's `JAVA_HOME` (for example sdkman's
  "current") may point at an older JDK, and running the JDK-25-built jar on it fails with
  `UnsupportedClassVersionError`. Every Make target prefixes its Gradle call with this
  value. On a machine where the JDK lives elsewhere, that line is the single place to
  change.
- **The Gradle wrapper.** `gradle/wrapper/gradle-wrapper.properties` pins
  `gradle-9.7.0-bin.zip`. Always call `./gradlew`, never a system Gradle.
- **Python 3.9 or newer** for `scripts/merge_pr.py` and its test suite
  `scripts/merge_pr_test.py` (standard library only, no packages). `make merge-pr` runs the
  script, `make plugins` runs the suite, so the CI build job needs it too (the GitHub runner
  ships it).
- **Xvfb, fluxbox, wmctrl, xdpyinfo and ss for any test run.** Every test task gets a virtual
  display from the own-display Gradle plugin (astrapi69/own-display-gradle-plugin, #741), locally and
  in CI alike, and never uses the desktop's (#505); `ss` (iproute2) names the listener on the
  display's socket (#597). See `.claude/rules/lessons-learned.md`.

## The version lives in exactly one place

`projectVersion` in `gradle.properties` is the only hand-edited version field:

```properties
projectVersion=8.2-SNAPSHOT
```

Everything else derives from it:

| Consumer | How it derives the version |
|---|---|
| `build.gradle` | `version = "$projectVersion"` |
| Jar manifest (`gradle/packaging.gradle`) | `Manifest-Version` and `Implementation-Version` from `project.version` |
| Fat jar file name | `mystic-crypt-ui-<projectVersion>-all.jar` (classifier `all`) |
| IzPack installer (`gradle/izpack.gradle`) | `app.version` and `app.subpath` from `${projectVersion}`; output file `build/distributions/mystic-crypt-ui-<projectVersion>-installer.jar` |
| `install.xml` | reads `@{app.version}` for `<appversion>` and for the jar it packs |
| Release tag (`gradle/tagging.gradle`) | `RELEASE-${project.version}` |
| Release-vs-snapshot switches | `def releaseVersion = !version.endsWith("SNAPSHOT")` in `gradle/packaging.gradle` and `gradle/publishing.gradle` |

That last line is worth internalising: **the string `SNAPSHOT` in `projectVersion` is the
switch** that decides whether jars get signed, whether the Maven publication is signed,
and whether publishing goes to the Sonatype releases or the snapshots repository.

The signing keys are on the maintainer's machine and nowhere else - not in the repository
and not in the CI secrets, deliberately - so **a release is always cut locally**. What that
costs the runner is covered since #431: on a release version, a publish to the LOCAL Maven
repository without a signing key signs nothing and prints that it did not. A publish to a
remote repository without one used to fail; there is no remote repository since #453. CI needs the local publish to build the
plugins (#333), and before this a version bump could not pass CI at all - it died in
`signMavenJavaPublication` with "No configured signatory" before a single test ran.

`.claude/rules/release-workflow.md` states the rule directly: "Version source:
`gradle.properties` (`projectVersion`), the ONLY hand-edited version field. Nothing else
carries an independent version literal; if one appears, fix the derivation, not the
literal."

### The version the info dialog shows

The info dialog takes its version from the running jar's manifest: `ApplicationInfo` reads
`Implementation-Version`, which the build writes from `projectVersion`, and shows "development"
when the jar has none. No second literal exists to be edited at release time; the two the
dialog once carried (`InfoJPanel.version.value` in `messages.properties` and in `DesktopMenu`)
went with #115.

## Make targets by purpose

Every target is described on its own rule line in the `Makefile`, after `##`, with the group it
belongs to: `make` or `make help` prints them all, grouped into start, build, test, plugins,
quality and release. That list is the reference; this section does not repeat it, because a copy
of it here drifted from the Makefile within weeks (#911). `MakefileTargetsAreDescribedTest` fails
on a target without a description and on a target this page, the README, `docs/development.md` or
`CLAUDE.md` names that the Makefile does not have. Which target to run for what, with or without
tests, is in [development.md](development.md).

Every target runs `./gradlew` with the Makefile's `JAVA_HOME` and appends `GRADLE_FLAGS`. What the
one-line descriptions cannot say:

- `make build` cleans, because `createAllDependendiesJar` declares `dependsOn: [clean, ...]`;
  `make all-dependencies-jar` builds the same jar without the clean and without signing, and is
  what `make run` uses.
- `make build-full` is the local equivalent of CI: every plugin with its tests, the release jars
  the compatibility tests read, the runnable jar, then `./gradlew build`.
- The broken bootRun target is gone: it installed the plugins into the real profile and then
  called a Gradle task this build does not have (#918). `make run` is the path that is wired end
  to end, and a start in the real profile is what the installed release is for.

### Tests

Mutation testing has no Make target: run `./gradlew pitest`, report in
`build/reports/pitest`. Its scope is configured in `gradle/mutation-testing.gradle` and is
deliberately limited to headless logic classes (menu, KeePass converters, settings), never
the Swing UI.

Test configuration worth knowing (from `gradle/testing.gradle` and `build.gradle`):

- `useJUnitPlatform()` and `forkEvery = 1`. Each test class gets a fresh JVM, because the
  UI tests drive the process-wide `MysticCryptApplicationFrame` singleton and register
  listeners on the static `ApplicationEventBus`.
- `test` is ordered with `mustRunAfter(tasks.named("jar"))`.
- `awt.robot.screenshotMethod` is set to `x11`, so a Wayland session does not raise the
  XDG portal "share your screen" dialog on every UI-test run.
- `-Dmystic.crypt.ui.test.mode` is forwarded from the Gradle JVM into the forked test JVM
  only when it is set on the command line.
- `check.dependsOn jacocoTestReport`, so any `build`/`check` run also produces the
  coverage report.

### Formatting and license headers

The Spotless Java step uses `src/test/resources/spotless/importorder.properties` and the
Eclipse config `src/test/resources/spotless/alpharo-formatter.xml`; the `misc` format
covers `*.gradle`, `*.md` and `.gitignore`. Never hand-format against the formatter. Since #686
Spotless also writes and checks the license header of every Java source of the host build, from
`src/test/resources/spotless/license-header.txt`; `make license-format` is the same as
`make spotless-java`.

### Internal plugins

Each feature plugin lives under `plugins/{name}-plugin/` and is built as a standalone
Gradle build against the host published to the local Maven repository: every
`make plugin-<name>` runs `make publish-local` first, then the plugin's tests and `pluginZip`.

The Makefile `plugins:` target is the canonical inventory of internal plugins; do not
count them from any document, read that target. Each plugin directory is bound to a
`PLUGIN_*_DIR` variable at the top of the Makefile, and `PLUGIN_INSTALL_DIR` is
`$(HOME)/.config/mystic-crypt-ui/plugins`.

The host must be published locally first because the plugins compile `compileOnly`
against `io.github.astrapi69:mystic-crypt-ui:<projectVersion>` plus each library the moved
panels use directly. The plugin reads that version from the root `gradle.properties` and never
repeats it as a literal (#217). From `plugins/obfuscation-plugin/build.gradle`:

```gradle
def hostVersion = new Properties().with { properties ->
    file("../../gradle.properties").withInputStream { properties.load(it) }
    properties.getProperty("projectVersion")
}

dependencies {
    compileOnly "io.github.astrapi69:mystic-crypt-ui:$hostVersion"
    ...
    compileOnly libs.pf4j
    annotationProcessor libs.pf4j
}
```

The plugin's Java toolchain must stay at the host's level (the plugin sets
`JavaLanguageVersion.of(25)` with the comment that a lower toolchain's javac cannot read
JDK 25 class files from the compile classpath).

`pluginZip` packages the layout the host's `DefaultPluginManager` expects, that is
`plugin.properties` at the zip root and the compiled classes under `classes/`:

```gradle
tasks.register('pluginZip', Zip) {
    archiveFileName = "obfuscation-plugin-${version}.zip"
    destinationDirectory = layout.buildDirectory.dir("plugin-dist")
    from("src/main/resources") {
        include "plugin.properties"
    }
    into("classes") {
        from sourceSets.main.output.classesDirs
        from("src/main/resources") {
            exclude "plugin.properties"
        }
    }
    dependsOn classes
}
```

Note that each plugin carries its own `version = '1.0.0'`, independent of
`projectVersion`, and that `install.xml` refers to the zips by their exact file names. A
plugin version bump therefore has to be mirrored in `install.xml`.

### Packaging and installer

Every installer target depends on `plugins` first: the installer ships the internal plugin zips
(pack `plugins` in `src/main/izpack/install.xml`), except the menu designer, so they have to be
built before izpack packs them, otherwise it fails on the missing files. They also need the release
keystore; see [Building the installer](#building-the-installer).

## The fat jar

The runnable artifact is a fat jar, produced by `withAllDependendiesJar` in
`gradle/packaging.gradle` (the task name is spelled with the double "d", both here and in
`createAllDependendiesJar`):

```gradle
task withAllDependendiesJar(type: Jar) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.from jar.manifest
    archiveClassifier = "all"
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    } {
        exclude "META-INF/*.SF"
        exclude "META-INF/*.DSA"
        exclude "META-INF/*.RSA"
    }
    with jar
}
```

The signature files of the bundled dependencies are excluded on purpose: a shaded jar that
keeps another party's `META-INF` signatures fails verification at startup.

The manifest is built in the `jar` block and carries, among others, `Main-Class` from the
`mainClass` property in `gradle.properties`
(`io.github.astrapi69.mystic.crypt.StartMysticCryptApplication`), `Implementation-Version`
from `project.version`, build timestamp, JDK and OS. It declares no `Add-Opens`: the one it
carried was for KeePassJava2's Simple model, which the application no longer uses (#408).

The aggregate task fixes the order:

```gradle
task createAllDependendiesJar(dependsOn: [clean, withAllDependendiesJar, jar]) {
    withAllDependendiesJar.mustRunAfter(clean)
    jar.mustRunAfter(withAllDependendiesJar)
}
```

`jar` runs **after** the fat jar deliberately: the signing step hangs off `jar.doLast` and
signs everything sitting in `build/libs` at that moment, the fat jar included.

## Signing configuration

### Where it comes from

The keystore coordinates are not in this repository. `gradle.properties` only holds the
**names** of the properties to look up:

```properties
############################
# keystore properties keys #
############################
mysticCryptStoreFile=release.mystic-crypt.store.file
mysticCryptStoreType=release.mystic-crypt.store.type
mysticCryptStorePassword=release.mystic-crypt.store.password
mysticCryptKeyPassword=release.mystic-crypt.key.password
mysticCryptKeyAlias=release.mystic-crypt.key.alias
```

`gradle/packaging.gradle` then resolves that indirection with
`project.property("$mysticCryptKeyAlias")` and friends. So the five keys a developer sets
in their **private** Gradle properties file outside this repository, normally
`~/.gradle/gradle.properties`, are:

| Property key | What it names |
|---|---|
| `release.mystic-crypt.store.file` | path to the keystore file |
| `release.mystic-crypt.store.type` | keystore type, passed to `signjar` as `storetype` |
| `release.mystic-crypt.store.password` | keystore password |
| `release.mystic-crypt.key.password` | key password |
| `release.mystic-crypt.key.alias` | alias of the signing key |

**Their values are never committed to this repository, never printed by the build, and
never written to any log.** The build reads them only to hand them to Ant's `signjar` task
and reports nothing but whether the set is present or absent. Do not paste a value into an
issue, a commit, a CI variable dump, a chat message or a document, and do not add the
keystore file itself to the working tree; `.claude/rules/coding-standards.md` forbids
committing keys, keystores with real keys and passwords outright.

### When signing runs

```gradle
def releaseVersion = !version.endsWith("SNAPSHOT")
def createIzPackInstaller = project.property('createIzPackInstaller').toBoolean()
...
def signingConfigured = [mysticCryptKeyAlias, mysticCryptStoreFile, mysticCryptStorePassword,
                         mysticCryptKeyPassword, mysticCryptStoreType].every { project.hasProperty(it) }
```

Signing happens when `releaseVersion || createIzPackInstaller` is true **and** all five
properties are present. `createIzPackInstaller` is a property in `gradle.properties` and
ships as `false`.

The build fails open rather than hard, by design. The comment in the file says it: the
keystore properties live in the developer's private `gradle.properties`, never in the
repository, so a build that wants signing but has no keystore configured (CI, a fresh
clone, a contributor's machine) says so and produces an unsigned jar instead of failing.
The two messages you will see are `No signing configuration found, the created jar file is
not signed. Set the release.mystic-crypt.* properties to sign it.` and `Created jar file
is not signed. IzPackInstaller can not be created because it depends on signed jar.`

When it does run, every jar in `build/libs` is signed into `build/signed` through
`ant.signjar`, preserving last-modified timestamps.

Because the `jar` task's `doLast` reaches for script objects at execution time, the task
opts out of the configuration cache explicitly:

```gradle
jar.notCompatibleWithConfigurationCache(
        "jar signing doLast uses script objects at execution time")
```

### The other signing: the Maven publication

`gradle/publishing.gradle` applies the `signing` plugin to the publication, and only for a
release version:

```gradle
signing {
    if (releaseVersion) {
        sign publishing.publications.mavenJava
    }
}
```

This is Gradle's own signing plugin over the published artifacts, separate from the
`release.mystic-crypt.*` jar signing above.

## Building the installer

The IzPack configuration is split between `gradle/izpack.gradle` (the Gradle side) and
`src/main/izpack/install.xml` (the installer definition). The Gradle side:

```gradle
izpack {
    baseDir = file("$buildDirectory")
    installFile = file("src/main/izpack/install.xml")
    outputFile = file("$buildDirectory/distributions/$rootProject.name-${projectVersion}-installer.jar")
    compression = "deflate"
    compressionLevel = 9
    appProperties = ["app.group": "$groupPackage", ...]
}
```

`baseDir` is `build`, so every `src` path inside `install.xml` is relative to the **build
directory**, not to the source tree. That is why the installer definition refers to
`resources/main/doc/start.sh` (the processed resources under `build/resources/main`),
`signed/mystic-crypt-ui-<version>-all.jar` (the signed fat jar under `build/signed`) and
`../plugins/...` and `../README.md` (back out of `build` into the project).

`appProperties` are the `@{...}` placeholders in `install.xml`: `app.group`, `app.name`,
`app.title`, `app.version`, `app.java.version`, `app.author`, `app.author.email`,
`app.url` and `app.subpath`, all derived from `gradle.properties`.

The IzPack Ant library is pulled in through its own configuration in
`gradle/dependencies.gradle`:

```gradle
izpack libs.izpack.ant
```

### The three packs

`install.xml` defines:

1. `executables` (`required="yes"`): `CHANGELOG.md`, `LICENSE`, `README.md`, the icon, the
   `start.sh` / `start.bat` launchers, the `.desktop` entry, and the application jar as
   `<singlefile src="signed/@{app.name}-@{app.version}-all.jar" target="${INSTALL_PATH}/@{app.name}.jar" />`.
   The desktop entry is installed twice, into the install path and into
   `${USER_HOME}/.local/share/applications`, and both copies are `parsable` so that
   `${INSTALL_PATH}` is substituted into `Exec` and `Icon`; the Desktop Entry
   specification does not expand `~`, so the real path has to be written in.
2. `commandline` (optional, preselected): the `mystic-crypt` and `mystic-crypt.bat`
   launcher scripts, which run the CLI entry point out of the same application jar.
   Depends on `executables`.
3. `plugins` (optional, preselected): every internal plugin zip, installed into
   `${USER_HOME}/.config/mystic-crypt-ui/plugins`, which is where the application's plugin
   manager looks. Without this pack the "Plugins" menu stays empty until the user installs
   them by hand through Settings, Plugins, "Install from Zip...". Depends on
   `executables`.

**The plugin set in `install.xml` must match the Makefile `plugins:` list.** The release
gate in `.claude/rules/release-workflow.md` calls this out explicitly, and the entries are
literal file names including the plugin version, so a rename or a version bump breaks the
installer build with a missing-file error.

### What the installer needs in place

`make izpack-installer` runs three steps (the target in the `Makefile`): its `plugins`
prerequisite builds every plugin zip; `createAllDependendiesJar -PcreateIzPackInstaller=true`,
with the keystore's passwords from `scripts/with-signing-passwords.sh`, cleans and builds the
fat jar and signs it; and `checksumInstaller`, which depends on `izPackCreateInstaller`, builds
the installer and then writes its `.sha256` and `.sha512` files next to it. What the installer
step reads from `build`:

- `build/resources/main/...`, that is a completed `processResources`, for the launchers,
  the desktop entry and the icon;
- `build/signed/mystic-crypt-ui-<version>-all.jar`, that is a signed fat jar, which
  requires `createIzPackInstaller=true` or a non-SNAPSHOT version **and** the keystore: its
  file, type and alias as `release.mystic-crypt.*` properties, and its two passwords from the
  environment `scripts/with-signing-passwords.sh` fills from the keyring, else from the two
  password properties (#579);
- the plugin zips under `plugins/*/build/plugin-dist/`, which the `plugins` prerequisite
  provides.

The two other targets:

- `make clean-build-installer` builds the plugins and the signed fat jar the same way, then runs
  `build izPackCreateInstaller`; it writes no checksums.
- `make izpack-installer-signed` runs `createIzPackInstallerFromSignedJar`, which orders
  the whole chain:

  ```gradle
  task createIzPackInstallerFromSignedJar(dependsOn: [clean, withAllDependendiesJar, jar, izPackCreateInstaller]) {
      withAllDependendiesJar.mustRunAfter(clean)
      jar.mustRunAfter(withAllDependendiesJar)
      izPackCreateInstaller.mustRunAfter(jar)
  }
  ```

  clean, then the fat jar, then `jar` (whose `doLast` signs everything in `build/libs` into
  `build/signed`), then the installer.

The result is `build/distributions/mystic-crypt-ui-<projectVersion>-installer.jar`.

`DEPLOYMENT-INFO.md` points at the project wiki for the IzPack walkthrough:
<https://github.com/astrapi69/mystic-crypt-ui/wiki/How-to-create-izpack-installer-with-gradle>

## Publishing

This application is not published to a remote repository. It is distributed as an installer,
and it has never been on Maven Central. `gradle/publishing.gradle` builds one publication,
`mavenJava`, with `artifactId` set to the root project name, the main component plus a
`sourcesJar` and a `javadocJar`, and a full POM (name, description, url, organization, issue
management, license, developer, scm), all assembled from `gradle.properties` values. Its only
target is the local Maven repository, through `make publish-local`.

Until #453 the file also declared a remote repository: the OSSRH staging endpoint for a release
version, which was shut down, and the snapshot repository otherwise. No release ever went there.
It was removed rather than moved, because nothing needs the host as a Maven artifact. If that
changes, the host gets the libraries' Central Portal shape (`com.gradleup.nmcp`, `USER_MANAGED`)
and a snapshot run before anything depends on it.

Note also `gradle/repositories.gradle`, where `mavenLocal()` is **off unless asked for**:
`./gradlew build -PuseMavenLocal`. Anything in the local repository otherwise wins over Maven
Central, so a snapshot published hours ago becomes what the build compiles against - on that
machine and nowhere else (#475). The handshake between the host and the plugin builds still
works without the flag, because every plugin build takes its repositories from
`gradle/plugin-repositories.gradle`, which keeps `mavenLocal()` scoped to the one module it needs
from there - and, with the same `-PuseMavenLocal`, admits the family's libraries as well (#508):

```groovy
mavenLocal {
    content {
        if (providers.gradleProperty('useMavenLocal').isPresent()) {
            includeGroup('io.github.astrapi69')
        } else {
            includeModule('io.github.astrapi69', 'mystic-crypt-ui')
        }
    }
}
```

So without the flag a locally published library or chain snapshot cannot shadow Central for a
plugin, and with it `make bump-check GRADLE_FLAGS=-PuseMavenLocal` checks a crypt-data or
mystic-crypt candidate in the host and in every plugin before it is on Central.

## The release sequence

The binding sequence is `.claude/rules/release-workflow.md`; this page does not copy its steps,
because a copy here kept naming `vX.Y.Z` tags and a `make test` plus `make test-e2e` gate long
after the rules had moved on (#911). In short: capture the state and wait for confirmation, pick
the version per SemVer, assemble the changelog from `changelog.d/`, bump `projectVersion`, run
`make release-gate`, merge the release pull request, build `make izpack-installer` from the merged
state and check it with `make verify-installer`, then `make tag-release`, push the tag, move
`master` onto it and write the GitHub release with the installer and its checksums.

### Two things to know before you tag

- **Tag name.** `make tag-release` runs the `tagRelease` task in `gradle/tagging.gradle`, which
  creates the annotated tag `RELEASE-${project.version}` with the message `New release in version
  ${project.version}` locally; pushing it is a step of its own. Every release tag of this
  repository has that shape (#291).
- **Signing and the installer.** As soon as `projectVersion` no longer ends in `SNAPSHOT`,
  the `jar` task wants to sign; without the keystore it says so and leaves the jar unsigned. An
  installer build refuses instead: it needs the `release.mystic-crypt.*` properties for the
  keystore's file, type and alias, and its two passwords, which `make izpack-installer` takes from
  the keyring through `scripts/with-signing-passwords.sh` (#579).

## Continuous integration

Among the workflows under `.github/workflows/`:

- **`gradle.yml`** ("Java CI with Gradle"), on push and pull request against `master` and
  `develop`. Temurin JDK 25, `gradle/actions/setup-gradle`, installs Xvfb, fluxbox, wmctrl,
  x11-utils and iproute2 and runs `./gradlew build`, whose test tasks bring their own display (#505), so
  the AssertJ-Swing end-to-end tests really run instead of being skipped by their headless
  assumption.
  On failure it
  uploads `build/reports/tests/test` and `build/test-results/test` for 14 days, because a
  failing UI test prints only its top stack frame to the console.
- **`mutation.yml`** ("Mutation testing"), on `workflow_dispatch`, on a weekly cron
  (Mondays 04:17 UTC) and on pull requests that touch the measured logic packages or
  `gradle/mutation-testing.gradle`. It runs `./gradlew pitest`, writes killed / survived /
  not covered / total into the job summary from `build/reports/pitest/mutations.xml`, and
  uploads the HTML report. It is kept off the push path on purpose: PIT reruns the
  covering tests for every mutation, so it is far slower than a normal build.
- **`wiki-mirror.yml`** ("Wiki mirror"), on push to `master` that changes `docs/wiki/**`, and on
  `workflow_dispatch`, never on a pull request. It runs `scripts/mirror-wiki.sh`, which clones the
  GitHub wiki with plain git, writes and deletes pages until the wiki holds exactly the pages of
  `docs/wiki/`, names every page written or deleted, and pushes; the list goes into the job
  summary. It needs `contents: write` and nothing else. A `docs/wiki/` without a page, a wiki it
  cannot clone and a refused push each fail the run (#869).

## Gradle build layout

`build.gradle` stays small and delegates:

```gradle
apply from: "gradle/apply-gradle-files.gradle"
```

which reads `gradle/gradle-files.list` line by line and applies each file. To add a new
build concern, add the file **and** its line in `gradle-files.list`, otherwise it is
silently not applied. The file itself is the current list; this page does not repeat it.

Dependency versions live in the version catalog `gradle/libs.versions.toml` and are
referenced as `libs.*` (libraries) and `libs.plugins.*` (plugins). `make
version-catalog-update` and `make version-catalog-format` maintain it;
`gradle/version-catalog-update.gradle` sets `sortByKey` and `keepUnusedVersions = true`.

### Configuration cache

`gradle.properties` turns on caching, parallel execution, configure-on-demand, file-system
watching and the configuration cache, and degrades configuration-cache problems to
warnings:

```properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.vfs.watch=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

The comment above the last line gives the grgit plugin as its reason, which started git processes
at configuration time; the build no longer applies grgit (the tag task is a plain `Exec` in
`gradle/tagging.gradle`), and whether anything else still needs the setting has not been measured.
The other configuration-cache accommodation is the `jar.notCompatibleWithConfigurationCache(...)`
call for signing.
