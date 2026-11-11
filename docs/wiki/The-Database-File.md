# The Database File

Everything you keep in mystic-crypt lives in one file, the `.mcrdb` file you picked when you
created the database. This page explains what that file is, what protects it, what happens to a
file written by an older or a newer version, and what someone who gets hold of a copy can actually
do with it.

## 1. What a `.mcrdb` file is

The `.mcrdb` file is your whole database, encrypted. It is a single ordinary file: you choose
where it lives when you create it, and the `.mcrdb` extension is added for you if you do not type
one. Every save rewrites the whole file.

On every one of the three ways a database can be protected, the encryption happens in memory and
the file is written once, with the encrypted result. The readable content is never written
anywhere, not even into a temporary file. The test suite pins exactly that: the text of an entry
must not be findable anywhere in the encrypted bytes, and the first thing a save writes is already
encrypted.

### Two files next to it, for a moment

A save is built so that it cannot leave you with half a database:

- The new content is first written to `<name>.mcrdb.part` next to the database, flushed to the
  disk, and then moved onto the database in one step. A save that fails before the move - a full
  disk, a removed stick - leaves the previous database exactly as it was, and the `.part` file is
  removed again.
- Before the move, the previous database is copied to `<name>.mcrdb.bak`. It stays there until the
  new file has been read back once: the next successful sign-in to the database removes it. Until
  then it is your way back to the state before the last save. It is encrypted just like the
  database, with the credentials the database had then.

### What is inside

Internally the database is XML, and that XML is what gets encrypted. It holds:

- **Your entries**, as the tree you see in the main window: the nodes with their names, and in
  them each entry's title, user name, password, URL, notes, icon, whether the entry expires and
  when, attached files, and any custom properties you added. Every entry has an identifier of its
  own. Where they are known, for example after a KeePass import, an entry also carries its
  creation, access and modification times, its exact expiry time, its KeePass icon, its earlier
  versions and which of its custom properties KeePass marked as protected.
- **The sign-in settings of the database**: the application file and key file paths it remembers
  for the dropdowns, and the flags saying whether it is guarded by a master password, by a key
  file, or by both.
- **The format version** it was written in, see section 7.

What is **not** inside: the master password and the private key of a key file. Both come from
whoever signs in and are held in memory for the session only, so a decrypted copy of the database
does not carry the credential that opens it.

## 2. What protects a database guarded by a password alone

A password-only database written by the current version starts with a six byte marker, `MCRDB2`,
followed by a 16 byte salt and a four byte number recording the cost of the key derivation. The
rest of the file is the encrypted XML.

- Your master password is never used as a key directly. The key is derived from it with
  **PBKDF2-HMAC-SHA256**, 600,000 rounds (the current OWASP recommendation for that function,
  measured at roughly a tenth of a second), producing a 256 bit key.
- The **salt is drawn fresh on every single save**. Two saves of the same database never produce
  the same bytes.
- The payload is **AES-GCM, key-committing**. Marker, salt and iteration count are fed to the
  cipher as associated data, so none of the three can be altered without the file refusing to
  open.

Three practical consequences:

1. A wrong password fails as a wrong password, immediately and every time. It cannot produce
   plausible looking rubbish.
2. A file that was tampered with, even by a single flipped bit, fails to open instead of opening
   with changed content.
3. The cost of the key derivation is written into each file, so it can be raised in a future
   release without making the files you have today unreadable.

## 3. What changes when a key file is used as well

The format described above is the format of a database guarded by a **master password alone**.
The moment a key file is involved, the file is written differently:

- **Key file only**: on every save the application draws a fresh random AES key (128 bit), encrypts
  the XML with it in GCM mode, and locks that key with the public key belonging to the private key
  in your key file. Opening the file requires that private key. The master password plays no part.
- **Password and key file**: the XML is first encrypted with your master password - password
  based AES (128 bit, CBC) with a key derived over SHA-1 in 65,536 rounds and a fresh random salt
  on every save - and the result is then encrypted to the key from your key file as above. Both
  are needed to get back in, which is why [[Signing-In]] calls this the strongest of the three
  options: possession of the key file alone does not open the database, and knowing the password
  alone does not either.

Neither of the key file variants carries the `MCRDB2` marker. The key file is a private key file,
such as the RSA key in PEM format that **Create key file...** writes when you create a database.

## 4. Files written by older versions

Releases up to and including 8.1.1 encrypted a password-protected database with
`PBEWithMD5AndDES`, using a salt and an iteration count that were constants inside a library
published on Maven Central. That meant the same eight salt bytes on every installation in the
world, which is precisely what a salt must never be, since one precomputation then works against
everybody's file. Together with a 56 bit cipher and 19 iterations, very little stood between that
file and whoever held it, and because that cipher authenticates nothing, a wrong password produced
rubbish instead of an error.

Such files are still readable, and so are the databases 7.x wrote and those 8 wrote with a key
file. **They migrate by being used**: open the database once with the current version and save it,
and it is rewritten in the current format. There is deliberately no way to write an old format
again. The old file is read in memory; nothing decrypted is placed on the disk for it.

> **Before that first save, make a copy of your `.mcrdb` file and put it somewhere safe.** The
> migration is one way. The `.bak` file a save keeps is removed at the next sign-in, so a copy of
> your own is the way back to the state you had.

## 5. If you lose the master password

There is nothing to recover with. The key that decrypts the file is derived from your master
password (and, in the key file variants, from the private key in your key file); it is not stored
anywhere, and the application has no recovery mechanism, no backdoor and no reset. A lost master
password, or a lost key file where one is required, means the contents of that database are gone.

The credentials are therefore as worth backing up as the database itself, and they belong
somewhere separate from the database file.

## 6. What someone who copies your file can and cannot do

**Can:**

- See that the file exists and how large it is, and, for a password-protected database in the
  current format, read the marker, the salt and the iteration count. All three sit in the clear at
  the front of the file by design.
- Tell whether the file is in the current format or is still an unmigrated old one, by whether the
  marker is there.
- Guess your master password offline, as often as they like. This is the real attack, and the only
  thing standing in its way is the strength of your password and the cost of the key derivation:
  every single guess costs a full PBKDF2 run at the iteration count recorded in the file.
- Take the `<name>.mcrdb.bak` next to it as well, while it is there: it is the previous save, and
  it is protected the same way.

**Cannot:**

- Read a single entry without the password (or, for the key file variants, without the key file).
  The encrypted bytes contain nothing readable.
- Reuse work done against somebody else's file, or against your own earlier saves. Because the
  salt is drawn fresh for every save, one precomputed table does not work against everybody.
- Change anything in a password-protected file and hand it back to you unnoticed. Any
  modification, in the payload or in the header, makes the file refuse to open rather than open
  with altered content.
- Learn from comparing two copies of the same database whether anything inside changed. The same
  content saved twice produces different bytes.

The one thing this protection cannot make up for is a weak master password. Everything above
assumes the password is not guessable.

## 7. Files written by a newer version

The XML records the format version it was written in; the current one is `2`. A database written
by a newer version of the application, in a higher format version, **opens read-only**. Right
after signing in a dialog **Opened read-only** says so and names the format version the file
needs. You can read everything this version understands, but **Save**, **Save As...** and the
toolbar's Save are disabled, and every change - an entry, a node, an import - is refused with the
same dialog. The reason is that this version skips what it does not know while reading; saving
would remove that part from the file. Open the database with a version that reads its format to
change it.

## 8. Planned: a new content format for 9.0

A [decision record](https://github.com/astrapi69/mystic-crypt-ui/blob/develop/docs/decisions/vault-content-format-9.0-2026-11-10.md)
of this project plans a new format for the content inside the encryption, for version 9.0, and
the work on it is open in [#865](https://github.com/astrapi69/mystic-crypt-ui/issues/865) and
[#866](https://github.com/astrapi69/mystic-crypt-ui/issues/866). Nothing of it is built yet:
everything on this page describes the files the application writes today. As planned, 9.0 keeps
reading every format above, converts a database when it is saved, and keeps the previous file as
a backup before the first conversion. A database 9.0 has saved will not open in 7.x or 8.x.

## See also

- [[Getting-Started]]
- [[Signing-In]]
- [[create-new-mystic-crypt-database]]
- [[open-existing-mystic-crypt-database]]
- [[Secure-your-passwords-with-obfuscation]]
- [[import-export-keepass-database]]
