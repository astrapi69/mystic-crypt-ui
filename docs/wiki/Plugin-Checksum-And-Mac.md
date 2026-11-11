# Checksum and MAC

This tool answers two questions that look alike and are not. A **checksum** tells you whether a
file or a piece of text is byte for byte what it was supposed to be, and anybody can compute one,
including whoever tampered with the file. A **message authentication code** takes a key, so only
someone who holds that key can produce a code that checks out. Reach for this tool when you have
downloaded a file and want to compare it against the value published next to it, or when you and
somebody else share a secret key and want to prove that a message came from the holder of that
key. If what you have is a checksum *file* the owner shipped alongside the download, the
neighbouring **Verify Checksum** tool is the better fit, because it reads that file for you; it
is described at the end of the section *Using it* below.

## Where to find it

Menu **Plugins**, submenu **Checksum**, item **Checksum and MAC**.

The submenu holds two items: **Verify Checksum** and **Checksum and MAC**. This page is mostly
about the second one. The **Plugins** menu only exists once at least one plugin is installed; see
[[install-a-plugin]]. Both items can be used without a database: before you sign in, and while
the workspace is locked.

Choosing the item opens a window titled *Checksum and MAC* inside the main window. If the
application is currently showing your database, it switches to its desktop view first, which is
where tool windows live; the database then stays reachable in a window of its own, titled
*Key database*.

## Using it

The window has two tabs, **Checksum** and **Message authentication code**, and one result line
underneath both of them. That line is the only place the tool reports success or failure, so read
it after every button press.

### Computing or checking a checksum

1. Open the **Checksum** tab.
2. Pick a value in **Digest**. The list offers `SHA-256`, `SHA-512`, `SHA-384`, `SHA-1`, `MD5`,
   `SHA3-256`, `SHA3-512`, `BLAKE2B-256`, `BLAKE2B-512` and `BLAKE2S-256`. The window starts with
   whichever digest the settings name (see below).
3. Decide what you are hashing:
   - For text, type or paste it into **Text** and leave the checkbox alone.
   - For a file, put its path into **File** (the **...** button opens a file chooser) and tick
     **use the file instead of the text**. The checkbox, not the filled-in field, is what decides.
4. Press **Compute**. The value appears in the read-only **Checksum** box as lower case hex, and
   the result line says which digest over which input, for example `SHA-256 over the text` or
   `SHA3-256 over the file`.
5. To check a published value, paste it into **Compare with** and press **Compare** instead.
   Compare always recomputes first and then compares, so you do not have to press **Compute**
   beforehand. The result line then reads `the checksums are the same` or
   `the checksums are NOT the same`.

If it did not work, the result line begins with `not computed:` and gives the reason, for example
`not computed: '/home/me/nope.txt' is not a file that could be read`.

### Saving a checksum file

**Save checksum**, next to **Compute** and **Compare**, writes the computed checksum into a file
next to the file it describes, in the form `sha256sum -c` reads: the checksum, two spaces, the
bare file name. It works for a checksum over a **file** only, because a checksum file has to name
the file it belongs to: while **use the file instead of the text** is not ticked, the button is
greyed out and its tooltip says so.

1. Compute the checksum over the file, as above.
2. Press **Save checksum**. The file is named after the original plus the digest in lower case
   without hyphens: `installer.jar.sha256` for `SHA-256`, `installer.jar.md5` for `MD5`,
   `installer.jar.sha3256` for `SHA3-256`.
3. The result line reads `written: ` and the full path of the checksum file.

If that checksum file exists already, the tool asks *Replace the checksum file?*, naming the file.
Answering **No** writes nothing, and the result line reads
`nothing written - the existing checksum file was kept`. Pressing the button before anything was
computed gives `there is nothing to save yet - compute the checksum first`.

### Computing or checking a message authentication code

1. Open the **Message authentication code** tab.
2. Pick a value in **Code**. The list offers `HmacSHA256`, `HmacSHA512`, `HmacSHA3-256` and
   `HmacSHA3-512`. It always starts on `HmacSHA256`.
3. Type the shared key into **Key**. It is a password field, so the characters are masked.
4. Choose the input the same way as on the other tab: **Text**, or **File** plus
   **use the file instead of the text**.
5. Press **Compute**. The code appears in the read-only **Code** box as lower case hex and the
   result line reads, for example, `HmacSHA256 over the text`.
6. To check a code somebody sent you, paste it into **Compare with** and press **Compare**. The
   result line reads `the codes are the same` or `the codes are NOT the same`.

Without a key there is no code: the result line says
`not computed: a message authentication code needs a key - that is what makes it one`.

### Verify Checksum, the other item in the submenu

**Plugins** > **Checksum** > **Verify Checksum** opens a window of that name for the other common
case: a download and the checksum its owner published for it.

1. **Open File to check** picks the file. Its checksum is computed straight away with the
   algorithm in **Checksum algorithm** and shown under **Generated checksum**.
2. **Checksum from owner** takes the published value: type or paste it, or load it with
   **Open Checksum File**. A loaded file locks the field and selects the algorithm its extension
   names (`.sha256`, `.sha512`, `.sha384`, `.sha1`, `.md5`, `.md2`), or the one the length of
   the value suggests. A checksum file larger than 8192 bytes is refused as
   `Given checksum file is invalid`.
3. Press **Compare**. The field next to it turns green and reads `Match`, or red and reads
   `No Match`.

While **Checksum from owner** is empty, opening a file also looks for its checksum file next to
it, under both names tools use: `backup.alb.sha256` and `backup.sha256` for `backup.alb`, for
each of the extensions above in that order. Changing the algorithm computes again, empties
**Checksum from owner**, and looks for the checksum file of the new algorithm. The comparison
reads the first line of what the owner published and takes the hash out of it, whether it is a
bare hash, the `sha256sum` form `<hash>  <name>` or the BSD form `SHA256 (<name>) = <hash>`.
Each field has a **Copy** button, and the two **Clear** buttons empty the file or the owner's
checksum again. The algorithms offered are `MD2`, `MD5`, `SHA_1`, `SHA_256`, `SHA_384` and
`SHA_512`; the window starts on the one the setting `default.algorithm` names.

## Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Checksum** in the list on
the left. Change the value column, then press **Apply**; **Reset to defaults** puts both keys
back. A changed value takes effect the next time you open a tool window.

| Key | Default | What it changes |
|---|---|---|
| `default.digest` | `SHA-256` | The digest the **Checksum** tab of this tool starts with. Only one of the ten offered names, spelled exactly as in the list, is accepted; anything else is ignored and `SHA-256` applies. |
| `default.algorithm` | `MD5` | The algorithm the neighbouring **Verify Checksum** tool starts with, not this one. It expects one of the constant names `MD2`, `MD5`, `SHA_1`, `SHA_256`, `SHA_384` or `SHA_512`, with the underscore; an unusable value falls back to `MD5`. |

There is no setting for the message authentication code: that tab always starts on `HmacSHA256`.

## On the command line

This plugin contributes no command of its own, and neither does any other plugin that ships with
the application: everything on the command line comes from the mystic-crypt library. The command
that covers this tool is `checksum`, and the installer places it next to the application as
`mystic-crypt` (`mystic-crypt.bat` on Windows) if you selected the *Command-line interface* pack.
It answers both of this page's questions: a plain digest by default, and with `--hmac` a keyed MAC.
See [[The-Command-Line]] for the other ways to start it.

### Has the file changed at all: the plain digest

```bash
printf 'abc' > /tmp/abc.txt
./mystic-crypt checksum -a SHA-256 /tmp/abc.txt
```

It prints one line, the value first so that it stays the first whitespace separated field the way
`sha256sum` writes it, followed by what was computed over which file:

```
ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  SHA-256 digest of /tmp/abc.txt
```

`-a` (or `--algorithm`) defaults to `SHA-256`, so it can be omitted; it takes a JDK digest name
such as `MD5`, `SHA-1`, `SHA-256` or `SHA-512`, and every digest the window offers, `SHA3-256` and
`BLAKE2B-256` included. A name that does not resolve is refused with
`error: unknown digest algorithm '...'. Use a JDK digest name such as MD5, SHA-1, SHA-256 or
SHA-512.` and the command exits with 2.

### Was it changed by somebody without the key: the keyed MAC

Add `--hmac`, and the same command answers the other question instead. Without a key option it
asks for the key in the terminal, without showing what you type:

```console
$ ./mystic-crypt checksum --hmac /tmp/abc.txt
MAC key:
<64 hex digits>  HmacSHA256 keyed digest of /tmp/abc.txt
```

In a script, `--key-stdin` reads the key from the first line of standard input instead, for
example from a file only you can read:

```bash
./mystic-crypt checksum --hmac --key-stdin /tmp/abc.txt < mac-key.txt
```

With `--hmac`, `-a` names a MAC rather than a digest and defaults to `HmacSHA256`; `HmacSHA512`,
`HmacSHA3-256` and `HmacSHA3-512` work as well. A digest name in that position is refused rather
than quietly hashed:

```
error: unknown MAC algorithm 'SHA-256'. Use a JDK MAC name such as HmacSHA256, HmacSHA512 or HmacSHA3-256. A plain digest name like SHA-256 is not one: drop --hmac to compute that instead.
```

Where there is no terminal to ask in and no `--key-stdin`, nothing is computed and the command
exits with 2, saying
`error: a MAC key is required: pass --key-stdin, or run in a terminal to be asked for it`. The
`--key` option, which takes the key as an argument, still works in this release but prints a
warning on standard error: an argument stands in the process list, where anyone on the machine
can read it, and in the shell history. It goes away in mystic-crypt 14.0.

The key is used exactly as the window uses it, its characters as UTF-8, so the same file and the
same key produce the same code on both sides, and the plain digest of a file is the one the
**Checksum** tab shows for it.

Run `./mystic-crypt checksum --help` for the full usage.

One limit still applies to both halves: the command takes a **file**, never a piece of typed text,
so the **Text** input of either tab has no counterpart on the command line, and the command
writes no checksum file. The desktop application started with `--cli` as its first argument runs
the same commands. A plugin may still add a command of its own through the plugin command
extension point, but none of the installed ones does.

## Limits and pitfalls

- **The checkbox decides, not the filled-in field.** Typing a path into **File** and pressing
  **Compute** without ticking **use the file instead of the text** hashes the text, silently. With
  an untouched text area that is the checksum of the empty input, which for SHA-256 is the
  suspiciously memorable `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`. The
  result line tells you which one it was, and it is the only thing that does.
- **Typed text is hashed exactly as typed, with no trailing newline.** Command line tools like
  `sha256sum` hash whatever is in the file, and a file made with `echo abc` ends in a newline. So
  the **Text** tab over `abc` and `sha256sum` over such a file legitimately disagree. Compare
  files with files.
- **Pressing Compare can change your digest selection.** If the value in **Compare with** is 32,
  40 or 96 hex characters long, the tool switches **Digest** to `MD5`, `SHA-1` or `SHA-384`
  respectively, because a value of that length can only have come from that digest. A value of 64
  or 128 characters is deliberately not guessed, since it fits `SHA-256`, `SHA3-256` and
  `BLAKE2B-256` alike. Pick the right digest yourself in that case, or a perfectly good value will
  be reported as not the same.
- **Compare is forgiving about presentation, not about content.** Upper case, surrounding spaces
  and colons between the bytes are all accepted, so a value pasted straight off a download page in
  any of the usual shapes works. A value that is one character short is simply not the same.
- **A failed computation leaves the previous value on screen.** The **Checksum** and **Code** boxes
  are read-only and are only overwritten when a computation succeeds. If it fails and you press
  **Compare** anyway, you will get a verdict about the value from the run before. Read the result
  line before believing the box.
- **Save checksum writes the box as it stands.** The file is named after the digest and the file
  currently selected, while its content is the value computed last. After changing either, press
  **Compute** again before saving, or the file holds a value of another digest or another file.
- **A checksum proves nothing about origin.** If the page that offers the download also publishes
  the checksum, whoever could replace the file could replace the value next to it. That is exactly
  why the second tab exists, and why it insists on a key.
- **The key is used as text.** Its characters, encoded as UTF-8, are the key. A key that was
  written down as hex is used as those hex *characters*, not as the bytes they stand for, so a
  code produced elsewhere from the raw bytes will not match. Both sides have to agree on the key as
  a string, not just on the value it prints as.
- **Very large files: the two tabs behave differently.** The checksum tab reads a file in blocks
  and does not care how big it is. The code tab reads the whole file into memory first, so a file
  of several gigabytes can fail there while the checksum tab handles it without complaint.
- **Little is kept.** This window reads no checksum file and writes one only when you press
  **Save checksum**; closing it discards the text, the paths, the key and the computed values.
  Copy what you need out first. For a checksum shipped as a file, use **Verify Checksum**.
- **The window does not remember your choices.** Every time you open it the digest goes back to the
  configured one and the code goes back to `HmacSHA256`.

## Under the hood

Both halves use the digest and MAC implementations of the Java runtime; nothing is hand written.
`SHA-3` and `BLAKE2` come from Bouncy Castle, which the application registers at startup.
Checksums over a file are streamed in 8 KB blocks, so file size is not a memory question on the
checksum tab; a code over a file reads the file's bytes in one go, which is where the size limit
above comes from. Values are always produced as lower case hex. The comparison behind both
**Compare** buttons normalises case, spaces and colons and then compares in a way that does not
finish early on the first differing character, so the time it takes does not leak how much of a
guessed value was right. The key you type stays in memory as characters for as long as the window
is open, and is never written to disk, into the settings, or into any message the tool shows.

## See also

- [[install-a-plugin]] - how the Plugins menu comes to exist
- [[The-Command-Line]] - the `checksum` command and the other commands
- [[Getting-Started]] - first launch, database and sign-in
- [[how-to-create-a-plugin]] - for developers
