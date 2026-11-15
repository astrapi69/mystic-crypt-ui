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

## The window of the published 8.2

`written-by-the-window-8.2.txt.mcenc` (135 bytes, the marker `MCFILE1`) and `written-by-the-window-8.2.text` (196
Base64 characters) were written by the file encryption window of the published 8.2, the first release with the
window, before #490 moved it onto the library's `PassphraseEnvelope` (from 8.7, "byte for byte the same layout"). They
hold `a file the file encryption window of 8.2 sealed` and a line break, and
`a text the file encryption window of 8.2 sealed: ä ö ü ß`. The passphrase is the one above. `DecryptWhatTheWindowWroteTest` opens them with `--cli decrypt`.

On 2026-11-15, with OpenJDK 25.0.5 and nothing of this checkout on the class path:

```
scripts/fixtures/write-file-crypt-of-an-old-release.sh 8.2 <empty directory>
```

It took `mystic-crypt-ui-8.2-installer.jar` from RELEASE-8.2, checked it against the published sha256
`c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626`, and called that release's own
`FileCryptSupport.encryptFile` and `encryptText` from its application jar and its file-crypt plugin
(`scripts/fixtures/WriteFileCryptWithAnOldRelease.java`). The same two files are in `plugins/file-crypt-plugin/src/test/resources/filecrypt/compat`, where today's window opens them (`FileCryptSupportOpensWhatEightTwoWroteTest`), byte for byte.

| file | sha256 |
|---|---|
| `written-by-the-window-8.2.txt.mcenc` | `b0d6bfbb4112cd7d49ba046491be12d2c207010b05e38092e95d356c195559f8` |
| `written-by-the-window-8.2.text` | `72fb42636ee464bc6e6f3ff8643705e8d294f6d8cedfd858422e909fad0049fc` |
