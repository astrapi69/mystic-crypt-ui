# Sign and Verify

## What it is for

This tool answers one question: does this text or this file really come from the holder of a
particular key, and is it still byte for byte what that holder signed? You sign data with a
private key and hand out the signature; anyone with the matching public key, or with a
certificate carrying it, can check it. Reach for it instead of the encryption tools when you
do not need secrecy but proof of origin: encryption hides the content, a signature leaves it
readable and only proves who stands behind it. It is also the place to try the NIST
post-quantum signature families side by side with the classical ones, because the result line
tells you how long a key pair or a signature took and how large the signature came out.

## Where to find it

**Plugins** menu, submenu **Signatures**, item **Sign and Verify**.

The "Plugins" menu appears once at least one plugin is installed and enabled, see
[[install-a-plugin]]. The tool opens as a window inside the main window, titled "Sign and
Verify"; if the application is currently not showing the desktop view, it switches to it first.
The item is greyed out until you have signed in to a database, and again while the workspace is
locked.

## Using it

The panel works with two kinds of keys: a throwaway pair generated right here, which is meant
for trying an algorithm out, and a key that already exists on disk, which is the case that
matters in practice.

### Trying an algorithm out with a generated key pair

1. Pick an entry in **Algorithm:**. The list starts with the ones this tool can generate keys
   for (Ed25519, the three ML-DSA parameter sets, the twelve SLH-DSA parameter sets) and ends
   with the classical ones (`SHA256withRSA`, `SHA512withRSA`, `SHA256withECDSA`,
   `SHA512withECDSA`, `SHA256withDSA`).
2. Press **Generate key pair**. The result line at the bottom reads for example
   `generated a ML-DSA-65 key pair in 8 ms`, and **Public key:** shows the new public key in
   PEM form. With one of the classical algorithms selected the button is disabled, and the
   result line says why before you press anything: `SHA256withRSA: a key for this algorithm has
   to come from a file - this tool generates only the Ed25519, ML-DSA and SLH-DSA families`.
   Choosing a generatable algorithm again enables the button and clears the line.
3. Decide what gets signed: either type into **Message:** (it starts with the text from the
   settings), or fill **File to sign:** with the **...** button and tick
   **sign the file instead of the text**. The checkbox decides; an unticked box means the file
   path is ignored and the text is signed.
4. Press **Sign**. **Signature (Base64):** fills up and the result line reads for example
   `signed 43 bytes with ML-DSA-65 in 8 ms, signature is 3309 bytes`.
5. Press **Verify**. The result line reads `signature is valid`.

### Signing with a key that already exists

1. Put the private key into **Private key file:** with the **...** button next to it. PEM and
   DER files are both read, including the openssl styles and a file that carries both halves of
   a pair.
2. Put the verifying side into **Public key or certificate:**. A public key file works, and so
   does an X.509 certificate, which is how a public key usually arrives.
3. Choose the message or the file to sign, as above.
4. Press **Sign**. The algorithm now comes from the key itself, not from the combo box, so an
   RSA key produces `signed 37 bytes with SHA256withRSA in 3 ms, signature is 256 bytes`.
   **Public key:** shows the public counterpart computed from the private key, or the note
   `RSA private key loaded from file` when it cannot be computed.
5. Press **Verify**: `signature is valid` when the signature belongs to this data and this key,
   `signature is not valid` when it does not.
6. To keep the signature, put a path into **Signature file:** and press **Save signature**:
   `saved the signature to contract.sig`. **Load signature** reads it back into the field:
   `loaded the signature from contract.sig`, and Verify then works against the loaded one.

Every failure appears on the same result line, prefixed with what did not happen and followed
by the reason: `not signed: choose a private key file, or generate a key pair first`,
`not saved: there is no signature to save`, `not saved: choose a signature file`,
`not signed: choose a file to sign`, `not loaded: ...`. An empty signature field answers Verify
with `there is no signature to check`. Every field and button explains itself in a tooltip.

## Settings

Edited under **File** > **Settings...**, tab **Plugin settings**, entry **Signatures** in the
list on the left. Change the cell in the value column and press **Apply**;
**Reset to defaults** throws the stored values away again. A changed value reaches the tool the
next time the Sign and Verify window is opened, not while it is open.

| Key | Default | What it changes |
| --- | --- | --- |
| `default.algorithm` | `Ed25519` | Which algorithm the **Algorithm:** box starts on. Only a name the tool can generate a key for is accepted here: `Ed25519`, `ML-DSA-44`, `ML-DSA-65`, `ML-DSA-87` or one of the twelve `SLH-DSA-...` sets. Anything else, including the classical names the combo box offers, is ignored and Ed25519 is used. |
| `default.message` | `The quick brown fox jumps over the lazy dog` | What the **Message:** field starts with, so a recurring test message does not have to be typed again. |

## On the command line

This tool contributes no commands of its own, and neither does any other plugin in this
application. The closest thing is the pair of signature commands that the mystic-crypt library
brings, which the application exposes through `--cli`: `sign` and `verify-signature`. Between
them they cover every family the panel knows, the classical RSA, ECDSA and DSA next to Ed25519,
ML-DSA and SLH-DSA, and both read their key from a PEM or a DER file. `-a` takes `RSA`,
`EC` (or `ECDSA`), `DSA`, `Ed25519`, `ML-DSA-44`, `ML-DSA-65`, `ML-DSA-87`, an SLH-DSA parameter
set such as `SLH-DSA-SHA2-128S`, or a JCA name such as `SHA512withRSA`, with dashes or
underscores accepted.

```
# a post-quantum key pair on disk
java -jar mystic-crypt-ui.jar --cli keygen -a ML-DSA-65 \
    --out-private mldsa-private.pem --out-public mldsa-public.pem

# sign a file with the private key
java -jar mystic-crypt-ui.jar --cli sign -a ML-DSA-65 \
    --key mldsa-private.pem --in doc.txt --signature doc.sig
wrote signature to doc.sig

# check it with the public key
java -jar mystic-crypt-ui.jar --cli verify-signature -a ML-DSA-65 \
    --key mldsa-public.pem --in doc.txt --signature doc.sig
signature is valid

# the same with a classical key, here an RSA key pair in DER form
java -jar mystic-crypt-ui.jar --cli sign -a RSA \
    --key rsa-private.der --in contract.txt --signature contract.sig
wrote signature to contract.sig
java -jar mystic-crypt-ui.jar --cli verify-signature -a RSA \
    --key rsa-public.der --in contract.txt --signature contract.sig
signature is valid
```

The description of `verify-signature`, in `--cli --help` and in its own help alike, names only
Ed25519, ML-DSA and SLH-DSA; the `-a` option in `--cli verify-signature --help` lists what the
command really accepts. A key pair in DER form, as the RSA example uses, comes out of
`--cli convert --to der`, see [[Plugin-Key-Conversion]].

The exit code carries the verdict, so a script can branch on it: `0` valid, `1` invalid
(`signature is invalid` on standard output), `2` an error before the check could happen, such
as an unreadable key or an unknown algorithm. `--in -` reads the data from standard input;
`--signature` always needs a real file path.

The signature file the panel saves holds the raw signature bytes, which is the same shape the
`sign` command writes and the `verify-signature` command expects, so a signature can be made on
one side and checked on the other as long as the key and the algorithm match.

## Limits and pitfalls

- **The Algorithm box stops deciding as soon as a key file is named.** With a private key file
  in the field, the algorithm comes from the key: an RSA key signs with `SHA256withRSA` even
  when `ML-DSA-65` or `SHA512withRSA` is selected. The combo box only decides for a key pair
  generated here.
- **Verify answers `signature is not valid` for every kind of failure**, not only for a bad
  signature. A public key file that does not exist, cannot be read, or holds something other
  than a key produces the same sentence as a forged signature. When a verdict surprises you,
  check the key path first.
- **Most post-quantum public keys in PEM form cannot be read back in this panel.** Signing with
  an ML-DSA or SLH-DSA private key from a file works, PEM or DER. On the verifying side, an
  ML-DSA-65 public key is read in both forms, but a PEM public key of ML-DSA-44, ML-DSA-87 or any
  SLH-DSA set is not, and Verify then answers `signature is not valid` even though the signature
  is perfectly good. The same key in DER form is read: convert it with **to DER** in
  [[Plugin-Key-Conversion]], keep the generated pair in the open window and verify there, or
  verify with `--cli verify-signature`, which reads those keys in either form.
- **A generated key pair cannot be saved from this panel and does not survive the window.**
  Close the Sign and Verify window and the private key is gone, which makes every signature
  made with it unverifiable for good. Only generate keys here for experiments; for keys you
  intend to keep, create the pair as files first and sign from the file.
- **The Signature (Base64) field cannot be typed or pasted into.** It is filled by Sign or by
  Load signature. To check somebody else's signature you need it as a file and must load it.
- **Nothing checks that the two key fields belong together.** A public key from an unrelated
  pair verifies to `signature is not valid`, with no hint that the mismatch is the reason.
- **The data is read again at every Verify.** That is what makes tampering visible, and it also
  means a file that an editor rewrote (a changed line ending, an added trailing newline) no
  longer verifies against the older signature, although its text looks unchanged.
- **A signature is not attached to what it signs.** The signed file is left exactly as it was,
  so whoever should check the signature needs three things from you: the data, the signature
  file, and the public key or certificate.
- **The SLH-DSA sets are slow and their signatures are large.** On one test machine
  `SLH-DSA-SHA2-128S` needed about 850 ms for a single signature, and a `SLH-DSA-SHAKE-256F`
  signature is 49856 bytes, against 64 bytes for Ed25519. The work happens while the
  window waits, so the panel does not react during it. The result line reports both numbers,
  which is exactly what makes the comparison worth doing.
- **A setting the tool cannot honour is dropped in silence.** Putting `RSA` into
  `default.algorithm` gives no error, the tool simply starts on Ed25519.

## Under the hood

The signing and verifying is done by the mystic-crypt library family on top of the JDK and
BouncyCastle: Ed25519 and ML-DSA come from the JDK, SLH-DSA, which the JDK lacks, from
BouncyCastle; nothing cryptographic is implemented in the tool itself. Three families are
covered: the classical Edwards-curve Ed25519, and the NIST post-quantum standards ML-DSA (FIPS
204, three parameter sets) and SLH-DSA (FIPS 205, twelve parameter sets). The classical RSA,
ECDSA and DSA algorithms are offered for keys that come from a key store, a certificate
authority or another tool, and they deliberately run through BouncyCastle rather than through
the provider the platform would pick, because a key generated inside this application can
otherwise fail to load with an error nobody can act on. Two properties are worth keeping in
mind: a signature protects origin and integrity but not confidentiality, so the signed text
stays readable to everyone; and a key pair generated in the panel lives only in the memory of
that open window, which is why it is meant for experiments and not for anything you have to
verify tomorrow.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
