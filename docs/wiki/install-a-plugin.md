# Install a plugin

mystic-crypt-ui can be extended with plugins - `.zip` files that get picked up automatically on
startup. No installer, no restart-and-configure step beyond copying the file.

The tools that come with the application are plugins of this kind. The installer puts them into
the plugins directory, unless you deselect its plugins pack, and they show up in the **Plugins**
menu.

## Where plugins go

The plugins directory is created automatically the first time the app runs, nested under the
application's own configuration directory: `.config/mystic-crypt-ui/plugins` in your home
directory, on every operating system (`~/.config/mystic-crypt-ui/plugins` on Linux and macOS).

## Installing a plugin

There are two ways, and both end with the plugin in the plugins directory.

**From the settings dialog**, with a database open:

1. Get the plugin's `.zip` file from its author.
2. **File > Settings...**, tab **Plugins**, button **Install from Zip...**, and pick the zip.
3. The plugin is copied into the plugins directory, loaded and started at once. No restart is
   needed.

The **Plugins** tab is greyed out while no database is open: installing a plugin runs its code,
and the application does not offer that before you have signed in. [[Settings-And-Plugins]]
describes the tab, including how to disable a plugin again.

**By hand**:

1. Get the plugin's `.zip` file from its author, or build one yourself as
   [[how-to-create-a-plugin]] describes.
2. Copy the `.zip` into the plugins directory above. You don't need to unzip it - the app does
   that itself.
3. (Re)start mystic-crypt-ui.

If the plugin loaded successfully, a **Plugins** menu appears in the menu bar, before **Help**,
with whatever items the plugin contributed; a plugin that names a submenu gets one of its own
there. If the menu doesn't appear, the plugin either failed to load or didn't contribute any menu
items - nothing else in the app is affected either way.

A plugin's menu items are disabled while no database is open and while the workspace is locked,
unless the plugin declares that its tool works without a database. A plugin can also bring its
own settings, which appear on the **Plugin settings** tab of the settings dialog, and its own
commands for the command line, see [[The-Command-Line]].

## A note on trust

Plugins run with the full privileges of the application process - there is no sandboxing.
Only install plugins from sources you trust.

## Writing your own plugin

See [[how-to-create-a-plugin]].
