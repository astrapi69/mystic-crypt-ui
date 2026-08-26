# Architecture

Developer documentation for `mystic-crypt-ui`: a Java 25 Swing desktop password manager and
crypto toolbox built on the astrapi69 library family (mystic-crypt, crypt-data, crypt-api)
and BouncyCastle, with a Gradle wrapper build, Spotless formatting, JUnit 5, PIT mutation
testing and an izpack installer.

The binding short form of these rules lives in `.claude/rules/architecture.md`. This page is
the long form: the same rules with the reasoning, the real classes behind them, and the
places where the code already follows them.

---

## 1. The three layers

```
  Swing UI            panels, dialogs, actions, the frame and its menus
        |  delegates to
  Support / worker    *Support, *Worker, the crypto/ package
        |  calls
  Libraries           mystic-crypt, crypt-data, crypt-api, BouncyCastle
```

### Layer 1: Swing UI

Panels, dialogs, actions, the application frame and its menu shell.

**What may not appear here:** business logic and crypto logic. A panel or an action decides
what to show and delegates the work. `SaveApplicationFileAction` is the shape to copy: it
holds no persistence code of its own, it calls the worker and then announces the state
change on the event bus.

```java
// src/main/java/io/github/astrapi69/mystic/crypt/action/SaveApplicationFileAction.java
@Override
public void actionPerformed(final ActionEvent e)
{
    ApplicationXmlFileStoreWorker
        .storeApplicationFile(MysticCryptApplicationFrame.getInstance().getModelObject());
    ApplicationEventBus.getSaveState().fireEvent(new EventObject<>(RenderMode.VIEWABLE));
}
```

### Layer 2: Support and worker

Classes named `*Support` or `*Worker`, plus the `crypto/` package. This is where the
behaviour lives, and it is the layer that carries the test weight.

**What may not appear here:** Swing types in the signatures. The layer has to be testable
without a display, which is also what makes it usable from the command line side. The
`PluginSettings` Javadoc states the intent for the whole layer:

> Deliberately free of Swing and of the application frame: the plugin panels use it, the
> settings dialog uses it, and the command line uses it, and it stays testable without a
> display.

Two classes in `crypto/` carry the crypto construction that the rest of the application
reuses:

- `PassphraseBox` wraps one construction: PBKDF2-HMAC-SHA256 over a fresh salt, then
  AES-GCM through the library's `KeyCommittingAeadEncryptor`, with magic, salt and
  iteration count fed in as associated data. Its constants (`SALT_LENGTH`, `ITERATIONS`,
  `KEY_LENGTH_BITS`) are the single place those values are defined.
- `KeyFiles` reads private keys, public keys and certificates out of whatever shape a file
  happens to be in (PKCS#8, the openssl PEM styles, raw DER, a public key inside a
  certificate), decoding through BouncyCastle so that the keys this application generates
  can be read back.

`PasswordVaultFormat` and the file-crypt plugin's `FileCryptSupport` both sit on
`PassphraseBox` and differ only in their magic bytes. That is the intended relationship, and
the reason the construction is not copied into either of them.

### Layer 3: Libraries

mystic-crypt, crypt-data, crypt-api and BouncyCastle. **All crypto primitives come from
here.** Nothing in this repository hand-rolls a primitive, a padding scheme, an IV or salt
derivation, or a constant-time comparison. Support classes wrap library calls; they do not
replace them.

### Where the layers are not clean yet

`NewPrivateKeyPanel` calls `KeyPairFactory.newKeyPair(...)`, `KeyModelExtensions` and
`PrivateKeyWriter.writeInPemFormat(...)` directly from the panel body. This is existing debt,
not a pattern to copy. The Boy Scout rule applies: a panel touched for another reason gets
its crypto moved down into a support class in the same change.

---

## 2. Where the astrapi69 library family sits

| Library | What it holds | Callable from |
|---|---|---|
| `crypt-api` | algorithm and key enums and constants: `KeySize`, `KeyPairGeneratorAlgorithm`, `AesAlgorithm`, `SunJCEAlgorithm`, `CompoundAlgorithm` | any layer, including model beans and panels (these are value types, not behaviour) |
| `crypt-data` | key and certificate factories, readers, writers, extensions and models: `KeyPairFactory`, `PrivateKeyReader`, `CertificateReader`, `KeyModel`, `X509CertificateV3Info` | support/worker layer and the `crypto/` package |
| `mystic-crypt` | the encryptors, decryptors and the picocli CLI: `KeyCommittingAeadEncryptor`, `PBEFileDecryptor`, `PrivateKeyHexDecryptor`, `PublicKeyHexEncryptor`, `MysticCryptCli` | support/worker layer and `cli/` |
| BouncyCastle (`bcprov-jdk18on`, `bcpkix-jdk18on`) | the provider and the PEM parsing under `KeyFiles` | the `crypto/` package; registered once as a JCE provider at startup |

The "callable from" column is the layer rule applied to what each library actually holds,
not a separately written rule: crypt-api is enums and constants, so it is a value type
dependency and may be referenced anywhere, while crypt-data and mystic-crypt carry behaviour
and belong below the UI layer.

Versions are declared in `gradle/libs.versions.toml` and wired in `gradle/dependencies.gradle`.
Read the current numbers there rather than from any document.

The provider is registered in exactly two entry paths, both guarded so it is added once:
`MysticCryptApplicationFrame.setSecurityProvider()` for the UI, and
`MysticCryptUiCli.execute(...)` for the command line, which never touches Swing.

### A namespace trap worth knowing on day one

The mystic-crypt **library** and this application share the base package
`io.github.astrapi69.mystic.crypt`. The library owns sub-packages including `aead`,
`algorithm`, `base`, `chainable`, `cli`, `core`, `decorator`, `file`, `gm`, `hex`, `io`,
`key`, `obfuscation`, `processor`, `provider`, `pw`, `secret`, `sha`, `simple`, `srp` and
`ssl`. This application adds its own sub-packages under the same base package.

`cli` exists on both sides. That is why `MysticCryptUiCli`, itself in
`io.github.astrapi69.mystic.crypt.cli`, refers to the library's root command by its fully
qualified name:

```java
// src/main/java/io/github/astrapi69/mystic/crypt/cli/MysticCryptUiCli.java
CommandLine commandLine = new CommandLine(
    io.github.astrapi69.mystic.crypt.cli.MysticCryptCli.class);
```

When an import looks like it should resolve locally and does not, check which side of the
split owns the package.

---

## 3. Why features live in plugins

A new feature ALWAYS belongs in an internal plugin under `plugins/{name}-plugin/`, unless it
touches the core. The core is a short, closed list:

- vault open, save and format
- sign-in
- the main frame and the menu shell
- the settings infrastructure

Everything else is a plugin. The reason is containment: a plugin brings its own submenu, its
own settings file, its own tests and, when it has something to offer there, its own CLI
commands. It can be disabled by the user without the application losing a feature it
depends on, and a broken plugin fails on its own rather than taking the application with it.

### The canonical inventory

The `plugins:` target in the `Makefile` is the inventory. Do not maintain a second list of
plugins anywhere, including in this document. A new plugin is added in ONE change to:

1. the `Makefile` (a `plugin-{name}` target, the `plugins:` aggregate and the
   `plugins-install` copy step),
2. the izpack installer configuration in `src/main/izpack/install.xml`,
3. the test wiring.

### How the host loads them

pf4j. The frame builds its plugin manager over a `plugins` directory nested inside the
application's configuration directory, then loads, starts and asks for extensions:

```java
// src/main/java/io/github/astrapi69/mystic/crypt/MysticCryptApplicationFrame.java
File pluginsDirectory = DirectoryFactory.newDirectory(getConfigurationDirectory(), "plugins");
pluginManager = new DefaultPluginManager(pluginsDirectory.toPath());
// ... later, in onAfterInitialize():
pluginManager.loadPlugins();
pluginManager.startPlugins();
((DesktopMenu)getMenu())
    .addPluginsMenu(pluginManager.getExtensions(PluginMenuContribution.class));
```

`make plugins-install` puts the built plugin zips where the running application looks for
them, which is `~/.config/mystic-crypt-ui/plugins` for a default installation.
`MysticCryptApplicationFrame.APPLICATION_NAME` is `"mystic-crypt-ui"`.

### The three extension points

All three live in `io.github.astrapi69.mystic.crypt.plugin.api` and extend pf4j's
`ExtensionPoint`.

| Interface | Contract |
|---|---|
| `PluginMenuContribution` | `List<JMenuItem> getMenuItems()` plus an optional `getMenuName()`. A non-blank name groups the items under the plugin's own submenu of the "Plugins" menu; `null` or blank adds them ungrouped. |
| `PluginSettingsContribution` | `getPluginId()`, `getDefaults()` and optional `getDisplayName()` / `getDescription(key)`. The declared defaults are the contract: an undeclared key is not offered for editing, a declared key always has a value. |
| `PluginCommandContribution` | `List<Object> getCommands()`. The objects are picocli commands, declared as `Object` on purpose so a menu-only plugin never needs picocli on its classpath. |

### Plugin anatomy

`plugins/file-crypt-plugin/` is the reference. It is its own Gradle build with its own
`settings.gradle`, and it compiles against the host from the local Maven repository, which
is why every plugin target depends on `publish-local`:

```
plugins/file-crypt-plugin/
  build.gradle                      own toolchain (JDK 25), all host deps compileOnly, pluginZip task
  settings.gradle                   rootProject.name = 'file-crypt-plugin'
  src/main/resources/plugin.properties   plugin.id / plugin.class / plugin.version / ...
  src/main/java/.../filecrypt/
    FileCryptPlugin.java                 the pf4j Plugin
    FileCryptSupport.java                the behaviour, no Swing
    FileCryptPanel.java                  the UI
    FileCryptPanelModel.java             the panel's model object
    FileCryptMenuContribution.java       @Extension, PluginMenuContribution
    FileCryptSettingsContribution.java   @Extension, PluginSettingsContribution
    FileCryptCommandContribution.java    @Extension, PluginCommandContribution
    FileCryptCommand.java                the picocli command
  src/test/java/.../filecrypt/           one test class per non-trivial class above
```

Two build details that bite if missed: the toolchain must be JDK 25 because the host's
classes are compiled for class file major 69, and pf4j must be declared as an
`annotationProcessor` as well as `compileOnly`, otherwise `@Extension` never writes
`META-INF/extensions.idx` and the plugin loads but contributes nothing.

### Plugin settings visibility

Every plugin setting is either editable in the plugin's settings UI or marked INTERNAL
(debug and tuning only). Hidden settings that change user-visible behaviour are forbidden,
and so are dead settings: when adding a key, verify the code reads it; when removing a
feature, remove its settings with it.

`PluginSettings` stores one properties file per plugin in a `plugin-settings` directory next
to the application settings. Reading always returns every declared key, so a plugin never
deals with a missing value; writing keeps only the declared keys, so a key dropped in a new
version does not linger. Plugin enable/disable state is not stored there at all, pf4j
persists that itself in the plugins directory.

---

## 4. The command line is a backlink to the library

`--cli` switches the application from the user interface to the command line. It is handled
before any window exists, so it works on a machine without a display:

```java
// src/main/java/io/github/astrapi69/mystic/crypt/StartMysticCryptApplication.java
public static void main(String[] args)
{
    if (MysticCryptUiCli.isCliInvocation(args))
    {
        System.exit(MysticCryptUiCli.execute(MysticCryptUiCli.stripCliArgument(args)));
    }
    MysticCryptApplicationFrame frame = new MysticCryptApplicationFrame();
    while (!frame.isVisible())
    {
        ScreenSizeExtensions.showFrame(frame);
    }
}
```

`MysticCryptUiCli.execute(...)` registers BouncyCastle, quiets the pf4j logging so that
redirecting standard output stays usable, starts the plugin manager over the same plugins
directory the installer writes to, and builds the command line as: the library's root
command, plus whatever the started plugins contribute.

### What this forbids

- **No command is reimplemented here.** The library's `MysticCryptCli` is instantiated as it
  is, so every command the library gains is available the moment the dependency is bumped.
- **New CLI capability is implemented in the mystic-crypt LIBRARY first**, in its own
  repository on its own release cycle, and only then consumed here.
- **Never fork or duplicate a library command into the UI**, and never change library code
  from inside this repository.
- A plugin contributes only what the library does not cover. `PluginCommandContribution`
  exists for that gap, not as a place to shadow library commands.

Two deliberate design points in `MysticCryptUiCli` are worth preserving when it is touched:
a plugin whose `getCommands()` throws is skipped with a message on standard error rather
than taking the whole command line down, and the failure to stop plugins cleanly at the end
does not turn a successful run into a failed one.

### The knobs it reads

| System property | Effect |
|---|---|
| `mystic.crypt.ui.plugins.dir` | points at the plugins directory; without it the installed one under the user's home directory is used |
| `mystic.crypt.ui.cli.verbose` | set to `true` to leave the plugin system's logging as it is instead of turning it down to warnings |

---

## 5. Every panel holds its state in a model

Swing components in this application are model backed. A panel extends `BasePanel<T>` over
one model object, and its inputs are the `JM*` components from swing-model-components
(`JMTextField`, `JMPasswordField`, `JMCheckBox`, `JMComboBox`, `JMSpinner`), each bound to
one property of that object.

```java
// src/main/java/io/github/astrapi69/mystic/crypt/panel/pw/GeneratePasswordPanel.java
spnPasswordLength = new JMSpinner<Integer>();
((JMSpinner)spnPasswordLength).setPropertyModel(LambdaModel
    .of(getModelObject()::getPasswordLength, getModelObject()::setPasswordLength));

cbxLowercase = new JMCheckBox();
((JMCheckBox)cbxLowercase).setPropertyModel(
    LambdaModel.of(getModelObject()::isLowercase, getModelObject()::setLowercase));
```

The model object itself is a plain Lombok bean, `GeneratePasswordModelBean` in this case,
with one field per input. A combo box over an enum uses `EnumComboBoxModel` rather than a
hand built array:

```java
// src/main/java/io/github/astrapi69/mystic/crypt/panel/privatekey/NewPrivateKeyPanel.java
cmbKeySize.setModel(new EnumComboBoxModel<>(KeySize.class, modelObject.getKeySize()));
```

### What this replaced

Reading values out of the components when a button is pressed. That pattern scatters the
panel's state across the widgets and makes it unreachable from anywhere else: from a test,
from the command line side, from a second panel. With a model object the panel's state is
readable at any moment and every edit updates it, which is what lets the same state be
driven from a headless test or reused by a CLI command.

**New panels are written this way.** A panel touched for another reason is converted while
it is open (Boy Scout).

One detail that has already caused a broken UI test: when a field is re-created in order to
bind it, the lookup name used by the AssertJ-Swing tests has to be assigned again
afterwards, as `NewPrivateKeyPanel` does with `setName("txtFilenameOfPrivateKey")`.

---

## 6. Map of the packages under `src/main`

Base package: `io.github.astrapi69.mystic.crypt`.

| Package | One line |
|---|---|
| (root) | The application shell: `StartMysticCryptApplication` (entry point and `--cli` fork), `MysticCryptApplicationFrame` (the frame, the pf4j plugin manager, the security provider), `ApplicationPanel`, `ApplicationToolbar`, `ApplicationModelBean`, `DesktopMenu`, `MenuId` and `Messages`. |
| `action` | The `AbstractAction` implementations behind menu and toolbar entries: new, save and save-as for the vault, search, the database tree frame, lock workspace, KeePass import and export, open private key, the settings frame and full screen. |
| `app.file.xml` | Vault persistence: `PasswordVaultFormat` (the MCRDB2 on-disk format), `ApplicationXmlFileReader`, `ApplicationXmlFileStoreWorker` and `ApplicationXmlFileFactory`. |
| `button.state` | Reusable state machines that tie a `JButton`'s enabled state to another component, for example a table selection. |
| `cli` | `MysticCryptUiCli`: the headless entry point that assembles the library's root command with the plugins' contributed commands. |
| `crypto` | `PassphraseBox` (the one passphrase-to-AEAD construction) and `KeyFiles` (reading keys and certificates in any of the shapes they arrive in). |
| `eventbus` | `ApplicationEventBus`: the process-wide event sources for save state and navigation state. |
| `keepass` | Conversion between a KeePassJava2 `SimpleDatabase` and this application's own tree model, including the KeePass metadata that has no dedicated field on the library's tree types. |
| `menu` | `MenuLayoutSupport`: exports the menu bar to xml with an explicit action id per item and rebuilds it from such a layout, disabling items whose action id is unknown instead of failing the whole menu. |
| `panel.certificate` (+ `.wizard`) | The certificate panels and the older three-step certificate creation wizard. |
| `panel.dbtree` | The vault's tree-with-content view: the tree panel, the entry panel and tabbed panel, the table models and the attachment panel. |
| `panel.keepass` | The import and export panels for KeePass databases. |
| `panel.keygen` | `EnDecryptPanel`, the two-text-area encrypt/decrypt panel; it is a `BasePanel<Pair<String, String>>` and holds no crypto of its own. |
| `panel.privatekey` | Creating, saving and viewing a private key, together with the button state machine that decides when saving is possible; `PrivateKeyModelBean` carries the library's `PrivateKeyHexDecryptor` and `PublicKeyHexEncryptor`. |
| `panel.properties` | The properties view and its new-entry panel. |
| `panel.pw` | The password generator: dialog, panel, model bean and the generation helper. |
| `panel.signin` | Sign-in: master password with and without a key file, the new-vault dialogs, `PasswordType`, `SignInType` and the OK-button state machine under `button.state.ok`. |
| `panel.table` | The generic new-table-entry panel and its model. |
| `plugin.api` | The three pf4j extension points a plugin implements: `PluginMenuContribution`, `PluginSettingsContribution`, `PluginCommandContribution`. |
| `settings` | `MysticCryptSettings` (application settings as JSON), `PluginSettings` (one properties file per plugin, Swing-free) and the settings dialog tabs: Plugins, Plugin settings, General. |
| `wizard` (+ `.model`, `.state`) | The certificate wizard: the wizard panels, the `CertificateInfoModel` family of model objects, the wizard state, and `CertificateInfoModelToX509` which turns the model into the library's X.509 info type. |

Two notes on that table. `wizard/CertificateWizardPanelTest.java` sits in `src/main` and is a
`main`-method demo launcher for the wizard, not a JUnit test, despite the name. And
`panel.certificate.wizard` and `wizard` both contain a `CertificateWizardContentPanel`; check
the package before assuming which one an import means.

---

## 7. Changing an architectural decision

Before implementing a larger architectural decision, check the open issues and the recent PRs
for already-planned work in the area (`.claude/rules/architecture.md` also names a ROADMAP;
there is no `ROADMAP.md` at the repository root at the time of writing). On a conflict between an instruction and
documented planning: stop and ask which applies. Never build a parallel system that is
already slated for replacement.

Changing the vault format, the plugin mechanism or the XML persistence requires asking
first. Any change to the vault format or to file encryption is proven by a real round-trip
through the real application path, not by unit tests alone, including the legacy migration
path when it is touched. See `.claude/rules/quality-checks.md` for the full acceptance rule
and `.claude/rules/lessons-learned.md` for why it exists.

---

## 8. Where to read next

- `CLAUDE.md` for the short form and the Make targets.
- `.claude/rules/architecture.md` for these rules in binding form.
- `.claude/rules/coding-standards.md` for the Java, naming, error handling and git rules.
- `.claude/rules/quality-checks.md` for the test pyramid, coverage targets per module type
  and the round-trip acceptance duty.
- `.claude/rules/lessons-learned.md` for the pitfalls that produced several of the rules
  above, including the Xvfb harness the Swing e2e suite needs.
- `gradle/testing.gradle` for why the test task uses `forkEvery = 1`: the UI tests drive a
  process-wide singleton frame and register listeners on the static event bus, so each test
  class needs a fresh JVM.
