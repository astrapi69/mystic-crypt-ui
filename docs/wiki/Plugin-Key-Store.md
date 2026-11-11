# Manage Key Store

## 1. What it is for

A key store is one password-protected file that holds private keys together with the
certificates that belong to them, in one of the formats a Java application or a server reads:
PKCS12, JKS or JCEKS. This tool opens such a file, shows every entry it contains, generates new
key pairs with a self-signed certificate, takes in a key that a certificate authority handed
out, imports and exports certificates, holds a symmetric AES key and removes entries again. A
guided wizard next to it creates a new store, with a first key pair if you want one.

Reach for it whenever the keys have to live *inside* a store file. If you only want loose key
files on disk, "Key Generation" produces those; "Create Certificate..." makes a certificate on
its own; "Convert Key/Certificate..." changes the encoding of a key file you already have. This
tool is the one that deals with the container.

## 2. Where to find it

**Plugins** menu, submenu **Key Stores**. It holds two items:

- **Manage Key Store** opens a window titled "Manage Key Store" inside the application, switching
  the main window to its desktop view first if needed.
- **Create Key Store...** opens a wizard in a window titled "Create Key Store" that blocks the main
  window until you finish or cancel it.

The Plugins menu only appears once at least one plugin is installed, so if you do not see it,
install the plugin first (see [[install-a-plugin]]). Both items are greyed out until you have
signed in to a database, and again while the workspace is locked.

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
     reports `created server.p12`. A file that already exists and is not empty is refused, with
     `not created: 'server.p12' already exists; open it instead, or choose another file`.
   - When it does not work, the line starts with `not opened:` or `not created:` and gives the
     reason, for instance `not opened: keystore password was incorrect`.
4. **Read the table.** One row per alias, with the columns *alias*, *kind*, *algorithm*,
   *subject*, *valid until* and *SHA-256 fingerprint*. *kind* is either `private key` or
   `certificate`; *algorithm* is the certificate's signature algorithm, for instance
   `SHA256withRSA`. The table refreshes itself after every operation, and it is never edited
   directly; the buttons do the changing.
5. **Add a generated key pair.** Fill in **Alias:**, **Distinguished name:** (the subject of the
   self-signed certificate, for instance `CN=my-server, O=example`) and **Key algorithm:**
   (RSA, EC or ML_DSA_65), then press **Add key pair**. The result reads
   `added 'server' (RSA)` followed by how long the generation took, for instance
   `added 'server' (RSA) in 412 ms`. Without an alias it reads `not added: enter an alias`;
   before a store is open, `not added: open or create a key store first`. A name that is not a
   distinguished name is refused with `not added: 'my-server' is not a valid distinguished name -
   use the form 'CN=name', for example 'CN=example.com': ...`.
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

An alias that is already taken is refused by **Add key pair**, **Import key + certificate**,
**Import certificate** and **Add secret key** alike, for example `not added: 'server' already
exists in server.p12; delete it first or choose another alias`. Nothing is replaced behind your
back.

Every one of these writes the key store file immediately. There is no save step that could be
forgotten, and equally no undo.

### Creating a store with the wizard

**Create Key Store...** walks through three steps, **Store**, **Entry** and **Review**, with the
buttons **Previous**, **Next**, **Cancel** and **Finish** along the bottom.

1. **Store**: **Key store file:** (with a **...** button), **Type:** (PKCS12, JKS or JCEKS,
   PKCS12 first), **Store password:** and **Repeat password:**. **Next** moves on only once a
   path is given and both password fields are filled in and agree.
2. **Entry**: tick **Add a key pair now** to generate a first key pair with a self-signed
   certificate, then fill in **Alias:**, **Distinguished name:** and **Key algorithm:** (RSA, EC
   or ML_DSA_65). Left unticked, the new store stays empty and the three fields are greyed out.
   With the box ticked, **Next** moves on only once alias and distinguished name are filled in.
3. **Review**: a read-only summary with `File:`, `Type:` and either the alias, distinguished name
   and key algorithm of the first entry or `no key pair - an empty store will be created`.
4. **Finish** writes the store, adds the key pair if one was asked for, closes the wizard and
   opens a new "Manage Key Store" window. A file that already exists and is not empty is refused
   in a dialog titled `Key store creation failed`, with `Could not create the key store:` and the
   reason, and the wizard stays open.

## 4. Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Key Store** in the list on
the left. Change a value in the *value* column, then press **Apply**; **Reset to defaults** drops
the stored file and shows the declared defaults again. The tool takes these values when its
window opens, so close and reopen "Manage Key Store" after changing them. The one exception is
`default.secret.key.size`, which is read each time **Add secret key** is pressed.

| Key | Default | What it changes |
|---|---|---|
| `default.store.type` | `PKCS12` | The type the **Type:** box starts on. PKCS12, JKS or JCEKS. |
| `default.key.algorithm` | `RSA` | The algorithm the **Key algorithm:** box starts on. RSA, EC or ML_DSA_65. |
| `default.distinguished.name` | `CN=mystic-crypt, O=mystic-crypt` | What **Distinguished name:** is prefilled with, the subject of a generated certificate. Left empty, `CN=mystic-crypt` is used. |
| `default.key.size` | `2048` | The key size for a generated key pair. Only used by the size based algorithms such as RSA. |
| `default.days.valid` | `365` | How many days a generated self-signed certificate stays valid. |
| `default.secret.key.size` | `256` | The length of a generated AES key: 128, 192 or 256. |

A value that does not name one of the offered constants is not an error and produces no warning:
the tool falls back to the built-in default (the first offered type or algorithm, 256 bits for a
secret key of any other length). Write the algorithm exactly as it is listed, with underscores:
`ML_DSA_65`, not `ML-DSA-65`.

The settings belong to the "Manage Key Store" window. The **Create Key Store...** wizard does not
read them: it starts on PKCS12 and RSA with an empty distinguished name, and its key pair is
always 2048 bits for RSA with a certificate valid for 365 days.

## 5. On the command line

This plugin contributes no commands of its own. Key stores are covered by the `keystore` command
of the mystic-crypt library, which the application makes available through `--cli`, with the
subcommands `list`, `create`, `add-keypair`, `import-cert`, `export-cert` and `delete`.

Every subcommand takes the store with `-f`/`--file` and `-t`/`--type` (PKCS12, JKS or JCEKS,
default `PKCS12`). The password is never typed on the command line: run the command in a terminal
and it asks for it with a hidden prompt, `key store password:`. In a script use
`--password-stdin`, which reads it from the first line of standard input. The old
`-p`/`--password` option with a value still works in this release, prints a warning on standard
error and goes away in mystic-crypt 14.0.

Create a store and put a key pair into it:

```console
$ java -jar mystic-crypt-ui.jar --cli keystore create -f demo.p12
key store password:
created demo.p12

$ java -jar mystic-crypt-ui.jar --cli keystore add-keypair \
    -f demo.p12 -a server -d "CN=my-server, O=example"
key store password:
added 'server' (RSA)
```

List what it holds. One tab-separated line per alias (alias, kind, algorithm, subject, validity
end, SHA-256 fingerprint), then the count:

```console
$ java -jar mystic-crypt-ui.jar --cli keystore list -f demo.p12 --password-stdin < store-password.txt
server	private key	SHA256withRSA	O=example,CN=my-server	Thu Nov 11 06:46:59 UTC 2027	ec8736d171d4e73d5ce77039c94750b0f85a5feb4c89fca65af728217e84ce57
1 entries
```

Export a certificate, import it back under another alias, and remove it again:

```console
$ java -jar mystic-crypt-ui.jar --cli keystore export-cert -f demo.p12 -a server -o server.pem
key store password:
exported 'server' to server.pem

$ java -jar mystic-crypt-ui.jar --cli keystore import-cert -f demo.p12 -a trusted -c server.pem
key store password:
imported the certificate as 'trusted'

$ java -jar mystic-crypt-ui.jar --cli keystore delete -f demo.p12 -a trusted
key store password:
deleted 'trusted'
```

A post-quantum key pair, with the algorithm written the way the command line spells it:

```console
$ java -jar mystic-crypt-ui.jar --cli keystore add-keypair \
    -f demo.p12 -a pq -A ML-DSA-65 -d "CN=quantum safe"
key store password:
added 'pq' (ML_DSA_65)
```

`add-keypair` takes `-A`/`--algorithm` (RSA by default, also EC, DSA, RSASSA-PSS, ML-DSA-44,
ML-DSA-65 and ML-DSA-87, with dashes or underscores), `-d`/`--dname` (default `CN=mystic-crypt`),
`--key-size` (default 2048) and `--days-valid` (default 365). The command line follows these
defaults of its own and ignores what is configured for the plugin. Like the window, it refuses an
existing file for `create` (`'demo.p12' already exists; delete it first or choose another file.`)
and an alias that is already taken for `add-keypair` and `import-cert`. `list` tells a symmetric
key apart and calls it `secret key`. Errors arrive as a Java stack trace on standard error whose
first line carries the message, and the exit code is then 1.

## 6. Limits and pitfalls

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

**An empty file counts as free.** **Create** and the wizard refuse a file that holds something,
but write over one that exists with nothing in it, which is what a file chooser sometimes leaves
behind.

**The wizard's Finish button works on every step.** The check that the two password fields agree
runs when you press **Next** on the Store step. Pressing **Finish** earlier creates the store with
whatever **Store password:** holds, so walk through the steps when you type a new password.

**The wizard does not open the store it made.** The "Manage Key Store" window that appears after
**Finish** starts empty: point it at the new file and press **Open** to see what was written.

**Only three store types and three key algorithms.** The window offers PKCS12, JKS and JCEKS,
and RSA, EC and ML_DSA_65. Hardware-backed stores (PKCS11) and domain key stores (DKS) are not
offered, since they need external hardware or a configuration file. The command line accepts
more key algorithms (DSA, RSASSA-PSS, ML-DSA-44 and ML-DSA-87) than the window does.

**Changed settings need a fresh window.** The defaults are read when "Manage Key Store" opens.
An already-open window keeps what it started with, apart from the AES key length.

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
why a mismatch is caught before anything is written. The key file is read whatever shape it is
in: PKCS#8, the openssl styles, PEM or DER.

The password is held as a character array while the window is open, and the key store file is
rewritten in full after each operation.

## 8. See also

- [[install-a-plugin]] - how the plugin gets into the application in the first place
- [[Getting-Started]] - signing in, which comes before any plugin tool
- [[how-to-create-a-plugin]] - for developers, how a tool like this one is built
