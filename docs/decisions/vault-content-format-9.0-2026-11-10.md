# The vault content format of 9.0

Status: **accepted**. A record of the decisions the maintainer took on 2026-11-08 and 2026-11-10, after an
options document (A to F), an external review and a fact-checked measurement pass over the code; not a
rule. Revised on 2026-11-10, the day it was merged: the acceptance's own wording replaces the first
version's choices - preservation of `extensions` moved from raw byte-equal fragments to canonical JSON
(RFC 8785), the vault UUID and the RFC 3339 timestamp rule were added, three sync tickets were added
(#858, #859, #860) with #855 narrowed to the rules for merging groups, and the legacy-id measurement was
taken. Nothing of it is built yet: the binding short form will be the code of the 9.0 initiative, whose
issues the last section names. The measured figures below are of 2026-11-08, against `develop` at
`1eca866f`, and were checked by a second pass against the same commit; the legacy-id measurement of
section 6 is of 2026-11-10, against `c1b930c`, checked the same way.

## 1. What is decided, in one paragraph

The vault content leaves XStream for a **versioned container** (option F): a header with a magic and the
format version as major.minor, one JSON document with the logical model, and attachments as binary blobs
beside the JSON, each with a stable id, its length and a SHA-256 checksum, referenced from the JSON by id.
The container lives inside the unchanged `MCRDB2` envelope (PBKDF2 at 600,000 iterations, AES-GCM,
key-committing, header authenticated), so the crypto is untouched; only the inner bytes change. The file
carries no Java type identity, and an explicit, hand-written mapper connects the JSON tree and the
application's objects - no reflective binding, no polymorphic typing. The target is version **9.0**, a
deliberate major break: released 7.x and 8.x cannot read the new format.

Every 9.0 vault carries a **random vault UUID, stored as the field `vaultId` in the JSON root** - the
binary header carries only the magic and the version, so no binary encoding or position is needed for the
id. The remaining byte-level choices (the magic bytes, byte order, the width of version and length
fields, what the index's offsets refer to) are fixed with test vectors in **#862** before the first byte
is written. Encodings, the maintainer's wording:
"Attachment checksums use SHA-256. Metadata JSON is UTF-8 without BOM. Timestamps are stored as RFC 3339
strings in UTC with millisecond precision, for example 2026-11-10T14:02:00.000Z. No other representation
is allowed. Other 64-bit integers are stored as strings."  9.0 holds attachments **in memory, with a size limit** whose number is set at
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
- **real fields where the type is known**: times as RFC 3339 strings per the rule in section 1, never
  stringly-typed properties for values the host understands; **groups carry a modification time and a
  last-move time exactly as entries do**, and deletions of both land in the tombstone list (section 8);
- one hand-written **mapper class** between the JSON tree and the objects, the only place that knows both
  sides, as `VaultXmlCodec` is that one place today.

**The format part of #775 is solved by this as a side effect**: no silly-bean type is part of the 9.0
format. #775 keeps its other parts - the frozen legacy reader still resolves `KeyValuePair` and
`KeySetPair`, and the obfuscation plugin's rule tables and the panels are its own chapters. The vestigial
`dataOfNodes` that #757 found (closed by #763 before this record) does not exist in the format.

## 4. Unknown fields and plugin data: canonical JSON

Every entity (vault, group, entry, attachment metadata) carries one optional **`extensions` object the
host never interprets**. Plugin data lives in namespaced keys inside it (the lethenon wallet stays an
attachment plus marker values, under the plugin's namespace).

Preservation is specified in the maintainer's wording: "Preservation means semantic equality of JSON
values, not byte equality. The writer emits canonical JSON according to RFC 8785. Round-trip tests
compare parsed JSON values and verify that repeated writes produce identical canonical output. Because
RFC 8785 serializes numbers as IEEE 754 doubles, the format admits JSON numbers only within the safe
integer range (|n| <= 2^53 - 1); any larger integer, anywhere in the document including extensions, is
written as a string. Plugins that need byte-exact data use attachments, not extensions." The first merged
version of this record chose raw byte-equal fragments instead; the acceptance replaced that.

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

Migration identity, the maintainer's wording ("its header" is the `vaultId` field of the JSON root,
section 1): "Every 9.0 vault carries a random vault UUID in its
header. Sync merges only copies of the same 9.0 vault. Legacy copies migrated independently are distinct
vaults: merging them is refused and import is offered instead. If a legacy object already carries a
stable unique id, migration reuses it; otherwise it receives a new random UUID." The modification time is
the migration time unless a better value exists (an entry's history, or the KDBX times where present).

Measured for that rule on 2026-11-10 (develop at `c1b930c`): **an entry's id is already a random UUID** -
`MysticCryptEntryModelBean.id` is `java.util.UUID`, given by `EntryIdentitySupport` to every entry that
lacks one when a vault is opened (#287, #272), in every release since 8.5 (8.5 marked the vault changed
for it; since 8.6 the ids travel with the next real save). It is **not guaranteed unique inside a vault**:
"Duplicate node" copies every entry of the group with its id unchanged, so migration **reuses an entry id
only where it is unique in the vault** and mints new ones for duplicates. **A group made in this
application has no UUID**: the tree key is a per-vault sequential `Long` (`rootTreeAsMap`, `lastId` with
`LongIdGenerator`), so groups **receive new random UUIDs**. A group imported from KDBX carries its KeePass
group UUID in `properties["keepass.uuid"]` (since 8.1.1), which stays a property but is no stable unique
id either - "Duplicate node" and a second import repeat it - so it does not take part in the reuse rule.
An entry of a vault never saved by 8.5 or later gets its UUID at open, before any migration, so two
devices opening the same old file independently still mint different entry ids. The limits and what could
soften them are #856.

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
  the fixtures' README; none of 7.2, 7.3, 8 and 8.1.1 publishes a checksum file (8.2 is the first that
  does), so for all four the sums measured on 2026-11-05 and recorded in
  `src/testSupport/resources/compat/vaults/README.md` are the pin; **no Docker**:
  `scripts/fetch-release-jar.sh` fetches and unpacks the jar, and the fixture writers run it with plain
  `java -cp` (`scripts/fixtures/WriteWithAnOldRelease.java`);
- the migration proof is the round-trip rule of `quality-checks.md` per fixture: open in 9.0, save,
  reopen, compare content, attachments by their SHA-256, through the running application for the critical
  paths, as `LegacyDatabaseMigrationUiTest` does now.

## 8. Sync between devices: the format carries it from 9.0

Syncing is planned as **merging two files, like KeePass's "Synchronize", without a server**. The merge
function is a later release; 9.0 only makes it possible. The format carries, for **entries and groups
alike** (groups are renamed, moved and deleted too):

- a **random stable id (UUID)** per entry and per group;
- a **modification time** per entry and per group, UTC with millisecond precision;
- a **list of deleted objects** (tombstones), covering entries and groups; a tombstone holds the
  **object type, the object id and the deletion time**;
- the **time of the last move** per entry and per group.

**Limit, decided:** the merge's base is a **shared 9.0 vault**, recognised by the vault UUID of section
1 - one migration, then the copies. Legacy copies migrated independently are distinct vaults: merging
them is refused and import is offered instead (#856). The open design questions each have their ticket
and are designed before the merge function is built: tombstone retention #858, clock skew between devices
#859, merge granularity #860 (which can still touch the 9.0 format, as a last-save field out of #859
could), the rules for merging groups #855, and migration identity #856.

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

1. **Tombstone retention** - when a deleted object's record may be discarded: #858.
2. **Clock skew between devices**: #859.
3. **Merge granularity**, per entry or per field - it can still touch the 9.0 format (as a last-save
   field out of #859 could), so it is decided before 9.0 ships: #860.
4. **The rules for merging groups**: #855.
5. **Migration identity**, with the measurement of section 6: #856. Until decided otherwise, the
   shared-vault limit of section 8 is the answer.

Until these are designed, 9.0 carries the fields and no merge exists.

## Standards referenced

- Canonical JSON: RFC 8785 (JSON Canonicalization Scheme), https://www.rfc-editor.org/rfc/rfc8785
- Timestamps: RFC 3339 (Date and Time on the Internet), https://www.rfc-editor.org/rfc/rfc3339
- Checksums: SHA-256 per FIPS 180-4 (Secure Hash Standard), https://csrc.nist.gov/pubs/fips/180-4/upd1/final
- The safe integer range: |n| <= 2^53 - 1, IEEE 754 double precision as ECMA-262 defines it for JSON
  numbers (`Number.MAX_SAFE_INTEGER`), https://tc39.es/ecma262/#sec-number.max_safe_integer

## Issues of this initiative

#775 (its format part solved by the model; #757's dataOfNodes question answered), #847 (save backup, merged), #848 (the legacy reader's admitted types - an independent hardening, wanted with or without 9.0 and outside its scope),
#849 (key-file envelope), #855, #856, #858, #859, #860 (the sync design tickets of section 11), #862 (the binary layout, with test vectors), and the implementation issues to
be cut from this record. The options document this record replaces, with the full comparison and its
evidence, is in the maintainer's private notes.
