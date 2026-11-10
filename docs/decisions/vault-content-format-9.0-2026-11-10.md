# The vault content format of 9.0

Status: **accepted**. A record of the decisions the maintainer took on 2026-11-08 and 2026-11-10, after an
options document (A to F), an external review and a fact-checked measurement pass over the code; not a
rule. Nothing of it is built yet: the binding short form will be the code of the 9.0 initiative, whose
issues the last section names. The measured figures below are of 2026-11-08, against `develop` at
`1eca866f`, and were checked by a second pass against the same commit.

## 1. What is decided, in one paragraph

The vault content leaves XStream for a **versioned container** (option F): a header with a magic and the
format version as major.minor, one JSON document with the logical model, and attachments as binary blobs
beside the JSON, each with a stable id, its length and a SHA-256 checksum, referenced from the JSON by id.
The container lives inside the unchanged `MCRDB2` envelope (PBKDF2 at 600,000 iterations, AES-GCM,
key-committing, header authenticated), so the crypto is untouched; only the inner bytes change. The file
carries no Java type identity, and an explicit, hand-written mapper connects the JSON tree and the
application's objects - no reflective binding, no polymorphic typing. The target is version **9.0**, a
deliberate major break: released 7.x and 8.x cannot read the new format.

Encodings, fixed: the JSON metadata is **UTF-8 without a BOM**; attachment checksums are **SHA-256**,
written as lowercase hex. 9.0 holds attachments **in memory, with a size limit** whose number is set at
implementation time and refused with a message that names it; the container is laid out so streaming can
come later without a format break - every blob is length-prefixed, and the JSON carries an index with each
blob's offset into the blob area, so a later reader can seek to one attachment without parsing the others.

## 2. Why XStream leaves

Three recorded problems are symptoms of one design property - the file carries Java class names and the
reader constructs from them:

- a 7.x vault named `io.github.astrapi69.swing.tree.GenericTreeElement`; the class moved package in 8.x
  and the vault stopped opening, reported as a wrong password (#624, fixed by an alias table);
- silly-bean is archived and its `KeyValuePair`/`KeySetPair` sit in the format, so the replacement must
  read the old names exactly as #624 does (#775, open);
- both CWE-502 advisories came from type-driven construction: the reader once built every type of the
  libraries (GHSA-w87j-p568-fr6v, fixed 8.12 with a six-type allow-list), and the key-file path fed Java
  serialization unfiltered (GHSA-xp4c-rpcx-hw5c, fixed 8.13 with an exact-type filter); the breadth is
  also what once let a property path reach commons-beanutils (GHSA-9m97-3xjj-4gwm, fixed 8.11).

A format with stable string field names and no embedded type identity removes the class of problem rather
than patching each instance. #18 (2023) tried JAXB and was abandoned unfinished, not rejected on merit.

## 3. The logical model: cleaned, not transcribed

Measured today: entries live in `rootTreeAsMap -> TreeIdNode -> GenericTreeElement.properties
["default_content"]`; `dataOfNodes` is vestigial, null in every app-written vault (#757); the KDBX group
times ride as ISO text in string properties. The 9.0 model instead:

- **flat entries with a group reference** (the entry names its group's id), **groups with a parent
  reference**; no map level, no tree-node wrapper types, no `dataOfNodes`;
- **real fields where the type is known**: times as UTC ISO-8601 instants, never stringly-typed
  properties for values the host understands;
- one hand-written **mapper class** between the JSON tree and the objects, the only place that knows both
  sides, as `VaultXmlCodec` is that one place today.

**#757 and #775 are solved by this as a side effect**: `dataOfNodes` does not exist in the format, and no
silly-bean type is part of it.

## 4. Unknown fields and plugin data: raw fragments

Every entity (vault, group, entry, attachment metadata) carries one optional **`extensions` object the
host never interprets**. Plugin data lives in namespaced keys inside it (the lethenon wallet stays an
attachment plus marker values, under the plugin's namespace).

Preservation is specified at the byte level: the mapper keeps each `extensions` object as the **raw JSON
fragment it was read as, and writes those bytes back unchanged**. A parsed-and-reserialized tree may
change key order, whitespace, number formatting or Unicode escaping, so parsing is not the preservation
mechanism; the fragment is. The round-trip test asserts byte equality of every `extensions` fragment
after read-and-write. The parts the host itself writes carry no byte-stability promise (the host may
normalize its own output), only the UTF-8 rule above. Canonical JSON (RFC 8785) was considered and
rejected: it buys a canonical equality the raw fragment already gives, at the price of a canonicalization
implementation.

A newer minor version's additions land in `extensions` or in new optional fields, which an older 9.x
reader keeps through the same mechanism - that is what makes the minor rule below safe.

## 5. The version rule

- **A newer major version is not readable.** The reader refuses it with a message naming the version the
  file needs - never "wrong password".
- **A newer minor version opens read-only.** The reader reads what it knows, keeps the rest (section 4),
  and refuses every write, as #402 decided for the XStream format: a save would silently drop what this
  build cannot read. Same major, same or older minor: read and write.

## 6. Migration

**The legacy readers stay for good.** A vault migrates when it is opened and saved, and a user may first
open a 7.2 vault years from now. So `PasswordVaultFormat.decryptLegacy`, the `MCRDB2` reader, the key-file
legacy paths and `VaultXmlCodec` with its alias table stay in the tree permanently as read-only legacy
paths; the alias table is **frozen**, because nothing new is ever written in the XStream format. The write
path writes 9.0 only. XStream therefore stays a dependency for reading; #848 (the types that reader
admits) applies to the legacy path and is wanted independently of 9.0.

**Before the first conversion of a vault, the previous file is kept as a backup and the user is told
so** - in addition to the general save backup of #847, because this one crosses a format boundary the old
release could still read.

Migration identity: entries and groups of old vaults get **new UUIDs**, and the **migration time as their
modification time unless a better value exists** (an entry's history, or the KDBX times where present).
The consequence for syncing and its limit are in section 8 and #856.

The key-file envelope is its own piece: #849 replaces the Java-serialized `AesRsaCryptModel` with a fixed
binary header, versioned and refusable, independent of the content format; the 9.0 container goes inside
whichever envelope the sign-in variant uses, exactly as the XML does today.

## 7. Fixtures first: the proof gate

Before any building starts, the fixture set is completed - today 4 of the 12 release x variant
combinations have a checked-in fixture (7.2 password, 7.3 password, 8 key-file, 8 password+key-file),
though all 12 were measured open:

- fixtures for **all 12 combinations** (7.2, 7.3, 8, 8.1.1 x password, key file, both), written by the
  published release jars through their own save paths, as `scripts/fixtures/WriteWithAnOldRelease.java`
  does it;
- the release jars come **from the GitHub release pages, checked against fixed sha256 sums** recorded in
  the fixtures' README; for 7.x and 8.1.1, which publish no checksum file, the sums measured on
  2026-11-05 and recorded in `src/testSupport/resources/compat/vaults/README.md` are the pin; **no
  Docker**, plain `java -cp` as `scripts/fetch-release-jar.sh` does it;
- the migration proof is the round-trip rule of `quality-checks.md` per fixture: open in 9.0, save,
  reopen, compare content, attachments by their SHA-256, through the running application for the critical
  paths, as `LegacyDatabaseMigrationUiTest` does now.

## 8. Sync between devices: the format carries it from 9.0

Syncing is planned as **merging two files, like KeePass's "Synchronize", without a server**. The merge
function is a later release; 9.0 only makes it possible. The format carries, for **entries and groups
alike** (groups are renamed, moved and deleted too):

- a **random stable id (UUID)** per entry and per group;
- a **modification time** per entry and per group, UTC with millisecond precision;
- a **list of deleted objects with their deletion time** (tombstones), covering entries and groups;
- the **time of the last move** per entry and per group.

**Limit, decided:** the merge's base is a **shared 9.0 vault** - one migration, then the copies. Two
independent migrations of the same legacy vault create different UUIDs and are different objects to the
merge; whether deterministic ids can soften that is #856. The merge rules themselves (who wins, conflict
copies, tombstone lifetime, move against edit) are #855 and are designed before the merge function is
built.

## 9. Attachments

An attachment is **id, name, length, offset, SHA-256 checksum and bytes**, plus an **optional
`contentType`**, never required and empty for everything migrated (nothing today records one:
`FileContentInfo` has none, KDBX binaries carry none). A viewer and the KDBX export can use it when
present.

## 10. Alternatives considered

- **A, XStream as today**: the cause of section 2; stays as the frozen legacy reader only.
- **B, JAXB/XSD**: buys formal XML validation nothing requires; #18 remains the unfinished attempt.
- **C, plain JSON DTOs**: F without the blob container; Base64 attachments grow the file and block
  streaming. F contains C.
- **D, CBOR/Protobuf**: binary evolution and size that nothing measured requires, poorer to audit;
  Protobuf adds code generation (BouncyCastle ships a low-level CBOR codec, but no data binding).
- **E, KDBX as the main format**: collides with the `MCRDB2` envelope (double crypto, or handing the key
  derivation and container crypto to a third-party library), cannot hold the app's version rule and ids
  natively, and binds the vault to a dependency with a single maintainer and a spec this project does not
  own. KDBX stays import and export.

## 11. Open points, each with its ticket

1. **The merge rules** for the two-file synchronization: #855. Until designed, 9.0 carries the fields and
   no merge exists.
2. **Migration identity**: #856. Until decided otherwise, the shared-vault limit of section 8 is the
   answer.

## Issues of this initiative

#757 and #775 (solved by the model), #847 (save backup, merged), #848 (legacy reader's admitted types),
#849 (key-file envelope), #855 (merge rules), #856 (migration identity), and the implementation issues to
be cut from this record. The options document this record replaces, with the full comparison and its
evidence, is in the maintainer's private notes.
