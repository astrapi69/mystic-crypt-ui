# A KDBX 4 database written by KeePassXC

`keepassxc-2.7.10-kdbx4.kdbx`

## The password is `fixture`, and it is test material

It is written here on purpose. This file contains nothing but invented entries, it exists so that a
test has something a third party wrote, and a fixture whose password lives somewhere else is a
fixture nobody can use. Nothing in it opens anything.

## Why this file exists

Every KDBX 4 file this project had before was written by the library this project itself uses
(KeePassJava2), so reading one back proved only that our writer and our reader agree. This one was
written by KeePassXC, which is the point: it is the only way to measure whether the import reads a
KDBX 4 database somebody else produced.

The other fixture in this repository, `src/test/resources/test-db.kdbx`, is KDBX **3.1**.

## What it is, measured

`keepassxc-cli db-info`, run before the file was placed here:

```
UUID:{ce83182d-807e-4046-8835-182a7a310bac}
Name: kdbx4-fixture
Verschlüsselungsalgorithmus:AES 256-Bit
KDF:Argon2d (235 Runden, 16384 KB)
Anzahl der Gruppen: 2
Anzahl der Einträge: 1
```

The file header reads `sig1=0x9aa2d903 sig2=0xb54bfb67`, version **4.0**. Argon2d is what makes it a
KDBX 4 file rather than a 3.1 one with a 4 in the header, so both were checked, not just the number.

Written with **KeePassXC 2.7.10**.

## What is in it

One group, one entry, one history version.

| | |
|---|---|
| groups | `Root` / `Team` |
| entries | **one**, `Titel test foo`, in `Team` |
| user name / password | `foo` / `bar` |
| URL | `http://example.com` |
| notes | `das kommt nicht in Fräge mit ümlauteöüä` - deliberately carries real umlauts |
| icon index | 57 |
| custom attributes | `foo=bar`, `key=value` |
| attachment | `foo.txt` |
| expiry | `2026-09-30T07:23:08Z`, `Expires=True` |
| history | one previous version, without the custom attributes and without the attachment - its title, user name, password, URL and notes are the same as the current entry's |
| created / modified | `2026-09-16T07:21:23Z` / `2026-09-16T07:26:13Z` |

One entry, not two: the inventory this file was requested with said two, and what arrived has one.
It is recorded here as measured rather than as asked for, because everything the fixture is needed
for - identifier, timestamps, icon index, custom attributes, attachment, expiry, history - is
present in the one entry it has.

## Checksum

```
sha256  5425536d78bdb1abf56b8a5ab51b4e80289909effdce3996c651f61c0953e947
```

If the file is ever replaced, this line and the table above are replaced with it, measured again -
a fixture description that drifts from the fixture is worse than none, because tests that fail
against it look like product defects.

# A KDBX 3.1 database with a protected attachment

`keepass2-kdbx31-protected-attachment.kdbx`, used by `AKdbx31FileWithAProtectedAttachmentIsRefusedUiTest`
(#568).

## The password is `foo-secret-bar-1969-?`, and it is test material

The same as `src/test/resources/test-db.kdbx`, which this file is built from
(`TestPasswords.KEEPASS_FIXTURE`).

## Why this file exists

KeePassJava2's author states that the library cannot read a KDBX 3.1 file with a protected attachment
(jorabin/KeePassJava2#114, comment of 2026-10-10). This file measures what the import does with one:
on KeePassJava2 3.1.0 it refuses it, with a message, and nothing reaches the vault.

## How it was built

KeePassXC cannot write this case. `keepassxc-cli db-create` has no choice of format, and
`keepassxc-cli attachment-import` on a KDBX 3.1 file keeps the file at 3.1 but writes the attachment
as `<Binary ID="0" Compressed="True">`, without `Protected="True"` (read from the decrypted XML,
2026-10-13). So the protection is added by `scripts/fixtures/BuildProtectedAttachment31.java`, the
way KeePass 2 writes a protected binary: `Protected="True"`, not compressed, XORed with the file's
inner Salsa20 stream in document order. Every protected value after it is encrypted again, because
the stream position moves; `HeaderHash` is dropped, because the header is written anew.

1. Copy `src/test/resources/test-db.kdbx` (written by KeePass 2: `Generator` reads `KeePass`; KDBX 3.1,
   AES-KDF).
2. `keepassxc-cli attachment-import <copy> "Sample Entry" protected.txt <file>`, with KeePassXC 2.7.10.
   The file holds the 38 bytes `secret attachment content, 2026-10-13` and a line break.
3. `java -cp <classpath> scripts/fixtures/BuildProtectedAttachment31.java <copy> <password> <out>`. The
   class path is the KeePassJava2 3.1.0 jars (`KeePassJava2.kdbx.database`, `KeePassJava2.kdbx.io`,
   `database`), `bcprov-jdk18on` 1.86, commons-codec and Guava from the Gradle cache.

## What it is, measured

With keepassxc-cli 2.7.10, against the built file:

```
db-info:                 AES 256-Bit, KDF: AES (60000 Runden), 7 groups, 2 entries
file header:             version 3.1
show -s -a Password:     both entries' passwords identical to the base file (compared as SHA-256)
attachment-export:       protected.txt identical to the input, 38 bytes (cmp)
raw XML:                 <Binary ID="0" Protected="True">
```

So a second, independent implementation reads the file as a valid KDBX 3.1 database with a protected
attachment. SHA-256 of the file: `4c0a979083ee1d1c2f0ba927eed5fc2ba085e7b8e361820c34f01c9351e37b9c`,
2362 bytes.

## What KeePassJava2 3.1.0 does with it

`KdbxDatabase.read` throws `UnrecognizedPropertyException: Unrecognized field "Protected" (class
org.linguafranca.pwdb.kdbx.jackson.model.KeePassFile$Binary), not marked as ignorable (3 known
properties: "", "Compressed", "ID")`. The same file without the protection reads both entries and the
attachment.

# The same file, saved as KDBX 4 by KeePassXC

`keepassxc-2.7.10-kdbx4-saved-from-kdbx31-protected-attachment.kdbx`, used by
`AKdbx31FileSavedAsKdbx4InKeePassXcIsImportedUiTest` (#568). The password is the same,
`foo-secret-bar-1969-?`.

## Why this file exists

The import refuses `keepass2-kdbx31-protected-attachment.kdbx` and tells the person how to get the
file in: save it as KDBX 4 in KeePassXC. The message names that way only because this file shows
that it works - its test imports it through the menus and finds the attachment byte for byte.

## How it was made, measured

On 2026-10-13, in KeePassXC 2.7.10's window, on an Xvfb of its own and with a configuration of its
own: `keepass2-kdbx31-protected-attachment.kdbx` opened, then

| | English | German |
|---|---|---|
| menu | Database > Database Settings... | Datenbank > Datenbankeinstellungen... |
| page | Security | Sicherheit |
| tab | Encryption Settings | Verschlüsselungs-Einstellungen |
| choice | Database format: KDBX 4 (recommended) | Datenbankformat: KDBX 4 (empfohlen) |

then OK and Ctrl+S. The labels were read off screenshots of both languages.

- file header version **4.0**; the key derivation changed with the format, from AES-KDF (KDBX 3) to
  Argon2d (KDBX 4)
- KeePassJava2 3.1.0 reads both entries, and `protected.txt` with 38 bytes, `secret attachment
  content, 2026-10-13`
- SHA-256 `67d4d22526446ae35561160ad488130510e022752b53964ef74d80ae2a00b09e`, 2949 bytes

Not a way: KeePassXC's command line. `keepassxc-cli db-create` and `keepassxc-cli import` both write
KDBX 3.1 with AES-KDF (measured the same day), and the command line has no setting for the format.
