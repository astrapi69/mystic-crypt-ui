# A lethenon wallet lives in the vault

A decision record, taken on 2026-10-30 by the maintainer on the draft that proposed three options and
recommended the first. The code this describes is built after the tests for it are written and red.

## The decision

1. **The wallet is stored as an attachment, sealed.** The wallet file (the `LETHWF` bytes, as lethenon
   writes them, under the wallet password) is the attachment `wallet.lethwf` of one vault entry. The
   wallet password is **not** stored in the vault. The 24 words are the offline backup: the plugin
   shows them once, at creation, and says plainly that they must be kept offline. They are never stored
   in the vault and never exported.
2. **The wallet format has no network field.** A wallet is the 32 bytes of entropy and nothing else, as
   lethenon has always written it. The network (testnet or mainnet) is a hint on the vault entry, not
   part of the wallet.
3. **No new entry type and no change to the vault format.** A wallet is an ordinary entry, marked by its
   own custom properties, which the vault model carries per entry (`MysticCryptEntryModelBean`,
   `properties` and `protectedPropertyKeys`):
   - `lethenon.wallet` = `1`, the wallet format version. This is the marker. An entry is a wallet only
     when this property is present and the attachment `wallet.lethwf` is there as well; any other shape
     is an ordinary entry.
   - `lethenon.network` = `testnet` or `mainnet`, optional, a hint for the user. It is not part of the
     wallet and carries no secret.
   The title reads `lethenon wallet (testnet)` or `lethenon wallet (mainnet)` for people; the plugin
   reads the property, not the title.

   Why a property and not the URL field: the vault model carries custom properties with their protection
   flag, and they survive a KDBX round trip through KeePassXC 2.7.6 (`KdbxRoundTripKeepsEveryFieldUiTest`,
   11 tests, all green, including "custom properties survive, including the protected one"). The URL is
   a field the user sees and edits as an address, so a marker there is one accidental edit away from
   gone.
4. **Export.** Wallet attachments are exported like every other attachment, still sealed. The export
   dialog shows one short line before the export starts: the file contains sealed lethenon wallets; the
   wallet password is needed to use them; the 24 words are not in the file.
5. **Unsealing happens in memory only.** Opening the wallet from the vault reads the attachment bytes and
   unseals them with the wallet password, which the user types at the moment of the action. Nothing is
   written to disk. The library provides the byte API for this (see below), not a temporary file.
6. **Wiping.** Until mystic-crypt 13.9 is released, the unsealed secret is held as character arrays and
   zeroed when the vault locks or closes, the pattern of #294. After that release the same secrets move
   to `SecretCopies` (mystic-crypt#227).
7. **The words in the vault are the user's choice.** Off by default. Option 2 is allowed as an explicit
   user choice, with the risk named (section "The words in the vault, as the user's choice").
8. **Locking.** The lock invariant stays. With no open vault no wallet action is offered; locking clears
   every buffer that holds an unsealed wallet.

## What lethenon needs (library first)

`WalletFile` gets a byte API next to the path API, with the same format and the same checks:

- `static Wallet read(byte[] sealed, char[] password)` - opens a wallet from its bytes;
- `static byte[] toBytes(Wallet wallet, char[] password)` - seals a wallet to bytes.

The path API is written in terms of these. No change to the on-disk format, so existing wallet files
open unchanged. Nothing here is a temporary file in the plugin.

## Tests first (red before the code)

In lethenon:
- a wallet sealed with `toBytes` opens with `read` and yields the same entropy;
- a wrong password is refused, with the same message the path API gives;
- a file written by the path API opens through the byte API, and the reverse.

In mystic-crypt-ui, the lethenon plugin:
- create in the vault: the wallet attachment exists, the entry carries the marker, the 24 words are shown
  once and are not in the vault;
- take over an existing wallet file: it opens, the original file is left untouched, the user decides
  whether to delete it;
- export a wallet to a file: only after confirmation, with the warning that the file lies outside the
  vault;
- restore from 24 words directly into the vault;
- no decrypted wallet on disk: the test directory is listed before and after each action;
- after lock, no buffer the test holds still contains secret bytes;
- a locked vault offers no wallet action;
- a KDBX export carries the sealed attachment and not the words.

## Backup assistant (added 30 October 2026)

Part of "wallet in the vault". The assistant makes the 24 words and the wallet file survive a lost
vault and a lost computer, and tells the user whether it has checked that they do.

### What it does

1. **Words, once.** At creation, and later on request, the plugin shows the 24 words once, with the
   request to write them down on paper or on an offline medium. It then asks for three words chosen at
   random, for example the 4th, the 11th and the 19th, to confirm they were written down. A wrong answer
   is refused and the question repeats. The words are never stored: not in the vault, not on disk.
   After they are shown, the buffers holding them are overwritten.
2. **Wallet file, to a place the user picks.** The sealed `LETHWF` file is written to a location the
   user chooses, for example a USB stick. After writing, the plugin reads the file back and shows its
   SHA-256 checksum, so the user can compare it with the copy on the stick.
3. **Backup check.** The user types the 24 words. The plugin derives the address from them and compares
   it with the address of the wallet in the vault. The result is one clear line: matches, or does not
   match. No file is written by this step.
4. **State shown.** After a successful check, the vault entry gets the property `lethenon.backupVerified`
   with the date of the check (ISO 8601). The wallet overview shows a clear notice while the property is
   absent, and a gentle reminder once the last check is older than the interval set below.
5. **Plain explanations.** The texts say, in simple words: the 24 words restore the wallet on any
   computer; the wallet file is the wallet's data, sealed; the wallet password opens the file. The words
   are not put into the vault, because a vault that holds them is a vault that gives the whole wallet
   to whoever opens it.

### Limits, stated

- Java strings cannot be overwritten. The words are handled as character arrays, as in #294. The string
  copies that Swing components hold, as recorded in `locking-keeps-the-content`, cannot be cleared from
  here; the words are not put into text components where avoidable.
- The check compares addresses, so it proves that the typed words produce the wallet's address. It does
  not prove that the words are the only copy written down.

### Open decision

- **The interval for the reminder.** Not decided here. Proposal to be confirmed: 180 days. Until it is
  decided, the reminder is not built.

### Tests (before the code, red first)

- asking the three words: a wrong word is refused; the right three pass; the positions are random per
  request (two requests in a row do not ask the same positions in the same order, to be checked over 50 runs in the test; not measured yet)
- the check: the right 24 words match the vault's wallet; one word wrong does not match; a shorter or
  unknown list is refused with a reason that names the count or the word
- the property: after a successful check `lethenon.backupVerified` holds today's date; after a failed
  check it does not change
- no words on disk: the test directory is listed before and after the show, the ask and the check, and
  no file holds a word of the phrase
- no decrypted wallet on disk: the same directory test, for each step
- after each step, every buffer the test holds that was filled with words or entropy is zeroed
- the export: the sealed file is written to the chosen location, reads back with the same checksum, and
  the printed checksum equals the SHA-256 of the file
- the export shows the sealed file only; no decrypted copy is written in between

## The words in the vault, as the user's choice (option 2, added 30 October 2026)

The default stays option 1: the words are not in the vault. The user may choose otherwise, deliberately:

- Off by default. At creation and in the backup assistant the plugin offers "Store the words in the
  vault as well", with the plain warning: whoever can open the vault then has the whole wallet, also
  without the wallet password. For wallets of real value, an offline paper copy is recommended in
  addition.
- Stored as a protected custom property `lethenon.mnemonic` on the wallet entry. It is written only
  after that explicit choice.
- Exported to KDBX with the protection flag, like every protected property. The export warning from
  decision 4 applies, and it names the words.
- Still checked: a user who stores the words goes through the three-word question and the backup check
  as well, the same as everyone.
- Removable at any time. Removing deletes the property and overwrites the buffers that held the words.

Risk, named: a stolen unlocked or opened vault, or a vault file copied and opened with the master
password, gives the attacker the full wallet with no second factor. This is the reason for the default.

Tests (red first): storing happens only after an explicit choice; a KDBX round trip keeps the property
protected; after removal the property is gone and no buffer the test holds contains the words.

## Rejected

- **Both** (option 3): two copies of the same full key, and no gain in recovery.
- **A new entry type**: a format change to the vault, which needs its own decision.
- **The URL field as marker** (first draft of this record): visible and editable as an address; the
  property above survives the same round trip and is not shown as one.
- **A network field in the wallet format**: a change to lethenon's format, for a fact the title already
  carries.
