# How mystic-crypt-ui is tested

This page describes the test setup of this repository: which layers exist, what belongs
in each of them, how to run them, and what the CI gates actually check. The binding norms
(coverage targets, the round-trip duty, test naming) live in
[`.claude/rules/quality-checks.md`](../.claude/rules/quality-checks.md); this page
documents the machinery that enforces them.

Every command below assumes the repository root as the working directory.

---

## 1. The layers

`.claude/rules/quality-checks.md` draws the pyramid like this:

```
        /  E2E   \      Swing UI tests (AssertJ-Swing shape), few, critical flows
       / Integration\   whole features through real files/keystores (tmp dirs)
      /  Unit Tests  \  JUnit 5, business logic in isolation
     / Mutation (PIT) \ verifies tests actually catch bugs
```

In this repository the layers map onto directories as follows.

| Layer | Where it lives | What belongs there |
|---|---|---|
| Unit / integration, host | `src/test/java/io/github/astrapi69/mystic/crypt/**` outside the `ui` package | Vault format and file factories (`app/file/xml/`), KeePass converters (`keepass/`), menu layout (`menu/`), settings (`settings/`), the CLI wiring (`cli/`), panel model checks (`panel/`) |
| Unit / integration, plugin | `plugins/{name}-plugin/src/test/java/...` | The plugin's own support class, its settings contribution, its panel model binding, its picocli command |
| Construction smoke | `src/test/java/.../ui/*ConstructionSmokeTest.java` | Panels must build on the EDT with a valid model and lay out at least one child; no application launch |
| End-to-end (e2e) | `src/test/java/.../ui/*UiTest.java`, all extending `AbstractUiTest` | Complete user flows through the real application: sign in, tree editing, KeePass import/export, save and reopen, settings, plugin tools |
| Mutation | configured in `gradle/mutation-testing.gradle` | Reruns the headless tests against mutated logic classes; never the Swing UI |

Two properties separate the bottom of the pyramid from the top:

* Tests below the e2e layer are headless and fast. Some declare that explicitly, for
  instance `FileCryptPanelTest`:

  ```java
  static
  {
      System.setProperty("java.awt.headless", "true");
  }
  ```

* Tests at the e2e layer need a graphical display, boot the real
  `MysticCryptApplicationFrame`, and are the repository's manual-testplan equivalent.

Cryptographic tests register BouncyCastle themselves rather than relying on the
application's startup having done it. The pattern is the same everywhere:

```java
@BeforeAll
static void registerBouncyCastle()
{
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
    {
        Security.addProvider(new BouncyCastleProvider());
    }
}
```

Fixtures are real: `@TempDir` directories, real keystores, real vault files, plus the
checked-in fixtures under `src/test/resources` (for example `test-db.kdbx`, used by the
KeePass import and round-trip tests, and `expected-empty-db-xml.mcrdb`).

---

## 2. Running each layer

All Make targets pin `JAVA_HOME` to a JDK 25 installation at the top of the `Makefile`,
deliberately with `:=` and not `?=`, because the shell's `JAVA_HOME` may point at an older
JDK and the JDK-25-built jar then fails with `UnsupportedClassVersionError`. If your JDK
lives elsewhere, edit that one line or invoke `./gradlew` directly with your own
`JAVA_HOME`.

| Goal | Command | What it does |
|---|---|---|
| Everyday gate | `make test` | `./gradlew test` - the whole host suite, e2e tests included |
| E2E only | `make test-e2e` | `./gradlew test --tests "io.github.astrapi69.mystic.crypt.ui.*" --rerun` |
| E2E, watchable | `make test-e2e-demo` | Same, plus `-Dmystic.crypt.ui.test.mode=demo` |
| One plugin | `make plugin-{name}` | Publishes the host to the local Maven cache, then runs `test pluginZip` in that plugin's build |
| All plugins | `make plugins` | Every `plugin-{name}` target; this list is the canonical plugin inventory |
| Coverage numbers | `make jacoco-report` | `./gradlew jacocoTestReport` (`build/reports/jacoco`) |
| Mutation | `./gradlew pitest` | Report in `build/reports/pitest` |
| Full build with all checks | `make build-full` | `./gradlew clean build`, then the runnable jar |

Note that `make test` already includes the e2e tests: the root `test` task has no package
filter. `make test-e2e` differs only in narrowing the selection to the `ui` package and
adding `--rerun`, so a green-and-up-to-date test task is executed again anyway. That
package filter also picks up the `*ConstructionSmokeTest` classes, which live in the `ui`
package but do not extend `AbstractUiTest` and never launch the application.

The plugins are **separate Gradle builds**, not subprojects: `settings.gradle` declares
only `rootProject.name = 'mystic-crypt-ui'`, and each plugin carries its own
`settings.gradle`. A plain `./gradlew test` therefore never compiles or runs plugin tests.
That is why `make plugin-{name}` depends on `publish-local`
(`./gradlew publishToMavenLocal -x test`): the plugin compiles and tests against the host
artifact from the local Maven cache.

---

## 3. The end-to-end harness

This is the part that goes wrong, so it gets the detail.

### 3.1 What the test JVM is given

`gradle/testing.gradle`:

```groovy
test {
    useJUnitPlatform()
    // the AssertJ-Swing UI tests drive a process-wide singleton (MysticCryptApplicationFrame)
    // and register listeners on the static ApplicationEventBus; run each test class in its own
    // fresh JVM so that static/singleton state from one class cannot contaminate the next - this
    // is what makes the full UI suite pass, not just each test in isolation
    forkEvery = 1
    testLogging {
        exceptionFormat = 'full'
        showStackTraces = true
        events 'failed'
    }
}
```

`forkEvery = 1` is a correctness setting, not a performance knob. One JVM per test class
is what keeps the singleton frame and the static event bus from leaking between classes.
Removing it makes classes pass in isolation and fail in a suite run.

`build.gradle` adds three more things to the test task:

```groovy
tasks.named("test") {
    it.mustRunAfter(tasks.named("jar"))
    // KeePassJava2-simple's SimpleXML-based serialization reflects into java.util (e.g. UUID)
    // at runtime, which the JDK 9+ module system blocks without this on JDK 17+
    it.jvmArgs "--add-opens", "java.base/java.util=ALL-UNNAMED"
    if (System.getProperty("mystic.crypt.ui.test.mode") != null) {
        it.systemProperty "mystic.crypt.ui.test.mode", System.getProperty("mystic.crypt.ui.test.mode")
    }
    // in a Wayland session java.awt.Robot (used by AssertJ-Swing) takes screenshots through the
    // XDG desktop portal, which pops up a "share your screen" permission dialog on every UI-test
    // run - force the X11 path instead (the Swing windows run on XWayland anyway); ignored on
    // plain X11 and in headless environments
    it.systemProperty "awt.robot.screenshotMethod", "x11"
}
```

The `mystic.crypt.ui.test.mode` forwarding is what makes `make test-e2e-demo` work: a
system property on the Gradle JVM does not reach the forked test JVM by itself.

### 3.2 The display

`AbstractUiTest` opens with an assumption, not an assertion:

```java
// no display, no UI test: skips cleanly on headless CI runners instead of failing there
Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
    "UI tests need a graphical display and are skipped in headless environments");
```

So a headless run reports the e2e tests as skipped and stays green. This is convenient and
also a trap: a suite that "passed" on a machine without a display has not exercised a
single UI flow. Check the skip count, not just the exit code.

In CI a display is provided explicitly (`.github/workflows/gradle.yml`):

```yaml
- name: Install Xvfb
  run: sudo apt-get update && sudo apt-get install -y xvfb
- name: Execute Gradle build
  # xvfb-run provides a virtual display so the AssertJ-Swing e2e UI tests really run
  # in CI (without it they would be skipped by their headless assumption)
  run: xvfb-run -a --server-args="-screen 0 1920x1080x24" ./gradlew build
```

**Locally**, `.claude/rules/quality-checks.md` and `.claude/rules/lessons-learned.md` state
the rule plainly: the full Swing e2e suite needs Xvfb plus fluxbox plus JDK 25 plus
`forkEvery=1`, and running it against a live `:0` display hangs. The `make test-e2e`
target itself does **not** start a display, and this repository contains no harness script
that does; the virtual display has to be put in front of the command, the way CI does it.

The test sources carry the counter-evidence of what happens when that rule is ignored.
`AbstractUiTest` and `UiTestSpeed` both describe their environment as "this shared, live
desktop display (no isolated Xvfb in this environment)", and both contain code that exists
only to survive it:

```java
/**
 * Raises and focuses the given dialog. On this shared, live desktop display (no isolated Xvfb
 * in this environment) a window manager focus race can otherwise leave it without OS focus
 */
private void raiseAndFocus(DialogFixture dialogFixture)
{
    dialogFixture.moveToFront();
    dialogFixture.focus();
    UiTestSpeed.windowManagerSettle();
}
```

Three hazards on a live display are documented in the code and worth knowing before you
debug a hang:

1. **Focus races.** A real window manager can hand focus elsewhere between raising a
   dialog and clicking in it. `UiTestSpeed.windowManagerSettle()` keeps a non-zero pause
   even in fast mode for exactly this, and it is documented as a stability wait rather
   than user-pacing.
2. **Modal dialogs.** `SignInDialogSteps` states it: clicks whose listener opens a modal
   dialog are dispatched with `SwingUtilities.invokeLater`, because blocking on them
   through `GuiActionRunner.execute` "would deadlock the test thread against a dialog it
   is itself responsible for driving". Every step class follows this convention.
3. **`EXIT_ON_CLOSE`.** The application frame is configured to exit the JVM when closed,
   and the frame registers a `CloseWindow` adapter whose `windowClosing` and
   `windowClosed` both call `System.exit(0)`. Left in place, teardown kills the test JVM
   and the finished test is reported as "skipped". The harness defuses both (see below).

Pixel-coordinate robot input is avoided throughout for the same reason: text is set
through the components' own EDT API, which "fires the same listeners as typing", because
robot input "proved unreliable on this shared, live desktop display".

### 3.3 Per-test isolation

`AbstractUiTest.setUpUiTest` / `tearDownUiTest` give every test:

* a fresh `user.home`, pointed at a new temp directory, so the app under test never
  touches the real `~/.config/mystic-crypt-ui` (memoized sign-in, installed plugins,
  configuration). The original value is restored in teardown.
* a fresh AssertJ-Swing robot (`BasicRobot.robotWithNewAwtHierarchy()`). The
  `FailOnThreadViolationRepaintManager` is deliberately not installed, because
  `StartMysticCryptApplication` constructs the frame off the EDT and the tests mirror
  that.
* a reset `MysticCryptApplicationFrame` singleton. The private static `instance` field is
  nulled by reflection before and after each test, so each test boots a completely fresh
  application.

Teardown order matters and is commented in place: first `appThread.join(15000)` so the
application thread can finish building the UI, then dispose all windows (removing the
`CloseWindow` listeners first), and only then

```java
// NOT robot.cleanUp(): the plain cleanUp also disposes windows via a window-closing
// path, and the application frame is configured with EXIT_ON_CLOSE - that kills the
// whole test JVM mid-teardown (the test then shows up as "skipped").
robot.cleanUpWithoutDisposingWindows();
```

If you see a UI test reported as skipped that clearly ran, suspect a `System.exit` during
teardown rather than an assumption.

### 3.4 Launching and driving the application

`launchApplication()` starts `MysticCryptApplicationFrame::new` on a daemon thread (the
constructor blocks on the modal sign-in dialog), finds the sign-in dialog by title with a
15 second timeout, and then downgrades the frame's close operation to `DISPOSE_ON_CLOSE`.
The dialog matcher requires `isShowing()` as well as the title, because the title is set
by the `JDialog` constructor before the content pane is filled.

Most tests do not start there but at one of the composed entry points:

```java
protected ApplicationSteps signInWithExistingDatabase(File databaseFile, String masterPassword)
{
    SignInDialogSteps signIn = launchApplication();
    signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(masterPassword)
        .browseApplicationFile(databaseFile).requireOkEnabled().okAndAwaitSignIn();
    return new ApplicationSteps(robot).awaitSignedIn();
}
```

The flow objects are `SignInDialogSteps`, `CreateMasterKeySteps` and `ApplicationSteps`:
one method per thing a real user does, composed by the tests into complete flows. New e2e
tests add a step method there rather than reaching into components directly.

For use cases that only *start* from an existing database, `createDatabaseFileHeadless`
creates the vault file through the persistence layer without the UI; the UI creation flow
itself is covered by `CreateNewDatabaseUiTest`. For flows that span a restart,
`shutdownApplication()` joins the app thread, disposes the windows and resets the
singleton so the same test can launch the application again. `SaveAndReopenDatabaseUiTest`
is the round-trip this exists for: import, save, shut down, sign in again, assert the data
is still in the tree.

### 3.5 How components are found

The main frame is normally never made visible. Menu actions are therefore not clicked
through the menu bar but fired through the item's **stable component name**:

```java
/** Finds a menu item by its stable name and fires it (the menu bar is never shown in tests) */
private void clickMenuItem(String menuItemName)
{
    JMenuItem menuItem = robot.finder().findByName(menuItemName, JMenuItem.class, false);
    SwingUtilities.invokeLater(menuItem::doClick);
}
```

Those names come from the `MenuId` enum. `DesktopMenu` builds every item with
`.name(MenuId.SOMETHING.propertiesKey())`, and the steps call
`clickMenuItem(MenuId.IMPORT_KEEPASS.propertiesKey())`. The last argument of `findByName`
is AssertJ-Swing's `requireShowing` flag, passed as `false` here precisely because the
menu bar is not on screen; lookups for components in a visible window pass `true`.

Panel components are named in the production sources, one `setName` per component, for
example in `NewMasterPwFilePanel`:

```java
btnOk.setName("btnOk");
cbxMasterPw.setName("cbxMasterPw");
txtMasterPw.setName("txtMasterPw");
btnApplicationFileChooser.setName("btnApplicationFileChooser");
```

and used through the fixtures by that name: `dialog.button("btnOk")`,
`dialog.checkBox("cbxMasterPw")`, `dialog.textBox("txtMasterPw")`,
`frame.textBox("txtSourceFile")`, or via the finder when a specific type is needed:

```java
frame.robot().finder().findByName("pwdFile", javax.swing.JPasswordField.class, true)
    .setText(PASSPHRASE);
```

These names are load bearing beyond the tests. `MenuLayoutSupport.ACTION_ID` uses the
component name as the action id in the exported menu XML:

```java
public static final Function<AbstractButton, String> ACTION_ID = button -> button
    .getName() != null ? button.getName() : "text:" + button.getText();
```

Renaming a component therefore breaks both the tests that address it and the saved menu
layouts that reference it. Treat a component name as API.

What cannot be addressed by name is addressed by title, always combined with
`isShowing()`: dialogs through `WindowFinder.findDialog(...)` with a `GenericTypeMatcher`,
file choosers through `JFileChooserFinder`, plugin tool windows through the desktop pane's
internal frames (`findInternalFrameByTitle`). Buttons in generic message dialogs are
matched by text (`JButtonMatcher.withText("OK")`), since those are created by Swing and
carry no name.

Waiting is done on conditions, never on fixed sleeps, for example:

```java
Pause.pause(new Condition("application model is signed in")
{
    @Override
    public boolean test()
    {
        MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame.getInstance();
        return applicationFrame != null && applicationFrame.getModelObject() != null
            && applicationFrame.getModelObject().isSignedIn()
            && applicationFrame.getApplicationPanel() != null;
    }
}, 15000);
```

### 3.6 Pacing: fast mode and demo mode

`UiTestSpeed` has two modes, selected with the `mystic.crypt.ui.test.mode` system property:

* **fast** (default): `UiTestSpeed.step()` is a no-op, `windowManagerSettle()` pauses 80 ms.
* **demo** (`-Dmystic.crypt.ui.test.mode=demo`, i.e. `make test-e2e-demo`): `step()` pauses
  600 ms and `windowManagerSettle()` 400 ms, so a run is paced like an unhurried user and
  can be watched on screen. This is the fastest way to see what a failing flow actually
  does.

### 3.7 Tests that need built plugin zips

E2E tests for plugin features install a **built plugin zip** into the isolated config
directory before launching the app:

```java
protected void installPluginRequiringItBuilt(Path pluginZip) throws IOException
{
    Assumptions.assumeTrue(Files.exists(pluginZip),
        "plugin zip " + pluginZip + " not built - run 'make plugins' first");
    File pluginsDir = new File(tempHome, ".config/mystic-crypt-ui/plugins");
    ...
}
```

The zip paths are constants on `AbstractUiTest`
(`plugins/{name}-plugin/build/plugin-dist/{name}-plugin-1.0.0.zip`, matching the
`pluginZip` task and the `version = '1.0.0'` in each plugin build). Because the check is a
JUnit assumption, a plain `./gradlew test` without built plugins skips these tests instead
of failing. Run `make plugins` first if you want them to mean anything.

---

## 4. Plugin suite versus host suite

The split follows the layer rule from `.claude/rules/architecture.md`: logic lives in support
classes, the UI only wires. How much of the pattern below a given plugin has depends on how much
it does. A plugin whose window only shows something, such as the console or the menu designer,
has a `*SettingsContributionTest` and nothing else; the plugins that carry crypto have all four
kinds. `ls plugins/*/src/test/java/**/` is the honest answer for any given day.

**The plugin's own suite** (`plugins/{name}-plugin/src/test`, run by `make plugin-{name}`
or `make plugins`) is headless and covers the plugin in isolation:

* `*SupportTest` - the real work, through real files and real crypto. `FileCryptSupportTest`
  encrypts and decrypts files of 0, 1, 16, 17, 4096 and 100000 bytes in a `@TempDir` and
  asserts they come back byte for byte, that a wrong passphrase is refused rather than
  answered with rubbish, and that a tampered file does not open at all.
* `*SettingsContributionTest` - the plugin's own configuration: its plugin id, its
  defaults, the effect of a stored value. It redirects
  `PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY` to a `@TempDir` and restores it
  afterwards. This is where the "no dead settings, no hidden settings" rule from
  `architecture.md` gets enforced in code.
* `*PanelTest` / `*PanelBindingTest` - the panel is constructed headless and checked
  against its model: what is typed into a component is what the buttons act on.
* `*CommandTest` - the picocli command the plugin contributes.

**The host suite** covers everything the plugin cannot see by itself:

* that the plugin is discovered, loaded and started from its packaged zip, and that it
  contributes its items to the "Plugins" menu (`PluginLoadingUiTest`);
* that the plugin's tool opens from the menu and does its job through the real UI
  (`ApplicationSteps.openPluginTool(...)`, then driving the panel by component name; see
  `FileCryptFileUiTest`, `ChecksumPluginUiTest`, `KeygenPluginUiTest` and the other
  `*PluginUiTest` classes);
* the host's own plugin management: installing a plugin zip through the Settings dialog
  (`SettingsInstallPluginUiTest`), disabling and re-enabling a plugin
  (`SettingsEnablePluginUiTest`);
* the host-side aggregation of plugin contributions headlessly, for instance
  `MysticCryptUiCliTest`, which asserts that the command line is built from the library's
  root command plus the plugin contributions and that a broken contribution never takes
  the whole command line down.

Rule of thumb: if it can be answered without the host, it belongs in the plugin's suite.
If the question is "does the host really pick this up", it belongs in an e2e test in
`src/test/java/.../ui/`.

A new plugin wires its Makefile target, the izpack installer config and the test wiring in
the same change (`.claude/rules/architecture.md`), and the `plugins:` target in the
`Makefile` is the canonical inventory - count plugins there, not from this page.

---

## 5. Mutation testing

Configured in `gradle/mutation-testing.gradle`, run with `./gradlew pitest`, report in
`build/reports/pitest`.

The scope is deliberately narrow:

```groovy
targetClasses = [
        "io.github.astrapi69.mystic.crypt.menu.*",
        "io.github.astrapi69.mystic.crypt.keepass.KeePassEntryConverter",
        "io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverter",
        "io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings",
        "io.github.astrapi69.mystic.crypt.settings.PluginSettings"
]
// only the headless tests - excludes the whole ...crypt.ui package with its Swing tests
targetTests = [
        "io.github.astrapi69.mystic.crypt.menu.*",
        "io.github.astrapi69.mystic.crypt.keepass.KeePassEntryConverterTest",
        "io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverterTest",
        "io.github.astrapi69.mystic.crypt.settings.MysticCryptSettingsTest",
        "io.github.astrapi69.mystic.crypt.settings.PluginSettingsTest"
]
threads = 4
outputFormats = ["HTML", "XML"]
timestampedReports = false
```

The reasons, all recorded in that file:

* The Swing UI is never mutated. PIT reruns the covering tests for every single mutation,
  and the e2e tests need a display and take minutes per run; mutations in layout and
  wiring code are usually equivalent anyway, producing noise instead of insight.
* `PluginSettingsPanel` and `MysticCryptUiCli` were measured and left out again: almost
  all mutations in the panel hit `setName`/`add`/`setText` calls no assertion can see, and
  the CLI class is plumbing around the pf4j plugin manager whose calls only become
  observable with plugin zips actually installed. Both keep their own tests.
* The application file format in `...crypt.app` and the `KdbxCreds` round trip are out for
  now because their tests depend on a security provider and on fixtures another test class
  prepares, so they pass in a full suite run but not in PIT's own forked JVM. That is a
  real coupling in those tests, which PIT simply made visible.
* One surviving mutant in `MenuLayoutSupport.harvestActions` is documented as equivalent:
  turning `index < menuBar.getMenuCount()` into `<=` makes `getMenu(count)` return null,
  which the null guard below already handles.
* There is deliberately **no** `mutationThreshold` yet. The plan recorded in the file is to
  measure first, then set it just below what was reached, so it guards against regressions.

**When it runs:** weekly and on demand, plus on pull requests that touch the measured code.
`.claude/rules/quality-checks.md` adds: nightly or before a release, not per commit;
surviving mutants in crypto, vault format or key handling get tests immediately, surviving
mutants in trivial code are ignored or documented.

---

## 6. CI workflows

Two workflows, both under `.github/workflows/`.

### `gradle.yml` - "Java CI with Gradle"

Triggers: push to `master` or `develop`, and pull requests against `master` or `develop`.
Runner: `ubuntu-latest`, Temurin JDK 25, `gradle/actions/setup-gradle@v3`.

The gate is a single `./gradlew build` under `xvfb-run`. From `./gradlew build --dry-run`, the
task graph is, in order:

```
:generateEffectiveLombokConfig
:compileJava :processResources :classes :jar :assemble
:compileTestJava :processTestResources :testClasses
:test
:jacocoTestReport
:licenseMain :licenseTest :license
:spotlessJava :spotlessJavaCheck :spotlessMisc :spotlessMiscCheck :spotlessCheck
:check :build
```

So what this gate really checks is:

* **compilation** of main and test sources on JDK 25;
* **the full test suite**, e2e tests included, because `xvfb-run` gives them the display
  their assumption requires. Without it they would silently skip;
* **`jacocoTestReport`**, wired in with `check.dependsOn jacocoTestReport` in
  `gradle/testing.gradle`. It produces XML and HTML reports. It is a report, not a
  threshold: no `violationRules` are configured anywhere in the Gradle files, so coverage
  cannot fail this build. The targets in `quality-checks.md` are a norm you uphold by
  reading the report, not a gate;
* **Spotless** (`spotlessJavaCheck`, `spotlessMiscCheck`) - formatting really does fail
  the build, so run `make spotless-java` before pushing;
* **the license header check is disabled.** `gradle/licensing.gradle` sets
  `ignoreFailures = true` and additionally disables the `licenseMain` and `licenseTest`
  tasks outright (a documented workaround for license plugin issue #76). The tasks appear
  in the graph and do nothing. `make license-format` still applies headers.

On failure, `build/reports/tests/test` and `build/test-results/test` are uploaded as the
`test-reports` artifact with 14 days retention, because the console only prints the top
stack frame of a failure.

### `mutation.yml` - "Mutation testing"

Triggers: `workflow_dispatch`, a weekly schedule (`17 4 * * 1`, Mondays 04:17 UTC), and
pull requests against `master` or `develop` **only** when paths under
`src/main/java/.../menu|keepass|settings|cli`, the matching test paths, or
`gradle/mutation-testing.gradle` change. Same runner and JDK as the build workflow, no
display needed because the UI is out of scope.

It runs `./gradlew pitest`, then, with `if: always()`, parses
`build/reports/pitest/mutations.xml` and writes a killed / survived / not covered / total
table into the GitHub job summary, so a run can be judged without downloading anything. If
no report was produced, it says so in the summary. The HTML report is uploaded as the
`pitest-report` artifact with 14 days retention.

Note that the summary step reports numbers but does not fail on them, and PIT itself has no
threshold configured. This workflow currently informs, it does not block.

---

## 7. Conventions to follow when adding tests

* **Naming**: `methodUnderTest_expectedOutcome_whenCondition`, or a JUnit 5 `@DisplayName`.
  The name describes behavior, not implementation. Both styles are in use, for example
  `getResultText_isTheModelsMessage_whenNothingHasRunYet` and
  `importedDataIsStillThereAfterSaveAndReopen`.
* **Real interfaces, real fixtures**: `@TempDir` files and keystores, invoke the tool the
  way the app invokes it. Mocks only for genuinely external things.
* **Edge and boundary cases** belong in one `@ParameterizedTest` with speaking display
  names, not in copied test methods (see `.claude/rules/tdd.md`); `FileCryptSupportTest`'s
  `@ValueSource(ints = { 0, 1, 16, 17, 4096, 100_000 })` is the shape to copy.
* **Bug fixes start with a failing reproduction test** that stays in the repository as a
  regression guard.
* **Vault format and file encryption changes owe a real round trip** through the real app
  path, not only unit tests, including the legacy migration path when touched
  (`.claude/rules/quality-checks.md`). `SaveAndReopenDatabaseUiTest` and
  `LegacyDatabaseMigrationUiTest` are the existing examples.
* **Every user-visible UI change needs at least one e2e test**; add the interaction as a
  method on the relevant `*Steps` class so the next test can reuse it.
* **Give new components a stable `setName`** in the production source. It is how tests and
  the saved menu layouts address them.
