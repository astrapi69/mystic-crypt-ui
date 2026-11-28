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
- **`python3`** for the script test suites that `make plugins` runs (its rule in the `Makefile` lists
  them), and **`gpg`** for the tests of the release key plugin.
- **`make install-hooks`**, once per clone and once per worktree: the pre-commit hook then refuses a
  commit that Spotless would reformat, and the pre-push hook a push whose commits name the private
  notes ([below](#references-to-the-private-notes)) and a push to the branch of a pull request that
  is merged or closed (AG-061), asking GitHub through `gh`. When GitHub cannot be asked, that hook
  refuses the push as well; `git push --no-verify`, which skips both checks, is used only on the
  maintainer's instruction, and only for that case.

## References to the private notes

The maintainer's private notes hold names, paths and rule ids that this public repository must not
carry, and which strings those are is private too (#1103). So the repository holds only the check,
`scripts/private_references.py`, and the patterns are in a list outside it, which a local setting
names:

```bash
git config --global mysticcrypt.privateReferences <path to the list>
export MYSTIC_CRYPT_PRIVATE_REFERENCES=<path to the list>    # wins over the key
```

The list holds one Python regular expression per line, searched in every line of a text; a line that
starts with `#` is a comment. Two places run the check:

- **The pre-push hook** checks the added lines and the message of every commit a push adds, the
  commits the remote already has left out, and names the commit, the file and the line of a finding.
- **The tool hook** in `.claude/settings.json` runs before every `Bash` call and every tool of the
  GitHub MCP server. It blocks a `gh` call whose title, body, body file or `gh api` field names the
  private notes, and an MCP write whose text does. A write into a repository the list itself names is
  not checked, since that is the private notes.

Without a setting nothing is checked, and both say so ("not checked"); a setting whose list cannot be
read refuses the push or blocks the call. A text can be checked by hand before it is sent:
`python3 scripts/private_references.py --file <file>`.

`.claude/settings.json` holds this hook and nothing else. Personal settings for this project, plugins
for example, belong in `.claude/settings.local.json`, which Claude Code applies over the committed file
and keeps out of git (its documentation, "Settings files and who they affect"). A cloud session reads
the committed file only when it works on this one repository outside a project's threads ("Settings in
cloud sessions" there), and the list has to be at the path the variable names in that session.

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
| check that no declared dependency is unused, in the host and every plugin build | `make dependency-analysis` |
| format the Java sources before a commit | `make spotless-java` |
| check a crypt-data or mystic-crypt candidate before it is released | `make bump-check GRADLE_FLAGS=-PuseMavenLocal` |
| merge a pull request | `make merge-pr PR=<number>` |

`GRADLE_FLAGS` passes extra Gradle options to every target, for example
`make test GRADLE_FLAGS=--info`.

A target that starts Gradle, directly or through another target or a script, runs only inside the build
slot: the slot script that queues the builds of a shared machine sets `GRADLE_SLOT`, and without it the
target refuses and names itself. Run such a target through the slot, or deliberately without one with
`make without-slot TARGET=<target>`. CI runners have no slot and pass.
`scripts/gradle_slot_guard_test.py` checks which targets start Gradle against the list in the `Makefile`.

The pre-commit hook follows the same rule, because its formatting check starts Gradle when a commit stages
a file Spotless covers: inside the slot it runs Gradle itself, with `GRADLE_SLOT_RUNNER` set to the slot
script (called as `<runner> <tree> <log file> <gradle arguments...>`) it queues through that, and otherwise
it refuses the commit before Gradle starts. `GRADLE_SLOT=none git commit ...` commits deliberately without
a slot. `scripts/double-build.sh` takes its slot script from the same `GRADLE_SLOT_RUNNER`.

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
which an installed release reads too - deliberate only when installing for real use (#918 removed
the broken bootRun target, which did it as a side effect).

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

`.github/workflows/gradle.yml` on every push and pull request, the required check `build`: `make plugins`,
`make release-jar`, then the compile (the end-to-end sources included), the unit tests and Spotless.
`.github/workflows/e2e.yml` runs the three end-to-end suites on every pull request, after every merge into
develop or a hotfix branch, every night and by hand; no merge waits for it. `make build-full` runs both locally.

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
