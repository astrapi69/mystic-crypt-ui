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

The **Plugins** menu, submenu **Conversion**, item **Convert Key/Certificate...**. It opens a
wizard in a window titled `Convert Key or Certificate File` that blocks the main window until
you finish or cancel it.

The Plugins menu only appears once at least one plugin is installed; see
[[install-a-plugin]] if the menu is missing. The item is greyed out until you have signed in to
a database, and again while the workspace is locked.

## Using it

The wizard has three steps, **Source**, **Target** and **Review**, and the buttons **Previous**,
**Next**, **Cancel** and **Finish** along the bottom. Each step starts with a short note that says
what it is for.

1. **Source.** Put the file you want to convert into the **File:** field, by typing or pasting the
   path, or with the **...** button next to it, which opens a file chooser whose accept button
   reads **Select**.
2. Read the **It holds:** line. The wizard looks at the file as soon as there is a path and says
   what it found, for example `an RSA private key, PKCS#1`, `a private key, PKCS#8`,
   `an EC private key, SEC1`, `a certificate`, `a public key`, `a certificate request`,
   `a private key in DER form`, `a certificate in DER form`, `a public key in DER form` or
   `nothing this tool recognises`. An OpenPGP export is named as such, for instance
   `an OpenPGP public key block, which this tool does not convert`. A path that names no readable
   file is reported in red as `not read:` and the reason. You are not asked to declare the type.
   The line can be selected and copied. Press **Next**; it moves on only once the file could be
   read.
3. **Target.** The step repeats the source under **From file:** and what it holds, and offers the
   four conversions as a choice. Only the ones that make sense for the file are enabled:
   - **to DER** - strips the header and the Base64 from a PEM file. Enabled for a PEM source.
   - **to PEM** - wraps a DER file in the header that fits what it holds. Enabled for a DER
     source.
   - **to PKCS#8 (Java)** - rewrites a private key under `BEGIN PRIVATE KEY`. Enabled only
     when the file holds a private key.
   - **to PKCS#1 (openssl)** - rewrites a private key under `BEGIN RSA PRIVATE KEY` and
     its relatives. Enabled only when the file holds a private key.
4. Picking a conversion fills **Write to:** with a name next to the source: `key.der` for to DER,
   `key.pem` for to PEM, `key-pkcs8.pem` and `key-pkcs1.pem` for the two format conversions. Type
   another path or use the **...** button if you want the result elsewhere; left blank, the same
   default is used. Press **Next**; it moves on only once a conversion is chosen.
5. **Review.** A read-only summary lists `Source:`, `It holds:`, `Conversion:` and `Write to:`.
   Nothing has been written yet. Press **Finish** to write the file; the wizard closes when it
   worked.
6. When it did not work, a dialog titled `Conversion failed` says `Could not convert the file:`
   and the reason, for example `'/home/you/keys/key-pkcs8.pem' already exists - pick another
   name or remove it first`, and the wizard stays open so you can change something.
7. **Cancel** closes the wizard and writes nothing.

## Settings

This plugin has no settings. The wizard determines what a file holds from the file itself.

## On the command line

No plugin in this application contributes a command of its own; every command on the
command line comes from the mystic-crypt library and is reached through the application's
`--cli` switch. The one that matches this plugin is `convert`, which does what the wizard does:
it works out what a file holds and converts it in either direction.

Ask it what a file is, and let it convert nothing:

```
$ java -jar mystic-crypt-ui.jar --cli convert --in ~/keys/key.pem --describe
/home/you/keys/key.pem is a private key in PKCS#1, the traditional form, PEM encoded, algorithm 1.2.840.113549.1.1.1
```

That answers the same question as the **It holds:** line in the wizard, in more words: the
encoding, the wrapping and the algorithm of the key. It is the thing to reach for when a
file arrived from somewhere else and you do not know what is in it.

To convert, name the target with `--to`, which takes `pem`, `der`, `pkcs1` or `pkcs8`, and the
file to write with `-o`, `--out`:

```
$ java -jar mystic-crypt-ui.jar --cli convert --in ~/keys/key.pem --to pkcs8 --out ~/keys/java-key.pem
/home/you/keys/key.pem is a private key in PKCS#1, the traditional form, PEM encoded, algorithm 1.2.840.113549.1.1.1
wrote PEM, PKCS#8 to /home/you/keys/java-key.pem
```

Every run prints the description first, so you always see what was read before anything was
written; while converting, the description and the `wrote` line go to standard error, so
standard output carries only a printed result. The four targets are the four conversions of the
wizard: `--to pkcs8` is **to PKCS#8 (Java)**, `--to pkcs1` is **to PKCS#1 (openssl)** and writes
the algorithm specific `BEGIN RSA PRIVATE KEY` header for an RSA key, `--to der` and `--to pem`
are **to DER** and **to PEM**. Certificates and public keys go through it as well, between PEM and
DER:

```
$ java -jar mystic-crypt-ui.jar --cli convert --in ~/certs/example.crt --to der --out ~/certs/example.der
/home/you/certs/example.crt is an X.509 certificate, PEM encoded, algorithm 1.2.840.113549.1.1.1
wrote DER to /home/you/certs/example.der
```

Things worth knowing before you rely on it:

- Without `--out` (or with `--out -`) the result is printed, which only works for PEM. Asking for
  DER on the terminal is refused with `error: DER is binary and cannot be printed; pass --out to
  write it to a file`.
- With neither `--to` nor `--describe` the command describes the file and stops, saying
  `nothing was converted: pass --to to say what to convert it to, or --describe to ask only
  what it is`. Nothing is written by accident.
- `--to der` on a private key always writes the PKCS#8 form, whatever the source was: the
  confirmation reads `wrote DER, PKCS#8`.
- `--to pem` on a private key keeps the wrapping the file already had, so a PKCS#1 file
  stays PKCS#1. When it is the wrapping you want changed, say `--to pkcs8` or `--to pkcs1`.
- `--to pkcs1` and `--to pkcs8` are for private keys only. On a public key they fail with
  `PKCS#1 and PKCS#8 describe how a private key is wrapped; a public key has neither. Use --to
  pem or --to der.`, on a certificate with the same sentence about a certificate.
- `--to pkcs1` on a key that has no traditional form, such as an X25519 or an Ed25519 key, is
  refused: `PKCS#1 was asked for, but a 'X25519' private key has no traditional form - PKCS#8 is
  the only encoding it has. Ask for pkcs8, or leave the format option out.`
- A file that is neither a key nor a certificate is reported, not converted:
  `error: 'plain.txt' is not a DER private key, public key or X.509 certificate this tool
  converts`. That is the command line form of *nothing this tool recognises*.
- Exit code 0 means done, 2 means an error.

Unlike the wizard, the command does not refuse to write over an existing file, so pick the
`--out` name with the same care you would give any other shell redirection.

The old `der2pem` command still exists and still turns a DER private key into PEM, but it is
deprecated in favour of `convert`, which detects what the file is and converts in both
directions. Its own help says so. Use `convert` for new work.

## Limits and pitfalls

- **The destination must not exist.** The wizard refuses to write over a file that is already
  there and says so, leaving the existing content untouched. This is deliberate: overwriting a
  key file is not something anybody recovers from. The default name it proposes can be taken
  already, for instance by an earlier run, so read the **Write to:** field before Finish.
- **A file that is already PEM is not converted to PEM again.** **to PEM** stays disabled for a
  PEM source, and **to DER** for a DER one, since there is no header and no Base64 to strip.
- **"nothing this tool recognises" means no conversion is offered.** Point it at a text
  file, an archive or anything else that is not a key, a certificate or a certificate
  request, and the *It holds:* line says exactly that while every conversion stays disabled.
  That is the expected result, not a failure to report. A PEM file with a header the tool
  does not know is described as `something this tool does not recognise`.
- **An OpenPGP file is named, not converted.** For a GnuPG export the wizard still offers
  **to PEM**, and Finish then refuses with the reason: the file holds an OpenPGP block, which
  this tool does not convert.
- **Nothing here asks for a password.** There is no passphrase field in the wizard, so key
  material that is protected by one is out of scope; decrypt it with the tool that protected it
  first.
- **A certificate is not a private key.** The two format conversions stay disabled for
  certificates, public keys and certificate requests. PKCS#8 and PKCS#1 are shapes a
  private key can have, and nothing else.
- **PKCS#1 exists for RSA, RSASSA-PSS, DSA and EC keys only.** For an Ed25519, X25519, X448,
  ML-KEM or ML-DSA key **to PKCS#1 (openssl)** is still offered, and Finish refuses with
  "PKCS#1 was asked for, but a '...' private key has no traditional form - PKCS#8 is the only
  encoding it has." rather than writing PKCS#8 under the PKCS#1 name.
- **Finish works on every step.** Pressed on Source or Target before the steps are complete, it
  says what is missing in the `Conversion failed` dialog, `choose a file to convert` or
  `choose a conversion`, and writes nothing.
- **to DER and to PEM move bytes, the two format conversions re-encode the key.** If a program
  rejects a key even though the header looks right, the fix is usually **to PKCS#8** or
  **to PKCS#1**, not a trip through DER and back.

## Under the hood

Reading and writing the key material is done entirely by the mystic-crypt and crypt-data
libraries together with BouncyCastle, which also supplies the PEM writer. Nothing is
parsed or re-encoded by hand. The type of a PEM file is taken from its header; a DER file
carries no header, so the wizard decides what it holds by trying to read it as a private
key, then as a certificate, then as a public key, and reports the first that succeeds. The
private keys it can read cover RSA, EC, DSA, EdDSA, Ed25519, X25519, X448, ML-KEM-768 and
ML-DSA-65, with Diffie-Hellman and others through the library's general reader. Keys are held in
memory only for the duration of a conversion, and no password is asked for or stored anywhere.
Nothing is written to the file you chose as source, and nothing is written outside the
destination you named.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
