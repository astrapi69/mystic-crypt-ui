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

### 1.5 The wizard classes and `VaultAccess`: API, a family library, or API types

The maintainer's question (2026-11-20, German, translated): the certificate wizard classes (13, used by certificate and keygen)
and `VaultAccess` with `MysticCryptEntryModelBean`: into the API, into a family library of their own, or replaced by API types?
The decision is taken after RELEASE-8.16. Facts, measured on `develop` at `edba6448` unless a line says otherwise.

**The wizard classes.**
- The 13 holds as a bytecode count, not as a source count. The two plugins import 10 distinct `wizard.*` classes (certificate all
  10, keygen 5: `CertificateInfoModelToX509` and the four models). `jdeps -verbose:class` with the 8.15 host jar on the class path and the unpacked classes of the certificate and keygen plugins
  lists 13 wizard types: those 10 and three Lombok nested builders, plus `panel.certificate.NewCertificateInfoPanel` (keygen).
  Caveat: this run used the installed plugin classes and the 8.15 host jar, not a rebuild of `edba6448`; the plugin sources have
  not changed since `e1034a61` (`git log -1 -- plugins/certificate-plugin plugins/keygen-plugin`).
- The real set to move is larger: the source closure of the 10 inside the wizard packages is 17 files (all 18 files of `wizard/` but
  `CertificateWizardPanelTest.java`, a class of that name that sits in `src/main`), 2,681 lines (the sum of the per-file line counts, `git show origin/develop:<file> | wc -l`), with `NewCertificateInfoPanel` (606 lines) beside it. Closure computed by a
  name-reference script over `git archive origin/develop src/main/java`; the script is not in the repository.
- The host itself does not use the wizard panels: outside `wizard/`, only `panel/certificate/NewCertificateAttributesPanel.java:34`
  and `panel/certificate/NewCertificateInfoPanel.java:42-44` import wizard classes, and those are the four models.
  `NewCertificateInfoPanel` has no host main user either (`git grep` of `src/main` finds only a comment in
  `messages.properties:145`); keygen uses it (`CryptographyPanel.java:37`) and the wizard's `IssuerPanel` and `SubjectPanel` reach
  `NewCertificateAttributesPanel`. Host tests use the wizard (`src/test/.../wizard/*Test.java`, three e2e smoke tests).
  **Finding, not an option:** the wizard code is used by the two plugins only; the host carries it as plugin code.
- Dependencies of the 17 files outside `wizard/`: `Messages`, `eventbus.ApplicationEventBus`, `random.SecureRandomValues` and
  `panel.certificate.NewCertificateAttributesPanel`. `NewCertificateInfoPanel` also pulls `MysticCryptApplicationFrame` (970
  lines), `ui.screen.ScreenPlacement` and `random.*`. Libraries: swing-wizard 2.1, state-pattern 7.1, model-data,
  swing-base-components, swing-model-components, silly-collection, crypt-data, LGoodDatePicker, MigLayout, Lombok and Bouncy
  Castle (`org.bouncycastle.asn1` in three files). The four models are Swing-free (Lombok, crypt-data models, one ASN.1 type).
- The family catalog (`coding/Familie-Katalog.md`, lines for swing-wizard and state-pattern) lists two wizard libraries, both
  already dependencies of this application (`gradle/libs.versions.toml:72, 81, 152, 161`; the certificate plugin declares both
  `compileOnly`, `plugins/certificate-plugin/build.gradle:48-49`). They are the generic wizard framework. A certificate wizard
  library does not exist in the family; it would be new.

**`VaultAccess` and the entry bean.**
- `plugin/api/VaultAccess.java:30` imports `panel.dbtree.MysticCryptEntryModelBean`; `entries()` (`:54`) returns it, `add` (`:65`)
  and `change` (`:76`) take it; `remove(UUID)` does not. The one implementation is `plugin/vault/MarkedEntryAccess.java:44`. Only
  lethenon uses `VaultAccess` (`git grep -l -E 'plugin\.api\.VaultAccess|VaultAccessContribution' origin/develop -- plugins`,
  minus lethenon, is empty); its main sources call `getId`, `getTitle`, `setTitle(char[])`, `getProperties` and the builder with
  `id`, and through the host helpers `VaultEntryProperties` and `VaultEntryAttachments` also the protected property keys and the
  resources. It touches nothing else of the bean (password, user name, URL, notes, expiry, times, history).
- **The bean is persisted, and its class name is part of the format.** The vault XML names the element by the fully qualified name
  (XStream, no alias): `VaultFormatIsUnchangedTest.java:91-102` and `EntryPropertiesTypeIsPartOfTheFormatTest.java:92-105` hold
  `<io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>`; `ApplicationModelBean.java:62,67` holds it; and
  `VaultXmlCodec.java:118-122` allowlists it, so a reader builds only listed types. Moving or renaming the class is a
  persisted-format change (architecture.md: ask first). A mechanism for reading old names exists (`VaultXmlCodec.java:105-107`,
  `:270-276`, `xStream.alias(oldName, type)`, used for 7.x names in #624 and held by `VaultsOfSevenXOpenTest`), but the alias given
  last decides the written name, so a rename that older builds must still read keeps the old name on disk. 30 files in `src/main`
  and 51 in `src/test` and `src/e2eTest` reference the class (`git grep -l`).
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
   `KeyValuePair` from silly-collection) are the vault format, so the format becomes an API promise and lethenon, ccw's, sees the
   whole entry; if the package changes, the vault format changes and `VaultXmlCodec` needs the alias, with the effect on older
   builds above. The class and `VaultAccess` would be the only host types in the API. Risk: the narrowest-access rule of #675
   is lost for the entry.
2. **Into a family library of their own.** Wizard: a new library (a name and a repository are the maintainer's; a Gradle subproject
   here first, published later, is the same step as 1.4) holds the 17 files; certificate and keygen bundle it in `lib/` (the
   lethenon zip bundles its own) or declare it `compileOnly` if the host ships it. Consequences: a new repository or a new
   subproject with its own versions, Central publishing (RL-090 signing, 1.4) and family release cycle; the host removes
   `NewCertificateAttributesPanel`, `ApplicationEventBus`, `Messages`, `SecureRandomValues`, `ScreenPlacement` and
   `MysticCryptApplicationFrame` from the closure first (the library cannot depend on the host); the 4 models and the panels
   in host tests (`src/test/.../wizard`, three e2e smoke tests) move with it; the plugins' zips grow by the library, and if the host
   and a zip both carry it, class loading decides which copy runs (not measured). Bean: a library of the entry model would hold the
   persisted class; the family has no vault entry library (catalog: none), so it would be new too, and the same two cases as in
   option 1 apply to its package name; the host would depend on the library for its core format, which is the largest change of the
   three for something only lethenon reads.
3. **Replace by API types.** Bean: the API gets a small value type for an entry (id, title, properties, protected property names,
   attachments); `VaultAccess` uses it and `MarkedEntryAccess` maps it to the bean and back. The bean, its package and the vault
   format do not change; lethenon's change is ccw's, bounded to `LethenonVault`, `VaultWalletEntry` and the plugin's tests (20 test files of the plugin use it, among them an in-memory `VaultAccess`); the two helpers `VaultEntryProperties` and `VaultEntryAttachments` become API methods or
   host-side mapping. Wizard: this works for the wizard only where a plugin needs a value, not a panel. Keygen needs
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
it holds only what a plugin must see (read at semver.org/spec/v2.0.0.html, 2026-11-20). This replaces the loose sentence in 1.2
that the wizard goes into "a support library the zips bundle", which this section now decides after RELEASE-8.16 instead.

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
- No main source of the host or of a plugin uses `java.net` sockets or `HttpURLConnection`, `HttpClient`, `openConnection`,
  `openStream` or `URLClassLoader`, nor `pf4j.update`, `UpdateManager`, `UpdateRepository`, `PluginRepository`, `InetAddress` or
  `DatagramSocket`: `git grep -E "java\.net\.(URL|URI|HttpURLConnection|URLConnection|Socket|ServerSocket|http)|HttpClient|openConnection|openStream|URLClassLoader|pf4j\.update|UpdateManager|UpdateRepository|PluginRepository|InetAddress|DatagramSocket" origin/develop -- src/main/java "plugins/*/src/main/java"`
  finds three `java.net.URL` imports, all in the host: `SecretKeyTreeWithContentPanel` (validates an entry's address and opens it
  in the browser, lines 1284-1296 and 1436-1450) and the two sign-in panels, whose Help button asks whether the wiki page
  answers (`URLExtensions.isReachable`, `MasterPwWithApplicationFilePanel.java:437`, `NewMasterPwFilePanel.java:352`) before it
  opens the browser. None of the three fetches code.
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
A consequence used in 2.5: the application cannot learn of a revocation while it runs.

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
Since then the subkey was measured to be a key that also sits in CI secrets of public repositories; 2.5 sets out both options with
that fact and the lifecycle of an embedded key, and recommends differently.

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
  successful tag run of its `publish.yml`, 2026-11-19) names issuer key `9FCF7C9710E2BD8D`, RSA 4096, SHA-512
  (`gpg --list-packets` of `mystic-crypt-13.11.1.jar.asc` from `repo1.maven.org`). The signature has the issuer key ID subpacket
  and no issuer fingerprint subpacket. Public workflow files use the secret `GPG_PRIVATE_KEY` (plus `GPG_PASSPHRASE` and the
  optional `GPG_KEY_ID`, described in `mystic-crypt`'s `publish.yml` header as "the signing subkey's last 8 hex digits, when
  `GPG_PRIVATE_KEY` holds that subkey alone") in 35 of the 63 repositories of the relevance list, 40 lines in all (`git grep -n -I -E
  '^[^#]*(secrets\.GPG_PRIVATE_KEY|secrets\.[A-Z_]*SIGNING_KEY|ORG_GRADLE_PROJECT_signing|gpg --import|signingKey)' origin/develop
  -- .github/workflows` in each local checkout). Not measured: 19 of the 63 have no local checkout, so 35 is a lower bound; and
  whether each of the 35 holds the same subkey, not the primary (only `mystic-crypt` was checked through its signature).
  `docs/decisions/release-key-openpgp-2026-10-29.md` says the same arrangement: primary offline, signing subkey in CI.
  This repository has no such workflow, and signs its host jar with a Java keystore through `jarsigner` (`gradle/packaging.gradle:57-89`),
  a different trust root from GPG.
- What this application embeds today: no public key. The release-key plugin only reads the user's GnuPG home and displays
  fingerprint, usage, expiry (`KeyRows.java:108-118`: `never`, a date, or `expired <date>`, compared with today in UTC); it has
  no revocation check and no test or code path for a renewed or replaced key (`git grep -i -E 'revok|renew|extend'
  plugins/release-key-plugin/src`). There is no precedent in the repository for an embedded verification key.
- What the application can learn at run time: nothing about a revocation, see "No path loads a plugin over the network" in 2.1. A
  revocation reaches an installation only with a new release of the application (or a new zip, which is checked by the very key
  in question).

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
- Cost: no new key; the signing at the release is one more `gpg --detach-sign` per zip, by the maintainer as for the installer
  checksums. The release workflows that exist are not touched. The embedded certificate is the one already on a key server.
- What it changes: the trust root of what runs inside users' JVMs is a key that, by the release-key record and the workflow files
  above, also lives in the CI secrets of 35 or more public repositories and signs every library artifact on Central. A leak of that
  secret from any one of them (a runner, an action, a fork's secret) lets its holder sign a plugin zip that this application
  accepts, and Maven Central artifacts cannot be told apart from plugin zips by the key alone. The blast radius of the CI
  secret and of the plugin trust are then one (NIST SP 800-57 Part 1 Rev. 5, section 5.2: "Limiting the use of a key limits the
  damage that could be done if the key is compromised").
- Risk: a compromise of the release subkey is a compromise of plugin trust at once; revoking it for Central (reason 2) also
  invalidates every installed plugin of every installed application until a release with a new key.
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
  the expiry fail (see below). Smaller: the signature carries only the 8-byte key id, so the application finds its candidate by that
  id, verifies, and compares the full fingerprint with its list (inference from the `gpg --list-packets` output above).

**Replacing, retiring and revoking an embedded key (lifecycle).** The embedded set is a list: for each entry a full fingerprint,
the certificate with its binding, and a state.
- Superseded or retired (RFC 9580 reason 1 or 3): a release adds the new key and marks the old one retired with a date. Signatures
  of the old key whose creation time lies before that date stay valid, as the RFC says, so the 14 shipped zips and any zip the user
  installed keep loading; a zip signed with the old key after the date is refused. The creation time is the signer's own claim, which
  is fine for a retired key and is why it is not enough after a compromise.
- Compromised (reason 2): the release drops the entry; no signature of that key stays valid ("all signatures created by that key
  are suspect"), so every zip it signed, the shipped ones included, must be re-signed with another key and shipped in the same
  release, and installed copies stop loading until the application is updated. An application that was not updated keeps trusting
  the key, which is the price of having no network path; the release text must say so.
- Expiry and renewal: the check at every start must not be "is the key valid now", or every installed plugin stops loading on the
  expiry date. It is "was the signature made inside the key's validity" (the signature creation time, which RFC 9580 section 5.2.3.11 calls
  "the time the signature was made", against the key creation time plus the expiration time of section 5.2.3.13), which keeps old zips valid and refuses a zip signed
  after the expiry. Extending an expiry makes a new self-signature; an application with the old certificate embedded does not see
  it, so the new certificate has to be in a host release before the first zip is signed after the old date. The release subkey
  expires 2028-10-09 (measured above); that date is the first one this work meets.
- The release that retires or drops a key also changes the embedded list, so it is the host's release, tested with throwaway keys
  (2.4), not with the real one.

**Recommendation (a suggestion): option 2, with the plugin subkey pinned by its full fingerprint, and the list as above.**
Reason: the measured fact that the release subkey sits in the CI secrets of at least 35 public repositories, which is the exact
situation NIST's key-usage rule is written for; a plugin runs with the user's rights inside the vault's JVM (2.1), so its trust
root should be a key that is never in a runner. Option 1 is the cheaper first step and stays defensible for a solo maintainer; it
revises the earlier lean of 2.3 only on that one fact, and 2.3's other recommendations stand. Best practice: NIST SP 800-57 Part
1 Rev. 5, section 5.2 (one key, one purpose; limit damage on compromise) and section 5.3.4 (a signature key's recipient-usage period
can run past its originator-usage period, which is the "signatures before the date stay valid" rule above); RFC 9580, sections
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
not know it runs in an isolated profile (`git grep -E "ISOLATED|mystic-crypt-ui-dev" origin/develop -- src/main/java` finds
nothing); the plugin folder comes from the configuration directory, which derives from `user.home` (the library's
`getConfigurationDirectory`; its source was not read, inferred from how the script and the e2e suites redirect it).

**What exists to tell a development build from a release build.** The version is `8.16-SNAPSHOT` in `gradle.properties:4`;
`releaseVersion = !version.endsWith("SNAPSHOT")` (`gradle/packaging.gradle:2`). The jar manifest carries `Implementation-Version`
(`packaging.gradle:26`); at run time only `ApplicationInfo.current()` reads it, falling back to the string `development` when the
code runs from `build/classes` (`ApplicationInfo.java:36,54-57`): a display value, no switch. Existing development switches in
host main code: `mystic.crypt.ui.config.dir` (relocates the configuration directory, `PluginSettings.java:58,77`),
`mystic.crypt.ui.startup.signal`, and for the command line only `mystic.crypt.ui.plugins.dir` (`MysticCryptUiCli.java:62,110`), which
nothing in the Makefile, Gradle files or workflows sets (tests only). The one in-plugin precedent is `-Dmystic.crypt.ui.menu.designer=true`,
which sits in the not-shipped menu-designer plugin (`MenuDesignerMenuContribution.java:60`) and is kept out of the installer by
`verify-installer.sh:125`.

**What can reach an installed application from outside.** The launchers lay down plain `java ... -jar mystic-crypt-ui.jar` with no
fixed properties and no wrapper (`src/main/resources/doc/start.sh`, `mystic-crypt-ui.desktop`, `start.bat`;
`src/main/izpack/install.xml:69-89`). A system property reaches it by editing the desktop entry or `start.sh` (both writable by the
user), by `JAVA_TOOL_OPTIONS` or `JDK_JAVA_OPTIONS` (general JDK behaviour, not run here), or by `java -D... -jar` by hand. So a
runtime switch alone cannot be "only in the isolated profile": the profile is `user.home`, and `-Duser.home` is one of the properties
that arrive the same way. The installed application also copies a user-chosen zip into the plugins folder
(`PluginsSettingsPanel.java:156`).

**What the check changes.** For `make run`: the copied zips are unsigned, so a fail-closed check (2.3) refuses every one of them (one per plugin, 15 plugins),
including the one the maintainer just built. For the tests (measured): 59 of 155 `src/e2eTest` classes plus one `src/e2eLock` class
install a built zip through one helper, `AbstractUiTest.installPluginRequiringItBuilt` (`AbstractUiTest.java:408-418`), from 15
zip constants (`:77-115`), with `user.home` redirected to a temp directory per test (`:139-141`); `PluginLoadingUiTest` and
`BuiltArchivesNameNoBuildMachineTest` read zips on their own; the two `zipTest` suites (lethenon, release-key) start a real zip;
the host unit and CLI tests build synthetic zips with `ZipOutputStream` (`PluginArchivesTest`, `PluginZipInstallationTest`,
`CommandLineWithInstalledPluginsTest`). Every one of them meets the check at the load path. The tests sign with throwaway keys
made at run time and hand the trust set to the host through the injectable set of 2.4; they never use the real key.

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
   Makefile or the workflows except `packaging.gradle:2`, measured with `git grep -n -i snapshot`).
3. **A build-time binding: the development code does not exist in a release jar.** The switch and the extra trust entry live in
   a class (or source set) that the host jar of a release version does not contain, the way the menu designer is not shipped
   (architecture.md, `verify-installer.sh:125`); the installer pack check would refuse a jar that contains it. Cost: the largest,
   a second source set or build variant and one more check. What it changes: nothing at run time in a release; the development
   build and the release differ in bytes, which `make verify-installer` can state.

**Recommendation (a suggestion): 3 for the binding, with the sub-question answered by (ii), and 2's version string as the
condition that decides which jar is a release.** Reason: neither the profile nor a property is a boundary on a machine where the
launchers pass `-D` freely, so the boundary has to be the artifact: a release jar that has no such code cannot be talked into the
mode. Option 2 alone is the cheap form and is acceptable if the maintainer prefers; it should not be taken for what option 3 gives.
(ii) keeps one code path and means the real key is never involved in development. Enforcement, as checks that fail the release
and not as promises: (a) `make verify-installer` gains two checks: the jar's `Implementation-Version` has no `SNAPSHOT`, and the
development class is absent (`unzip -l` of the extracted application jar); it already checks that the version equals
`gradle.properties` and runs `jarsigner -verify` (`verify-installer.sh:70-81`); (b) a unit test that builds the trust set the way
a release host does and proves an unsigned zip, a zip signed by an unknown throwaway key and the development key are all refused
when the version is a release version, and that the development key is accepted only when the development build says so; (c) the
e2e and `zipTest` suites run with throwaway keys they create, so no test depends on the development mode being on. Best practice:
Mozilla's add-on signing, where release and beta builds require signed add-ons and an unsigned one may be installed only in
Developer Edition, Nightly and ESR builds, after toggling `xpinstall.signatures.required` (extensionworkshop.com, "Signing and
distribution overview", read 2026-11-20; the switch is a property of the build channel, not of a profile); and Android, where the
build tools sign a debug build with a debug certificate generated on the developer's machine, "insecure by design", which the
Play Store does not accept for publishing (developer.android.com, "Sign your app", read 2026-11-20): a different key for development,
never the release key, and a store-side refusal of the debug one.

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

Proof of concept files, kept outside the repository: `mystic-crypt-ui-worktrees/_work/plugin-api/` (`jdeps/`, `poc/UnsignedZipPoc.java`
with `poc/unsigned-zip-poc.out`, `poc/RequiresCheck.java` with `poc/requires-check.out`, `host-method-calls.txt`).
