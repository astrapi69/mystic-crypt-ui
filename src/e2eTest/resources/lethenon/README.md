# A chain file written by mystic-crypt-ui 8.8

`written-by-8.8.lethenon`

## Why this file exists

lethenon 0.6.0 keeps the chain in an SQLite database, and a chain file of 0.5.0 or earlier is
converted into one once the person agrees to it (#581). Every chain file a user of 8.8 or 8.9 has is
such a file. The end-to-end test of the conversion therefore needs one that 8.8 wrote, not one this
build's lethenon writes in the old encoding: what is being tested is whether the files people
already have survive the conversion.

Freezing a format is what a committed fixture does, and here that is the point. The other lethenon
tests build their chains with the library's own encoder, because there the current format is what
matters.

## How it was written

```
scripts/fixtures/write-a-chain-file-with-8.8.sh src/e2eTest/resources/lethenon
```

The script downloads the 8.8 installer, checks it against the sha256 published with RELEASE-8.8,
and takes the lethenon plugin out of it. It then runs `scripts/fixtures/WriteAChainFileWith88.java`
with a class path of only the 8.8 plugin's classes, the `lethenon-0.4.0.jar` that plugin carries,
and the 8.8 application jar. The generator creates a wallet and mines three blocks with 8.8's own
`MiningSupport.mine`, as the plugin's mine window does. Nothing of this checkout is on that class
path.

Each run writes a different file: the wallet and the proof of work are new every time. The wallet
was not kept; nothing here needs it.

## What it is, measured

The script's own output, from the run that wrote the committed file:

```
mystic-crypt-ui-8.8-installer.jar: OK
==> extracted the lethenon plugin, 338344 bytes
==> the plugin carries lethenon-0.4.0.jar
height 0 pays 6e6f626f647920686f6c6473207468652067656e6573697320726577617264206f66206c657468656e6f6e2d32: replayed 1 blocks, applied 0 transfers, checked 0 signatures; ...
height 1 pays 302a300506032b6570032100725555da49e490e6f8c2b54d4a7af2e70b96248489b81b8596860dc6d304e3c8: replayed 2 blocks, applied 0 transfers, checked 0 signatures; ...
height 2 pays 302a300506032b6570032100725555da49e490e6f8c2b54d4a7af2e70b96248489b81b8596860dc6d304e3c8: replayed 3 blocks, applied 0 transfers, checked 0 signatures; ...
8ff65fe9faadac967ed8f24008244da363f06346dd6866f12be68c3f9497d31d  src/e2eTest/resources/lethenon/written-by-8.8.lethenon
```

| | |
|---|---|
| size | 622 bytes (`ls -l`) |
| sha256 | `8ff65fe9faadac967ed8f24008244da363f06346dd6866f12be68c3f9497d31d` |
| first bytes | `02 00 00 00`: block encoding version 2, the format of lethenon 0.4.0 and 0.5.0 (`xxd`) |
| chain | `lethenon-test-2`, the test network, three blocks |
| puns | `written by 8.8 #40`, `a second block from 8.8 #193`, `a third block from 8.8 #68`: the number is the variation mining settled on |
| times | 2026-10-16T12:00:00Z, 12:01:00Z, 12:02:00Z |
| block 0 | pays the burn account, as every genesis block since lethenon 0.4.0 (lethenon#148) |
| blocks 1 and 2 | pay the generated wallet's Ed25519 account, `302a3005...04e3c8` |
| replay | `replayed 3 blocks, applied 0 transfers, checked 0 signatures; the sum of all balances is 1984000000.00000000 LETH, which is the supply` |

Chain, puns, times and replay were read from the file with lethenon 0.6.0's own decoder,
`CanonicalEncoding.readChain`, and `Replay.verify`. The end-to-end test reads them again, from the
database the conversion wrote.

The test checks the sha256 before it uses the file, so a changed fixture fails there and not
somewhere further down.
