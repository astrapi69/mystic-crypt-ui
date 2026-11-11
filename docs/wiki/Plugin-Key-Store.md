# Manage Key Store

## 1. What it is for

A key store is one password-protected file that holds private keys together with the
certificates that belong to them, in one of the formats a Java application or a server reads:
PKCS12, JKS or JCEKS. This tool opens such a file, shows every entry it contains, generates new
key pairs with a self-signed certificate, takes in a key that a certificate authority handed
out, imports and exports certificates, holds a symmetric AES key and removes entries again.

Reach for it whenever the keys have to live *inside* a store file. If you only want loose key
files on disk, "Key Generation" produces those; "Create Certificate..." makes a certificate on
its own; "Convert DER to PEM" changes the encoding of a key file you already have. This tool is
the one that deals with the container.

## 2. Where to find it

**Plugins** menu, submenu **Key Stores**, item **Manage Key Store**.

The item opens a window titled "Manage Key Store" inside the application. The Plugins menu only
appears once at least one plugin is installed, so if you do not see it, install the plugin first
(see [[install-a-plugin]]).

## 3. Using it

The window is one form. The top part decides *which* store you work on, the table in the middle
shows what is in it, the lower part is what you do to it. A bold line at the very bottom always
reports what the last button did.

1. **Point at the file.** Type the path into **Key store file:** or pick it with the **...**
   button next to it.
2. **Enter the password** into **Store password:**. The same password protects the file and
   every key entry the tool puts into it.
3. **Open an existing store** with **Open**, or **create a new one** with **Create**.
   - Opening works out from the file itself what kind of store it is, so the **Type:** box does
     not have to be right beforehand. When the file turns out to be something other than what
     the box said, the box is corrected and the line at the bottom says
     `opened server.p12 - it is a JKS store`. Otherwise it says `opened server.p12`.
   - Creating writes an empty store of the type selected in **Type:** (PKCS12, JKS or JCEKS) and
     reports `created server.p12`.
   - When it does not work, the line starts with `not opened:` or `not created:` and gives the
     reason, for instance `not opened: keystore password was incorrect`.
4. **Read the table.** One row per alias, with the columns *alias*, *kind*, *algorithm*,
   *subject*, *valid until* and *SHA-256 fingerprint*. *kind* is either `private key` or
   `certificate`. The table refreshes itself after every operation, and it is never edited
   directly; the buttons do the changing.
5. **Add a generated key pair.** Fill in **Alias:**, **Distinguished name:** (the subject of the
   self-signed certificate, for instance `CN=my-server, O=example`) and **Key algorithm:**
   (RSA, EC or ML_DSA_65), then press **Add key pair**. The result reads
   `added 'server' (RSA)` followed by how long the generation took, for instance
   `added 'server' (RSA) in 412 ms`. Without an alias it reads `not added: enter an alias`;
   before a store is open, `not added: open or create a key store first`.
6. **Take in a key that came from somewhere else.** Put the key file into **Private key file:**
   and the certificate that belongs to it into **Certificate file:** (both PEM or DER, both with
   a **...** button), give an **Alias:** and press **Import key + certificate**. Success reads
   `imported the key and its certificate as 'server'`, and the entry appears with kind
   `private key`. If the two halves do not match, nothing is stored and the line reads
   `not imported: the private key and the certificate do not belong together - the key is EC, the certificate holds a RSA key`.
7. **Import a certificate alone** (someone else's, to trust it): **Certificate file:** plus
   **Alias:**, then **Import certificate**. Result: `imported the certificate as 'trusted'`,
   listed with kind `certificate`.
8. **Export a certificate**: select its row in the table (or type its name into **Alias:**) and
   press **Export certificate as PEM**. Result: `exported 'server' to server.pem`. When
   **Certificate file:** is empty, the file is written next to the key store as
   `<alias>.pem`, and the field is filled in with the path that was used.
9. **Look at a certificate in full**: select the row and press **Details...**. A dialog
   "Certificate of 'server'" shows *Issued to*, *Issued by*, *Valid from*, *Valid until* (with
   `(expired)` appended when that date has passed), *Key*, *Signed with*, *Serial number*,
   *X.509 version* and the *SHA-256* fingerprint, with the certificate itself as PEM underneath,
   ready to select and copy.
10. **Add a symmetric key**: give an **Alias:** and press **Add secret key**. An AES key of the
    configured length is generated and stored: `added the 256 bit AES key 'the-secret'`.
11. **Remove an entry**: select its row and press **Delete alias**. Result: `deleted 'trusted'`.

Every one of these writes the key store file immediately. There is no save step that could be
forgotten, and equally no undo.

## 4. Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Key Store** in the list on
the left. Change a value in the *value* column, then press **Apply**; **Reset to defaults** drops
the stored file and shows the declared defaults again. The tool takes these values when its
window opens, so close and reopen "Manage Key Store" after changing them.

| Key | Default | What it changes |
|---|---|---|
| `default.store.type` | `PKCS12` | The type the **Type:** box starts on. PKCS12, JKS or JCEKS. |
| `default.key.algorithm` | `RSA` | The algorithm the **Key algorithm:** box starts on. RSA, EC or ML_DSA_65. |
| `default.distinguished.name` | `CN=mystic-crypt, O=mystic-crypt` | What **Distinguished name:** is prefilled with, the subject of a generated certificate. |
| `default.key.size` | `2048` | The key size for a generated key pair. Only used by the size based algorithms such as RSA. |
| `default.days.valid` | `365` | How many days a generated self-signed certificate stays valid. |
| `default.secret.key.size` | `256` | The length of a generated AES key: 128, 192 or 256. |

A value that does not name one of the offered constants is not an error and produces no warning:
the tool falls back to the built-in default (the first offered type or algorithm, 256 bits for a
secret key of any other length). Write the algorithm exactly as it is listed, with underscores:
`ML_DSA_65`, not `ML-DSA-65`.

## 5. On the command line

This plugin contributes no commands of its own. Key stores are covered by the `keystore` command
of the mystic-crypt library, which the application makes available through `--cli`, with the
subcommands `list`, `create`, `add-keypair`, `import-cert`, `export-cert` and `delete`. The
runnable jar carries the version in its file name; the examples below shorten it to
`mystic-crypt-ui-all.jar`.

Every subcommand takes the store with `-f`/`--file`, `-t`/`--type` (default `PKCS12`) and the
password with `-p`/`--password`. Given `-p` without a value it asks for the password
interactively; in a script prefer `--password-stdin`, which reads it from the first line of
standard input and keeps it out of the process arguments.

Create a store and put a key pair into it:

```console
$ java -jar mystic-crypt-ui-all.jar --cli keystore create -f demo.p12 -p changeit
created demo.p12

$ java -jar mystic-crypt-ui-all.jar --cli keystore add-keypair \
    -f demo.p12 -p changeit -a server -d "CN=my-server, O=example"
added 'server' (RSA)
```

List what it holds. One tab-separated line per alias (alias, kind, algorithm, subject, validity
end, SHA-256 fingerprint), then the count:

```console
$ echo "changeit" | java -jar mystic-crypt-ui-all.jar --cli keystore list -f demo.p12 --password-stdin
server	private key	SHA256withRSA	O=example,CN=my-server	Thu Aug 26 12:47:39 CEST 2027	73d511794f95095a084a483f43be0b33a2432917ab99e2509a3545e2c3483353
1 entries
```

Export a certificate, import it back under another alias, and remove it again:

```console
$ java -jar mystic-crypt-ui-all.jar --cli keystore export-cert -f demo.p12 -p changeit -a server -o server.pem
exported 'server' to server.pem

$ java -jar mystic-crypt-ui-all.jar --cli keystore import-cert -f demo.p12 -p changeit -a trusted -c server.pem
imported the certificate as 'trusted'

$ java -jar mystic-crypt-ui-all.jar --cli keystore delete -f demo.p12 -p changeit -a trusted
deleted 'trusted'
```

A post-quantum key pair, with the algorithm written the way the command line spells it:

```console
$ java -jar mystic-crypt-ui-all.jar --cli keystore add-keypair \
    -f demo.p12 -p changeit -a pq -A ML-DSA-65 -d "CN=quantum safe"
added 'pq' (ML_DSA_65)
```

Two differences to the window are worth knowing. The command line follows the library's own
defaults (`CN=mystic-crypt`, RSA, `--key-size 2048`, `--days-valid 365`) and ignores what is
configured for the plugin. And it refuses what the window silently allows: an existing file for
`create`, an alias that is already taken for `add-keypair` and `import-cert`. Errors arrive as a
Java stack trace whose first line carries the message.

## 6. Limits and pitfalls

**Create on an existing file empties it.** Press **Create** with a path that already holds a key
store and you get an empty store at that path; the entries that were in it are gone, with no
question asked and no backup. Use **Open** for a file that exists. (On the command line the same
mistake is refused: `'demo.p12' already exists; delete it first or choose another file.`)

**An alias that is already taken is overwritten, not refused.** **Add key pair** and **Import
certificate** replace whatever the alias held, without warning; only the result line ("added
'server' ...") hints that anything happened. Check the table before reusing a name. The command
line refuses this: `'server' already exists in demo.p12; delete it first or choose another alias.`

**The table selection is gone after every operation.** **Delete alias**, **Export certificate as
PEM** and **Details...** work on the selected row, and fall back to whatever stands in
**Alias:** when nothing is selected. Since the table is rebuilt after each button press, the
selection is cleared exactly when you are most likely to reach for Delete next, and the Alias
field then decides. Re-select the row you mean, or clear the Alias field.

**Delete alias does not work on a JCEKS store.** It fails with
`not deleted: Invalid keystore format`, whatever the file really is; PKCS12 and JKS stores delete
fine. For a JCEKS store use `--cli keystore delete`, which handles all three types.

**A JKS store cannot hold a symmetric key.** **Add secret key** on one ends with
`not added: Cannot store non-PrivateKeys`. PKCS12 and JCEKS accept it.

**A symmetric key is listed as a `private key`** with the algorithm, subject, validity and
fingerprint columns empty, because there is no certificate behind it. The `keystore list`
command distinguishes it properly and calls it `secret key`.

**The key size setting only applies to RSA.** EC and ML-DSA have fixed parameter sets and ignore
`default.key.size` entirely, so raising it changes nothing for them. The command line says so out
loud instead: `--key-size only applies to the size-based algorithms RSA, DSA and DH; 'EC' has a fixed parameter set.`

**A wrong store password gives no useful guess about the file.** The type detection also runs on
the password, so with the wrong one nothing is detected, the **Type:** box is used as it stands,
and opening fails. The reason is passed through from the library and can be as short as the name
of an error class when the library supplies no message.

**One password for everything.** Every entry this window writes is protected with the store
password. There is no field for a separate key password, so a store in which each key is meant
to have its own cannot be built here.

**Details... and Export need an actual certificate.** An alias that holds none, or one that does
not exist, is refused with `not shown: 'the-secret' holds no X.509 certificate` and
`not exported: ...` respectively. This is what you hit when a secret key entry is selected.

**Only three store types and three key algorithms.** The window offers PKCS12, JKS and JCEKS,
and RSA, EC and ML_DSA_65. Hardware-backed stores (PKCS11) and domain key stores (DKS) are not
offered, since they need external hardware or a configuration file. The command line accepts
more key algorithms (DSA, RSASSA-PSS, ML-DSA-44 and ML-DSA-87) than the window does.

**Changed settings need a fresh window.** The defaults are read when "Manage Key Store" opens.
An already-open window keeps what it started with.

## 7. Under the hood

The store handling, the key generation and the certificate work come from the crypt-data library
on top of BouncyCastle, so nothing here is home-made cryptography. A generated entry is an X.509
version 3 self-signed certificate whose signature algorithm is chosen to match the key: SHA-256
with RSA, SHA-256 with ECDSA, or ML-DSA-65 for the post-quantum key, which is why an algorithm
that can only do key exchange is refused rather than mapped to something it cannot do. The
fingerprints in the table and in the details dialog are SHA-256 over the certificate.

"Import key + certificate" does not merely compare algorithm names, which would be misleading
because the same EC key calls itself "ECDSA" in one place and "EC" in the other. It actually
signs a probe with the private key and verifies it against the certificate's public key. That is
why a mismatch is caught before anything is written.

The password is held as a character array while the window is open, and the key store file is
rewritten in full after each operation.

## 8. See also

- [[install-a-plugin]] - how the plugin gets into the application in the first place
- [[Getting-Started]] - signing in, which comes before any plugin tool
- [[how-to-create-a-plugin]] - for developers, how a tool like this one is built
