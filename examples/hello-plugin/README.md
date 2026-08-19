# hello-plugin

Minimal example plugin for `mystic-crypt-ui`, demonstrating the plugin API end to end. Copy this
project as a starting point for a real plugin.

## What it does

Contributes two items to the host app's "Plugins" menu:

- **Hello from Plugin** — shows a simple message dialog.
- **Open Hello Panel** — opens a real `JInternalFrame` panel inside the host's desktop pane, the
  same pattern every built-in feature of the app uses (see `HelloMenuContribution.java`).

## Build

This is a standalone Gradle project, independent of the host app's build.

```
# 1. publish the host app to your local Maven repo so this project can compile against it
cd ../..
./gradlew publishToMavenLocal

# 2. build the plugin zip
cd examples/hello-plugin
gradle pluginZip
```

Produces `build/plugin-dist/hello-plugin-1.0.0.zip`.

## Install

Drop the zip into the host app's plugins directory: `~/.config/mystic-crypt-ui/plugins/`
(created automatically the first time the app runs). Relaunch the app and sign in — the "Plugins"
menu appears once at least one plugin contributes a menu item.

## Writing your own plugin

1. A main class extending `org.pf4j.Plugin`, with the `(PluginWrapper wrapper)` constructor
   (see `HelloPlugin.java`).
2. One or more classes annotated `@org.pf4j.Extension` implementing
   `io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution`, returning the
   `JMenuItem`s you want added to the "Plugins" menu.
3. A `plugin.properties` at the resources root with `plugin.id`, `plugin.class`,
   `plugin.version`, `plugin.provider`.
4. A `Zip` task packaging `plugin.properties` at the root and your compiled output under
   `classes/` (see `build.gradle`'s `pluginZip` task) — this exact layout is what pf4j expects.

You don't need to bundle the host's own dependencies (crypt-api, crypt-data, swing-*, ...) —
plugins load with a plugin-first classloader that falls back to the host's classpath, so those
resolve automatically as long as your plugin doesn't ship its own conflicting copies.

## Security

pf4j applies no sandboxing — an installed plugin runs with the full privileges of the host JVM
process (file system, network, reflection into the host's own classes). Only install plugin
`.zip` files from sources you trust. There is no code signing or permission model in this version
of the plugin API — this is a known, explicit limitation, not an oversight.
