# Create Certificate

## What it is for

This tool produces a self-signed X.509 certificate: you fill in a name, it generates a
fresh key pair behind the scenes, signs a certificate for that key with itself and writes
it as a PEM file with the extension `.crt`. Reach for it when something asks you for a
certificate file to look at, to import as a test trust anchor or to feed to a tool while
you are trying it out. It is not the place to make keys you want to keep: the key pair it
generates is used for the signature and then thrown away, so use the key generator for key
material and the key store tools for storing it.

## Where to find it

The **Plugins** menu, submenu **Certificate**, item **Create Certificate...**. It opens a
window titled `Create Certificate` that blocks the main window until you finish or cancel
it.

The Plugins menu is part of the main window, so sign in to a database first, and the menu
itself only appears once at least one plugin is installed; see [[install-a-plugin]] if it
is missing.

## Using it

1. Choose **Plugins > Certificate > Create Certificate...**. A key pair is generated while
   the window opens, so there can be a short pause before you see anything.
2. The first step is **Issuer**, with six fields: **Common Name:**, **Organisation:**,
   **Organisation Unit:**, **Country Code:**, **State:** and **Location:**. Common Name
   comes prefilled with the name from the settings (`mystic-crypt` until you change it);
   the other five start empty. Fill in what you want to appear in the certificate and
   press **Next**.
3. The second step is **Subject**, with exactly the same six fields, prefilled the same
   way. The certificate is self-signed, so issuer and subject describe the same thing;
   nothing forces the two forms to agree, and whatever you type into each one is what ends
   up in that half of the certificate. Press **Next**.
4. The third step is **Certificate Data**, with **Version:**, **Serial Number:** and a
   **Generate** button next to it, **Not Before:** and **Not After:** as two calendars,
   and **Signature Algorithm:** as a list. Everything here is already filled in: the
   serial from the current clock, Not Before today, Not After today plus the configured
   number of years, the signature algorithm from the settings. You can type your own
   serial number or press **Generate** for a random one. The rest of this step is display
   only, which is worth reading about in *Limits and pitfalls* below. Press **Next**.
5. The last step is **Extensions**. Pick a kind in the **Extension:** list, which offers
   `Basic Constraints`, `Key Usage`, `Subject Alternative Name` and `Other (object id)`.
   Picking one fills **Extension ID:** with its object id and shows an example of the
   value underneath the **Value:** field, for instance
   `CA:true or CA:true,pathlen:1 or CA:false`. Type the value, tick **Critical:** if it
   should be critical and press **Add**; the extension appears in the table below.
   **Edit** and **Delete** work on the row you select in that table. A value that cannot be turned into a real extension is
   refused with a dialog titled `This extension cannot be built`, naming the extension and
   what was expected, and nothing is added.
6. Press **Finish**. The Finish button is enabled on every step, so you can also press it
   on the first one and accept everything that was prefilled.
7. A file chooser titled `Save the certificate as` opens in your configuration directory
   (`~/.config/mystic-crypt-ui`), filtered to `Certificate (*.crt)`. Type a name and save.
   If the name has no `.crt` at the end, it is added for you.
8. When it worked, a dialog titled `Certificate created` says `Certificate saved to` and
   the file name, and the wizard closes.
9. When it did not work, a dialog titled `Certificate failed` appears. It says
   `Could not create the certificate:` followed by the reason, and the wizard stays open
   so you can go back and change something. If the key pair could not be generated at all,
   the wizard never opens: you get `Certificate wizard failed` with
   `Could not initialize the certificate wizard:` and the reason instead.
10. **Cancel** closes the wizard and writes nothing.

## Settings

Four keys, edited under **File > Settings...**, tab **Plugin settings**, entry
**Certificate** in the list on the left. Change a value in the table on the right, then
press **Apply**; **Reset to defaults** puts all four back. The values are read when the
wizard opens, so change them before you open it, not while it is on screen.

| Key | Default | What it changes |
|---|---|---|
| `default.common.name` | `mystic-crypt` | The Common Name that the Issuer and Subject steps start with. Leading and trailing blanks are removed. A value that is empty or only blanks is ignored and `mystic-crypt` applies. |
| `default.key.algorithm` | `RSA` | Which kind of key pair is generated when the wizard opens. Described as RSA, EC, DSA or Ed25519, but only RSA, DSA and EC actually work here, see the pitfalls. |
| `default.signature.algorithm` | `SHA256withRSA` | How the certificate is signed, for instance `SHA256withRSA` or `SHA512withRSA`. If the value does not fit the configured key, it is dropped and an algorithm that the key can produce is used instead. |
| `default.validity.years` | `1` | How many years the certificate stays valid, counted from the moment the wizard opens. `0`, a negative number and anything that is not a number all fall back to `1`, because a shorter span would produce a certificate that is expired on arrival. |

## On the command line

This plugin contributes no command of its own. The closest thing is the `cert` command
that ships in the mystic-crypt library and is therefore reachable through the
application's command line switch:

```
java -jar mystic-crypt-ui-all.jar --cli cert --subject "CN=example.com,O=Example,C=DE" --days 730 --out ~/certs/example.crt
```

It prints one line and exits with code 0:

```
wrote self-signed certificate for 'CN=example.com,O=Example,C=DE' to /home/you/certs/example.crt
```

The options are `--subject` (the whole distinguished name in one string, default
`CN=mystic-crypt`), `-a`/`--algorithm` (default `RSA`), `-s`/`--size` (default `2048`, for
size based algorithms), `--signature-algorithm` (default `SHA256withRSA`), `--days`
(default `365`) and `--out`, which is required.

Two differences from the window are worth knowing. The command counts validity in days,
so it is the only way to get a certificate that is valid for less than a year. And it does
not correct a mismatched pair of algorithms for you: `--algorithm EC` on its own fails
with a stack trace whose first line reads
`cannot create signer: Supplied key ... is not a RSAPrivateKey instance`, and writes no
file, so name the matching signature algorithm yourself:

```
java -jar mystic-crypt-ui-all.jar --cli cert --algorithm EC --signature-algorithm SHA256withECDSA --subject "CN=ec test" --out ~/certs/ec.crt
```

The command has no way to add extensions, and like the window it writes the certificate
only, never the private key. Plugin start and stop messages, and sometimes a line about
plugins that could not be stopped cleanly, are written alongside on the error stream; the
result line above is the only thing on standard output, so redirecting it to a file gives
you just that line.

## Limits and pitfalls

- **The private key is never saved.** The key pair is generated when the wizard opens,
  used to sign, and gone when the window closes. What you get is the certificate and
  nothing else, so it cannot serve TLS, sign anything or be imported as a key. It is a
  public document: the certificate half of a key pair whose other half no longer exists.
  If that is not what you wanted, generate the key pair with the key generator first.
- **The extensions you add do not reach the certificate.** The Extensions step validates
  what you type properly and puts it in its table, but nothing carries that table into the
  certificate that is written: the saved file has no key usage, no subject alternative
  name and no basic constraints, whatever the table shows. There is no warning. If you
  need a certificate with extensions, this tool cannot produce one today.
- **On the Certificate Data step, only the serial number is yours.** The two calendars and
  the signature algorithm list are filled in from the settings and are never read back, so
  changing them changes nothing in the file. The validity window always comes from
  `default.validity.years`, and the signature always from `default.signature.algorithm`
  (or, when that does not fit the key, from the algorithm the key can actually produce).
  To get a different validity or a different signature, change the settings and reopen the
  wizard.
- **The Version list is not a choice either.** It offers 1 and 3, and the certificate is
  always written as an X.509 version 3 certificate.
- **An empty name is accepted.** Clear the Common Name field and leave the other five
  empty, and the certificate is still written, with an issuer and a subject that carry
  nothing at all. No dialog objects; the result is a file that every consumer will find
  useless. The same holds for the Subject step on its own.
- **Clearing the Serial Number field does not mean "no serial".** An empty field is read
  as the number 0, and that is what the certificate carries.
- **Ed25519 does not work as a key algorithm**, even though the setting's own description
  mentions it. Keys are always requested at 2048 bits for anything that is not EC, and an
  Ed25519 key pair generator refuses that size: the wizard then does not open at all, and
  the `Certificate wizard failed` dialog reports `Unsupported size: 2048`. RSA and DSA
  work at 2048 bits, EC works because it is generated through BouncyCastle without a size.
  Any name that is not a known algorithm ends the same way, with the reason in that same
  dialog and no wizard.
- **You cannot force a signature algorithm that the key cannot produce.** Setting `EC` as
  the key algorithm while leaving `SHA256withRSA` as the signature simply drops the
  signature setting; the certificate is then signed the EC way. This is deliberate: the
  mismatch would otherwise fail deep inside the certificate builder. It also means the
  Signature Algorithm shown in the wizard may not be the one that ends up in the file.
- **There is no validity shorter than one year here.** Anything below one year in the
  settings falls back to one year. Use the command line's `--days` if you need a short
  lived certificate.
- **Pressing Finish and then cancelling the save dialog throws the work away.** The wizard
  closes anyway, and everything you typed is gone; it does not return you to the steps.
- **Nothing asks before an existing file is replaced.** Pick a name that does not exist
  yet, especially inside the configuration directory the chooser opens in.
- **The certificate is self-signed and trusted by nobody.** No browser, no operating
  system and no Java key store will accept it until you import it as a trust anchor
  yourself, and the issuer name you typed does not make it one.

## Under the hood

The key pair comes from the crypt-data key pair factory, at a fixed size of 2048 bits for
size based algorithms; EC is the exception, generated through BouncyCastle's own generator
with its default parameters. The certificate is assembled and signed by the same library
over BouncyCastle and written in PEM form by its certificate writer, so nothing about the
encoding is done by hand. Before signing, the signature algorithm is checked against the
key and an impossible combination is refused with a message that names one that would
work. Extension values typed in words (`CA:true,pathlen:1`, `digitalSignature,keyCertSign`,
`DNS:example.com,IP:10.0.0.1`) are turned into proper DER by BouncyCastle rather than
being stored as the letters you typed, which is why a value in the wrong shape is refused
instead of quietly producing a malformed certificate; for an object id the tool does not
know, the value has to be given as hex. The private key lives only in the memory of the
open wizard, is never written to disk and is not offered for export.

## See also

- [[Settings-And-Plugins]]
- [[install-a-plugin]]
- [[Plugin-Key-Conversion]]
- [[Getting-Started]]
