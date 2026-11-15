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

## The window of the published 8.2

`written-by-the-window-8.2.txt.mcenc` (135 bytes, the marker `MCFILE1`) and `written-by-the-window-8.2.text` (196
Base64 characters) were written by the file encryption window of the published 8.2, the first release with the
window, before #490 moved it onto the library's `PassphraseEnvelope` (from 8.7, "byte for byte the same layout"). They
hold `a file the file encryption window of 8.2 sealed` and a line break, and
`a text the file encryption window of 8.2 sealed: ä ö ü ß`. The passphrase is the host's `TestPasswords.FILE_CRYPT_WINDOW_FIXTURE`, kept in the test as `WINDOW_FILES_FIXTURE`. `FileCryptSupportOpensWhatEightTwoWroteTest` opens them with today's window.

On 2026-11-15, with OpenJDK 25.0.5 and nothing of this checkout on the class path:

```
scripts/fixtures/write-file-crypt-of-an-old-release.sh 8.2 <empty directory>
```

It took `mystic-crypt-ui-8.2-installer.jar` from RELEASE-8.2, checked it against the published sha256
`c00409e0ede55e459693485e3d3024bc8076f92b209f7f322ac2e4d089efb626`, and called that release's own
`FileCryptSupport.encryptFile` and `encryptText` from its application jar and its file-crypt plugin
(`scripts/fixtures/WriteFileCryptWithAnOldRelease.java`). The same two files are in `src/test/resources/compat/file-crypt` of the host, where `--cli decrypt` opens them (`DecryptWhatTheWindowWroteTest`), byte for byte.

| file | sha256 |
|---|---|
| `written-by-the-window-8.2.txt.mcenc` | `b0d6bfbb4112cd7d49ba046491be12d2c207010b05e38092e95d356c195559f8` |
| `written-by-the-window-8.2.text` | `72fb42636ee464bc6e6f3ff8643705e8d294f6d8cedfd858422e909fad0049fc` |
