# Release keys in mystic-crypt-ui: OpenPGP through Bouncy Castle, the primary key in the vault

A record of the decision taken in [#642](https://github.com/astrapi69/mystic-crypt-ui/issues/642) on
2026-10-29, not a rule. It describes the decision and what was measured for it. Of what follows
from it, stage 1a exists: the reader is `io.github.astrapi69.mystic.crypt.gnupg.GnuPgHome` in the
mystic-crypt library since 13.8 (astrapi69/mystic-crypt#211), and the release key plugin
(`plugins/release-key-plugin`, #644) shows what it returns. The rest of stage 1 and stages 2 and 3
do not exist yet.

## The decision

Taken by the maintainer on 2026-10-29 (the words are quoted in #642):

- OpenPGP is done through Bouncy Castle's `bcpg`, not by driving the `gpg` command line.
- The primary key lives in the vault.
- Where `gpg` needs a key, the plugin hands an exported file to `gpg --import`. It never writes into
  gpg's store itself.
- Reading `pubring.kbx` and `private-keys-v1.d` with Bouncy Castle's readers is allowed, for display.
- Signing and verifying files is rebuilt with OpenPGP signatures (see "The predecessor").

Taken the same day after this record and stage 1a (comment in #642):

- `bcpg` directly, not PGPainless, as recommended below. Stages 2 and 3 are approved.
- **Library first.** What a user triggers is built in the mystic-crypt library, with a command of its
  own, and the plugin uses only the library. The reader of stage 1a is
  `io.github.astrapi69.mystic.crypt.gnupg` there, with `release-key status` and `--json` on its
  command line. mystic-crypt's `.claude/rules/library-first.md` carries the rule. Passphrases come
  only from standard input or a hidden prompt, never from an argument.
- **Stage 1c stays in the plugin.** The dates of the CI secrets are GitHub's, not OpenPGP's, and
  `gh secret list` is already the command line for them: names and dates, never a value. No library
  command is built for it. The read-only token gets read access to secrets for astrapi69. A second
  token follows for lightblueseas, and until it exists the view shows a note for organization
  secrets instead of an error.

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
turns out to be missing there. Decided so by the maintainer on 2026-10-29.

## What was measured for the readers

A throwaway GnuPG 2.4.8 home in the state of the real key:
- one RSA key shaped like it: primary rsa2048 [SC], subkeys rsa2048 [E] and rsa4096 [S];
- one Ed25519/Cv25519 key;
- in both, the primary key's secret removed and the subkeys imported again;
- gpg's own revocation certificates in `openpgp-revocs.d`.

It was read with `bcpg`, `bcprov` and `bcutil` 1.86 only. The probes became the reader's tests in the
library: `GnuPgHomeTest` lets gpg write such a home in a temporary directory and measures the reader's
answers against what gpg says about the same home.

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

Two findings from building the reader were reported to Bouncy Castle by the maintainer on 2026-10-29,
each with a minimal reproduction:

- the keygrip of RSA keys, which is computed without the leading zero byte of `n` that gpg hashes:
  [bcgit/bc-java#2534](https://github.com/bcgit/bc-java/issues/2534);
- `SExpression.hasLabel`, which throws a `ClassCastException` for an expression that starts with
  another expression, as gpg's protected keys contain:
  [bcgit/bc-java#2535](https://github.com/bcgit/bc-java/issues/2535).

The reader works around both, so it does not depend on how they are resolved.

## Where the code goes

Decided on 2026-10-29: library first.

- **Stage 1, read only:** the reader is in mystic-crypt and reads with `bcpg` directly; the plugin
  shows what it returns. Stage 1c, the dates of the CI secrets, stays in the plugin (see above).
- **Stage 2:** the steps that create or change key material go into the mystic-crypt library first and
  are consumed from there, as architecture.md asks of every cryptographic building block and of new
  command-line capability:
  - exporting the subkey;
  - protecting it again with the CI passphrase;
  - signing and verifying files with detached OpenPGP signatures;
  - the sealed-box encryption GitHub requires for secret values.
- `bcpg-jdk18on` comes into mystic-crypt as a dependency, in the version of the other Bouncy Castle
  modules (1.86), and reaches the plugin through the library.

## The predecessor

astrapi69/sign-and-verify (`de.alpharogroup:sign-and-verify`) signs byte arrays, serialized Java
objects and JSON with `java.security.Signature`, and hands the signature back as a Base64 string. It
has no standard container and no binding to a key someone else can check, so a signature means
nothing outside the program that made it. Signing and verifying files is rebuilt with detached
OpenPGP signatures instead: the `.asc` beside a file, the form Maven Central requires and
`gpg --verify` checks.

## What is still open

- **The dates of the CI secrets need the token's new right.** The read-only GitHub token got 403 on
  `repos/.../actions/secrets` and on `orgs/.../actions/secrets` (measured 2026-10-29, #642). Read
  access to secrets is metadata only; GitHub never returns a value.
- **Stage 1b comes next:** the offline media and the CI working folder.
- **Acceptance stays as #642 states it:** every step the maintainer has done by hand, from
  `release-key.md` and from the status file, works in the plugin without a terminal.
