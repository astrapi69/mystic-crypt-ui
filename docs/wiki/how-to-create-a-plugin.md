# How to create a plugin

mystic-crypt-ui plugins are built on [pf4j](https://github.com/pf4j/pf4j). Every tool under the
**Plugins** menu is one: the internal plugins live under
[`plugins/`](https://github.com/astrapi69/mystic-crypt-ui/tree/develop/plugins) in the
repository, each a Gradle build of its own, compiled against the application jar and packaged as
a zip that the application loads from its plugins directory. This page walks through what such a
plugin consists of, with
[`plugins/checksum-plugin`](https://github.com/astrapi69/mystic-crypt-ui/tree/develop/plugins/checksum-plugin)
as the model. Copy an internal plugin as a template rather than starting from scratch: it is
built and tested with every change, so its build files fit the current application.

# Overview

A plugin consists of:

1. A `plugin.properties` descriptor.
2. A main class extending `org.pf4j.Plugin`.
3. One or more classes annotated `@org.pf4j.Extension`, each implementing one of the application's
   extension points in the package `io.github.astrapi69.mystic.crypt.plugin.api`.
4. Its own text, in a messages bundle of its own.
5. Its own `build.gradle` and `settings.gradle`, with a `pluginZip` task that writes the zip in
   the layout pf4j expects.

The checksum plugin, shortened:

```
plugins/checksum-plugin/
├── build.gradle
├── settings.gradle
└── src/
    ├── main/java/io/github/astrapi69/mystic/crypt/plugin/checksum/
    │   ├── ChecksumPlugin.java                  the plugin class
    │   ├── ChecksumMenuContribution.java        @Extension: the submenu and its items
    │   ├── ChecksumSettingsContribution.java    @Extension: the plugin's settings
    │   ├── ChecksumMessages.java                the plugin's text lookup
    │   ├── ChecksumPanel.java, ...              the tool windows and their logic
    ├── main/resources/
    │   ├── plugin.properties
    │   └── checksum/messages.properties
    └── test/java/...
```

# plugin.properties

At `src/main/resources/plugin.properties`; the zip carries it at its root:

```properties
plugin.id=checksum-plugin
plugin.class=io.github.astrapi69.mystic.crypt.plugin.checksum.ChecksumPlugin
plugin.version=1.0.0
plugin.provider=Asterios Raptis
plugin.description=Internal plugin providing the file and text checksum verifier
```

`plugin.id` is the plugin's identity everywhere else too: its settings file is named after it,
and a settings contribution returns the same id.

# The plugin class

```java
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.util.logging.Logger;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class ChecksumPlugin extends Plugin
{
	private static final Logger LOGGER = Logger.getLogger(ChecksumPlugin.class.getName());

	public ChecksumPlugin(PluginWrapper wrapper)
	{
		super(wrapper);
	}

	@Override
	public void start()
	{
		LOGGER.info("Checksum plugin started");
	}

	@Override
	public void stop()
	{
		LOGGER.info("Checksum plugin stopped");
	}
}
```

The `(PluginWrapper wrapper)` constructor is required: pf4j's `DefaultPluginFactory` instantiates
the plugin class with it, although pf4j marks it deprecated.

# The extension points

All four live in `io.github.astrapi69.mystic.crypt.plugin.api`. A plugin implements the ones it
needs, each in a class annotated `@Extension`.

| Extension point | What it contributes |
|---|---|
| `PluginMenuContribution` | Menu items under the **Plugins** menu: `getMenuItems()`, and with `getMenuName()` a submenu of that name to group them in (without one the items go into **Plugins** directly). Submenus are sorted by name; `getAnchor()` (`FIRST`, `BEFORE`, `AFTER`, `LAST`) and `getRelativeToMenuId()` place one elsewhere. `isUsableWithoutAVault()` decides whether the items stay usable before sign-in and while the workspace is locked; it is `false` unless the plugin overrides it. |
| `PluginSettingsContribution` | Settings shown under **File** > **Settings...**, tab **Plugin settings**: `getPluginId()`, `getDefaults()` (a `LinkedHashMap` keeps their order), `getDisplayName()` and a tooltip per key from `getDescription(key)`. The values are stored in `~/.config/mystic-crypt-ui/plugin-settings/<plugin.id>.properties`; the plugin reads them with `PluginSettings.load(PLUGIN_ID, defaults)`, which fills in every default the file lacks. |
| `PluginCommandContribution` | Commands for the command line: `getCommands()` returns picocli command objects, added next to the mystic-crypt library's commands when the application runs with `--cli`. The `mystic-crypt` launcher scripts run the library's entry point only, so they do not offer them. None of the internal plugins contributes one. |
| `VaultAccessContribution` | Entries of the plugin's own in the open database. The host hands the plugin a `VaultAccess` through `connect(...)`, which sees only the entries carrying the plugin's marker (`getEntryMarker()`), a name in the plugin's own namespace: its id without `-plugin`, then a dot, as in `lethenon.wallet`. `vaultClosing()` is called before the database is locked or closed. The rules are in [`docs/decisions/plugin-vault-access-2026-10-31.md`](https://github.com/astrapi69/mystic-crypt-ui/blob/develop/docs/decisions/plugin-vault-access-2026-10-31.md); the Lethenon plugin is the one that uses it. |

A menu contribution that opens a tool window the way every internal plugin does:

```java
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.awt.Component;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;

@Extension
public class ChecksumMenuContribution implements PluginMenuContribution
{
	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem checksumAndMac = new JMenuItem("Checksum and MAC");
		checksumAndMac.addActionListener(
			event -> openInternalFrame("Checksum and MAC", new ChecksumAndMacPanel()));
		return List.of(checksumAndMac);
	}

	@Override
	public String getMenuName()
	{
		return "Checksum";
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

The tool windows share one layout, the host's `ToolForm` (`io.github.astrapi69.mystic.crypt.ui.form`):
a short intro row, label and field pairs, a row of buttons and a result line underneath. Their
fields are the model-backed components of swing-model-components (`JMTextField`,
`JMPasswordField`, `JMCheckBox`, `JMComboBox`, ...), each bound to one model object per panel
with `LambdaModel.of(getter, setter)`, so the panel's state can be read at any moment. The work
itself belongs in a class without Swing in it, such as `ChecksumSupport`, where it can be tested
without a display. Cryptography always comes from the libraries the application ships with
(mystic-crypt, crypt-data, crypt-api, Bouncy Castle), never from code of the plugin's own.

# The plugin's own text

A plugin does not use the application's text bundle; it brings its own. The text lives in
`src/main/resources/<name>/messages.properties`, for the checksum plugin
`src/main/resources/checksum/messages.properties`, with keys named after where the text sits:

```properties
checksum.tooltip.compare.button=compares the generated checksum with the owner's checksum
```

A small class looks it up, always with a default, so that a missing key shows the default text
instead of failing the panel:

```java
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.util.ResourceBundle;

import io.github.astrapi69.resourcebundle.locale.ResourceBundleExtensions;

public final class ChecksumMessages
{
	private static final String BUNDLE_NAME = "checksum.messages";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private ChecksumMessages()
	{
	}

	public static String getString(final String key, final String defaultValue)
	{
		return ResourceBundleExtensions.getStringQuietly(RESOURCE_BUNDLE, key, defaultValue);
	}
}
```

Text shown on a component goes through it, with the default as the second argument, as the
checksum plugin does for its tooltips and dialog texts:
`ChecksumMessages.getString("checksum.tooltip.compare.button", "compares the generated checksum with the owner's checksum")`.
`ResourceBundleExtensions` comes from `io.github.astrapi69:resourcebundle-core`, which the
application provides at runtime: declare it `compileOnly` and `testImplementation`.

# The build

Each internal plugin is an independent Gradle build: the repository's own `settings.gradle` does
not include it. Its `settings.gradle` names the project and borrows the application's version
catalog, so the plugin compiles against the same library versions the application ships with:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = 'checksum-plugin'

dependencyResolutionManagement {
    versionCatalogs {
        libs {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
```

The essential parts of its `build.gradle`:

```groovy
plugins {
    id 'java'
    // the tests run on a display of the build's own, and a test class that leaves no result fails the build
    alias(libs.plugins.own.display)
    alias(libs.plugins.test.results.complete)
}

group = 'io.github.astrapi69'
version = '1.0.0'

java {
    toolchain {
        // must match the host: its classes are compiled for JDK 25
        languageVersion = JavaLanguageVersion.of(25)
    }
}

apply from: '../../gradle/plugin-repositories.gradle'

// the host version comes from the root gradle.properties, never repeated as a literal
def hostVersion = new Properties().with { properties ->
    file("../../gradle.properties").withInputStream { properties.load(it) }
    properties.getProperty("projectVersion")
}

dependencies {
    compileOnly "io.github.astrapi69:mystic-crypt-ui:$hostVersion"
    compileOnly libs.resourcebundle.core
    compileOnly libs.swing.base.components
    compileOnly libs.swing.model.components
    compileOnly libs.miglayout.swing
    // ... every other library the plugin's classes use directly, compileOnly as well

    compileOnly libs.pf4j
    // required so the @Extension annotation processor runs and writes META-INF/extensions.idx
    annotationProcessor libs.pf4j

    testImplementation libs.junit.jupiter
    testImplementation "io.github.astrapi69:mystic-crypt-ui:$hostVersion"
    testImplementation libs.pf4j
    testImplementation libs.resourcebundle.core
    testRuntimeOnly libs.junit.platform.launcher
}

test {
    useJUnitPlatform()
}

apply from: '../../gradle/test-jvms.gradle'

// packages the plugin the way the host's DefaultPluginManager expects: plugin.properties at the
// root, compiled classes and the other resources under classes/
tasks.register('pluginZip', Zip) {
    archiveFileName = "checksum-plugin-${version}.zip"
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

Everything is `compileOnly`: the application, the libraries and pf4j are all present at runtime,
and the plugin's class loader finds what the plugin does not carry itself in the application. A
library the application does not ship would have to travel inside the zip.

`gradle/plugin-repositories.gradle` takes the application jar from the local Maven repository and
everything else from Maven Central. The application is not published to a remote repository, so
it has to be published locally first. From the repository root:

```bash
make plugin-checksum
```

runs `make publish-local` (`./gradlew publishToMavenLocal -x test`) and then
`./gradlew -p plugins/checksum-plugin test pluginZip`. The zip lands in
`plugins/checksum-plugin/build/plugin-dist/checksum-plugin-1.0.0.zip`:

```
checksum-plugin-1.0.0.zip
├── plugin.properties
└── classes/
    ├── META-INF/extensions.idx
    ├── checksum/messages.properties
    └── io/github/astrapi69/mystic/crypt/plugin/checksum/*.class
```

`META-INF/extensions.idx` is the index the pf4j annotation processor writes; without it the
application finds no `@Extension` in the plugin. `make plugins` builds every internal plugin the
same way.

# Where the application looks for it

The application loads every plugin in its plugins directory, `~/.config/mystic-crypt-ui/plugins`,
when it starts. pf4j unpacks each zip into a directory of the same name beside it and loads that
directory, and an unpacked directory is used in preference to the zip: replace a plugin by
removing both. To install one:

- copy the zip into the directory and restart, or
- use **File** > **Settings...**, tab **Plugins**, **Install from Zip...**, which installs and
  starts it at once (the tab is available once you have signed in), see [Install a plugin](install-a-plugin).

`make plugins-install` builds every internal plugin, copies the zips into that directory and
removes unpacked directories left from an earlier install; `make run` starts the development
build in a profile of its own (`~/mystic-crypt-ui-dev`) with the freshly built plugins, without
touching the installed one. The command line (`--cli`) reads the same plugins directory, or the
one `-Dmystic.crypt.ui.plugins.dir=/path/to/plugins` names.

# An internal plugin in this repository

A new plugin under `plugins/` is wired in the same change that adds it:

- the `Makefile`: a `PLUGIN_<NAME>_DIR` variable, a `plugin-<name>` target, the `plugins:` list
  and a copy line in `plugins-install`; the `plugins:` list is the inventory of internal plugins;
- `src/main/izpack/install.xml`: a `singlefile` line in the `plugins` pack, and the plugin's name
  in that pack's description, which `InstallerPackDescriptionTest` counts against the pack;
- tests, with at least one end-to-end test of what the user sees;
- a plugin whose `isUsableWithoutAVault()` returns `true` is added, with its reason, to
  `PublicPluginGrantTest`, which fails on every other plugin that declares it.

The Menu Designer is the one internal plugin that is built and tested but not packed by the
installer, see [[Plugin-Menu-Designer]].

# Security

Plugins run with the full privileges of the host application: there is no sandboxing, and no
code signing in the current version. A plugin reaches the application through
`MysticCryptApplicationFrame.getInstance()`, which hands it the whole application; issue
[#678](https://github.com/astrapi69/mystic-crypt-ui/issues/678) is open to give plugins a
narrower way in. Install only plugins from sources you trust, and document this clearly for your
own plugin's users too.
