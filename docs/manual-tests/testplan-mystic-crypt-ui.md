# Manual Test Plan - mystic-crypt-ui

Status: 2026-11-13 (written for the 8.15 release, #948)
Tester: Aster

This plan has the same shape as adaptive-learner's
`docs/manual-tests/testplan-adaptive-learner-en.md`, the model the maintainer named:

- **PART A** is what only a human can check, sorted by priority.
- **PART B** is what runs automatically, as a reference - so what is NOT covered is visible.

Every manual case is answered with OK or BUG. On BUG: screenshot, what was done, what was
expected, and the case number (`TC-nnnn`) - that becomes a GitHub issue. The numbers are
stable: a new case takes the next free number, a dropped case leaves a gap.

## Prerequisite before every round: really start the current build

This has cost days more than once, so it stands at the top:

```bash
make build            # the jar that `make run` starts
make plugins-install  # the plugins the tool windows come from
make run
```

- [ ] TC-0001 After `make plugins-install` the plugin directory holds **only zips**, no
      unpacked directories: `ls ~/.config/mystic-crypt-ui/plugins`
- [ ] TC-0002 A plugin fix takes effect only after a new plugin build. The application jar
      alone is not enough - layout constants are compiled into the plugin class.

---

# PART A: MANUAL TESTS

## TS-0001 Database round-trip (data-loss gate)

Real data, no test fixture. Nothing else is released while this does not hold.

- [ ] TC-0010 Create a new database, set a master password, sign in.
- [ ] TC-0011 Build a tree: several folders, in each one entries with title, user name,
      password, URL, notes.
- [ ] TC-0012 Give one entry custom fields and attach a file.
- [ ] TC-0013 Save, close the application, restart, sign in.
- [ ] TC-0014 Everything is back: tree structure, every field, the custom field, the
      attachment.
- [ ] TC-0015 View the attachment with a double click, then write it out with "Save to" and
      open the file outside the application - content identical.

## TS-0002 The backup file (#847)

- [ ] TC-0020 Save an opened database: a sibling `.bak` file appears next to it.
- [ ] TC-0021 Close and reopen the database successfully: the `.bak` is gone.
- [ ] TC-0022 Make the saved file unreadable by hand (truncate a copy of the scenario, not
      your real database), open: the open fails and the `.bak` still holds the previous
      state and opens.

## TS-0003 Moving the database

- [ ] TC-0030 With the application closed, move the database file to another directory, open
      it from there, change something, save.
- [ ] TC-0031 Check that the **new** file was written and the old one does not reappear.

## TS-0004 Lock and unlock (the factors of the open database)

The unlock prompt asks for the factors the database was opened with, and the key file is
read from the disk again at unlock (8.15, GHSA-px49-m539-22h8).

- [ ] TC-0040 Database opened with a master password only: lock, unlock with the password.
      A wrong password is refused.
- [ ] TC-0041 Database opened with password AND key file: lock. The unlock prompt asks for
      BOTH; the key file field is preset with the path the database was opened with.
- [ ] TC-0042 Same lock: move the key file away before unlocking. The unlock names the
      missing file and refuses; it does NOT create a file at the key file's path. Put the
      file back, unlock works.
- [ ] TC-0043 Same lock: point the prompt at a different key (same format). Refused.
- [ ] TC-0044 Database opened with a key file only: lock, unlock with the key file.
- [ ] TC-0045 Cancel in the unlock prompt: the workspace stays locked, and
      "Unlock workspace..." brings the prompt back (#915).
- [ ] TC-0046 Locked and waited past "Close a locked database after minutes": the database
      is closed, opening again needs all factors.

## TS-0005 Protected values in the "Properties" tab (#946)

- [ ] TC-0050 Open a vault entry with properties the entry marks as protected (a lethenon
      wallet's words, or a KeePass entry with a protected custom field): the table shows
      `********`, the same mask whatever the length.
- [ ] TC-0051 Select and copy the masked row: what arrives on the clipboard is the mask, not
      the value.
- [ ] TC-0052 Open the property in the edit dialog: it opens masked and shows the value only
      on request.
- [ ] TC-0053 Rename the protected property: the table keeps the new name masked.

## TS-0006 KeePass, with a real file

- [ ] TC-0060 Import a real `.kdbx` (password, key file, both).
- [ ] TC-0061 Spot check: three entries match what KeePass shows.
- [ ] TC-0062 Export to a new `.kdbx`, open that file in **real KeePass**.

## TS-0007 Creating a database does not overwrite (#917)

- [ ] TC-0070 **New...** in the sign-in dialog, pick an existing file: the application asks
      before writing over it; No leaves the file untouched.
- [ ] TC-0071 **Browse...** in "Create your master key", same check.

## TS-0008 The lethenon wallet (real chain where named)

- [ ] TC-0080 Create a wallet: step 1 says where the wallet is and that the vault has to be
      saved (#818).
- [ ] TC-0081 Restore: the 24 words go into 24 numbered fields; a phrase pasted into any
      field fills all 24, numbers of a backup sheet (`1.`, `1)`, `01`, `1:`) read away
      (#804, #802).
- [ ] TC-0082 After the paste the words are off the clipboard: paste into another program
      gives nothing (#804). Something copied afterwards stays.
- [ ] TC-0083 Backup check with one wrong word: "One is wrong or two are swapped", the date
      of the last successful check stays (#804).
- [ ] TC-0084 Words kept in the vault are removed from the overview after a confirmation
      (#821).
- [ ] TC-0085 Overview without a chain: "No chain yet" with "Fetch the chain" and the node
      of the settings (#827). With a chain: it says when it was last synced and how old the
      last block is (#831).

## TS-0009 Multiple screens

- [ ] TC-0090 Start the application on the second screen: the sign-in dialog appears there,
      and the main window opens on the same screen.
- [ ] TC-0091 Open dialogs from the main window (settings, edit entry, save certificate):
      each appears on the screen the application is on.

## TS-0010 Key generator, trying everything out

The window is built for trying out, so that is how it is tested.

- [ ] TC-0100 Generate once with every algorithm in the box. Both text areas fill, no empty
      display, no silent error.
- [ ] TC-0101 While doing so: **nothing in the window moves or shrinks**.
- [ ] TC-0102 Encrypt and decrypt again with RSA.
- [ ] TC-0103 With another algorithm: the two buttons are not pressable and the tooltip says
      why.
- [ ] TC-0104 Save private key, public key, certificate - each time type a name **without an
      extension** and check the file gets the fitting one.
- [ ] TC-0105 Set the format to DER and do the same again: extension `.der`, binary content.
- [ ] TC-0106 Save the private key with a password and open the file again with that
      password (for example `openssl pkcs8 -inform DER`).

## TS-0011 Settings

- [ ] TC-0110 Switch the view to desktop, close the dialog: the application stands in that
      view at once.
- [ ] TC-0111 Restart: it opens in the same view.
- [ ] TC-0112 Change look and language, restart, the setting is still there.

## TS-0012 Keyboard

- [ ] TC-0120 Sign-in dialog: the caret is in the master password field at once, Enter
      signs in.
- [ ] TC-0121 In the tree: Del deletes, F2 renames, Ins creates - each with its confirmation
      where there is one.
- [ ] TC-0122 Search field in the toolbar: typing jumps to the first match, Enter to the
      next.

## TS-0013 Plugins

The canonical inventory is the `plugins:` target in the Makefile.

- [ ] TC-0130 Open and close every tool window once without the application freezing.
- [ ] TC-0131 The order of the plugin menu entries is the same after a reinstall.
- [ ] TC-0132 Disable a plugin in the settings, restart: its menu item is gone. Enable it
      again: it is back.

## TS-0014 Help buttons and the wiki

- [ ] TC-0140 Every Help button in the sign-in dialogs opens a wiki page that exists, not a
      "page does not exist" editor (#868).

## TS-0015 The installer (release rounds)

`make verify-installer PREVIOUS=<last release>` runs the measurable checks; this is the rest.

- [ ] TC-0150 Install from the izpack jar on a machine or fresh directory without a
      development build, start from the installed launcher.
- [ ] TC-0151 Help > About names the released version.
- [ ] TC-0152 Open a real database created with the previous release.

## TS-0016 The command line

- [ ] TC-0160 `--cli` answers with the library's root command help, no stack trace.
- [ ] TC-0161 One hash and one checksum command against a real file; verify the checksum
      against `sha256sum`.

---

# PART B: WHAT RUNS AUTOMATICALLY

The end-to-end suites drive the real application (AssertJ-Swing) on the build's own Xvfb;
the plugins bring their own test classes. Run with `./gradlew build`; the suite inventory
and counts come from the build output (`executed in this build` lines) - this document
points there instead of carrying numbers that age.

- `src/e2eTest` - sign-in, tree, KeePass, key generator, key store and certificates,
  encrypt/checksum/secret plugins, windows and settings (the `*UiTest` classes; read the
  class list with `ls src/e2eTest/java/.../ui/`).
- `src/e2eLockTest` - lock and unlock, including the factor cases of TS-0004 with made
  key files in every format the open dialog takes.
- `src/e2eKdbxTest` - the KDBX import/export round trips against fixture files.
- `src/test` - the unit suites, including `WikiLinksTest` (TS-0014's automated half) and
  the vault format round trips.
- `make verify-installer PREVIOUS=<release>` - size, archive, checksums, signer, class
  major, mystic-crypt against Central, plugins, the jar tests, a start.

## What PART B cannot do

- Anything with the user's **real** data: your own `.kdbx`, your own database, a move
  across machines.
- Anything with **multiple screens**: the test environment has one.
- Whether a font size, a contrast or a window split is **comfortable**.
- Whether another program accepts the written files (KeePass, openssl, a browser for a
  certificate, a real lethenon node).
- The installed launcher on a machine without a development build (TS-0015).
