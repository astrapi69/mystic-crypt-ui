# Building, packaging and releasing

Developer documentation for `mystic-crypt-ui`. Everything below is derived from the build
files in this repository: `Makefile`, `build.gradle`, `gradle.properties`, the include
files under `gradle/`, `src/main/izpack/install.xml`, `.claude/rules/release-workflow.md`
and `DEPLOYMENT-INFO.md`.

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
- **Xvfb for the end-to-end UI tests.** The AssertJ-Swing suite needs a virtual display;
  see `.claude/rules/lessons-learned.md`. CI does the same with
  `xvfb-run -a --server-args="-screen 0 1920x1080x24"`.

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
| Release tag (`gradle/grgit.gradle`) | `RELEASE-${project.version}` |
| Release-vs-snapshot switches | `def releaseVersion = !version.endsWith("SNAPSHOT")` in `gradle/packaging.gradle` and `gradle/publishing.gradle` |

That last line is worth internalising: **the string `SNAPSHOT` in `projectVersion` is the
switch** that decides whether jars get signed, whether the Maven publication is signed,
and whether publishing goes to the Sonatype releases or the snapshots repository.

`.claude/rules/release-workflow.md` states the rule directly: "Version source:
`gradle.properties` (`projectVersion`), the ONLY hand-edited version field. Nothing else
carries an independent version literal; if one appears, fix the derivation, not the
literal."

### Known drift from that rule

Two places currently carry their own literal, and `DEPLOYMENT-INFO.md` still instructs a
developer to hand-edit them at release time:

- `src/main/resources/ui/messages.properties`, key `InfoJPanel.version.value`
- `src/main/java/io/github/astrapi69/mystic/crypt/DesktopMenu.java`, as the fallback
  argument of `Messages.getString("InfoJPanel.version.value", ...)`

These are the version shown in the info dialog. They are independent literals, so they
can and do fall behind `projectVersion`. Per the rule above, the fix is to derive the
dialog version from the build rather than to keep editing the literals; until that
happens, treat `DEPLOYMENT-INFO.md` as the checklist for them.

## Make targets by purpose

Every target runs `JAVA_HOME=$(JAVA_HOME) ./gradlew <task>`. The table gives the task, the
prose below adds what is not obvious.

### Build and run

| Target | Runs |
|---|---|
| `make build` | `gradlew createAllDependendiesJar` |
| `make build-full` | `gradlew clean build`, then `gradlew createAllDependendiesJar` |
| `make build-stacktrace` | `gradlew build --stacktrace --warning-mode all` |
| `make build-warning` | `gradlew build --warning-mode all` |
| `make jar` | `gradlew jar` (the thin jar only) |
| `make all-dependencies-jar` | `gradlew withAllDependendiesJar` (the fat jar task alone, no clean, no signing) |
| `make clean` | `gradlew clean` |
| `make run` | finds the first `build/libs/*-all.jar` and runs `"$(JAVA_HOME)/bin/java" -jar` on it |
| `make all` | `build`, then `plugins-install`, then `run` |
| `make bootRun` | `plugins-install`, then `gradlew bootRun` |

Notes:

- `make build` is the fast path. The Makefile comment describes it as "clean, compile,
  package the runnable jar, skips tests/spotless/license". It still cleans, because
  `createAllDependendiesJar` declares `dependsOn: [clean, ...]`.
- `make build-full` is described in the Makefile as "clean, compile, test, spotless,
  license, then package the runnable jar".
- `make run` fails loudly rather than silently doing nothing:

  ```make
  @jar=$$(find build/libs -maxdepth 1 -name '*-all.jar' -print -quit); \
  if [ -z "$$jar" ]; then \
      echo "No *-all.jar in build/libs - run 'make build' first." >&2; \
      exit 1; \
  fi; \
  ```
- `make all` is the target that guarantees you run exactly what you just built, with the
  internal plugins built and installed beforehand.
- `make bootRun` installs the plugins and then calls `./gradlew bootRun`. The Makefile
  groups it under "mirrors Gradle Run Configurations panel", but no `bootRun` task is
  defined in this repository's Gradle files, so check that the task resolves before
  relying on it. `make all` is the path that is wired end to end.

### Test and quality

| Target | Runs |
|---|---|
| `make test` | `gradlew test` |
| `make test-e2e` | `gradlew test --tests "io.github.astrapi69.mystic.crypt.ui.*" --rerun` |
| `make test-e2e-demo` | the same plus `-Dmystic.crypt.ui.test.mode=demo` |
| `make jacoco-report` | `gradlew jacocoTestReport` |
| `make jacoco-coverage` | `gradlew jacocoTestCoverageVerification` |
| `make javadoc` | `gradlew javadoc` |

Mutation testing has no Make target: run `./gradlew pitest`, report in
`build/reports/pitest`. Its scope is configured in `gradle/mutation-testing.gradle` and is
deliberately limited to headless logic classes (menu, KeePass converters, settings), never
the Swing UI.

Test configuration worth knowing (from `gradle/testing.gradle` and `build.gradle`):

- `useJUnitPlatform()` and `forkEvery = 1`. Each test class gets a fresh JVM, because the
  UI tests drive the process-wide `MysticCryptApplicationFrame` singleton and register
  listeners on the static `ApplicationEventBus`.
- `test` is ordered with `mustRunAfter(tasks.named("jar"))`.
- The test JVM gets `--add-opens java.base/java.util=ALL-UNNAMED` (KeePassJava2-simple's
  SimpleXML serialization reflects into `java.util`).
- `awt.robot.screenshotMethod` is set to `x11`, so a Wayland session does not raise the
  XDG portal "share your screen" dialog on every UI-test run.
- `-Dmystic.crypt.ui.test.mode` is forwarded from the Gradle JVM into the forked test JVM
  only when it is set on the command line.
- `check.dependsOn jacocoTestReport`, so any `build`/`check` run also produces the
  coverage report.

### Formatting, licensing, dependency hygiene

| Target | Runs |
|---|---|
| `make spotless-java` | `gradlew spotlessJavaApply` |
| `make spotless-misc` | `gradlew spotlessMiscApply` |
| `make license-format` | `gradlew licenseFormat` |
| `make dependencies` | `gradlew dependencies` |
| `make dependency-updates` | `gradlew dependencyUpdates` |
| `make version-catalog-format` | `gradlew versionCatalogFormat` |
| `make version-catalog-update` | `gradlew versionCatalogUpdate` |

The Spotless Java step uses `src/test/resources/spotless/importorder.properties` and the
Eclipse 4.21 config `src/test/resources/spotless/alpharo-formatter.xml`; the `misc` format
covers `*.gradle`, `*.md` and `.gitignore`. Never hand-format against the formatter.

In `gradle/licensing.gradle` the check tasks `licenseMain` and `licenseTest` are
permanently disabled (a workaround for license plugin issue #76, and it also keeps the
configuration cache happy). `licenseFormat` stays available and is the way headers get
applied; the header template is `src/main/resources/LICENSE.txt`.

### Internal plugins

Each feature plugin lives under `plugins/{name}-plugin/` and is built as a standalone
Gradle build against the host published to the local Maven repository.

| Target | Runs |
|---|---|
| `make publish-local` | `gradlew publishToMavenLocal -x test` |
| `make plugin-<name>` | `publish-local`, then `gradlew -p plugins/<name>-plugin test pluginZip`, then echoes the produced zip path |
| `make plugins` | every `plugin-<name>` target |
| `make plugins-install` | `plugins`, then copies every `build/plugin-dist/*.zip` into `$(HOME)/.config/mystic-crypt-ui/plugins` |

The Makefile `plugins:` target is the canonical inventory of internal plugins; do not
count them from any document, read that target. Each plugin directory is bound to a
`PLUGIN_*_DIR` variable at the top of the Makefile, and `PLUGIN_INSTALL_DIR` is
`$(HOME)/.config/mystic-crypt-ui/plugins`.

The host must be published locally first because the plugins compile `compileOnly`
against `io.github.astrapi69:mystic-crypt-ui:<projectVersion>` plus each library the moved
panels use directly. From `plugins/obfuscation-plugin/build.gradle`:

```gradle
dependencies {
    compileOnly "io.github.astrapi69:mystic-crypt-ui:8.2-SNAPSHOT"
    ...
    compileOnly "org.pf4j:pf4j:3.15.0"
    // required so the @Extension annotation processor runs and writes META-INF/extensions.idx
    annotationProcessor "org.pf4j:pf4j:3.15.0"
}
```

Two consequences: the plugin's host coordinate must be bumped in lockstep with
`projectVersion`, and its Java toolchain must stay at the host's level (the plugin sets
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

| Target | Runs |
|---|---|
| `make izpack-installer` | `plugins`, then `gradlew izPackCreateInstaller` |
| `make clean-build-installer` | `plugins`, then `gradlew clean build izPackCreateInstaller` |
| `make izpack-installer-signed` | `plugins`, then `gradlew createIzPackInstallerFromSignedJar` |

All three depend on `plugins` first. The Makefile explains why: "the installer ships the
internal plugin zips (pack `plugins` in `src/main/izpack/install.xml`), so they have to be
built before izpack packs them, otherwise it fails on the missing files".

### Publishing and release

| Target | Runs |
|---|---|
| `make publish` | `gradlew publish` |
| `make publish-local` | `gradlew publishToMavenLocal -x test` |
| `make tag-release` | `gradlew tagRelease` |

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
from `project.version`, build timestamp, JDK and OS, and:

```gradle
"Add-Opens"             : "java.base/java.util")
```

which is the packaged-jar counterpart of the test JVM's `--add-opens`, again for
KeePassJava2-simple's reflective serialization.

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

`make izpack-installer` runs `izPackCreateInstaller` alone. For it to succeed, `build`
must already contain:

- `build/resources/main/...`, that is a completed `processResources`, for the launchers,
  the desktop entry and the icon;
- `build/signed/mystic-crypt-ui-<version>-all.jar`, that is a signed fat jar, which
  requires `createIzPackInstaller=true` or a non-SNAPSHOT version **and** the five
  `release.mystic-crypt.*` properties;
- the plugin zips under `plugins/*/build/plugin-dist/`, which the `plugins` prerequisite
  provides.

The two other targets take care of more of that themselves:

- `make clean-build-installer` runs `clean build izPackCreateInstaller`, so the build
  directory is repopulated from scratch first.
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

## Publishing to Sonatype

`gradle/publishing.gradle` builds one publication, `mavenJava`, with `artifactId` set to
the root project name, the main component plus a `sourcesJar` and a `javadocJar`, and a
full POM (name, description, url, organization, issue management, license, developer,
scm), all assembled from `gradle.properties` values.

The target repository is chosen by the same snapshot switch:

```gradle
def releasesRepoUrl = "$projectRepositoriesReleasesRepoUrl" as Object
def snapshotsRepoUrl = "$projectRepositoriesSnapshotsRepoUrl" as Object
url = releaseVersion ? releasesRepoUrl : snapshotsRepoUrl
```

Credentials are configured only for a release version, and are read from the environment
first, from the private Gradle properties second:

```gradle
username System.getenv("$projectRepositoriesUserNameKey") ?: project.findProperty("$projectRepositoriesUserNameKey")
password System.getenv("$projectRepositoriesPasswordKey") ?: project.findProperty("$projectRepositoriesPasswordKey")
```

The two key names come from `gradle.properties`: `ossrhUsername` and `ossrhPassword`.
Their values, like the signing values, belong in the private Gradle properties file or in
CI secrets, never in this repository and never in output. CI passes them as
`secrets.OSSRHUSERNAME` and `secrets.OSSRHPASSWORD`. The comment in the file explains why
the credentials block is conditional: a build that only compiles and tests a release
version, on CI or from a fresh clone, has neither and must not fail at configuration time
for that reason.

Note also `gradle/repositories.gradle`, which resolves from `mavenLocal()` first. That is
what makes `make publish-local` work as the handshake between the host and the plugin
builds.

## The release sequence

From `.claude/rules/release-workflow.md`. Gitflow applies: the release is prepared from
`develop`, tagged, and `master` carries releases. Red tests block the release, with no
exceptions and no "disable the test for this release". Do not release mid-feature.

1. **Capture state.** `git tag --sort=-creatordate | head -5` and
   `git log $(git describe --tags --abbrev=0)..HEAD --oneline --no-merges`. Show the
   summary and wait for confirmation.
2. **Pick the version** per SemVer (`feat:` minor, `fix:`/`refactor:` patch, breaking
   change major), propose it with a rationale, wait for an OK.
3. **CHANGELOG.md**, a grouped entry (Breaking / Added / Changed / Fixed / Security),
   summarised for humans. Commit as `docs: changelog for vX.Y.Z`.
4. **Bump `projectVersion`** in `gradle.properties`. This is the one edit.
5. **The full gate. Every item is mandatory and a red result aborts the release:**
   - `make test`
   - `make test-e2e` (Xvfb harness)
   - `make build-full`
   - `make izpack-installer`, the installer smoke test; the plugin set in `install.xml`
     must match the Makefile `plugins:` list
   - `make dependency-updates`, the dependency currency check. Routine patch and minor
     bumps belong to the release; major bumps get their own session and are never bundled
     in.
6. **Tag and push:** `make tag-release`, or the manual `git tag -a vX.Y.Z` plus push.
7. **GitHub release** from the CHANGELOG entry, `gh release create vX.Y.Z`.
8. **Post-release:** CHANGELOG link check, update `CLAUDE.md` if the architecture changed,
   add a `.claude/rules/lessons-learned.md` entry if anything noteworthy happened.

If tests fail right before the release: abort, fix in its own commit, restart from step 1.
Checklist items that touch safety (tests green, build successful, correct version) are
never skipped, not even on instruction. Postpone rather than ship broken.

### Two things to decide before you tag

- **Tag name.** `make tag-release` runs the grgit task in `gradle/grgit.gradle`, which
  creates the tag `RELEASE-${project.version}` with the message `New release in version
  ${version}`. The rules file also offers the manual alternative `git tag -a vX.Y.Z`.
  Those two produce different tag names, so pick one deliberately and stay consistent with
  what `git tag` already shows for this repository.
- **Signing and the installer.** As soon as `projectVersion` no longer ends in `SNAPSHOT`,
  the `jar` task starts signing and the publication gets signed. Make sure the five
  `release.mystic-crypt.*` properties are in place before the gate reaches
  `make izpack-installer`, otherwise the build prints "IzPackInstaller can not be created
  because it depends on signed jar" and the installer step fails on the missing
  `build/signed/...-all.jar`.

Also check `DEPLOYMENT-INFO.md` for the info-dialog version literals described under
"Known drift" above.

## Continuous integration

Two workflows under `.github/workflows/`:

- **`gradle.yml`** ("Java CI with Gradle"), on push and pull request against `master` and
  `develop`. Temurin JDK 25, `gradle/actions/setup-gradle`, installs Xvfb and runs
  `xvfb-run -a --server-args="-screen 0 1920x1080x24" ./gradlew build` so the AssertJ-Swing
  end-to-end tests really run instead of being skipped by their headless assumption.
  `ossrhUsername` and `ossrhPassword` are passed from repository secrets. On failure it
  uploads `build/reports/tests/test` and `build/test-results/test` for 14 days, because a
  failing UI test prints only its top stack frame to the console.
- **`mutation.yml`** ("Mutation testing"), on `workflow_dispatch`, on a weekly cron
  (Mondays 04:17 UTC) and on pull requests that touch the measured logic packages or
  `gradle/mutation-testing.gradle`. It runs `./gradlew pitest`, writes killed / survived /
  not covered / total into the job summary from `build/reports/pitest/mutations.xml`, and
  uploads the HTML report. It is kept off the push path on purpose: PIT reruns the
  covering tests for every mutation, so it is far slower than a normal build.

## Gradle build layout

`build.gradle` stays small and delegates:

```gradle
apply from: "gradle/apply-gradle-files.gradle"
```

which reads `gradle/gradle-files.list` line by line and applies each file. To add a new
build concern, add the file **and** its line in `gradle-files.list`, otherwise it is
silently not applied. The current list: `dependencies`, `formatting`, `grgit`, `izpack`,
`licensing`, `mutation-testing`, `packaging`, `publishing`, `repositories`, `testing`,
`version-catalog-update`.

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

The reason for the last line is in the comment above it: the grgit plugin starts git
processes at configuration time, which the configuration cache treats as an error by
default; degrading it to a warning keeps the build passing, and the cache entry for
affected runs is simply discarded. The two other configuration-cache accommodations are
the `jar.notCompatibleWithConfigurationCache(...)` call for signing and the disabled
license check tasks.
