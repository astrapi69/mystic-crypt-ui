# Manual Test Plan - mystic-crypt-ui

Status: 2026-11-13 (written for the 8.15 release, #948)
Tester: Aster

This plan has the same shape as adaptive-learner's
`docs/manual-tests/testplan-adaptive-learner-en.md`, the model the maintainer named:

- **PART A** is what only a human can check, sorted by priority.
- **PART B** is what runs automatically, as a reference - so what is NOT covered is visible.

Every manual case is answered with OK or BUG. On BUG: screenshot, what was done, what was
expected, and the case number (`TC-nnnn`) - that becomes a GitHub issue.

**The numbers are stable.** A `TS-` or `TC-` number is never reassigned and never moved: a
new case takes the next free number, and a case that falls away stays in place, marked
`withdrawn`, with one line saying why.

**Every case says how it is covered**: `automated by` names the test method that drives the
same path, written `Class#method`, and for a parameterized test the method and the case
(`Class#method` with the case named in parentheses); `manual only` says no machine does, with
the reason. A part of a case that no test drives is named as manual in the same line. No test counts in this document - the counts
come from the build output and age here (DC-010).

## Release smoke (the subset for a release candidate)

Before a tag, the maintainer runs this subset against the **built installer**, about 15
minutes (release-workflow.md step 5): **TS-0001** (database round-trip), **TS-0004** (lock
and unlock factors), **TS-0005** (protected values), **TS-0015** (the installer round) and
**TS-0017** (a vault of an earlier version). The suites below carry the marker
`(release smoke)`.

## Prerequisite before every round: really start the current build

This has cost days more than once, so it stands at the top. For a release smoke the
installer's launcher replaces `make run`.

```bash
make run   # builds every plugin zip and the jar, then starts them in ~/mystic-crypt-ui-dev
```

`make run` is `make run-isolated`: it builds the plugin zips (without their tests) and the
runnable jar before every start, replaces the plugins of its own profile
(`~/mystic-crypt-ui-dev`, `scripts/run-isolated.sh`) and never reads the installed release's
profile `~/.config/mystic-crypt-ui`. `make plugins-install` therefore does not belong in front
of it: it copies into that real profile, which `make run` does not read. `make start` starts
the last build without building anything, so after a code change it starts the OLD code.

- [ ] TC-0001 withdrawn: it checked that `make plugins-install` leaves only zips in the real
      profile, and `make run` no longer reads that profile. It replaces the plugins of its own
      profile on every start (`scripts/run-isolated.sh`), and a changed zip is unpacked again
      before the plugins load (#998). The risk it guarded, an old unpacked directory serving
      old code, is automated by
      `PluginArchivesTest#aDirectoryYoungerThanAChangedZip_getsTheNewContent`.
- [ ] TC-0002 withdrawn: it states a build fact, not a behaviour of the application. `make run`
      builds every plugin zip before each start (`Makefile`, target `run-isolated`); only
      `make start` starts the last build without building (see the prerequisite above).

---

# PART A: MANUAL TESTS

## TS-0001 Database round-trip (data-loss gate) (release smoke)

Real data, no test fixture. Nothing else is released while this does not hold.

- [ ] TC-0010 Create a new database, set a master password, sign in. - automated by
      `CreateNewDatabaseUiTest`
- [ ] TC-0011 Build a tree: several folders, in each one entries with title, user name,
      password, URL, notes. - automated by `AddMultipleNodesUiTest`, `AddEntryUiTest`
- [ ] TC-0012 Give one entry custom fields and attach a file. - automated by
      `AttachmentContentTest` (content round-trip); the dialog path manual only
- [ ] TC-0013 Save, close the application, restart, sign in. - automated by
      `SaveAndReopenDatabaseUiTest`
- [ ] TC-0014 Everything is back: tree structure, every field, the custom field, the
      attachment. - automated by `SaveAndReopenDatabaseUiTest`
- [ ] TC-0015 View the attachment with a double click, then write it out with "Save to" and
      open the file outside the application - content identical. - manual only

## TS-0002 The backup file (#847)

- [ ] TC-0020 Save an opened database: a sibling `.bak` file appears next to it. - automated
      by `VaultFileWriterTest`
- [ ] TC-0021 Close and reopen the database successfully: the `.bak` is gone. - automated by
      `VaultFileWriterTest`
- [ ] TC-0022 Make the saved file unreadable by hand (truncate a copy of the scenario, not
      your real database), open: the open fails and the `.bak` still holds the previous
      state and opens. - manual only

## TS-0003 Moving the database

- [ ] TC-0030 With the application closed, move the database file to another directory, open
      it from there, change something, save. - automated by `MovedDatabaseUiTest`
- [ ] TC-0031 Check that the **new** file was written and the old one does not reappear. -
      automated by `MovedDatabaseUiTest`

## TS-0004 Lock and unlock (the factors of the open database) (release smoke)

The unlock prompt asks for the factors the database was opened with, and the key file is
read from the disk again at unlock (8.15, GHSA-px49-m539-22h8).

- [ ] TC-0040 Database opened with a master password only: lock, unlock with the password.
      A wrong password is refused. - automated by
      `LockWorkspaceUiTest#lockHidesContentAndUnlockRestoresItWithTheMasterPassword` (lock,
      unlock) and
      `LockWorkspaceUiTest#wrongUnlockPasswordKeepsWorkspaceLockedThenCorrectOneUnlocks` (wrong
      password refused)
- [ ] TC-0041 Database opened with password AND key file: lock. The unlock prompt asks for
      BOTH; the key file field is preset with the path the database was opened with. -
      automated by
      `UnlockAsksForTheFactorsOfOpeningUiTest#aCombinedDatabase_needsItsKeyFileOnTheDisk` (both
      fields, the key file preset) and
      `UnlockAsksForTheFactorsOfOpeningUiTest#aCombinedDatabase_refusesEveryWrongFactorAlike`
      (both are needed)
- [ ] TC-0042 Same lock: move the key file away before unlocking. The unlock names the
      missing file and refuses; it does NOT create a file at the key file's path. Put the
      file back, unlock works. - automated by
      `UnlockAsksForTheFactorsOfOpeningUiTest#aCombinedDatabase_needsItsKeyFileOnTheDisk`
      (the key file gone before the lock) and
      `UnlockAsksForTheFactorsOfOpeningUiTest#aCombinedDatabase_whoseKeyFileGoesWhileLocked_staysLockedUntilItIsBack`
      (gone while locked)
- [ ] TC-0043 Same lock: point the prompt at a different key (same format). Refused. -
      automated by `UnlockAsksForTheFactorsOfOpeningUiTest#aCombinedDatabase_refusesEveryWrongFactorAlike`
- [ ] TC-0044 Database opened with a key file only: lock, unlock with the key file. -
      automated by
      `UnlockAsksForTheFactorsOfOpeningUiTest#aKeyFileOnlyDatabase_unlocksWithItsKeyFile` and
      `UnlockTakesTheKeyFileThatOpenedTheDatabaseUiTest#unlocksWithTheKeyFileThatOpenedIt`
      (cases "key file alone, PEM as Create key file writes it", "key file alone, PEM PKCS#8",
      "key file alone, PEM PKCS#1", "key file alone, DER PKCS#8", "key file alone, DER as the
      key generator writes PKCS#1")
- [ ] TC-0045 Cancel in the unlock prompt: the workspace stays locked, and
      "Unlock workspace..." brings the prompt back (#915). - automated by
      `LockCancelUnlockUiTest#cancellingUnlockKeepsWorkspaceLocked` (stays locked) and
      `LockCancelUnlockUiTest#afterCancel_unlockWorkspaceBringsThePromptBack` (the entry brings
      the prompt back)
- [ ] TC-0046 Locked and waited past "Close a locked database after minutes": the database
      is closed, opening again needs all factors. - automated by
      `LockedVaultClosesItselfUiTest#theFramesOwnWatchdogClosesALockedVaultAfterTheSettingsMinutes`
      (the application's own watchdog, the setting changed to 1 minute while the application
      runs, the real timer, and the reopening of a password plus key file database: the
      password alone is refused, the key file alone is refused, both factors open it);
      `LockedVaultClosesItselfUiTest#aLockedVaultIsClosedOnceItHasBeenLockedLongEnough`
      (what the close empties, over a test clock) and
      `LockedVaultClosesItselfUiTest#lockingKeepsPendingChanges_soTheTimedCloseLeavesThemAlone`
      (a dirty vault is left alone); the spinner of the setting in the settings dialog manual
      only: the tests save the value into the running application's settings file, as the
      dialog does

## TS-0005 Protected values in the "Properties" tab (#946) (release smoke)

- [ ] TC-0050 Open a vault entry with properties the entry marks as protected (a lethenon
      wallet's words, or a KeePass entry with a protected custom field): the table shows
      `********`, the same mask whatever the length. - automated by
      `PropertiesPanelProtectedValuesTest`
- [ ] TC-0051 Select and copy the masked row: what arrives on the clipboard is the mask, not
      the value. - automated by `PropertiesPanelProtectedValuesTest` (the mask sits in the
      model's `getValueAt`, which a copy reads)
- [ ] TC-0052 Open the property in the edit dialog: it opens masked and shows the value only
      on request. - automated by `PropertiesPanelProtectedValuesTest`
- [ ] TC-0053 Rename the protected property: the table keeps the new name masked. -
      automated by `PropertiesPanelProtectedValuesTest`

## TS-0006 KeePass, with a real file

- [ ] TC-0060 Import a real `.kdbx` (password, key file, both). - automated by
      `KeePassImportUiTest`, `KeePassImportKeyFileUiTest` (fixture files; the real file
      manual only)
- [ ] TC-0061 Spot check: three entries match what KeePass shows. - manual only
- [ ] TC-0062 Export to a new `.kdbx`, open that file in **real KeePass**. - automated by
      `KeePassRoundTripUiTest` (reimport here); the open in real KeePass manual only

## TS-0007 Creating a database does not overwrite (#917)

- [ ] TC-0070 **New...** in the sign-in dialog, pick an existing file: the application asks
      before writing over it; No leaves the file untouched. - automated by
      `NoSilentVaultOverwriteUiTest#newInTheSignInDialogOntoAnExistingVault`
- [ ] TC-0071 **Browse...** in "Create your master key", same check. - automated by
      `NoSilentVaultOverwriteUiTest#browseInCreateYourMasterKeyOntoAnExistingVault`

## TS-0008 The lethenon wallet (real chain where named)

- [ ] TC-0080 Create a wallet: step 1 says where the wallet is and that the vault has to be
      saved (#818). - automated by
      `LethenonBackupPanelBindingTest#afterCreate_stepOneSaysWhereTheWalletIs` (the text, at
      panel level in the plugin) and `LethenonWalletUiTest#aCreatedWallet_isAnEntryOfTheVault`
      (the create flow through the running app, the entry and the dirty vault); the text of step
      1 through the installed plugin manual only, until ccw adds the check to
      `LethenonWalletUiTest#aCreatedWallet_isAnEntryOfTheVault`
- [ ] TC-0081 Restore: the 24 words go into 24 numbered fields; a phrase pasted into any
      field fills all 24, numbers of a backup sheet (`1.`, `1)`, `01`, `1:`) read away
      (#804, #802). - automated by
      `WordGridTest#theFields_areNumberedInTheOrderOfTheBackupSheet` (24 numbered fields),
      `WordGridTest#aPastedPhrase_fillsAllTwentyFourFields` (the cases "pasted into field 1",
      "pasted into field 13" and "pasted into field 24", all 24 fields checked),
      `WordGridTest#aNumberedPaste_fillsTheFieldsWithTheWordsAlone` (format `1.` only, pasted
      into field 1), `WalletInputTest#aNumberedPhrase_isAccepted` (the cases "numbered with a
      dot, as the backup sheet shows them", "numbered with a bracket", "numbered bare and
      zero-padded", "numbered with a colon"; at parser level, not through the grid) and `LethenonWalletUiTest#aPhrasePastedWithNumbers_restoresTheWallet` (format `1.`
      through the running app, checks fields 1 and 24); the formats `1)`, `01`, `1:` pasted
      into the grid manual only, until ccw adds them as cases of
      `WordGridTest#aNumberedPaste_fillsTheFieldsWithTheWordsAlone`; all 24 fields through the
      running app manual only, until ccw extends
      `LethenonWalletUiTest#aPhrasePastedWithNumbers_restoresTheWallet`
- [ ] TC-0082 After the paste the words are off the clipboard: paste into another program
      gives nothing (#804). Something copied afterwards stays. - automated by
      `WordGridClipboardTest#aPhrasePastedFromTheClipboard_isGoneFromIt`,
      `WordGridClipboardTest#aWordPastedFromTheClipboard_isGoneFromIt` (plugin),
      `LethenonWalletUiTest#aPhrasePastedWithNumbers_restoresTheWallet` (the clipboard reads
      empty after the paste, through the running app),
      `WordGridClipboardTest#aPasteNotFromTheClipboard_leavesTheClipboardAlone` and
      `ClipboardClearWatchdogTest#clearNowIfStillHolding_leavesTheClipboard_whenItHoldsSomethingElse`
      (something else on the clipboard stays); the paste into another program manual only
      (no test reads the clipboard as a second program)
- [ ] TC-0083 Backup check with one wrong word: "One is wrong or two are swapped", the date
      of the last successful check stays (#804). - automated by
      `LethenonBackupPanelBindingTest#aCheckThatDoesNotMatch_saysWhatToLookFor` (the cases "en" and "de":
      the whole sentence; the words of another wallet),
      `BackupAssistantTest#aSuccessfulCheckRecordsItsDateAndAFailedOneChangesNothing` (one wrong
      word, the date of the last success stays; at backup level, no panel) and
      `LethenonVaultBackupTest#aCheck_recordsItsDayOnlyWhenItMatches` (vault level: another
      wallet's words record nothing, the right ones record the day); the case as written through
      the panel (one wrong word of the own wallet after a successful check, the sentence and the
      unchanged date) manual only, until ccw adds it to `LethenonBackupPanelBindingTest`
- [ ] TC-0084 Words kept in the vault are removed from the overview after a confirmation
      (#821). - automated by
      `LethenonWalletUiTest#wordsStoredInTheBackup_areRemovedFromTheOverview` (through the
      running app) and `RemoveWordsTest#cancelling_keepsTheWords` (Cancel, plugin)
- [ ] TC-0085 Overview without a chain: "No chain yet" with "Fetch the chain" and the node
      of the settings (#827). With a chain: it says when it was last synced and how old the
      last block is (#831). - automated by
      `FetchTheChainTest#withoutAChain_theOverviewShowsTheEmptyStateOfDraft06` and
      `FetchTheChainTest#theEmptyState_namesTheNodeOfTheSettings` (plugin),
      `LethenonWalletUiTest#aCreatedWallet_isAnEntryOfTheVault` (the label "No chain yet", the
      fetch button shown; the button's text "Fetch the chain" manual only), `LethenonWalletUiTest#afterASync_theOverviewSaysHowCurrentItIs_andTakesTheIncomingPaymentOver`
      (synced, age of the last block; a local node); the node line of the settings through the
      running app manual only, until ccw adds it to
      `LethenonWalletUiTest#aCreatedWallet_isAnEntryOfTheVault`; against a real node manual only
      (a public node is an external service)

## TS-0009 Multiple screens

- [ ] TC-0090 Start the application on the second screen: the sign-in dialog appears there,
      and the main window opens on the same screen. - manual only (the e2e harness starts one
      Xvfb screen, so a second screen cannot be tested; the geometry alone is automated by
      `ScreenPlacementTest#aWindowIsCentredOnTheScreenItIsGiven` and
      `ScreenPlacementTest#aWindowThatFillsAScreenGetsExactlyTheUsableArea`, and does not show
      where the windows land)
- [ ] TC-0091 Open dialogs from the main window (settings, edit entry, save certificate):
      each appears on the screen the application is on. - manual only (one Xvfb screen in the
      harness, as for TC-0090; `ScreenPlacementTest#aWindowIsCentredOnTheScreenItIsGiven` pins
      only the geometry; known defect, open as #63: every dialog shown through
      `JOptionPaneExtensions.getSelectedOption`, Edit Crypt Entry among them, is centred on the
      default screen whatever parent it is given (swing-base-components 5.2.1,
      `JOptionPaneExtensions.java:88`; read from the code, not run on two screens))

## TS-0010 Key generator, trying everything out

The window is built for trying out, so that is how it is tested.

- [ ] TC-0100 Generate once with every algorithm in the box. Both text areas fill, no empty
      display, no silent error. - automated by `KeygenEveryAlgorithmUiTest`
- [ ] TC-0101 While doing so: **nothing in the window moves or shrinks**. - automated by
      `KeygenLayoutStaysPutUiTest`; whether it LOOKS right manual only
- [ ] TC-0102 Encrypt and decrypt again with RSA. - automated by `KeygenPluginUiTest`
- [ ] TC-0103 With another algorithm: the two buttons are not pressable and the tooltip says
      why. - automated by `KeygenEncryptUnavailableUiTest`
- [ ] TC-0104 Save private key, public key, certificate - each time type a name **without an
      extension** and check the file gets the fitting one. - automated by
      `KeygenSavesRealArtifactsUiTest`
- [ ] TC-0105 Set the format to DER and do the same again: extension `.der`, binary
      content. - automated by `KeygenSavesRealArtifactsUiTest`
- [ ] TC-0106 Save the private key with a password and open the file again with that
      password (for example `openssl pkcs8 -inform DER`). - manual only (another program
      accepts the file)

## TS-0011 Settings

- [ ] TC-0110 Switch the view to desktop, close the dialog: the application stands in that
      view at once. - automated by `ViewModeSwitchUiTest`
- [ ] TC-0111 Restart: it opens in the same view. - manual only (a real restart)
- [ ] TC-0112 Change look and language, restart, the setting is still there. - manual only

## TS-0012 Keyboard

- [ ] TC-0120 Sign-in dialog: the caret is in the master password field at once, Enter
      signs in. - automated by `SigninFocusUiTest`, `SignInEnterSubmitUiTest`
- [ ] TC-0121 In the tree: Del deletes, F2 renames, Ins creates - each with its confirmation
      where there is one. - automated by `TreeShortcutsUiTest` (F2); Del and Ins manual only
- [ ] TC-0122 Search field in the toolbar: typing jumps to the first match, Enter to the
      next. - automated by `ToolbarSearchUiTest`

## TS-0013 Plugins

The canonical inventory is the `plugins:` target in the Makefile.

- [ ] TC-0130 Open and close every tool window once without the application freezing. -
      opening is automated per tool, each in a session of its own, by
      `PluginLoadingUiTest#internalPluginsLoadFromZipAndContributeTheirMenuItems` (the entries
      exist), `ObfuscationPluginUiTest#simpleObfuscationObfuscatesAndDisentanglesThroughTheUi`,
      `ChecksumPluginUiTest#verifyChecksumComputesTheFileChecksumThroughTheUi`,
      `ConversionPluginUiTest#convertDerToPemWritesTheKeyAsPemThroughTheWizard`,
      `ConsolePluginUiTest#consoleCapturesStandardOutputThroughTheUi`,
      `KeygenPluginUiTest#keyGenerationGeneratesAndRoundTripsEncryptionThroughTheUi`,
      `CertificateWizardUiTest#certificateWizardOpensAndClosesWithoutKillingTheApp`,
      `PasswordHashPluginUiTest#hashesAndVerifiesAPasswordThroughTheUi`,
      `KemDemoPluginUiTest#runsAKeyEncapsulationExchangeThroughTheUi`,
      `KeyExchangeUiTest#carriesAMessageBetweenTheTwoSidesThroughTheUi`,
      `PqcSignaturePluginUiTest#signsAndVerifiesAndRejectsATamperedMessageThroughTheUi`,
      `KeyStorePluginUiTest#managesAKeyStoreThroughTheUi`,
      `FileCryptTextUiTest#encryptsAndDecryptsATextThroughTheUi`,
      `SecretSharingUiTest#splitsASecretAndRebuildsItFromEnoughSharesThroughTheUi`,
      `LethenonPluginUiTest#thePlugin_verifiesAChainFile` and
      `ReleaseKeyPluginUiTest#showsTheStateOfAKeyThatGpgWrote`; closing is driven only for the
      certificate wizard (`CertificateWizardUiTest#certificateWizardOpensAndClosesWithoutKillingTheApp`),
      the conversion wizard's Finish
      (`ConversionPluginUiTest#convertDerToPemWritesTheKeyAsPemThroughTheWizard`) and the
      lethenon window (`LethenonPluginUiTest#thePlugin_remembersTheChainAndTheWalletThatWorked`);
      the closing of the other tools and the whole round over every tool in one session manual only
      (the tools open in different ways, as dialogs, internal frames or a docked console, and
      no test walks them as one round; a first generic attempt found no common signal to wait
      for)
- [ ] TC-0131 The order of the plugin menu entries is the same after a reinstall. -
      automated by
      `PluginMenuOrderSurvivesPluginToggleUiTest#aSavedPluginsMenuOrder_isTheSameAfterAReinstallAndARestart`
      (the saved order, Settings > Install from Zip... of the same zip, the order again, a
      restart, the order again) and
      `PluginMenuOrderSurvivesPluginToggleUiTest#aSavedPluginsMenuOrderSurvivesDisablingAndReenablingAPlugin`
      (disable and enable twice in one session)
- [ ] TC-0132 Disable a plugin in the settings, restart: its menu item is gone. Enable it
      again: it is back. - automated by
      `SettingsEnablePluginUiTest#aDisabledPlugin_isGoneFromTheMenuAfterARestart_andBackOnceEnabled`
      (disable, the entries gone at once and after the restart, enable, back after the next
      restart) and `SettingsEnablePluginUiTest#aPluginCanBeDisabledThenEnabledAgain` (the state
      column of the plugins table)
- [ ] TC-0133 Set "Lock after idle minutes" to 1, wait until the workspace locks, unlock:
      every plugin entry is offered again, and a plugin tool opens from its entry (#1006). -
      automated by `EverySignInOffersThePluginEntriesUiTest#everyWayBackOffersThePluginEntriesAgain`
      (case "the application's own idle lock, then unlock": the setting changed to 1 minute
      while the application runs, the frame's own timer on the system clock, about 60 s of real
      time; and, over a test clock, the case "the automatic lock, then unlock"); the spinner of
      the setting in the settings dialog manual only: the test saves the value into the running
      application's settings file, as the dialog does
- [ ] TC-0134 With a database open, open a second one with File > Open Database...: every
      plugin entry is offered again, and a plugin tool opens from its entry (#1006). -
      automated by `EverySignInOffersThePluginEntriesUiTest#everyWayBackOffersThePluginEntriesAgain`
      (case "open a second database while one is open", through the File menu item)
- [ ] TC-0135 File > Close Database, then File > Open Database...: every plugin entry is
      offered again, and a plugin tool opens from its entry (#1006). - automated by
      `EverySignInOffersThePluginEntriesUiTest#everyWayBackOffersThePluginEntriesAgain`
      (case "close the database, then open it again", through the File menu items Close
      Database and Open Database...)
- [ ] TC-0136 Cancel the sign-in at the start, then File > Open Database...: every plugin
      entry is offered, and a plugin tool opens from its entry (#1006). - automated by
      `EverySignInOffersThePluginEntriesUiTest#aSignInAfterACancelledStartOffersThePluginEntries`
      (the sign-in cancelled in its dialog, then the File menu item Open Database...)

## TS-0014 Help buttons and the wiki

- [ ] TC-0140 Every Help button in the sign-in dialogs opens a wiki page that exists, not a
      "page does not exist" editor (#868). - automated by `WikiLinksTest` (the targets
      exist); the browser opening manual only

## TS-0015 The installer (release rounds) (release smoke)

`make verify-installer PREVIOUS=<last release>` runs the measurable checks; this is the rest.

- [ ] TC-0150 Install from the izpack jar on a machine or fresh directory without a
      development build, start from the installed launcher. - manual only
- [ ] TC-0151 Help > About names the released version. - manual only
- [ ] TC-0152 Open a real database created with the previous release. - manual only

## TS-0016 The command line

- [ ] TC-0160 `--cli` answers with the library's root command help, no stack trace. -
      automated by `MysticCryptUiCliTest`
- [ ] TC-0161 One hash and one checksum command against a real file; verify the checksum
      against `sha256sum`. - manual only (another program checks the output)

## TS-0017 A vault of an earlier version (release smoke)

On a COPY of a real vault created with an earlier release, never the original.

- [ ] TC-0170 Open the copy, change an entry, save, lock, unlock, close, reopen: the change
      is there and nothing else moved. - automated by `VaultsOfSevenXSignInUiTest`,
      `VaultsOfEightWithAKeyFileSignInUiTest`, `LegacyDatabaseMigrationUiTest` (opening and
      migrating fixture vaults); the full chain on a real vault manual only

---

# PART B: WHAT RUNS AUTOMATICALLY

The end-to-end suites drive the real application (AssertJ-Swing) on the build's own Xvfb;
the plugins bring their own test classes. Run with `./gradlew build`; the suite inventory
and counts come from the build output (`executed in this build` lines) - this document
points there instead of carrying numbers that age.

- `src/e2eTest` (task `e2eTest`) - sign-in, tree, KeePass, key generator, key store and
  certificates, encrypt/checksum/secret plugins, windows and settings (the `*UiTest`
  classes; read the list with `ls src/e2eTest/java/io/github/astrapi69/mystic/crypt/ui/`).
- `src/e2eLock` (task `e2eLockTest`) - lock and unlock, including the factor cases of
  TS-0004 with made key files in every format the open dialog takes.
- `src/e2eKdbx` (task `e2eKdbxTest`) - the KDBX round trips measured against what KeePassXC
  wrote and what `keepassxc-cli` reads back (`KdbxRoundTripKeepsEveryFieldUiTest` and its
  neighbours, with their fixtures). The KeePass import and export UI tests with fixture files
  (`KeePassImportUiTest`, `KeePassImportKeyFileUiTest`, `KeePassExportUiTest`,
  `KeePassRoundTripUiTest`) are in `src/e2eTest`, not here.
- `src/test` (task `test`) - the unit suites, including `WikiLinksTest` (TS-0014's
  automated half), `VaultFileWriterTest` (TS-0002) and the vault format round trips.
- `make verify-installer PREVIOUS=<release>` - size, archive, checksums, signer, class
  major, mystic-crypt against Central, plugins, the jar tests, a start.

## What PART B cannot do

- Anything with the user's **real** data: your own `.kdbx`, your own database, a move
  across machines.
- Anything with **multiple screens**: the test environment has one.
- Whether a font size, a contrast or a window split is **comfortable**.
- Whether another program accepts the written files (KeePass, openssl, `sha256sum`, a
  browser for a certificate, a real lethenon node).
- The installed launcher on a machine without a development build (TS-0015), and a real
  restart of the installed application (TS-0011).
