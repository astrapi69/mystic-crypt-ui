# The 9.0 container: the byte layout

The bytes of the container that holds a 9.0 vault's content, fixed before the first byte is written (#862). The
shape is decided in [the 9.0 decision record](decisions/vault-content-format-9.0-2026-11-10.md), section 1; the
conventions are the maintainer's decision of 2026-11-24 on #862 (option a, those of `MCRDB2`, see
[the database format](vault-format.md)):

- an ASCII magic with a version;
- big endian for every integer;
- major and minor as two unsigned 2-byte integers;
- the length of a blob as 8 bytes;
- offsets relative to the start of the blob area.

The container is the plaintext of whichever envelope the sign-in variant uses: the `MCRDB2` envelope for the
master password, the key-file envelope of #849 for the other two. Nothing in it is encrypted on its own; the
envelope encrypts and authenticates all of it. Nothing of it is built yet: the reader and the writer are #865.

## The layout

```
offset   length   field
-------  -------  ------------------------------------------------------------
      0        6  magic, the ASCII bytes "MCVLT1"
      6        2  major version, unsigned, big endian (9)
      8        2  minor version, unsigned, big endian (0)
     10        8  J, the length of the JSON in bytes, unsigned, big endian
-------  -------  header ends at 18
     18        J  the JSON document, UTF-8 without a byte order mark
   18+J      ...  the blob area, to the end of the container
```

The blob area is a sequence of attachments, nothing before, between or after them:

```
offset within the area   length   field
----------------------   -------  -----------------------------------------------
                     o         8  L, the length of the attachment's bytes, unsigned, big endian
                   o+8         L  the attachment's bytes
```

The first attachment starts at offset 0 of the area, every next one at `o + 8 + L` of the one before. A
container without attachments ends after the JSON. The fixed overhead is the 18 header bytes plus 8 bytes per
attachment.

## What the fields mean

- **The magic names the layout, the version fields name the content.** `MCVLT1` changes only when this table
  changes: another header field, another width, another order. The content format's version lives in the two
  version fields, so 9.1 or 10.0 is written with the same magic. The `1` follows the family's pattern of a
  version in the magic (`MCRDB2`, `MCFILE1`), as the decision asks; it is the layout's first version, not the
  application's.
- **The JSON length is 8 bytes** like an attachment's, so both length fields of the container read alike. It
  counts bytes, not characters.
- **An offset in the JSON index points at the attachment's length prefix**, relative to the start of the blob
  area, so the JSON's own length does not shift it: a writer can produce the index before it knows how long the
  JSON will be. A reader that seeks to one attachment reads its 8-byte length there and its bytes after it,
  without reading any other attachment. The index also records the length, and it must agree with the prefix.
- **Unsigned 8-byte values above 2^63 - 1** are refused as `JSON_PAST_THE_END` or `TRUNCATED_ATTACHMENT`:
  Java has no unsigned long to hold them, and no container that long can exist in memory, where 9.0 keeps
  attachments (decision record, section 1).

The names of the index's fields in the JSON (`attachments`, `id`, `offset`, `length` in the vectors) belong to
the logical model of #865, which may rename them; the vectors change with it. This table does not.

## What a reader refuses

In the order a reader meets them; the first that applies is the verdict:

| verdict | when |
|---|---|
| `TRUNCATED_HEADER` | the container is shorter than 18 bytes |
| `WRONG_MAGIC` | the first 6 bytes are not `MCVLT1` |
| `NEWER_MAJOR` | the major is above the reader's; the message names the version the file needs (decision record, section 5) |
| `UNKNOWN_MAJOR` | the major is below 9, which no container carries |
| `JSON_PAST_THE_END` | J is larger than what follows the header |
| `JSON_WITH_BOM` | the JSON starts with the bytes `ef bb bf` |
| `JSON_NOT_UTF8` | the JSON is not valid UTF-8 |
| `TRUNCATED_ATTACHMENT` | an attachment's length prefix or bytes run past the end of the container |
| `INDEX_MISMATCH` | the JSON index does not name exactly the attachments the area holds, with their offsets and lengths |

A newer minor of the same major is accepted read-only (decision record, section 5): the reader reads what it
knows and refuses every write.

## The test vectors

Under `src/test/resources/vault/container-vectors/`, one annotated hex file per case. A `# verdict:` line names
what a reader must reach (`accept`, `accept read-only`, or `refuse` and the verdict above), a `# blob offsets:`
line the offsets an accepted container's attachments start at, and the comment before each block says which
part of the layout it is. `ContainerLayoutVectorsTest` walks every vector by this table alone and checks the
verdict, so the table and the vectors cannot drift apart; it also requires a vector for every verdict. The reader
of #865 runs the same vectors and must reach the same verdicts.

| vector | what it holds |
|---|---|
| `accept-no-attachments` | 9.0, the smallest container: header and JSON |
| `accept-one-attachment` | 9.0, one attachment of 5 bytes |
| `accept-two-attachments` | 9.0, two attachments, the second at offset 13 (8 + 5) |
| `accept-newer-minor` | 9.1, read-only |
| `refuse-truncated-header` | 10 of the 18 header bytes |
| `refuse-wrong-magic` | `MCRDB2` where `MCVLT1` belongs |
| `refuse-newer-major` | 10.0 |
| `refuse-older-major` | 8.0 |
| `refuse-json-past-the-end` | J names 4 bytes more than the file holds |
| `refuse-json-with-bom` | the JSON starts with a byte order mark |
| `refuse-json-not-utf8` | the JSON holds the byte `ff` |
| `refuse-truncated-attachment` | a length prefix names 5 bytes, 3 follow |
| `refuse-index-mismatch` | the index names 5 bytes, the area holds 4 |
