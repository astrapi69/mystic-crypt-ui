# Menu Designer

## What it is for

The Menu Designer shows the application's own menu bar as xml and lets you rearrange it:
reorder the menus, rename entries, group items into a submenu of your own, drop the ones you
never use. It is the tool to reach for once the menu bar has grown, because every installed
plugin adds its own submenu under **Plugins** and the entries you actually use every day end up
buried. It is not the tool for changing what an entry does: the designer only moves, renames
and hides commands that already exist, it never creates a new one.

## Where to find it

**Plugins** menu, submenu **Menu Designer**, item **Menu Designer**.

The tool opens as a window inside the main window. If the application is currently showing the
database panel it switches into the desktop view to do so; you get back to the database with
**View** > **View mode** > **Switch to panel mode** (Alt+F7).

## Using it

1. Open the tool. With the setting `export.on.open` at its default the editor is already filled
   with the menu that is currently in place and the status line under the buttons reads
   `exported the current menu`. If it is empty, click **Export current menu**. When the export
   fails the status line reads `export failed: ` followed by the reason.

2. Edit the xml in the text area. The layout is one `<menubar>` element with `<menu>` children,
   which in turn hold `<item>`, `<checkbox>`, `<radio>`, `<separator>` and nested `<menu>`
   entries:

   ```xml
   <menubar id="global.menu.bar">
     <menu id="global.menu.file" text="File">
       <item id="global.menu.file.save" text="Save" action="global.menu.file.save"/>
       <separator/>
       <item id="global.menu.file.exit" text="Exit" action="global.menu.file.exit"/>
     </menu>
   </menubar>
   ```

   Three attributes carry the meaning: `id` is the entry's own identity, `text` is the label the
   menu shows, and `action` names the command the entry runs. Reordering, renaming a `text`,
   moving an entry into a different menu and deleting an entry are all safe as long as you leave
   `action` alone. An entry without an `action` attribute falls back to its `id`; for the
   application's own entries the two carry the same value anyway. The
   export also carries `mnemonic`, `accelerator`, `toolTip` and `icon` where the original entry
   had them.

   Entries contributed by a plugin have no id of their own in the application, so they are
   exported with an action of the form `action="text:Console"`, built from the label the plugin
   gives the item.

   Every menu, entry and the `<menubar>` itself needs an `id`; the schema requires it, so an
   entry you add by hand without one is rejected by **Validate**. An ampersand inside a `text`
   marks the following letter as the keyboard mnemonic, which is why the export doubles a
   literal ampersand in a label.

3. Click **Validate**. The status line reads `valid`, or the number of findings and the first
   one, in the shape `2 problem(s): ...`. Validation checks the document against the menu
   schema. It does not check that the commands you referenced exist.

4. Click **Apply now** to rebuild the menu bar of the running application from the editor
   content. The status line reads `applied to the running menu`, and the new menu bar is
   immediately usable. If the xml cannot be read the menu bar stays as it is and the status line
   reads `apply failed: ` with the reason, for example
   `apply failed: Unknown menu element <nonsense>, expected one of menubar, menu, item, checkbox, radio, separator, popup, toolbar, tray`
   or `apply failed: Invalid menu xml: ...`. Applying writes nothing to disk.

5. Click **Save as my menu** to keep the layout. It is written to `menubar.xml` in the
   application's configuration directory and the status line names the full path, for example
   `saved to /home/you/.config/mystic-crypt-ui/menubar.xml`. From the next start on, this layout
   replaces the menu the application builds for itself.

6. Click **Reset to standard** to drop a saved layout again. The status line reads
   `removed the saved menu, the standard menu returns on the next start`, or
   `there was no saved menu` when there was nothing to remove.

## Settings

| Key | Default | What it changes |
| --- | --- | --- |
| `export.on.open` | `true` | Whether the editor is filled with the current menu the moment the tool opens. Leave it on for normal use. Turn it off if you keep your layout in an external editor and only paste it in, so the tool stops overwriting what you paste. |

Edited under **File** > **Settings...**, tab **Plugin settings**, entry **Menu Designer** in the
list on the left. Change the value in the table and press **Apply**; nothing is stored until you
do. **Reset to defaults** removes the stored file and shows the declared default again.

Only the literal value `false`, in any capitalisation, switches the export off. Every other
value, a typo included, counts as `true`.

## On the command line

This tool has no command line side yet. It contributes no command to `--cli`, and none of the
commands the mystic-crypt library brings along come close: they are about hashing, keys, key
exchange, checksums, certificates, key conversion and obfuscation, not about the user interface.

What the command line is good for here is the layout file itself, which is plain xml in the
configuration directory:

```console
$ ls -l ~/.config/mystic-crypt-ui/menubar.xml
-rw-r--r-- 1 you you 4821 Aug 26 10:12 /home/you/.config/mystic-crypt-ui/menubar.xml
$ rm ~/.config/mystic-crypt-ui/menubar.xml
```

Removing the file does exactly what **Reset to standard** does: the next start builds the
standard menu again. Copying the file to another machine carries your layout over; it holds menu
labels and command ids only, nothing about your database or your keys.

## Limits and pitfalls

- **It arranges commands, it does not write them.** An entry whose `action` names something the
  running menu does not offer is still built, but it stays greyed out instead of breaking the
  menu. An item that is greyed out after applying a layout is nearly always a mistyped or
  outdated `action`.
- **Not every entry carries a command.** A purely decorative entry has none, so nothing is
  registered under its id. Pointing another entry's `action` at it produces the same greyed-out
  item as a typo.
- **Save does not validate.** You can save xml that **Validate** would reject. A layout file
  that cannot be read or cannot be built is ignored at the next start: the standard menu comes
  back and the reason goes into the log only. So a saved layout that "does nothing" is usually a
  broken layout, not a lost one. Validate, then apply, then save.
- **The layout is the whole menu bar, not a patch on it.** Whatever you delete is gone at the
  next start, and that includes the **Plugins** menu that holds the Menu Designer itself. If you
  lock yourself out this way, delete `~/.config/mystic-crypt-ui/menubar.xml` and start again.
- **A saved layout does not grow with your plugins.** A plugin you install or enable later
  contributes entries that your saved layout does not mention, so they do not appear at all.
  After installing or enabling a plugin, open the tool, export and save again.
- **Plugin entries are matched by their label.** Because their action reads `text:<label>`, a
  plugin version that renames its menu item breaks the link, and the entry returns greyed out.
  Renaming the `text` inside your own layout is harmless, renaming it upstream is not. Entries
  of a plugin you disable behave the same way: they stay in the menu, greyed out, rather than
  disappearing.
- **Apply and Save are independent.** Apply changes only the running session, Save changes only
  the next start. Do both if you want to keep what you are looking at. **Reset to standard**
  likewise only removes the file: the menu bar on screen stays exactly as it is until you
  restart.
- **Export takes the menu that is in place, not the original one.** Once you have applied a
  trimmed layout, exporting again gives you the trimmed menu. To get the untouched menu back
  into the editor, reset and restart the application first.
- **The editor is a plain text area.** No undo button, no syntax highlighting, no file dialog.
  Before a larger rework, export once and keep a copy of that text somewhere outside the tool.
- **A hand-written entry needs an id.** `id` is required on the `<menubar>`, on every `<menu>`
  and on every entry. Copying an existing entry and editing its `text` and `action` is the
  quickest way to get this right.
- An empty editor is not a neutral starting point: an empty document is reported as a problem by
  **Validate**, and so are text that is not xml at all, a truncated document and a document
  whose root element is not a menu element.

## Under the hood

No cryptography is involved; the tool touches nothing but the menu bar. Reading, validating and
writing the layout is done by the menu library the application already builds its menus with
(menu-action), and the xml is checked against that library's menu schema. The trick that makes a
user-editable menu possible is the action id: on export every entry gets one, on import the ids
are looked up in a registry harvested from the menu bar currently in place, and an id that
cannot be resolved yields a disabled entry rather than a failed menu. This also explains a
property worth knowing: the layout can never smuggle in a command, because the only commands it
can reach are the ones the running application already has. The saved layout is a single UTF-8
file, `menubar.xml`, in the configuration directory (`~/.config/mystic-crypt-ui` on Linux); the
setting of this tool lives next to it in `plugin-settings/menu-designer-plugin.properties`.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
