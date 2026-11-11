# Obfuscation

## What it is for

Obfuscation veils a text by swapping characters for other characters, following a rule
table that you write yourself. It answers one question: "how do I make this password or
this note unreadable at a glance, and still be able to turn it back myself?" Reach for it
when the risk is a shoulder, a screenshot or a printed sheet, and reach for the database
itself or the file and text encryption tool when the risk is somebody who has the file:
a substitution cipher has no key and no password behind it, and the rule table is the
only thing standing between the text and whoever reads it.

The plugin brings two tools. **Simple Obfuscation** swaps one character for another,
nothing else, and reverses cleanly. **Operated Obfuscation** adds an operation (upper
case, lower case, negated case) that is applied at positions you name, instead of the
substitution, which makes the result depend on where a character sits in the text. Its
window obfuscates, but its way back does not work in this release; the command line's
`disentangle` reverses such a text (see "On the command line" below).

## Where to find it

**Plugins** menu, submenu **Obfuscation**, items **Simple Obfuscation** and
**Operated Obfuscation**.

The Plugins menu only appears once at least one plugin is installed (see
[[install-a-plugin]]). Both items are enabled while a database is open; before you sign in,
and while the workspace is locked, they are greyed out. Choosing either item switches the
main window to desktop mode and opens the tool as a window inside it, titled like the menu
item. The database view stays on that desktop as a window of its own, and
**File > Show Database View** (`Ctrl+D`) brings it to the front.

## Using it

### Simple Obfuscation

1. Open **Plugins > Obfuscation > Simple Obfuscation**.
2. Under **Obfuscation Rule**, type one character into **Original Character** and one
   into **Replace with**. Both fields hold exactly one character; further keystrokes are
   ignored rather than rejected with a message.
3. Press **Add**. The rule appears in **Table of key rules for obfuscate** with the
   columns *Original character*, *Replace with*, *Edit* and *Delete*, and both input
   fields are emptied for the next rule. Four warnings can stop the Add instead:
   - *Original character is empty*
   - *Replace with character is empty*
   - *Original character already exists*
   - *Replace with character already exists*

   The last two both add the sentence "Disentangle process can not be executed if same
   characters exists": every original and every replacement has to be unique, otherwise
   the way back is ambiguous.
4. Repeat for every character you want to swap. **Edit** in a row loads that rule back
   into the two fields, and pressing **Add** again stores the change. **Delete** removes
   the row.
5. Type or paste your text into **Text to encrypt** on the left, then press
   **Obfuscate >**. The result appears in **Encrypted text** on the right, and, with the
   default setting, the left field is emptied.
6. To reverse it, keep the same rule table, put the obfuscated text into **Encrypted
   text** and press **< Disentangle**. The recovered text appears in **Text to encrypt**,
   and the right field is emptied.

With the single rule `a` to `x`, the text `cab` becomes `cxb`, and `cxb` disentangles
back to `cab`. That is exactly what the end-to-end test of this tool clicks through.

There is no success message: the filled result field is the confirmation. If a button
does nothing, look at whether it is greyed out. **Obfuscate >** stays disabled while
**Text to encrypt** is empty, **< Disentangle** while **Encrypted text** is empty, which
is also why the buttons go grey again right after a run under the default setting.

### Operated Obfuscation

1. Open **Plugins > Obfuscation > Operated Obfuscation**.
2. Under **Obfuscation Operation Rule**, fill **Original Character** and **Replace with**
   as above, then the two extra fields:
   - **Indexes**: positions in the text, counted from 0, separated by commas without
     spaces, for example `0,4`. At those positions the operation is applied to the original
     character and the substitution is skipped; at every other position the replacement
     character is used. Leave the field empty for a plain substitution.
   - **Operation**: `LOWERCASE`, `NEGATE`, `NONE`, `UPPERCASE` or `UNKNOWN`. The box
     starts on `LOWERCASE`. `NONE` and `UNKNOWN` leave the character as it is, so a rule
     with indexes and one of those two writes the original character in clear at those
     positions.
3. Press **Add**. The row shows *Original character*, *Replace with*, *Indexes*,
   *Operation*, *Edit* and *Delete*, and all four inputs are reset, the operation box back
   to its first entry.
4. Obfuscate with **Obfuscate >** as in the simple tool. Here the left field is always
   emptied after a run, whatever the setting says.
5. **< Disentangle** does not work in this tool in this release. The library call it uses
   for the way back fails with a `NullPointerException` (visible in the [[Plugin-Console]]
   if one is open), and both text fields stay as they were, with no message in the window.
   To get an operated text back, hand the same rules to the command line's `disentangle`, as
   shown under "On the command line" below, and keep a copy of the original until you have
   checked that it comes back.

### Saving a rule table

Below the table, **Import** and **Export** read and write a rule file. The file chooser
opens in the application's configuration directory and filters on
*Mystic crypt obfuscation files (\*.obf)*. Export encrypts the current table with the key
of the database you are signed in to, so the file is only readable again after signing in
with that same key. Import replaces the table in the window with the one from the file.

Both buttons therefore need a database that was opened with a key file; see
[[Getting-Started]] for that choice. With a database opened by master password alone they
are greyed out, and their tooltip says why; should either be reached anyway, a dialog
**No key file for this database** refuses it. A file exported from the other of the two
tools, or one that names any type a rule table does not hold, is refused with the dialog
**Rule table not imported**, which names the type, and nothing is imported.

## Settings

| Key | Default | What it changes |
|---|---|---|
| `clear.input.after.run` | `true` | Whether the field that was just read is emptied after a run. `true`: after **Obfuscate >** the left field is cleared, after **< Disentangle** the right one, so the two sides cannot be mistaken for each other. `false`: the text stays, and the same text can be sent through several rule tables in a row. |

Edited under **File > Settings...**, tab **Plugin settings**, entry **Obfuscation** in the
list on the left. Change the value in the *value* column, press **Apply** to store it, or
**Reset to defaults** to drop the stored file and see the default again.

Two details worth knowing. Only the exact word `false` (in any capitalisation) switches
the emptying off; any other value, a typo included, counts as `true`. And the setting is
read by **Simple Obfuscation** only: **Operated Obfuscation** clears its fields either
way.

## On the command line

This plugin contributes no commands of its own. The closest thing is the pair of
commands the mystic-crypt library itself brings, reachable through the application's
`--cli` switch (see [[The-Command-Line]]), and they cover both kinds of rule: the plain
substitution and the operation at named positions.

```
java -jar mystic-crypt-ui.jar --cli obfuscate \
    --rule a=x --rule b=y --rule c=z --text "cabbage"
```

prints the obfuscated text on standard output:

```
zxyyxge
```

and the way back, with the same rules:

```
java -jar mystic-crypt-ui.jar --cli disentangle \
    --rule a=x --rule b=y --rule c=z --text "zxyyxge"
```

```
cabbage
```

`--rule` is required and repeatable. A rule is either a plain substitution, `a=x`, or an
operated one, `a=x:UPPERCASE@1,4`, which writes the operated character at those positions
and the substitution everywhere else. The operations are `UPPERCASE`, `LOWERCASE`,
`NEGATE` and `NONE`, in any capitalisation, and the positions after the `@` are counted
from 0 and separated by commas, exactly as in the **Indexes** field of the tool window.
So

```
java -jar mystic-crypt-ui.jar --cli obfuscate \
    --rule a=x:UPPERCASE@1,4 --rule b=y --rule c=z --text "cabbage"
```

prints

```
zAyyAge
```

because the two `a` of `cabbage` sit at positions 1 and 4 and are upper-cased there
instead of being replaced by `x`. Handing the very same rules to `disentangle` gives
`cabbage` back:

```
java -jar mystic-crypt-ui.jar --cli disentangle \
    --rule a=x:UPPERCASE@1,4 --rule b=y --rule c=z --text "zAyyAge"
```

The help of `disentangle` describes `--rule` as the plain `a=x` map, but it accepts the
operated form, and the way back needs it: with the plain rules alone `zAyyAge` comes back
as `cAbbAge`.

This is also the way back for a text from the **Operated Obfuscation** window, whose own
**< Disentangle** does not work in this release. Write each row of its table as one
`--rule`: a row without indexes as `a=x`, a row with indexes as
`a=x:OPERATION@indexes`, with the operation from the row. The command line obfuscates
those rules exactly as the window does.

`--text` takes the text inline, and like any argument it is visible to the other
processes on the machine and lands in the shell history: fine for `cabbage`, not for a
password you are veiling for real. `--text-stdin` reads the text from standard input
instead, all of it and exactly as it comes, a final line break included, so hand it over
with `printf '%s'` rather than `echo` (the page on [[The-Command-Line]] shows how to get a
secret into a shell variable without typing it into a command). The plugins that are
installed log their start and stop on standard error around the result, so redirecting
standard output alone gives you a clean file.

An exported `*.obf` rule file cannot be handed to these commands: the rules have to be
repeated as `--rule` options, operations included.

## Limits and pitfalls

**It is not encryption.** No key, no password, no integrity check. Anyone who gets the
rule table reads the text, and a longer text can be broken by counting letters. Never
treat an obfuscated password as protected the way a database entry is.

**A replacement character that also occurs unmapped ruins the way back.** With the single
rule `a` to `x`, the text `fax` obfuscates to `fxx`, and disentangling `fxx` gives `faa`:
the `x` that was already in the text is turned into an `a` as well. Keep replacements
outside the alphabet your texts actually use, or check the result before you throw the
original away. This is the single most common way to end up with a rule table that looks
fine and quietly corrupts texts.

**An empty rule table fails without saying so.** In **Simple Obfuscation**, **Obfuscate >**
with no rules does nothing at all: the library refuses an empty rule table, and the window
shows no message. **< Disentangle** with no rules copies the text unchanged into the left
field, and **Operated Obfuscation** does the same on **Obfuscate >**, which both look like
they worked.

**The rule table lives only in the open window.** Closing the tool window loses it, and so
does locking the workspace, which closes every window on the desktop. Choosing the menu
item a second time opens a second window with its own empty table rather than the one you
filled. Nothing is stored in the database and nothing survives a restart, so export before
you close if you will need those rules again.

**Editing a rule and changing its original character does not update it.** The update only
applies while the original character stays the same. Change it and press Add and you
either get a new rule or the "Original character already exists" warning, with the old
rule still in the table. Delete and re-add is the safe route.

**The Operated Obfuscation window cannot reverse what it produced.** Its **< Disentangle**
fails in this release, as described above, so a text veiled there comes back only through
the command line's `disentangle`. Try that way back once before you rely on an operated
rule table for anything you need to read again.

**Indexes are absolute positions in the text, not occurrences.** The same operated rule
table applied to a text with one extra leading space produces a different result, and
disentangling only works for a text of the same shape.

**The Indexes field accepts other separators but does not understand them.** Only commas
separate positions, with nothing else between them. A semicolon, or a space after a comma
as in `0, 4`, makes **Add** fail silently: no rule is added, no dialog appears.

**An import under the wrong key is silent.** Importing a file that was exported under a
different key leaves the table exactly as it was, without a dialog; the reason goes to the
application's error output only, which the [[Plugin-Console]] shows if one is open.

**A simple rule file and an operated rule file share the `*.obf` extension but are not
interchangeable.** A file exported from Simple Obfuscation carries no operation and no
index information, and each tool refuses the other's file with **Rule table not imported**.
Keep the two apart by file name.

**Rules are single characters and case sensitive.** `a` and `A` need two rules, and there
is no way to map a whole word or a digit group in one rule. Everything without a rule
passes through untouched, so length, spaces and line breaks survive obfuscation and still
show the shape of what you veiled.

## Under the hood

Both tools call the obfuscation code of the mystic-crypt library, with the rule model from
crypt-data and bi-directional maps from Guava underneath; the plugin itself only collects
your rules and hands them over. Three consequences are worth carrying around. The simple
tool reverses a text by turning its rules into a plain character map and applying that
map's inverse, the same way the command line's `disentangle` does for plain rules. That is
why a rule whose
replacement is not itself an original character reverses correctly, and it is also why an
unmapped occurrence of a replacement character is reversed along with the rest. The
operated tool's way back goes through an older call of the library, which fails in
mystic-crypt 13.9 when it tries to copy the rule table; the command line's `disentangle`
uses the library's current one, which also checks positions. And an export is a real
encryption step: the rules are written as XML and encrypted with the
public key derived from the private key of the signed-in database, which is what ties the
file to that one database. Everything else, the rule table and both texts, is held in the
open window as ordinary text for as long as the window is open.

## See also

- [[Getting-Started]]
- [[Secure-your-passwords-with-obfuscation]]
- [[The-Command-Line]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
