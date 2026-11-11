# The Database File

Everything you keep in mystic-crypt lives in one file, the `.mcrdb` file you picked when you
created the database. This page explains what that file is, what protects it, what happens to a
file written by an older version, and what someone who gets hold of a copy can actually do with
it.

## 1. What a `.mcrdb` file is

The `.mcrdb` file is your whole database, encrypted. It is a single ordinary file: you choose
where it lives when you create it, and the `.mcrdb` extension is added for you if you do not type
one. There is no companion file, no index, no journal next to it, and every save rewrites the whole
file.

When the database is guarded by a password, the encrypted bytes go from memory into the file
directly and the readable content is never written anywhere. When it is guarded by a key file
alone, that is currently not true: the readable database is written to the file first and
overwritten with the encrypted bytes immediately afterwards, so an interrupted save can leave it
readable on disk. That is a defect and is being fixed; until it is, prefer a master password, or
at least know that the moment exists.

Internally the database is XML, and that XML is what gets encrypted. It holds:

- **Your entries**, as the tree you see in the main window: title, user name, password, URL,
  notes, icon, whether the entry expires and when, attached resources, and any custom properties
  you added, along with creation, access and modification timestamps where they are known (for
  example after a KeePass import).
- **The sign-in settings of the database**: the application file and key file paths it remembers
  for the dropdowns, and the flags saying whether it is guarded by a master password, by a key
  file, or by both.
- **The master password itself**, which is stored in the xml as plain text. It is inside the
  encrypted file, so reading it means opening the database first, but it means that any decrypted
  copy of the database carries the credential as well as the data. That is a defect and is being
  fixed.

Nothing readable is left in the file. The test suite pins exactly that: the text of an entry must
not be findable anywhere in the encrypted bytes.

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
   plausible looking rubbish any more.
2. A file that was tampered with, even by a single flipped bit, fails to open instead of opening
   with changed content.
3. The cost of the key derivation is written into each file, so it can be raised in a future
   release without making the files you have today unreadable.

## 3. What changes when a key file is used as well

The format described above is the format of a database guarded by a **master password alone**.
The moment a key file is involved, the file is written differently:

- **Key file only**: on every save the application draws a fresh random AES key, encrypts the XML
  with it in GCM mode, and locks that key with the public key belonging to the private key in your
  key file. Opening the file requires that private key. The master password plays no part.
- **Password and key file**: the XML is first encrypted with your master password (with its own
  fresh random salt), and the result is then encrypted to the key from your key file. Both are
  needed to get back in, which is why the [[Getting-Started]] page calls this the strongest of the
  three options: possession of the key file alone does not open the database, and knowing the
  password alone does not either.

Neither of the key file variants carries the `MCRDB2` marker, and the section below about
migration does not apply to them.

## 4. Files written by older versions

Releases up to and including 8.1.1 encrypted a password-protected database with `PBEWithMD5AndDES`, using
a salt and an iteration count that were constants inside a library published on Maven Central.
That meant the same eight salt bytes on every installation in the world, which is precisely what a
salt must never be, since one precomputation then works against everybody's file. Together with a
56 bit cipher and 19 iterations, very little stood between that file and whoever held it, and
because that cipher authenticates nothing, a wrong password produced rubbish instead of an error.

Such files are still readable, and **they migrate by being used**: open the database once with the
current version and save it, and it is rewritten in the format from section 2. There is
deliberately no way to write the old format again.

> **Before that first save, make a copy of your `.mcrdb` file and put it somewhere safe.** The
> migration is one way. A copy taken beforehand is your way back to the state you had, and a
> backup is worth having whenever a file is about to be rewritten in a new format.

One detail worth knowing about the one-time read of an old file: the decryptor used for the legacy
format only works on files, so it briefly places the decrypted database next to the encrypted one.
It is removed again right away, but removing is not the same as wiping. This path is walked at
most once per database, since the next save writes the new format.

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

**Cannot:**

- Read a single entry without the password (or, for the key file variants, without the key file).
  The encrypted bytes contain nothing readable.
- Reuse work done against somebody else's file, or against your own earlier saves. Because the
  salt is drawn fresh for every save, one precomputed table no longer works against everybody, and
  that was the central weakness of the old format.
- Change anything in the file and hand it back to you unnoticed. Any modification, in the payload
  or in the header, makes the file refuse to open rather than open with altered content.
- Learn from comparing two copies of the same database whether anything inside changed. The same
  content saved twice produces different bytes.

The one thing this protection cannot make up for is a weak master password. Everything above
assumes the password is not guessable.

## See also

- [[Getting-Started]]
- [[create-new-mystic-crypt-database]]
- [[open-existing-mystic-crypt-database]]
- [[Secure-your-passwords-with-obfuscation]]
- [[import-export-keepass-database]]
