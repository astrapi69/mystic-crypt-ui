# Import/export a KeePass database

mystic-crypt-ui can read and write real KeePass `.kdbx` files, so you can migrate an existing
KeePass database in, or take your data out to use in any standard KeePass client.

Both dialogs have the same three rows: **KeePass file:** (a read-only field with a `Browse...`
button), **Password:**, and a **Key File:** checkbox with its own field and `Browse...`, which
only work while the checkbox is checked. Both remember the last file and key file you used, in
`memoizedKeePass.json` in the configuration directory - never the password.

## Import from KeePass

1. Sign in to your mystic-crypt database first - import adds entries into whichever database is
   currently open. A database that opened read-only takes no import.
2. **File > Import from KeePass...**
3. Click **Browse...** and select the `.kdbx` file. The chooser ("Select the KeePass database
   file to import") is filtered to `KeePass files (*.kdbx)`.
4. Enter the password and/or select the key file, matching whichever credentials the KeePass
   database was created with. Leave the key file checkbox unchecked if the database only uses a
   password, and the password empty if it only uses a key file.
5. Click **OK**.

The KeePass database's root group, with everything in it, appears in your tree as a new top-level
node, under the name it has in KeePass. Your existing entries are left untouched. A dialog
**Import successful** confirms it. The change is applied immediately to the tree you're looking
at; save the database afterwards (`Ctrl+S`) to persist it.

What comes along: the group structure with the groups' names, icons and times, and for every entry
its title, user name, password, URL, notes, custom properties (and which of them are protected),
attachments, icon, its creation, modification, access and expiry times, and its earlier versions.
[[The-Entry-Tree]] shows where they appear.

A failed import shows **Import failed** with "Could not import the KeePass file:" and the reason,
and adds nothing to your database.

### If the import fails with "Header HMAC does not match"

This means the password and/or key file you entered don't match what the `.kdbx` file was
actually encrypted with - the file itself is fine. Double-check:

- The password is typed correctly (case-sensitive).
- You're selecting the *correct* key file for this specific database - key files aren't
  interchangeable between databases.
- If the KeePass database only uses a password, the key file checkbox is unchecked.

### If the file is a KDBX 3.1 file with protected attachments

The import cannot read this kind of file yet, and refuses it instead of losing the attachments:
the message names the format and the way out. Open the file in KeePassXC, choose
**Database > Database Settings... > Security > Encryption Settings**, set the database format to
KDBX 4, save it, and import it again.

## Export to KeePass

1. Sign in to the database you want to export.
2. **File > Export to KeePass...**
3. Click **Browse...** and choose a destination filename (`.kdbx` is appended automatically if
   omitted). An existing file of that name is replaced without a further question.
4. Set the password and/or key file to protect the exported file with - these don't need to
   match your mystic-crypt master key, since the exported file is an independent copy.
5. Click **OK**. A dialog **Export successful** confirms it.

When a plugin keeps something of its own in the database, the export dialog says so above the
fields. The Lethenon plugin, for example, says that the file contains sealed wallets, and whether
their 24 words are in it as well: whoever can open the exported file then has those wallets.

The resulting `.kdbx` file is written in the KDBX 4 format and can be opened in any standard
KeePass client. Its database name is the name of your database file without the extension, its
description "Exported from mystic-crypt-ui". When your tree has a single top-level node, that node
becomes the KeePass root group; several top-level nodes go under the KeePass root group.

An export does not carry everything KeePass itself can hold: a group's recycle-bin flag, and an
entry's custom icon, tags, colours, auto-type settings and custom data are not written. An entry
imported from KeePass and exported again keeps everything listed under the import above.

## See also

- [[The-Entry-Tree]]
- [[The-Database-File]]
- [[Getting-Started]]
