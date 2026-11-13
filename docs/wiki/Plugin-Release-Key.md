# Release Key Status

This tool shows what a GnuPG home holds: every OpenPGP key in it, what each key is for, when it
expires, whether its secret part is on this computer or kept offline, and whether gpg keeps a
revocation certificate for it. It is meant for whoever signs releases with an OpenPGP key and wants
to see, before a release, that the signing subkey is here, the primary key is not, nothing has
expired and a revocation certificate is at hand. It is just as useful for any other OpenPGP key you
keep with gpg.

It only reads. Nothing is written to the GnuPG home, nothing is decrypted, no passphrase is asked
for, and gpg itself is never run.

## Where to find it

**Plugins** menu, submenu **Release Key**, item **Key Status**.

The item only appears once the release key plugin is installed and enabled; the installer ships it.
If the **Plugins** menu is missing entirely, no plugin is installed yet; see [[install-a-plugin]].
The item is enabled while a database is open; before you sign in, and while the workspace is
locked, it is greyed out. Locking the workspace closes an open **Release Key Status** window.

The window's texts are in English only: the plugin has no German translation yet.

## Using it

The window opens inside the main frame and reads the configured GnuPG home at once (see
[Settings](#settings)), so it usually shows something before you touch anything.

1. **GnuPG home:** is the directory to read. It starts with the home from the settings; when that is
   blank, with the one gpg itself uses: the directory `GNUPGHOME` names, else `~/.gnupg`. The
   **...** button opens a directory chooser that also shows hidden directories, so `~/.gnupg` can be
   picked.
2. **Release key:** narrows the table to one certificate. Type the fingerprint or the long key id of
   its primary key; left blank, every certificate of the home is shown.
3. **Read** reads the home again with what the two fields say now.

The table has one row per key, the primary key of each certificate first and its subkeys below it.
The bold line under the table says what was read, or why nothing could be:

| Line | Meaning |
|---|---|
| `3 certificate(s) in /home/you/.gnupg` | No release key named; every certificate of the home is in the table. |
| `The release key, read from /home/you/.gnupg` | The named key was found; the table shows its certificate only. |
| `The release key 0123... is not in /home/you/.gnupg` | No certificate of the home has that primary key. The table is empty. |
| `Could not read the GnuPG home ...: ...` | The directory is missing, or its keyring cannot be read; the reason follows the colon. The table is empty. |

An empty table with `0 certificate(s)` means the home has no keybox, or a keybox without keys. An
empty table after an error is never an empty keyring: the line says which of the two it is.

### Naming the release key

The release key is found by its primary key, not by a subkey. Any of these forms works, and they all
find the same certificate:

```
0123456789ABCDEF0123456789ABCDEF01234567
0123 4567 89AB CDEF 0123  4567 89AB CDEF 0123 4567
0x89ABCDEF01234567
89abcdef01234567
```

Upper or lower case, spaces, and a leading `0x` do not matter. The shortest accepted name is the long
key id, the last 16 hexadecimal characters of the fingerprint. A short key id of 8 characters is
not accepted, because it is too easy to collide with: it gives "is not in", whatever the home holds.

### What the columns mean

| Column | What it shows |
|---|---|
| **key** | `primary` for the certificate's primary key, `subkey` for each of its subkeys. |
| **fingerprint** | The key's full fingerprint in groups of four characters, the way gpg prints it, so it can be compared by eye. |
| **use** | What the key may do, in gpg's letters: `S` sign, `C` certify, `E` encrypt, `A` authenticate, for example `[SC]` or `[E]`. `[]` when the key carries no flags. |
| **algorithm** | The key's algorithm in the names gpg uses: `rsa4096` (with the key size), `ed25519`, `cv25519`, `nistp256`, `nistp384`, `nistp521`, another curve by its name, or `algorithm` with OpenPGP's number for anything else. |
| **created** | The day the key was made, in UTC. |
| **expires** | `never`, a day, or `expired` followed by the day it expired, compared with today in UTC. |
| **secret part** | `here`, `offline` or `unknown`, see below. |
| **revocation certificate** | Primary keys only: `valid`, `absent` or `not valid`, see below. |
| **user ID** | Primary keys only: the certificate's first user ID, for example `Your Name <you@example.org>`. |

**secret part** answers where the key's secret half is, as far as this one home can tell:

- `here`: the home's `private-keys-v1.d` holds it. gpg can sign or decrypt with this key on this
  computer. For a signing subkey that is what you want.
- `offline`: the home has no secret part for this key. For a primary key you keep on an offline
  medium, with only a stub in the daily keyring, this is the wanted state.
- `unknown`: it cannot be told. Either the key's algorithm is one the tool does not match (it
  matches RSA and elliptic curve keys, so a DSA or ElGamal key gives `unknown`), or a file in
  `private-keys-v1.d` could not be read. "offline" would then be an answer that can be wrong, so the
  tool does not give it.

The match is made without decrypting anything: every key file in `private-keys-v1.d` carries the
public half of its key in the clear, and that is compared with the keys of the keyring.

**revocation certificate** is about the file gpg writes into `openpgp-revocs.d` when it creates a
primary key, named after the key's fingerprint with `.rev` appended:

- `valid`: the file is there and holds a key revocation signature that verifies against this
  primary key. Imported when needed, it would revoke the key.
- `absent`: there is no such file. If you moved the certificate somewhere safer, off this computer,
  this is what you see, and it says nothing about that copy.
- `not valid`: a file is there, but it holds no key revocation signature that verifies against this
  primary key: it is unreadable, belongs to another key, or is a signature of another kind. It would
  not revoke the key when you need it to.

## Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Release Key** in the list on the
left. Change the value cell in the table and press **Apply**. A window that is already open keeps
what it started with; the next **Key Status** window starts with the new values.

| Key | Default | What it changes |
|---|---|---|
| `gnupg.home` | blank | The GnuPG home the window starts with. Blank means the one gpg uses: `GNUPGHOME`, else `~/.gnupg`. |
| `release.key` | blank | The fingerprint or long key id of the release key the window starts with. Blank shows every certificate of the home. |

## On the command line

The same reading is a command of the mystic-crypt library, `release-key status`. It is there whether
or not the plugin is installed, and it needs no open database. Reach it through the application jar
with the `--cli` switch, from the directory the application was installed into (see
[[The-Command-Line]]):

```
java -jar mystic-crypt-ui.jar --cli release-key status
java -jar mystic-crypt-ui.jar --cli release-key status --home /media/offline/gnupg --key 0x89ABCDEF01234567
java -jar mystic-crypt-ui.jar --cli release-key status --json
```

`--home` is the GnuPG home (default `GNUPGHOME`, else `~/.gnupg`), `--key` the one certificate to
show, `--json` prints the same facts for a script. Exit code 0 means the home was read (and the key
named with `--key` is in it), 1 that the named key is not in the home, 2 that the home cannot be
read. The plugin's settings do not reach the command line: name the home and the key there
explicitly.

## Limits and pitfalls

- **Only the keybox of GnuPG 2.1 and later is read**, `pubring.kbx`. A home that still holds only the
  old `pubring.gpg` is refused with a message that says so, instead of being shown as empty. Let a
  current gpg migrate it first.
- **A keybox larger than 64 MiB is not read.** No keybox of a person's keys comes near that; a larger
  file is refused rather than parsed.
- **The window reads files, it does not ask gpg.** gpg's agent and a smartcard are never asked, so
  what the window says about a secret part is what the files of the home say.
- **A revocation certificate kept elsewhere is not seen.** Only `openpgp-revocs.d` of the home is
  looked at.
- **Nothing here changes a key.** Extending an expiry, moving a subkey or revoking a key is done with
  gpg. Reading the home again with **Read** shows the result.

## Under the hood

The reading is done by the mystic-crypt library (`io.github.astrapi69.mystic.crypt.gnupg`), with
Bouncy Castle's OpenPGP readers; the plugin only shows what the library returns. That is the line
the decision of 2026-10-29 draws for release keys: gpg's store is read for display, never written,
and OpenPGP is done through Bouncy Castle rather than by driving gpg. The record of that decision,
and the stages that are planned after this read-only view, is
`docs/decisions/release-key-openpgp-2026-10-29.md` in the repository.

## See also

- [[The-Command-Line]] - `release-key status` with its options and exit codes
- [[Plugin-Signatures]] - signing and verifying files with keys of your own
- [[Settings-And-Plugins]] - where the plugin settings are edited
- [[install-a-plugin]] - getting the **Plugins** menu to appear in the first place
