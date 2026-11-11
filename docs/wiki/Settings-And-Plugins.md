# Settings and plugins

Everything that is configurable in mystic-crypt-ui lives behind one entry point: **File >
Settings...**. It is available with or without an open database, and opens a modal **Settings**
dialog with three tabs:

- **Plugins** - the installed plugins, with enable, disable and install.
- **Plugin settings** - the configuration each plugin declares for itself.
- **General** - the look and feel, the view, the automatic lock and the clipboard.

The **Plugins** tab is only usable while a database is open: installing or enabling a plugin runs
its code in this application, so without a database the tab is greyed out and its tool tip says
"Sign in first".

The dialog is closed with the **Close** button or the window's own close button; either is the
moment the general settings are written to disk and the chosen view is applied. Plugin
enable/disable state and plugin settings are not tied to that button, see the sections below.

## Where the configuration lives on disk

Everything sits in the application's own configuration directory: the folder
`.config/mystic-crypt-ui` in your home directory, on every operating system
(`~/.config/mystic-crypt-ui` on Linux and macOS).

| Path inside the configuration directory | What it holds |
|---|---|
| `settings.json` | the general settings, stored as JSON |
| `plugins/` | the installed plugin archives; the plugin system also records here which plugins are disabled, in `disabled.txt` |
| `plugin-settings/<plugin-id>.properties` | one properties file per plugin, named after the plugin id |
| `memoizedSignin.json` | the files the sign-in dialog remembers, see [[Signing-In]] |
| `memoizedKeePass.json` | the files the KeePass import and export dialogs remember, see [[import-export-keepass-database]] |

None of these files holds a password or key material.

A plugin's file only appears once its settings have been saved at least once. Until then the
plugin runs on the defaults it declares in code, and the settings dialog shows those defaults.

Two system properties can point the application somewhere else, mainly so that tests can run
against a throwaway directory: `mystic.crypt.ui.config.dir` for the configuration directory the
plugins read their settings from, and `mystic.crypt.ui.plugins.dir` for the plugins directory the
command line reads.

## The General tab

| Setting | What it does | Default |
|---|---|---|
| **Look and feel:** | A dropdown of the look and feels installed in your Java runtime, plus FlatLaf Light, FlatLaf Dark, FlatLaf IntelliJ and FlatLaf Darcula. The choice is applied immediately to every open window, so you see the result while the dialog is still open. A look and feel that turns out to be unavailable is skipped silently, the dialog stays usable. | FlatLaf Light |
| **Language (applies after restart):** | `en` or `de`. The choice is stored, but this version does not apply it: the texts that have a German translation follow the language of your system instead. | `en` |
| **View:** | `APPLICATION_PANEL`, the tree and the table filling the window, or `DESKTOP_PANE`, a desktop with the database in a window of its own. See [[The-Entry-Tree]]. | `APPLICATION_PANEL` |
| **Tooltips:** | **Show tooltips** turns the tool tips of the whole application on or off, at once. | on |
| **Save when locking:** | Whether locking the workspace writes pending changes to the file first. Off, a lock never commits a change you had not decided about. | off |
| **Lock after idle minutes (0 = off):** | After how many minutes without activity the workspace locks itself. 0 to 480. | 15 |
| **Close a locked database after minutes (0 = off):** | After how many further minutes a locked database is closed, so its decrypted content leaves memory. A database with unsaved changes is not closed this way. 0 to 480. | 15 |
| **Clear a copied password after seconds (0 = off):** | How long a password or user name copied from an entry stays on the clipboard. It is cleared only if the clipboard still holds what was copied. 0 to 300. | 20 |

All values are held in memory while you work and written to `settings.json` when you close the
dialog. The timers read the stored values on every check, so a change takes effect without a
restart. [[Signing-In]] explains locking, and [[The-Entry-Tree]] copying.

**View > Look and Feel** in the menu bar switches the look and feel too, but only for the running
session; the choice that is kept across a restart is the one made here.

## The Plugins tab

A table with the columns **Plugin** (the plugin id), **Version** and **State**, one row per
installed plugin, and five buttons below it:

- **Enable** - enables the selected plugin and starts it. Its state becomes `STARTED`.
- **Disable** - disables the selected plugin. Its state becomes `DISABLED`.
- **Install from Zip...** - see below.
- **Open plugins folder** - opens the plugins directory in your system file manager, where the
  desktop environment supports it.
- **Refresh** - re-reads the plugin list into the table.

Enabling and disabling take effect at once, not when the dialog is closed, and the state is
persisted by the plugin system itself, so it survives a restart. There is no "cancel" for it:
closing the dialog does not undo a disable.

### Installing a plugin from a zip

1. Press **Install from Zip...**. A file chooser titled "Choose a plugin zip to install" opens,
   filtered to `Plugin archives (*.zip)`.
2. Pick the plugin's zip file and confirm.
3. The archive is copied into the plugins directory (a file of the same name there is replaced),
   then loaded and started.
4. The table refreshes and the new plugin appears, normally with the state `STARTED`.

If the archive cannot be installed, an **Install failed** dialog names the reason instead. No
restart is needed: a freshly installed plugin contributes its menu items right away.

Copying the zip into the plugins directory by hand and restarting the application does the same
thing, see [[install-a-plugin]]. A plugin runs with all the rights of the application, so install
only plugins from a source you trust.

## Disabling a plugin removes what it contributed

A plugin can contribute menu items and command line commands, and disabling it withdraws both.

- **Menu entries**: right after every enable, disable or install, the "Plugins" menu is rebuilt
  from the plugins that are currently running. The old menu is removed first, so a disabled
  plugin's items disappear immediately, without a restart. When nothing is left to show, the
  "Plugins" menu itself is gone from the menu bar.
- **Command line commands**: `--cli` loads and starts the plugins from the same plugins
  directory that the Plugins tab manages. A disabled plugin is not started, so the commands it
  contributes are not part of the command line either. The commands that come from the
  mystic-crypt library are always there and are not affected by any plugin. See
  [[The-Command-Line]].

Re-enabling the plugin brings its menu items back, again without a restart.

While no database is open, or while the workspace is locked, a plugin's menu items are disabled,
unless the plugin declares that its tool works without a database. Of the plugins that come with
the application, Checksum and Password Hashing do.

## Every plugin brings its own settings

A plugin declares the settings it understands, each with a default value, and those show up on
the **Plugin settings** tab: on the left the list of plugins that have settings, by their display
name, on the right a table of their settings with the columns `setting` and `value`.

- The `setting` column is fixed, it names what the plugin declared. Only the `value` column is
  editable.
- **Apply** writes the values of the selected plugin into its properties file. Nothing is stored
  before that, so a stray keystroke in the table cannot silently change how a plugin behaves.
- **Reset to defaults** deletes that plugin's properties file, and the declared defaults apply
  again. The table shows them immediately.
- The line below the table reports what happened: how many settings the selected plugin has
  ("Console: 3 settings"), that they were saved, or that the defaults apply again. When no
  installed plugin declares settings, it says "no plugin brings its own settings" instead of
  showing an empty table.

A plugin only appears in this list when it declares a plugin id and at least one setting. A
plugin whose settings cannot be read at that moment is skipped rather than taking the whole tab
down with it. The list is collected when the dialog opens, so a plugin you enable or install in
the Plugins tab shows up in the Plugin settings tab after closing and reopening the dialog.

As an example, the internal console plugin declares `dock.height.divisor` (default `4`, the
console then docks into the bottom quarter of the screen), `dock.resizable` (default `false`) and
`buffer.max.lines` (default `2000`, the lines the console keeps). The table shows the names and
values only; what each setting means is explained on the plugin's own page, such as
[[Plugin-Console]].

## What happens to a setting whose value is unusable

Settings never block the application. Every layer falls back instead of failing:

- **A key that is missing from the file** gets the default the plugin declares filled in when the
  settings are loaded, so a plugin always sees a value for every key it declared.
- **A properties file that cannot be read** is ignored and the declared defaults are used. The
  plugin still starts.
- **A value that is not a number where the plugin expects one** is replaced by the fallback the
  plugin names at that spot. The console plugin goes one step further and refuses even a number
  that would leave no room for anything else, falling back to its default height divisor.
- **Keys the plugin no longer declares** are dropped the next time the settings are saved, so an
  obsolete entry does not linger in the file forever. Likewise, a declared key that has no value
  is written with its default.
- **A `settings.json` that cannot be read** is replaced by fresh defaults, the ones in the table of
  the General tab above. A view that the file names but this version does not know is the panel
  view.

The flip side is that a typo in a value does not produce an error message: the setting simply
behaves as though it had its default. When a plugin ignores what you set, check the value against
the plugin's page, or press **Reset to defaults** and start from the declared value again.

## See also

- [[Getting-Started]]
- [[Signing-In]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
- [[The-Command-Line]]
- [[Secure-your-passwords-with-obfuscation]]
