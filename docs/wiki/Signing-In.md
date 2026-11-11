# Signing In and Locking

Every session with mystic-crypt-ui starts in the same dialog and, if you step away from your
desk, ends in the same one. This page covers the sign-in dialog in detail: creating a database,
opening one, the three credential combinations and what each is worth, what a wrong password
looks like, and how locking the workspace works.

If you just want the short path from first launch to a usable database, read
[[Getting-Started]] first.

## 1. The sign-in dialog

On launch the application shows the modal dialog **Enter your credentials**. It is the way into a
database: no database is open until this dialog is confirmed. **File > Open Database...**
(`Ctrl+O`) shows it again later; with a database open, that database is closed first, after the
question about its unsaved changes. **File > Show Database View** (`Ctrl+D`) is a different item:
it only brings the open database back into view.

Under the heading "Enter Master Key" the dialog has three rows:

- **Application File** - a dropdown of database files you used before, a `Browse...` button and
  a `New...` button.
- **Master Password:** - a checkbox, a password field and a `***` button that toggles the
  password between hidden and visible.
- **Key File:** - a checkbox, a dropdown of key files you used before and a `Browse...` button.

The password field and its `***` are disabled until the **Master Password:** checkbox is checked,
and the key file dropdown and its `Browse...` are disabled until the **Key File:** checkbox is
checked. So the first thing you do in this dialog is to declare which kind of master key the file
needs.

A few conveniences worth knowing:

- The caret starts in the password field when the master password is in use, and checking
  **Master Password:** moves it there.
- With OK enabled, pressing Enter in the master password field signs you in, the same as
  clicking **OK**.
- Under the FlatLaf look and feels, which are the default, every password field also has a small
  button of its own that shows the typed password.
- **Help** opens the help page of the dialog in your browser; if that page cannot be reached, a
  short help text is shown in a window of its own instead.

[[open-existing-mystic-crypt-database]] and [[create-new-mystic-crypt-database]] are the help
pages of the two dialogs on this page.

## 2. Creating a new database

1. Click **New...**. A save dialog titled "Specify the database file to save" opens, filtered to
   `Mystic crypt files (*.mcrdb)`. Type a name and confirm, the extension comes from that
   filter if you do not type one. The file is created on disk immediately, still empty. If a
   file of that name exists, the question **Replace this database?** comes first, and **No**
   leaves it as it is: the next step would write the new database over it.
2. The dialog **Create your master key** opens with the **Application File** field already
   filled in with the path you just chose (the field is read-only, the `Browse...` next to it
   lets you point somewhere else).
3. Decide on the master key, this is the decision that cannot be revised later without creating
   a new database:
   - **Master Password:** checked - type the password into **Master Password:** and into
     **Repeat Password:**. **Generate** opens the password generator, with 20 characters from
     lower case, upper case, digits and special characters preselected, and fills both fields
     with the result. Nothing is copied to the clipboard; press `***` to read the password.
     **OK** stays disabled while the two fields differ, and **Repeat Password:** is outlined in
     red with the tooltip "The two passwords differ" once it is as long as the password or you
     leave it.
   - **Key File:** checked - **Create key file...** opens the **Create new private key** dialog,
     which generates an RSA key pair (default key size 2048) and writes the private key in PEM
     format, by default into the application configuration directory. **Browse..** selects a
     private key file you already have.
   - Both checked - do both of the above.
4. **OK** encrypts the (still empty) database and writes it to the file.
5. You are back in the sign-in dialog, with the new file selected in **Application File**. Its
   **OK** is now enabled, click it to sign in to the database you just created.

Once the main window is open, the toolbar's first button, **New application**, does the same two
steps and opens the new database directly. [[create-new-mystic-crypt-database]] covers both
dialogs field by field.

> Back up the master password, the key file, or both, somewhere separate from the database
> file. Neither the password nor the key file can be recovered or reset; without them the
> content of the database is gone.

## 3. Opening an existing database

1. Pick the file from the **Application File** dropdown, or click `Browse...` and select it.
2. Check exactly the boxes that were checked when the database was created and fill them in:
   the password, the key file, or both. Checking a box that was not used at creation time, or
   leaving one unchecked that was used, produces an authentication error, not a helpful hint.
3. Click **OK**, or press Enter in the password field.

## 4. Password, key file, or both

The checkbox combination decides which encryption path the database is written and read with.
All three are real options; they differ in what an attacker needs to get at your data.

| Combination | How the file is protected | What is needed to open it |
|---|---|---|
| Master password only | The database content is encrypted with the versioned password vault format, using the master password. The content goes straight from memory into the encrypted file, it is never written out in the clear. | The password. |
| Key file only | A fresh AES key (128 bit, GCM without padding) is generated when the database is saved and encrypts the content; that key is in turn encrypted with the public key derived from the private key in your key file. | The private key file. |
| Both | The content is first encrypted with the master password, and that result is then encrypted on the key file path described above. | The password **and** the key file. |

On every path the encryption happens in memory and the file is written once, with the encrypted
result. [[The-Database-File]] describes each format in detail.

What this means in practice:

- **Password only** puts everything on one thing you remember. Anyone who copies the database
  file can attack it offline, so the strength of the password is the strength of the database.
  This is the option where the **Generate** button earns its keep.
- **Key file only** replaces something you know with something you have. Nothing is memorized,
  which also means nothing protects the database once someone has a copy of the key file.
  Keep the key file off the machine that holds the database, for example on a stick you plug in
  for the session. Read the note on locking in section 7 before you choose it.
- **Both** is the strongest of the three: the two layers are applied one after the other, so a
  stolen key file alone or a guessed password alone is not enough.

## 5. Why the OK button stays disabled

The OK button of both dialogs is driven by a small state machine that re-evaluates after every
relevant change: a file chosen, a checkbox toggled, a character typed in the password field. It
enables OK only when all of the following hold:

- The selected **Application File** exists on disk.
- At least one of the two checkboxes is checked.
- If **Master Password:** is checked, the password is at least 6 characters long.
- If **Key File:** is checked, the selected key file exists on disk.

So a checked but empty password field, a checked key file box with nothing selected, a password
of five characters, or an application file entry that no longer exists all keep OK disabled.
In the create dialog, with **Master Password:** checked, **Repeat Password:** has to hold the same
characters as well.

## 6. When the credentials are wrong

A wrong password (or a wrong key file, or the wrong combination of boxes) is reported as an
error dialog after you click OK. Which one you get depends on the combination you tried:

| You tried | Dialog title | Message |
|---|---|---|
| Password only | Authentication with Password | Password is not valid |
| Key file only | Authentication with key file | Key file is not valid |
| Both | Authentication with Password or key file | Password or key file or both are not valid |

A file that could be decrypted but not read is not reported as a wrong password. Such a dialog is
titled **Database not opened** and says why: the file names a type no database of this
application holds, its content could not be read (with the reason), or the key file opened the
key of the database but the content is in no format this version reads.

No database is opened after such an error, and the sign-in dialog stays open, so you can correct
the input and try again.

**Cancel** closes the sign-in dialog without opening anything. The main window then comes up with
no database: only what does not need one is enabled - **File > Open Database...**,
**Settings...** and **Exit**, the **View** and **Help** menus, the toolbar's **New application**,
and the tools that declare that they work without a database (Checksum and Password Hashing).
**File > Open Database...** brings the sign-in dialog back.

## 7. Locking the workspace

**File > Lock workspace** (`Ctrl+L`) hides an open database without closing it:

- The content is switched away for the neutral desktop pane, so nothing of the database is on
  screen any more. Tool windows open on the desktop are closed, and the toolbar's search field is
  emptied.
- The clipboard is cleared, so a password copied before the lock cannot be pasted afterwards.
- The menus and toolbar buttons that belong to a signed-in session are disabled, the
  application behaves as if nobody were signed in: what stays enabled is the same short list as
  after a cancelled sign-in (section 6), without the toolbar's **New application**. **File > Open
  Database...** refuses while the workspace is locked and says why: the locked database would
  have to be closed first, and that needs it unlocked.
- The **Unlock workspace** prompt appears right away, with the label "Enter the master password
  to unlock the workspace:" and a password field. When the database has unsaved changes, the
  prompt says "Unsaved changes are waiting in this database."

Entering the correct master password brings the content and the menus back, in the view you chose
in the settings. A wrong password produces an error dialog "Unlock failed" with the message "Wrong
master password.", after which the unlock prompt opens again, the workspace stays locked in the
meantime.

Do not cancel the prompt unless you mean to leave the database locked. Cancelling keeps the
workspace locked, and the prompt does not come back: **File > Lock workspace** is one of the items
that are disabled while locked. The database then stays locked until the timed close below closes
it, after which **File > Open Database...** works again, or until you end the application. A
database with unsaved changes is not closed by the timer, so for it only ending the application
is left, which asks whether to discard the changes.

### Locking by itself

You do not have to remember to lock. In **File > Settings... > General**:

- **Lock after idle minutes** - the workspace locks itself after this many minutes without
  activity, 15 by default.
- **Close a locked database after minutes** - a database that has stayed locked this many more
  minutes is closed, so its decrypted content leaves memory, 15 by default. A database with
  unsaved changes is not closed this way; it stays locked until you decide.
- **Save when locking** - off by default, so a lock never writes changes you have not decided
  about. Turn it on and locking saves pending changes first.

`0` turns either timer off. [[Settings-And-Plugins]] lists every setting.

### A database opened with a key file alone

The unlock prompt asks for the master password, and a database opened with a key file alone has
none. Such a database cannot be unlocked again in the same session: whatever you type, the prompt
answers "Wrong master password." It is closed by the timed close above, unless it has
unsaved changes; ending the application asks whether to discard them. So save before you lock a
key-file-only database, or before you leave it alone long enough for the automatic lock, or set
**Lock after idle minutes** to `0` while you work with one.

### What survives a lock

The database is not closed and not re-read from disk, it stays in memory for the whole locked
period. Everything you had open is exactly as you left it after unlocking, including edits you
have not saved yet: a node added to the tree before locking is still there afterwards.

What locking does remove from memory is the key material: the master password is replaced by a
value that can recognise it but not reproduce it, and the private key of a key file is
overwritten. Unlocking compares what you type against the master password of the database that is
open.

The flip side is that locking is not saving. Unless **Save when locking** is on, the file on disk
is untouched by a lock, so if you want your changes persisted, save them with **File > Save**
(`Ctrl+S`) before you walk away. A lock is a screen guard for an unattended desk, not a substitute
for closing the database.

## 8. The recently used files list

Both dropdowns in the sign-in dialog are filled from a small file named `memoizedSignin.json` in
the application configuration directory (`.config/mystic-crypt-ui` in your home directory). It is
written after every successful sign-in and remembers:

- the database file paths you have used, and which one was selected last,
- the key file paths you have used, and which one was selected last,
- the state of the two checkboxes.

That is why a returning user usually finds the dialog already set up correctly and only has to
type the password. Note what the file does **not** contain: no password, no key material, only
paths and the two flags.

The list prunes itself. If you select an entry whose file has meanwhile been moved or deleted,
the entry is removed from the dropdown and OK stays disabled until you pick a file that exists.
Deleting `memoizedSignin.json` resets the dialog to the state of a first launch: empty
dropdowns, both checkboxes unchecked.

## See also

- [[Getting-Started]]
- [[create-new-mystic-crypt-database]]
- [[open-existing-mystic-crypt-database]]
- [[The-Database-File]]
- [[Settings-And-Plugins]]
- [[Secure-your-passwords-with-obfuscation]]
- [[import-export-keepass-database]]
- [[install-a-plugin]]
