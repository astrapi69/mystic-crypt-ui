# Key Generation

## What it is for

Key Generation produces a brand new key pair on the spot and shows both halves as PEM text, ready
to be saved to files, protected with a password, or tried out on a piece of text before you commit
to it. Reach for it whenever you need a key that does not exist yet: an RSA pair for the
encrypt/decrypt demo, an EC key on one particular curve because a certificate or a wallet demands
that curve, or a post-quantum pair to see what one looks like. It is not the place to open,
inspect or convert a key you already have, and it never touches your open database: whatever it
generates lives in its own window until you save it.

## Where to find it

Menu bar, **Plugins** menu, **Key Generation** submenu, item **Key Generation**.

The item opens an internal window titled **Key generation demo** inside the main window, switching
the main window to its desktop view first if it was in another view mode. The **Plugins** menu only
exists once at least one plugin is installed, see [[install-a-plugin]].

## Using it

1. Pick the algorithm in the **Algorithm** dropdown. It offers `RSA` (the one the tool starts on),
   `EC`, `X25519`, `X448`, `ML_KEM_768` and `ML_DSA_65`.
2. For RSA, pick the length in the **Keysize** box: 1024, 2048, 4096, 8192, 16384, 32768. For every
   other algorithm the box is greyed out, because their parameter sets are fixed.
3. For EC, pick the curve in the box labelled **Curve (EC):**. It offers `secp256r1`,
   `secp384r1`, `secp521r1`, `secp256k1`, `secp224r1`, `brainpoolP256r1`, `brainpoolP384r1` and
   `brainpoolP512r1`. The box is shown for every algorithm but only the EC branch reads it.
4. Choose what a saved private key file should look like in the box labelled
   **Private key file format:**, either **PKCS#8 (Java)** or **PKCS#1 (openssl, nginx)**. This
   choice only affects the file that **Save private key** writes.
5. Press **Generate keys**. While the key is being made the two boxes read
   `Generating private key...` and `Generating public key...`; when it worked they show the PEM
   text, starting with `-----BEGIN` and ending with `-----END`. When it did not work both boxes end
   up empty and no dialog appears: the reason goes to the application log only.
6. Optional, RSA only: type something into **Text to encrypt** and press **Encrypt >**. The hex
   result appears in **Encrypted text** and the left box is emptied. **< Decrypt** does the
   reverse. Each button stays disabled while its own box is empty. With a key from any other
   algorithm you get an information dialog titled **Not available for this algorithm**, saying
   "Encrypt/decrypt is available for RSA keys only. Select RSA and generate a key pair to use it."
7. **Save private key** opens a save dialog and writes the key as PEM in the format picked in
   step 4.
8. **Save private key with password** first asks in a dialog titled **Enter a password**, with the
   fields **Password** and **Repeat password** and the buttons **Set password** and **Cancel**;
   after that the save dialog appears. If the two fields differ you get the message
   "Entered passwords are not the same." and nothing is saved.
9. **Save public key** opens a save dialog and writes the public key as a binary file.
10. **Clear keys** empties both key boxes and both text boxes and forgets the key pair.

## Settings

Edit these under **File**, **Settings...**, tab **Plugin settings**, entry **Key Generation** in
the list on the left. Change the value column, press **Apply** to store it; **Reset to defaults**
throws the stored values away. New values take effect the next time the tool window is opened.

| Key | Default | What it changes | Where to change it |
|---|---|---|---|
| `default.algorithm` | `RSA` | Which algorithm the **Algorithm** dropdown starts on. Only a name the dropdown actually offers has an effect: `RSA`, `EC`, `X25519`, `X448`, `ML_KEM_768`, `ML_DSA_65`. | **Settings...**, tab **Plugin settings**, entry **Key Generation** |
| `default.key.size` | `KEYSIZE_2048` | Which entry the **Keysize** box starts on, for the size based algorithms. Written as the constant name, for instance `KEYSIZE_4096`. | **Settings...**, tab **Plugin settings**, entry **Key Generation** |

A value that is not a name of the respective kind is ignored without a message and the default
applies: `RSA` for the algorithm, `KEYSIZE_2048` for the key size.

## On the command line

This plugin contributes no commands of its own. The closest thing is the `keygen` command that
comes from the mystic-crypt library and is available through the application's `--cli` switch.

Written to files, nothing is printed and the exit code is 0:

```
java -jar mystic-crypt-ui-all.jar --cli keygen --algorithm X25519 \
  --out-private private.pem --out-public public.pem
```

Without `--out-private` and `--out-public` both keys go to standard output:

```
$ java -jar mystic-crypt-ui-all.jar --cli keygen -a RSA -s 1024
-----BEGIN RSA PRIVATE KEY-----
MIICXQIBAAKBgQCjdFL41/Llkg6trNHxv2ktrbpGbFRHoDj7qcP2x0Vs6KP3jB3v
...
-----END RSA PRIVATE KEY-----
-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCjdFL41/Llkg6trNHxv2ktrbpG
...
-----END PUBLIC KEY-----
```

`-a` accepts `RSA`, `X25519`, `X448`, `ML-KEM-768`, `ML-DSA-65` and the like, with dashes or
underscores; `-s` is the bit length and only the size based algorithms look at it. The command has
no counterpart for the panel's curve choice, its PKCS#1 output or its password protection, and the
private key it prints for an RSA pair comes out as `BEGIN RSA PRIVATE KEY`.

## Limits and pitfalls

**Nothing is kept.** The generated pair exists in that one window. Closing the window, pressing
**Clear keys** or generating again replaces it, and there is no way back. Nothing is written into
the open database either. Save what you want to keep, immediately.

**The public key file is not the text you see.** **Save public key** writes the key in binary
form, not the PEM block shown in the **Public key** box. If the tool on the other side wants PEM,
copy the text out of the box instead of using the button.

**PKCS#8 versus PKCS#1 decides who can read your private key.** PKCS#8 ("BEGIN PRIVATE KEY") is
what Java reads; PKCS#1 ("BEGIN RSA PRIVATE KEY") is what openssl, nginx and much of the rest of
the world expect. A key that a web server rejects is usually a key that was saved in the other
format. Both come back into this application unchanged. The format box governs **Save private key**
only: the PEM shown in the panel does not change when you switch it, and the password protected
file ignores it.

**Pressing Save before Generate.** The save dialog opens as usual, you pick a file, and nothing is
written; there is no error dialog. Nothing warns you about an existing file either, it is simply
overwritten.

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

**Encrypt and decrypt are a demo.** They exist for RSA keys only, they move the text between the
two boxes rather than copying it, and nothing is stored. If encryption or decryption fails, for
instance when you press **< Decrypt** on text that is not the hex this tool produced, no dialog
appears at all: the boxes stay as they are and the reason only reaches the log.

**Save certificate... does not write a certificate yet.** The button becomes available after an
RSA generation and opens a **Create certificate** dialog and then a save dialog, but no file is
produced; that part is unfinished. Use the certificate tool in the **Plugins** menu
("Create Certificate...") instead.

**The password dialog is thin.** An empty password cancels the whole action silently, with no
message and no file. Mismatched fields produce "Entered passwords are not the same." and then
nothing, so you have to start the button again. There is no minimum length and no strength check.
The resulting file is binary, not PEM: to verify it, reopen it through **File**, **Secret key**,
**Open private key**, which asks for the password.

**Settings that name something the dropdown does not offer.** `default.algorithm` is checked
against the algorithm names of the library, not against the six entries in the dropdown. A name
like `DSA` or `ML_DSA_87` is stored happily and then has no effect: the tool opens on RSA.

## Under the hood

Key pairs come from the crypt-data key pair factory over the JDK's own providers plus
BouncyCastle. RSA and the modern fixed parameter algorithms (X25519, X448, ML-KEM-768, ML-DSA-65)
go through the plain factory; EC goes through BouncyCastle explicitly with the named curve, which
is why the curve you pick is the curve you get. The encrypt/decrypt demo is not raw RSA: a fresh
AES-128 key in GCM mode with a random nonce encrypts the text, the RSA public key wraps that
session key, and both travel together in the hex string, so the length of the text does not
matter. **Save private key with password** produces a DER encoded PKCS#8 structure protected with
PBEWithSHA1AndDESede, a random 8 byte salt and 20 iterations: legacy protection that is fine for
moving a key between two of your own machines, and not a replacement for a proper keystore. All
key material stays in memory in the open window; nothing reaches the disk until you press one of
the save buttons.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[Secure-your-passwords-with-obfuscation]]
- [[how-to-create-a-plugin]]
