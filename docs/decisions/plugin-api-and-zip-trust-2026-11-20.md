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
the user's machine"). No earlier record in `docs/decisions/` decides a plugin API or the signing of plugin zips
(`git grep -n -i -E "plugin.?api|zip" edba6448 -- docs/decisions` finds nothing in the 7 files
of that directory).

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

30 distinct host classes outside the API in all. The lists per plugin and the raw output are kept outside the repository; the
command above, run per zip, gives them again. Provenance: the host jar and the zips of checksum and keygen were built from
`a80d9c54` (`publishToMavenLocal -x test`, `pluginZip -x test`); the other 13 zips are the 8.15 ones, whose plugin sources are
unchanged since
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
is implemented by no plugin (`grep -rn --include='*.java' PluginCommandContribution plugins/*/src/main` finds nothing in the 247 Java files of the plugins' main sources, at `a80d9c54`).

Version check today: `grep -rn "plugin.requires" plugins/*/src/main/resources` and `grep -rn --include='*.java'
"setSystemVersion" src plugins` find nothing (29 resource files and 1019 Java files searched, at `a80d9c54`), and `javap -c` of pf4j 3.16.1 `AbstractPluginManager.isPluginValid` skips the
constraint when the system version is `0.0.0`, the default. So `plugin.requires` is never enforced. Measured with a scratch
program over `DefaultVersionManager` (kept outside the repository, like the jdeps output): `^1.0.0`, `>=1.0.0 & <2.0.0` and
`1.x` all accept 1.2.0 and refuse 2.0.0 and refuse 0.0.0.

### 1.2 Scope of the API

What must be in it, what each use becomes (the shape follows #675: the host hands the plugin an access object, no static way in):

| today | in the API as |
|---|---|
| the 7 existing types | stay; what `VaultAccess` takes and returns instead of `MysticCryptEntryModelBean` is open, the maintainer decides after RELEASE-8.16 (1.5; an API entry type with id, title, properties and attachments that the host maps is its option 3) |
| `MysticCryptApplicationFrame.getInstance()` for windows (`switchToDesktopPane`, `getFrameMode`, parent window) | a `HostWindow` access handed to the contribution in a `connect` method; this is #678's second class of uses |
| `getConfigurationDirectory`, `PluginSettings` | `PluginSettingsStore` in the API (the class moves; it is a plain file helper) |
| `ToolForm` | moves into the API (layout helper over miglayout, already a `compileOnly` of the plugins) |
| `ClipboardClearWatchdog`, `OverwriteConfirmation`, `DataClass`, `ScreenPlacement` | small API types, or part of `HostWindow` |
| reading the model (`getModelObject`, the login private key) | no model access: a narrow read-only access for what the obfuscation plugin needs, decided in #678 (these are the uses it ranks first) |
| `EnDecryptPanel`, `NewCertificateInfoPanel`, the wizard and its models, `CertificateInfoModelToX509` | open for the wizard, its models, `CertificateInfoModelToX509` and `NewCertificateInfoPanel`: the API, a family library or API types, the maintainer decides after RELEASE-8.16 (1.5). As drafted for all of them: not API, features that sit in the host and belong to plugins (architecture.md, plugin-first); they move into the plugin that owns them, and as two plugins use each of them (certificate and keygen; keygen and obfuscation), the shared code goes into a support library the zips bundle in `lib/`, as the lethenon zip bundles its own |
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
`>=1.0.0 & <2.0.0`); pf4j then puts a plugin whose range does not contain the host's API version into the state DISABLED. It
does not reject it: the plugin is loaded, `getPlugin` still finds it and its `failedException` says only "Plugin validation
failed" (measured on pf4j 3.16.1, see "Existing solutions", pf4j). So the host checks the state of every plugin after loading
and shows a disabled plugin with its reason (the plugin's `requires` range and the API version it met) in Settings > Plugins;
today that list has three columns, plugin, version and state (`PluginsSettingsPanel.java:298`), and shows no reason. The test
for this checks the state and the display, not only the absence of the plugin's menu. The version of the plugin itself
(`plugin.version`) stays separate.
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
(the build already makes both), signing with the release subkey through the publish workflow of the family and its secrets,
a Central Portal publish workflow run once against a snapshot first (release-workflow.md step 6), a rule for tags and
for when the API version moves, reproducible builds (#920), and only Central dependencies in the API's POM (pf4j and the menu
library for `Anchor`). A published API is a promise that cannot be withdrawn, which is why the 1.0.0 cut waits for the plugins
to have built against it for a release.

### 1.5 The wizard classes and `VaultAccess`: API, a family library, or API types

The maintainer's question (2026-11-20, German, translated): the certificate wizard classes (13, used by certificate and keygen)
and `VaultAccess` with `MysticCryptEntryModelBean`: into the API, into a family library of their own, or replaced by API types?
The decision is taken after RELEASE-8.16. Facts, measured on `develop` at `edba6448` unless a line says otherwise.

**The wizard classes.**
- The 13 holds as a bytecode count, not as a source count. The two plugins import 10 distinct `wizard.*` classes (certificate all
  10, keygen 5: `CertificateInfoModelToX509` and the four models). `jdeps -verbose:class --multi-release 25` with the host jar
  `mystic-crypt-ui-8.15-SNAPSHOT.jar` (a build of 2026-11-15) on the class path and the unpacked classes of the 8.15 certificate
  and keygen zips lists 13 wizard types: those 10 and three Lombok nested builders, plus `panel.certificate.NewCertificateInfoPanel`
  (keygen). Caveat: this run is not a rebuild of `edba6448`. Keygen has changed since 8.15, in `e1034a61` (2026-11-19;
  `git merge-base --is-ancestor e1034a61 RELEASE-8.15` fails), and that change adds no host import:
  `git diff RELEASE-8.15 edba6448 -- plugins/keygen-plugin/src/main plugins/certificate-plugin/src/main | grep '^[+-].*import'`
  adds only `io.github.astrapi69.crypt.api.key.KeyFileFormat`, a crypt-api type.
- The real set to move is larger: the source closure of the 10 inside the wizard packages is 17 files (all 18 files of `wizard/` but
  `CertificateWizardPanelTest.java`, a class of that name that sits in `src/main`), 2,681 lines (the sum of the per-file line counts, `git show origin/develop:<file> | wc -l`), with `NewCertificateInfoPanel` (606 lines) beside it. Closure computed by a
  name-reference script over `git archive origin/develop src/main/java`; the script is not in the repository.
- The host itself does not use the wizard panels: outside `wizard/`, only `panel/certificate/NewCertificateAttributesPanel.java:34`
  and `panel/certificate/NewCertificateInfoPanel.java:42-44` import wizard classes, and those are three of the four models,
  `CertificateInfoModel`, `DistinguishedNameInfoModel` and `ValidityModel`
  (`git grep -n 'mystic\.crypt\.wizard' edba6448 -- src/main ':!src/main/java/io/github/astrapi69/mystic/crypt/wizard'`, 252 files searched: the 270 of `src/main` less the wizard package).
  `NewCertificateInfoPanel` has no host main user either (`git grep` of the 270 files of `src/main` finds only a comment in
  `messages.properties:145`); keygen uses it (`CryptographyPanel.java:37`) and the wizard's `IssuerPanel` and `SubjectPanel` reach
  `NewCertificateAttributesPanel`. Host tests use the wizard (`src/test/.../wizard/*Test.java`, three e2e smoke tests).
  **Finding, not an option:** the wizard code is used by the two plugins only; the host carries it as plugin code.
- Dependencies of the 17 files outside `wizard/`: `Messages`, `eventbus.ApplicationEventBus`, `random.SecureRandomValues` and
  `panel.certificate.NewCertificateAttributesPanel`. `NewCertificateInfoPanel` also pulls `MysticCryptApplicationFrame` (970
  lines), `ui.screen.ScreenPlacement` and `random.*`. Libraries: swing-wizard 2.1, state-pattern 7.1, observer 6 (the wizard's
  events, `CertificateWizardPanel.java:27-28`, `DatesPanel.java:39-40`; the certificate plugin declares it `compileOnly`,
  `plugins/certificate-plugin/build.gradle:51`), model-data, swing-base-components, swing-model-components, silly-collection,
  silly-bean 4.0 (`ValueBox`, `DatesPanel.java:37`; silly-bean is archived and gets no further releases, as its README says),
  crypt-data, LGoodDatePicker, MigLayout, Lombok and Bouncy Castle (`org.bouncycastle.asn1` in three files). The four models are
  Swing-free (Lombok and crypt-data models); through `CertificateInfoModel` (`:90`, `ExtensionInfoModel[] extensions`) they pull
  in `ExtensionInfoModel`, which uses four Bouncy Castle ASN.1 types (`ExtensionInfoModel.java:32-35`).
- The family has two generic wizard libraries, the public repositories `astrapi69/swing-wizard` (the panels) and
  `astrapi69/state-pattern` (the wizard's state machine, `io.github.astrapi69.design.pattern.state.wizard`), both already
  dependencies of this application (`gradle/libs.versions.toml:72, 81, 152, 161`; the certificate plugin declares both
  `compileOnly`, `plugins/certificate-plugin/build.gradle:48-49`). A certificate wizard library does not exist in the family
  (`gh search repos wizard --owner astrapi69 --owner lightblueseas` lists only `astrapi69/swing-wizard`; a repository search, not a
  file search, over the 363 and 92 public repositories of the two owners); it would be new.

**`VaultAccess` and the entry bean.**
- `plugin/api/VaultAccess.java:30` imports `panel.dbtree.MysticCryptEntryModelBean`; `entries()` (`:54`) returns it, `add` (`:65`)
  and `change` (`:76`) take it; `remove(UUID)` does not. The one implementation is `plugin/vault/MarkedEntryAccess.java:44`. Only
  lethenon uses `VaultAccess` (`git grep -l -E 'plugin\.api\.VaultAccess|VaultAccessContribution' origin/develop -- plugins`,
  minus lethenon, is empty: 487 files of `plugins` searched at `e9c7d983`, 6 hits, all in lethenon); its main sources call `getId`, `getTitle`, `setTitle(char[])`, `getProperties` and the builder with
  `id`, and through the host helpers `VaultEntryProperties` and `VaultEntryAttachments` also the protected property keys and the
  resources. It touches nothing else of the bean (password, user name, URL, notes, expiry, times, history).
- **The bean is persisted, and its class name is part of the format.** The vault XML names the element by the fully qualified name
  (XStream, no alias): `VaultFormatIsUnchangedTest.java:91-102` and `EntryPropertiesTypeIsPartOfTheFormatTest.java:92-105` hold
  `<io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>`; `ApplicationModelBean.java:62,67` holds it; and
  `VaultXmlCodec.java:118-122` allowlists it, so a reader builds only listed types. Moving or renaming the class is a
  persisted-format change (architecture.md: ask first). A mechanism for reading old names exists (`VaultXmlCodec.java:105-107`,
  `:270-276`, `xStream.alias(oldName, type)`, used for 7.x names in #624 and held by `VaultsOfSevenXOpenTest`), but the alias given
  last decides the written name, so a rename that older builds must still read keeps the old name on disk. 30 files in `src/main`
  and 51 in `src/test` and `src/e2eTest` reference the class (`git grep -l` at `edba6448`, 270 files searched in `src/main` and 395 in `src/test` and `src/e2eTest`).
- The API package has 7 types today and no value type: interfaces, one exception, one enum (`git ls-tree` of
  `src/main/java/io/github/astrapi69/mystic/crypt/plugin/api`). `VaultAccess.java:30` is its only host-internal import.

**Options.**

1. **Into the API** (the classes themselves become part of `plugin-api`). Wizard: the 17 files and the panel move into the API
   artifact. Consequences: the API gains Swing panels and the dependencies above (swing-wizard, state-pattern, crypt-data, Bouncy
   Castle, LGoodDatePicker), so a plugin that wants only `ToolForm` and `PluginSettingsStore` pulls them; every change to a panel
   or a model is a change of the API's contract (a removal needs a new major, semver.org spec item 8), and 1.3 option 1 meant the
   API to be small and stable; the host's `NewCertificateInfoPanel` copy and its `MysticCryptApplicationFrame` use must be cut
   first. Bean: `MysticCryptEntryModelBean` goes into the API artifact. If the package name stays, the persisted format does not
   change, but the API then publishes a class whose fields (KeePass fields, history, `FileContentInfo` from file-worker,
   `KeyValuePair` from silly-bean, which is archived) are the vault format, so the format becomes an API promise and lethenon,
   ccw's, sees the whole entry; if the package changes, the vault format changes and `VaultXmlCodec` needs the alias, with the
   effect on older builds above. The class and `VaultAccess` would be the only host types in the API. Risk: the narrowest-access rule of #675
   is lost for the entry.
2. **Into a family library of their own.** Wizard: a new library (a name and a repository are the maintainer's; a Gradle subproject
   here first, published later, is the same step as 1.4) holds the 17 files; certificate and keygen bundle it in `lib/` (the
   lethenon zip bundles its own) or declare it `compileOnly` if the host ships it. Consequences: a new repository or a new
   subproject with its own versions, Central publishing (signed as in 1.4) and family release cycle; the host removes
   `NewCertificateAttributesPanel`, `ApplicationEventBus`, `Messages`, `SecureRandomValues`, `ScreenPlacement` and
   `MysticCryptApplicationFrame` from the closure first (the library cannot depend on the host); the library would start with an
   archived dependency, silly-bean's `ValueBox` (`DatesPanel.java:37`), or would have to replace it first; the 4 models and the
   panels in host tests (`src/test/.../wizard`, three e2e smoke tests) move with it; the plugins' zips grow by the library, and if
   the host and a zip both carry it, class loading decides which copy runs (not measured). Bean: a library of the entry model would
   hold the persisted class; the family has no vault entry library (`gh search repos vault --owner astrapi69 --owner lightblueseas`
   lists this application and `ai-key-vault`, a TypeScript library for AI provider keys; a repository search over the 363 and 92 public
   repositories of the two owners, not a file search), so it would be new too, and the same two
   cases as in option 1 apply to its package name; the host would depend on the library for its core format, which is the largest
   change of the three for something only lethenon reads.
3. **Replace by API types.** Bean: the API gets a small value type for an entry (id, title, properties, protected property names,
   attachments); `VaultAccess` uses it and `MarkedEntryAccess` maps it to the bean and back. The bean, its package and the vault
   format do not change; lethenon's change is ccw's, bounded to `LethenonVault`, `VaultWalletEntry` and the plugin's tests (18
   test files of the plugin use `VaultAccess`, the bean or one of the two helpers, 17 of them `VaultAccess`, among them an
   in-memory one, `InMemoryVaultAccess`:
   `git grep -l -E 'VaultAccess|MysticCryptEntryModelBean|VaultEntryProperties|VaultEntryAttachments' edba6448 -- 'plugins/lethenon-plugin/src/test/*' | wc -l`, 64 files searched);
   the two helpers `VaultEntryProperties` and `VaultEntryAttachments` become API methods or host-side mapping. Wizard: this
   works for the wizard only where a plugin needs a value, not a panel. Keygen needs
   `CertificateInfoModelToX509`, the four models and `NewCertificateInfoPanel`; a narrow API type for "the data of a certificate
   request" could replace the models, but the panel and the conversion would still have to live in one plugin that the other
   cannot depend on. Whether keygen can shrink to a value type is not measured here; as far as the facts reach, option 3 alone
   does not remove the wizard from the host.

What each option does on the day a vault changes: with 1 and 2 a field added to the entry bean for the host's own needs is
visible to every plugin and, in 2, needs a library release first; with 3 the host adds the field and the API type is widened only
when a plugin needs it (additive, a minor version).

**Recommendation (a suggestion):** the entry through option 3, the wizard through option 2, in two steps. Reasons: the vault
format is a decision that is already protected (architecture.md, `VaultXmlCodec`'s allowlist), and #675's principle is that a
plugin sees what it needs, which an API entry type honours and the bean does not; an API that carries 17 files of Swing and
a certificate model is not the small, stable contract of 1.3, and the wizard is used by two plugins only, so a library they both
bundle fits it better than the host. The first step for the wizard can be a Gradle subproject in this repository (no new
repository, no Central release yet), which removes the host closure first and keeps the Central step for after the plugins have
built against it for a release (as 1.4 says for the API). Best practice: semver.org specification 2.0.0, item 1 (the public API
is declared and precise) and item 8 (a backward-incompatible change to it needs a new major): what is in the API is promised, so
it holds only what a plugin must see (read at semver.org/spec/v2.0.0.html, 2026-11-20). The two rows of the 1.2 table this
section touches, the entry type of `VaultAccess` and where the wizard goes, are marked open there; the maintainer decides them
after RELEASE-8.16.

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
- T4, the same for the 14 zips the installer lays down. The application jar inside the installer is jarsigned
  (`gradle/packaging.gradle`, `scripts/verify-installer.sh`), but the zips are plain files once installed.

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

**No path loads a plugin over the network** (issue #1093; measured on `develop` at `edba6448`, 2026-11-20). The question was
whether the application has an update, download or catalog route for plugins; if it had, the threat model below would be
wrong and the issue could not stay public. It has none:
- Both plugin managers are pf4j `DefaultPluginManager` over a local directory: `MysticCryptApplicationFrame.java:297-299` (the
  `plugins` folder of the configuration directory) and `MysticCryptUiCli.java:166` (the directory of the property
  `mystic.crypt.ui.plugins.dir`, or the installed one, `MysticCryptUiCli.java:108-110`).
- The one way to install a plugin is "Install from Zip..." (`PluginsSettingsPanel.java:83`), a file chooser over local zip files
  (`PluginsSettingsPanel.java:134-146`).
- The version catalog carries `org.pf4j:pf4j` 3.16.1 only (`gradle/libs.versions.toml:64` and `:144`), not `pf4j-update`, the
  pf4j module for plugin repositories and updates.
- No main source of a plugin touches `java.net` or `javax.net` at all, the host only through `java.net.URL` and its parsing
  exceptions, and no main source uses `HttpClient`, `openConnection`, `openStream`, `URLClassLoader`, `pf4j.update`,
  `UpdateManager`, `UpdateRepository`, `PluginRepository`, `InetAddress` or `DatagramSocket`:
  `git grep -n -E "java\.net|javax\.net|HttpClient|openConnection|openStream|URLClassLoader|pf4j\.update|UpdateManager|UpdateRepository|PluginRepository|InetAddress|DatagramSocket" edba6448 -- src/main/java "plugins/*/src/main/java/*"`
  searches 432 files (185 in the host, 247 in the plugins) and prints five import lines in three host files, `java.net.URL` three times and `java.net.MalformedURLException` and
  `java.net.URISyntaxException` once each, and nothing of a plugin. The pathspec needs the trailing `/*`: written as
  `plugins/*/src/main/java` it matches no file, and the command then searches the host alone. The three files:
  `SecretKeyTreeWithContentPanel` (validates an entry's address and opens it in the browser, lines 1284-1296 and 1436-1450) and
  the two sign-in panels, whose Help button asks whether the wiki page answers (`URLExtensions.isReachable`,
  `MasterPwWithApplicationFilePanel.java:437`, `NewMasterPwFilePanel.java:352`) before it opens the browser. None of the three
  fetches code.
- The installer's `src/main/izpack/install.xml` has no web packs: the plugin zips are files inside the installer
  (`install.xml:138-139` for lethenon).
- The shipped lethenon plugin runs the lethenon library's peer-to-peer node: `jdeps -verbose:class` on `lethenon-0.9.0.jar` (the
  version `gradle/libs.versions.toml` pins) shows `java.net.Socket`, `ServerSocket` and `Proxy` in
  `io.github.astrapi69.lethenon.transport`, and the plugin uses that package (`NodeSupport`, `RunningNode`, `SyncSupport`). It
  carries chain data, not code: the same `jdeps` run shows no `ClassLoader` and no `org.pf4j` dependency in the library. Its file
  writes at RELEASE-0.9.0 (`ChainExport.java:159-160` to a target the caller gives, `ChainMigration.java:200` next to the chain
  file, `SqliteNativeLibrary.java:244-245` the native library of the bundled `sqlite-jdbc` into `$XDG_CACHE_HOME/lethenon/native`,
  `WalletFile.java:120`) and the plugin's one (`WalletVault.java:151`, a wallet export to a file the user chose, `CREATE_NEW`) do
  not write into the plugins directory. That last sentence is an inference from these writes, not a review of lethenon's network
  code.

So the only route into the plugins directory is a file that a person or a process puts there, which is what T1 to T4 describe.
For a revocation, used in 2.5: the application does use the network (the help page check and the lethenon node above), but no
main source checks whether a key is revoked or fetches a key (measured:
`git grep -n -i -E "hasRevocation|isRevoked|KEY_REVOCATION|SUBKEY_REVOCATION|revoked|keyserver|hkps?://|keys\.openpgp|openpgpkey" edba6448 -- src/main "plugins/*/src/main/*"`
finds nothing in 547 files). With the key list embedded in the host and nothing fetched, a revocation reaches an installation only with a
release (a design consequence of option A, not a measurement).

### 2.2 Options for the signature

**Option A (recommended): a detached OpenPGP signature of the zip, `<zip>.asc` beside it, checked with Bouncy Castle against a
public key embedded in the host.** The zip stays byte for byte what pf4j and #998 handle; the SHA-256 of #998 stays as the cache
key for "unpack again", behind the signature check. The signature is the format Maven Central requires and `gpg --verify`
checks (RFC 9580, section 10.4). The reader code goes into the mystic-crypt library first (library first, as in #642), which
already carries `bcpg` 1.86; the host calls it. The maintainer signs at the release with `gpg --detach-sign`, which is a new
step: what this repository's release ships carries no OpenPGP signature today (the assets of RELEASE-8.15 and RELEASE-8.14 are the
installer and its `.sha256` and `.sha512`, `gh release view`), the one thing he signs for it is the application jar, with
`jarsigner` (`gradle/packaging.gradle:57-89`), and the family's Central artifacts are signed in CI (2.5); the installer packs the
zip and its `.asc`. Consequences: two files to keep together (a zip whose `.asc` is missing counts as unsigned); the embedded
certificate must carry the signing subkey's binding, so a renewed subkey needs the updated certificate in the next host release
(the lifecycle in 2.5); the embedded set is a list of keys so that another key can be added without a format change.

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
   at the release, a new step (today he signs only the application jar, with `jarsigner`; the installer gets checksums, not a
   signature, and the family's Central artifacts are signed in CI), no agent touches the key.
2. A key of its own for plugins: separates the blast radius from Maven Central and lets another maintainer sign plugins, at the
   price of a second offline key and a second renewal.
Reason for 1: fewer secrets for a solo maintainer; the embedded list of keys allows 2 later (NIST SP 800-57: key separation is a
reason for 2 once someone else signs).
Since then, the workflow files of at least 37 public repositories of astrapi69 and lightblueseas were found to pass the signing
secret to CI, which by the release-key record holds the signing subkey (the workflow references are measured, the key inside a
secret is not); 2.5 sets out both options with that and the lifecycle of an embedded key, and recommends differently.

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

### 2.5 The signing key: the release subkey or a plugin subkey

The maintainer's question (2026-11-20, German, translated): the release subkey `10E2BD8D` against a signing subkey of its own for
plugins under the same primary key; and how an embedded key is replaced or revoked. The decision is taken after RELEASE-8.16.
This section extends the first question of 2.3 with what was measured since; it does not change option A.

**Facts** (measured 2026-11-20; the public key is from `keys.openpgp.org`, which is a statement of that source, not of any other
keyring).
- Primary key `5B2FA6B11E298BC09287F423D8C403518C49CA75`: RSA 2048, created 2016-05-01, usage `[SC]`, no expiry, valid (neither
  revoked nor expired). Signing subkey `6D67F8442A6FCC96BD7C1B5E9FCF7C9710E2BD8D`: RSA 4096, created 2026-10-10, expires
  2028-10-09, usage `[S]` only, valid. The primary has an encryption subkey too (`346DAC64...4E31A6E2`, RSA 2048, no expiry).
  Command: `curl https://keys.openpgp.org/vks/v1/by-keyid/9FCF7C9710E2BD8D | gpg --show-keys --with-subkey-fingerprints --with-colons`;
  the validity field is `-` for all three keys.
- Where the subkey signs today: a Central signature carries it. The jar signature of `mystic-crypt` 13.11.1 (published by a
  successful tag run of its `publish.yml`, 2026-11-19, and made inside that run, at 13:43:28 UTC, by Gradle's
  `useInMemoryPgpKeys`, `mystic-crypt` `gradle/publishing.gradle:124`) names issuer key `9FCF7C9710E2BD8D`, RSA 4096, SHA-512
  (`gpg --list-packets` of `mystic-crypt-13.11.1.jar.asc` from `repo1.maven.org`). The signature has the issuer key ID subpacket
  and no issuer fingerprint subpacket.
- Where the key sits: workflow files on `develop` of at least 37 public repositories of astrapi69 and lightblueseas pass a secret
  named `GPG_PRIVATE_KEY` to the build, 42 lines in all (plus `GPG_PASSPHRASE` and the optional `GPG_KEY_ID`, described in
  `mystic-crypt`'s `publish.yml` header as "the signing subkey's last 8 hex digits, when `GPG_PRIVATE_KEY` holds that subkey
  alone"). Read for the family's libraries and applications through the GitHub API,
  `gh api repos/<owner>/<repo>/contents/.github/workflows?ref=develop` and then each file, counting the lines that are not
  comments; not every public repository of the two owners was read, so 37 is a lower bound. 36 of the 37 have a secret of that
  name, a repository secret or, in lightblueseas, an organisation secret (`gh api repos/<owner>/<repo>/actions/secrets` and
  `.../actions/organization-secrets`, which list names, never values); `swing-model-components` names it in `publish.yml:36` and
  has none. Not measured, and not readable from outside: which key each secret holds. That it is the signing subkey is an inference
  from `docs/decisions/release-key-openpgp-2026-10-29.md` (primary offline, the signing subkey "in the CI secrets") and from the one
  signature above.
  This repository has no such workflow, and signs its host jar with a Java keystore through `jarsigner` (`gradle/packaging.gradle:57-89`),
  a different trust root from GPG.
- What this application embeds today: no public key. The release-key plugin only reads the user's GnuPG home and displays
  fingerprint, usage, expiry (`KeyRows.java:108-118`: `never`, a date, or `expired <date>`, compared with today in UTC); it has
  no revocation check and no test or code path for a renewed or replaced key (`git grep -i -E 'revok|renew|extend'
  plugins/release-key-plugin/src` at `edba6448`: 19 files searched, 4 lines hit, all of them the Java keyword `extends`). There is no precedent in the repository for an embedded verification key.
- Revocation, measured: today no main source checks whether a key is revoked or fetches a key (the `git grep` at the end of 2.1
  finds nothing). Design consequence, not a measurement: with an embedded list and no fetch, a revocation reaches an installation
  only with a new release of the application; a new zip does not carry it, since it is checked by the very key in question.

**What a key means in OpenPGP, from RFC 9580** (read at rfc-editor.org/rfc/rfc9580.txt, 2026-11-20). Section 5.2.3.29: the Key
Flags subpacket says whether a key may sign data (flag `0x02`) or certify other keys (`0x01`); the maintainer's primary is `[SC]`,
the subkey `[S]`. Section 5.2.3.13: the key expiration time is a number of seconds after the key's creation time, stored in a
self-signature (for a subkey, in its binding signature), zero or absent means no expiry. Section 5.2.3.31: a revocation carries
a reason code: 1 key superseded, 2 key material compromised, 3 key retired; "If a key has been revoked because of a compromise,
all signatures created by that key are suspect. However, if it was merely superseded or retired, old signatures are still valid."
Section 10.4 describes detached signatures (the `.asc` beside a file).

**What the application accepts if it trusts the primary key instead of pinning one subkey.** It would embed the primary's
certificate with its subkey bindings and accept a signature from any subkey that is bound to the primary, not revoked, and flagged
for signing: the release subkey `10E2BD8D`, a plugin subkey, and a signing subkey made later (a binding signature can be made only
by whoever holds the primary, which is offline; inference from RFC 9580 section 5.2.3.10, self-signatures). That is convenient (a
renewed or new subkey works without a release of the application, as long as the embedded certificate carries its binding, which it
cannot know before it exists, so the new binding still needs a release: inference) and it makes the release subkey a plugin signer,
whatever option is chosen. Pinning a subkey fingerprint accepts exactly that subkey. So option 2 below only separates anything if
the application pins the plugin subkey by its full fingerprint and does not trust the primary.

**Option 1: the release subkey `10E2BD8D` signs the plugin zips.**
- Cost: no new key; a new step at the release, one `gpg --detach-sign` per zip by the maintainer (today he signs only the
  application jar, with `jarsigner`, 2.2). The release workflows that exist are not touched. The embedded certificate is the one
  already on a key server.
- What it changes: the trust root of what runs inside users' JVMs is a key that, by the release-key record, sits in the CI
  secrets, which the workflow files of at least 37 public repositories pass to their builds (measured are the workflow references;
  that the secrets hold this key is the inference of the facts above), and that signs the family's Central artifacts since its
  creation on 2026-10-10 (measured for `mystic-crypt` 13.11.1). A leak of that secret from any one of them (a runner, an action,
  a fork's secret) lets its holder sign a plugin zip that this application accepts, and Maven Central artifacts cannot be told apart from plugin zips by the key alone. The blast radius of the CI
  secret and of the plugin trust are then one (NIST SP 800-57 Part 1 Rev. 5, section 5.2: "Limiting the use of a key limits the
  damage that could be done if the key is compromised").
- Risk: a compromise of the release subkey is a compromise of plugin trust at once. Revoking it for Central (reason 2) changes
  nothing in an installed application, which learns of no revocation (2.1): a leak from CI forces a host release that drops the
  key and ships the zips signed again with another one, and until a user installs that release the application keeps accepting
  zips signed with the leaked key (the lifecycle below).
- The day the key is replaced or revoked: see the lifecycle below, with the coupling that a library release and a plugin release
  must move together.

**Option 2: an own signing subkey for plugins under the same primary.**
- Cost: the maintainer makes a new `[S]` subkey (his act, with the primary offline,
  once), publishes the updated certificate (key server), and keeps the secret part off every CI secret, so only he signs
  and only at the release. A second key to renew, with its own expiry (2.5 lifecycle). The application embeds the subkey's
  fingerprint and the certificate that carries its binding. The workflow files and the Central signing do not change.
- What it changes: nothing persisted; an embedded list of key entries instead of one (2.2 already planned a list); the
  certificate shipped in the host changes at each rotation.
- What it separates: the key in CI cannot sign a plugin; the key that signs plugins is never in a runner. It does not separate
  the primary: someone holding the primary can bind any subkey, which is why the primary stays out of reach (release-key record).
- Risk: one more thing to forget; a plugin subkey that expires without a host release shipped before makes every zip signed after
  the expiry fail (see below). Smaller: the application takes the issuer fingerprint when the signature carries one and falls back
  to the 8-byte key id, then verifies and compares the full fingerprint with its list. `gpg --detach-sign` writes both (GnuPG
  2.4.8, measured with a throwaway key in a temporary GnuPG home: the issuer fingerprint subpacket 33 beside the issuer key ID
  subpacket 16); the signature above with the key id alone comes from Gradle's signer, not from gpg.

**Replacing, retiring and revoking an embedded key (lifecycle).** The embedded set is a list: for each entry a full fingerprint,
the certificate with its binding, and a state.
- Superseded or retired (RFC 9580 reason 1 or 3): a release adds the new key and marks the old one retired with a date. Signatures
  of the old key whose creation time lies before that date stay valid, as the RFC says, so the 14 shipped zips and any zip the user
  installed keep loading; a zip signed with the old key after the date is refused. The creation time is the signer's own claim, which
  is fine for a retired key and is why it is not enough after a compromise.
- Compromised (reason 2): no installed application learns of the revocation. The release that drops the entry re-signs every zip
  it ships with another key and ships them; after the update no signature of the dropped key stays valid ("all signatures created
  by that key are suspect"), so a zip the user installed that carries only that key's signature stops loading. An application that
  is not updated keeps trusting the compromised key, which is the price of having no network path; the release text must say so.
  Under option 1, a leak of the CI secret forces exactly such a host release.
- Expiry and renewal: the check at every start must not be "is the key valid now", or every installed plugin stops loading on the
  expiry date. It is "was the signature made inside the key's validity" (the signature creation time, which RFC 9580 section 5.2.3.11 calls
  "the time the signature was made", against the key creation time plus the expiration time of section 5.2.3.13), which keeps old zips valid and refuses a zip signed
  after the expiry. Extending an expiry makes a new self-signature; an application with the old certificate embedded does not see
  it, so the new certificate has to be in a host release before the first zip is signed after the old date. The release subkey
  expires 2028-10-09 (measured above); that date is the first one this work meets.
- The release that retires or drops a key also changes the embedded list, so it is the host's release, tested with throwaway keys
  (2.4), not with the real one.

**Recommendation (a suggestion): option 2, with the plugin subkey pinned by its full fingerprint, and the list as above.**
Reason: the release subkey is, by the release-key record, the key in the CI secrets that the workflow files of at least 37 public
repositories pass to their builds (measured are the workflow references, not the key inside a secret); a plugin runs with the
user's rights inside the vault's JVM (2.1), so its trust root should be a key that is never in a runner. Option 1 is the cheaper
first step and stays defensible for a solo maintainer; it revises the earlier lean of 2.3 only on that one fact, and 2.3's other
recommendations stand. Best practice: NIST SP 800-57 Part 1 Rev. 5, section 5.2, reason 2 ("Limiting the use of a key limits the
damage that could be done if the key is compromised"), applied by analogy: section 5.2 asks for one cryptographic purpose per key,
and signing Central artifacts and signing plugin zips are one purpose, digital signatures, so the rule itself does not require two
keys here; its reason for limiting the use of a key argues for them. Section 5.3.4 (a signature key's recipient-usage period can
run past its originator-usage period, which is the "signatures before the date stay valid" rule above); RFC 9580, sections
5.2.3.13 and 5.2.3.31.

### 2.6 Development builds: unsigned plugins under `make run`

The maintainer's question (2026-11-20, German, translated): how do unsigned, locally built plugins start under `make run`; a
development mode only in the isolated profile, never in an installed application; how is that enforced? The decision is taken
after RELEASE-8.16.

**How `make run` starts plugins today** (`origin/develop`, `scripts/run-isolated.sh`, `Makefile:101-119`). `make run` is an alias of
`run-isolated`: it builds the plugin zips without tests, builds the all-dependencies jar, and runs `scripts/run-isolated.sh
"$(ISOLATED_HOME)"` (default `$(HOME)/mystic-crypt-ui-dev`). The script refuses a profile that is or contains the real home
(`run-isolated.sh:26-34`), wipes old plugin zips and unpacked directories in the profile's plugins folder, copies each
`plugins/*/build/plugin-dist/*.zip` there (`:42-53`), and ends in `exec java -Duser.home="$isolated" ... -jar "$jar"`
(`:58-59`). Nothing else carries the profile to the application: no environment variable, no property of its own. The host does
not know it runs in an isolated profile (`git grep -E "ISOLATED|mystic-crypt-ui-dev" origin/develop -- src/main/java`, 186 files searched at `e9c7d983`, finds
nothing); the plugin folder comes from the configuration directory, which derives from `user.home` (the library's
`getConfigurationDirectory`; its source was not read, inferred from how the script and the e2e suites redirect it).

**What exists to tell a development build from a release build.** The version is `8.16-SNAPSHOT` in `gradle.properties:4`;
`releaseVersion = !version.endsWith("SNAPSHOT")` (`gradle/packaging.gradle:2`). The jar manifest carries `Implementation-Version`
(`packaging.gradle:26`); at run time only `ApplicationInfo.current()` reads it, falling back to the string `development` when the
code runs from `build/classes` (`ApplicationInfo.java:36,54-57`): a display value, no switch. Existing development switches in
host main code: `mystic.crypt.ui.config.dir` (relocates the configuration directory, `PluginSettings.java:58,77`),
`mystic.crypt.ui.startup.signal`, and for the command line only `mystic.crypt.ui.plugins.dir` (`MysticCryptUiCli.java:62,110`), which
nothing in the Makefile, Gradle files or workflows sets (tests only). The one in-plugin precedent is `-Dmystic.crypt.ui.menu.designer=true`,
which sits in the not-shipped menu-designer plugin (`MenuDesignerMenuContribution.java:60`) and is kept out of the installer, which
the `plugins` check of `verify-installer.sh` confirms (it drops menu-designer from the Makefile's list before comparing it with the
pack).

**What can reach an installed application from outside.** The launchers lay down plain `java ... -jar mystic-crypt-ui.jar` with no
fixed properties and no wrapper (`src/main/resources/doc/start.sh`, `mystic-crypt-ui.desktop`, `start.bat`;
`src/main/izpack/install.xml:69-89`). A system property reaches it by editing the desktop entry or `start.sh` (both writable by the
user), by `JAVA_TOOL_OPTIONS` or `JDK_JAVA_OPTIONS` (general JDK behaviour, not run here), or by `java -D... -jar` by hand. So a
runtime switch alone cannot be "only in the isolated profile": the profile is `user.home`, and `-Duser.home` is one of the properties
that arrive the same way. The installed application also copies a user-chosen zip into the plugins folder
(`PluginsSettingsPanel.java:156`).

**What the check changes.** For `make run`: the copied zips are unsigned, so a fail-closed check (2.3) refuses every one of them (one per plugin, 15 plugins),
including the one the maintainer just built. For the tests, how each one gets its zips is measured (`edba6448`); which of them
would meet the check is an inference, since the check does not exist yet and 2.3 places it in the host's plugin manager.
- Through a load path of the host, so they would meet it: 59 of 155 `src/e2eTest` classes plus one `src/e2eLock` class copy a
  built zip into the plugins folder before the application starts, through one helper,
  `AbstractUiTest.installPluginRequiringItBuilt` (`src/e2eSupport`, `AbstractUiTest.java:408-418`), from 15 zip constants
  (`:77-115`), with `user.home` redirected to a temp directory per test (`:139-141`); `PluginLoadingUiTest` copies five built zips
  the same way on its own (`PluginLoadingUiTest.java:155-159`), and `PluginsUsableWhileLockedHoldTheLockUiTest` (`src/e2eLock`)
  every shipped one in a loop of its own (`:217-243`); the command line tests write a synthetic zip and run it through
  `MysticCryptUiCli`, whose own `DefaultPluginManager` (`MysticCryptUiCli.java:166`) needs the check as much as the window's
  (`MysticCryptApplicationFrame.java:297-299`): `CommandLineWithInstalledPluginsTest` (`src/test`, in the test's JVM) and
  `CommandLineStandardErrorTest` (`src/e2eTest`, `:299-317`, in a process of its own).
- Not through it: the two `zipTest` suites (lethenon, release-key) start a real zip in a plain pf4j manager of their own
  (`new DefaultPluginManager(plugins)`, `LethenonPluginFromItsZipTest.java:104`, `ReleaseKeyPluginFromItsZipTest.java:91`), so
  they need the host's manager or a check of their own; `BuiltArchivesNameNoBuildMachineTest` only reads the manifests out of the
  zips (`collectManifests`, `:240`) and loads nothing, so it needs none. `PluginArchivesTest` calls `PluginArchives` directly
  (`pluginIdOf`, `unpackIfChanged`, `isUnpackedFrom`) and `PluginZipInstallationTest` runs `PluginZipInstallation.decide` over a
  plain `DefaultPluginManager` (`:70`), so whether these two meet it depends on where the check is placed.

With the check, the tests sign with throwaway keys made at run time and hand the trust set to the host through the injectable set
of 2.4; they never use the real key.

**What a development build accepts (a sub-question):** (i) unsigned zips, or (ii) zips signed by a throwaway key that `make run`
makes for the isolated profile and adds to the trust set. With (ii) the check has one code path in every build and an unsigned zip
is never loaded; only the trust set differs. With (i) the unsigned path exists in the code. Either needs a binding to the
development build, which is the numbered question:

**Options for the binding.**
1. **A runtime switch, honoured when the profile is the isolated one** (a property, plus a check of the profile's path or a marker
   file in it). Cost: small. Does not hold: the property, the path and a marker file all come from the command line or the user's
   directory, which is what an installed application's launchers and `JAVA_TOOL_OPTIONS` give to anyone who controls them (above);
   the host cannot tell an isolated profile from an installed one with a file in the profile. It is a courtesy, not a boundary.
2. **A runtime version gate:** the mode is honoured only when `Implementation-Version` ends with `-SNAPSHOT` or the code runs from
   `build/classes` (no manifest, which is how the e2e suites run). Cost: small; the check sits in the host (one class). What it
   changes: a release build refuses the switch whatever the command line says. Risk: the code is in the release jar, inert; it
   depends on the version string being right, which nothing refuses today (no check for `SNAPSHOT` in `verify-installer.sh`, the
   Makefile or the workflows except `packaging.gradle:2` and `publishing.gradle:2`, measured with `git grep -n -i snapshot` at `a80d9c54`:
   1412 files searched, 44 with hits, those in the Makefile and the workflows are comments).
3. **A build-time binding: the development code does not exist in a release jar.** The switch and the extra trust entry live in
   a class (or source set) that the host jar of a release version does not contain, the way the menu designer is not shipped
   (architecture.md, the `plugins` check of `verify-installer.sh`); the installer pack check would refuse a jar that contains it. Cost: the largest,
   a second source set or build variant and one more check. What it changes: nothing at run time in a release; the development
   build and the release differ in bytes, which `make verify-installer` can state.

**Recommendation (a suggestion): 3 for the binding, with the sub-question answered by (ii), and 2's version string as the
condition that decides which jar is a release.** Reason: neither the profile nor a property is a boundary on a machine where the
launchers pass `-D` freely, so the boundary has to be the artifact: a release jar that has no such code cannot be talked into the
mode. Option 2 alone is the cheap form and is acceptable if the maintainer prefers; it should not be taken for what option 3 gives.
(ii) keeps one code path and means the real key is never involved in development. Enforcement, as checks that fail the release
and not as promises: (a) `make verify-installer` gains two checks: the jar's `Implementation-Version` has no `SNAPSHOT`, and the
development class is absent (`unzip -l` of the extracted application jar); it already checks that the version equals
`gradle.properties` and runs `jarsigner -verify` (the `application jar` and `signature` checks of `verify-installer.sh`); (b) a unit test that builds the trust set the way
a release host does and proves an unsigned zip, a zip signed by an unknown throwaway key and the development key are all refused
when the version is a release version, and that the development key is accepted only when the development build says so; (c) the
e2e and `zipTest` suites run with throwaway keys they create, so no test depends on the development mode being on. Best practice:
Mozilla's add-on signing, where release and beta builds require signed add-ons and an unsigned one may be installed only in
Developer Edition, Nightly and ESR builds, after toggling `xpinstall.signatures.required` (extensionworkshop.com, "Signing and
distribution overview", read 2026-11-20; the switch is a property of the build channel, not of a profile); and Android, where the
build tools sign a debug build with a debug certificate generated on the developer's machine, "insecure by design", which the
Play Store does not accept for publishing (developer.android.com, "Sign your app", read 2026-11-20): a different key for development,
never the release key, and a store-side refusal of the debug one.

### 2.7 An allow-list of plugins inside the vault (the maintainer's idea, 2026-11-20)

The maintainer's idea (2026-11-20, German, translated), put to the decision after RELEASE-8.16 as one more option. Facts in this
section are measured on `develop` at `d40879c5` (2026-11-20) unless a line says otherwise; pf4j lines are read from the 3.16.1
sources jar, whose binary jar has the SHA-256 of the one in the Gradle cache. Nothing was run in the application, and a statement
that rests on reading says so. Nothing of this is built.

**1. The option as he stated it.** A list of approved plugins (plugin id, version, SHA-256 of the zip) is kept in the vault
itself, protected by the vault's encryption. Before the sign-in only the core and signed plugins start. After the sign-in a
further plugin starts only if it is in the list of the opened vault. An unknown or changed plugin raises a warning ("Did you
install this plugin yourself? It may come from an attacker."), with No as the default. "Install from Zip" writes the entry into
the list. He asked for these points to be worked out: the interplay with the signature check, which plugins must run before the
sign-in, updates (no question for signed plugins), several vaults, the change to the vault format (an architecture decision, and
its relation to the 9.0 format), the threat model, and why a list in the configuration directory does nothing against a program
with the user's rights.

**2. The interplay with the signature check (2.2 to 2.6) and the verifier states.** The list is not a second verifier. It is the
override path of the state "unsigned" in the table of the verifier proposal, kept under the vault's encryption, where 2.3 point 2
keeps it in a file of the plugin settings. It is more than a new place for the same consent, though: the idea adds an approval at
the start that 2.3 does not have. 2.3 point 1 does not load an unsigned zip at a start (fail closed), and 2.3 point 2 asks only at
Install from Zip, as the unsigned row of the verifier table under "Existing solutions" says too; with the idea, an unsigned zip
that a start finds and the list does not name gets the warning after the sign-in, and a Yes approves it there. A replaced zip (T2,
point 7) comes in by that second route. The states as the idea gives them:

| state of the zip | before the sign-in | after the sign-in |
|---|---|---|
| signed, key in the embedded list, signature verifies | runs | runs; no list entry is needed or written |
| unsigned, listed in the opened vault, SHA-256 equal to the entry | does not run | runs |
| unsigned, not listed, or listed with another SHA-256 | does not run | not started; the warning, default No; Yes writes the entry (a new hash replaces the old one) |
| modified or invalid (signature does not verify, key unknown, outside the key's validity) | refused | refused; no list entry overrides it, as in Grafana, where only the unsigned state has an override |

This keeps the two rules of the verifier table: modified and invalid are never overridden, and only unsigned has an override.
The variant that matches 2.3: an unsigned zip that the list does not name, or names with another SHA-256, is not started at a
start and is named in Settings > Plugins with the reason, as 2.3 point 1 does, and an entry is written only through Install from
Zip, after the consent dialog of 2.3 point 2. A start then asks nothing, so a replaced zip meets no question that a wrong Yes could
answer; the price is that an unsigned plugin updated by copying its zip over the old one stays off until it goes through Install
from Zip. Which of the two the list follows is the maintainer's choice.

The check order at a start becomes: signature first (no vault needed), then, for what is unsigned, the list. The development mode
of 2.6 has to stay outside the list. A locally built zip gets a new SHA-256 whenever its content changes: the archives are
reproducible (#991; `gradle/reproducible-archives.gradle:5-8` fixes entry times and order for every archive task,
`git grep -l reproducible-archives.gradle d40879c5 -- '*.gradle'` finds it applied in the 15 plugin builds among 49 build files, and
two clean builds of one commit gave byte-equal zips for all 15 plugins, commit `d8237a8c`). So an unsigned plugin checked against
a list would ask after every change the developer makes to it, and the default answer would be No. With the recommendation of
2.6 (the zips are signed by a throwaway development key that `make run` adds to the trust set) the plugins are "signed" and never
reach the list; with its sub-question answered by unsigned zips accepted in a development build, the check returns before the
list is read. Either way the e2e suites sign with throwaway keys (2.4) and need no vault list.
The warning belongs to the installed application only.

**3. Which plugins must run before the sign-in.** None of the 15 plugins needs to. Measured: no plugin code runs before the
sign-in dialog closes today: `MysticCryptApplicationFrame.java:299` creates the `DefaultPluginManager` and loads nothing, the
dialog is shown in `onBeforeInitializeComponents` (`:334-339`), and `unpackChangedPluginArchives`, `loadPlugins`, `startPlugins`
follow in `onAfterInitialize` (`:380-382`). The host has four extension points (`PluginMenuContribution`,
`PluginSettingsContribution`, `VaultAccessContribution`, `PluginCommandContribution`) and none is for a sign-in method, a key
source, a reader or the look and feel; the sign-in dialog, key files, KeePass import and the look and feel are host code
(`git grep -n -E 'ServiceLoader|Class\.forName' d40879c5 -- src/main` over 324 files, 186 of them Java: no hit), and no host main
class names a shipped plugin class (`git grep -n -E 'mystic\.crypt\.plugin\.[a-z-]+' d40879c5 -- src/main/java` over 186 files:
24 lines, all of them the host's own packages `plugin.api` and `plugin.vault`, none a package of the 15 plugins). The 15 plugins
implement 30 extensions (15 menu, 14 settings, 1 vault access, 0 command), and for each of them the answer to "needed before the
sign-in" is no (`git grep -n '@Extension' d40879c5 -- 'plugins/*/src/main/java/*'` over the 247 Java files of the plugins' main
sources: 30 hits). Two plugins, checksum and password-hash, declare `isUsableWithoutAVault()`; they are used from the menu when a
vault is locked, which is after the dialog. So "before the sign-in only the core and signed plugins" costs nothing today: it
describes the order that already holds, and the only thing it adds is a rule for later: an extension point that is used before the
sign-in (a sign-in method, a key source) may be filled by a signed plugin only. An unsigned plugin that contributes something
before the sign-in would not be started; it is shown as "not started, signed plugins only before the sign-in" and cannot be
approved from the list, because the list is unreadable until the vault is open. The cancelled sign-in is the case that changes
behaviour: `onAfterInitialize` has no `isSignedIn` guard and the cancel handler does not exit
(`MysticCryptApplicationFrame.java:368-407`, `MasterPwWithApplicationFilePanel.java:789-798`), so plugins start with no vault open
today; the test `aSignInAfterACancelledStartOffersThePluginEntries` of `EverySignInOffersThePluginEntriesUiTest` (`:134-161`)
relies on plugin entries existing after a cancelled start. With the idea, only signed plugins would start there, and an unsigned
plugin would start when a vault is opened later, which needs the start of a plugin after the sign-in (point 5 below). The 14
shipped plugins are signed under 2.4; the menu designer, not shipped, is unsigned and for the development profile (2.6). Plugin
start-up code that runs: `Plugin.start()` of every plugin only logs, except lethenon, which registers the SQLite driver with
`Class.forName("org.sqlite.JDBC")` (`LethenonPlugin.java:61-86`), and that is a reason the order matters: an unlisted plugin must
not get as far as `start()`.

**4. Updates.** A signed plugin updates with no question: the new zip carries a signature of a key in the list, and no entry is
read or written. An unsigned plugin changes its SHA-256 with every change of its content (point 2), so after a file replacement
with a changed zip it is unknown to the list and the warning appears at the next start with the hash it now has (in the variant
of point 2 it is not started and is named in Settings > Plugins); after "Install from Zip" the host writes the new entry itself
after the consent dialog of 2.3. `PluginZipInstallation.decide` already tells "update of a loaded plugin" from "new plugin"
(`PluginZipInstallation.java:91-120`), and an update copies the zip and runs from the next start (`PluginsSettingsPanel.java:223-247`),
so the entry is written at the install, from the zip that was copied, and the next start finds the hash equal. What happens to
the entry when the version changes: the version cannot be the key. All 15 `plugin.properties` carry `plugin.version=1.0.0`, the
lethenon plugin carried 1.0.0 in RELEASE-8.14, RELEASE-8.15 and on develop with 29 commits to its main sources between the first
and develop (`git log --oneline RELEASE-8.14..d40879c5 -- plugins/lethenon-plugin/src/main | wc -l`), and #998 measured 8 of 14 zips
differing between the 8.14 and 8.15 candidate installer. So the entry is keyed by plugin id, the SHA-256 decides, and the version
is shown to the person in the warning and kept in the entry as information; an entry for the same id with another hash is
replaced, not added, so the list does not keep stale hashes of a plugin the user has updated, and an entry for a plugin that is
no longer installed is dropped when the person opens Settings > Plugins (an editing path the list needs; none is designed here).
A hash of the zip does not cover T3: #998's record is written from the zip and the unpacked directory is never hashed
(`PluginArchives.java:132-171`, `:252-257`, `:335-346`), so a directory changed after unpacking is loaded (inference from those
lines, not run). The entry would need the directory hash of 2.3 as a second field; that hash, stored in the vault, cannot be
rewritten by a process without the vault key, which a hash in a file next to the directory can. An inference, since nothing is
built: the host could instead compare the unpacked directory with the zip it came from at each start, which needs no stored hash
at all; with that check, a directory hash in the vault would add nothing under T3.

**5. Several vaults.** Each vault has its own list, because the list is a part of the vault's content. What pf4j allows and what
the host does decide how far that goes in one session. Measured: the host starts all plugins once, in `onAfterInitialize`
(`MysticCryptApplicationFrame.java:382`), and single plugins from Settings > Plugins on Enable (`PluginsSettingsPanel.java:110-121`,
`enablePlugin` then `startPlugin`) and after a new install (`:249-258`);
`git grep -n -E 'startPlugin\(|startPlugins\(' d40879c5 -- src/main/java` over 186 files has these three hits and the CLI's own
start (`MysticCryptUiCli.java:171`). Enable is a start path the list has to gate too; a gate in `isPluginValid` would cover it,
because `enablePlugin` consults it (`AbstractPluginManager.java:661`) and `startPlugin` enables a disabled plugin through
`enablePlugin` (`:419-421`; read, not run). Opening another database closes the open vault, shows the sign-in dialog and enables
the menu, and does nothing to the plugins (`OpenExistingDatabaseAction.java:74-88`);
`git grep -n -E 'unloadPlugin|deletePlugin|unloadPlugins|stopPlugins\(|getPluginClassLoader|PluginStateListener' d40879c5 -- src plugins`
over 1279 files, 1029 of them Java, hits the CLI (`MysticCryptUiCli.java:179`), two `zipTest` classes and a comment in
`stopPluginsQuietly` (`MysticCryptApplicationFrame.java:954`), which stops every started plugin when the application ends
(`:906`, `:948-969`) and unloads none. pf4j can do it (read, not run):
`unloadPlugin` stops the plugin and its dependents, removes it and closes its class loader (`AbstractPluginManager.java:272-352`);
`loadPlugins` runs no plugin code and a start can be selective, and `isPluginValid` is the hook that sets DISABLED at load
(`:912`, `:1037`; inference that no plugin class is loaded by `loadPlugins`). What pf4j cannot do is make the plugin's code go
away: extension objects live on in the host's menu items and open windows, `PluginVaultConnections` holds the vault contributions
and has no disconnect (`PluginVaultConnections.java:60-136`), `connectVaultPlugins` is called once at the start
(`MysticCryptApplicationFrame.java:383`, `:821`), and the lethenon plugin registers a JDBC driver in the JVM-wide `DriverManager`
and keeps a static `LethenonVault` (inference for the driver; not measured: whether a class loader is collected after an unload).
So there are two readings, and they differ in cost:
- the list of the first vault of the session decides what runs, and a vault opened later can only add a start of a plugin that
  is not yet running (a connect step for the late plugin, which `loadAndStart` does not do today, `PluginsSettingsPanel.java:249-258`,
  and a menu refresh, which exists). A plugin allowed by vault A stays running while vault B is open, so vault B's list does not
  bind. Cheap, and honest in its limit;
- the list of the open vault decides at every sign-in: the host stops and unloads the plugins the previous vault allowed,
  disconnects their vault contributions, loads and starts what the next vault allows, connects them, and rebuilds the menu.
  Real work in the host, and it cannot remove a registered driver or a static, so it is not a boundary either (inference).
Recommended reading, if the list is built: the first; the session's trust is the union of what was approved at the first
sign-in and what the person approves later, and the dialog says so. The CLI signs in to no vault: `MysticCryptUiCli.java:164-188`
creates its own `DefaultPluginManager`, unpacks, loads and starts all plugins and collects command contributions, and no shipped
plugin contributes a command (`PluginCommandContribution` has implementations only in `src/test` and an example in
`docs/plugin-development.md`; 1487 files searched). A vault-held list cannot be read there, so the CLI is protected by the
signature or not at all: unsigned plugins must not start under `--cli`, which also means a plugin that a person installed unsigned
never runs from the command line.

**6. The change to the vault format.** A list in the vault is a change to the vault's content, so it is an architecture decision
that needs the maintainer's yes first. `architecture.md` says: "Changing an architectural decision (e.g. vault format, plugin
mechanism, XML persistence) requires asking first", and "Never build parallel systems already slated for replacement"; the idea
touches two of the three examples, the vault format and the plugin mechanism, and the second sentence bears on the first. Where it
could sit, measured:
- Not in the MCRDB2 header: the plaintext is 26 bytes (marker, salt, iteration count), all of it associated data, and
  `docs/vault-format.md` says a further header field needs a new marker (`PasswordVaultFormat.java:41-80`,
  `docs/vault-format.md:45-59`, `:202-203`). A plaintext list would also be readable before the sign-in.
- Not in the two key-file variants: they have no marker and no header and are not versioned (`docs/vault-format.md:278-349`), so
  for a vault of that kind anything new goes into the XML content or nowhere.
- (a) As a marked entry with a host-owned marker, through the mechanism of `plugin-vault-access-2026-10-31.md` (a custom property
  in the namespace of its owner). No format change, readable by every build from 8.5. It is visible and editable as an ordinary
  entry in the tree, an older build can change or delete it, and a plugin could read and change it, and save the vault without
  the read-only check, through the host's `getInstance()` (the same record, `plugin-vault-access-2026-10-31.md:51-53`; #678 is
  open).
- (b) As a new element in `ApplicationModelBean` (XStream). It needs `FORMAT_VERSION` 3. Without the bump, 8.6 to 8.16 skip the
  unknown element (`VaultXmlCodec.java:256`, `ignoreUnknownElements`) and their next save drops it from the file, and 8.5.x
  refuses the vault and reports a false wrong password (`vault-format-version-2026-09-17.md:20-26`). With the bump, every build
  from 8.6 on opens such a vault read-only, with Save, Save As and every change refused (`VaultXmlCodec.java:136-168`,
  `ANewerFormatOpensReadOnlyUiTest`). That protects the list, and it keeps a person who runs two versions from changing or saving
  the vault in the older one, which still shows it (`vault-format-version-2026-09-17.md:13-16`, `:30-33`). It adds a field to a
  format that the 9.0 record already slates for freezing ("nothing new is ever written in the XStream format"), and the round trip
  through the real app, including the legacy path, is mandatory (`quality-checks.md`).
- (c) In the 9.0 content format (`vault-content-format-9.0-2026-11-10.md`, accepted, nothing built): every entity, the vault
  included, carries one optional `extensions` object "the host never interprets", with plugin data in namespaced keys
  (`:75-77`); a newer minor version's additions land in `extensions` or in new optional fields, which an older 9.x reader keeps
  (`:87-88`), and a newer minor opens read-only (`:94-96`). A list the host reads and enforces is host data, so it does not fit
  `extensions` as the record defines it. It needs a dedicated optional field in the vault root, a schema addition under the
  record's minor rule (`:87-88`) and an edit to #865's model list, or a change to the record's rule that the host never interprets
  `extensions` (`:75-76`), which the maintainer decides separately. A host key among the plugins' namespaces would also need a rule
  that keeps plugins from writing it (an inference; nothing of 9.0 is built). Taken into 9.0, either adds no break of its own,
  because 9.0 is a break already. Issues #862, #864, #865 and #866 are all open, and none names plugins or a vault-level setting;
  the model list of #865 ("one optional `extensions` object per entity") has no field for it, so taking it in is an edit to the
  scope of #865, the maintainer's decision.
  Cost: nothing exists before 9.0, and released 7.x and 8.x cannot read a 9.0 vault, so the list protects nothing until 9.0 ships.
  A legacy vault has no list: the first save in 9.0 starts it empty (every unsigned plugin asks once) or seeds it from the installed
  plugins (trust on first use; an attacker's zip that is already there becomes trusted); which of the two is a decision that
  belongs with #866. KDBX 4 is a precedent for the place: its database-level `CustomData` dictionary is serialized in the encrypted
  XML, and the format page says "it is highly recommended that plugins store additional data in the CustomData dictionary of
  PwDatabase" (keepass.info/help/kb/kdbx_4.html, read 2026-11-20).
Recommendation within the format question: (c). The list is consent state, not a feature that must ship with the signature check,
so it can wait for the format that can take it as a field of its own; (b) spends a format bump on a format that is about to be
replaced, and (a) puts a security record where the user and an older build can edit it.

**7. The threat model.** The assumption of 2.1 stands: a plugin runs with the user's rights in the vault's JVM. What the list
defends against, fitted to T1 to T4 and to what 2.1 leaves out. D is defended, P partly, N not.

| threat | list | reason |
|---|---|---|
| T1, a zip chosen at Install from Zip that is changed or foreign | P | the install is where the person answers the dialog, and the list is only where the answer is kept; a person who says Yes defeats it. The signature refuses what it does not know; the list asks |
| T2, a zip replaced in the plugins directory between starts by a process that cannot run code in the application | P, for unsigned plugins (D only while the person answers No) | the process cannot forge the entry, since it cannot decrypt the list, and the host finds the hash unknown after the sign-in; but the host then asks instead of refusing, and a replaced zip of a plugin the person did install is the case where "Did you install this plugin yourself?" invites a wrong Yes (the version the warning shows does not change on a swap: all 15 plugins carry 1.0.0, point 4). In the variant of point 2 the swapped zip is not started and nothing is asked: D. A replaced zip whose signature does not verify is refused without a question (modified or invalid, no override) |
| T3, the unpacked directory changed after unpacking | P | only if the entry carries a directory hash the host computed at unpacking; a hash of the zip, as stated in the idea, does not see it (point 4) |
| T4, the 14 installer-laid zips | not the list's business | they are signed (2.4); no entry exists for them |
| a process that runs inside the JVM or replaces the host jar, the launcher or the Java found by `start.sh`, or injects an agent | N | it can ignore the list, or read the decrypted list and the master password; see point 8 |
| a key logger, a screen or clipboard reader, a memory reader of the same user | N | it reads the password as typed and the plain data after the unlock; the list is irrelevant |
| a user who answers Yes | N | default No lowers the rate and does not change the outcome (2.1) |
| a plugin that is signed and malicious | N | signed plugins need no entry; the list cannot say anything about intent (out of scope in 2.1) |
| a loaded plugin (signed, or unsigned and approved) | N | it reaches the vault model through `getInstance()` (#678) and can add entries for other plugins, in every placement (for (c) an inference, as nothing of 9.0 is built) |
| rollback: an older copy of the vault file with an older list | N | the file is replaceable with the user's rights; an old list is a valid list (inference; AEAD proves a file is whole, not that it is the newest) |
| a vault that was made before the list, or imported | P | the list is empty or seeded (point 6 (c)); seeded means trust on first use |
| the CLI | N | no vault is opened (point 5) |

**8. A list in the configuration directory, and where the vault's list stops too.** The configuration directory is
`~/.config/mystic-crypt-ui` on every OS (the code hard-codes `.config`, `PluginSettings.java:58-83`) and the plugins directory is
its child, where the installer also lays the zips (`install.xml:127-135`). A list kept there is a file next to the zip. A process
that can put a zip there can write the list with it: it is the same directory, the same rights and one more file, so such a list
protects against an accident (a sync tool, a download that added a zip) and not against a program. It is the limit 2.3 already
names for the consent record, and pf4j has the same list already: `enabled.txt` and `disabled.txt` in the plugins directory
(`DefaultPluginStatusProvider.java:43-60`), which say what the user chose and prove nothing. The list inside the vault cannot be
written without the vault key. That is the gain, and it is exactly as large as the set of attackers that can write files and cannot
run code inside the application. Where it stops, measured on the maintainer's Linux install:
- A replaced application jar. `start.sh` runs `java -jar $DIR/mystic-crypt-ui.jar`; the jar is jarsigned and nothing at start
  checks that (2.1, an inference the record marks). On a Linux install without root, IzPack's default path is under the home
  directory (`AbstractInstallDataProvider.getDir()` of izpack 5.2.7: `/usr/local/` only if writable, else `user.home`), and the install
  directory of the maintainer's machine is owned by the user. In Program Files or `/Applications` ownership is not measured.
- The launcher and the Java it picks: `start.sh`, the `.desktop` copy in `~/.local/share/applications` and the file
  `installer-java-home` are writable by the user on a home install (`start.sh:9-15, 32-55`, `install.xml:82-84, 92-103`).
- An agent through the environment: `JAVA_TOOL_OPTIONS="-Dx.y=1" java -version` printed "Picked up JAVA_TOOL_OPTIONS: -Dx.y=1" on
  JDK 25.0.5 (run 2026-11-20), and `JDK_JAVA_OPTIONS` likewise; a throwaway agent given as `-javaagent:` in either variable ran
  its `premain` before `main` under `java -jar` (JDK 25.0.5, 2026-11-20).
Precedent, each with its source, all read on 2026-11-20:
- KeePass: the page on security issues says "An attacker who has write access to the KeePass configuration file can modify it
  maliciously", that "an attacker can for instance simply replace the "KeePass.exe" file by some malware", and that "KeePass cannot
  magically run securely in an insecure environment" (keepass.info/help/kb/sec_issues.html). Its answer is the permission of the
  install directory, which is the line this section ends at. KeePass 2 loads plugins from its application directory and the plugin
  page names no signature or hash check (keepass.info/help/v2/plugins.html; read by an earlier stage of this work); it does not tie
  plugins to a database. KDBX 4's `CustomData` is the precedent for keeping such a record in the encrypted part (point 6).
- Firefox: add-ons "need to be signed before they can be installed into release and beta versions of Firefox", and unsigned ones
  can be installed only in Developer Edition, Nightly and ESR after toggling `xpinstall.signatures.required`
  (extensionworkshop.com/documentation/publish/signing-and-distribution-overview/); signing was announced on February 10, 2015
  (blog.mozilla.org/addons/2015/02/10/extension-signing-safer-experience/, whose update note says that details of the plan, the
  timeline in particular, changed after it was published). Sideloading, where an installer drops an extension into a special
  location, was to stop being supported in Firefox 74, "which will be released on March 10, 2020", and the post gives the purpose:
  "To give users more control over their extensions, support for sideloaded extensions will be discontinued." It adds that the
  mechanism "has also been employed in the past to install malware into Firefox"
  (blog.mozilla.org/addons/2019/10/31/firefox-to-discontinue-sideloaded-extensions/). The answer there was a signature and a refusal
  of the drop-in route, not a list in the profile; that the signature is anchored in the program is an inference, neither page says
  so. I found no Mozilla text that says a profile list was tried and failed.
- Chrome: other programs may install extensions through external-extension files or the Windows registry; Windows and macOS users
  are asked to enable them, Windows takes no local file path, and a blocklisted one is not installed again
  (developer.chrome.com/docs/extensions/how-to/distribute/install-extensions). That is a prompt and a source restriction, with no
  user-held list as the guard. I did not read the enterprise policy pages.
- No password manager was found that gates plugin loading by a list kept in its database (searches by an earlier stage, not
  exhaustive).

**9. Options for the decision after RELEASE-8.16.**

1. **Signatures with the allow-list in the vault as the consent store** (the idea, on top of 2.2 to 2.6). The signature check
   stays and is what protects the first start, the CLI and the shipped zips; the list takes the place of the consent record of
   2.3 point 2 for unsigned and self-built plugins, with the entry written at Install from Zip and the warning (default No) for an
   unknown or changed one. Placement: the 9.0 format (point 6 (c)), as a new optional field of the vault root, or in `extensions`
   once the maintainer changes the 9.0 record's rule that the host never interprets it; until 9.0 the consent record of 2.3 stays
   in the plugin settings. Consequences: a schema addition to the 9.0 content (or that change of the rule), which waits for #865
   and #866 and for an edit to #865's scope; a connect step for plugins started after the sign-in (`connectVaultPlugins` runs
   once, at the start); a rule for the cancelled sign-in and for a second vault (point 5); an editing path for entries; the CLI
   unchanged. Gain over option 2: under T2, and under T3 only if the entry carries the directory hash (open point below), an
   unsigned plugin's consent cannot be forged by a process that has no vault key.
2. **Signatures alone, as 2.2 to 2.6 recommend.** Option A, with the consent of 2.3 point 2 recorded by SHA-256 in the plugin
   settings. Consequences: no change to the vault, no multi-vault question, one code path for the window and the CLI; the consent
   record of an unsigned plugin lies in the writable directory and protects against T1 only (2.3). For a person who installs no
   unsigned plugin the options differ only under T2: with option 2 a process can write a consent record for its own unsigned zip
   and the zip starts; with option 1 it cannot, and the host asks (default No), or in the variant of point 2 does not start it. In
   daily use the person sees no difference.
3. **A list in the configuration directory.** Shown to say why not: it adds a file that a process able to write the zip can write
   as well (point 8), it is the consent record of option 2 under another name, and pf4j's `disabled.txt` already is such a list.
   Not recommended.

Recommendation (a suggestion): option 2 first, and option 1 as its later home when the 9.0 format has a field for it, not as a
format change of MCRDB2 now. Reason: the signature does what the list cannot (it refuses without a question, covers the CLI and holds
without a vault), while the list adds protection only for unsigned plugins and only against an attacker who writes files and
does not run code, at the cost of a format decision, a late-start path and a multi-vault rule. Nothing in the 15 plugins needs it
before the sign-in, so deferring loses nothing today. Best practice: keep a plugin's data in the part of the database that is
encrypted, as KDBX 4 recommends (keepass.info/help/kb/kdbx_4.html), and do not expect a user-writable file or an in-process check to
hold against a process that runs with the user's rights, which is KeePass's own limit statement (keepass.info/help/kb/sec_issues.html);
release channels that need signed add-ons and refuse a drop-in route are the Firefox precedent for the signature.
Open points inside option 1, for the maintainer: seed an empty list from the installed plugins or start it empty at migration;
whether the list is read once per session or at every sign-in (point 5); whether it carries the directory hash (T3); whether an
unsigned zip that a start finds and the list does not name gets the warning, as in the idea, or stays off until Install from Zip,
the variant of point 2.

## Existing solutions

The maintainer's request (2026-11-20, German, translated): before anything is built, look at what exists, with measurement
instead of assumption, and cut the verifier so that it could satisfy the `FileVerifier` interface of pf4j-update without
depending on it. Measured, or read where a line says so, on 2026-11-20 with throwaway material made at run time (a throwaway zip, throwaway keys and
certificates, throwaway plugin directories); the scratch programs and their output are kept outside the repository, like the
other proofs of this record, and each statement below carries the command or the source that showed it. Where a statement
comes from a search, it names the number of files searched next to the number of hits; a search of 0 files would be an error,
not a result. Source citations name the tag or commit read. Nothing here is code for `develop`: all of it waits for
RELEASE-8.16.

### pf4j 3.16.1 itself

What it offers: `plugin.requires` in the plugin descriptor (`PropertiesPluginDescriptorFinder.java:47`, default `*` in
`DefaultPluginDescriptor.java:31`), `AbstractPluginManager.setSystemVersion`, and a `VersionManager` whose default
implementation compares with the java-semver library (`DefaultVersionManager.java:41-43`). pf4j-3.16.1-sources.jar from Maven
Central, confirmed with `javap -c -p` on the jar, which is byte-identical (SHA-1 `45e100467c89d70460052b611af95392317aedfc`)
to the copy in the Gradle cache of this build.

Measured with a scratch program over a `DefaultPluginManager` on JDK 25.0.5, two unpacked plugin directories holding only a
`plugin.properties`, one with `plugin.requires=>=1.0.0 & <2.0.0` (fits) and one with `>=2.0.0` (too new):

| system version set | fitting plugin | plugin that requires too much |
|---|---|---|
| `setSystemVersion("1.0.0")` | RESOLVED, then STARTED | **DISABLED**, `failedException` is `PluginRuntimeException: Plugin validation failed`; `getPlugin(id)` still finds it; it is not in `getStartedPlugins()`; `enablePlugin(id)` returns false |
| not called (default `0.0.0`) | STARTED | RESOLVED, STARTED, listed as started, `enablePlugin` returns true: **`requires` is not checked at all** |

So the check holds, but a plugin whose `requires` does not fit is DISABLED, not rejected and not absent. The cause is in the
code: `isPluginValid` returns true when the system version equals `0.0.0` (`AbstractPluginManager.java:918`), and
`loadPluginFromPath` sets DISABLED and the failed exception when it returns false (`:1037-1041`); `enablePlugin` repeats the check
(`:661`). Consequences for the host, which are part of the recommendation of 1.3: it calls `setSystemVersion(<API version>)`;
it checks the state of every plugin after loading, not `getPlugin(id) == null`; it shows a disabled plugin with its reason in
Settings > Plugins. The reason cannot be taken from pf4j, whose message is the same sentence for every invalid plugin, so the
host composes it from the descriptor's `requires` and its own API version. The enable action must say why it refuses. The test
for this asserts the state (DISABLED, with the failed exception), the row in the plugin settings (state and reason) and that the
plugin's menu is absent: the third alone would pass for a plugin that failed for any other reason. Not measured: the display
itself (it does not exist yet). Measured with a throwaway probe on JDK 25.0.5, pf4j 3.16.1 and java-semver 0.10.2, through
`DefaultVersionManager.checkVersionConstraint` and `Version.satisfies` with the same result: `^1.0.0`, `>=1.0.0 & <2.0.0` and `1.x`
accept the versions 1.0.0 and 1.2.0 and refuse 2.0.0 and 0.0.0 (the constraint itself; `isPluginValid` lets `0.0.0` through before
it asks).

The API module after pf4j's own demo (tag `release-3.16.1`, commit `6ffa261f0e28f88a9115432482cad64979715a0d`): the Gradle demo
has an `api` module with one interface (`Greeting extends ExtensionPoint`, `demo/gradle/api/src/main/java/org/pf4j/demo/api/Greeting.java:23`),
the application depends on it with `implementation project(':api')` (`demo/gradle/app/build.gradle:9`), and each plugin with
`compileOnly project(':api')` under the comment "compileOnly important!!! We do not want to put the api into the zip file since the
main program has it already!" (`demo/gradle/plugins/plugin1/build.gradle:2-3`). The plugin zip holds `classes/` and `lib/`
(`demo/gradle/plugins/build.gradle:12-21`), the shape of ours. The demo does not show `requires` or `setSystemVersion`: of its 59
files, 1 line matches `requires` (a comment in `gradlew`) and 0 match `systemVersion` (`grep -rn -i requires demo`, `grep -rniE
systemVersion demo`). The version pattern therefore comes from pf4j's code, not from the demo.

Take over: the separate API module with `compileOnly` in the plugins (1.3 option 1); `plugin.requires` with `setSystemVersion`;
the state check after loading. Does not fit: the Maven demo's fat-jar plugins and its API version tied to pf4j's own
(`${project.version}`), since ours is versioned by itself; nothing in pf4j checks where a zip came from, and its manager loads a
plain directory with no zip at all (inference from the repository code, 2.1).

### pf4j-update 2.3.0

State of maintenance (measured with `gh api` and `curl` on 2026-11-20): the newest release on Maven Central is 2.3.0, `lastUpdated`
2020-01-27 (`maven-metadata.xml`); GitHub has no release objects, only tags, the newest `release-2.3.0` (commit `13bf5de4`,
2020-01-27). The default branch has 11 commits after that tag, the last on 2024-10-12 ("Fix #65", after "Update to PF4J 3.12.1"),
and its POM says `2.4.0-SNAPSHOT`; no release of it exists. 10 open items: 3 pull requests, `pf4j/pf4j-update#64`
(2023-06-13, dependabot), `pf4j/pf4j-update#61` (2022-05-20, dependabot) and `pf4j/pf4j-update#59` (2021-11-21, a feature), none
reviewed, and 7 issues, the newest `pf4j/pf4j-update#66` of 2026-09-03. The repository is not archived. The 2.3.0
sources jar was read in full (14 Java files).

The verifiers it ships. The artifact holds exactly three `FileVerifier` implementations in `org/pf4j/update/verifier/`
(`unzip -l pf4j-update-2.3.0.jar`; the `master` tree lists the same three):
- `BasicVerifier`: only that the path is a regular file and not empty (`BasicVerifier.java:40`);
- `Sha512SumVerifier`: the SHA-512 of the file against `context.sha512sum`, given inline, or as a URL, or as `.sha512` meaning the
  release URL with that extension (`:54-64`, compare and throw at `:70-77`); it **passes silently when no checksum is given**
  (`:54-56`, debug log "No sha512 checksum specified, skipping verification");
- `CompoundVerifier`: runs a list in order, by default the two above (`:32-34`, `:62-66`).

Is a signature check among them: no. `grep -rniE 'signature|openpgp|pgp|x509|certificate|jarsigner|JarFile'`
over the 14 Java source files has 1 hit, a Javadoc sentence in `UpdateRepository.java:58` that signature verification is something an
integrator could do. The checksum comes from the same `plugins.json` or from a URL on the same channel as the file, so it shows
that a file was not damaged on the way, not who made it.

The interface, verbatim from `org/pf4j/update/FileVerifier.java:27-72` of `pf4j-update-2.3.0-sources.jar`:

```java
public interface FileVerifier {

    void verify(Context context, Path file) throws IOException, VerifyException;

    class Context {

        public String id;
        public Date date;
        public String version;
        public String requires;
        public String url;
        public String sha512sum;
        public Map<String,Object> meta = new HashMap<>();

        public Context(String id, PluginInfo.PluginRelease pluginRelease) { ... }

        public Context(String id, Date date, String version, String requires, String url, String sha512sum) { ... }
    }
}
```

(The Javadoc is left out. Both constructors copy the first six fields and leave `meta` empty.) `VerifyException` extends
`org.pf4j.PluginRuntimeException` (`VerifyException.java`), which extends `java.lang.RuntimeException`
(`javap -cp pf4j-3.16.1.jar org.pf4j.PluginRuntimeException`): the failure is unchecked.

When it checks: `UpdateManager.downloadPlugin` downloads from the release URL into a temporary file and runs the verifier once
(`UpdateManager.java:266-275`); `installPlugin` then moves the file into the plugins root and loads it (`:239-255`). Nothing
verifies again at a later start. It has no method that takes a local zip (the public methods are listed in
`UpdateManager.java`: repositories, refresh, install, update, uninstall, none with a file argument). A `file:` repository would
avoid the network, by reading the code, not run.

Dependencies (POM 2.3.0, compile scope): pf4j 3.2.0, gson 2.2.4, commons-codec 1.10; with pf4j's own slf4j-api 1.7.25 and
java-semver 0.9.0 the jars sum to 705,767 bytes (six `Content-Length` values added by hand; asm is optional and not counted).
The artifact targets Java 1.8.

Take over: the shape of `FileVerifier`, which is small enough to meet with a class of our own (below). Does not fit, and why:
- it loads plugins from a catalog over http, https, ftp or file, and this application loads no plugin over the network (2.1);
- its verifiers check a size and a checksum from the same channel and skip the checksum when it is absent, which answers
  neither T1 nor T2 nor T3;
- it verifies once on a download, not at every start and not an unpacked directory;
- the last release is from 2020 and the maintenance since is thin (above), so a needed signature verifier or fix could not be
  expected from a release;
- it would add one library, the pf4j-update jar itself (25,736 bytes): the host already declares commons-codec 1.23.0 and gson 2.15.0
  (`gradle/dependencies.gradle:47` and `:78`, `gradle/libs.versions.toml:11` and `:34`), which its POM asks for at 1.10 and 2.2.4;
  that Gradle resolves both upward to the host's versions is an inference, not run. A new dependency needs the maintainer's yes
  first all the same (`vibe-coding.md`).
Because `Context` is a nested class of the interface, a class cannot implement `org.pf4j.update.FileVerifier` without the jar on
the class path: only an equal shape is possible without it. Not measured: whether 2.3.0 runs against pf4j 3.16.1 on JDK 25, the
unreleased `master` line by line, and the content of issue `pf4j/pf4j-update#66` and of the three pull requests beyond their titles.

### JetBrains marketplace-zip-signer 0.1.45

Facts (Maven Central and GitHub, 2026-11-20): license Apache-2.0 (the `LICENSE` of the tag, the GitHub license API and the POM
agree); newest version 0.1.45, tag commit `d710f0ed63cff802337248ff658b18217b671f36`, released 2026-10-30, the previous ones
0.1.44 the same day and 0.1.43 on 2025-09-02; the repository is not archived and has 16 open items, 8 issues and 8 pull requests (`open_issues_count` counts both). The jar is 161,472 bytes
(SHA-256 `715ddf235c8a422fe09a3b374ad0ed2d0a7c6ae96b03299ef43738ce6c197065`, equal to the `.sha256` on Central), Java 8 bytecode
(`javap -v ZipVerifier.class`: major version 52).

Runtime dependencies (POM 0.1.45): bcpkix-jdk18on 1.81 and kotlin-stdlib 2.2.10 (compile), protobuf-java 3.25.6 (runtime); through
them bcutil and bcprov 1.81 and `org.jetbrains:annotations` 13.0. Jar sizes in bytes (`stat -c '%s'` of the seven downloaded
jars): signer 161,472, kotlin-stdlib 1,750,374, protobuf-java 1,876,053, bcpkix 1,149,752, bcutil 705,888, bcprov 8,948,201,
annotations 17,536, together 14,609,276 (13.9 MiB). This application already declares bcprov, bcpkix and bcutil at 1.86
(`gradle/libs.versions.toml:7-9`; bcpkix 1.86 depends on bcutil, so it is on the host's class path); the part that would be new is
Kotlin's standard library (1,750,374 bytes), protobuf (1,876,053), the signer (161,472) and `org.jetbrains:annotations` (17,536),
3,805,435 bytes together; Kotlin's standard library and protobuf alone are 3,626,427 bytes. The signing and verifying path uses the
JDK's `Signature` and `CertificateFactory`; Bouncy Castle appears in 4 of the 46 files of the sources jar
(`grep -rln org.bouncycastle`): `CertificateUtils.kt`, `PrivateKeyUtils.kt` and `SignerInfoLoader.kt` (loading PEM keys) and
`SecurityUtils.kt` (registers the provider), and the verifier calls `CertificateUtils.isValidCertificateChain`
(`ZipVerifier.kt:82`). Not run: the verifier without Bouncy Castle on the class path, and the library against BC 1.86 instead of
its 1.81.

Measured with a throwaway probe on JDK 25.0.5, a 342-byte zip (`plugin.properties`, `lib/demo.txt`), two throwaway certificates
(RSA-2048 with SHA-256 and EC secp384r1 with SHA-384, self-signed, made at run time), `ZipSigner.sign` and `ZipVerifier.verify`:

| case | result |
|---|---|
| signed with its own self-signed certificate | `SuccessfulVerificationResult`, chain of 1 (the certificate itself), `isSignedBy(trusted)` true, `isSignedBy(other)` false; same for EC |
| signed by key A, caller pins certificate B | `SuccessfulVerificationResult` too, `isSignedBy(B)` false: the library does not decide trust, the caller must pin |
| one byte flipped in a stored entry, in a deflated entry, in the central directory, in the digest area of the block | `InvalidSignatureResult`, "ZIP integrity check failed. CHUNKED_SHA256 digest mismatch." |
| one byte flipped near the end of the block (the signature) | `InvalidSignatureResult`, "Failed to verify SHA256withRSA signature using public key from certificate" |
| signature removed with `ZipSigner.unsign`, or zip rebuilt with `ZipOutputStream` | `MissingSignatureResult` (an unsigned zip is a result, not an error; `unsign` returns the original 342 bytes exactly) |
| 5 bytes appended after the end | throws `ZipException: ZIP End of Central Directory record not found` |
| `unzip -l`, `unzip -tq`, `ZipFile`, `ZipInputStream`, pf4j 3.16.1 `Unzip` on the signed zip | all list and extract both entries; a signed zip is still a valid zip |

So it accepts a signature made with a self-signed certificate of our own, provided the caller compares the signer with a
built-in certificate (`ZipVerificationResult.kt:38`, `certificateChains.find { it.last() == certificateAuthority }`); it checks
no validity dates and does no PKIX path validation (`grep -rniE 'checkValidity|getNotAfter|notBefore|PKIX|CertPath|TrustAnchor'`
over the 46 files of the sources jar: 0 hits). The signature lives in a block between the last entry and the central
directory, in the scheme of APK Signature Scheme v2 (`ZipMetadata.kt:17-18`, `:23-63`; the probe printed "central dir at 1336,
signing block [199,1336) length 1137"); the digest covers everything before the block, the central directory and the end record, in
1 MiB chunks. Not run: chains longer than 1, zips over 1 MiB, ZIP64, an archive comment, RSA-4096 and SHA-512 variants, and a
full pf4j `PluginManager` loading a signed zip (only `Unzip` ran).

Take over: the idea that one signature covers the whole zip, central directory and entry names included, so that a file outside
any jar (`plugin.properties`, `classes/`) is covered, which jarsigner alone does not do (next section); the rule that the
caller pins trust and that a missing signature is the caller's failure; a sealed result with three cases. Does not fit:
- it signs with X.509 certificates, while the release key of this project is an OpenPGP key (#642) and 2.2 option A makes the
  verifier a Bouncy Castle `bcpg` call in the mystic-crypt library;
- it rewrites the zip (a block inside it), so the zip is no longer what Gradle's `Zip` task made, and a tool that repacks it
  drops the signature without a word (the `MissingSignatureResult` rows above); a detached `.asc` leaves the zip byte for byte
  as it was;
- it checks no validity and no revocation, which the lifecycle of 2.5 needs (a signature made inside the key's validity);
- it brings Kotlin's standard library and protobuf, 3,626,427 bytes (3.8 MB with the signer and the annotations jar), for a verifier that a Bouncy Castle call already gives us.
A comparison, not a measurement: a signed zip of this kind and a detached signature protect the same bytes; they differ in who
holds the key format and in whether the zip changes.

### jarsigner and `JarFile`

What JDK jar signing covers, measured with throwaway keys (two self-signed EC secp256r1 keys made with `keytool`, the password a
random value in the process environment only) and a probe over `java.util.jar.JarFile` on JDK 25.0.5, plus `jarsigner -verify`:

| case | result |
|---|---|
| untouched signed jar, `JarFile(file, true)` | every entry reads, each reports the signer; `META-INF/*.SF` and `*.EC` report none, by design |
| one byte of a signed entry changed | reading that entry throws `SecurityException: SHA-384 digest error`; other entries still read, so a caller must read every entry |
| an unsigned entry added to the signed jar | read without error, `getCodeSigners()` is null; `jarsigner -verify` still prints "jar verified" and only warns of unsigned entries |
| the signature files removed | a plain unsigned jar, accepted with no error, all signers null: a downgrade needs no key |
| a jar signed by a second key B | accepted, the signer reported; no trust decision, `jarsigner` only warns "self-signed" and "PKIX path building failed" |
| plugin zip with a signed `lib/demo.jar`, then `plugin.properties` and a class outside it changed | the jar inside is byte-identical and still verifies; the changes are invisible. Opening the zip itself with `JarFile` reports every entry unsigned, and `jarsigner -verify` on the zip says there is no manifest |

The Javadoc of `JarFile` (JDK 25) says the verification "does not include validating the signer's certificate" and that a caller
"should inspect the return value of `JarEntry.getCodeSigners()`", and the JAR File Specification allows later-added files and
signing a subset (both read at docs.oracle.com on 2026-11-20). What it does not cover, then: any file outside a signed jar, the
entry list of a zip, an added or removed entry, and a downgrade to unsigned; and it needs the caller to require a signer on every
entry and to compare it with a pinned certificate.

This matches the layout of our zips. The checksum plugin's zip holds `plugin.properties` and `classes/` and no jar
(`plugins/checksum-plugin/build.gradle:104-117`, `unzip -l` of the built zip); `git grep -n pluginZip origin/develop -- '*.gradle'`
searches 49 `.gradle` files at `e9c7d983` and hits 16: the 15 plugin builds, each with the same task, and
`gradle/testing.gradle`. Only the lethenon zip has `lib/`, with two bundled jars, and neither is signed (`unzip -l`
of each for `META-INF/*.SF|RSA|DSA|EC`: no signature files). The plugin's own code and its descriptor, `requires` included, are
outside any jar. Not measured: pf4j's loader with a jarsigned `lib/` jar, a signed entry deleted, multi-release entries,
timestamped signatures. The application jar itself is jarsigned today (`gradle/packaging.gradle:57-89`), which is the 2.2
option B situation, and stays so.

Take over: the rule to treat absence as failure, from the downgrade row. Does not fit: as the only check, for the reason in the
last table row; as a second check inside the zip it would need every plugin packed as a jar, which 2.2 option B already weighs.

### Grafana

Read at tag `v13.3.1` (published 2026-11-10, tag object `e8dedc1e3b79f85173a8a1ee6a93153c9092dd45`) and the plugin-tools
documentation; nothing was run, no Grafana was installed. Where a plugin carries `MANIFEST.txt`, an OpenPGP clearsigned message
whose JSON lists `plugin`, `version`, `keyId`, `time` and for every file its SHA-256 (`signature/manifest.go:40-54`,
`:271-276`), Grafana checks the signature against a key by its id (`openpgp.CheckDetachedSignature`, `:366`) and every file
against the list.

The states, from the code (`pkg/plugins/manager/signature/manifest.go:143-248`, `signature.go:28-78`, `authorizer.go:22-38`,
`pkg/plugins/models.go:229-234`):

| state | detected when | Grafana does |
|---|---|---|
| valid ("signed and accurate") | signature verifies, every listed file matches, no file outside the list, plugin id and version match | loads |
| modified | the signature is valid but a listed file differs or is missing, a file is not in the manifest, or id or version differ | refuses, no override, not even in development mode |
| invalid | manifest unparsable, signature fails or the key id is unknown, not a version 2 manifest, no files listed, or the root URL does not match | refuses, no override |
| unsigned | no `MANIFEST.txt`, or one shorter than 10 bytes | refuses by default; loads in development mode (every unsigned plugin) or when the plugin id is in `allow_loading_unsigned_plugins`, with a warning in the log |

The documentation says the same ("If a plugin is unsigned, then Grafana neither loads nor starts it.",
`docs/sources/administration/plugin-management/plugin-sign.md:44`, and "Plugins with modified signatures are never loaded.", on the
configuration page `:2858`, same tag), and the plugin-tools documentation says that Grafana "has a built-in public key that it
uses to verify" the signature (`grafana/plugin-tools`, `docusaurus/docs/publish-a-plugin/sign-a-plugin.md:129`, commit
`81668566961ab1ad65206af190239847656f8c03`; the v13.3.1 documentation read here does not contain the phrase). Development mode is `app_mode = development`
(default `production`, `conf/defaults.ini:6-7`); it only lifts the unsigned state (`authorizer.go:22-38`). The built-in key is one
OpenPGP public key as a constant in the source (`statickey/static_retriever.go:10-33`); Grafana additionally downloads a key list
from grafana.com every 10 days (`keyretriever/dynamic/dynamic_retriever.go:21`, `:111-178`) unless
`public_key_retrieval_disabled = true` (`retriever.go:17-25`), in which case the constant alone is used. These files were read as
named; a count-backed search was not run over the repository, so "there is no list of several keys in the code" is a reading of
those retriever files, not a search result. Not decoded: the armored key itself (the user id in the key was not checked).

Take over, as the model for the verifier's states:
- the state is computed locally from the files, and the extra-file rule (a file the manifest does not list makes the plugin
  modified) closes the hole jarsigner leaves; for us it corresponds to T3, the unpacked directory;
- only the unsigned state can be overridden: modified and invalid never, in no mode;
- the override of unsigned is per plugin id, not only a global switch (it corresponds to the consent record recommended in 2.3, 2);
- a built-in public key; Grafana itself supports running on that key alone, with the download of keys switched off.
Does not fit: the key rotation over the network (2.1: no path fetches a key, a rotation here is a host release, 2.5); signature
levels (private, community, commercial) and root URLs, which exist for a catalog and a server; and the development mode as one
switch for all unsigned plugins, which 2.6 decides differently (an artifact-level binding, not a switch). A manifest of per-file
hashes is the shape of 2.2 option C plus the directory check of 2.3; Grafana's route is nearer to those than to option A, which
signs the zip as a whole (inference).

### Spinnaker

Spinnaker's plugin framework is kork-plugins on pf4j. Read at: kork `v7.254.0` (commit `a7b6fdea15ec2e98c80093947f194afb0ee552f3`),
pf4j `release-3.10.0` and pf4j-update `release-2.3.0` (the versions kork pins in `spinnaker-dependencies.gradle:186,196`),
spinnaker-gradle-project `v8.32.1`, gate `v6.69.0`, deck `v3.21.0`, and the spinnaker.io documentation at commit
`7fb6d742067f41b50b9cfe188e96e38b6767749d` (no release tags there). The pf4j lines were read at 3.10.0, not at the 3.16.1 this
application uses.

- Plugin API. A separate module `kork-plugins-api` (it exposes pf4j and `kork-annotations` as `api`,
  `kork-plugins-api/kork-plugins-api.gradle:22-27`), one API module per service, and plugins declare them `compileOnly`: "The
  plugin APIs are marked as `compileOnly` so as to not include them in the resulting plugin jar. These dependencies are loaded
  from the service's classloader, not the plugin classloader" (spinnaker.io `plugin-creator/project-config.md:129-132, 152-153`).
  The API has no version of its own; it is released with kork and a plugin pins it with `korkVersion`. Stability is marked per
  API with `@Alpha` (may be removed without notice) and `@Beta` (a long-term commitment, the implementation may change)
  (`plugin-creator/plugin-backend.md:115-116`). Not looked for beyond that text: no search was run over the kork
  repository for a compatibility policy of the API module itself.
- Version check. The manifest attribute `Plugin-Requires`, written by the Gradle plugin (`SpinnakerServiceExtensionPlugin.kt:69`,
  default `<service>>=0.0.0`, `SpinnakerPluginExtension.kt:51-56`), is a list of `service constraint` pairs such as
  `orca>=8.0.0,deck>=0.0.0`, because one bundle extends several services (`VersionRequirementsParser.kt:23-34, 66-70`); kork's
  `SpinnakerServiceVersionManager` picks its own entry (`:34-51`). The check is pf4j's `isPluginValid` at load: on failure the
  plugin is DISABLED and logged (pf4j `AbstractPluginManager.java:746-763, 862-865` at release-3.10.0), and kork's own test kit
  asserts that such a plugin is not among the started ones (`PluginsTck.kt:55-63`). This is the same behaviour as measured above
  at 3.16.1: disabled, not rejected.
- Display of a disabled plugin. A log line, not a view: pf4j warns "requires a minimum system version of X, and you have Y"; the
  `installedPlugins` actuator endpoint and Gate's `/plugins/installed` return only the pf4j `PluginDescriptor`, with no state and
  no reason (`InstalledPluginsEndpoint.kt:31-53`, `PluginsInstalledController.kt:29-35`). Strict mode
  (`spinnaker.extensibility.strict-plugin-loading`, default true) stops the service when a plugin cannot be loaded
  (`SpringStrictPluginLoaderStatusProvider.kt:27-35`). In Deck, a manifest or code that fails to load goes to `console.error` and is
  rethrown (`plugin.registry.ts:92-94, 106-113` for the manifest, where a Gate 404 is swallowed instead, `:109-111`; `:162-164` for
  the code). Enabling is a Spring property, default false
  (`SpringPluginStatusProvider.kt:39-45, 63-67`).
- Origin check. A checksum, no signature: the `sha512sum` of a release in `plugins.json` or Front50, written by the Gradle plugin
  (`CreatePluginInfoTask.kt:74-88, 100-110`), checked by pf4j-update's default `CompoundVerifier` (`Front50UpdateRepository.kt:42`,
  `ConfigurableUpdateRepository.kt:33`), which skips the check when the value is absent (see pf4j-update above). A plugin placed in
  the plugins directory is not checked at all. The search `grep -rliE 'sha512|checksum|signature|verif|gpg|pgp'` over the 174
  source files of `kork-plugins` has 14 files hitting, all of them the checksum, update-repository and test code just named; the
  narrower `signature|gpg|pgp` has 2 hits, both the word in a Javadoc sentence ("a similar signature as ...",
  `PluginInfoReleaseProvider.kt:29`, `PluginInfoReleaseSource.kt:28`). No signature, key or certificate handling in kork-plugins or
  pf4j-update 2.3.0. In spinnaker-gradle-project, the plugin-bundle module `spinnaker-extensions` (29 files searched, 0 hits for
  `signature|gpg|pgp|signing`) signs nothing; the same repository's `spinnaker-project-plugin` PGP-signs Spinnaker's own Maven
  publications (`NexusPublishPlugin.groovy:51-84`), which is not a signature on a plugin bundle. A `Plugin-Unsafe` manifest flag loads a plugin into the parent class loader
  (`SpinnakerManifestPluginDescriptorFinder.kt:33, 41`).

Take over: the separate API module with `compileOnly`; `Plugin-Requires`-style requirement checked by a version manager with the
DISABLED result; `@Alpha` and `@Beta` on API types as a cheap way to say what may still change without a version per package;
a per-plugin enabled switch and a failing plugin that does not stop the others (we already keep the enable state in settings);
generating the requires value and the checksum in the Gradle build so that they cannot drift from the built zip. Does not fit: the
update repository, Front50, `plugins.json` and pf4j-update (no server, no network path); the multi-service `requires` (one host,
one range); a reason shown only in a log (a desktop user has no log reader, so the plugin list shows state and reason itself,
which Spinnaker gives no model for); a checksum from the same place as the zip and skipped when absent (no origin check for a
password manager; it must not be copied); `Plugin-Unsafe` and Spring properties for enabling (server concepts). Spinnaker has no
signature scheme, so an origin check has no precedent there.

### Halo CMS

Read at tag `v2.27.3` (commit `736beca9766cea6c1c46a9d19d89cba405acf552`; `gradle.properties` at that tag still says
`2.27.0-SNAPSHOT`), plus docs.halo.run and the `plugin-starter` repository on branch `main` on 2026-11-20 (not pinned to a
commit). The host pins pf4j 3.16.0 (`gradle/libs.versions.toml:23`).

- Plugin API. A separate artifact built from the host repository's `api/` module, `run.halo.app:api` (`api/build.gradle:10-11`,
  and `:86`, `api "org.pf4j:pf4j"`), versioned with the host, with no version of its own; a plugin declares it `compileOnly`, with
  versions from a BOM (`platform/plugin/build.gradle:6`) and a Gradle plugin that names the targeted Halo version. The exact
  dependency block comes from the starter's `build.gradle` on `main` through a summarising fetch, so it is not quoted verbatim.
- Version check. `plugin.yaml` has `spec.requires`, a SemVer range, default `*` (`Plugin.java:103`); the documentation page does not
  say what a mismatch does. The code checks it twice: at install and upgrade, where `PluginServiceImpl.satisfiesRequiresVersion`
  throws `UnsatisfiedAttributeValueException` and the plugin is **rejected** and not stored (`PluginServiceImpl.java:161-168`,
  `:489-503`); and when enabling, where `PluginReconciler.checkRequiresVersion` sets the phase FAILED and a condition with the
  reason `UnsatisfiedRequiresVersion` and the message "Plugin requires Halo version [..], but the current version is [..]", with
  no retry (`PluginReconciler.java:452-471`, called at `:678-683`). The second check exists because the host can be updated
  after the plugin was installed.
- Display. The status is a resource: `status.phase` (`PENDING`, `STARTING`, `CREATED`, `DISABLING`, `DISABLED`, `RESOLVED`,
  `STARTED`, `STOPPED`, `FAILED`, `UNKNOWN`) and `status.conditions` with type, status, reason, message and time
  (`Plugin.java:130-155, 165-177`; reasons such as `StartError`, `SystemError`, `UnsatisfiedRequiresVersion` at
  `PluginReconciler.java:1020-1038`). The console shows reason and message for a failed plugin: `use-plugin.ts:57-72`,
  `Detail.vue:96-98, 197-215` (a red alert with the reason as title, the message, a copy button) and
  `PluginConditionsModal.vue:20-81` (a table of the conditions).
- Origin check: none found. The search `grep -rniE 'signature|checksum|sha512|digest|sha256'` over the 65 files of
  `application/src/main/java/run/halo/app/plugin` has 4 hits: SHA-256 of strings as cache keys for the UI bundle
  (`PluginServiceImpl.java:321`, `UiPluginBundleServiceImpl.java:377, 392`) and a checksum of a configuration map
  (`DefaultReactiveSettingFetcher.java:157`); none checks a plugin file. Install is by upload or from a URI the server fetches
  (`PluginEndpoint.java:424-430`), which only parses `plugin.yaml`, checks `requires` and dependencies and guards against path
  traversal; a new plugin starts disabled (`PluginServiceImpl.java:176`). The one protection is a warning in the install dialog
  against untrusted sources, pointing to the official store (`ui/src/locales/en.json:626-627`). The store's own code is not in
  that repository, so whether it signs packages is not known.

Take over: the separate API artifact as `compileOnly`; the range field checked at install and again at enable; a per-plugin
status with a stable reason code, a message and a time, shown in the list or detail view as "reason: message", and its reason
set as the list of states to cover; plugins starting disabled after install, so the user enables them on purpose; a warning
in the install dialog (our consent dialog of 2.3 is the stronger form of it). Does not fit: the reconciler loop with
Kubernetes-style conditions stored as a resource (a small state and one reason and message per plugin is enough on a desktop); install
from a URI (a network path, which this application has none of); a Spring-based API with web and security types as `api`
dependencies (ours is small and stable, 1.3); trust through a store link (no store here). Halo has no origin check to copy,
so for this part it is no precedent.

### The verifier, cut for the `FileVerifier` shape (a proposal, after RELEASE-8.16)

What the maintainer asked: a verifier that could satisfy pf4j-update's `FileVerifier` without depending on pf4j-update. The
measured facts above allow it: the interface is one method with a `Context` of seven public fields. The proposal below is a
suggestion for the day the decisions of 2.2, 2.3 and 2.5 are taken; none of it is built, and it is code for after
RELEASE-8.16. The package name is a placeholder.

```java
package io.github.astrapi69.mystic.crypt.plugin.trust;

public interface PluginFileVerifier {

    void verify(Context context, Path file) throws IOException, VerifyException;

    class Context {
        public String id;
        public Date date;
        public String version;
        public String requires;
        public String url;
        public String sha512sum;
        public Map<String,Object> meta = new HashMap<>();
        // the six-parameter constructor of the measured Context; the PluginRelease constructor stays in the adapter
    }
}
```

`VerifyException` is our own unchecked exception in the same package, extending `org.pf4j.PluginRuntimeException` as
pf4j-update's does (pf4j is already a dependency of the host). Method, parameters and exceptions are those of the measured
interface; `Context` carries the fields the measured one carries. For a local zip, `url` and `sha512sum` stay null (our
verifier does not read them; the SHA-256 of #998 remains the cache key behind the check), `id`, `version` and `requires` come
from the plugin's descriptor, and the detached signature is the file `<zip>.asc` beside the zip, or the path in `meta` under the
key `signature` when a caller has copied the zip elsewhere. An adapter to pf4j-update would be one class: it implements
`org.pf4j.update.FileVerifier`, builds our `Context` from the pf4j-update one field by field, calls `verify` and turns our
`VerifyException` into `org.pf4j.update.VerifyException`. It would be added only if the application ever loads plugins from a
repository, which 2.1 says it does not.

What it checks, from the option 2.2 recommends: the detached OpenPGP signature of the zip, with Bouncy Castle `bcpg` through the
mystic-crypt library, against the embedded list of keys (2.5, with its lifecycle: a signature made inside the key's validity),
before anything is unpacked, at install and at every start. The class that implements the interface also has a second method,
`inspect(Context, Path)`, which returns the state and a reason instead of throwing; `verify` calls it and throws unless the
state lets the plugin load. The states follow Grafana's model as read above:

| state | meaning here | what the host does |
|---|---|---|
| signed | the `.asc` parses, its key is in the embedded list and valid when it signed, and it verifies over the zip | loads |
| modified | the `.asc` parses and names a key of the list, but does not verify over this zip's bytes | refuses at every start; no override |
| unsigned | no `.asc` | refuses at the start; at Install from Zip the consent of 2.3, 2, recorded by the zip's SHA-256 |
| invalid | the `.asc` does not parse, names a key that is not in the list, or was made outside that key's validity | refuses; no override |
| development mode | in a development build only: signed by the throwaway development key that 2.6's recommendation puts into the trust set, or, if the maintainer chooses 2.6 (i), unsigned accepted | loads, and the window title and the plugin list say so |

Two things the table keeps from Grafana: modified and invalid are never overridden, and only unsigned has an override. One
caution, an inference: with a detached signature, a forged signature that names a key of the list looks the same as a changed zip,
so "modified" is a name for "does not verify", not a proof that the zip was edited. Grafana's extra-file rule has no counterpart in
a signature over the zip alone; for T3 the directory record of 2.3 remains.

### Taking stock: what we take over, what does not fit

| solution | take over | does not fit | why |
|---|---|---|---|
| pf4j 3.16.1 | `plugin.requires` with `setSystemVersion`; the API module with `compileOnly`; the state check after loading | the Maven demo's fat jars and API version tied to pf4j's; no origin check, no signature | `requires` is not checked unless the host sets the system version, and a mismatch gives DISABLED, so the host must read the state and show the reason itself |
| pf4j-update 2.3.0 | the shape of `FileVerifier` (one method, a `Context`) | the library: catalog, download, checksum from the same channel, once, skipped when absent | no network path (2.1); no signature verifier in it; last release 2020; one more library (25,736 bytes), gson and commons-codec are already in the host |
| marketplace-zip-signer 0.1.45 | a signature over the whole zip; the caller pins trust and treats a missing signature as failure | the library: X.509, a block inside the zip, no validity check, Kotlin and protobuf | the release key is OpenPGP (#642); a detached `.asc` keeps the zip as it is; 3.8 MB more (Kotlin, protobuf, the signer, annotations) |
| jarsigner and `JarFile` | treat absence of a signature as failure | as the only check | our zips hold `plugin.properties` and `classes/` outside any jar; adding a file, removing the signature and a second key all pass |
| Grafana | local states; only unsigned overridable; a per-id consent; a built-in key; the extra-file check for the directory | key download over the network; signature levels and root URLs; one global development switch | a catalog and a server shape those parts; our key list changes with a host release (2.5) |
| Spinnaker | the `compileOnly` API module; `requires` checked by a version manager with DISABLED; `@Alpha` and `@Beta`; build-generated requires | update repository and Front50; multi-service `requires`; reason only in a log; checksum skipped when absent; `Plugin-Unsafe` | server concepts; a desktop user needs the reason on screen; it has no origin check |
| Halo CMS | the `compileOnly` API artifact; the range checked at install and at enable; status with a reason code and message shown in the list; start disabled; an install warning | the reconciler and stored conditions; install from a URI; a Spring API; trust by store | heavier than a desktop needs and network-based; it has no origin check |

None of this adds a decision beyond those already listed below; the choices it touches are 2.2 (option A), 2.3 (unsigned
handling), 2.5 (the key) and 2.6 (development builds), and the proposal above follows the recommendation of each.

## What stays for the maintainer

1. Part 1: option 1, 2 or 3; the scope table of 1.2 as drafted or narrowed; menu designer in the API or exempt.
2. Part 2: option A, B or C; release subkey or a plugin key; unsigned handling (1 to 4 above); whether the directory is checked
   (T3) in the first cut.
3. Public wording: `SECURITY.md` says the application "loads plugins from a directory"; issue #1093 describes the gap neutrally,
   with no attack steps, as the task asked. 2.1 now shows that no path loads a plugin over the network, so the issue stays public
   as the maintainer decided; where `SECURITY.md` keeps back a path that is open in every release, he may still prefer the private
   route.
4. **After RELEASE-8.16**, section 2.5: the signing key, the release subkey `10E2BD8D` or a plugin signing subkey under the same
   primary, and the lifecycle of an embedded key (retire, drop, expiry, renewal).
5. **After RELEASE-8.16**, section 2.6: how a development build starts locally built plugins, and how the mode is kept out of an
   installed application (options 1 to 3, and the sub-question unsigned zips or a throwaway development key).
6. **After RELEASE-8.16**, section 1.5: the certificate wizard classes and `VaultAccess` with `MysticCryptEntryModelBean`: into the
   API, into a family library, or replaced by API types. Moving or renaming the entry bean is a persisted-format change and his
   decision (architecture.md).
7. **After RELEASE-8.16**, section 2.7: the maintainer's idea of an allow-list of plugins inside the vault. Signatures with the
   list as the consent store, signatures alone, or a list in the configuration directory (shown only to say why not); where
   the list sits (a new optional field of the 9.0 vault root, or 9.0 `extensions` with a change to that record's rule that the
   host never interprets it; a new MCRDB2 element with `FORMAT_VERSION` 3; or a marked entry) and how a second vault
   and the cancelled sign-in are handled.

Proof of concept files (the jdeps output, the program that installs a changed zip and the `plugin.requires` check, each with its
output, and the `javap` call counts) are kept outside the repository. What they showed is quoted in #1092 and #1093 and in the
sections above, with the command that produced each figure.
