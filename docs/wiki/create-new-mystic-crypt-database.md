# Create a new mystic-crypt database

A new database is made in two steps: first you choose the file it is written to, then you decide
what protects it - a master password, a key file, or both. The second decision is the one that
cannot be changed later without creating another database, so make it deliberately.

[[Signing-In]] explains what each kind of master key is worth, and [[The-Database-File]] what is
in the file and what protects it.

## Where you start

- **In the sign-in dialog** (**Enter your credentials**), with the `New...` button next to
  **Application File**. This is the way on the first launch.
- **In the main window**, with the first toolbar button, **New application**. It closes a database
  that is open first, asking about its unsaved changes, and opens the new database as soon as it is
  created. While the workspace is locked it refuses, because the locked database would otherwise
  be closed without its master password.

Both go through the same two dialogs.

## 1. Choose the file

A save dialog titled "Specify the database file to save" opens, filtered to
`Mystic crypt files (*.mcrdb)`. Type a name and confirm; without an extension the file gets
`.mcrdb`. Nothing is written yet: the file comes into being when **OK** in the next dialog writes
the database.

If a file of that name already exists, a question titled **Replace this database?** comes first:
"'<name>' already exists. Replacing it destroys what it holds, and nothing can produce it again.
Replace it?". **No** ends it, and the file stays as it was. **Yes** goes on, and clicking **OK** in
the next dialog writes the new, empty database over that file; its previous content is kept beside
it as `<name>.mcrdb.bak` only until you sign in to the new database. The toolbar's
**New application** asks the same question.

## 2. The dialog "Create your master key"

Under the heading "Create Master Key" the dialog shows:

| Row | What it offers |
|---|---|
| **Application File** | The path you just chose, read-only. `Browse...` chooses another path instead, and asks the same question first when the file it is given exists. |
| **Master Password:** | A checkbox, the password field, `***` to show or hide what you typed, and `Generate`. |
| **Repeat Password:** | The second password field. |
| **Key File:** | A checkbox, a dropdown of key files, `Browse..` to select a private key file you already have, and `Create key file...` to make a new one. |

Below them are **Help**, **OK** and **Cancel**. The fields of a row only work while its checkbox
is checked, so check the box first.

### A master password

Check **Master Password:** and type the password into both fields, at least 6 characters.

`Generate` opens the **Generate Password** dialog, with a length of 20 and lower case, upper case,
digits and special characters chosen; change what you like there. Its `Generate Password` button
fills both fields with the result. Nothing goes to the clipboard: press `***` to read the password
and note it down, or copy it yourself.

**OK** is enabled once **Master Password:** holds at least 6 characters and **Repeat Password:**
holds the same ones. While they differ, OK stays disabled, and **Repeat Password:** is outlined in
red, with the tooltip "The two passwords differ: type the same password in both fields.", as soon
as it is as long as the password or you leave it. A typo in a new master password is caught here,
before the database is written with it.

### A key file

Check **Key File:**, then either:

- `Browse..` and select a private key file you already have, or
- `Create key file...`, which opens **Create new private key**: choose the **Keysize** (2048 is
  preselected), click **Generate key**, type a **File name for private key**, and click
  **Save private key**. The key is an RSA key, written in PEM format into the
  **Directory to save the private key** - the application's configuration directory unless you
  pick another with `Browse...`. **Save private key** stays disabled until a key was generated and
  a name typed, and asks before it replaces an existing file. The new key file is then selected
  in the **Key File:** dropdown.

### Both

Check both boxes and set both. To open the database later, both are needed.

## 3. Confirm

**OK** encrypts the still empty database and writes it to the file. It is enabled only when the
file exists, at least one box is checked, a checked password has at least 6 characters and a
checked key file exists.

- Started with `New...`, you are back in the sign-in dialog with the new file selected and **OK**
  enabled: click it to open the database you just created.
- Started with **New application**, the new database opens directly.

One thing to know about `Browse...` next to **Application File**: the database is then written to
the file you browsed to, but the sign-in dialog keeps the file from `New...` selected, which stays
empty. Select the other file there with `Browse...` before you click **OK**.

**Cancel** writes nothing and leaves the disk as it was. **Help** opens the help page
of this dialog in your browser, or a short help text in a window of its own when the page cannot
be reached.

> Back up the master password, the key file, or both, somewhere separate from the database file,
> and the database file itself. Neither the password nor the key file can be recovered or reset;
> without them the content of the database is gone.

## See also

- [[Signing-In]]
- [[The-Database-File]]
- [[open-existing-mystic-crypt-database]]
- [[Getting-Started]]
