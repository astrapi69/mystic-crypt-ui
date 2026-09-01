# Journal

AI-session memory for this repo: what was discussed, decided, and built, chronologically.
Not user-facing documentation (that belongs in the wiki or CHANGELOG.md) - a place for a new
session to catch up on recent work and on ideas that came up but were not built yet, per
architecture.md's "check recent journal/PRs for already-planned work" rule.

Append new entries at the bottom. Keep each entry short: what happened, why, issue/PR numbers.
Move an idea out of "Open ideas" once it has its own issue and is scheduled or done.

## 2026-08-31 -- 2026-09-01

**Plugin menu ordering, IntelliJ-style (PR #100, closes #98).** The Plugins submenu order used to
follow pf4j's plugin-directory read order (root cause of #95), papered over with an alphabetical
sort. Compared against IntelliJ's `ActionGroup` anchor model on request; the `menu-action` library
already had the primitives (`Anchor`, `MenuInfo.relativeToMenuId`, `MenuInfoExtensions.orderByAnchor`)
unused. Added `PluginMenuContribution.getAnchor()`/`getRelativeToMenuId()` (default `LAST`, no
plugin opts in yet), extracted the pure ordering logic into `menu.PluginMenuOrder` so it is
mutation-tested (Swing classes are excluded from PIT by design - too slow, mutations mostly
equivalent). Also fixed `DesktopMenu.getMenubar()` returning a stale bar after
`applyPersistedMenuLayout()` (#98).

**Key generation window: honest key format, readable DER for every algorithm (PR #103, closes
#101, #102).** Checking whether the key-generation window was tested for every algorithm x
key-format x save-format combination surfaced two real bugs: `cmbKeyFormat` (PKCS#1/PKCS#8) stayed
choosable for every algorithm, but PKCS#1 silently becomes PKCS#8 for anything but RSA/EC in the
library - the box now closes to PKCS#8 outside RSA/EC, mirroring the key-size box. Separately,
`KeyFiles.readPrivateKey`'s fixed algorithm list for DER files was missing X25519/X448/ML-KEM-768/
ML-DSA-65 - a key generated with any of those and saved as DER could not be read back.

Cross-repo: `crypt-data` issue #42 (a parallel agent's track, not touched from this repo) plans to
make the library itself throw when PKCS#1 is asked for a key with no traditional form, staged so
the UI/CLI stop offering the option first (this PR's #101 fix already is that first step) before
the library starts throwing - avoids another case like crypt-data #12/#14/#19/#23/#24/#25's history
of silent-fallback-becomes-silently-wrong.

**Certificate wizard, full pass on #93's follow-up (PRs #104, #108).** A user screenshot report
that steps 3/4 still scrolled and step 3's Previous button did not work led to three, not one,
root causes:
- `CertificateWizardState.DATES` never set `.previous(true)` on its `WizardStateInfo` - a primitive
  `boolean` left off a builder chain is `false`, not unset.
- `CertificateWizardPanel` duplicated `AbstractWizardPanel`'s own component construction (its own
  `navigationPanel` field/factory/layout override), producing two live `NavigationPanel`s in one
  dialog - the base class's one orphaned by `BorderLayout`, never touched by `updateButtonState()`
  again. Removing the duplication also fixed the dialog's opening size (two-pass `dialog.pack()`
  instead of a guessed inset constant). Closes #93.
- Even after that, Dates and Extensions still ran much taller than Issuer/Subject: `DatesPanel`
  embedded a `CalendarPanel` (the always-open 42-cell month grid, meant only for a `DatePicker`'s
  own transient popup) directly, twice, and never wired a `DateChangeListener` - a picked date
  never reached the model at all (#105). `ExtensionsPanel` had a dead `JScrollPane` field (a second,
  separate one was actually shown), a MigLayout row spec naming 5 templates for 8 real rows, and an
  empty `JTable` reserving Swing's fixed 450x400 default regardless of row count (#106). Content
  height for the tallest step dropped from 656px to 336px once both were fixed.

**Certificate created dialog: open the file (PR #108, closes #107).** Requested mid-investigation:
after saving, offer to open the file instead of only saying where it went. No in-app viewer exists
in this app (checked - `Desktop` is used exactly once elsewhere, `PluginsSettingsPanel`'s "Open
plugins folder"), so this uses the same `Desktop.getDesktop().open(file)` pattern, with a real
guard-clause test for "this system offers no way to open files."

**In-app editor for the saved certificate (PR #111, closes #110).** Built: "Edit" alongside
"Open" (system) in the certificate-created dialog, a small in-app text editor
(`CertificateFileEditorPanel`, model-bound `JMTextArea`) with a Save that writes back the
editor's current content.

### Open ideas (discussed, not yet built)

- **Shell/CLI vault unlock and read, security-first.** Discussed 2026-09-01: the mystic-crypt
  library CLI (`--cli`, backlink pattern) has no vault-open command yet. User wants to unlock a
  vault and read entries from the shell, no GUI - confirmed the design must be security-first
  ("bombensicher"), modeled on the Unix `pass` tool rather than a naive dump: master password
  NEVER as a CLI argument (only an interactive masked prompt or stdin, never in `ps aux`/shell
  history), a `list` command that shows entry names only, and a `show <entry>` that defaults to
  clipboard-copy with an auto-clear timeout instead of printing the secret to stdout (terminal
  scrollback/tmux/session-logging exposure). New capability goes into the mystic-crypt LIBRARY
  first (separate repo, own release cycle), per the CLI-is-a-backlink rule - not started, no issue
  filed yet.
