# How a KeePass file crosses into the vault and back

A record of the decisions taken in the KDBX work on 2026-09-16 and 2026-09-17 (#384 and the
issues it links), not a rule. The binding short form is in `KeePassEntryConverter`,
`KeePassTreeConverter`, `KeePassLibraryFields` and the tests named at the end.

## The state

- **One serializer: KeePassJava2 2.2.4, its Jackson model.** Import reads with
  `JacksonDatabase.load`, export writes a new `JacksonDatabase` (#407). The Simple model is gone
  from the code and the dependencies, and with it `--add-opens java.base/java.util` from the test
  JVM, the packaged jar's manifest and the conversion plugin (#409).
- **One place reflects: `KeePassLibraryFields`.** Jackson keeps an entry's and a group's UUID and
  times, and an entry's history, in `protected` fields with no setter and no factory that takes
  them. The capsule reaches them by name, every name is resolved against the library on the class
  path by `KeePassLibraryFieldsTest`, and a test pins that no other main source reflects on a
  library type (#382). No `--add-opens` is needed for it: the library is in the unnamed module.
- **Its way out is upstream.** jorabin/KeePassJava2#96 asks for setters or a factory for UUID, times
  and history, #99 for getters of a group's times. When either lands, the capsule's methods become
  delegations and the reflection goes.
- **The KeePass root is a group like any other.** Import hangs it under the vault root with its own
  name; export makes a vault root's only node the database root again. A vault root with several
  nodes exports them under the database's root (#377).
- **The exported header names the vault**: database name = the vault's file name without extension,
  description "Exported from mystic-crypt-ui" (#378).
- **Export writes KDBX 4.** `JacksonDatabase.save` without a stream format uses `new KdbxHeader(4)`
  (library source, `JacksonDatabase.save(Credentials, OutputStream)`).

## What a round trip keeps

Measured by `KdbxRoundTripKeepsEveryFieldUiTest` through the application's menus - import, save,
end, sign in, export - against `src/e2eKdbx/resources/kdbx/keepassxc-2.7.10-kdbx4.kdbx`, both files
read with `keepassxc-cli export -f xml`, 10 of 10 green locally with keepassxc-cli 2.7.10 and in CI
with 2.7.6 (CI run 35205468621; the version is printed on every run since #407):

- group structure with no added level and no renaming; group names, identifiers and icon indices
- per entry: identifier; creation, last modification, last access and expiry time with the expiry
  flag; icon index; title, user name, password, URL, notes, umlauts included; custom properties and
  which of them are protected (#389); attachments with their content; the history
- the header, as above

Two things the import normalizes, decided by the maintainer: an empty notes, URL or user name
becomes null, as the Simple reader delivered it and existing vaults carry it; a duplicated entry
carries no history and its own copies of every list and of the protected names (#407).

## What a round trip does not carry

Read from the converters with `grep -n 'setTimes\|getTimes' KeePassTreeConverter.java` (no match),
`grep -n 'RECYCLE_BIN' KeePassTreeConverter.java` (written on import only) and
`grep -n 'customIcon\|Tags\|Color\|AutoType\|CustomData\|UsageCount\|LocationChanged' KeePassEntryConverter.java`
(no match). None of them is asserted by the round trip test, whose group facts are path, name,
icon index and identifier:

- a group's four times - not read on import, not written on export; measured lost on a round trip
  of the fixture, every one of them becoming the moment of the export (#413)
- a group's recycle-bin flag - kept in the vault, not written back
- an entry's custom icon, tags, colours, auto-type settings and custom data
- `UsageCount` and `LocationChanged`, outside the agreed scope from the start

## Known and not fixed

- **The binary warning** (#379). KDBX 4 writing leaves the attachment pool in the XML as well as in
  the inner header, a TODO in the library's `KdbxStreamFormat.save` (jorabin/KeePassJava2#97).
  KeePassXC 2.7.10 reports it as `skip element "Binaries"` for a Jackson-written file; it said
  `overwriting binary item "0"` while Simple wrote it. The attachment arrives either way. The round
  trip test pins the warning as present, so the fix is noticed when it lands.
- **8.5.1 and older do not import an 8.6 export.** Their Simple reader requires an empty
  `DefaultUserName` the Jackson writer leaves out (jorabin/KeePassJava2#101). KeePass and KeePassXC
  read the file; accepted by the maintainer, and said in the CHANGELOG format section.
- **The two library serializers do not read each other in every case**: Jackson cannot load a file
  Simple re-saved with history (jorabin/KeePassJava2#100). No file this application wrote is
  affected - its export built new entries - but a KeePass file produced that way elsewhere would not
  import.
- **Jackson written as KDBX 3.1 loses attachments** (jorabin/KeePassJava2#98). This application
  writes KDBX 4 and is not affected.

## What was rejected

Decided by the maintainer in phase C (2026-09-16): Jackson as the serializer, reflection as the
bridge, a contribution upstream as the way out; a fork of KeePassJava2 and the library's DOM module
were rejected. What had been measured in phase B beforehand:

- the DOM module can write UUID and timestamps (`DomHelper.setElementContent`, the element names in
  `DomEntryWrapper`), on a different set of types from the ones the converter used
- the Simple model needed `--add-opens java.base/java.util` for its serializer
  (`InaccessibleObjectException ... java.util.UUID.mostSigBits` without it) and exposes no public
  accessor for the history (`protected List<SimpleEntry> history`)
- at library level both models kept everything the fixture holds, history and group identifiers
  included: what phase A lost, the application's converter lost

## What checks it

- `KdbxRoundTripKeepsEveryFieldUiTest` - the round trip above, through the menus, against KeePassXC.
- `KeePassLibraryFieldsTest` - every field name resolves; nothing else reflects on library types.
- `KeePassEntryConverterTest`, `KeePassTreeConverterTest` - both directions, including unset fields
  importing as null.
- `KeePassImportUiTest`, `KeePassExportUiTest`, `KeePassRoundTripUiTest`,
  `KeePassImportKeyFileUiTest` - the menus, without an "Imported from" level.
- `VaultOpensInTheLastReleaseTest` - a vault with imported history or protected names needs 8.6;
  one without opens in the published 8.5.1 (see `vault-format-version-2026-09-17.md`).
