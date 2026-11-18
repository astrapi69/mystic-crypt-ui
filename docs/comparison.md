# How mystic-crypt-ui differs from other password and vault tools

- **As of:** 2026-11-17.
- **Refers to:** mystic-crypt-ui 8.15 (`RELEASE-8.15` on `600ea252`, 2026-11-18). The text was
  checked on `develop` at `7dd653a5`; up to the release only `CHANGELOG.md`, the changelog
  fragments it took in and the version changed. Where 8.14 differs, the text says so.
- **Other tools:** KeePassXC, KeePass, Bitwarden and 1Password, as their official sites described
  them on 2026-11-17.

Every statement about mystic-crypt-ui names its evidence: a class or file with its line, a test, an
issue, a pull request or a release. A statement without evidence is not in this document. Every
statement about another tool names a source from that tool's official site; where there was none,
the cell says "not evidenced", which is not a claim that the tool lacks the feature. How the
sources were read, and what that limits, is at the end ([Method and limits](#method-and-limits)).

## Contents

- [What mystic-crypt-ui has](#what-mystic-crypt-ui-has)
- [lethenon](#lethenon)
- [How it compares](#how-it-compares)
- [Where mystic-crypt-ui is weaker](#where-mystic-crypt-ui-is-weaker)
- [Method and limits](#method-and-limits)

## What mystic-crypt-ui has

### A desktop application with plugins

- **Plugin framework:** [pf4j](https://pf4j.org) 3.16.1 (`gradle/libs.versions.toml:65`). The
  application creates its plugin manager on `<config>/plugins`, which is
  `~/.config/mystic-crypt-ui/plugins` (`MysticCryptApplicationFrame.java:289-293`,
  `docs/plugin-development.md`). A plugin is a zip in that directory; it is unpacked again whenever
  its SHA-256 changes (`PluginArchives`, tested by `PluginArchivesTest`, #998). Settings has
  "Install from Zip..." and enables or disables a plugin (`PluginsSettingsPanel`).
- **Extension points:** `PluginMenuContribution`, `PluginSettingsContribution`,
  `PluginCommandContribution`, `VaultAccessContribution` (`plugin/api/`).
- **No sandbox:** a plugin runs in the application's process with its rights (`SECURITY.md`,
  "Plugins run with the application's own rights").
- **The plugins in this repository** (`plugins/`), each described by its
  `src/main/resources/plugin.properties`. The installer's preselected plugins pack installs all
  but menu-designer (`src/main/izpack/install.xml:111-149`, `Makefile:219`).

| Plugin | What it does | Evidence |
|---|---|---|
| certificate | create an X.509 certificate | `plugins/certificate-plugin` |
| checksum | checksums of files and text | `plugins/checksum-plugin` |
| console | an output console | `plugins/console-plugin` |
| conversion | convert keys and certificates between PEM, DER, PKCS#1 and PKCS#8 | `plugins/conversion-plugin` |
| file-crypt | encrypt and decrypt files and text with a passphrase, in the `MCFILE1` format on the same envelope as the vault | `docs/vault-format.md:323-333` |
| kem-demo | ML-KEM-512/768/1024 and a hybrid X25519 + ML-KEM encapsulation between two parties | `NativeKemExchange`, `KemRoundTripTest` |
| keygen | generate key pairs | `plugins/keygen-plugin`, `KeygenEveryAlgorithmUiTest` |
| keystore | inspect and manage Java key stores | `plugins/keystore-plugin` |
| lethenon | the lethenon wallet and chain tools ([below](#lethenon)) | `plugins/lethenon-plugin` |
| menu-designer | view and edit the application menu as XML; development tooling, not in the installer | `Makefile:219` |
| obfuscation | character obfuscation of text with an editable map | `plugins/obfuscation-plugin` |
| password-hash | Argon2id, PBKDF2, bcrypt and scrypt hashes | `PasswordHashSupport.java:76-79` |
| pqc-signature | sign and verify with Ed25519, ML-DSA and SLH-DSA | `SignatureSupport.java:37-42`, `SignatureWithRealKeysTest` |
| release-key | the state of an OpenPGP release key in a GnuPG home, read-only | `docs/decisions/release-key-openpgp-2026-10-29.md` |
| secret-sharing | split a secret into shares and combine them again (Shamir) | `SecretSharingSupport.java:35` |

### Its own vault format

- **Password only:** the format `MCRDB2` (`PasswordVaultFormat.java:80`), laid out in
  `docs/vault-format.md`. AES-GCM with a 128-bit tag and a 32-byte Blake2b key-commitment tag; the
  key comes from PBKDF2WithHmacSHA256, 256 bits, 600,000 iterations, with a fresh 16-byte salt on
  every save, and the header is authenticated as associated data (`docs/vault-format.md:40-140`).
  Tested by `PasswordVaultFormatTest` and `SaveAndReopenDatabaseUiTest`.
- **Older files:** vaults written up to 8.1.1 (PBEWithMD5AndDES) still open and are rewritten as
  `MCRDB2` on the next save (`docs/vault-format.md:218-276`, `LegacyDatabaseMigrationUiTest`). A
  vault this build writes opens in release 8.5.1, checked with that release's own jar in a child
  JVM (`VaultOpensInTheLastReleaseTest`, `formatCompatibilityRelease=8.5.1` in `gradle.properties`,
  #402).
- **Key file:** an RSA private key. With a key file alone, PEM or DER (PKCS#1 and PKCS#8); with a
  password and a key file, PEM (`KeyFileOnDisk.java:67-112`, `WhatOpeningAndSavingTakeAsAKeyFileTest`,
  #937, #943). The key-file variants and their weaker parts are described under
  [Where mystic-crypt-ui is weaker](#where-mystic-crypt-ui-is-weaker).
- **While it runs:** the workspace locks after a time without use, and a copied value clears from
  the clipboard (`MysticCryptApplicationFrame.java:410-432`, `IdleLockDecisionTest`,
  `ClipboardClearsItselfAfterACopyUiTest`).

### KeePass import and export

- **Library:** KeePassJava2 3.1.0 (`gradle/libs.versions.toml:52`); import reads a `.kdbx` with
  `KdbxDatabase.read` (`ImportKeePassDatabaseAction.java:123`), export writes one
  (`ExportKeePassDatabaseAction.java:122`).
- **Unlock factors:** password, key file, or both, in both directions
  (`ImportKeePassDatabaseAction.java:135-155`, `ExportKeePassDatabaseAction.java:146-166`). Tested
  with a password (`KeePassImportUiTest`, `KeePassExportUiTest`) and with a password and a key file
  (`KeePassImportKeyFileUiTest`, `KdbxCredsRoundTripTest`).
- **Versions:** a KDBX 3.1 fixture is imported (`KeePassImportUiTest`); a KDBX 4.0 file written by
  KeePassXC 2.7.10 goes through import and export with every field compared
  (`KdbxRoundTripKeepsEveryFieldUiTest`, 11 tests, run against `keepassxc-cli` in CI); export
  writes KDBX 4.0 (`docs/decisions/kdbx-bridge-2026-09-17.md`).

### A command line

`--cli` as the first argument runs the command line of the mystic-crypt library instead of the
window (`MysticCryptUiCli.java:65`, `StartMysticCryptApplication.java:46-48`). With mystic-crypt
13.11 it has `hash`, `verify`, `keygen`, `kem`, `checksum`, `der2pem`, `obfuscate`, `disentangle`,
`cert`, `keystore`, `sign`, `verify-signature`, `encrypt`, `decrypt`, `share`, `keyx`, `convert`,
`release-key` and `help`; `MysticCryptUiCliTest` and `CommandLineStandardErrorTest` cover it. The
installer's optional command-line pack adds `mystic-crypt` and `mystic-crypt.bat` launchers.

### Licence and build

- **Licence:** MIT (`LICENSE`). The third-party notices ship with the application
  (`src/main/resources/legal/NOTICE.txt`, `ThirdPartyNoticeTest`; every library's licence under
  `META-INF/third-party/`, #779).
- **Reproducible archives:** the application and plugin jars carry no build machine and no build
  time (`gradle/packaging.gradle:16-30`, guarded by `BuiltArchivesNameNoBuildMachineTest`), and
  their entries are written in a fixed order without file times
  (`gradle/reproducible-archives.gradle`). Two builds in different locales, time zones and
  directories gave 17 byte-identical artifacts, measured once by hand for PR #991. The installer is
  not reproducible ([below](#where-mystic-crypt-ui-is-weaker)).

## lethenon

[lethenon](https://github.com/astrapi69/lethenon) is a cryptocurrency of the same author, with its
own chain, consensus and network, built as a library on Maven Central. Its README calls it "a joke
currency" and "a protest against the politics of surveillance". This section states where it
stands; it says nothing about value.

- **Version:** the latest release is 0.9.0 (`RELEASE-0.9.0`, 2026-11-12); `develop` is
  0.10.0-SNAPSHOT. mystic-crypt-ui's plugin uses 0.9.0 (`gradle/libs.versions.toml:60`).
- **Test network only:** there are two chains, the main chain `lethenon-2` and the test chain
  `lethenon-test-2` (lethenon `Chain.java`). The main chain does not exist yet: its genesis block is
  to be fixed in the code on its start day and released as 1.0.0, and until then a main chain
  cannot be started (lethenon README, "There are two chains"; ADR 0005). A node runs only on the
  test chain (lethenon ADR 0003, "Toward a main chain"). The test network can run over Tor (ADR
  0004).
- **Not tradeable:** "the coin is not sold and will not be - nobody can buy it, there is no
  liquidity pool and no listing" (lethenon README, "Status").
- **Privacy, as the project states it:** the recipient of a payment to a published address is
  hidden behind one-time destinations; the sender and the amount of every transfer are public on
  the chain (lethenon README, first paragraph).

What the wallet in mystic-crypt-ui does (`LethenonMenuContribution.java:70-94`, user guide
`docs/wiki/Plugin-Lethenon-Wallet.md`):

| Capability | Tests |
|---|---|
| create a wallet, kept in the vault | `WalletVaultTest`, `LethenonVaultTest`, `LethenonWalletPanelBindingTest` |
| restore it from its 24 words | `WalletVaultTest`, `TypedWordsTest`, `WordGridTest` |
| take over a `.lethwf` wallet file | `WalletVaultTest`, `LethenonVaultTest` |
| back it up: a three-step assistant, a reminder after 180 days, a wallet file export with its SHA-256, removing the stored words | `BackupAssistantTest`, `LethenonVaultBackupTest`, `RemoveWordsTest` |
| send, and receive through one-time destinations behind a published address, taken over or swept onto the account | `TransferSupportTest`, `LethenonSendFromVaultTest`, `TakeOverIncomingTest`, `SweepSupportTest` |
| an overview with the balance and the state of the chain, fetching the chain | `OverviewBalanceTest`, `OverviewChainStateTest`, `FetchTheChainTest` |
| synchronise with a node, mine, run a node | `SyncSupportTest`, `MiningSupportTest`, `NodeSupportTest` |
| verify a chain, show it and a balance | `ChainReplaySupportTest`, `WalletBalanceSupportTest` |

All of it works on the test network: starting a main chain is refused (`ChainKind.java:75-78`,
`LethenonMinePanelBindingTest`), and so are synchronising and running a node on one
(`SyncSupportTest`, `NodeSupportTest`). A wallet records which network it is meant for, and a
transfer from a main-chain wallet asks for confirmation first (`WalletNetwork`,
`LethenonSendFromVaultTest`).

## How it compares

Each cell for another tool names its source in the list under the table; every source was accessed on 2026-11-17. "not evidenced" means the sources gave no statement for that cell; it is not a claim that the tool lacks the feature. How the sources were read, and what that limits, is in [Method and limits](#method-and-limits).

| | mystic-crypt-ui | KeePassXC | KeePass | Bitwarden | 1Password |
|---|---|---|---|---|---|
| Licence | MIT | GPL v3 [X1] | GPL v2 or later [P1] | AGPL v3 or GPL v3 for most of the code, the Bitwarden License v1.0 for some parts; the sources differ in detail [B1][B11] | main apps: not evidenced; developer tools under proprietary terms, some SDKs and libraries open source [O1] |
| Desktop platforms | one installer jar for every platform, Java 25 required (`install.xml:17`); a desktop entry on Linux only (`install.xml:79-85`) | Windows, macOS, Linux [X2] | Windows natively; Linux, macOS, BSD through Mono [P2] | Windows, macOS, Linux [B2] | Windows 10+, macOS 12+, Linux (glibc 2.31+) [O2] |
| Official mobile apps | none | none; the FAQ points to third-party apps [X3] | none; a list of contributed, unofficial ports [P3] | iOS, Android [B2] | iOS, Android [O3] |
| Where the data lives | local file | local file [X3] | local file; can also be opened from and saved to a URL (FTP, HTTP, WebDAV) [P4] | Bitwarden cloud (US or EU region) or self-hosted [B3] | 1Password servers in the US, Canada or the EU; self-hosting not evidenced [O4] |
| Synchronisation between devices | none | left to a sync service on the folder; since 2.8.0 a remote sync through external tools (rsync, ssh, scp) [X3][X4] | merge with a file or URL, on demand or on save [P5] | through the server [B3] | through the account [O5] |
| Encryption | AES-GCM, 256-bit key, with a key commitment (`MCRDB2`); with a key file, AES-GCM with a 128-bit key wrapped with RSA-OAEP | AES-256, Twofish-256 or ChaCha20 (KDBX 3.1 and 4) [X5] | AES or ChaCha20 (KDBX 4 and 4.1); others by plugin [P6] | AES-256-CBC with HMAC-SHA256 [B4] | AES-GCM-256 [O6] |
| Key derivation | PBKDF2-HMAC-SHA256, 600,000 iterations; with password and key file, an SHA-1 based PBE at 65,536 iterations | AES-KDF; Argon2d or Argon2id with KDBX 4 [X6] | AES-KDF or Argon2 (Argon2id since 2.47) [P7] | PBKDF2-SHA256 (600,000 iterations by default) or Argon2id [B5] | PBKDF2-HMAC-SHA256, 650,000 iterations, combined with a 128-bit Secret Key [O7] |
| Key file as a factor | yes, an RSA private key (PEM; DER with a key file alone) | yes [X7] | yes [P8] | not evidenced | not evidenced |
| Hardware key | none | YubiKey or OnlyKey, HMAC-SHA1 challenge-response [X7] | through plugins (challenge-response, OTP) [P8] | FIDO2 security key for two-step login [B6] | FIDO U2F security key as a second factor for sign-in [O8] |
| KeePass `.kdbx` | import and export, with password, key file or both; KDBX 3.1 and 4.0 read, 4.0 written | native format [X5] | native format [P6] | import, with password and optional key file [B7] | no direct `.kdbx` import evidenced; import of a KeePass XML or KeePassXC CSV export [O9] |
| Browser integration | none | KeePassXC-Browser extension [X8] | Auto-Type built in; browser integration through plugins [P9] | browser extensions [B8] | browser extension [O10] |
| Command line | `--cli`, 19 commands of mystic-crypt 13.11 | `keepassxc-cli` [X9] | KPScript [P10] | `bw` [B9] | `op` [O11] |
| Plugins for third-party code | pf4j plugins as zips, 15 in this repository; no sandbox | not supported, per the FAQ [X3] | C# plugin API [P11] | not evidenced | CLI shell plugins (beta) [O12] |
| Published external audits | none | ANSSI CSPN certificate for 2.7.9 (2025); review of 2.7.4 (2023) [X10] | EU-FOSSA code audit and bug bounty; BSI-sponsored analysis of 2.57 [P12] | Cure53 assessments (2025 and earlier), ETH Zurich cryptography review, SOC 2, ISO 27001 [B10] | penetration tests and code review (2020), annual pentest reports, SOC 2 Type 2, ISO 27001:2022, bug bounty [O13] |

### Sources

All accessed 2026-11-17, through web search excerpts of the official sites (see [Method and limits](#method-and-limits)).

- [X1] KeePassXC home page, https://keepassxc.org/
- [X2] KeePassXC download page (lists 2.8.0), https://keepassxc.org/download/
- [X3] KeePassXC documentation and FAQ, https://keepassxc.org/docs/
- [X4] KeePassXC 2.8.0 release notes (2026-10-21), https://keepassxc.org/blog/2026-10-21-2.8.0-released/
- [X5] KeePassXC Getting Started Guide, https://keepassxc.org/docs/KeePassXC_GettingStarted
- [X6] KeePassXC User Guide, https://keepassxc.org/docs/KeePassXC_UserGuide
- [X7] KeePassXC FAQ and User Guide on key files and hardware keys, https://keepassxc.org/docs/ and https://keepassxc.org/docs/KeePassXC_UserGuide
- [X8] KeePassXC Getting Started Guide on the browser extension, https://keepassxc.org/docs/KeePassXC_GettingStarted
- [X9] KeePassXC User Guide on `keepassxc-cli`, https://keepassxc.org/docs/KeePassXC_UserGuide
- [X10] KeePassXC audits page, https://keepassxc.org/audits/
- [P1] KeePass licence, https://keepass.info/help/v2/license.html
- [P2] KeePass downloads and setup, https://keepass.info/download.html and https://keepass.info/help/v2/setup.html
- [P3] KeePass downloads, contributed and unofficial ports, https://keepass.info/download.html
- [P4] KeePass features and URL connections, https://keepass.info/features.html and https://keepass.info/help/v2/ioconnect.html
- [P5] KeePass synchronisation, https://keepass.info/help/v2/sync.html
- [P6] KeePass KDBX format and technical FAQ, https://keepass.info/help/kb/kdbx.html and https://keepass.info/help/base/faq_tech.html
- [P7] KeePass security, https://keepass.info/help/base/security.html, and 2.47 news, https://keepass.info/news/n210109_2.47.html
- [P8] KeePass composite master key and YubiKey, https://keepass.info/help/base/keys.html and https://keepass.info/help/kb/yubikey.html
- [P9] KeePass Auto-Type and plugins, https://keepass.info/help/base/autotype.html and https://keepass.info/plugins.html
- [P10] KPScript, https://keepass.info/help/v2_dev/scr_index.html
- [P11] KeePass plugin development, https://keepass.info/help/v2_dev/plg_index.html
- [P12] KeePass ratings and 2.57.1 notes, https://keepass.info/ratings.html and https://keepass.info/help/kb/kp_2.57.1_notes.html
- [B1] Bitwarden, "Is Bitwarden audited?", https://bitwarden.com/help/is-bitwarden-audited/
- [B2] Bitwarden downloads, https://bitwarden.com/download/
- [B3] Bitwarden server choice and self-hosting, https://bitwarden.com/help/choose-my-server/ and https://bitwarden.com/help/install-on-premise-linux/
- [B4] Bitwarden encryption, https://bitwarden.com/help/what-encryption-is-used/
- [B5] Bitwarden KDF algorithms, https://bitwarden.com/help/kdf-algorithms/
- [B6] Bitwarden FIDO2 two-step login, https://bitwarden.com/help/setup-two-step-login-fido/
- [B7] Bitwarden import from KeePass, https://bitwarden.com/help/import-from-keepass/
- [B8] Bitwarden browser extension, https://bitwarden.com/help/getting-started-browserext/
- [B9] Bitwarden CLI, https://bitwarden.com/help/cli/
- [B10] Bitwarden compliance and audits, https://bitwarden.com/help/is-bitwarden-audited/
- [B11] Bitwarden contributing documentation on licensing, https://contributing.bitwarden.com/contributing/template-repository/
- [O1] 1Password API and SDK terms, https://1password.com/legal/api-sdk-terms-of-service
- [O2] 1Password system requirements, https://support.1password.com/system-requirements/
- [O3] 1Password iOS and Android downloads, https://1password.com/downloads/ios and https://1password.com/downloads/android
- [O4] 1Password regions, https://support.1password.com/regions/
- [O5] 1Password sync, https://support.1password.com/sync/
- [O6] 1Password security, https://support.1password.com/1password-security/
- [O7] 1Password PBKDF2, https://support.1password.com/pbkdf2/
- [O8] 1Password security key, https://support.1password.com/security-key/
- [O9] 1Password import from KeePass and KeePassXC, https://support.1password.com/import-keepass/ and https://support.1password.com/import-keepassxc/
- [O10] 1Password browser extension, https://1password.com/downloads/browser-extension
- [O11] 1Password CLI, https://developer.1password.com/docs/cli/get-started/
- [O12] 1Password CLI shell plugins, https://developer.1password.com/docs/cli/shell-plugins/contribute/
- [O13] 1Password security assessments, https://support.1password.com/security-assessments/

The mystic-crypt-ui column rests on the evidence in [What mystic-crypt-ui has](#what-mystic-crypt-ui-has)
and, for "none", on the checks in [Where mystic-crypt-ui is weaker](#where-mystic-crypt-ui-is-weaker).

## Where mystic-crypt-ui is weaker

Each point was checked on `7dd653a5`; the check is named.

- **No browser integration and no auto-type.** No browser extension, native messaging host or
  auto-type exists (searched for `nativeMessaging`, `webextension`, `autotype`, `auto-type`,
  `autofill`, `java.awt.Robot`); `BrowserControlExtensions` only opens a URL in the browser. A
  KeePass entry's auto-type settings are not carried over on import (`docs/wiki/import-export-keepass-database.md`).
- **No mobile apps.** There is no mobile code; the only mention is "not on the list"
  (`docs/exploration/identity-and-direction-2026-09-10.md`).
- **No synchronisation.** The vault is a local file, and the application's code opens no network
  connection (no `HttpClient`, `Socket` or `URLConnection` in `src/main/java` or in the plugins'
  main code); the lethenon plugin reaches nodes through the lethenon library, and its
  "synchronise" is the chain, not the vault. Merging vaults is planned for the
  9.0 content format and not built (`docs/decisions/vault-content-format-9.0-2026-11-10.md`, #855,
  #858, #859, #860).
- **No external security audit.** None is published; `SECURITY.md` names the project's own checks
  (CodeQL, mutation testing) and seven published advisories (GHSA-w87j-p568-fr6v,
  GHSA-xp4c-rpcx-hw5c, GHSA-qxx5-r8qx-vj98, GHSA-9m97-3xjj-4gwm, and with 8.15
  GHSA-7qfp-94gw-vp62, GHSA-px49-m539-22h8, GHSA-f4g8-6wjr-5hch).
- **The key-file variants are the weaker format.** They write a Java-serialized envelope without a
  marker, a version or a recorded cost (`docs/vault-format.md:278-321`, #849 open). With a key file
  alone the content key is 128-bit AES, wrapped with RSA-OAEP (SHA-1, MGF1). With a password and a
  key file the password layer is `PBEWITHSHA1AND128BITAES-CBC-BC` at 65,536 iterations, not the
  600,000-iteration PBKDF2 of `MCRDB2`.
- **No hardware key.** No YubiKey, FIDO or PKCS#11 unlock exists (searched for `yubikey`, `fido`,
  `webauthn`, `pkcs11`, `smartcard`); the key store plugin leaves PKCS#11 out on purpose
  (`docs/wiki/Plugin-Key-Store.md:255`).
- **Plugins run without a sandbox.** A file placed in the plugin directory runs with the
  application's rights (`SECURITY.md`).
- **Tested on Linux.** The full test suite runs on `ubuntu-latest` only
  (`.github/workflows/gradle.yml:19`); on macOS, Windows and ARM Linux only the lethenon plugin's
  zip and archive tests run (`.github/workflows/lethenon-plugin-platforms.yml:40-45`). The
  installer creates a desktop entry on Linux and no shortcut on Windows or macOS
  (`src/main/izpack/install.xml:79-85`).
- **A Java runtime is needed.** The installer requires Java 25 and does not bring one
  (`src/main/izpack/install.xml:17`, README, "Requirements").
- **The installer is not reproducible.** It carries izpack entry times, absolute build paths, an
  unstable order of its dynamic variables and the jar signature's time (measured in #920, which is
  open); there is no CI job that builds twice and compares.
- **KeePass gaps.** A group's recycle-bin flag and an entry's custom icon, tags, colours, auto-type
  settings and custom data are not carried over (`docs/wiki/import-export-keepass-database.md`); a
  KDBX 3.1 file with a protected attachment is refused (#568); importing with a key file alone has
  code (`ImportKeePassDatabaseAction.java:135-155`) and no test; KDBX 4.1 is not evidenced.
- **No TOTP and no breach check.** A TOTP seed can only be stored as a property
  (`MysticCryptEntryModelBean.java:128`); nothing checks passwords against breach lists (searched
  for `pwned`, `hibp`, `breach`).
- **Few users, as far as it is measured.** On 2026-11-17 the GitHub repository had 8 stars and 3
  forks, and the 22 releases there had 393 asset downloads in total (GitHub API,
  `repos/astrapi69/mystic-crypt-ui` and its releases). Downloads from SourceForge were not
  counted, and no comparable figure for the other tools is given here.

## Method and limits

- **mystic-crypt-ui:** read from the repository on `7dd653a5` and from the GitHub API; the vault
  constants were confirmed against the mystic-crypt 13.11 jar the build resolves.
- **lethenon:** read from the lethenon repository on `develop` (`00d252a`) and its releases.
- **Other tools:** the official sites could not be fetched from the session that wrote this
  document; its network does not allow them. Every cell comes from web search results limited
  to the official domains (keepassxc.org, keepass.info, bitwarden.com, contributing.bitwarden.com,
  1password.com, support.1password.com, developer.1password.com) on 2026-11-17. Search results
  carry the page's address and a summary of it, not the page itself; a cell is what the search
  attributed to that page, and has not been read on the page.
- **Where the sources disagree, the table states nothing.** They disagree on KeePassXC's default key
  derivation (Argon2d in a 2023 review, Argon2id recommended in the user guide), on KeePass's
  default Argon2 variant, on Bitwarden's Argon2id defaults, and in detail on Bitwarden's
  licensing.
- **Ageing:** these products change. A cell is a statement about 2026-11-17 and goes stale; the
  sources are there so it can be checked again.
