# Key Generation

## What it is for

Key Generation produces a brand new key pair on the spot and shows both halves as PEM text, ready
to be saved to files, protected with a password, wrapped in a self-signed certificate, or tried out
on a piece of text before you commit to it. Reach for it whenever you need a key that does not
exist yet: an RSA pair for the encrypt/decrypt demo, an EC key on one particular curve because a
certificate or a wallet demands that curve, or a post-quantum pair to see what one looks like. It
is not the place to open, inspect or convert a key you already have, and it never touches your
open database: whatever it generates lives in its own window until you save it.

## Where to find it

Menu bar, **Plugins** menu, **Key Generation** submenu, item **Key Generation**.

The item opens an internal window titled **Key generation demo** inside the main window, switching
the main window to its desktop view first if it was in another view mode. The **Plugins** menu only
exists once at least one plugin is installed, see [[install-a-plugin]]. The item is greyed out until
you have signed in to a database, and again while the workspace is locked.

## Using it

The window has four choices at the top, the key generation part in the middle and the
encrypt/decrypt demo at the bottom.

1. Pick the algorithm in the **Algorithm** dropdown. It offers `RSA`, `EC`, `X25519`, `X448`,
   `ML_KEM_768` and `ML_DSA_65`, and starts on the one from the settings (`RSA` unless you changed
   it).
2. For RSA, pick the length in the **Keysize** box: 1024, 2048, 4096, 8192, 16384, 32768. It
   starts on the configured size, 2048 by default. Switching to any other algorithm greys the box
   out, because their parameter sets are fixed.
3. For EC, pick the curve in the box labelled **Curve (EC):**. It offers `secp256r1`,
   `secp384r1`, `secp521r1`, `secp256k1`, `secp224r1`, `brainpoolP256r1`, `brainpoolP384r1` and
   `brainpoolP512r1`, and starts on `secp256r1`. The box is shown for every algorithm but only the
   EC branch reads it.
4. Choose the structure of a saved private key in the box labelled **Private key file format:**,
   either **PKCS#8 (Java)** or **PKCS#1 (openssl, nginx)**. PKCS#1 is only offered for RSA and EC:
   for the other four algorithms the box is greyed out on PKCS#8, and its tooltip names the reason,
   for example "a X25519 private key has no traditional form - PKCS#8 is the only encoding it has".
5. Choose the encoding of every file this window saves in the box labelled **Save as:**, either
   `PEM` (text, the default) or `DER` (binary).
6. Press **Generate keys**. While the key is being made the two boxes read
   `Generating private key...` and `Generating public key...`; when it worked they show the PEM
   text, starting with `-----BEGIN` and ending with `-----END`. When it did not work both boxes end
   up empty and no dialog appears: the reason goes to the application log only.
7. Optional, RSA and EC: type something into **Text to encrypt** and press **Encrypt >**. The hex
   result appears in **Encrypted text** and the left box is emptied. **< Decrypt** does the
   reverse. Each button stays disabled while its own box is empty. After generating a key of any
   of the other four algorithms both buttons are disabled, and the line between them says why:
   "The hex encrypt/decrypt demo needs an RSA or an EC key pair. Choose one of them and press
   Generate." Before the first key is generated, pressing **Encrypt >** opens an information
   dialog titled **Not available for this algorithm**, saying "Encrypt/decrypt is available for RSA
   and EC keys. Select one of them and generate a key pair to use it."
8. **Save private key** opens a save dialog and writes the key in the structure picked in step 4
   and the encoding picked in step 5. A name without an ending gets `.pem` or `.der` added.
9. **Save private key with password** first asks in a dialog titled **Enter a password**, with the
   fields **Password** and **Repeat password** and the buttons **Set password** and **Cancel**;
   after that the save dialog appears. If the two fields differ you get the message
   "Entered passwords are not the same." and nothing is saved. The file is always binary, and a
   name without an ending gets `.der`.
10. **Save public key** opens a save dialog and writes the public key in the encoding picked in
    step 5, PEM or DER, with the matching ending added when the name has none.
11. **Save certificate...**, RSA and EC only, makes a self-signed certificate for the generated
    pair. It opens a **Create certificate** dialog that is already filled in: issuer and subject
    `CN=mystic-crypt`, serial number 1, valid for one year from now, and the signature algorithm
    that fits the key (`SHA256withRSA` or `SHA256withECDSA`). Change what you want, press **OK**,
    pick a file, and the certificate is written as PEM, with `.pem` added to a name without an
    ending. For the other algorithms the button is disabled and the line under it says "Only RSA
    and EC keys can be used for a self-signed certificate. Choose one of them and press Generate."
12. **Clear keys** empties both key boxes and both text boxes, forgets the key pair and puts the
    algorithm back to the configured one.

## Settings

Edit these under **File**, **Settings...**, tab **Plugin settings**, entry **Key Generation** in
the list on the left. Change the value column, press **Apply** to store it; **Reset to defaults**
throws the stored values away. New values take effect the next time the tool window is opened.

| Key | Default | What it changes | Where to change it |
|---|---|---|---|
| `default.algorithm` | `RSA` | Which algorithm the **Algorithm** dropdown starts on. Use one of the six names the dropdown offers: `RSA`, `EC`, `X25519`, `X448`, `ML_KEM_768`, `ML_DSA_65`. | **Settings...**, tab **Plugin settings**, entry **Key Generation** |
| `default.key.size` | `KEYSIZE_2048` | Which entry the **Keysize** box starts on, for RSA. Written as the constant name, for instance `KEYSIZE_4096`. | **Settings...**, tab **Plugin settings**, entry **Key Generation** |

A value that is not a name of the respective kind is ignored without a message and the default
applies: `RSA` for the algorithm, `KEYSIZE_2048` for the key size.

## On the command line

This plugin contributes no commands of its own. The closest thing is the `keygen` command that
comes from the mystic-crypt library and is available through the application's `--cli` switch.

Written to files, nothing is printed and the exit code is 0:

```
java -jar mystic-crypt-ui.jar --cli keygen --algorithm X25519 \
  --out-private private.pem --out-public public.pem
```

Without `--out-private` and `--out-public` both keys go to standard output, the private key first:

```
$ java -jar mystic-crypt-ui.jar --cli keygen -a RSA -s 2048
-----BEGIN PRIVATE KEY-----
...
-----END PRIVATE KEY-----
-----BEGIN PUBLIC KEY-----
...
-----END PUBLIC KEY-----
```

The options:

- `-a`, `--algorithm` accepts `RSA` (the default), `EC`, `X25519`, `X448`, `ML-KEM-768`,
  `ML-DSA-65` and the other algorithm names of the library, with dashes or underscores.
- `-s`, `--size` is the bit length, 2048 by default; only RSA, DSA and DH look at it.
- `-c`, `--curve` names the curve and makes the key an EC key on it, for instance
  `-c secp384r1`. It accepts any named curve Bouncy Castle knows, not only the eight of the window.
- `--format pkcs8` (the default) or `--format pkcs1` is the structure of the private key. Asking for
  PKCS#1 on a key that has none is refused with exit code 2: "PKCS#1 was asked for, but a 'XDH'
  private key has no traditional form - PKCS#8 is the only encoding it has."
- `--print-details` adds one line that says what was generated, for instance
  `algorithm: EC, curve: secp384r1, field size: 384 bits, private key format: PKCS#8, PEM label: PRIVATE KEY`.

The exit code is 0 when the keys were generated and 2 on an error. The command writes PEM only:
it has no counterpart for the window's DER encoding, its password protection or its certificate.

## Limits and pitfalls

**Nothing is kept.** The generated pair exists in that one window. Closing the window, pressing
**Clear keys** or generating again replaces it, and there is no way back. Nothing is written into
the open database either. Save what you want to keep, immediately.

**PKCS#8 versus PKCS#1 decides who can read your private key.** PKCS#8 ("BEGIN PRIVATE KEY") is
what Java reads; PKCS#1 ("BEGIN RSA PRIVATE KEY", "BEGIN EC PRIVATE KEY") is what openssl, nginx
and much of the rest of the world expect. A key that a web server rejects is usually a key that was
saved in the other format. Both come back into this application unchanged. The format box governs
**Save private key** only: the PEM shown in the panel does not change when you switch it, and the
password protected file ignores it.

**Pressing Save private key before Generate keys empties the file you pick.** The save dialog opens
as usual, the file you name is created empty, or emptied if it already held something, and there
is no error dialog. Nothing warns you about an existing file in any of the save buttons either, it
is simply overwritten. Generate first, and pick a name that does not exist yet.

**The Keysize list has a trap entry.** Besides the real lengths it contains an entry shown as
`-1`, a placeholder for "unknown". Generating with it produces no key at all: the boxes are left
showing `Generating private key...` and nothing further happens. At the other end, 8192 bits and
above take a very long time and the window does not react at all while an RSA key of that size is
being made; this is not a hang, but it can last minutes.

**Clear keys does not put back your configured key size.** It resets the algorithm to the one from
the settings, but sets the key size to 1024. Generate straight after a Clear and you get a
1024 bit RSA key, which is too weak for real use. Check the box before pressing Generate.

**The curve box is only read for EC.** Picking a curve while RSA is selected changes nothing, and
selecting EC without touching the curve box uses whatever it currently shows. The eight offered
curves are all verified to exist and to be exactly the curve the generated key sits on, which is
the point of the box: without a named curve the provider picks one, and its pick is neither
documented nor the same everywhere.

**Encrypt and decrypt are a demo.** They exist for RSA and EC keys only, they move the text between
the two boxes rather than copying it, and nothing is stored. If encryption or decryption fails, for
instance when you press **< Decrypt** on text that is not the hex this tool produced, no dialog
appears at all: the boxes stay as they are and the reason only reaches the log.

**The password dialog is thin.** An empty password cancels the whole action silently, with no
message and no file. Mismatched fields produce "Entered passwords are not the same." and then
nothing, so you have to start the button again. There is no minimum length and no strength check.
The resulting file is binary, not PEM: to verify it, reopen it through **File**, **Secret key**,
**Open private key**, which asks for the password.

**A setting outside the six names is not refused.** `default.algorithm` is checked against every
algorithm name the library knows, not against the six entries of the dropdown. A name such as `DSA`
or `ML_DSA_87` is stored without a message and then shows up as the dropdown's starting entry,
outside the list it offers. Keep the setting to one of the six.

## Under the hood

Key pairs come from the crypt-data key pair factory over the JDK's own providers plus
BouncyCastle. RSA and the modern fixed parameter algorithms (X25519, X448, ML-KEM-768, ML-DSA-65)
go through the plain factory; EC goes through BouncyCastle explicitly with the named curve, which
is why the curve you pick is the curve you get. The encrypt/decrypt demo comes from the mystic-crypt
library and is not raw RSA: for an RSA pair an AES-128 key made at Generate, in GCM mode with a
fresh random nonce for every encryption, encrypts the text, the RSA public key wraps that session
key with OAEP, and both travel together in the hex string, so the length of the text does not
matter. An EC pair has no encryption primitive
of its own and goes through ECIES (`ECIESwithSHA256`, BouncyCastle) instead. **Save private key
with password** produces a DER encoded PKCS#8 structure protected with PBEWithSHA1AndDESede, a
random 8 byte salt and 20 iterations: legacy protection that is fine for moving a key between two
of your own machines, and not a replacement for a proper keystore. All key material stays in memory
in the open window; nothing reaches the disk until you press one of the save buttons.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[Secure-your-passwords-with-obfuscation]]
- [[how-to-create-a-plugin]]
