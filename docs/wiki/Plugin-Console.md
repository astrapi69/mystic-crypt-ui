# Console

The Console is a window docked into the bottom of the application desktop that shows everything the
running application writes to its standard output and error stream. It answers one question: "what
did the program just print?", without you having to start the application from a terminal and watch
it there. Reach for it when a tool behaved oddly and you want to see the diagnostic output or a
stack trace before writing a bug report; it is a viewer only, so for anything you want to *run* on a
command line, use the application's command line mode instead.

## Where to find it

**Plugins** menu, **Console** submenu, item **Console**.

The entry only exists once the console plugin is installed. The installer's internal-plugins pack
puts `console-plugin-1.0.0.zip` into the plugins directory (`~/.config/mystic-crypt-ui/plugins` on
Linux); a manual install works the same way as for any other plugin, see
[[install-a-plugin]]. If the entry is missing, check **File > Settings... > Plugins**: the plugin is
listed there with its state, and it can be enabled, disabled or installed from a zip on that tab.

## Using it

1. Maximize the application window before you open the console. Its size and position are computed
   from your screen size, not from the window, so in a small window the console is placed below the
   visible part of the desktop and you will not see it.
2. Choose **Plugins > Console > Console**. If the application is currently showing the database as a
   tree with a details table, it switches to the desktop view first, so the tree disappears from
   sight. That is expected; the database stays open.
3. A window titled **Console** appears across the full width of the bottom quarter of the screen,
   with an empty text area. It worked when that window is there. Nothing else is reported: if no
   window appears, the plugin is not loaded, and the **Plugins** menu is where you check that.
4. Now do whatever you want to observe: open a tool, run an operation. Every line the application
   prints from this moment on appears in the text area. Output produced *before* you opened the
   console is not shown, so open it first and reproduce afterwards.
5. Select the text with the mouse and copy it with `Ctrl+C` to paste it into a bug report. There is
   no save, export or clear button.
6. To get the database tree back, use **View > View mode > Switch to panel mode** (`Alt+F7`). The
   console keeps collecting output while it is out of sight; **Switch to desktop mode** (`Alt+F6`)
   brings the desktop view with the console window back.

## Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Console** in the list on the
left. Change the value in the right-hand column, then press **Apply**; **Reset to defaults** deletes
the stored values so the defaults below apply again.

| Key | Default | What it changes |
|---|---|---|
| `dock.height.divisor` | `4` | The fraction of the screen height the console takes. `4` docks it into the bottom quarter, `3` into the bottom third, `8` into the bottom eighth. Values below `2` and anything that is not a number are refused and the console falls back to `4`. |
| `dock.resizable` | `false` | Whether the console window can be resized and dragged around the desktop. Only the exact value `true` switches this on; every other value, including `yes`, reads as `false`. |

Both settings are read when a console window is opened, so a change applies to the next console you
open. A console that is already on screen keeps the size it was given.

## On the command line

This tool contributes no commands, and it has no command line equivalent, because it exists for the
opposite situation: seeing program output when there is no terminal attached. When you run the
application in command line mode
(`java -jar mystic-crypt-ui-all.jar --cli <command> ...`), its output already goes to your terminal
and the console has nothing to add.

## Limits and pitfalls

- **Only the last 60 lines survive.** The console keeps a rolling buffer; once the 61st line
  arrives, the oldest one is dropped and is gone for good. A long stack trace can push out the
  message you were after. Copy what you need out of the window while it is there.
- **It is not saved anywhere.** Closing the window, or closing the application, discards everything
  it showed. Nothing is written to a log file by this tool.
- **Taking over the output is permanent for the session.** Once a console is open, the application's
  standard output and error go into it and stay there. Closing the console window does not give them
  back: from then on the output is written into a window that no longer exists, and it does not
  reappear in the terminal you may have started the application from. If you started the application
  from a terminal and want its output there, do not open the console.
- **A second console silences the first.** Every console you open takes the output over for itself.
  An older console window that is still on screen simply stops updating, which looks exactly like
  "nothing is happening".
- **The text area accepts typing, but do not use it for notes.** Anything you type is replaced the
  next time a line of output arrives.
- **A wrong divisor fails silently.** `0`, `1`, a negative number or a typo such as `four` do not
  produce an error message; the console just opens at the default quarter height. If your setting
  seems to have no effect, that is why. In the other direction there is no upper limit: a value like
  `20` gives you a sliver of a window, and since the window cannot be resized by default, the only
  way out is to correct the setting and open a new console.
- **Opening it changes the view mode.** This is the one action of this plugin that touches the rest
  of the application: it forces the desktop view, replacing the tree-and-table view of your database
  until you switch back.
- **Whatever is printed is on screen.** The console shows raw program output as it is. If a library
  ever prints something sensitive, it is visible to anyone looking at the window, and it will be in
  the text you copy into a bug report. Read a pasted block before you post it.

## Under the hood

The console window is the output panel from the astrapi69 `swing-components` library. It installs a
replacement for the process-wide standard output and error streams that appends every written line
into the text area, keeping at most 60 lines in memory; there is no file, no buffer on disk and no
cryptography involved anywhere in this tool. Its two settings live in a plain properties file next
to the settings of the other plugins, at
`~/.config/mystic-crypt-ui/plugin-settings/console-plugin.properties`, which is what the
**Reset to defaults** button deletes.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
