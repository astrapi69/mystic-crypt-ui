# mystic-crypt-ui

A password manager for the desktop, and a workbench for the everyday cryptographic jobs that
usually send people looking for a command line they half remember.

Everything stays on your machine. There is no account, no sync service and nothing to sign up for:
your data lives in one file that you put where you want it, and only you can open it.

## What it holds

Your entries live in a tree you arrange yourself, the way folders work. An entry keeps the usual
things - title, user name, password, address, notes - and beyond that whatever you need: your own
named fields, and files attached to the entry itself, so the recovery codes sit with the account
they belong to instead of in a folder somewhere.

You can search the whole database by title or by any field, copy a user name and password without
showing them, and move entries around as your arrangement changes.

Coming from KeePass? A `.kdbx` database can be imported, with a password, a key file or both, and
your database can be exported back to a `.kdbx` that real KeePass opens. See
[[import-export-keepass-database]].

## What protects it

One file, encrypted whole, opened by a master password, by a key file, or by both together. A
master password alone is turned into a key with 600,000 rounds of PBKDF2 and a random salt for
every save, and the file is sealed with AES-GCM, so a changed byte is refused rather than quietly
decrypted into something wrong. With a key file the file is written differently: it is encrypted to
the key in your key file, and with both, first with the password and then to the key.

There is no recovery. Nobody can open your database without what you chose to protect it with,
which is the point, and which is also why a forgotten master password means the data is gone.
[[The-Database-File]] explains what is in the file, what an attacker who copies it can and cannot
do, and what happens to databases written by older versions.

## The tools

The tools come with the application, each in its own window under the **Plugins** menu, and most
with settings of their own in the settings dialog:

| | |
|---|---|
| **Keys and certificates** | [[Plugin-Key-Generator]], [[Plugin-Key-Store]], [[Plugin-Certificate-Wizard]], [[Plugin-Key-Conversion]] |
| **Protecting things** | [[Plugin-File-Encryption]], [[Plugin-Secret-Sharing]], [[Plugin-Password-Hashing]] |
| **Proving things** | [[Plugin-Signatures]], [[Plugin-Checksum-And-Mac]] |
| **Exchanging a key** | [[Plugin-Key-Exchange]] |
| **The rest** | [[Plugin-Obfuscation]], [[Plugin-Console]] |

One more, [[Plugin-Menu-Designer]], is a tool for working on the application itself: it is built
with it but not installed, and shows up only when the application is started with
`-Dmystic.crypt.ui.menu.designer=true`.

Post-quantum algorithms are not an afterthought here: ML-KEM and ML-DSA sit next to RSA and the
elliptic curves, and the key exchange offers a hybrid that stays safe as long as either half does.

The application is extensible: a plugin is a `.zip` file, and installing one means putting it where
the application looks. See [[install-a-plugin]], and [[how-to-create-a-plugin]] if you want to
write your own.

## Without the window

Most of what the tools do is also available from the command line, for the jobs that belong in a
script: hashing and verifying passwords, generating keys, certificates, checksums, signatures,
encrypting a file, splitting a secret, exchanging a key. See [[The-Command-Line]].

## Getting started

You need **Java 25 or later** - the application is compiled for it and an older runtime cannot
start it.

1. [[Getting-Started]] walks through the first launch and your first database
2. [[create-new-mystic-crypt-database]] and [[open-existing-mystic-crypt-database]] cover the two
   ways in
3. [[Signing-In]] explains master password, key file, and what locking the workspace does
4. [[The-Entry-Tree]] is the day-to-day: adding, editing, searching, moving, attachments
5. [[Settings-And-Plugins]] covers the settings dialog and managing plugins

The source, the issue tracker and the releases are at
[github.com/astrapi69/mystic-crypt-ui](https://github.com/astrapi69/mystic-crypt-ui). It is free
software under the MIT licence, and problem reports are welcome.
