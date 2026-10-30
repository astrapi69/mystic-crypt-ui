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
3. **No new entry type and no change to the vault format.** A wallet is an ordinary entry, marked by two
   fixed values:
   - the entry's URL field is exactly `lethenon:wallet`;
   - the attachment is named `wallet.lethwf`.
   An entry with the URL is a wallet only when the attachment is there as well; the plugin treats any
   other shape as an ordinary entry. The title reads `lethenon wallet (testnet)` or `lethenon wallet
   (mainnet)`, and the network is taken from the title's hint, not from anything secret.
4. **Export.** Wallet attachments are exported like every other attachment, still sealed. The export
   dialog shows one short line before the export starts: the file contains sealed lethenon wallets; the
   wallet password is needed to use them; the 24 words are not in the file.
5. **Unsealing happens in memory only.** Opening the wallet from the vault reads the attachment bytes and
   unseals them with the wallet password, which the user types at the moment of the action. Nothing is
   written to disk. The library provides the byte API for this (see below), not a temporary file.
6. **Wiping.** Until mystic-crypt 13.9 is released, the unsealed secret is held as character arrays and
   zeroed when the vault locks or closes, the pattern of #294. After that release the same secrets move
   to `SecretCopies` (mystic-crypt#227).
7. **Locking.** The lock invariant stays. With no open vault no wallet action is offered; locking clears
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

## Rejected

- **The 24 words in the vault** (option 2): a reader of the open vault gets the whole wallet, with no
  second factor.
- **Both** (option 3): two copies of the same full key, and no gain in recovery.
- **A new entry type**: a format change to the vault, which needs its own decision.
- **A network field in the wallet format**: a change to lethenon's format, for a fact the title already
  carries.
