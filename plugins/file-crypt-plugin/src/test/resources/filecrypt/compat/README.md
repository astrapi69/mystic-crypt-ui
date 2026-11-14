# Files the library's command line wrote

`FileCryptSupportAndTheCommandLinesFilesTest` hands these to the file encryption window, for #877: the window reads
only its own marker, `MCFILE1`, and a user who sealed a file with `mystic-crypt encrypt` could not open it there.

- `written-by-encrypt.txt.mcenc`, 117 bytes: `encrypt -i <file> -o <this file> --passphrase-stdin` of a file holding
  `a file the command line sealed` and a line break, the marker `MCFILE`.
- `written-by-encrypt.text`: `encrypt --text 'a text the command line sealed: ä ö ü ß' --passphrase-stdin`, the
  Base64 it prints, 129 bytes decoded, the marker `MCFILE`.
- `written-by-passphrase-cryptor.mcrypt`, 135 bytes: `PassphraseCryptor.encrypt` of
  `a file sealed in the layout encrypt wrote first` and a line break, the layout `MCRYPT` that `encrypt` wrote before
  it moved to `MCFILE` and that `decrypt` still reads.

The passphrase is `FileCryptSupportAndTheCommandLinesFilesTest.COMMAND_LINE_FILES_FIXTURE`. The text is invented and
guards nothing.

## How they were written

On 2026-11-14, with OpenJDK 25.0.4.1 and the published mystic-crypt 13.10 from Maven Central, resolved into a build
of its own, so the version this repository pins did not decide it:

```
scripts/fixtures/write-file-crypt-formats.sh command-line <empty directory>
```

with the passphrase on standard input. `encrypt` runs through `MysticCryptCli.execute`, which is the `main` of the
`mystic-crypt` command without its exit.

| file | sha256 |
|---|---|
| `written-by-encrypt.txt.mcenc` | `b69996d62a837d1371eaaf630f7d70bcc12c267b521725384bc76a0942982779` |
| `written-by-encrypt.text` | `abb94c51270c5a03569cf629f992cf1e27a5aebaca71bec0967708cf0563743a` |
| `written-by-passphrase-cryptor.mcrypt` | `bb55c6bf4b7fadb5bdfd2d64baa2b9200b595fdc6fb01cb825fd853cef66bee0` |

The byte after the `MCFILE` marker of both `encrypt` files is the first byte of a random salt: 0x4a for the file,
0xbe for the text. Neither is `1` (0x31), so neither also starts with `MCFILE1`.
