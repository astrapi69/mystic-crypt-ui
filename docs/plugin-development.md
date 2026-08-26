# Building an internal plugin

Every feature of this application that is not the core (vault open/save/format, sign-in, the main
frame and menu shell, the settings infrastructure) lives in an internal plugin under `plugins/`.
A plugin is a standalone Gradle build that produces one zip; the running application discovers the
zip through [pf4j](https://pf4j.org) and asks it for three kinds of contribution: menu items,
settings, and command line commands.

This page walks through building one from an empty directory to a merged change, in the order you
would actually do it. The running example is `plugins/secret-sharing-plugin`, with
`plugins/file-crypt-plugin` used wherever it shows something the sharing plugin does not have
(a panel test, a mixin-based command). The code blocks on this page come from those files; where
a block is shortened for reading, the comment above it says so, and the file itself is the
authority.

Related rules: `.claude/rules/architecture.md` (plugin-first, layers, settings visibility, model
backed panels), `.claude/rules/quality-checks.md` (test pyramid), `.claude/rules/tdd.md`.

---

## 1. Plugin or core?

New features ALWAYS belong in a plugin, unless they touch the vault format, sign-in, the main frame
and menu shell, or the settings infrastructure. If you are unsure, look at what the change imports:
if it needs `PasswordVaultFormat` or the application model, it is core; if it only needs the crypto
libraries and a window of its own, it is a plugin.

The canonical inventory of plugins is the `plugins:` target in the `Makefile`. Nothing else counts
as the list, and a new plugin is not finished until it appears there.

---

## 2. Directory layout and naming

```
plugins/{name}-plugin/                       kebab-case folder, always suffixed "-plugin"
├── settings.gradle                          rootProject.name = '{name}-plugin'
├── build.gradle                             java plugin, JDK 25 toolchain, compileOnly deps, pluginZip
└── src
    ├── main
    │   ├── java/io/github/astrapi69/mystic/crypt/plugin/{package}/
    │   │   ├── {Name}Plugin.java             the pf4j Plugin class named in plugin.properties
    │   │   ├── {Name}MenuContribution.java   @Extension, contributes menu items
    │   │   ├── {Name}SettingsContribution.java  @Extension, contributes settings
    │   │   ├── {Name}CommandContribution.java   @Extension, contributes CLI commands (optional)
    │   │   ├── {Name}Command.java            the picocli command itself (optional)
    │   │   ├── {Name}Panel.java              the Swing tool window
    │   │   ├── {Name}PanelModel.java         the panel's state
    │   │   └── {Name}Support.java            the logic, no Swing types in its signatures
    │   └── resources/plugin.properties       the pf4j descriptor
    └── test/java/io/github/astrapi69/mystic/crypt/plugin/{package}/
        ├── {Name}SupportTest.java
        ├── {Name}CommandTest.java
        └── {Name}SettingsContributionTest.java
```

Naming rules that matter beyond taste:

- The folder is `plugins/{name}-plugin/` and the Makefile target is `plugin-{name}`.
- The Java package is `io.github.astrapi69.mystic.crypt.plugin.{package}`, where `{package}` is a
  single lowercase word (`sharing`, `filecrypt`, `keystore`, `kem`). It does not have to repeat the
  plugin id.
- `plugin.id` in `plugin.properties`, the `PLUGIN_ID` constant in the settings contribution, and the
  zip file name all carry the same `{name}-plugin` string.

The license header of every Java file is the MIT header used across the repository; copy it from an
existing plugin file. See the formatting trap in section 16: the root Spotless and license tasks do
not reach into `plugins/`.

---

## 3. settings.gradle and build.gradle

`settings.gradle` is one line, and it is what makes the plugin a build of its own rather than a
subproject of the host:

```groovy
rootProject.name = 'secret-sharing-plugin'
```

The `build.gradle` of every plugin has the same shape. Below is
`plugins/secret-sharing-plugin/build.gradle` with its comments trimmed, so read the file itself
for the full text; the parts that matter are explained underneath:

```groovy
plugins {
    id 'java'
}

group = 'io.github.astrapi69'
version = '1.0.0'

java {
    toolchain {
        // must match the host: its classes are compiled for JDK 25 (class file major 69),
        // which a lower toolchain's javac cannot read from the compile classpath
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly "io.github.astrapi69:mystic-crypt-ui:8.2-SNAPSHOT"
    compileOnly "io.github.astrapi69:mystic-crypt:11.2"
    compileOnly "io.github.astrapi69:swing-base-components:5.1"
    compileOnly "io.github.astrapi69:swing-components:9"
    // the tool window binds every component to its panel model, the host carries both at runtime
    compileOnly "io.github.astrapi69:swing-model-components:1.2"
    compileOnly "io.github.astrapi69:model-data:3.2.1"
    // ShamirSecretSharingFactory lives in crypt-data, FeldmanVSS in the mystic-crypt library
    compileOnly "io.github.astrapi69:crypt-data:11.2"
    compileOnly "org.bouncycastle:bcprov-jdk18on:1.85.2"

    // the command line side: the host puts picocli on the classpath, so like everything else it
    // is only needed to compile against here
    compileOnly "info.picocli:picocli:4.7.6"

    compileOnly "org.pf4j:pf4j:3.15.0"
    // required so the @Extension annotation processor runs and writes META-INF/extensions.idx
    annotationProcessor "org.pf4j:pf4j:3.15.0"

    testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"
    testImplementation "info.picocli:picocli:4.7.6"
    // the contribution class implements the host's extension point, so the host jar has to be on
    // the test classpath to load it - for the main sources it stays compileOnly like everything else
    testImplementation "io.github.astrapi69:mystic-crypt-ui:8.2-SNAPSHOT"
    testImplementation "org.pf4j:pf4j:3.15.0"
    testImplementation "io.github.astrapi69:crypt-data:11.2"
    testImplementation "org.bouncycastle:bcprov-jdk18on:1.85.2"
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
}

test {
    useJUnitPlatform()
}
```

### The JDK 25 toolchain

The host is built with the Java toolchain from `projectSourceCompatibility` in
`gradle.properties` (25), so the host jar the plugin compiles against contains JDK 25 class files.
A lower toolchain's `javac` cannot read them off the compile classpath, and the plugin build fails
before it reaches your code. Every plugin therefore pins:

```groovy
languageVersion = JavaLanguageVersion.of(25)
```

The Makefile passes the same JDK explicitly (`JAVA_HOME := /home/astrapi69/.sdkman/candidates/java/25-tem`
at the top of the file), so `make plugin-{name}` works even when the shell's `JAVA_HOME` points
somewhere else.

### Why every dependency is compileOnly

At runtime a plugin never brings its own copies of these libraries. The host jar
(`build/libs/mystic-crypt-ui-*-all.jar`) already carries mystic-crypt, crypt-data, crypt-api,
BouncyCastle, the swing component libraries, pf4j and picocli, and pf4j's `PluginClassLoader` has
the host's class loader as its parent (see section 16). So the plugin sees all of them at runtime
without shipping anything.

That is why no plugin `build.gradle` in this repository has a single `implementation`, `api` or
`runtimeOnly` dependency for its main sources: only `compileOnly`, `annotationProcessor` and the
`test*` configurations. Shipping a second copy of a library inside the plugin zip would give you two
versions of the same class in two class loaders and the mismatch shows up as a `ClassCastException`
or a `NoSuchMethodError` at runtime, not at build time.

`mavenLocal()` is in the repository list because the host is a `-SNAPSHOT`
(`projectVersion=8.2-SNAPSHOT` in `gradle.properties`), published to the local Maven cache by
`make publish-local`. The version in the `compileOnly "io.github.astrapi69:mystic-crypt-ui:..."`
line has to be exactly that `projectVersion`.

### The annotationProcessor line and what breaks silently without it

```groovy
compileOnly "org.pf4j:pf4j:3.15.0"
// required so the @Extension annotation processor runs and writes META-INF/extensions.idx
annotationProcessor "org.pf4j:pf4j:3.15.0"
```

The pf4j jar contains `org.pf4j.processor.ExtensionAnnotationProcessor` and registers it through
`META-INF/services/javax.annotation.processing.Processor`. It runs at compile time, collects every
class annotated with `@Extension`, and writes their names into `META-INF/extensions.idx` next to the
compiled classes:

```
# Generated by PF4J
io.github.astrapi69.mystic.crypt.plugin.sharing.SecretSharingSettingsContribution
io.github.astrapi69.mystic.crypt.plugin.sharing.SecretSharingCommandContribution
io.github.astrapi69.mystic.crypt.plugin.sharing.SecretSharingMenuContribution
```

At runtime, pf4j's `DefaultExtensionFinder` uses only the `IndexedExtensionFinder`, which reads that
index file from the plugin's class loader. No index file, no extensions.

Without the `annotationProcessor` line the build is green, the zip is produced, the plugin is
discovered, loaded and started, and its `start()` log line appears. But `getExtensions(...)` returns
an empty list, so:

- the "Plugins" menu has no entry for your plugin,
- its settings do not appear in Settings > "Plugin settings",
- `--cli` does not know its commands.

pf4j logs the missing index at DEBUG level only (`Cannot find 'META-INF/extensions.idx'`), and the
application quiets pf4j logging down to warnings for CLI runs by default
(`MysticCryptUiCli.quietPluginLogging`, overridable with `-Dmystic.crypt.ui.cli.verbose=true`).
Nothing fails, nothing is printed, the feature is simply absent. If a freshly built plugin loads but
contributes nothing, check `META-INF/extensions.idx` inside the zip first:

```bash
unzip -p plugins/secret-sharing-plugin/build/plugin-dist/secret-sharing-plugin-1.0.0.zip \
  classes/META-INF/extensions.idx
```

### Test dependencies

Test dependencies are real (`testImplementation`), because the plugin's own test JVM has no host to
inherit from. Add exactly what the tests touch:

- `junit-jupiter` and `junit-platform-launcher` always,
- `mystic-crypt-ui` when a test constructs a contribution class (it implements a host interface) or
  uses `PluginSettings`,
- `pf4j` for the `@Extension` annotation on those classes,
- `picocli` when there is a command test,
- `bcprov-jdk18on` because the host registers BouncyCastle at startup and the test JVM does not, so
  tests register it themselves,
- `swing-model-components` and `model-data` when a test constructs the panel (see
  `plugins/file-crypt-plugin/build.gradle`, which has them for exactly that reason).

---

## 4. plugin.properties

`src/main/resources/plugin.properties` is the pf4j descriptor. The whole file:

```properties
plugin.id=secret-sharing-plugin
plugin.class=io.github.astrapi69.mystic.crypt.plugin.sharing.SecretSharingPlugin
plugin.version=1.0.0
plugin.provider=Asterios Raptis
plugin.description=Internal plugin for splitting a secret into shares and putting it back together
```

Read by `org.pf4j.PropertiesPluginDescriptorFinder`, which also understands `plugin.dependencies`,
`plugin.requires` and `plugin.license`; no plugin here uses those.

- `plugin.id` is the identity of the plugin everywhere: it names the settings file
  (`plugin-settings/{plugin.id}.properties`), it is what the settings contribution returns from
  `getPluginId()`, and it is what the Plugins settings tab shows and enables/disables.
- `plugin.class` must be the fully qualified name of your `Plugin` subclass. A typo here produces a
  plugin that fails to start rather than one that starts without extensions.
- `plugin.version` is the version pf4j reports, and by convention here it is the same `1.0.0` string
  as `version` in `build.gradle` and the version in the zip file name.

---

## 5. The plugin class

Minimal, and the constructor signature is not optional
(`plugins/secret-sharing-plugin/src/main/java/.../SecretSharingPlugin.java`):

```java
/**
 * Main class of the internal secret sharing plugin. The legacy {@code (PluginWrapper wrapper)}
 * constructor is required: it is what {@code DefaultPluginFactory} instantiates plugin classes with
 * in pf4j 3.15.0, despite being marked deprecated in favor of a not-yet-standard
 * {@code PluginContext} alternative.
 */
public class SecretSharingPlugin extends Plugin
{

	private static final Logger LOGGER = Logger.getLogger(SecretSharingPlugin.class.getName());

	public SecretSharingPlugin(PluginWrapper wrapper)
	{
		super(wrapper);
	}

	@Override
	public void start()
	{
		LOGGER.info("Secret sharing plugin started");
	}

	@Override
	public void stop()
	{
		LOGGER.info("Secret sharing plugin stopped");
	}

}
```

Do not put feature logic in `start()`. The extensions are constructed by pf4j when the host asks for
them, and each one wires itself.

---

## 6. The support class: the logic layer

Before any Swing or picocli code, write the class that does the work. It takes and returns plain
types, holds no Swing types in its signatures, and calls the libraries for every crypto primitive.
`SecretSharingSupport` is the pattern:

```java
public static List<String> split(final byte[] secret, final int threshold, final int totalShares)
{
	requireUsable(secret, threshold, totalShares);
	List<String> lines = new ArrayList<>();
	for (ShamirSecretSharingFactory.Share share : ShamirSecretSharingFactory.split(secret,
		threshold, totalShares, new SecureRandom()))
	{
		lines.add(encode(threshold, totalShares, share.getIndex(), share.getValue()));
	}
	return lines;
}
```

The splitting itself comes from `io.github.astrapi69.crypt.data.factory.ShamirSecretSharingFactory`.
Nothing cryptographic is hand-rolled here; the support class only validates inputs, encodes the
result, and produces messages a user can act on.

Both the panel and the command call this class and nothing else. That is what keeps the window and
the command line from drifting apart, and it is what makes the plugin testable without a display.

---

## 7. Extension point: the menu

`io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution`:

```java
public interface PluginMenuContribution extends ExtensionPoint
{
	List<JMenuItem> getMenuItems();

	default String getMenuName()
	{
		return null;
	}
}
```

`getMenuName()` decides the grouping: a non-blank name puts the plugin's items into a submenu of the
"Plugins" menu with that name, `null` or blank adds them directly to the "Plugins" menu
(`DesktopMenu.addPluginsMenu`). The host inserts the "Plugins" menu just before "Help", and only when
at least one item was contributed; a contribution that throws is logged as a warning and skipped, so
one broken plugin does not cost the others their menu.

A complete, working contribution
(`plugins/secret-sharing-plugin/src/main/java/.../SecretSharingMenuContribution.java`):

```java
@Extension
public class SecretSharingMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem splitAndCombine = new JMenuItem("Split and Combine");
		splitAndCombine.addActionListener(
			event -> openInternalFrame("Split and Combine", new SecretSharingPanel()));
		return List.of(splitAndCombine);
	}

	@Override
	public String getMenuName()
	{
		return "Secret Sharing";
	}

	private void openInternalFrame(String title, Component panel)
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
		{
			instance.switchToDesktopPane();
		}
		JInternalFrame internalFrame = JComponentFactory.newInternalFrame(title, true, true, true,
			true);
		JInternalFrameExtensions.addInternalFrameToMainFrame(panel, internalFrame, instance);
	}
}
```

`openInternalFrame` is identical in `FileCryptMenuContribution`; copy it. It switches the frame to
its desktop pane if needed and adds the panel as an internal frame. The `@Extension` annotation is
what puts the class into `extensions.idx`.

---

## 8. Extension point: the settings

`io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution`:

```java
public interface PluginSettingsContribution extends ExtensionPoint
{
	String getPluginId();

	Map<String, String> getDefaults();

	default String getDisplayName()
	{
		return getPluginId();
	}

	default String getDescription(String key)
	{
		return null;
	}
}
```

What the plugin declares here becomes an editable list of keys and values in the settings dialog's
"Plugin settings" tab, stored in its own properties file. The declared defaults are the contract: an
undeclared key is not offered for editing, and a declared key always has a value, even in a fresh
installation. `getDescription` becomes the tool tip.

The full contribution, plus the accessors the rest of the plugin uses
(`plugins/secret-sharing-plugin/src/main/java/.../SecretSharingSettingsContribution.java`):

```java
@Extension
public class SecretSharingSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "secret-sharing-plugin";

	/** How many shares are needed to rebuild the secret */
	public static final String KEY_THRESHOLD = "default.threshold";

	/** How many shares are produced */
	public static final String KEY_TOTAL_SHARES = "default.total.shares";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Secret Sharing";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_THRESHOLD, "3");
		defaults.put(KEY_TOTAL_SHARES, "5");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_THRESHOLD -> "how many shares are needed to rebuild the secret, at least two";
			case KEY_TOTAL_SHARES -> "how many shares are produced, at least as many as are needed";
			default -> null;
		};
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new SecretSharingSettingsContribution().getDefaults());
	}

	/**
	 * How many shares are needed to rebuild the secret, at least two
	 *
	 * @return the threshold
	 */
	public static int threshold()
	{
		return Math.max(2, PluginSettings.asInt(current(), KEY_THRESHOLD, 3));
	}

	/**
	 * How many shares are produced, never fewer than are needed to rebuild the secret
	 *
	 * @return the number of shares
	 */
	public static int totalShares()
	{
		return Math.max(threshold(), PluginSettings.asInt(current(), KEY_TOTAL_SHARES, 5));
	}
}
```

Points worth copying exactly:

- Use a `LinkedHashMap`: it decides the order the keys are shown in.
- Read the values through `io.github.astrapi69.mystic.crypt.settings.PluginSettings`. The
  `load(pluginId, defaults)` overload resolves the configuration directory itself, which is what a
  plugin needs: it runs in its own class loader and has no access to the application frame.
  `PluginSettings.asInt(values, key, fallback)` handles a value that is not a number.
- Clamp on read (`Math.max(2, ...)`), because a user can type anything into the settings table.
- Static accessors on the contribution class keep the key strings in one place; the panel calls
  `SecretSharingSettingsContribution.threshold()` in its constructor to start with what the user
  configured.

Two rules from `architecture.md` apply here and are checked in review: every setting must either be
editable in this list or be marked INTERNAL, and a setting the code never reads is forbidden. When
you remove a feature, remove its settings with it.

---

## 9. Extension point: the commands

`io.github.astrapi69.mystic.crypt.plugin.api.PluginCommandContribution`:

```java
public interface PluginCommandContribution extends ExtensionPoint
{
	List<Object> getCommands();
}
```

The commands are declared as `Object` on purpose, so this interface carries no compile time
dependency on picocli and a plugin that only contributes menu items never has to know the command
line exists. The returned objects are picocli commands: classes annotated with `@Command` that
implement `Runnable` or `Callable<Integer>`.

The contribution itself is four lines
(`plugins/secret-sharing-plugin/src/main/java/.../SecretSharingCommandContribution.java`):

```java
@Extension
public class SecretSharingCommandContribution implements PluginCommandContribution
{

	@Override
	public List<Object> getCommands()
	{
		return List.of(new SecretSharingCommand());
	}
}
```

The command is a normal picocli command with subcommands
(`plugins/secret-sharing-plugin/src/main/java/.../SecretSharingCommand.java`, shortened to the
skeleton):

```java
@Command(name = "share", mixinStandardHelpOptions = true, description = "Split a secret into shares and put it back together", subcommands = {
		SecretSharingCommand.SplitCommand.class, SecretSharingCommand.CombineCommand.class })
public class SecretSharingCommand implements Runnable
{

	@Spec
	CommandSpec spec;

	@Override
	public void run()
	{
		// without a subcommand the usage is the most useful answer
		spec.commandLine().usage(spec.commandLine().getOut());
	}

	@Command(name = "split", mixinStandardHelpOptions = true, description = "Split a secret into shares")
	public static class SplitCommand implements Callable<Integer>
	{
		@Option(names = { "-s", "--secret" }, arity = "0..1", interactive = true,
			description = "the secret; asked for when the value is left out")
		String secret;

		@Option(names = { "-t",
				"--threshold" }, required = true, description = "how many shares are needed to rebuild the secret")
		int threshold;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			// ... validate, then call the same support class the panel calls
			List<String> shares = SecretSharingSupport.splitText(secret, threshold, totalShares);
			shares.forEach(spec.commandLine().getOut()::println);
			return 0;
		}
	}
}
```

Conventions that are load bearing:

- Write output through `spec.commandLine().getOut()`, never `System.out`. That is what makes the
  command testable (the test injects a `StringWriter`) and what keeps piping usable.
- A secret is an `interactive = true` option with `arity = "0..1"`, so the value can be left out and
  is then asked for instead of appearing in the shell history.
- Never overwrite an existing output file; refuse instead. `SplitCommand` throws
  `IllegalArgumentException("'" + out + "' already exists")`, which picocli turns into a non-zero
  exit code.
- Call the same support class the panel calls.

How the host assembles the command line (`MysticCryptUiCli.newCommandLine`): it starts from the
mystic-crypt library's own root command and adds every contributed command as a subcommand.

```java
CommandLine commandLine = new CommandLine(
	io.github.astrapi69.mystic.crypt.cli.MysticCryptCli.class);
for (Object command : commands(contributions))
{
	commandLine.addSubcommand(command);
}
```

New general purpose CLI capability belongs in the mystic-crypt library, not in a plugin. A plugin
command is for what the library does not cover.

---

## 10. The panel and its model

The menu item opens a `JPanel`. Two rules from `architecture.md` apply:

1. No business or crypto logic in the panel. It reads its model, calls the support class, and shows
   the result or the failure message.
2. The panel holds its state in one model object, and every component is bound to it with the
   model-backed components from swing-model-components (`JMTextField`, `JMPasswordField`,
   `JMCheckBox`, `JMComboBox`, `JMSpinner`, `JMTextArea`). A combo box over an enum uses
   `EnumComboBoxModel` (see `plugins/checksum-plugin/.../ChecksumPanel.java`) rather than a
   hand built array.

The binding, from `SecretSharingPanel`:

```java
private void bindToModel()
{
	pwdSecret.setPropertyModel(LambdaModel.of(modelObject::getSecret, modelObject::setSecret));
	txtSecretFile.setPropertyModel(
		LambdaModel.of(modelObject::getSecretFile, modelObject::setSecretFile));
	chkUseFile
		.setPropertyModel(LambdaModel.of(modelObject::isUseFile, modelObject::setUseFile));
	spnThreshold
		.setPropertyModel(LambdaModel.of(modelObject::getThreshold, modelObject::setThreshold));
	spnTotalShares.setPropertyModel(
		LambdaModel.of(modelObject::getTotalShares, modelObject::setTotalShares));
	txtShares.setPropertyModel(LambdaModel.of(modelObject::getShares, modelObject::setShares));
}
```

Give every component a `setName(...)`: the names (`pwdSecret`, `spnThreshold`, `btnSplit`,
`txtShares`, `lblResult`) are how both the plugin's own panel test and the host's end-to-end tests
find the widgets. Start the panel from the configured defaults:

```java
// the tool starts with what the user configured in the settings dialog
modelObject.setThreshold(SecretSharingSettingsContribution.threshold());
modelObject.setTotalShares(SecretSharingSettingsContribution.totalShares());
```

Keep a typed password as `char[]` in the model, and clear the byte array after use:

```java
byte[] secret = secret();
try
{
	List<String> shares = SecretSharingSupport.split(secret, threshold, totalShares);
	...
}
finally
{
	Arrays.fill(secret, (byte)0);
}
```

---

## 11. The pluginZip task and the zip layout the host expects

The task is the same in every plugin, with only the archive name changed:

```groovy
// packages the plugin the way the host's DefaultPluginManager expects: plugin.properties at the
// root, compiled classes under classes/
tasks.register('pluginZip', Zip) {
    archiveFileName = "secret-sharing-plugin-${version}.zip"
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

The layout it produces, which is exactly what pf4j's `DefaultPluginClasspath` reads
(`CLASSES_DIR = "classes"`, `LIB_DIR = "lib"`):

```
secret-sharing-plugin-1.0.0.zip
├── plugin.properties                       at the ROOT of the zip, not under classes/
└── classes/
    ├── META-INF/extensions.idx             written by the pf4j annotation processor
    └── io/github/astrapi69/mystic/crypt/plugin/sharing/*.class
```

Three things to get right:

- `plugin.properties` sits at the zip root. Under `classes/` the descriptor is not found and the
  plugin is not loaded at all.
- Everything else from `src/main/resources` goes under `classes/`, so it stays reachable as a
  classpath resource; only `plugin.properties` is excluded there.
- The archive name is `{plugin-id}-{version}.zip`. That exact name is repeated in the izpack
  configuration and in the test wiring, so pick it once.

A `lib/` directory of jars would also be read by pf4j, but no plugin here ships one: everything comes
from the host (see section 3). Adding a bundled jar means adding a dependency, which needs to be
asked for first.

---

## 12. Where installed plugins live on disk

The application creates its plugin manager over a `plugins` directory inside its configuration
directory (`MysticCryptApplicationFrame.onBeforeInitialize`):

```java
File pluginsDirectory = DirectoryFactory.newDirectory(getConfigurationDirectory(), "plugins");
pluginManager = new DefaultPluginManager(pluginsDirectory.toPath());
```

Resolved, that is:

```
~/.config/mystic-crypt-ui/
├── plugins/
│   ├── secret-sharing-plugin-1.0.0.zip        the installed zip
│   └── secret-sharing-plugin-1.0.0/           pf4j expands the zip next to it
│       ├── plugin.properties
│       └── classes/...
└── plugin-settings/
    └── secret-sharing-plugin.properties       one file per plugin, written by PluginSettings
```

The same directory is used by:

- `make plugins-install` (`PLUGIN_INSTALL_DIR := $(HOME)/.config/mystic-crypt-ui/plugins`),
- the izpack installer (`target="${USER_HOME}/.config/mystic-crypt-ui/plugins/..."`),
- the settings dialog's "Install from Zip..." button, which copies the chosen zip into
  `pluginManager.getPluginsRoot()` and loads it immediately,
- the command line, which reads the same directory unless
  `-Dmystic.crypt.ui.plugins.dir=/somewhere/else` says otherwise
  (`MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY`).

pf4j itself persists the enabled/disabled state as a `disabled.txt` in the plugins directory, so the
Plugins settings tab's Enable/Disable survives a restart.

For settings, `PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY` (`mystic.crypt.ui.config.dir`)
redirects the configuration directory, which is what the settings tests use to keep out of the real
one.

---

## 13. Wiring it into the repository, in the same change

A plugin that exists only under `plugins/` is not done. The Makefile target, the `plugins` aggregate,
the installer configuration and the test wiring are part of the same commit.

### Makefile

Four edits, following the existing entries:

```make
PLUGIN_SECRET_SHARING_DIR := plugins/secret-sharing-plugin
```

```make
.PHONY: ... plugin-keystore plugin-file-crypt plugin-secret-sharing plugins plugins-install
```

```make
# build the internal secret sharing plugin zip (needs the host published locally first)
plugin-secret-sharing: publish-local
	JAVA_HOME=$(JAVA_HOME) ./gradlew -p $(PLUGIN_SECRET_SHARING_DIR) test pluginZip
	@echo "==> plugin zip: $$(find $(PLUGIN_SECRET_SHARING_DIR)/build/plugin-dist -name '*.zip')"
```

```make
plugins: plugin-obfuscation ... plugin-file-crypt plugin-secret-sharing
```

and one line in `plugins-install`:

```make
	cp $(PLUGIN_SECRET_SHARING_DIR)/build/plugin-dist/*.zip "$(PLUGIN_INSTALL_DIR)/"
```

Note what `plugin-{name}` runs: `test pluginZip`. The plugin's own unit tests run there, and only
there; the root `make test` does not reach into `plugins/` because the plugin builds are not
subprojects of the host build.

`publish-local` is a prerequisite of every plugin target because the plugin compiles against the
host's current API from the local Maven cache:

```make
publish-local:
	JAVA_HOME=$(JAVA_HOME) ./gradlew publishToMavenLocal -x test
```

### The izpack installer

`src/main/izpack/install.xml` ships the plugin zips in the `plugins` pack. Add one `singlefile`
entry and extend the pack description:

```xml
<singlefile src="../plugins/secret-sharing-plugin/build/plugin-dist/secret-sharing-plugin-1.0.0.zip"
            target="${USER_HOME}/.config/mystic-crypt-ui/plugins/secret-sharing-plugin-1.0.0.zip" />
```

The `../` in `src` is relative to the izpack base directory, which is the build directory
(`baseDir = file("$buildDirectory")` in `gradle/izpack.gradle`), so it points back at the repository
root. The zips must exist before izpack runs, which is why `make izpack-installer` depends on
`plugins`; a missing file fails the installer build.

The release checklist requires that the plugin set in `install.xml` matches the Makefile `plugins:`
list, so these two are edited together.

### Test wiring in the host

`src/test/java/io/github/astrapi69/mystic/crypt/ui/AbstractUiTest.java` holds one constant per plugin
zip, used by the end-to-end tests:

```java
protected static final Path SECRET_SHARING_ZIP = Path
	.of("plugins/secret-sharing-plugin/build/plugin-dist/secret-sharing-plugin-1.0.0.zip");
```

Add yours there, then write at least one `*UiTest` that uses it (section 15).

---

## 14. Build, install, run

```bash
make plugin-secret-sharing     # publish-local, then the plugin's tests and its zip
make plugins                   # every plugin
make plugins-install           # every plugin, copied into ~/.config/mystic-crypt-ui/plugins
make build && make run         # build the app jar and start it
make all                       # build + plugins-install + run
```

For the command line, with the plugin installed:

```bash
java -jar build/libs/mystic-crypt-ui-*-all.jar --cli share split -t 3 -n 5 --secret
java -jar build/libs/mystic-crypt-ui-*-all.jar --cli --help
```

`--cli --help` lists the library commands plus every contributed one; it is the fastest check that
a command contribution actually arrived.

---

## 15. How to test a plugin

The pyramid from `.claude/rules/quality-checks.md` applies: plugin logic is a HIGH coverage target
(>= 80 percent, mutation hardened), Swing panels are covered through flows rather than line count,
and data-critical round trips are must-have.

Where the tests live:

| What | Where | Run by |
|---|---|---|
| Support/logic, commands, settings, panel construction | `plugins/{name}-plugin/src/test/java` | `make plugin-{name}` |
| End-to-end through the real UI | `src/test/java/io/github/astrapi69/mystic/crypt/ui/*UiTest.java` | `make test-e2e` |

### Support tests: the round trip and the refusals

`SecretSharingSupportTest` is the model: parameterized happy paths, then every way it must refuse.

```java
@ParameterizedTest
@CsvSource({ "2,3", "3,5", "2,2", "5,7", "3,10" })
void anyThresholdManySharesRebuildTheSecret(int threshold, int totalShares)
{
	List<String> shares = SecretSharingSupport.splitText(SECRET, threshold, totalShares);

	assertEquals(totalShares, shares.size());
	for (int first = 0; first + threshold <= totalShares; first++)
	{
		List<String> some = shares.subList(first, first + threshold);
		assertEquals(SECRET, SecretSharingSupport.combineText(some),
			"shares " + first + " to " + (first + threshold - 1) + " have to be enough");
	}
}
```

Register BouncyCastle yourself; in the application the host does it at startup, in the test JVM
nobody does:

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

Assert on real properties, not on implementation details: that arbitrary bytes survive the round trip
byte for byte, that a share carries no part of the secret in plain sight, that a mistyped share is
refused instead of silently rebuilding the wrong secret, and that the error message says what the
user has to do about it.

### Panel tests: a panel can be constructed headless

A panel does not need a display to be built and exercised. `FileCryptPanelTest` sets headless mode in
a static initializer and then drives the panel through its component names:

```java
class FileCryptPanelTest
{

	static
	{
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	void onEncryptText_bringsTheTextBack_whenItIsDecryptedAgain()
	{
		FileCryptPanel panel = new FileCryptPanel();
		String secret = "a text that must survive the round trip - äöüß";
		type(panel, "txtPlainText", secret);
		type(panel, "pwdText", PASSPHRASE);
		type(panel, "pwdTextRepeated", PASSPHRASE);

		click(panel, "btnEncryptText");

		String encrypted = textOf(panel, "txtEncryptedText");
		assertFalse(encrypted.isEmpty(), "the encrypted text is shown");
		assertFalse(encrypted.contains("survive"),
			"the encrypted text does not hold the plain one");
		...
	}
```

with three small helpers plus a recursive finder:

```java
	private void type(Container panel, String name, String text)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		((JTextComponent)component).setText(text);
	}

	private void click(Container panel, String name)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		((AbstractButton)component).doClick();
	}

	private Component find(Container container, String name)
	{
		for (Component component : container.getComponents())
		{
			if (name.equals(component.getName()))
			{
				return component;
			}
			if (component instanceof Container child)
			{
				Component found = find(child, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
```

This works because the components are bound to a model: setting the text updates the model, and the
button acts on the model rather than reading the widgets. A panel test needs
`swing-model-components` and `model-data` as `testImplementation`.

### Command tests: through picocli, by exit code and output

Drive the command the way a terminal drives it. From `SecretSharingCommandTest`:

```java
	private int execute(String... args)
	{
		out = new StringWriter();
		CommandLine commandLine = new CommandLine(new SecretSharingCommand());
		commandLine.setOut(new PrintWriter(out));
		commandLine.setErr(new PrintWriter(new StringWriter()));
		return commandLine.execute(args);
	}

	@Test
	void splitsAndCombinesThroughTheCommandLine(@TempDir File directory) throws Exception
	{
		File sharesFile = new File(directory, "shares.txt");

		assertEquals(0, execute("split", "-s", SECRET, "-t", "3", "-n", "5", "-o",
			sharesFile.getAbsolutePath()));
		List<String> shares = Files.readAllLines(sharesFile.toPath(), StandardCharsets.UTF_8);
		assertEquals(5, shares.size(), output());

		// three of the five, given one at a time the way they would arrive
		assertEquals(0, execute("combine", "-s", shares.get(0), "-s", shares.get(2), "-s",
			shares.get(4)));

		assertEquals(SECRET, output().trim());
	}
```

Cover at least: the round trip, the refusals with a non-zero exit code
(`assertNotEquals(0, execute(...))`), that an existing output file is never overwritten, that every
subcommand answers `--help` with exit code 0, and that the contribution really offers the command
under the intended name:

```java
	@Test
	void theContributionOffersTheShareCommand()
	{
		assertEquals("share",
			new CommandLine(new SecretSharingCommandContribution().getCommands().get(0))
				.getCommandName());
	}
```

### Settings tests: point the configuration directory at a temp dir

From `FileCryptSettingsContributionTest`:

```java
	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private void store(String key, String value) throws Exception
	{
		PluginSettings.save(configurationDirectory, CONTRIBUTION.getPluginId(),
			CONTRIBUTION.getDefaults(), Map.of(key, value));
	}
```

Then assert the defaults that matter for safety (`removing the original must never be the default`)
and that a stored value is actually read back, including a nonsense value falling back to the
default:

```java
	@ParameterizedTest
	@CsvSource({ "true,true", "false,false", "yes,false" })
	void theDeleteSettingIsRead(String stored, boolean expected) throws Exception
	{
		store(FileCryptSettingsContribution.KEY_DELETE_SOURCE, stored);

		assertEquals(expected, FileCryptSettingsContribution.deleteSourceAfterEncrypt());
	}
```

### End-to-end: the plugin loaded from its zip, driven through the real UI

Every user-visible change needs an e2e test in the host
(`src/test/java/io/github/astrapi69/mystic/crypt/ui/`). The base class installs the built zip into a
per-test isolated home directory before the application starts, and skips the test (a JUnit
assumption, not a failure) when the zip was not built:

```java
	protected void installPluginRequiringItBuilt(Path pluginZip) throws IOException
	{
		Assumptions.assumeTrue(Files.exists(pluginZip),
			"plugin zip " + pluginZip + " not built - run 'make plugins' first");
		File pluginsDir = new File(tempHome, ".config/mystic-crypt-ui/plugins");
		...
	}
```

A test then reads like `SecretSharingUiTest`:

```java
	@Test
	void splitsASecretAndRebuildsItFromEnoughSharesThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(SECRET_SHARING_ZIP);

		File databaseFile = new File(tempHome, "secret-sharing.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Split and Combine", "Split and Combine");

		GuiActionRunner.execute(() -> {
			frame.robot().finder().findByName("pwdSecret", javax.swing.JPasswordField.class, true)
				.setText(SECRET);
			frame.spinner("spnThreshold").target().setValue(3);
			frame.spinner("spnTotalShares").target().setValue(5);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnSplit").target().doClick());
		robot.waitForIdle();
		...
	}
```

`openPluginTool(menuItemText, internalFrameTitle)` clicks the item in the "Plugins" menu and waits
for the internal frame. `PluginLoadingUiTest` is the complementary test: it proves the zips are
discovered and that their menu items are present.

Run them with `make test-e2e`; the suite needs the Xvfb harness (Xvfb, fluxbox, JDK 25,
`forkEvery=1`). Running against a live `:0` display hangs.

---

## 16. Traps

### The isolated class loader

pf4j gives each plugin its own `PluginClassLoader`, and it is parent-last by default: the plugin's
own classes win, and only what it does not have is delegated to the parent, which is the host's class
loader. Consequences:

- Everything the host jar carries (mystic-crypt, crypt-data, crypt-api, BouncyCastle, the swing
  libraries, picocli, pf4j) is visible at runtime. That is why `compileOnly` is correct.
- Bundling a library inside the plugin gives you a second copy loaded by a different loader, and the
  two are different classes to the JVM. Do not.
- The host cannot reach into the plugin. A test in `src/test/java` cannot import or cast to a plugin
  class; `KeygenCurveUiTest` says it plainly: "the plugin runs in its own class loader, so the test
  cannot reach into it". E2e tests therefore assert through component names, through what the panel
  displays, and through the files it writes.
- The plugin cannot reach the application's settings object either. Read configuration through
  `PluginSettings.load(pluginId, defaults)`, which finds the configuration directory on its own.
- Static state is per plugin, not shared with the host.

### mixinStandardHelpOptions on every command

Put `mixinStandardHelpOptions = true` on the root command AND on every subcommand:

```java
@Command(name = "share", mixinStandardHelpOptions = true, ...)
@Command(name = "split", mixinStandardHelpOptions = true, ...)
@Command(name = "combine", mixinStandardHelpOptions = true, ...)
```

Without it, that subcommand has no `-h/--help` and no `-V/--version`, and a user who types
`--cli share split --help` gets an unknown-option error with a non-zero exit code instead of the
usage. It is invisible in a happy-path test, so pin it:

```java
	@ParameterizedTest
	@ValueSource(strings = { "split", "combine" })
	void everySubcommandExplainsItself(String subcommand)
	{
		assertEquals(0, execute(subcommand, "--help"));
		assertTrue(output().contains("--"), output());
	}
```

### A command name that collides with a library command

The application's command line starts from the mystic-crypt library's root command and adds the
contributed commands to it. picocli refuses a duplicate subcommand name with a
`DuplicateNameException` ("Another subcommand named 'x' already exists for command '...'"), and that
throw is not caught: `MysticCryptUiCli.commands(...)` guards a contribution that fails while
producing its commands, but `addSubcommand` runs outside that guard, and `execute` has only a
`finally`. A colliding name therefore takes down the whole `--cli` invocation, library commands
included, not just the offending plugin.

Check the names in use before you pick one:

```bash
java -jar build/libs/mystic-crypt-ui-*-all.jar --cli --help
```

The library brings its own set (hash, verify, keygen, kem, checksum, der2pem, obfuscate,
disentangle, cert, keystore, sign, verify-signature at the mystic-crypt version currently consumed,
plus picocli's `help`), and the other plugins add theirs (`filecrypt`, `keyx`, `share`). Pick a name
that is in neither list, and remember that a library update can introduce a new one: when a plugin
command duplicates a name the library later takes, the CLI breaks on the next dependency bump.
`MysticCryptUiCliTest` pins a handful of library command names for exactly this reason.

### The host version in build.gradle must match gradle.properties

`compileOnly "io.github.astrapi69:mystic-crypt-ui:8.2-SNAPSHOT"` has to match `projectVersion` in
`gradle.properties`, because that is what `make publish-local` puts into the local Maven cache. After
a release bumps the version, the plugin builds fail to resolve until they follow.

### Spotless and the license task do not reach into plugins

The plugin builds are standalone (their own `settings.gradle`, and only `id 'java'` in
`build.gradle`), and the root `settings.gradle` does not include them. `make spotless-java` and
`make license-format` run `./gradlew` in the root build, so they format the host sources only. Keep
plugin sources consistent by copying the header and the style from an existing plugin file.

### One version string in five places

`version` in `build.gradle`, `plugin.version` in `plugin.properties`, the `archiveFileName` of
`pluginZip`, the `singlefile` entries in `install.xml`, and the `*_ZIP` constant in `AbstractUiTest`
all spell out the same version. Changing a plugin's version means changing all of them, and the two
that fail quietly are the installer (a build failure only when izpack runs) and the e2e test (which
skips itself when the zip path does not exist, so the suite still reads green).

---

## 17. Checklist for the pull request

- [ ] `plugins/{name}-plugin/` with `settings.gradle`, `build.gradle` (JDK 25 toolchain, all
      dependencies `compileOnly`, the pf4j `annotationProcessor` line, `pluginZip`).
- [ ] `plugin.properties` with `plugin.id`, `plugin.class`, `plugin.version`, `plugin.provider`,
      `plugin.description`.
- [ ] The `Plugin` subclass with the `(PluginWrapper)` constructor.
- [ ] Support class with the logic, no Swing types in its signatures, crypto from the libraries.
- [ ] `@Extension` menu contribution with `getMenuName()`, and a panel bound to a model with named
      components.
- [ ] `@Extension` settings contribution: every setting editable or marked INTERNAL, every declared
      setting actually read somewhere.
- [ ] Optional `@Extension` command contribution: `mixinStandardHelpOptions` everywhere, a name that
      collides with nothing, output through `spec.commandLine().getOut()`.
- [ ] Tests: support round trip and refusals, commands through picocli, settings against a temp
      configuration directory, panel constructed headless if there is a panel, and at least one
      `*UiTest` in the host.
- [ ] Makefile: directory variable, `.PHONY`, `plugin-{name}` target, entry in `plugins:`, `cp` line
      in `plugins-install`.
- [ ] `src/main/izpack/install.xml`: `singlefile` entry plus the pack description.
- [ ] `AbstractUiTest`: the `*_ZIP` constant.
- [ ] `make plugin-{name}` green, `make test` green, `make test-e2e` green under the Xvfb harness.
- [ ] Conventional commit citing the issue, PR against `develop`.

---

## Reference: the files this page is built on

| Purpose | File |
|---|---|
| Extension points | `src/main/java/io/github/astrapi69/mystic/crypt/plugin/api/PluginMenuContribution.java`, `PluginSettingsContribution.java`, `PluginCommandContribution.java` |
| Plugin manager creation, menu refresh | `src/main/java/io/github/astrapi69/mystic/crypt/MysticCryptApplicationFrame.java` |
| The "Plugins" menu | `src/main/java/io/github/astrapi69/mystic/crypt/DesktopMenu.java` |
| Settings storage and the settings tabs | `src/main/java/io/github/astrapi69/mystic/crypt/settings/PluginSettings.java`, `PluginSettingsPanel.java`, `PluginsSettingsPanel.java`, `SettingsPanel.java` |
| The command line | `src/main/java/io/github/astrapi69/mystic/crypt/cli/MysticCryptUiCli.java` |
| Example plugin, full set of contributions | `plugins/secret-sharing-plugin/` |
| Example plugin with a panel test and a mixin command | `plugins/file-crypt-plugin/` |
| Build, install and installer wiring | `Makefile`, `src/main/izpack/install.xml`, `gradle/izpack.gradle` |
| E2e wiring | `src/test/java/io/github/astrapi69/mystic/crypt/ui/AbstractUiTest.java`, `PluginLoadingUiTest.java`, `SecretSharingUiTest.java` |
