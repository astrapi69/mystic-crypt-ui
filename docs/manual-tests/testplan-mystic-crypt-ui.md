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

**No case names an advisory id or describes the defect of a fix under an unpublished advisory**
(AG-117): such a case enters the plan once the advisory is published. TS-0004 names
GHSA-px49-m539-22h8, published on 2026-11-18, and may stay.

**Every case says how it is covered**: `automated by` names the test method that drives the
same path, written `Class#method`, and for a parameterized test the method and the case
(`Class#method` with the case named in parentheses); `manual only` says no machine does, with
the reason. A part of a case that no test drives is named as manual in the same line. No test counts in this document - the counts
come from the build output and age here (DC-010).

## Release smoke (the subset for a release candidate)

Before a tag, the maintainer runs this subset against the **built installer**, about 25
minutes (release-workflow.md step 5): **TS-0001** (database round-trip), **TS-0004** (lock
and unlock factors), **TS-0005** (protected values), **TS-0015** (the installer round),
**TS-0017** (a vault of an earlier version) and **TS-0018** (the start). The suites below
carry the marker `(release smoke)`. The 15 minutes were the estimate for the five suites
before 8.16; the added ten are an estimate for the four cases of TS-0018 (one first start, four
starts with a saved look, one start from the menu), not a measured time: replace it with the
time of the first 8.16 round.

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
      `CreateNewDatabaseUiTest#createFirstDatabaseThroughNewFlowAndSignIn` (New..., the
      password and its repeat typed in the dialog, the file written, the sign-in with it)
- [ ] TC-0011 Build a tree: several folders, in each one entries with title, user name,
      password, URL, notes. - automated by
      `DatabaseRoundTripUiTest#aFullTreeComesBackAfterSaveEndAndRestart` (three folders,
      two entries each, title, user name, password with its repeat, URL and notes typed in the
      dialog; the expiry date is not driven by any test, manual only)
- [ ] TC-0012 Give one entry custom fields and attach a file. - automated by
      `EditEntryCancelUiTest#ok_takesCustomFieldsAndAnAttachedFile_andAReopenGivesThemBack`
      (two custom fields, a UTF-8 text file and a file of all 256 byte values through "Add
      File" and the file chooser, OK, save, restart, names and SHA-256 compared)
- [ ] TC-0013 Save, close the application, restart, sign in. - automated by
      `DatabaseRoundTripUiTest#aFullTreeComesBackAfterSaveEndAndRestart` (save, the
      application's own end path with the exit injected, an in-JVM restart through
      `AbstractUiTest#shutdownApplication`, sign-in), `SaveAndReopenDatabaseUiTest#importedDataIsStillThereAfterSaveAndReopen`
      and `AddEntryUiTest#addedEntryShowsUpAndSurvivesReopen` (same in-JVM restart);
      `EndingTheApplicationErasesTheVaultUiTest#endingACleanVaultOverwritesItWithoutAsking`
      runs the end path alone, and `CloseAndReopenDatabaseUiTest#aClosedVaultOpensAgainWithItsContent`
      closes the database, not the application, and opens it again in the same run; the
      end of the real process and a real new start: manual only (a test cannot end its own
      JVM and observe a successor, #288)
- [ ] TC-0014 Everything is back: tree structure, every field, the custom field, the
      attachment. - automated by
      `DatabaseRoundTripUiTest#aFullTreeComesBackAfterSaveEndAndRestart` (folders by where they hang,
      entries with title, user name, password, URL and notes, both custom fields, both
      attachments by name and SHA-256, compared with what was typed; the expiry date set in
      the dialog: manual only), and in the MCRDB2 file after an import and a save
      `KdbxRoundTripKeepsEveryFieldUiTest#theGroupStructureIsUnchanged`, `#theTextFieldsSurvive`,
      `#customPropertiesSurviveIncludingTheProtectedOne` and `#theAttachmentSurvivesWithItsContent`
- [ ] TC-0015 View the attachment with a double click, then write it out with "Save to" and
      open the file outside the application - content identical. - automated by
      `DatabaseRoundTripUiTest#theAttachmentIsShownAndWrittenOutByteForByte` (after a save
      and a restart: the double click shows the text, "Save to" proposes the attachment's
      name, the written text and binary files equal their sources byte for byte); at panel
      level `AttachmentPanelSaveToTest#saveTo_writesTheAttachmentAsItIs` and
      `TableDoubleClickTest#aDoubleClickOnAnAttachmentShowsWhatIsInIt`; opening the written
      file in another program and whether the display reads well to a human: manual only

## TS-0002 The backup file (#847)

- [ ] TC-0020 Save an opened database: a sibling `.bak` file appears next to it. - automated
      by `BackupFileUiTest#aSaveOfAPasswordDatabaseKeepsThePreviousStateAsTheSiblingBackup`
      (password database) and
      `BackupFileUiTest#aSaveOfAKeyFileDatabaseKeepsThePreviousStateAsTheSiblingBackup`
      (key file database) and
      `BackupFileUiTest#aSaveOfAPasswordAndKeyFileDatabaseKeepsThePreviousStateAsTheSiblingBackup`
      (password and key file database); all three check that the `.bak` holds the previous
      bytes and that nothing else lies next to the database; the writer itself is pinned by
      `VaultFileWriterTest#aReplacedDatabaseKeepsItsBackup`
- [ ] TC-0021 Close and reopen the database successfully: the `.bak` is gone. - automated by
      the second half of the same three `BackupFileUiTest` methods (an in-JVM restart through
      `AbstractUiTest#shutdownApplication`, then sign in again through the dialog: the `.bak` is
      gone and the saved node is in the tree); the reader's removal is pinned by
      `VaultFileWriterTest#aSuccessfulOpenRemovesTheBackup`; File > Close and then reopen is
      not driven for the `.bak`: manual only
- [ ] TC-0022 Make the saved file unreadable by hand (truncate a copy of the scenario, not
      your real database), open: the open fails and the `.bak` still holds the previous
      state and opens. - automated by
      `VaultFileWriterTest#aTruncatedDatabaseFailsToOpenAndTheBackupStillOpensWithThePreviousState`
      (a database saved twice and cut to half its length in code, opened through the reader the
      sign-in uses, then the `.bak` copied over it and opened through the same reader);
      manual only: truncating the file in the file manager, the sign-in dialog's refusal of
      the truncated file, and opening the restored `.bak` through the dialog

## TS-0003 Moving the database

- [ ] TC-0030 With the application closed, move the database file to another directory, open
      it from there, change something, save. - automated by
      `MovedDatabaseUiTest#aDatabaseThatWasMovedIsSavedWhereItNowIs` (case "moved into another
      directory"; the case "renamed in the same directory" covers a rename in place)
- [ ] TC-0031 Check that the **new** file was written and the old one does not reappear. -
      automated by `MovedDatabaseUiTest#aDatabaseThatWasMovedIsSavedWhereItNowIs` (both
      cases: the moved file changes, the old path does not exist, the change is there after
      a reopen)

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
      `AProtectedPropertyOfAnImportedEntryIsMaskedInTheEditDialogUiTest#theTableOfTheEditDialogMasksTheProtectedValue`
      (a KeePassXC entry opened in the real Edit Crypt Entry dialog, which works on
      `EntryEditing.copyOf`) and `PropertiesPanelProtectedValuesTest#theMaskIsTheSameWhateverTheLength`
      (the cases "an empty value", "one character", "the words of a wallet", "ten thousand
      characters"; the lethenon wallet is built in memory there, not opened through the dialog);
      manual only: a lethenon wallet's words opened through the dialog, since the lethenon plugin
      and its tests belong to the session ccw (TS-0008)
- [ ] TC-0051 Select and copy the masked row: what arrives on the clipboard is the mask, not
      the value. - automated by `PropertiesPanelProtectedValuesTest#theCopyOfAMaskedRow_putsTheMaskOnTheClipboard`
      (the table's own transfer handler into a clipboard of the test's own) and
      `AProtectedPropertyOfAnImportedEntryIsMaskedInTheEditDialogUiTest#theTableOfTheEditDialogMasksTheProtectedValue` (the real dialog's table
      copied through its transfer handler into a clipboard of the test's own); manual only: Ctrl+C on the desktop's
      clipboard, which no test touches
- [ ] TC-0052 Open the property in the edit dialog: it opens masked and shows the value only
      on request. - automated by
      `AProtectedPropertyOfAnImportedEntryIsMaskedInTheEditDialogUiTest#thePropertyDialogOpensTheProtectedValueMaskedAndShowsItOnRequest`
      (through the real Edit Crypt Entry dialog) and
      `PropertiesPanelProtectedValuesTest#theDialogOfAProtectedProperty_opensMaskedAndShowsTheValueOnRequest`
      (the property panel alone)
- [ ] TC-0053 Rename the protected property: the table keeps the new name masked. -
      automated by `AProtectedPropertyOfAnImportedEntryIsMaskedInTheEditDialogUiTest#aRenamedProtectedPropertyStaysMasked`
      (through the real dialog, then OK, save and reopen) and
      `PropertiesPanelProtectedValuesTest#renamingAProtectedProperty_movesTheMarkToTheNewName`
      (the panel alone)

## TS-0006 KeePass, with a real file

- [ ] TC-0060 Import a real `.kdbx` (password, key file, both). - automated by
      `KeePassImportUiTest#importKeePassDatabaseShowsEntriesInTree` (password),
      `KeePassImportKeyFileUiTest#importKeePassDatabaseProtectedByAKeyFileOnlyShowsEntriesInTree`
      (key file alone) and
      `KeePassImportKeyFileUiTest#importKeePassDatabaseProtectedByAKeyFileShowsEntriesInTree`
      (password and key file); the files are built by the library or are fixtures, so the real
      file of one's own with its real key file: manual only (its secrets belong in no test)
- [ ] TC-0061 Spot check: three entries match what KeePass shows. - automated by
      `KeePassImportMatchesWhatKeePassXcReportsUiTest#everyImportedEntryEqualsWhatKeePassXcReadsFromTheSameFile`
      (a KeePassXC-written file with three entries in three groups, compared with what
      keepassxc-cli reads from it); the look at KeePass's own window with the real file: manual only
- [ ] TC-0062 Export to a new `.kdbx`, open that file in **real KeePass**. - automated by
      `KdbxRoundTripKeepsEveryFieldUiTest#theTextFieldsSurvive`, `#customPropertiesSurviveIncludingTheProtectedOne`,
      `#theAttachmentSurvivesWithItsContent`, `#theGroupStructureIsUnchanged`, `#theHistoryIsCarriedThrough`,
      `#identifiersSurvive`, `#allFourTimestampsAndTheExpiryFlagSurvive`, `#groupTimesSurvive`,
      `#iconIndicesSurvive`, `#theExportedHeaderIsNotTheLibraryPlaceholder` and
      `#theBinaryPoolIsNoLongerWrittenTwice` (the exported file read by KeePassXC through
      keepassxc-cli), `KeePassExportUiTest#exportedKdbxFileIsReadableWithTheChosenPassword` (read back with
      KeePassJava2, this application's own library) and
      `KeePassRoundTripUiTest#exportedDatabaseCanBeReimported` (re-import here); the open in
      KeePass 2.x itself: manual only (a Windows program, not on the runner)

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
      display, no silent error. - automated by
      `KeygenEveryAlgorithmUiTest#everyAlgorithmTheBoxOffersGeneratesARealKeyPair` (walks the
      items of the box, checks the selection took, reads both keys back with the library
      readers and checks the algorithm the key names), `KeygenPqcPluginUiTest#generatesAPostQuantumKeyPairThroughTheAlgorithmDropdown`,
      `KeygenCurveUiTest#generatesAnEcKeyOnTheChosenCurveThroughTheUi`
- [ ] TC-0101 While doing so: **nothing in the window moves or shrinks**. - automated by
      `KeygenLayoutStaysPutUiTest#nothingInTheWindowMovesWhileItIsBeingTriedOut` (x and y of the
      four text panes, widths, the height of the two key panes, and the two encrypt panes never
      losing height from one state to the next, after picking every algorithm and key size and after
      generating with every algorithm and with RSA 1024 and 2048),
      `KeygenEveryAlgorithmUiTest#everyAlgorithmTheBoxOffersGeneratesARealKeyPair` (left edges
      after each generation); whether it LOOKS right manual only. Measured: the two encrypt panes
      grow from 148 to 168 pixels at the first generation, which is not a shrink
- [ ] TC-0102 Encrypt and decrypt again with RSA. - automated by
      `KeygenPluginUiTest#keyGenerationGeneratesAndRoundTripsEncryptionThroughTheUi` (RSA,
      through the running application); EC (ECIES) only at panel level,
      `KeyGenerationButtonsTest#anEcKeyPairDrivesTheDemo`
- [ ] TC-0103 With an algorithm that cannot encrypt (X25519, X448, ML-KEM-768, ML-DSA-65; RSA and
      EC can): the two buttons are not pressable and the tooltip says why. - automated by
      `KeygenEncryptUnavailableUiTest#encryptAndDecryptAreOutOfReachAndSayWhyForAnAlgorithmThatCannotEncrypt`
      (X25519, X448, ML-KEM-768, ML-DSA-65 alternating with RSA and EC, a new key on screen each
      time, both buttons, text in both areas, tooltip and reason label)
- [ ] TC-0104 Save private key, public key, certificate - each time type a name **without an
      extension** and check the file gets the fitting one. - automated by
      `KeygenSavesRealArtifactsUiTest#everyFileTheWindowSavesIsARealArtifact` (private key, key
      with a password, certificate), `KeygenSavePublicKeyUiTest#savesTheGeneratedPublicKeyToAFileThroughTheUi`
      (public key)
- [ ] TC-0105 Set the format to DER and do the same again: extension `.der`, binary
      content. - automated by
      `KeygenSavesRealArtifactsUiTest#everyFileTheWindowSavesIsARealArtifact` (private key and
      public key and certificate as DER, read back with `CertificateFactory`),
      `SaveFormatTest#aPrivateKeySavedAsPemIsTextAsDerItIsNot`,
      `SaveFormatTest#aPublicKeySavedAsPemIsTextAsDerItIsNot`,
      `SaveFormatTest#aCertificateSavedAsPemIsTextAsDerItIsBinary`. The key saved with a password
      is always DER, the one decided exception (#1050)
- [ ] TC-0106 Save the private key with a password and open the file again with that
      password (for example `openssl pkcs8 -inform DER`). - automated by
      `KeygenSavesRealArtifactsUiTest#everyFileTheWindowSavesIsARealArtifact` (the library's
      reader), `ProtectedKeyOpensElsewhereTest#theJdkAloneDecryptsTheFileWithThePasswordAndGetsTheGeneratedKey`
      (the JDK alone), `ProtectedKeyOpensElsewhereTest#opensslPkcs8OpensTheFileAndPrintsTheGeneratedKey`
      (`openssl pkcs8 -inform DER`, password on standard input, the printed key compared with the
      generated one; fails on CI when openssl is missing, skips locally); only a program other than these stays manual

## TS-0011 Settings

- [ ] TC-0110 Switch the view to desktop, close the dialog: the application stands in that
      view at once. - automated by
      `ViewModeSwitchUiTest#theViewChosenInTheSettingsIsTheViewTheFrameIsIn`
- [ ] TC-0111 Restart: it opens in the same view. - automated by
      `ViewModeSwitchUiTest#theChosenViewIsTheViewTheApplicationOpensInNextTime` (the file
      holds the view and the restart is `AbstractUiTest#shutdownApplication` plus a new
      sign-in, in the same JVM); a real restart of the installed application: manual only
- [ ] TC-0112 Change the look, restart: the look is still there (no window offers the language
      since #885). - automated by
      `LookAndFeelSurvivesRestartUiTest#theLookChosenInTheSettingsIsStillActiveAfterARestart`
      (chosen in the settings dialog, restart in the same JVM) and
      `SignInDialogLookAndFeelUiTest#thePersistedLookAndFeelIsAlreadyActiveOnTheSignInDialog`
      (a persisted look is active at sign-in); a real restart of the installed application:
      manual only

## TS-0012 Keyboard

- [ ] TC-0120 Sign-in dialog: the caret is in the master password field at once, Enter
      signs in. - automated by
      `SigninFocusUiTest#afterAPasswordSignInTheDialogOpensOnTheField` (the dialog's initial
      component is the field once the password is switched on, as it is after an earlier
      password sign-in) and `SigninFocusUiTest#theDialogOpensOnTheMasterPassword` (a fresh
      profile: the box first, then the field), and
      `SignInEnterSubmitUiTest#pressingEnterInTheMasterPasswordFieldSignsIn` (Enter is sent
      as the field's action event, `SignInDialogSteps#enterInMasterPasswordAndAwaitSignIn`; a
      real key press is not driven: manual only)
- [ ] TC-0121 In the tree: Del deletes, F2 renames, Ins creates - each with its confirmation
      where there is one. - automated by
      `TreeShortcutsUiTest#f2RenamesTheSelectedNode` (F2, rename confirmed with OK) and
      `TreeShortcutsUiTest#f2OpensTheSelectedNodeForEditing`,
      `TreeShortcutsUiTest#deleteRemovesTheSelectedNodeWhenConfirmed` (Del, confirmed) and
      `TreeShortcutsUiTest#deleteAsksBeforeItRemovesTheSelectedNode` (Cancel keeps the node),
      `TreeShortcutsUiTest#insertAddsAChildToTheSelectedNode` (Ins); the key is looked up in
      the tree's input map and run, not typed on a physical keyboard
- [ ] TC-0122 Search field in the toolbar: typing jumps to the first match, Enter to the
      next. - automated by `ToolbarSearchUiTest#typingJumpsToTheFirstMatchAndEnterToTheNext`
      (Enter is sent as the field's action event)
- [ ] TC-0123 After a sign-in, "add node..." from the tree's context menu: the "New node" dialog has
      the keyboard at once, and what is typed without a click lands in its name field; Enter adds
      the node. Once after the sign-in at the start, once after a sign-in through "File > Open
      Database..." while the main window shows. - manual only: on the e2e display (Xvfb with fluxbox) the
      dialog did not get the keyboard in a probe test once the sign-in dialog was centred for real,
      while on a Wayland desktop it did (2026-11-23, #1097); a test on the e2e display would report
      the display, not the application

## TS-0013 Plugins

The canonical inventory is the `plugins:` target in the Makefile.

- [ ] TC-0130 Open and close every tool window once without the application freezing. -
      opening is automated for one tool of each plugin, each in a session of its own, by
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
      `ReleaseKeyPluginUiTest#showsTheStateOfAKeyThatGpgWrote`; a close with a check after it is
      driven for the certificate wizard
      (`CertificateWizardUiTest#certificateWizardOpensAndClosesWithoutKillingTheApp`, a dispose by
      the test), the conversion wizard's Finish
      (`ConversionPluginUiTest#convertDerToPemWritesTheKeyAsPemThroughTheWizard`) and the
      lethenon window (`LethenonPluginUiTest#thePlugin_remembersTheChainAndTheWalletThatWorked`),
      the console
      (`ASecondConsoleBringsTheOpenOneToFrontUiTest#aSecondConsoleClick_leavesOneConsoleOpen`,
      `doDefaultCloseAction`, then `System.out` is the stream from before) and the key store
      wizard's Finish
      (`CreateKeyStoreWizardUiTest#finishWithoutAKeyPairCreatesAnEmptyReadableStore`, the wizard
      is no longer showing);
      the checksum and obfuscation windows are only closed as cleanup
      (`ApplicationSteps#closeInternalFrame` in `ChecksumFileWritingUiTest`,
      `ObfuscationRefusesWithoutAKeyUiTest` and `ObfuscationImportRefusesAForeignTypeUiTest`),
      with nothing checked after; the closing of the other tools and the whole round over every
      tool in one session manual only (the tools open in different ways, as dialogs, internal
      frames or a docked console, and no test walks them as one round; a first generic attempt
      found no common signal to wait for)
- [ ] TC-0131 The order of the plugin menu entries is the same after a reinstall. -
      automated by
      `PluginMenuOrderSurvivesPluginToggleUiTest#aSavedPluginsMenuOrder_isTheSameAfterAReinstallAndARestart`
      (the saved order, the plugin zips removed and installed again between two starts, the
      order after the start) and
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
      "page does not exist" editor (#868). - automated in part: that each of the two Help
      buttons opens the page the plan names and that the page is in `docs/wiki/` by
      `SignInHelpButtonsUiTest#signInHelpButton_opensTheOpenExistingPage_whenClicked` and
      `SignInHelpButtonsUiTest#createHelpButton_opensTheCreateNewPage_whenClicked` (the real button
      is clicked, the link is recorded, no browser opens); that every page the application and its
      documents link, the released versions' help links included, is in `docs/wiki/` by
      `WikiLinksTest#everyLinkedPage_isInTheWikiSource` and
      `WikiLinksTest#everyPageAReleasedVersionLinks_isInTheWikiSource`; that the Wiki button of
      Help > Info carries the wiki address by
      `ApplicationInfoPanelTest#theWikiButtonCarriesTheWikiAddressOfTheInfo_andTheDefaultOneIsTheWikiHome` and
      `ApplicationInfoPanelTest#theWikiButtonCarriesTheAddressTheInfoGives_notAFixedOne`.
      Manual only: that the published wiki, the mirror of `docs/wiki/`, serves the page (it needs
      the network and the workflow's run), and that the browser opens it

## TS-0015 The installer (release rounds) (release smoke)

`make verify-installer PREVIOUS=<last release>` runs the measurable checks; this is the rest.

- [ ] TC-0150 Install from the izpack jar on a machine or fresh directory without a
      development build, start from the installed launcher. - manual only (the install step
      and the generated launcher on a machine without a development build run no test; the
      installer file itself, its checksums and the application jar inside it are checked by
      `make verify-installer`, `scripts/verify-installer.sh`)
- [ ] TC-0151 Help > Info (the menu entry is "Info", not "About") names the released version.
      - automated by `ApplicationInfoPanelTest#showsTheGivenApplicationNameVersionCopyrightAndLicense`
      (the panel shows the version it is given) and
      `HelpAboutUiTest#helpInfoOpensItsDialogWithTheApplicationNameAndAGithubLink` (Help > Info
      opens); the `Implementation-Version` of the application jar in the installer equals the
      release version by `make verify-installer`; that the installed application's own Help >
      Info window names the released version manual only
- [ ] TC-0152 Open a real database created with the previous release. - automated half:
      `AnEarlierVaultKeepsItsChangeThroughTheWholeChainUiTest#theChange_survivesTheWholeChain_andNothingElseMoves`
      (case "8.14": the previous release's fixture signs in through the dialog),
      `VaultOfEightFourteenOpensTest#aVaultOfEightFourteen_opens_withItsKeyValuePairProperty`
      and `WhatEightFourteenStoresOpensTest#anEntryEightFourteenImportedFromKeePass_opensAsEightFourteenReadsIt`
      (at model level); a real database of the previous release manual only (it holds data and
      shapes no fixture has; the fixture is rewritten from the new previous release at each
      release)

## TS-0016 The command line

- [ ] TC-0160 `--cli` answers with the library's root command help, no stack trace. -
      automated by `MysticCryptUiCliTest`
- [ ] TC-0161 One hash and one checksum command against a real file; verify the checksum
      against `sha256sum`. - manual only (another program checks the output)

## TS-0017 A vault of an earlier version (release smoke)

On a COPY of a real vault created with an earlier release, never the original.

- [ ] TC-0170 Open the copy, change an entry, save, lock, unlock, close, reopen: the change
      is there and nothing else moved. - automated by
      `AnEarlierVaultKeepsItsChangeThroughTheWholeChainUiTest#theChange_survivesTheWholeChain_andNothingElseMoves`
      (cases "8.14" and "7.3": sign in through the dialog, change title and password of one
      entry, save, lock, unlock, close, reopen; a SHA-256 per entry over all fields,
      attachments and properties after the unlock and after the reopen); opening and migrating
      other fixture vaults: `VaultsOfEightWithAKeyFileSignInUiTest#aVaultOfEight_withAKeyFile_signsIn_andIsSavedInThisVersionsFormat`
      and `LegacyDatabaseMigrationUiTest#anOldDatabaseOpensAndMigratesWhenItIsSaved`; the
      chain on a real vault manual only

## TS-0018 The start (release smoke)

Added for 8.16. The start was reworked twice shortly before it: #1072 (a start died with
`ClassCastException: FlatLightLaf cannot be cast to NimbusLookAndFeel`, because the sign-in
dialog was built on the start thread while the persisted look was switched) and #1075 (the
sign-in dialog is now built and shown on the event thread, after the persisted look). The e2e
suite starts the application inside its own JVM on a throwaway home, so what it cannot see is
the real process on a real screen with a real configuration directory; these cases are that
check, run against the BUILT installer's installation, not `make run`.

If one of them breaks because of #1075 and the break cannot be fixed by 2026-11-24, it goes to
the maintainer with the option to revert #1075 (his fall-back limit of 2026-11-20, which
covers #1075 as it covers the other changes of 8.16).

- [ ] TC-0180 First start: end the application, move `~/.config/mystic-crypt-ui` aside (or use
      a fresh user), start. The sign-in dialog opens on the screen, with the default look
      (FlatLaf Light), no error window and no stack trace on the terminal; create a database
      through New..., set a master password, sign in: the main window opens in the same look. -
      automated half:
      `CreateNewDatabaseUiTest#createFirstDatabaseThroughNewFlowAndSignIn` (a throwaway home
      without a configuration directory, New..., sign in) and
      `SignInDialogBuiltOnTheEventThreadUiTest#theLookIsSwitchedOnTheEventThreadBeforeTheDialogExists`
      (the event thread builds the dialog); that the dialog is on the screen, in the default look
      and the process starts cleanly from a configuration directory that really does not exist:
      manual only (the harness's home exists before the start)
- [ ] TC-0181 Start with a saved FlatLaf look: choose View > Look and Feel > FlatLaf Dark, end
      the application, start it again, and repeat with FlatLaf Light. The sign-in dialog and,
      after signing in, the main window come up in that look from the first paint; no window,
      menu or dialog shows another look, and nothing is logged as an error. - automated half:
      `SignInDialogLookAndFeelUiTest#thePersistedLookAndFeelIsAlreadyActiveOnTheSignInDialog`
      (FlatLaf Dark in the settings file, active at the sign-in dialog),
      `LookAndFeelMenuChoiceSurvivesRestartUiTest#theLookChosenInTheViewMenu_isTheLookTheNextStartApplies`
      (case "FlatLaf Dark", a start in a JVM of its own applies it) and
      `LookAndFeelSurvivesRestartUiTest#theLookChosenInTheSettingsIsStillActiveAfterARestart`;
      the look as painted, on a real screen, with the installed application, and FlatLaf Light as
      a saved look: manual only (no test drives it, and a mixed look is a thing to see)
- [ ] TC-0182 Start with a saved JDK look: choose View > Look and Feel > Nimbus, end the
      application, start it again, and repeat with Metal. The sign-in dialog and the main
      window come up in that look, nothing mixes looks, no error. Nimbus is the case that
      died in #1072 (`NimbusStyle.validate`), so it is the one that matters. - automated half:
      `LookAndFeelMenuChoiceSurvivesRestartUiTest#theLookChosenInTheViewMenu_isTheLookTheNextStartApplies`
      (case "Nimbus": the forked JVM applies the class at the start, the window itself is not
      looked at) and `SignInDialogBuiltOnTheEventThreadUiTest#theLookIsSwitchedOnTheEventThreadBeforeTheDialogExists`
      (FlatLaf Dark only); the Nimbus and Metal sign-in dialog and main window as painted, and
      Metal as a saved look: manual only. Of the two JDK looks that share a class the settings
      hold "Metal", and the next start shows the runtime's default theme of it (Ocean, #1065):
      that is not a failure here
- [ ] TC-0183 Start through the installer's menu entry: install the built installer (TC-0150),
      find "mystic-crypt-ui" in the desktop's application menu (the desktop entry
      `~/.local/share/applications/mystic-crypt-ui.desktop` the installer lays down, see
      `src/main/izpack/installation.html`), start it from there, not from a terminal. It starts
      the INSTALLED version (Help > Info names the released version), and the sign-in dialog
      opens in the look the settings hold. Run it once with the look of TC-0181 saved, so the
      menu entry's start is also a start with a saved look. - manual only (the desktop
      environment's menu and the generated launcher run no test; `make verify-installer`
      (`scripts/verify-installer.sh`) starts the application jar only headless with `--cli
      --version`, not the window, and not through the menu entry)

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
