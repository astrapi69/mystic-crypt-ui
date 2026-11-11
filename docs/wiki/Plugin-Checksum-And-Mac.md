# Checksum and MAC

This tool answers two questions that look alike and are not. A **checksum** tells you whether a
file or a piece of text is byte for byte what it was supposed to be, and anybody can compute one,
including whoever tampered with the file. A **message authentication code** takes a key, so only
someone who holds that key can produce a code that checks out. Reach for this tool when you have
downloaded a file and want to compare it against the value published next to it, or when you and
somebody else share a secret key and want to prove that a message came from the holder of that
key. If what you have is a checksum *file* the owner shipped alongside the download, the
neighbouring **Verify Checksum** tool is the better fit, because it reads that file for you.

## Where to find it

Menu **Plugins**, submenu **Checksum**, item **Checksum and MAC**.

The submenu holds two items: **Verify Checksum** and **Checksum and MAC**. This page is about the
second one. The **Plugins** menu only exists once at least one plugin is installed; see
[[install-a-plugin]].

Choosing the item opens a window titled *Checksum and MAC* inside the main window. If the
application is currently showing your database, it switches to its desktop view first, which is
where tool windows live.

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

## Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Checksum** in the list on
the left. Change the value column, then press **Apply**; **Reset to defaults** puts both keys
back. A changed value takes effect the next time you open a tool window.

| Key | Default | What it changes |
|---|---|---|
| `default.digest` | `SHA-256` | The digest the **Checksum** tab of this tool starts with. Only one of the ten offered names is accepted; anything else is ignored and `SHA-256` applies. |
| `default.algorithm` | `MD5` | The algorithm the neighbouring **Verify Checksum** tool starts with, not this one. It expects a constant name with an underscore, such as `SHA_256`; an unusable value falls back to `MD5`. |

There is no setting for the message authentication code: that tab always starts on `HmacSHA256`.

## On the command line

This plugin contributes no command of its own, and neither does any other plugin that ships with
the application: everything on the command line comes from the mystic-crypt library. The command
that covers this tool is `checksum`, and the installer places it next to the application as
`mystic-crypt` (`mystic-crypt.bat` on Windows) if you selected the *Command-line interface* pack.
It answers both of this page's questions: a plain digest by default, and with `--hmac` a keyed MAC.

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
such as `MD5`, `SHA-1`, `SHA-256` or `SHA-512`. A name that does not resolve is refused with
`error: unknown digest algorithm '...'. Use a JDK digest name such as MD5, SHA-1, SHA-256 or
SHA-512.` and the command exits with 2.

### Was it changed by somebody without the key: the keyed MAC

Add `--hmac` and a key, and the same command answers the other question instead:

```bash
printf 'shared-secret\n' | ./mystic-crypt checksum --hmac --key-stdin /tmp/abc.txt
```

```
2bef89862d4be49a5a25877babeb9973dbcea0ef6e0a2411e05b2c9c2b00fdca  HmacSHA256 keyed digest of /tmp/abc.txt
```

With `--hmac`, `-a` names a MAC rather than a digest and defaults to `HmacSHA256`; `HmacSHA512` and
`HmacSHA3-256` work as well. A digest name in that position is refused rather than quietly hashed:

```
error: unknown MAC algorithm 'SHA-256'. Use a JDK MAC name such as HmacSHA256, HmacSHA512 or HmacSHA3-256. A plain digest name like SHA-256 is not one: drop --hmac to compute that instead.
```

The key can also be handed over as `--key shared-secret`, but it then stands in the process
arguments where anyone on the machine can read it, which is why the example pipes it in with
`--key-stdin`, the first line of standard input. With neither of the two, nothing is computed and
the command exits with 2, saying `error: a password is required: pass --password or
--password-stdin` - the wording says password, the options to pass are `--key` and `--key-stdin`.

The key is used exactly as the window uses it, its characters as UTF-8, so the same file and the
same key produce the same code on both sides: the two values printed above are the ones the window
shows for that file, on the **Checksum** and the **Message authentication code** tab.

Run `./mystic-crypt checksum --help` for the full usage.

One limit still applies to both halves: the command takes a **file**, never a piece of typed text,
so the **Text** input of either tab has no counterpart on the command line. The desktop application
started with `--cli` as its first argument runs the same commands. A plugin may still add a command
of its own through the plugin command extension point, but none of the installed ones does.

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
- **Nothing here is saved.** The tool reads no checksum file and writes none, and closing the
  window discards the text, the paths, the key and the computed values. Copy what you need out
  first. For a checksum shipped as a file, use **Verify Checksum**.
- **The window does not remember your choices.** Every time you open it the digest goes back to the
  configured one and the code goes back to `HmacSHA256`.

## Under the hood

Both halves use the digest and MAC implementations of the Java runtime; nothing is hand written.
`SHA-3` and `BLAKE2` come from Bouncy Castle, which the application registers at startup, which is
why the window can offer digests that older versions could not. Checksums over a file are streamed
in 8 KB blocks, so file size is not a memory question on the checksum tab; a code over a file reads
the file's bytes in one go, which is where the size limit above comes from. Values are always
produced as lower case hex. The comparison behind both **Compare** buttons normalises case, spaces
and colons and then compares in a way that does not finish early on the first differing character,
so the time it takes does not leak how much of a guessed value was right. The key you type stays in
memory as characters for as long as the window is open, and is never written to disk, into the
settings, or into any message the tool shows.

## See also

- [[Plugin-Verify-Checksum]] - the other tool in the same submenu, for a checksum that came as a file
- [[install-a-plugin]] - how the Plugins menu comes to exist
- [[Getting-Started]] - first launch, database and sign-in
- [[how-to-create-a-plugin]] - for developers
