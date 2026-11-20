# A plugin API of its own, and trust in plugin zips

Status: **open, the maintainer decides**. Prepared on 2026-11-20 for milestone 8.17 from the task of that day; the issues are
[#1092](https://github.com/astrapi69/mystic-crypt-ui/issues/1092) (Part 1) and
[#1093](https://github.com/astrapi69/mystic-crypt-ui/issues/1093) (Part 2). Nothing in it is built: until the tag RELEASE-8.16
there is analysis, this record and the issues, and no code on develop. Figures are measured on `develop` at `a80d9c54`
(2026-11-20) unless a line says otherwise; the command is named where one produced the figure, and an inference says so.
Goal of the milestone: the internal plugins build only against a plugin API of their own, and every plugin zip, the shipped
ones included, is checked for its origin before it is loaded. Both happen in this repository first; moving a plugin into a
repository of its own is a later step and not decided here.

Earlier decisions this builds on, none contradicted:
`plugin-vault-access-2026-10-31.md` (#675: least privilege, the host hands each plugin its own access, no static way in),
`release-key-openpgp-2026-10-29.md` (#642: OpenPGP through `bcpg`, library first, the primary key stays out of reach of agents),
#998 (the SHA-256 of a zip decides whether it is unpacked again), #1045 and #1060 (Install from Zip),
#678 (28 uses of `MysticCryptApplicationFrame.getInstance()`, open, P4), and `SECURITY.md` ("loads plugins from a directory on
the user's machine"). `chats-with-ai/coding/decisions-log.md` holds no decision on a plugin API or on signing plugin zips.

## Part 1: the plugin API

### 1.1 What the plugins take from the host today

Measured with JDK 25 `jdeps -verbose:class --multi-release 25 -cp mystic-crypt-ui-8.16-SNAPSHOT.jar <unpacked zip>/classes`
for each plugin zip against the host thin jar (287 classes), then split by the package
`io.github.astrapi69.mystic.crypt.plugin.api` (7 types: `PluginMenuContribution`, `PluginSettingsContribution`,
`PluginCommandContribution`, `VaultAccess`, `VaultAccessContribution`, `VaultAccessRefusedException`, `VaultExport`) and
everything else. Distinct host classes, nested classes counted:

| plugin | in plugin.api | outside plugin.api |
|---|---|---|
| certificate | 2 | 15 |
| checksum | 2 | 3 |
| console | 2 | 2 |
| conversion | 1 | 2 |
| file-crypt | 2 | 3 |
| kem-demo | 2 | 3 |
| keygen | 2 | 16 |
| keystore | 2 | 3 |
| lethenon (read only, ccw's) | 6 | 9 |
| menu-designer | 2 | 4 |
| obfuscation | 2 | 6 |
| password-hash | 2 | 3 |
| pqc-signature | 2 | 3 |
| release-key | 2 | 3 |
| secret-sharing | 2 | 3 |

30 distinct host classes outside the API in all. The lists per plugin are in the working folder
(`mystic-crypt-ui-worktrees/_work/plugin-api/host-usage-per-plugin.md`, the raw output beside it in `jdeps/`). Provenance: the
host jar and the zips of checksum and keygen were built from `a80d9c54` through the slot script (`publishToMavenLocal -x test`,
`pluginZip -x test`); the other 13 zips are the 8.15 ones, whose plugin sources are unchanged since
(`git diff --name-only 4aa8c8ff HEAD -- plugins | grep /src/main/` lists only checksum and keygen; plugin bytecode does not
change with the host's). Limits of jdeps: it sees class references, not reflection, and not constants the compiler inlined
(inference). The lethenon plugin's two bundled libraries (`lib/`) are not part of the count.

Call sites of the host classes that have methods, counted in the plugin bytecode with `javap -c` (call sites, not classes):

| host class | what the plugins call | plugins |
|---|---|---|
| `ui.form.ToolForm` | `buttons` 74, `newLayout` 58, `sized` 30, `scrolled` 20, `intro` 19, constants | 11 |
| `MysticCryptApplicationFrame` | `getInstance` 28, `switchToDesktopPane` 13, `getFrameMode` 13, `getModelObject` 6, `getConfigurationDirectory` 4, and for the menu designer `saveMenuLayout`, `resetMenuLayout`, `exportCurrentMenuXml`, `applyMenuXml`, `getDesktopPanePanel` | 15 |
| `settings.PluginSettings` | `load` 16, `asInt` 10, `configurationDirectory` 4, `save` 2 | 12 |
| `panel.keygen.EnDecryptPanel` | a panel of the host, 8 getters, a constructor | keygen, obfuscation |
| `ApplicationModelBean`, `panel.signin.MasterPwFileModelBean` | `getMasterPwFileModelBean`, `getPrivateKeyInfo`: the login's private key model (`ObfuscationOperationRuleTablePanel.java:141`) | obfuscation |
| `wizard.*` (13 classes), `panel.certificate.NewCertificateInfoPanel` | the certificate wizard, its models and `CertificateInfoModelToX509` | certificate, keygen |
| `panel.dbtree.MysticCryptEntryModelBean`, `VaultEntryProperties`, `VaultEntryAttachments` | the vault entry model | lethenon |
| `clipboard.ClipboardClearWatchdog`, `write.OverwriteConfirmation`, `write.DataClass`, `ui.screen.ScreenPlacement`, `menu.MenuLayoutSupport` | single helpers | lethenon, keygen, menu-designer |

Two facts about the existing API itself: `plugin/api/VaultAccess.java:30` imports `panel.dbtree.MysticCryptEntryModelBean` and
uses it in `entries()`, `add(...)` and `change(...)`, so the API hands out a host-internal type; and `PluginCommandContribution`
is implemented by no plugin (`grep -rn --include='*.java' PluginCommandContribution plugins/*/src/main` finds nothing).

Version check today: `grep -rn "plugin.requires" plugins/*/src/main/resources` and `grep -rn --include='*.java'
"setSystemVersion" src plugins` find nothing, and `javap -c` of pf4j 3.16.1 `AbstractPluginManager.isPluginValid` skips the
constraint when the system version is `0.0.0`, the default. So `plugin.requires` is never enforced. Measured with
`DefaultVersionManager` (`poc/requires-check.out`): `^1.0.0`, `>=1.0.0 & <2.0.0` and `1.x` all accept 1.2.0 and refuse 2.0.0 and
refuse 0.0.0.

### 1.2 Scope of the API

What must be in it, what each use becomes (the shape follows #675: the host hands the plugin an access object, no static way in):

| today | in the API as |
|---|---|
| the 7 existing types | stay; `VaultAccess` gets an API entry type (id, title, properties, attachments) instead of `MysticCryptEntryModelBean`, the host maps it |
| `MysticCryptApplicationFrame.getInstance()` for windows (`switchToDesktopPane`, `getFrameMode`, parent window) | a `HostWindow` access handed to the contribution in a `connect` method; this is #678's second class of uses |
| `getConfigurationDirectory`, `PluginSettings` | `PluginSettingsStore` in the API (the class moves; it is a plain file helper) |
| `ToolForm` | moves into the API (layout helper over miglayout, already a `compileOnly` of the plugins) |
| `ClipboardClearWatchdog`, `OverwriteConfirmation`, `DataClass`, `ScreenPlacement` | small API types, or part of `HostWindow` |
| reading the model (`getModelObject`, the login private key) | no model access: a narrow read-only access for what the obfuscation plugin needs, decided in #678 (these are the uses it ranks first) |
| `EnDecryptPanel`, `NewCertificateInfoPanel`, the wizard and its models, `CertificateInfoModelToX509` | not API: features that sit in the host and belong to plugins (architecture.md, plugin-first). They move into the plugin that owns them; two plugins use each of them (certificate and keygen; keygen and obfuscation), so the shared code goes into a support library the zips bundle in `lib/`, as the lethenon zip bundles its own |
| menu designer: `saveMenuLayout`, `resetMenuLayout`, `exportCurrentMenuXml`, `applyMenuXml`, `MenuLayoutSupport` | a `MenuLayoutAccess` in the API, or the plugin is named in the boundary check as an exemption with its reason until then (development tooling, not shipped, architecture.md) |

### 1.3 Options

**Option 1 (recommended): a Gradle subproject `plugin-api` with an artifact of its own.** The contract types, the small helpers
above, no host classes. Artifact `io.github.astrapi69:mystic-crypt-ui-plugin-api` in this repository; the host depends on it and
implements it; the plugins have `compileOnly` on it and on family libraries, and no longer on the host jar. The package name
stays `io.github.astrapi69.mystic.crypt.plugin.api`, so the plugins already shipped keep running against the host (binary
compatible; inference, to be proven by starting the 8.15 zips against the new host). The build check is the compiler: a host class
outside the API no longer resolves. Beside it a test over each built zip runs jdeps and fails on any reference to a class of
`io.github.astrapi69.mystic.crypt` outside `.plugin.api` and the plugin's own package, which catches what compilation cannot
(a stale class, a bundled copy); the exempt plugin is listed in the build file with its reason (gate principle 2).
Versioning: SemVer for the API, 1.0.0 at the first cut; within a major only additions (new default methods, new types), removals
only in a new major. The host calls `setSystemVersion(<API version>)`; each plugin states `plugin.requires=^1.0.0` (or
`>=1.0.0 & <2.0.0`); pf4j then refuses a plugin whose range does not contain the host's API version, and the Settings > Plugins
list shows the reason. The version of the plugin itself (`plugin.version`) stays separate.
Consequences: one more subproject and one more artifact to keep in step in the release (the host release publishes both);
moves of classes out of the host (`ToolForm`, `PluginSettings`) touch every plugin once; the certificate wizard and the two
panels move, which is the largest piece and a separate issue each; lethenon's change is ccw's.

**Option 2: the package stays in the host jar, plugins keep compiling against the whole host, a jdeps test enforces the boundary.**
Cheapest. Consequences: a violation is found after it was written, not impossible; no separate version, so `plugin.requires`
would name the host's version, which changes every release and says nothing about compatibility; moving a plugin to its own
repository later would still need the API cut first.

**Option 3: Java modules (`module-info`) to export only the API.** Not workable: pf4j loads plugins through class loaders, the
host has no module descriptor, and XStream persistence is a protected decision (lessons-learned.md). Rejected.

Recommendation: option 1. Reason: the boundary becomes a property of the build, not of review, and the version of the contract
becomes something pf4j can check; it is also the precondition for the later split into repositories. Best practice: pf4j's model
is an API that plugins depend on, with the compatible range in `plugin.requires` (`PluginDescriptor.getRequires`,
`VersionManager.checkVersionConstraint`, `AbstractPluginManager.setSystemVersion`, pf4j 3.16.1, measured with `javap`); least
privilege for plugins is #675's own principle. A binary-compatibility checker such as japicmp is not proposed: it is a new
tool, and AG-100 asks for a failure that already happened first.

### 1.4 Publishing on Central: later, not now; what it would need

The group `io.github.astrapi69` is already published by the family. Missing for the API: POM metadata, sources and javadoc jars
(the build already makes both), signing with the release subkey through the publish workflow of the family and its secrets
(RL-090), a Central Portal publish workflow run once against a snapshot first (release-workflow.md step 6), a rule for tags and
for when the API version moves, reproducible builds (#920), and only Central dependencies in the API's POM (pf4j and the menu
library for `Anchor`). A published API is a promise that cannot be withdrawn, which is why the 1.0.0 cut waits for the plugins
to have built against it for a release.

## Part 2: trust in plugin zips

### 2.1 Threat model

Today (measured, issue #1093): a zip with a readable `plugin.properties` is installed, unpacked and started without any check of
its origin, the shipped ones included. The #998 record (`<directory>.sha256`) is written by the application from the zip it finds;
`PluginArchives.isUnpackedFrom` compares a zip with that record, which detects a difference, not an author. A plugin runs in the
host's JVM with the rights of the user and can reach the vault model (#678), so a loaded plugin is as trusted as the host.

In scope:
- T1, a zip chosen in Settings > Plugins > "Install from Zip..." that was changed on the way or was not made for this application;
- T2, a zip in `~/.config/mystic-crypt-ui/plugins` replaced between two starts (by a sync tool, a restored backup, a shared
  folder, or a process that may write there but should not run code in the application);
- T3, the unpacked directory changed after unpacking: the #998 record covers the zip only, and pf4j loads a directory with no zip
  at all (inference from its repository code, not run);
- T4, the same for the 14 zips the installer lays down. The installer jar itself is jarsigned (`gradle/packaging.gradle`,
  `scripts/verify-installer.sh`), but the zips are plain files once installed.

Not in scope, named so nobody counts on it:
- an attacker who can already write the host jar or the embedded key: only a signature of the host that the JVM checks would
  help, and `java -jar` checks none (inference);
- a compromised release key or build machine, and a plugin that is properly signed and malicious;
- sandboxing a loaded plugin: the JVM offers none (the SecurityManager is permanently disabled since JDK 24, JEP 486);
- downgrade to an older signed zip: it is not prevented by a signature alone; `plugin.version` ordering and a minimum version
  can be added later;
- the user's own choice: a consent dialog is not a defence against a user who accepts everything.

Honest limit: a process that may write to the user's configuration directory is, on most systems, already able to run code as
the user. What the check buys is that a zip not made by the maintainer is not run by accident, that a changed or foreign zip is
noticed, and that the anchor of trust sits in the host (the embedded key), outside the directory that is written.

### 2.2 Options for the signature

**Option A (recommended): a detached OpenPGP signature of the zip, `<zip>.asc` beside it, checked with Bouncy Castle against a
public key embedded in the host.** The zip stays byte for byte what pf4j and #998 handle; the SHA-256 of #998 stays as the cache
key for "unpack again", behind the signature check. The signature is the format Maven Central requires and `gpg --verify`
checks (RFC 9580, section 10.4). The reader code goes into the mystic-crypt library first (library first, as in #642), which
already carries `bcpg` 1.86; the host calls it. The maintainer signs at the release with `gpg --detach-sign` as for the other
artifacts; the installer packs the zip and its `.asc`. Consequences: two files to keep together (a zip whose `.asc` is missing
counts as unsigned); the embedded certificate must carry the signing subkey's binding, so a renewed subkey needs the updated
certificate in the next host release (AG-055's renewal); the embedded set is a list of keys so that another key can be added
without a format change.

**Option B: jarsigner signatures of plugin jars, checked against the certificate.** Not applicable as is: the plugin zips hold
loose classes (`classes/...`, `pluginZip` in each plugin's `build.gradle`), and jarsigner signs jars. It would need every plugin
packed as a jar inside the zip (`lib/`), which changes the layout pf4j reads; `plugin.properties` and `lib/*.jar` stay outside the
signature unless covered by a second check; the certificate chain, validity and expiry need PKIX handling and a timestamp
authority to outlive the certificate; the key is the keystore of the host jar. Consequences: the largest build change, a
different trust root than the one the maintainer's release already uses for artifacts, and verification through the JDK instead of
the family libraries.

**Option C: a signed checksum list** (`SHA256SUMS` plus `.asc` per release, the form of the installer's checksum files), which
lists every shipped zip with its SHA-256, plugin id and version. One signature for 14 zips; reuses `checksumOf` of #998; the list
can carry the API range. Consequences: a zip that is not on the list (a third-party plugin, one zip installed alone) has no
route except a list of its own; an old valid list can be replayed unless it carries a serial; the list must be rebuilt and signed
whenever one zip changes.

Recommendation: option A, with the verification code written so that a signed list (C) can be added for a set of zips later.
Reason: it keeps the zip format, needs no layout change, uses the key type the release already has, and works for one zip alone,
which is the Install from Zip case. Best practice: detached OpenPGP signatures as Maven Central and distribution packages use
them (RFC 9580, section 10.4); the verifying key outside the place the attacker writes (NIST SP 800-57 Part 1 on key
management, protection of public verification keys).

### 2.3 Questions inside option A

**Which key signs.**
1. The release key's signing subkey (recommended for the first cut): one trust root, no new secret, the maintainer signs by hand
   at the release as for Maven Central and the installer checksums, no agent touches the key.
2. A key of its own for plugins: separates the blast radius from Maven Central and lets another maintainer sign plugins, at the
   price of a second offline key and a second renewal.
Reason for 1: fewer secrets for a solo maintainer; the embedded list of keys allows 2 later (NIST SP 800-57: key separation is a
reason for 2 once someone else signs).

**An unsigned zip, or one with an unknown key or a bad signature.**
1. At the start: not loaded, named in Settings > Plugins with the reason (recommended: fail closed, gate principle 1). The
   shipped plugins are never loaded unsigned.
2. At Install from Zip: refused by default; the user may accept it once after a dialog that shows the file name, the SHA-256 and
   the key fingerprint or "no signature" (recommended, for plugin authors and the development profile). The consent is recorded
   by the zip's SHA-256 in the plugin settings, shown as "unsigned, installed by you" at every start. A changed zip asks again.
   Limit: that record lies in the writable directory, so it is no defence against T2, only against T1.
3. Warn and load: not recommended, it is the state of today with a dialog.
4. A system property that switches the check off: not recommended; if the maintainer wants one for development, it belongs to
   the isolated development profile only (`make run`), is named in the window title, and the e2e suites use throwaway keys
   instead.

**When it is checked.** At install, before the zip is copied into the plugins directory; at every start, before
`PluginArchives.unpackChanged` unpacks anything; and the unpacked directory too (T3): at unpacking the application also records
a hash over the directory's files next to the #998 record, checks it at the start, and refuses a directory with no verified zip,
through a `PluginRepository` of its own (`DefaultPluginManager.createPluginRepository` is overridable; measured with
reflection). Cost: the unpacked plugins are 15.4 MB in all, 13.3 MB of them the lethenon plugin with its bundled `sqlite-jdbc`
(`du -sk` of the unpacked zips); the time of hashing them at every start is not measured. The #998 record stays: it decides cheaply whether to
unpack again, and is trusted only after the signature held.

### 2.4 The shipped plugins and the tests

The 14 shipped plugins are checked exactly like any other: the build of the installer writes `<zip>.asc` for each, the installer
lays down both (`src/main/izpack/install.xml`), `make verify-installer` gains a check that every `.asc` verifies against the
embedded key. The menu designer, not shipped, is built unsigned and is for the development profile. Signing the real zips is the
maintainer's step at the release, as it is today for the installer; no agent touches the key.

Crypto only through the family libraries and Bouncy Castle (RL-098, architecture.md): OpenPGP through `bcpg` in mystic-crypt,
SHA-256 through `HashAlgorithm` and `checksum-up` as in #998. Tests only with throwaway keys generated at run time (the
mystic-crypt `GnuPgHomeTest` pattern), never a key file in the repository; the host takes its trust anchor through an injectable
set of keys so a test can pass its own, which is the only way the check is exercised against zips built in the tests. Test
first (RL-001): a changed zip, a missing `.asc`, a signature of an unknown key, a changed directory, a signature made with a
revoked or expired subkey, and the round trip of the real install path with a signed zip (quality-checks.md).

## What stays for the maintainer

1. Part 1: option 1, 2 or 3; the scope table of 1.2 as drafted or narrowed; menu designer in the API or exempt.
2. Part 2: option A, B or C; release subkey or a plugin key; unsigned handling (1 to 4 above); whether the directory is checked
   (T3) in the first cut.
3. Public wording: `SECURITY.md` says the application "loads plugins from a directory"; issue #1093 describes the gap neutrally,
   with no attack steps, as the task asked. Where `SECURITY.md` keeps back a path that is open in every release, the maintainer
   may prefer the private route; this record and the issue follow the task's instruction to keep both public.

Proof of concept files, kept outside the repository: `mystic-crypt-ui-worktrees/_work/plugin-api/` (`jdeps/`, `poc/UnsignedZipPoc.java`
with `poc/unsigned-zip-poc.out`, `poc/RequiresCheck.java` with `poc/requires-check.out`, `host-method-calls.txt`).
