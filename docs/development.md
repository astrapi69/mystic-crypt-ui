# Developing mystic-crypt-ui

How to build, start and test the application from a checkout. Every target is described where it is
defined, on its rule line in the `Makefile`; `make` or `make help` lists them all, grouped. This page
says which of them to use for what. Releasing is in [build-and-release.md](build-and-release.md) and
`.claude/rules/release-workflow.md`.

## Before the first build

- **The JDK** that `.sdkmanrc` names: `sdk env` sets the shell to it. The `Makefile` uses it from
  `~/.sdkman/candidates/java/` when it is there and falls back to the shell's `JAVA_HOME` otherwise.
- **For any test run**: `Xvfb`, `fluxbox`, `wmctrl` and `xdpyinfo`. Every test task starts an X server
  and a window manager of its own with them and never uses the display of your desktop.
- **`python3`** for the two script test suites that `make plugins` runs, and **`gpg`** for the tests
  of the release key plugin.
- **`make install-hooks`**, once per clone and once per worktree: the pre-commit hook then refuses a
  commit that Spotless would reformat.

## What to run

| To | Run |
|---|---|
| see every target and what it does | `make` (the same as `make help`) |
| start the application from the last build, without building | `make start` |
| build the plugin zips (without their tests) and the jar, then start | `make run` |
| build the runnable jar only, no tests | `make build` |
| run the unit tests | `make test` |
| run the end-to-end suites | `make test-e2e` |
| watch the end-to-end suites on your own display | `make test-e2e-demo` |
| build one plugin with its tests | `make plugin-<name>`, for example `make plugin-keygen` |
| build every plugin with its tests | `make plugins` |
| run everything CI runs, and Spotless | `make build-full` |
| format the Java sources before a commit | `make spotless-java` |
| check a crypt-data or mystic-crypt candidate before it is released | `make bump-check GRADLE_FLAGS=-PuseMavenLocal` |
| merge a pull request | `make merge-pr PR=<number>` |

`GRADLE_FLAGS` passes extra Gradle options to every target, for example
`make test GRADLE_FLAGS=--info`.

## Where the development build runs

`make run` and `make start` start the build with `user.home` pointed at `ISOLATED_HOME`, by default
`~/mystic-crypt-ui-dev`: its settings, the sign-in memory and the plugins live under
`~/mystic-crypt-ui-dev/.config/mystic-crypt-ui/`, never in the profile of an installed release. The
plugin zips of the checkout are copied there on every start, replacing what an earlier start left. A
database is opened where it lies; the profile only remembers where.

```bash
make run ISOLATED_HOME=/tmp/mcu-profile
```

`make start` refuses with a message when there is no jar in `build/libs` or no plugin zip under
`plugins/*/build/plugin-dist`; `make run` builds both first.

`make plugins-install` copies the zips into `~/.config/mystic-crypt-ui/plugins`, the real profile,
which an installed release reads too. `make bootRun` does that and then fails, see #918.

## With or without tests

- `make run`, `make start` and `make build` run no tests.
- `make test` runs the unit tests of the host, `make test-e2e` builds every plugin with its tests and
  then runs the three end-to-end suites `e2eTest`, `e2eLockTest` and `e2eKdbxTest`.
- `make plugins` and `make plugin-<name>` run each plugin's tests before they package it.
- Gradle keeps the results of a test task whose inputs did not change, so a second run of an
  unchanged suite takes seconds; a SNAPSHOT jar carries no build timestamp, so it does not count as
  changed (#909).

A single test, or a single end-to-end class:

```bash
./gradlew test --tests '*PublicAccessTest*'
```

```bash
./gradlew e2eLockTest --tests '*LockCancelUnlockUiTest*'
```

An end-to-end test that installs a plugin needs that plugin's zip; a test whose zip is missing fails
and names it. `make plugins`, or the one `make plugin-<name>`, builds it. A plugin's own tests run in
its own Gradle build, against the host published to `~/.m2`:

```bash
make publish-local
```

```bash
./gradlew -p plugins/keygen-plugin test --tests '*SaveFormatTest*'
```

## What CI runs

`.github/workflows/gradle.yml` on every push and pull request: `make plugins`, `make release-jar`,
then `./gradlew build`, which runs the unit tests, the three end-to-end suites and Spotless. A green
local `make test` does not mean a green CI run: `make build-full` is the local equivalent.

## The Lethenon plugin

The plugin lives in `plugins/lethenon-plugin` and depends on the lethenon library
(`io.github.astrapi69:lethenon`, version in `gradle/libs.versions.toml`).

- `make plugin-lethenon` runs its tests, then `zipTest`, which starts the plugin from its zip in a JVM
  that has neither lethenon nor sqlite-jdbc on its class path and no display, then builds the zip.
  The zip carries both libraries under `lib/`, because the host ships neither.
- `.github/workflows/lethenon-plugin-platforms.yml` publishes the host locally and runs that `zipTest`
  on ARM Linux, macOS and Windows, where the native sqlite library has to load as well.
- Its windows are tested in the host's end-to-end suite: `LethenonPluginUiTest`,
  `LethenonWalletUiTest` and `LethenonChainFileOf88UiTest` in `src/e2eTest`, which install the zip.
- A lethenon candidate that is only published locally: publish it to `~/.m2` from the lethenon
  checkout, then `make plugin-lethenon GRADLE_FLAGS=-PuseMavenLocal`. Without the flag a plugin build
  takes nothing from `~/.m2` but the host (`gradle/plugin-repositories.gradle`, #508).
