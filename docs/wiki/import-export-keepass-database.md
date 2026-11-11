# Import/export a KeePass database

mystic-crypt-ui can read and write real KeePass `.kdbx` files, so you can migrate an existing
KeePass database in, or take your data out to use in any standard KeePass client.

## Import from KeePass

1. Sign in to your mystic-crypt database first - import adds entries into whichever database is
   currently open.
2. **File → Import from KeePass...**
3. Click **Browse...** and select the `.kdbx` file.
4. Enter the password and/or select the key file, matching whichever credentials the KeePass
   database was created with. Leave the key file checkbox unchecked if the database only uses a
   password.
5. Click **OK**.

The imported entries and groups appear in your tree under a new top-level group named
**Imported from `<filename>`** - your existing entries are left untouched. The change is applied
immediately to the tree you're looking at; save the database afterwards (`Ctrl+S`) to persist it.

The dialog remembers the last file and key file you used, so re-importing the same source later
doesn't require re-browsing for it.

### If the import fails with "Header HMAC does not match"

This means the password and/or key file you entered don't match what the `.kdbx` file was
actually encrypted with - the file itself is fine. Double-check:

- The password is typed correctly (case-sensitive).
- You're selecting the *correct* key file for this specific database - key files aren't
  interchangeable between databases.
- If the KeePass database only uses a password, the key file checkbox is unchecked.

## Export to KeePass

1. Sign in to the database you want to export.
2. **File → Export to KeePass...**
3. Click **Browse...** and choose a destination filename (`.kdbx` is appended automatically if
   omitted).
4. Set the password and/or key file to protect the exported file with - these don't need to
   match your mystic-crypt master key, since the exported file is an independent copy.
5. Click **OK**.

The resulting `.kdbx` file can be opened in any standard KeePass client.
