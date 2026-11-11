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
[[install-a-plugin]]. If the entry is missing, check **File > Settings...**, tab **Plugins**: the
plugin is listed there with its state, and it can be enabled, disabled or installed from a zip on
that tab.

The entry is enabled while a database is open. Before you sign in, and while the workspace is
locked, it is greyed out.

## Using it

1. Choose **Plugins > Console > Console**. If the application is currently showing the database as
   a tree with a details table that fills the window, it switches to the desktop view first. The
   database view stays on that desktop as a window of its own, and **File > Show Database View**
   (`Ctrl+D`) brings it to the front.
2. A window titled **Console** appears along the bottom of the application's desktop, across its
   full width and a quarter of its height by default, with an empty text area. It worked when that
   window is there. Nothing else is reported: if no window appears, the plugin is not loaded, and
   the **Plugins** menu is where you check that. When you resize the application window, the
   console follows and stays docked at the bottom.
3. Now do whatever you want to observe: open a tool, run an operation. Every line the application
   prints from this moment on appears in the text area, and so does what the application logs
   through Java's own logging. A line appears once it is complete. Output produced *before* you
   opened the console is not shown, so open it first and reproduce afterwards.
4. Select the text with the mouse and copy it with `Ctrl+C` to paste it into a bug report. The text
   area is read-only, and there is no save, export or clear button.
5. Close the console window when you are done. Closing it hands standard output and error back to
   where they went before, a terminal you may have started the application from included, and
   erases what the console showed.

To go back to the tree and table filling the whole window, choose `APPLICATION_PANEL` in the
**View:** box on the **General** tab of **File > Settings...**; the application takes that view
when you close the settings dialog. The console stays open meanwhile and keeps collecting output:
it lives on the desktop, so it is out of sight in the panel view, and `DESKTOP_PANE` in the same
box shows it again.

## Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Console** in the list on the
left. Change the value in the *value* column, then press **Apply**; **Reset to defaults** deletes
the stored values so the defaults below apply again.

| Key | Default | What it changes |
|---|---|---|
| `dock.height.divisor` | `4` | The share of the desktop's height the console takes. `4` docks it into the bottom quarter, `3` into the bottom third, `8` into the bottom eighth. Values below `2` and anything that is not a number are refused and the console falls back to `4`. |
| `dock.resizable` | `false` | Whether the console window can be resized and dragged around the desktop. Only `true`, in any capitalisation, switches this on; every other value, including `yes`, reads as `false`. A resizable console does not follow the application window when that is resized. |
| `buffer.max.lines` | `2000` | How many lines the console keeps. Above it the oldest lines are dropped. Values below `1` and anything that is not a number fall back to `2000`. |

All three settings are read when a console window is opened, so a change applies to the next
console you open. A console that is already on screen keeps what it was given.

## On the command line

This tool contributes no commands, and it has no command line equivalent, because it exists for the
opposite situation: seeing program output when there is no terminal attached. When you run the
application in command line mode
(`java -jar mystic-crypt-ui.jar --cli <command> ...`, see [[The-Command-Line]]), its output already
goes to your terminal and the console has nothing to add.

## Limits and pitfalls

- **Only the last 2000 lines survive**, or as many as `buffer.max.lines` says. Once the limit is
  reached, every new line drops the oldest one, and it is gone for good. A long stack trace can
  push out the message you were after. Copy what you need out of the window while it is there.
- **It is not saved anywhere.** Closing the window, locking the workspace or closing the
  application discards everything it showed. Nothing is written to a log file by this tool.
- **Locking closes it.** Locking the workspace closes every window on the desktop, the console
  among them, and the console erases its text as it goes. Unlocking does not bring it back; open a
  new one.
- **While it is open, the terminal gets nothing.** If you started the application from a terminal,
  standard output and error go into the console instead, until you close it.
- **Open one console at a time.** Every console you open takes the output over for itself, and an
  older console window that is still on screen simply stops updating, which looks exactly like
  "nothing is happening". Each console also remembers only the streams it replaced, so once two
  have been open, closing them does not reliably hand the output back to the terminal; restart the
  application if you need it there again.
- **A wrong divisor fails silently.** `0`, `1`, a negative number or a typo such as `four` do not
  produce an error message; the console just opens at the default quarter height. If your setting
  seems to have no effect, that is why. In the other direction a large value cannot make the
  console unreadable: it is never shorter than 120 pixels, or than the whole desktop if that is
  smaller.
- **Opening it changes the view mode.** This is the one action of this plugin that touches the rest
  of the application: it forces the desktop view, where the database is shown as a window of its
  own instead of filling the application window.
- **Whatever is printed is on screen.** The console shows raw program output as it is. If a library
  ever prints something sensitive, it is visible to anyone looking at the window, and it will be in
  the text you copy into a bug report. Read a pasted block before you post it.

## Under the hood

The console installs a replacement for the process-wide standard output and error streams that
appends every completed line to its text area, keeping at most `buffer.max.lines` lines in memory,
and it points the root logger of `java.util.logging` at that stream as well, so logged lines reach
it too. When the window closes, for whatever reason, it puts the previous streams back, points the
root logger at them again and erases both the text area and any line it had not shown yet. There is
no file, no buffer on disk and no cryptography involved anywhere in this tool. Its three settings
live in a plain properties file next to the settings of the other plugins, at
`~/.config/mystic-crypt-ui/plugin-settings/console-plugin.properties`, which is what the
**Reset to defaults** button deletes.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
- [[The-Command-Line]]
