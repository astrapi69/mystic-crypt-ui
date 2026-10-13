# How a KeePass file crosses into the vault and back

A record of the decisions taken in the KDBX work on 2026-09-16 and 2026-09-17 (#384 and the
issues it links), not a rule. The binding short form is in `KeePassEntryConverter`,
`KeePassTreeConverter`, `KeePassLibraryFields` and the tests named at the end.

## The state

- **One serializer: KeePassJava2 3.1.0, its KDBX model.** Import reads with `KdbxDatabase.read`,
  export writes a new `KdbxDatabase` with `write` (#407, #510). 3.1.0 made every time a
  `java.time.Instant` and replaced `load` and `save`, which closed the stream they were given, with
  `read` and `write`, which leave it open; both actions open and close their own streams (#510).
  Version 3 removed the Simple, JAXB and DOM
  implementations, renamed the Jackson classes (`JacksonDatabase` to `KdbxDatabase`, and so on),
  dropped the generics from `Database`, `Group` and `Entry`, and moved the credentials to
  `org.linguafranca.pwdb.format.KdbxCredentials`; the artifact is
  `org.linguafranca.pwdb:KeePassJava2.kdbx.database` (#467). The Simple model had already gone
  from the code and the dependencies at 2.2.4, and with it `--add-opens java.base/java.util` from
  the test JVM, the packaged jar's manifest and the conversion plugin (#409).
- **The library's own two defects on this path are fixed.** Up to 2.2.4 it wrote the binary pool
  twice, so KeePassXC complained on every read of an exported file (#379, upstream #97 and #98);
  up to 2.2.5 it wrote the database XML in the JVM's default encoding, so an export containing an
  umlaut could not be read back on a machine whose default was not UTF-8 (upstream #104). Both are
  guarded here now - the round trip asserts the warning is absent, and
  `NonAsciiTextSurvivesANonUtf8JvmTest` writes a database in a child JVM with a non-UTF-8 default
  (#466).
- **One place reflects: `KeePassLibraryFields`.** Jackson keeps an entry's and a group's UUID and
  times, and an entry's history, in `protected` fields with no setter and no factory that takes
  them. The capsule reaches them by name, every name is resolved against the library on the class
  path by `KeePassLibraryFieldsTest`, and a test pins that no other main source reflects on a
  library type (#382). No `--add-opens` is needed for it: the library is in the unnamed module.
- **Its way out is upstream, and half of it has arrived.** jorabin/KeePassJava2#99 landed in 3.0.0:
  `Group` has `getCreationTime`, `getLastModificationTime`, `getLastAccessTime`, `getExpires` and
  `getExpiryTime`, and the import reads a group's times through them since #473 - the capsule is no
  longer on the read path. #96, setters or a factory for UUID, times and history, is still open, so
  the writing stays reflective and the capsule stays. Measured on the 3.1.0 jar
  (`KeePassLibraryFieldsTest`): the field names `uuid`, `times` and `history` are unchanged on
  `KdbxEntry` and `KdbxGroup`. The SPI planned for 3.2.0 (jorabin/KeePassJava2#110) is the way
  out for the writing as well.
- **The KeePass root is a group like any other.** Import hangs it under the vault root with its own
  name; export makes a vault root's only node the database root again. A vault root with several
  nodes exports them under the database's root (#377).
- **The exported header names the vault**: database name = the vault's file name without extension,
  description "Exported from mystic-crypt-ui" (#378).
- **Export writes KDBX 4.0.** `KdbxDatabase.write` without a stream format uses `new KdbxHeader(4)`
  (library source, 3.1.0, `KdbxDatabase.write(Credentials, OutputStream)`), and 3.1.0 labels every
  KDBX 4 file it writes 4.0 (jorabin/KeePassJava2#114). That label is right for what this
  application exports: the converters set none of the 4.1 elements (tags, `PreviousParentGroup`,
  `QualityCheck`).

## What a round trip keeps

Measured by `KdbxRoundTripKeepsEveryFieldUiTest` through the application's menus - import, save,
end, sign in, export - against `src/e2eKdbx/resources/kdbx/keepassxc-2.7.10-kdbx4.kdbx`, both files
read with `keepassxc-cli export -f xml`, 11 of 11 green since the group times were added (#413):
locally with keepassxc-cli 2.7.10, and in CI with 2.7.6 (CI run 35726707643; the version is printed
on every run since #407):

- group structure with no added level and no renaming; group names, identifiers, icon indices, and
  every group's four times with its expiry flag, kept in the vault as ISO-8601 text in the group's
  properties (#413)
- per entry: identifier; creation, last modification, last access and expiry time with the expiry
  flag; icon index; title, user name, password, URL, notes, umlauts included; custom properties and
  which of them are protected (#389); attachments with their content; the history
- the header, as above

Two things the import normalizes, decided by the maintainer: an empty notes, URL or user name
becomes null, as the Simple reader delivered it and existing vaults carry it; a duplicated entry
carries no history and its own copies of every list and of the protected names (#407).

## What a round trip does not carry

Read from the converters with `grep -n 'RECYCLE_BIN' KeePassTreeConverter.java` (written on import
only) and
`grep -n 'customIcon\|Tags\|Color\|AutoType\|CustomData\|UsageCount\|LocationChanged' KeePassEntryConverter.java`
(no match). None of them is asserted by the round trip test:

- a group's recycle-bin flag - kept in the vault, not written back
- an entry's custom icon, tags, colours, auto-type settings and custom data
- `UsageCount` and `LocationChanged`, outside the agreed scope from the start

## Known and not fixed

- **8.5.1 and older do not import an 8.6 export.** Their Simple reader requires an empty
  `DefaultUserName` the writer leaves out (jorabin/KeePassJava2#101). KeePass and KeePassXC read
  the file; accepted by the maintainer, and said in the CHANGELOG format section.
- **UUID, times and history still travel by reflection** on the way out, because
  jorabin/KeePassJava2#96 is open. Everything else on that list is closed.
- **A KDBX 3.1 file with protected attachments cannot be read** by 3.1.0, as the library's author
  states in jorabin/KeePassJava2#114 (comment of 2026-10-10); 3.1.2 is to fix it. Not measured
  here: no fixture of that kind exists.

## Fixed since this record was written

- **The binary warning** (#379, upstream #97 and #98): KDBX 4 writing left the attachment pool in
  the XML as well as in the inner header, and the Jackson model wrote `Meta/Binaries` without its
  wrapper. Both landed in 2.2.5. Measured on a file this application exports, with keepassxc-cli
  2.7.10: 2.2.4 printed `skip element "Binaries"` at 1457 bytes, 2.2.6 prints nothing at 1345
  bytes, and the attachment still comes back byte for byte. The round trip test, which pinned the
  warning as PRESENT, is now inverted and guards its return (#466).
- **The non-UTF-8 export** (upstream #104): up to 2.2.5 the database XML was written in the JVM's
  default encoding, so an export containing an umlaut could not be read back on a machine whose
  default was not UTF-8 - data loss decided by the exporting JVM. Fixed in 2.2.6, and guarded by
  `NonAsciiTextSurvivesANonUtf8JvmTest`, which writes a database in a child JVM with
  `-Dfile.encoding=ISO-8859-1` (#466).
- **The two serializers reading each other** (upstream #100) and **KDBX 3.1 losing attachments**
  (upstream #98): version 3 has one implementation, so the first cannot arise any more, and the
  second is fixed.

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
