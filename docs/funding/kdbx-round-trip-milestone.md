# Milestone: a lossless KDBX round trip

mystic-crypt-ui can take a KeePass database in and give it back out unchanged, within the scope
listed below. This page is the evidence for that milestone: what was delivered, how it is measured,
what lies outside the scope, and what is known and not fixed. Every statement links to the issue or
pull request where it was measured.

**The work was done between 2026-09-16 and 2026-09-22 and completed before any funding
commitment.**

## The scope

Agreed by the maintainer on 2026-09-16, before the implementation started. A database written by
another KeePass client, imported into this application and exported again, keeps:

- the group structure, with no added level and no renamed group
- group names, identifiers, icon indices, and every group's four timestamps with its expiry flag
- per entry: identifier; creation, last modification, last access and expiry time with the expiry
  flag; icon index; title, user name, password, URL and notes; custom properties, including which of
  them are protected; attachments with their content; the version history

## How it is measured

Not against this application's own model - a field the model does not carry would compare equal to
itself. Against the program a user opens these files with:

- **The fixture** is a KDBX 4 database with Argon2d, written by KeePassXC 2.7.10, with an entry that
  carries every field above, including a protected custom property, an attachment and a history
  version (#380, `src/e2eKdbx/resources/kdbx/`).
- **The test** is `KdbxRoundTripKeepsEveryFieldUiTest`. It drives the running application through
  its menus: import the fixture, save the vault, end the application, sign in again, export. Both the
  fixture and the export are read with `keepassxc-cli export -f xml`, and the two dumps are compared
  field by field. Every run prints the keepassxc-cli version it compared with.
- **The result**: 11 of 11 green, locally with keepassxc-cli 2.7.10 and in CI with keepassxc-cli
  2.7.6 (CI run 35726707643). The test was written first and was red: seven of its ten original
  cases failed against the converter as it was (#384, #385), and the group-times case failed until
  #419.

## What was delivered

| Step | Issue | Pull request |
|---|---|---|
| Findings of the first measurement | #377, #378, #379 | - |
| A KDBX 4 fixture written by KeePassXC | #380, #390 | #381, #391 |
| One class that reaches the library's protected fields, and nothing else that reflects | #382 | #383 |
| The round trip test, red first | #384 | #385 |
| Entry history and protected property names in the vault; vault format version; newer vaults open read-only | #402, #404, #405 | #403, #406 |
| The converter on KeePassJava2's Jackson model, import and export | #377, #378, #384, #389 | #407 |
| No `--add-opens`, and no second serializer | #408 | #409 |
| Every group's timestamps | #413 | #419 |
| Decision record `docs/decisions/kdbx-bridge-2026-09-17.md` | #411 | #412 |

Found along the way and fixed, outside the milestone: three data paths in the released 8.5 (#386,
#387, #388, shipped as 8.5.1), a timeout that turned the round trip test red on slow CI runners (#414,
#415), and a build cache split so that a pull request re-runs only the test suites its change can
affect (#319, #417, #418).

## Outside the scope

Named as excluded when the scope was agreed: `UsageCount` and `LocationChanged`.

Not part of the scope either, and not carried - read from the converters, not asserted by the test:

- a group's recycle-bin flag (kept in the vault on import, not written back on export)
- an entry's custom icon, tags, colours, auto-type settings and custom data

## Known and not fixed

- **A warning KeePassXC prints on reading an exported file**, `skip element "Binaries"`: the library
  leaves the attachment pool in the XML when writing KDBX 4 as well as in the header. The attachment
  itself arrives. The test pins the warning as present, so an upstream fix is noticed (#379).
- **8.5.1 and older do not import a KDBX file exported by 8.6**: their reader requires an empty
  `DefaultUserName` element the KDBX format does not call for. KeePass and KeePassXC read the file.
- **A vault that holds an imported history or protected property names needs 8.6.** One without them
  opens in the published 8.5.1, measured against its release jar (#402).

## Reported upstream to KeePassJava2

The library this application builds on, reported with the measurements that found them. No pull
requests before the maintainer of KeePassJava2 has answered.

| Issue | Subject |
|---|---|
| jorabin/KeePassJava2#96 | no setter or factory for an entry's or group's UUID and times, or an entry's history |
| jorabin/KeePassJava2#97 | KDBX 4 writes attachments twice, the TODO in `KdbxStreamFormat.save` |
| jorabin/KeePassJava2#98 | the Jackson model writes `Meta/Binaries` without its wrapper; KDBX 3.1 attachments are lost |
| jorabin/KeePassJava2#99 | `JacksonGroup` has no getter for its times |
| jorabin/KeePassJava2#100 | Jackson cannot load a file the Simple model re-saved with history |
| jorabin/KeePassJava2#101 | Simple cannot load a file the Jackson model wrote: `DefaultUserName` required |

When #96 and #99 are resolved, the one class that reaches the library by reflection becomes plain
delegation and the reflection goes.
