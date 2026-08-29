# Testplan mystic-crypt-ui

Stand: 2026-08-29
Tester: Aster

Dieser Plan hat denselben Aufbau wie der von adaptive-learner:

- **Teil A** ist das, was nur ein Mensch prüfen kann, nach Priorität sortiert.
- **Teil B** ist das, was automatisch läuft, als Nachschlagewerk - damit sichtbar
  ist, was **nicht** abgedeckt ist.

Jeder manuelle Fall wird mit OK oder BUG beantwortet. Bei BUG: Screenshot, was
gemacht wurde, was erwartet war. Daraus wird ein GitHub-Issue.

## Voraussetzung vor jeder Runde: den aktuellen Stand wirklich starten

Das hat mehrfach Tage gekostet, deshalb steht es hier ganz oben:

```bash
make build            # das jar, das `make run` startet
make plugins-install  # die Plugins, aus denen die Werkzeugfenster kommen
make run
```

- [ ] Nach `make plugins-install` liegen im Plugin-Verzeichnis **nur Zips**, keine
      entpackten Verzeichnisse: `ls ~/.config/mystic-crypt-ui/plugins`
- [ ] Ein Plugin-Fix wirkt erst nach einem neuen Plugin-Bau. Das Anwendungs-jar
      allein genügt nicht - Layout-Konstanten werden beim Übersetzen fest in die
      Plugin-Klasse geschrieben.

---

# TEIL A: MANUELLE TESTS

## A1. Datenbank-Round-Trip (Datenverlust-Gate)

Echte Daten, keine Testfixture. Nichts anderes wird freigegeben, solange das hier
nicht sitzt.

- [ ] Neue Datenbank anlegen, Master-Passwort vergeben, anmelden.
- [ ] Baum aufbauen: mehrere Ordner, in jedem Einträge mit Titel, Benutzername,
      Passwort, URL, Notizen.
- [ ] Einem Eintrag eigene Felder geben und eine Datei anhängen.
- [ ] Speichern, Anwendung schließen, neu starten, anmelden.
- [ ] Alles wieder da: Baumstruktur, jedes Feld, das eigene Feld, der Anhang.
- [ ] Anhang per Doppelklick ansehen, danach über "Save to" herausschreiben und
      die Datei außerhalb der Anwendung öffnen - Inhalt identisch.

## A2. Die Datenbank umziehen

- [ ] Datenbankdatei bei geschlossener Anwendung in ein anderes Verzeichnis
      verschieben, von dort öffnen, etwas ändern, speichern.
- [ ] Prüfen, dass die **neue** Datei geschrieben wurde und die alte nicht wieder
      auftaucht.

## A3. KeePass, mit einer echten Datei

- [ ] Eine echte `.kdbx` importieren (Passwort, Schlüsseldatei, beides).
- [ ] Stichprobe: drei Einträge mit dem stimmen, was KeePass zeigt.
- [ ] Export in eine neue `.kdbx`, diese Datei in **echtem KeePass** öffnen.

## A4. Mehrere Bildschirme

- [ ] Anwendung auf dem zweiten Bildschirm starten: der Anmeldedialog erscheint
      dort, und das Hauptfenster öffnet auf demselben Bildschirm.
- [ ] Aus dem Hauptfenster heraus Dialoge öffnen (Einstellungen, Eintrag
      bearbeiten, Zertifikat speichern): jeder erscheint auf dem Bildschirm, auf
      dem die Anwendung steht.

## A5. Schlüsselgenerator, alles durchprobieren

Das Fenster ist zum Durchprobieren gebaut, also wird es auch so getestet.

- [ ] Jeden Algorithmus im Auswahlfeld einmal erzeugen. Beide Textfelder füllen
      sich, keine leere Anzeige, keine stumme Fehlermeldung.
- [ ] Dabei darauf achten, dass **nichts im Fenster wandert oder schrumpft**.
- [ ] Mit RSA verschlüsseln und wieder entschlüsseln.
- [ ] Mit einem anderen Algorithmus: die beiden Knöpfe sind nicht drückbar und
      sagen im Tooltip warum.
- [ ] Privaten Schlüssel speichern, öffentlichen Schlüssel speichern, Zertifikat
      speichern - jeweils **ohne Endung** tippen und prüfen, dass die Datei die
      passende Endung bekommt.
- [ ] Speicherformat auf DER stellen und dasselbe noch einmal: Endung `.der`,
      Inhalt binär.
- [ ] Privaten Schlüssel mit Passwort speichern und die Datei mit demselben
      Passwort wieder öffnen (z.B. `openssl pkcs8 -inform DER`).

## A6. Einstellungen

- [ ] Ansicht auf Schreibtisch umstellen, Dialog schließen: die Anwendung steht
      sofort in dieser Ansicht.
- [ ] Anwendung neu starten: sie öffnet in derselben Ansicht.
- [ ] Aussehen und Sprache umstellen, neu starten, Einstellung ist noch da.

## A7. Tastatur

- [ ] Anmeldedialog: der Cursor steht sofort im Master-Passwort-Feld, Enter
      meldet an.
- [ ] Im Baum: Entf löscht, F2 benennt um, Einfg legt an - jeweils mit Rückfrage,
      wo es eine gibt.
- [ ] Suchfeld in der Werkzeugleiste: tippen springt zum ersten Treffer, Enter
      zum nächsten.

## A8. Plugins

- [ ] Alle Werkzeugfenster einmal öffnen und wieder schließen, ohne dass die
      Anwendung stehenbleibt.
- [ ] Reihenfolge der Einträge im Plugin-Menü bleibt nach einer Neuinstallation
      gleich.
- [ ] Ein Plugin in den Einstellungen abschalten, Anwendung neu starten: sein
      Menüpunkt ist weg. Wieder anschalten: er ist wieder da.

---

# TEIL B: WAS AUTOMATISCH LÄUFT

87 End-to-End-Tests fahren die echte Anwendung (AssertJ-Swing), dazu 65
Testklassen in den Plugins. Gefahren mit `./gradlew build`; die
End-to-End-Tests brauchen einen Bildschirm (Xvfb genügt).

### Anmelden und Datenbank

| End-to-End-Test | prüft |
|---|---|
| `CancelSignInUiTest` | cancel leaves application not signed in |
| `CreateDatabaseWithKeyFileUiTest` | create database with password and key file and sign in |
| `CreateNewDatabaseUiTest` | create first database through new flow and sign in |
| `LegacyDatabaseMigrationUiTest` | an old database opens and migrates when it is saved |
| `LockCancelUnlockUiTest` | cancelling unlock keeps workspace locked |
| `LockPreservesContentUiTest` | unlock restores the tree content |
| `LockWorkspaceUiTest` | lock hides content and unlock restores it with the master password |
| `MovedDatabaseUiTest` | a database that was moved is saved where it now is |
| `SaveAndReopenDatabaseUiTest` | imported data is still there after save and reopen |
| `SaveAsCancelUiTest` | cancelling save as writes nothing and keeps the target |
| `SaveAsUiTest` | save as writes the database to the chosen file and retargets the model |
| `SignInDialogUiTest` | ok button enables only after password and application file are provided |
| `SignInEnterSubmitUiTest` | pressing enter in the master password field signs in |
| `SigninFocusUiTest` | the dialog opens on the master password, and the caret follows it into the field |
| `WrongPasswordSignInUiTest` | wrong password shows error and does not sign in |

### Eintragsbaum

| End-to-End-Test | prüft |
|---|---|
| `AddEntryCancelUiTest` | cancelling add entry adds nothing |
| `AddEntryUiTest` | added entry shows up and survives reopen |
| `AddMultipleNodesUiTest` | all added nodes appear in the tree |
| `AddNodeCancelUiTest` | cancelling add node adds nothing |
| `AddNodeUiTest` | added node shows up in tree and survives reopen |
| `CopyEntryCredentialsUiTest` | copy username and password put them on the clipboard |
| `DeleteEntryCancelUiTest` | cancelling entry deletion keeps the entry |
| `DeleteNodeCancelUiTest` | cancelling node deletion keeps the node |
| `DuplicateNodeCancelUiTest` | cancelling duplicate node creates nothing |
| `DuplicateNodeUiTest` | duplicated node with entry survives reopen |
| `EditAndDeleteNodeUiTest` | node can be renamed and deleted through context menu |
| `EditDuplicateAndDeleteEntryUiTest` | entry can be edited duplicated and deleted |
| `EditNodeCancelUiTest` | cancelling edit node keeps the name |
| `MoveNodeUiTest` | moves nodes up and down and under another node |
| `SearchByEntryFieldUiTest` | search finds the node by an entry user name |
| `SearchByEntryTitleUiTest` | search finds the node by an entry title |
| `SearchUiTest` | search selects the matching node |
| `ToolbarSearchUiTest` | typing jumps to the first match, enter to the next, and around again |
| `TreeShortcutsUiTest` | F2 opens the selected node for editing |

### KeePass

| End-to-End-Test | prüft |
|---|---|
| `KeePassExportCancelUiTest` | cancelling kee pass export writes nothing |
| `KeePassExportUiTest` | exported kdbx file is readable with the chosen password |
| `KeePassImportCancelUiTest` | cancelling kee pass import imports nothing |
| `KeePassImportKeyFileUiTest` | importing a KeePass database protected by a key file shows entries in tree |
| `KeePassImportWrongPasswordUiTest` | a wrong KeePass password shows error and imports nothing |
| `KeePassImportUiTest` | importing a KeePass database shows entries in tree |
| `KeePassRoundTripPreservesEntryUiTest` | an entry survives export and reimport |
| `KeePassRoundTripUiTest` | exported database can be reimported |

### Schlüsselgenerator

| End-to-End-Test | prüft |
|---|---|
| `KeygenClearResetsUiTest` | clear resets the algorithm to rsa and reenables the key size |
| `KeygenCurveUiTest` | generates an ec key on the chosen curve through the ui |
| `KeygenEncryptUnavailableUiTest` | encrypt on a non rsa key is out of reach and says why |
| `KeygenEveryAlgorithmUiTest` | every algorithm the box offers generates a real key pair |
| `KeygenKeySizeLayoutUiTest` | changing the key size leaves the encrypt panel where it was |
| `KeygenLayoutStaysPutUiTest` | nothing in the window moves while it is being tried out |
| `KeygenPluginUiTest` | key generation generates and round trips encryption through the ui |
| `KeygenPqcPluginUiTest` | generates a post quantum key pair through the algorithm dropdown |
| `KeygenSavePublicKeyUiTest` | saves the generated public key to a file through the ui |
| `KeygenSavesRealArtifactsUiTest` | every file the window saves is a real artifact of the key pair it generated |

### Schlüsselspeicher und Zertifikate

| End-to-End-Test | prüft |
|---|---|
| `CertificateWizardUiTest` | certificate wizard opens and closes without killing the app |
| `KeyStoreImportUiTest` | imports a key and its certificate through the ui |
| `KeyStorePluginUiTest` | manages a key store through the ui |
| `PqcSignaturePluginUiTest` | signs and verifies and rejects a tampered message through the ui |
| `SignatureWithKeyFileUiTest` | signs a file with a key from disk and verifies it again |

### Verschlüsseln, Prüfsummen, Geheimnisse

| End-to-End-Test | prüft |
|---|---|
| `ChecksumAndMacUiTest` | computes a checksum and a mac through the ui |
| `ChecksumPluginUiTest` | verify checksum computes the file checksum through the ui |
| `ConversionDetectsAndConvertsUiTest` |  |
| `ConversionInvalidDerUiTest` | converting an invalid der file produces no pem file |
| `ConversionPluginUiTest` | convert der to pem writes the key as pem through the ui |
| `FileCryptFileUiTest` | encrypts and decrypts a file through the ui |
| `FileCryptNegativeUiTest` | two different passphrases are refused and a wrong one does not open the file |
| `FileCryptTextUiTest` | encrypts and decrypts a text through the ui |
| `KemDemoPluginUiTest` | runs a key encapsulation exchange through the ui |
| `KemHybridPluginUiTest` | runs a hybrid key encapsulation exchange through the ui |
| `KeyExchangeUiTest` | carries a message between the two sides through the ui |
| `SecretSharingUiTest` | splits a secret and rebuilds it from enough shares through the ui |

### Fenster, Menü, Einstellungen

| End-to-End-Test | prüft |
|---|---|
| `ConsolePluginUiTest` | console captures standard output through the ui |
| `GeneratePasswordUiTest` | generate fills both password fields and enables ok |
| `HelpAboutUiTest` | help info opens its dialog |
| `MenuDesignerPluginUiTest` | ask for the development tool |
| `MenuStructureUiTest` | the view mode is no longer offered in the menu bar |
| `SettingsEnablePluginUiTest` | a plugin can be disabled then enabled again |
| `SettingsInstallPluginUiTest` | installing a plugin from zip lists it in the plugins table |
| `SettingsUiTest` | settings dialog lists plugins and can disable them |
| `ViewModeSwitchUiTest` | the view chosen in the settings is the view the frame is in |

### Sonstiges

| End-to-End-Test | prüft |
|---|---|
| `NicheMenuFramesUiTest` | open private key opens its internal frame |
| `ObfuscationPluginUiTest` | simple obfuscation obfuscates and disentangles through the ui |
| `OpenDatabaseUiTest` | open database shows the key database internal frame |
| `OpenExistingDatabaseUiTest` | sign in to existing database with password |
| `OperatedObfuscationPluginUiTest` | operated obfuscation tool opens through the ui |
| `PasswordHashBcryptUiTest` | hashes with bcrypt and checks it again through the ui |
| `PasswordHashPbkdf2NegativeUiTest` |  |
| `PasswordHashPluginUiTest` | hashes and verifies a password through the ui |
| `PluginLoadingUiTest` | internal plugins load from zip and contribute their menu items |

## Was Teil B nicht kann

- Alles, was mit **echten** Daten des Anwenders zu tun hat: die eigene `.kdbx`,
  die eigene Datenbank, ein Umzug über Rechnergrenzen.
- Alles mit **mehreren Bildschirmen**: die Testumgebung hat einen.
- Ob eine Schriftgröße, ein Kontrast oder eine Fensteraufteilung **angenehm** ist.
- Ob ein anderes Programm die geschriebenen Dateien annimmt (KeePass, openssl,
  ein Browser für ein Zertifikat).
