# A file and a text the file encryption window wrote

`DecryptWhatTheWindowWroteTest` hands these to `--cli decrypt`, the library's command line inside the application,
for #877: the window writes the marker `MCFILE1`, and a `decrypt` that reads only `MCFILE` cannot open them.

- `written-by-the-window.txt.mcenc`, 128 bytes: `FileCryptSupport.encryptFile` of a file holding
  `a file the file encryption window sealed` and a line break.
- `written-by-the-window.text`: `FileCryptSupport.encryptText` of `a text the file encryption window sealed: ä ö ü ß`,
  the Base64 the window shows, 140 bytes decoded.

The passphrase is `TestPasswords.FILE_CRYPT_WINDOW_FIXTURE`. The text is invented and guards nothing.

## How they were written

On 2026-11-14, with OpenJDK 25.0.4.1 and the window's `FileCryptSupport` of develop at `0cb31006`, unchanged since
`d01416d6` (#490), which RELEASE-8.14 contains, compiled against the published mystic-crypt 13.10:

```
scripts/fixtures/write-file-crypt-formats.sh window <empty directory>
```

with the passphrase on standard input.

| file | sha256 |
|---|---|
| `written-by-the-window.txt.mcenc` | `d00c5d18c8219251c5c8e0ec0c5ec93394afb3e749fe86805cbd03b8fbba70f8` |
| `written-by-the-window.text` | `fef8903eeb255cddc4f1771230307ccb93c2042ecf4c69f923e75400c6654211` |

## Why this run and not the first one

Read as an `MCFILE`, as 13.10's `decrypt` reads every file starting with `MCFILE`, the last byte of the window's salt
becomes the first byte of the iteration count. When that byte is 0x80 or more the count is negative and 13.10 refuses
at once, with exit code 2; when it is below, the count is in the millions and 13.10 derives a key for minutes before
it refuses. The test that pins 13.10's refusal cannot wait for that, so the script was run until both files had a
last salt byte of 0x80 or more, which took eight runs: 0xd4 for the file, 0xe2 for the text. What the candidate
of 13.11 does with the other case is a test of the library (`PassphraseFileFormatTest`, mystic-crypt#307), which
holds a window file whose last salt byte is 0x43.
