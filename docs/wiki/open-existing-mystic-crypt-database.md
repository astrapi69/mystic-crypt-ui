# Open an existing mystic-crypt database

Your database is one encrypted file, and the dialog **Enter your credentials** is the way into it.
It opens when the application starts, and again from **File > Open Database...** (`Ctrl+O`), which
closes a database that is already open first and asks about its unsaved changes.

This page goes through what the dialog offers. [[Signing-In]] explains the three kinds of master
key and what locking does, and [[The-Database-File]] what protects the file.

## What the dialog shows

Under the heading "Enter Master Key" there are three rows:

| Row | What it offers |
|---|---|
| **Application File** | A dropdown of the database files you opened before. `Browse...` picks another file from disk. `New...` creates a new database instead, see [[create-new-mystic-crypt-database]]. |
| **Master Password:** | A checkbox, the password field, and a `***` button that shows or hides what you typed. The field and the button only work while the checkbox is checked. |
| **Key File:** | A checkbox, a dropdown of the key files you used before, and a `Browse...` button for another one. Both only work while the checkbox is checked. |

Below them are **Help**, **OK** and **Cancel**.

The dialog remembers your last sign-in: the files in both dropdowns, which ones were selected, and
the state of the two checkboxes. It never remembers the password. So when you come back, the
dialog is usually set up already and the caret waits in the password field.

## Opening your database

1. Pick the file in the **Application File** dropdown, or click `Browse...` and select it. If you
   pick an entry whose file has been moved or deleted, it is removed from the dropdown.
2. Check the boxes your database was created with, and fill them in:
   - **Master Password:** only - type the password.
   - **Key File:** only - select the key file.
   - both - type the password and select the key file.

   The combination has to be exactly the one chosen when the database was created. A box too many
   or too few fails the same way a wrong password does.
3. Click **OK**, or press Enter in the password field.

## When OK stays disabled

**OK** only becomes clickable when all of these hold:

- the selected application file exists on disk,
- at least one of the two checkboxes is checked,
- with **Master Password:** checked, the password has at least 6 characters,
- with **Key File:** checked, the selected key file exists on disk.

## When the database does not open

A wrong password, a wrong key file or the wrong combination of boxes is reported in an error
dialog, and the sign-in dialog stays open so you can correct what you typed:

| You tried | Dialog title | Message |
|---|---|---|
| Password only | Authentication with Password | Password is not valid |
| Key file only | Authentication with key file | Key file is not valid |
| Both | Authentication with Password or key file | Password or key file or both are not valid |

A file that is not a mystic-crypt database at all, or whose content this version cannot read, is
not blamed on your password: the dialog is titled **Database not opened** and names the reason.
A database written by a newer version of the application opens read-only; [[The-Database-File]]
explains why.

## The three buttons

- **OK** opens the database with what you entered.
- **Cancel** closes the dialog without opening anything. The main window then comes up with no
  database. What does not need one stays available: **File > Open Database...** brings this dialog
  back, and **Settings...**, **Exit**, the **View** and **Help** menus and the tools that work
  without a database (Checksum, Password Hashing) can be used.
- **Help** opens the help page of this dialog in your browser. When the page cannot be reached, a
  short help text is shown in a window of its own instead.

There is no way to recover a forgotten master password or a lost key file: the key that decrypts
the file is made from them and stored nowhere else. Without them, the content of the database is
gone.

## See also

- [[Signing-In]]
- [[The-Database-File]]
- [[create-new-mystic-crypt-database]]
- [[Getting-Started]]
