# Conversion

## What it is for

Key and certificate files come in shapes that do not read the same everywhere: DER (raw
bytes, no header) or PEM (Base64 between `BEGIN`/`END` lines), and for private keys either
PKCS#8, which Java reads, or PKCS#1 and its relatives, which openssl, nginx and much of
the rest of the world expect. This plugin turns a file from one of those shapes into
another without changing the key inside it. Reach for it when a key you already have is
rejected by the tool that has to use it, or when you need to look at a DER file that no
text editor will show you. It creates nothing: for a new key pair use the key generator,
for a certificate use the certificate tool.

## Where to find it

The **Plugins** menu, submenu **Conversion**. It holds two items:

- **Convert key file...** - the newer tool, which works out what your file holds and
  offers only the conversions that fit it. Its window is titled `Convert key file`.
- **Convert DER to PEM** - the older, narrower tool, where you say yourself what the file
  holds. Its window is titled `Convert *.der-file to *.pem-file`.

The Plugins menu only appears once at least one plugin is installed; see
[[install-a-plugin]] if the menu is missing. Opening either item switches the main window
to the desktop view and opens the tool as an internal window there.

## Using it

### Convert key file...

1. Put the file you want to convert into the **File:** field, by typing or pasting the
   path, or with the **...** button next to it, which opens a file chooser whose accept
   button reads **Select**.
2. Read the **It holds:** line. The tool looks at the file as soon as there is a path and
   says what it found, for example `an RSA private key, PKCS#1`, `a private key, PKCS#8`,
   `an EC private key, SEC1`, `a certificate`, `a public key`, `a private key in DER form`
   or `nothing this tool recognises`. You are not asked to declare the type, and there is
   nothing to confirm.
3. Put the destination into the **Write to:** field, again by hand or with its **...**
   button. Give a file name that does not exist yet.
4. Press the conversion you want. Only the ones that make sense for the file you chose are
   enabled:
   - **to DER** - strips the header and the Base64 from a PEM file. Enabled for a PEM
     source.
   - **to PEM** - wraps a DER file in the header that fits what it holds. Enabled for a
     DER source that was recognised.
   - **to PKCS#8 (Java)** - rewrites a private key under `BEGIN PRIVATE KEY`. Enabled only
     when the file holds a private key.
   - **to PKCS#1 (openssl)** - rewrites a private key under `BEGIN RSA PRIVATE KEY` and
     its relatives. Enabled only when the file holds a private key.
5. The bold line at the bottom of the window reports the outcome. On success it names the
   shape and the file, for example `written as PKCS#8 to java-key.pem`. On failure it
   starts with `not converted:` and gives the reason, for example
   `not converted: '/home/you/keys/java-key.pem' already exists - pick another name or
   remove it first`. A file that cannot be read at all is reported on the same line as
   `not read: ...`.

### Convert DER to PEM

1. In **Choose type to convert**, pick what the file holds. The list offers every key type
   the application knows, but only `PRIVATE_KEY`, `CERTIFICATE` and `PUBLIC_KEY` are
   actually converted.
2. Press **Choose** next to *Choose private key in \*.der format to convert* and pick the
   DER file. The **Output** area confirms with `Set der file 'key.der' to convert.`
3. Press **Save** next to *Save to \*.pem private key* and name the PEM file to write. The
   Output area confirms with `Set pem file 'key.pem' to insert output.` Nothing is written
   yet at this point.
4. Press **Convert**. The Output area is the only feedback: it prints `Coversion
   started...` (the spelling is the application's), then a line per step such as
   `read private key...` and `private key written to file...`, and finally
   `Coversion finished...`
5. When something goes wrong, the Output area shows the error stack trace instead of the
   `written to file` line, and no usable PEM file is produced. When the selected type is
   one of the types this tool does not handle, it prints `unknown key type...` and writes
   nothing.

## Settings

One key, edited under **File > Settings...**, tab **Plugin settings**, entry **Der to Pem**
in the list on the left. Change the value in the table on the right, then
press **Apply**; **Reset to defaults** puts it back.

| Key | Default | What it changes |
|---|---|---|
| `default.key.type` | `PRIVATE_KEY` | Which entry **Convert DER to PEM** preselects in its *Choose type to convert* list when it opens. One of the key type constants, such as `PRIVATE_KEY`, `PUBLIC_KEY` or `CERTIFICATE`. A value that names no known type is ignored and `PRIVATE_KEY` applies. |

The setting has no effect on **Convert key file...**, which determines the type from the
file itself.

## On the command line

No plugin in this application contributes a command of its own; every command on the
command line comes from the mystic-crypt library and is reached through the application's
`--cli` switch. The one that matches this plugin is `convert`, which does what *Convert key
file...* does: it works out what a file holds and converts it in either direction.

Ask it what a file is, and let it convert nothing:

```
$ java -jar mystic-crypt-ui-all.jar --cli convert --in ~/keys/key.pem --describe
/home/you/keys/key.pem is a private key in PKCS#1, the traditional form, PEM encoded, algorithm 1.2.840.113549.1.1.1
```

That answers the same question as the **It holds:** line in the window, in more words: the
encoding, the wrapping and the algorithm of the key. It is the thing to reach for when a
file arrived from somewhere else and you do not know what is in it.

To convert, name the target with `--to`, which takes `pem`, `der`, `pkcs1` or `pkcs8`:

```
$ java -jar mystic-crypt-ui-all.jar --cli convert --in ~/keys/key.pem --to pkcs8 --out ~/keys/java-key.pem
/home/you/keys/key.pem is a private key in PKCS#1, the traditional form, PEM encoded, algorithm 1.2.840.113549.1.1.1
wrote PEM, PKCS#8 to /home/you/keys/java-key.pem
```

Every run prints the description first, so you always see what was read before anything was
written. The four targets are the four buttons of *Convert key file...*: `--to pkcs8` is
**to PKCS#8 (Java)**, `--to pkcs1` is **to PKCS#1 (openssl)** and writes the algorithm
specific `BEGIN RSA PRIVATE KEY` header for an RSA key, `--to der` and `--to pem` are **to
DER** and **to PEM**. Certificates and public keys go through it as well, between PEM and
DER:

```
$ java -jar mystic-crypt-ui-all.jar --cli convert --in ~/certs/example.crt --to der --out ~/certs/example.der
/home/you/certs/example.crt is an X.509 certificate, PEM encoded, algorithm 1.2.840.113549.1.1.1
wrote DER to /home/you/certs/example.der
```

Things worth knowing before you rely on it:

- Without `--out` the result is printed, which only works for PEM. Asking for DER on the
  terminal is refused with `error: DER is binary and cannot be printed; pass --out to write
  it to a file`.
- With neither `--to` nor `--describe` the command describes the file and stops, saying
  `nothing was converted: pass --to to say what to convert it to, or --describe to ask only
  what it is`. Nothing is written by accident.
- `--to der` on a private key always writes the PKCS#8 form, whatever the source was: the
  confirmation reads `wrote DER, PKCS#8`.
- `--to pem` on a private key keeps the wrapping the file already had, so a PKCS#1 file
  stays PKCS#1. When it is the wrapping you want changed, say `--to pkcs8` or `--to pkcs1`.
- `--to pkcs1` and `--to pkcs8` are for private keys only. On a public key or a certificate
  they fail with `PKCS#1 and PKCS#8 describe how a private key is wrapped; a public key has
  neither. Use --to pem or --to der.`
- A file that is neither a key nor a certificate is reported, not converted:
  `error: 'plain.txt' is not a DER private key, public key or X.509 certificate this tool
  converts`. That is the command line form of *nothing this tool recognises*.
- Exit code 0 means done, 2 means an error.

Unlike *Convert key file...*, the command does not refuse to write over an existing file, so
pick the `--out` name with the same care you would give any other shell redirection.

The old `der2pem` command still exists and still turns a DER private key into PEM, but it is
deprecated in favour of `convert`, which detects what the file is and converts in both
directions. Its own help says so. Use `convert` for new work.

## Limits and pitfalls

- **The destination must not exist.** *Convert key file...* refuses to write over a file
  that is already there and says so, leaving the existing content untouched. This is
  deliberate: overwriting a key file is not something anybody recovers from. *Convert DER
  to PEM* has no such check, so pick a name there that does not exist yet.
- **A file that is already PEM is not converted to PEM again.** The **to PEM** button
  stays disabled for a PEM source, and forcing the conversion reports that the file is
  `already pem`. The same the other way round: a file that is not PEM cannot be converted
  to DER, since there is no header and no Base64 to strip.
- **"nothing this tool recognises" means no conversion is offered.** Point it at a text
  file, an archive or anything else that is not a key, a certificate or a certificate
  request, and the *It holds:* line says exactly that while every button stays disabled.
  That is the expected result, not a failure to report. A PEM file with a header the tool
  does not know is described as `something this tool does not recognise`.
- **Nothing here asks for a password.** There is no passphrase field in either window, so
  key material that is protected by one is out of scope; decrypt it with the tool that
  protected it first.
- **A certificate is not a private key.** The two format buttons stay disabled for
  certificates, public keys and certificate requests. PKCS#8 and PKCS#1 are shapes a
  private key can have, and nothing else.
- **A path that names no file** is reported on the result line rather than silently doing
  nothing. Clearing the *File:* field clears the detection and disables every button
  again.
- **to DER and to PEM move bytes, the two format buttons re-encode the key.** If a program
  rejects a key even though the header looks right, the fix is usually **to PKCS#8** or
  **to PKCS#1**, not a trip through DER and back.
- **The older tool trusts you.** It never looks inside the file. Choosing `CERTIFICATE`
  for a file that holds a private key, or pointing it at a file that is not a DER key at
  all, produces a stack trace in the Output area and no output file. If you are unsure
  what your file holds, open *Convert key file...* first and read its *It holds:* line.
- **The Output area scrolls, the result line does not.** In the older tool the outcome of
  the last conversion is somewhere in the log; in the newer one it is always the single
  line at the bottom.
- **In Settings the plugin is listed as "Der to Pem"**, not as "Conversion". That is the
  entry to select when you are looking for its setting.

## Under the hood

Reading and writing the key material is done entirely by the mystic-crypt and crypt-data
libraries together with BouncyCastle, which also supplies the PEM writer. Nothing is
parsed or re-encoded by hand. The type of a PEM file is taken from its header; a DER file
carries no header, so the tool decides what it holds by trying to read it as a private
key, then as a certificate, then as a public key, and reports the first that succeeds. The
private keys it can read cover RSA, EC, DSA, EdDSA and Ed25519. Keys are held in memory
only for the duration of a conversion, and no password is asked for or stored anywhere.
Nothing is written to the file you chose as source, and nothing is written outside the
destination you named.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
