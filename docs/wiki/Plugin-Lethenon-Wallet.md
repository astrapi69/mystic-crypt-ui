# Lethenon Wallet

Lethenon is a small blockchain whose currency is called LETH; every block carries a line of text,
a pun, chosen by whoever mined it. This plugin is a wallet for it, kept inside your mystic-crypt
database: it creates a wallet or takes one over, shows your address and balance, backs up the 24
words that restore the wallet, and sends, receives and mines LETH. Next to the wallet it has tools
that check a chain file, show its blocks, keep it up to date from a node, or run a node of your
own.

Practise on the **test network**, where LETH has no monetary value. The windows also know the main
network, where LETH has real value and a transfer cannot be called back; a wallet meant for it says
so in every window, and sending from it asks for a confirmation first.

## Where to find it

**Plugins** menu, submenu **Lethenon**:

| Item | What it opens |
|---|---|
| **Wallet** | The wallet window, the way in: your wallets, your address, your balance, the backup. |
| **Verify a Chain** | Checks a chain file from its first block to its last. |
| **Show a Chain** | Lists the blocks of a chain file. |
| **Show a Balance** | Reads what a wallet holds from a chain file. |
| **Send LETH** | Signs a transfer. |
| **Mine a Pun** | Mines one block with your pun and adds it to the chain file. |
| **Sweep One-Time Payments** | Moves the payments to your address onto your account. |
| **Synchronise with a Node** | Brings your chain file up to date from a node. |
| **Run a Node** | Shares your chain file with other nodes, and mines if you want it to. |
| **More ... > For Operators** | The checklist "Before you start" for whoever starts the main chain in public. |

The items only appear once the Lethenon plugin is installed and enabled; the installer ships it.
If the **Plugins** menu is missing entirely, no plugin is installed yet; see [[install-a-plugin]].
They are enabled while a database is open; before you sign in, and while the workspace is locked,
they are greyed out. Locking the workspace closes every open Lethenon window.

Every item opens a new window inside the main frame. Each window but **Wallet** shows a network
badge above its content: `TESTNET` or `MAIN NETWORK` for the wallet the overview of the wallet window
shows, `NETWORK UNKNOWN` while it shows none, with a sentence on what that means for your LETH.

## The wallet, the wallet file and the chain file

Three things are worth telling apart:

- **The wallet** holds your keys. In this plugin it lives in the database: one entry per wallet,
  with the sealed wallet file as its attachment `wallet.lethwf`. Sealed means encrypted with the
  **wallet password**, which is a password of its own, not your master password, and which is never
  stored. The entry also carries your address (public), the network the wallet is meant for, and
  the day of your last successful backup check. Save the database after creating, taking over or
  restoring a wallet: until then it exists only in the database that is open.
- **A wallet file** (`.lethwf`) is the same sealed wallet outside the database, as lethenon's own
  command line writes it with `wallet create` or `wallet restore`, or as step 3 of the backup writes
  it onto a USB stick. Whoever has the file and the wallet password has the wallet. The file does not
  contain the 24 words.
- **The chain file** is your copy of the chain, wherever you keep it on your disk. Your balance is
  read from it, here on your computer, without asking anybody. A wallet without a chain file has no
  balance to show yet.

The wallet password is asked only for an action that needs the wallet's secret keys: showing the
words again, checking incoming payments, taking them over, sending, sweeping and mining. It opens
the wallet in memory for that action, and nothing of it is kept.

## The wallet window

**Plugins > Lethenon > Wallet** shows **No wallet in the vault yet** with three ways to get one,
**Create a new wallet**, **Take over a wallet file** and **Restore from 24 words**, or, once the
database holds a wallet, the overview of it.

### Getting a wallet

**Create a new wallet.** Choose the network, type the wallet password twice and press
**Create the wallet**. The wallet goes into the database as a new entry, and the backup starts at
once with its 24 words: write them down on paper before you go on. The step says which entry the
wallet is and that the database has to be saved.

**Take over a wallet file.** Pick a wallet file lethenon's command line wrote, type the password
it is sealed with (it stays the wallet password) and press **Take it over**. The sealed file is
copied into the database as it is. The file itself is only read and stays where it is; whether you
delete it is your decision.

**Restore from 24 words.** Type the 24 words of your note into the 24 numbered fields, choose the
network, type a wallet password twice and press **Restore the wallet**. You do not have to type the
words one by one: a whole phrase pasted into any field fills all 24, also with the numbers of a
backup sheet in front of the words. A field is checked as soon as you leave it: a word that is not in the word
list is outlined, and the window names it, for example
`Word 12, "qqq", is not in the word list. Check your note for a typing error.` After a paste, the
clipboard is emptied if it still holds what was pasted. If the 24 words do not give a wallet, the
window says `one is wrong or two are swapped`; compare them with your note, in order.

### The overview

From top to bottom:

- the network badge and what it means, for example `Test network, LETH has no monetary value.`;
- when this program last brought the chain up to date and how old its last block is, for example
  `Synced: today 14:02, last block: 3 min ago`. The balance is as of that block;
- the balance in large letters, what both accounts of the wallet hold, rounded to two decimals,
  and under it the exact amount;
- **Take over**, when payments came to your address that are not on your account yet (see
  [Receiving](#receiving));
- **No chain yet** with **Fetch the chain**, when there is no chain file to read the balance from;
- **Check incoming payments**;
- **Wallet:** to choose among the wallets of the database, and the wallet's **Entry:**,
  **Network:** and **Your address (public):** with **Copy**;
- the state of your backup, and **Check my backup**, **Back up** and, if you chose to keep the 24
  words in the database, **Remove words from vault**;
- **Another wallet:** with the same three ways to get one as above.

**Fetch the chain** fetches the chain from the node in the plugin's settings, or from the seed
nodes of the test network when the settings name none, into the chain file of the settings; when
they name no chain file either, it asks where your copy of the chain is to be kept, with a name
proposed. Nothing of your wallet is sent.

The other windows that sign (send, sweep, mine, run a node) use the wallet the overview shows. When
no wallet of the database is chosen, they offer a wallet file field instead, with the sentence
`No wallet of the vault is chosen in "Wallet", so the wallet comes from this file.`

### Your address and your accounts

Give your **address** to whoever pays you. It is public, opens nothing, and does not change. A
payment to it goes to a one-time destination that a reader of the chain cannot connect to your
address, so the chain does not show who paid you.

The wallet also has two **accounts**, one for each kind of signature it can make: Ed25519 and
ML-DSA-65. An account is named on the chain as it is. Somebody can pay you on an account directly,
but the address keeps payments private, so give the address.

## The backup

**Back up** in the overview asks for the wallet password, shows the 24 words once more and runs
the three steps of the backup assistant. A new wallet starts it on its own.

1. **Your 24 words.** Write them down on paper. With them you restore the wallet on any computer,
   even when the database and the computer are lost. No photo, no file, not to the clipboard:
   whoever has the words has the wallet.
2. **Three words from your note.** The words are off the screen now. Type the words at the three
   positions the step asks for; the positions change every time. Here you can also choose to keep
   the 24 words in the database, as a protected field. That is off by default, and the step says
   why: anyone who can open your database then has the whole wallet, even without the wallet
   password. For a wallet of real value, keep the paper note as well.
3. **The wallet file on a stick.** Tick **Understood, write the file**, then **Choose location ...**
   and pick a place, for example a USB stick. The name is proposed as `lethenon-testnet.lethwf` (or
   `lethenon-mainnet.lethwf`), and an existing file is never overwritten. The file is written, read
   back, and its SHA-256 is shown: compare it with the copy on the stick, on another computer.

**Check my backup** asks for the 24 words from your note and compares them with the address of the
wallet, without the wallet password and without writing anything but the day of a check that
matches: `These words match your wallet. The check is recorded; the next reminder comes in 180
days.` A check that does not match says `One is wrong or two are swapped.` and leaves the date of
the last successful check as it was.

The overview reminds you: `Your backup is not checked yet.` until the first successful check, and
`Your last backup check is more than 180 days ago.` once it is more than 180 days old.

**Remove words from vault** appears when the 24 words are kept in the database. It removes them from
the wallet's entry after a confirmation; your written note stays. Save the database afterwards, then
the words are gone from the file as well. The confirmation says what removing cannot do: the opened
database may keep a copy of the words in memory until the program exits.

## Receiving

Payments to your address can only be found with the wallet's private view key, which is sealed in
the wallet. So the exact line of the overview says `without unchecked incoming payments` until you
looked.

1. **Check incoming payments** asks for the wallet password and scans the chain file for payments to
   your address. The exact line then says how much came in how many payments and is not yet taken
   over.
2. **Take over** asks for the wallet password again and moves each payment onto your account, with a
   transfer of its own and without a fee. The transfers wait like any other (see
   [Sending](#sending)) until a block carries them.

**Sweep One-Time Payments** does the same as **Take over**, in a window of its own, with a fee per
payment you choose.

## Sending

**Send LETH** signs a transfer from one of your accounts (**Pay from:**) to what the recipient gave
you (**To:**), an account or an address with a `:` in the middle. **Amount (LETH):** is what the
recipient gets, **Fee (LETH):** what you pay on top to whoever mines the block (0 is allowed), and
**Note:** goes with the transfer, readable by everyone who reads the chain: write nothing private.

Where the signed transfer goes depends on **Node (optional):**

- **empty**: it waits next to your chain file until a miner that reads this file mines a block, or
  it goes to the node you started in this program for that file;
- **host:port** of a running node: that node puts it into one of its next blocks.

A node outside this program that serves your chain file does not see a transfer written into the
file and removes it at its next block: name that node in **Node (optional):**. For a sweep the
same node has to be stopped while you sweep and started again afterwards, so that it reads the
sweep when it starts.

When the wallet of the database you send from is meant for the main network, **Send** first shows
`You are sending ... on the MAIN NETWORK to ...` with the fee and the total, and the transfer is
signed only after you confirm. When its entry records no network, the window first asks which
network it is on. A send from a wallet file asks neither question: a file does not record its
network.

## The chain tools

These windows work on a chain file you pick in **Chain file:**; the last one that worked is
remembered.

- **Verify a Chain** (**Check my chain**) checks every block, transfer and signature from the first
  block on and answers `Your chain is in order` or `This chain is not in order, so nothing in it
  can be trusted`, with the reason.
- **Show a Chain** (**Show the blocks**) lists every block with its height, time in UTC,
  difficulty, transfers, the account it paid and its pun. It shows the blocks only after the whole
  chain verified.
- **Show a Balance** (**Show my balance**) reads what a wallet holds from the chain file: your
  address, each account with what it holds, and the payments waiting for a sweep.
- **Synchronise with a Node** (**Bring my chain up to date**) asks a node for the blocks your
  chain file is missing and checks each one here before it is written. Leave **Node (optional):**
  empty to ask the seed nodes of the test network, which are reached through Tor at their onion
  addresses, or type the host:port of a node you trust. **Tor proxy (optional):** is Tor's SOCKS
  proxy, usually `127.0.0.1:9050`; an onion address needs it. A chain file that does not exist yet
  starts with the node's first block, and the window says so.
- **Mine a Pun** (**Mine the pun**) looks for a block with your words and adds it to the chain file;
  the window waits until it is found, usually a few seconds. The block pays your wallet's Ed25519
  account, and carries the transfers that were waiting next to the chain file. For a chain file that
  does not exist yet, **New chain:** decides which network it starts; its first block belongs to
  nobody, so the next block you mine is the first that pays you.
- **Run a Node** (**Start my node**, **Stop my node**) shares your chain file with other nodes on
  **Port:** (0 lets the computer pick a free one), connects to the ones in
  **Other nodes (optional):**, and, when **Blocks:** is ticked, mines with **Your pun:**, paying your
  wallet. The window shows the port, the last block, the nodes connected, the transfers waiting, the
  blocks found for you, and the connections it refused, read again every second.

A chain file written by lethenon 0.5.0 or earlier, as mystic-crypt-ui 8.9 and earlier write them,
is converted once before any of these windows reads it, after a question that says what happens:
the file as it was stays beside it, and only the older versions of the program can still read that
copy.

## Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Lethenon** in the list on the
left. The windows fill these in themselves, each only once something worked: a chain read or
written, a wallet opened with its password, a sync taken from a node or through a proxy. A failure
leaves what worked before. Clearing a value forgets it.

| Key | What it holds |
|---|---|
| `chain.file` | The chain file every window starts with. |
| `wallet.file` | The wallet file the windows start with, when they use a file. |
| `node` | The node, host:port, a sync took the chain from; empty for the test network's seed nodes. **Fetch the chain** asks this node. |
| `proxy` | The SOCKS proxy, host:port, a sync went through; for Tor usually `127.0.0.1:9050`. |

A password is never remembered, in no form: there is no setting it could be written to.

## On the command line

The plugin brings no command of its own to `--cli`. Lethenon has a command line of its own, the one
that writes the wallet files **Take over a wallet file** reads; it is part of lethenon, not of this
application.

## Limits and pitfalls

- **The wallet password cannot be recovered.** It is not stored anywhere. Without it, the wallet in
  the database and every wallet file of it are closed; only the 24 words restore the wallet.
- **Saving the database is part of every change to a wallet.** A wallet created, taken over or
  restored, words kept or removed: none of it is in the database file until you save.
- **The balance is only as recent as your chain file.** The overview says when this program last
  synced and how old the last block is. **Synced: not by this program** means the chain file came
  from somewhere else.
- **Payments to your address are not in the balance until you check them.** See
  [Receiving](#receiving).
- **A note on a transfer is public.**
- **A transfer on the main network cannot be called back.** Check the address character by
  character before you confirm. Sending from a wallet file brings no confirmation for the main
  network, so take the wallet into the database first.

## See also

- [[The-Database-File]] - where the wallet entries and their attachments are kept
- [[Settings-And-Plugins]] - where the plugin settings are edited
- [[install-a-plugin]] - getting the **Plugins** menu to appear in the first place
