# Getting Started

This page walks through everything from first launch to having a usable, signed-in database.
The sign-in step in particular has several moving parts (password, key file, or both - and two
different ways to pick a database file), so read this fully before you create your first
database.

## 1. First launch

On launch the first thing you see is the **Enter your credentials** dialog. This dialog is your
one entry point for both **creating a new database** and **opening an existing one** - there is
no separate "new project" screen.

The dialog has three areas:

- **Application File** - a dropdown of previously used database files, a `Browse...` button, and
  a `New...` button.
- **Master Password** - a checkbox, a password field, and a "show password" button (`***`).
- **Key File** - a checkbox, a dropdown of previously used key files, and a `Browse...` button.

## 2. Creating a new database

1. Click **New...** next to Application File.
2. In the file chooser, pick a location and type a filename (no extension needed - `.mcrdb` is
   added automatically) and click **Save**. This creates the empty database file on disk and
   selects it in the Application File dropdown. Use a name that does not exist yet: an existing
   file of that name is overwritten in the next step.
3. A second dialog, **Create your master key**, opens. This is where you actually define how the
   database will be protected - decide *now*, since this choice cannot be changed later without
   creating a new database:
   - **Password only** - check the password checkbox, leave the key file checkbox unchecked, and
     enter (or generate) a password, twice. The application does not compare the two fields, so
     check them with `***` before you go on.
   - **Key file only** - check the key file checkbox, leave the password checkbox unchecked, and
     select or create a key file.
   - **Both** - check both checkboxes and set both. This is the strongest option: an attacker
     needs the key file *and* the password.
4. Click **OK** on the master-key dialog.
5. Back in **Enter your credentials**, click **OK** to sign in to the new database.

[[create-new-mystic-crypt-database]] goes through both dialogs field by field.

> **Back up your master password and/or key file now, somewhere safe, separate from the
> database file itself.** There is no recovery mechanism - if you lose the password or key file,
> the data in the database is unrecoverable.

## 3. Opening an existing database

1. Select the file from the **Application File** dropdown (if you've opened it before) or click
   **Browse...** to pick it from disk.
2. Enter the password and/or select the key file - exactly matching whichever combination was
   used when the database was created. Checking a box you didn't use at creation time (or
   leaving one unchecked that you did use) will fail to open the file.
3. Click **OK**, or press Enter in the password field.

If the password is wrong, an error dialog says so and the sign-in dialog stays open for another
try. [[open-existing-mystic-crypt-database]] has the details.

## 4. Why the OK button might look disabled

The OK button only enables once your input satisfies the credential combination you checked. If
OK stays disabled, double-check:

- An Application File is selected, and the file still exists on disk.
- At least one of the two checkboxes is checked.
- Every checked checkbox has its corresponding field filled in: a checked password needs at least
  6 characters, a checked key file has to be selected and exist.

## 5. After signing in

Once signed in, the main window shows your database as a tree on the left with a table of entries
on the right; [[The-Entry-Tree]] covers working with it. Above it is a toolbar with **New
application** (a new database), **Save** and a search field.

The **File** menu holds everything about the database itself:

- **Open Database...** (`Ctrl+O`) to open another database file, **Show Database View**
  (`Ctrl+D`) and **Close Database** (`Ctrl+W`).
- **Search...** (`Ctrl+F`).
- **Save** (`Ctrl+S`) and **Save As...** (`Ctrl+Shift+S`).
- **Import from KeePass...** and **Export to KeePass...** - see
  [[import-export-keepass-database]] for details.
- **Lock workspace** (`Ctrl+L`), which hides the database until the master password is typed
  again - see [[Signing-In]].
- **Secret key > Open private key** (`Ctrl+P`), **Toggle Fullscreen**, **Settings...** and
  **Exit**.

The **View** menu holds the look and feels. The tools - key generation, certificates, checksums,
obfuscation, file encryption and more - are in the **Plugins** menu, each in a submenu of its own.
The installer installs them unless you deselect its plugins pack; the **Plugins** menu is only
shown when at least one plugin is installed. See [[install-a-plugin]] for more, and [[Settings-And-Plugins]] for the settings dialog.

When you cancel the sign-in dialog, the main window opens without a database, and
**File > Open Database...** brings the dialog back.

## Related pages

- [[create-new-mystic-crypt-database]] - a shorter, focused version of section 2 above.
- [[open-existing-mystic-crypt-database]] - a shorter, focused version of section 3 above.
- [[Signing-In]] - the sign-in dialog, the master key and locking in depth.
- [[The-Database-File]] - what is in the file and what protects it.
- [[The-Entry-Tree]]
- [[Settings-And-Plugins]]
- [[Secure-your-passwords-with-obfuscation]]
- [[import-export-keepass-database]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]] (for developers)
