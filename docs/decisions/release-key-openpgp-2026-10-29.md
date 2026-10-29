# Release keys in mystic-crypt-ui: OpenPGP through Bouncy Castle, the primary key in the vault

A record of the decision taken in [#642](https://github.com/astrapi69/mystic-crypt-ui/issues/642) on
2026-10-29, not a rule. It describes the decision and what was measured for it. Of the plugin that
follows from it, the status view of a GnuPG home exists (`plugins/release-key-plugin`, #644); the rest
of stage 1 and stages 2 and 3 do not.

## The decision

Taken by the maintainer on 2026-10-29 (his words are quoted in #642):

- OpenPGP is done through Bouncy Castle's `bcpg`, not by driving the `gpg` command line.
- The primary key lives in the vault.
- Where `gpg` needs a key, the plugin hands an exported file to `gpg --import`. It never writes into
  gpg's store itself.
- Reading `pubring.kbx` and `private-keys-v1.d` with Bouncy Castle's readers is allowed, for display.
- Signing and verifying files is rebuilt with OpenPGP signatures (see "The predecessor").

## What was weighed

The release key today is the one of lethenon `docs/launch/release-key.md`. Its primary key is
offline, a stub in the daily keyring, and so are the copies on the media. Its signing subkey sits in
the daily keyring and, re-protected with the CI passphrase, in the CI secrets. Shell scripts drive
`gpg`, `secret-tool` and `gh` around it: `release-key-measure.sh` and `ci-signing-secrets.sh` with its
rehearsal, next to the maintainer's notes.

**Driving the gpg command line**
- Works with the keyrings as they are.
- Everything else is the cost:
  - the state lives in a store this application does not own;
  - passphrases travel through pipes and a pinentry;
  - the result is read back from text output, which changes between versions and locales.
- The traps of the last days were all of this kind:
  - libsecret under a snap reads a store of its own (#595);
  - a gpg agent stands in for a passphrase it cached, so a check can pass for the wrong reason;
  - Gradle signs with the primary key unless it is given the subkey's 8-digit id
    (astrapi69/lethenon#154, #155);
  - an organization secret carried a key from before the subkey;
  - `gh` lacked the `admin:org` scope;
  - `xtrace` and core dumps would have exposed secrets.
- Testable only against a real `gpg` with throwaway homes, which the rehearsal does at the price of a
  minute per run.

**OpenPGP in Java with Bouncy Castle, the primary key in the vault**
- The key material and the passphrases stay inside a process this application owns, behind the vault
  that already holds the other secrets. The vault is also a backup, which the GNOME keyring is not.
- Every step is a library call with a typed result, testable in a unit test with keys generated in the
  test.
- Same behaviour on every platform the JVM runs on.
- Compatible with `gpg` by the open format, in both directions: Bouncy Castle reads what `gpg` writes
  (measured below), and `gpg --import` reads what the plugin exports.

The decision takes the second. The first stays where it is needed: `gpg --import` for a key that has
to be in gpg's keyring, run by the user or handed a file by the plugin.

## PGPainless or bcpg directly

Measured 2026-10-29 from Maven Central and the projects' repositories:

| | PGPainless 2.0.4 | bcpg-jdk18on 1.86 |
|---|---|---|
| released | 2026-07-09 | the version line the application already ships (bcprov, bcpkix, bcutil 1.86) |
| licence | Apache-2.0 | the Bouncy Castle Licence and Apache-2.0, as its POM names them |
| depends on | bcprov, bcpg, bcutil 1.84, kotlin-stdlib 1.9.21, slf4j-api 1.7.36 | bcprov, bcutil |
| high-level API | its own, built on Bouncy Castle's new high-level API since 2.0 | `org.bouncycastle.openpgp.api`, 144 classes, among them `OpenPGPKey`, `OpenPGPCertificate`, `OpenPGPKeyGenerator`, `OpenPGPKeyEditor`, `OpenPGPDetachedSignatureGenerator` and `OpenPGPDetachedSignatureProcessor` |
| gpg keyrings | through bcpg | `org.bouncycastle.gpg.keybox` (pubring.kbx), `SExprParser` and `PGPSecretKeyParser` (private-keys-v1.d) |

PGPainless adds a policy layer with stricter defaults and an implementation of the Stateless OpenPGP
interface. It costs a Kotlin runtime and a second maintainer chain. Built against Bouncy Castle 1.84,
it would run here against 1.86, a version it was not built with, through the API that changed most
between those versions. The functions the stages need all exist in `bcpg` itself: reading keys,
keyrings and gpg's files, generating and editing keys, changing a protection, and detached signatures.
The Argon2 flaw in Bouncy Castle's OpenPGP (CVE-2026-59648) is fixed from 1.85 on.

**Recommendation:** `bcpg` directly, through its high-level API. PGPainless only if a needed function
turns out to be missing there. The decision is the maintainer's.

## What was measured for the readers

A throwaway GnuPG 2.4.8 home in the state of the real key:
- one RSA key shaped like it: primary rsa2048 [SC], subkeys rsa2048 [E] and rsa4096 [S];
- one Ed25519/Cv25519 key;
- in both, the primary key's secret removed and the subkeys imported again;
- gpg's own revocation certificates in `openpgp-revocs.d`.

It was read with `bcpg`, `bcprov` and `bcutil` 1.86 only. The probes became the plugin's tests (#644):
`GnuPgHomeSupportTest` lets gpg write such a home in a temporary directory and measures every answer
of the reader against what `gpg --with-colons` says about the same home.

- `BcKeyBox` reads `pubring.kbx`: both keys, every subkey with its expiry.
- The revocation certificates in `openpgp-revocs.d` parse as signatures of type 0x20 (key revocation)
  once gpg's leading `:` is removed from the armour line.
- **Bouncy Castle's `KeyGripCalculator` is not usable here.** For EdDSA and ECDH keys it refuses
  ("keygrip calculation not yet supported"). For RSA it returns a different keygrip than `gpg`: for
  the throwaway primary key `B74E5981...` against gpg's `3A745321...`. A probe built on it therefore
  reported the two subkeys whose secret is present as offline. A reader that answers wrong is worse
  than none.
- **What works:** every file in `private-keys-v1.d` carries the key's public part in the clear (RSA
  `n`, EC `q`). `PGPSecretKeyParser.parse(...).getKeyExpression()` reads it without decrypting
  anything. Matched against the public keys of `pubring.kbx`, it gives the same answer as `gpg` for all
  six keys: the primary keys offline, the subkeys present, each found under the file name `gpg` shows
  as its keygrip.

Two more findings from building the reader on it (#644):

- `SExpression.hasLabel` casts the expression's first value without looking, and throws a
  `ClassCastException` for an expression that starts with another expression, which gpg's protected
  keys contain. The reader checks the value's type first.
- gpg writes the explanation above the armor of a revocation certificate in the user's language. On
  the maintainer's keyring, in German, the umlauts made a reader that expected ASCII call both
  certificates unreadable. It reads the file byte for byte now, and a test pins the German first line.

Run against the maintainer's keyring on 2026-10-29, the reader gives gpg's answer for every key of the
release key: use, creation, expiry and where the secret part is. Measured by calling
`GnuPgHomeSupport.read` on `~/.gnupg` from a probe outside the repository and comparing it line by line
with `gpg --with-colons --with-subkey-fingerprint --list-secret-keys <fingerprint>`; the same probe
showed the two unreadable revocation certificates, and after the fix both valid.

The keygrip difference is worth a report to Bouncy Castle. Posting there is the maintainer's call.

## Where the code goes

This is a proposal, open for the maintainer:

- **Stage 1, read only:** the plugin's support layer reads with `bcpg` directly, for display. It
  writes nothing, so no building block is shared beyond the plugin. Built this way for the GnuPG home
  (#644); `bcpg` comes with the plugin's zip, which carries it under `lib/` with its licence, since
  the host does not ship it.
- **Stage 2:** the steps that create or change key material go into the mystic-crypt library first and
  are consumed from there, as architecture.md asks of every cryptographic building block and of new
  command-line capability:
  - exporting the subkey;
  - protecting it again with the CI passphrase;
  - signing and verifying files with detached OpenPGP signatures;
  - the sealed-box encryption GitHub requires for secret values.
- `bcpg-jdk18on` comes in as a dependency, in the version of the other Bouncy Castle modules (1.86).

## The predecessor

astrapi69/sign-and-verify (`de.alpharogroup:sign-and-verify`) signs byte arrays, serialized Java
objects and JSON with `java.security.Signature`, and hands the signature back as a Base64 string. It
has no standard container and no binding to a key someone else can check, so a signature means
nothing outside the program that made it. Signing and verifying files is rebuilt with detached
OpenPGP signatures instead: the `.asc` beside a file, the form Maven Central requires and
`gpg --verify` checks.

## What is still open

- **The rest of stage 1:** the offline media and the CI working folder, and the CI secrets' dates.
- **Stage 1 needs a source for the CI secrets' dates.** The read-only GitHub token gets 403 on
  `repos/.../actions/secrets` and on `orgs/.../actions/secrets` (measured 2026-10-29, #642). It needs
  read access to secrets (metadata only; GitHub never returns a value), or the dates come from
  somewhere else.
- **Stages 2 and 3 wait** for the decision on the open points above.
- **Acceptance stays as #642 states it:** every step the maintainer has done by hand, from
  `release-key.md` and from the status file, works in the plugin without a terminal.
