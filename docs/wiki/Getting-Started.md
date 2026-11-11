# Getting Started

This page walks through everything from first launch to having a usable, signed-in database.
The sign-in step in particular has several moving parts (password, key file, or both - and two
different ways to pick a database file), so read this fully before you create your first
database.

## 1. First launch

On first launch you'll see a short splash screen, followed by the **Enter your credentials**
dialog. This dialog is your one entry point for both **creating a new database** and **opening
an existing one** - there is no separate "new project" screen.

The dialog has three areas:

- **Application File** - a dropdown of previously used database files, a `Browse...` button, and
  a `New...` button.
- **Master Password** - a checkbox, a password field, and a "show password" button.
- **Key File** - a checkbox, a dropdown of previously used key files, and a `Browse...` button.

## 2. Creating a new database

1. Click **New...** next to Application File.
2. In the file chooser, pick a location and type a filename (no extension needed - `.mcrdb` is
   added automatically) and click **Save**. This creates the empty database file on disk and
   selects it in the Application File dropdown.
3. A second dialog, **Create your master key**, opens. This is where you actually define how the
   database will be protected - decide *now*, since this choice cannot be changed later without
   creating a new database:
   - **Password only** - check the password checkbox, leave the key file checkbox unchecked, and
     enter (or generate) a password.
   - **Key file only** - check the key file checkbox, leave the password checkbox unchecked, and
     select or create a key file.
   - **Both** - check both checkboxes and set both. This is the strongest option: an attacker
     needs the key file *and* the password.
4. Click **OK** on the master-key dialog.

> **Back up your master password and/or key file now, somewhere safe, separate from the
> database file itself.** There is no recovery mechanism - if you lose the password or key file,
> the data in the database is unrecoverable.

## 3. Opening an existing database

1. Select the file from the **Application File** dropdown (if you've opened it before) or click
   **Browse...** to pick it from disk.
2. Enter the password and/or select the key file - exactly matching whichever combination was
   used when the database was created. Checking a box you didn't use at creation time (or
   leaving one unchecked that you did use) will fail to open the file.
3. Click **OK**.

## 4. Why the OK button might look disabled

The OK button only enables once your input satisfies whichever credential combination the
selected Application File actually needs - for a brand new file this means: exactly the
password and/or key file state you configured in step 2/3 above, with the corresponding field(s)
actually filled in. If OK stays disabled, double-check:

- The Application File is actually selected (not just typed and not yet confirmed).
- Every checked checkbox has its corresponding field filled in (a checked-but-empty password
  field, or a checked-but-unselected key file, will keep OK disabled).

## 5. After signing in

Once signed in, the main window shows your database as a tree on the left with a details table
on the right. From the **File** menu you can:

- Create secret keys, open private keys, and manage obfuscation maps.
- Verify file checksums.
- Save the database (`Ctrl+S`), or use **Save as...**.
- [Import from or export to a real KeePass database](import-export-keepass-database) - see the
  dedicated page for details.

The **Plugins** menu (only visible once at least one plugin is installed) is where third-party
extensions surface their own menu items - see
[how to install a plugin](install-a-plugin).

## Related pages

- [Create a new mystic-crypt database](create-new-mystic-crypt-database) - a shorter, focused
  version of section 2 above.
- [Open an existing mystic-crypt database](open-existing-mystic-crypt-database) - a shorter,
  focused version of section 3 above.
- [Secure your passwords with obfuscation](Secure-your-passwords-with-obfuscation)
- [Import/export a KeePass database](import-export-keepass-database)
- [Install a plugin](install-a-plugin)
- [How to create a plugin](how-to-create-a-plugin) (for developers)
