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
