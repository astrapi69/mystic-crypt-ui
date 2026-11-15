# Encrypt and Decrypt

## What it is for

This tool puts a single file, or a piece of text, behind a passphrase you choose on the spot,
and takes it back out again. Reach for it when something has to leave the application: an
attachment you want to mail, a backup on a USB stick, a note pasted into a chat window, where
the only thing the other side will have is a passphrase agreed on separately. For secrets you
want to keep and look up again later, use the database instead; entries there are protected by
the master key you signed in with, while this tool asks for a passphrase every time and stores
it nowhere.

## Where to find it

Menu bar: **Plugins** > **Files** > **Encrypt and Decrypt**.

The tool opens as a window inside the main window, titled `Encrypt and Decrypt`. If the main
window is not showing its desktop view yet, it switches to it first; your database then stays
reachable in a window of its own, titled `Key database`. The **Plugins** menu only appears once
at least one plugin is installed; the installer puts this one in place, see [[install-a-plugin]].

The item can be used while a database is open and unlocked. Before you sign in, and while the
workspace is locked, it is greyed out.

## Using it

At the top of the window a short note says what the tool does, and that nothing can recover the
content once the passphrase is lost. Below it are two tabs, **File** and **Text**, and one
message line underneath them that reports what happened. Which tab is in front when the window
opens is a setting, see below.

### Encrypting a file

1. Open the **File** tab.
2. **File:** type the path of the file to encrypt, or press the **...** button next to the
   field and pick it in the chooser, whose confirm button is labelled **Select**.
3. **Write to:** leave it empty and the tool names the result itself: the original name plus
   `.mcenc`, written next to the original. Fill it in to choose another name or another
   directory.
4. **Passphrase:** and **Repeat (to encrypt):** type the same passphrase into both fields. The
   repetition is the only protection against a typo, because nothing else knows what you meant.
5. Press **Encrypt**.

It worked when the message line reads `encrypted to secret-notes.txt.mcenc` and the
**Write to:** field now shows the full path of the file that was written. With the delete
setting turned on, the message continues `, removed secret-notes.txt`.

It did not work when the message line starts with `not encrypted: ` and names the reason, for
example `not encrypted: the two passphrases are not the same` or
`not encrypted: '/home/you/out.mcenc' already exists - pick another name or remove it first`.
In that case nothing was written and the original is untouched.

### Decrypting a file

1. **File:** the encrypted file.
2. **Write to:** leave it empty to get the name back without the `.mcenc` ending. If the field
   still holds the path from the operation before, clear it first, see the pitfalls below.
3. **Passphrase:** the passphrase, once. The **Repeat (to encrypt):** field is ignored when
   decrypting.
4. Press **Decrypt**.

It worked when the message line reads `decrypted to recovered.txt`. It did not work when it
starts with `not decrypted: `. A wrong passphrase, or a file that was changed afterwards, reads
`not decrypted: could not decrypt: the passphrase is wrong or the data was altered`; a file this
tool did not write reads `not decrypted: this is not something this envelope sealed: the marker
is missing`. No file is written in either case.

### Encrypting a text

1. Open the **Text** tab.
2. **Text:** type or paste what should be encrypted.
3. **Passphrase:** and **Repeat (to encrypt):** the same passphrase twice.
4. Press **Encrypt**. The **Encrypted (Base64):** field fills with a block of letters, digits,
   `+`, `/` and `=`, and the message line reads `encrypted 16 characters`, with the length of
   your text. Copy that block wherever it has to go; it is plain text and survives mail, chat
   and text files.

To read it back, paste the block into **Encrypted (Base64):**, type the passphrase into
**Passphrase:** (again the repetition is not used), and press **Decrypt**. The **Text:** field
is replaced by the decrypted text and the message line reads `decrypted 16 characters`.

## Settings

Edited under **File** > **Settings...**, tab **Plugin settings**, entry **File Encryption** in
the list on the left. Change the value in the right-hand column and press **Apply**;
**Reset to defaults** throws the stored values away and shows the defaults again.

| Key | Default | What it changes |
|---|---|---|
| `delete.source.after.encrypt` | `false` | Removes the original file once it has been encrypted successfully. Only the value `true`, in any letter case, turns this on; anything else, `yes` included, counts as off. Deleting is not wiping: the content can still be recoverable from the disk. Applies to the **File** tab only. |
| `default.tab` | `file` | Which tab is in front when the tool window opens: `file` or `text`. Upper and lower case are both understood, any other value falls back to `file`. Takes effect the next time the window is opened. |

## On the command line

The same job is done by two commands of the mystic-crypt library, `encrypt` and `decrypt`. They
are not contributed by this plugin, they belong to the application itself, so they are there
whether the plugin is installed or not. Start them through the application jar with `--cli` as
the first argument (`java -jar mystic-crypt-ui.jar --cli ...`, from the installation
directory), or through the `mystic-crypt` launcher the installer's *Command-line interface* pack
puts next to it; see [[The-Command-Line]]. Both commands take the same options:

```
  -i, --in=<in>            The file to encrypt, or '-' for standard input.
  -o, --out=<out>          The file to write. Without it the result is printed
                             as base64.
  -p, --passphrase=<passphrase>
                           The passphrase. Deprecated, it puts the secret into
                             the process arguments and goes away in 14.0: use
                             --passphrase-stdin, or leave both out to be asked
                             in a terminal.
      --passphrase-stdin   Read the passphrase from the first line of standard
                             input.
  -t, --text=<text>        The text to encrypt instead of a file.
      --text-stdin         Read the text to encrypt from standard input.
```

`decrypt` has the same list with the wording turned around: `-i` is the file to decrypt, `-t` is
the Base64 text to decrypt, and without `-o` the result is printed as text.

Encrypting a file, then decrypting it to a new name. Leave the passphrase options out and the
command asks for it in the terminal, without showing what you type. Unlike the window, the
command line invents no name of its own: it is `-o` that decides where the result goes, and
without `-o` the result is printed instead of written. The line that reports what was written
goes to standard error.

```console
$ java -jar mystic-crypt-ui.jar --cli encrypt -i notes.txt -o notes.txt.enc
passphrase:
encrypted 12 bytes to notes.txt.enc

$ java -jar mystic-crypt-ui.jar --cli decrypt -i notes.txt.enc -o recovered.txt
passphrase:
decrypted 12 bytes to recovered.txt
```

In a script, where there is no terminal to ask in, `--passphrase-stdin` reads the passphrase from
the first line of standard input, here from a file only you can read:

```console
$ java -jar mystic-crypt-ui.jar --cli encrypt -i notes.txt -o notes.txt.enc --passphrase-stdin < passphrase.txt
encrypted 12 bytes to notes.txt.enc
```

Encrypting a text with `-t`; the result is one line of Base64 on standard output, so it can be
redirected into a file or piped onward, and `decrypt -t` takes it back:

```console
$ java -jar mystic-crypt-ui.jar --cli encrypt -t "a short note"
passphrase:
TUNGSUxFVoJ/pOjfIk6qt3SGl4dDpAAJJ8C1EJ9n/Jopl4LLCh3XdHNI/FjzDSGqGfvfw759xuQoWRNfk6Ddv6CzjvoLT4X8/+8isk5uqLziC7m/NAdIp+SVeUqEQVxkgyY=

$ java -jar mystic-crypt-ui.jar --cli decrypt -t "TUNGSUxFVoJ/pOjfIk6qt3SGl4dDpAAJJ8C1EJ9n/Jopl4LLCh3XdHNI/FjzDSGqGfvfw759xuQoWRNfk6Ddv6CzjvoLT4X8/+8isk5uqLziC7m/NAdIp+SVeUqEQVxkgyY="
passphrase:
a short note
```

A text given as `-t` stands in the shell history and in the process list just like a passphrase
would; for a text that is itself secret, write it into a file and encrypt that with `-i`.
`--text-stdin` and `--passphrase-stdin` cannot be combined, because both want standard input.

The `-p` option still works in this release, but it prints a warning on standard error, because
a passphrase given as an argument stands in the shell history and in the process list of every
user on the machine; it goes away in mystic-crypt 14.0.

`encrypt` ends with exit code 0 when it encrypted and 2 on an error. `decrypt` ends with 0 when
it decrypted, 1 when the passphrase was wrong or the data was altered, and 2 on an error before
that. The reason is one line on standard error beginning with `error: `, for example
`error: could not decrypt: the passphrase is wrong or the data was altered` or
`error: a passphrase is required: pass --passphrase-stdin, or run in a terminal to be asked for it`.
Scripts should check the exit code, not the output.

Three differences from the window are worth knowing:

- **The command line asks for the passphrase once.** There is no second field to catch a typo,
  so an encryption with a mistyped passphrase succeeds and cannot be opened again.
- **It writes over an existing output file without asking.** The window refuses instead.
- **It writes a different format, and each side reads the other's.** Files from this window
  start with the marker `MCFILE1` and are named `.mcenc`; `encrypt` writes files that start with
  `MCFILE`, and wrote the `MCRYPT` layout before that. The window opens all three, as files and as
  texts, and `decrypt` opens a file or a text from the window. Only reading is shared: each side
  still writes its own format (#877).

## Limits and pitfalls

- **Nothing is ever overwritten.** If the file to be written exists, the operation is refused
  with `already exists - pick another name or remove it first`, in both directions. This is
  deliberate: overwriting the wrong file with ciphertext is not recoverable.
- **The panel fills in "Write to:" for you, and then leaves it there.** After encrypting, the
  field holds the path of the file just written. Clicking **Decrypt** next without clearing it
  makes the tool try to write onto that same file, which fails with `already exists`. Clear the
  field, or type the name you actually want.
- **A passphrase mistyped identically twice cannot be detected.** The repetition catches a slip
  of the finger, nothing more. Nothing stores the passphrase, and there is no recovery: an
  encrypted file whose passphrase is gone is gone with it.
- **A wrong passphrase produces nothing, not garbage.** Decrypting with the wrong passphrase, or
  a file changed by so much as one bit afterwards, is refused and no output file is written.
  That also means this tool cannot repair a damaged encrypted file, not even partially.
- **Only this tool's files and those of the `encrypt` command can be read back.** Every result
  carries a marker; anything else, files encrypted by other programs included, is refused with
  `the marker is missing`. Files written here are equally useless to other tools.
- **Decrypting next to the original does not work by default.** With **Write to:** empty,
  `report.pdf.mcenc` wants to be written as `report.pdf`; if the original is still lying there,
  you get `already exists`. Give another target name, or move the original away.
- **A custom name does not survive the round trip.** Encrypting `plain.bin` into
  `elsewhere.dat` gives, on decrypting with **Write to:** empty, a file named
  `elsewhere.dat.decrypted`, because only a name ending in `.mcenc` can have that ending
  removed.
- **One file at a time, and no directories.** A directory is refused as
  `is not a file that could be read`. There is no recursion and no built-in archiving: pack a
  folder into a zip or tar first, then encrypt that.
- **The file is processed whole in memory.** A file far larger than the memory available to the
  application will fail rather than stream through.
- **The Text tab overwrites its own fields.** **Encrypt** replaces the **Encrypted (Base64):**
  field, **Decrypt** replaces the **Text:** field. Copy a value out before pressing the other
  button.
- **Pasted Base64 must be one unbroken block.** Leading and trailing spaces are tolerated, but a
  block that was hard-wrapped into several lines on its way through a mail client is refused
  with `it is not even Base64`. Remove the line breaks first.
- **An empty passphrase is refused** with `a passphrase is required`, but a passphrase of only
  spaces is accepted without a warning. Nothing here measures passphrase strength.
- **Deleting the source is not wiping it.** With `delete.source.after.encrypt` turned on the
  original is removed the ordinary way, and its content can still be recoverable from the disk.
- **This is not a signature.** An encrypted file proves that nobody changed it since it was
  encrypted by somebody who knew the passphrase, and nothing about who that somebody was. For
  proving authorship, use **Plugins** > **Signatures** > **Sign and Verify** instead.
- **The passphrase has nothing to do with the master password.** The tool never uses the key of
  the database you are signed in with, so a file encrypted here does not open with the database
  password unless you typed that password in here yourself.

## Under the hood

The construction is the same one the application uses for a database protected by a master
password: AES-GCM in a key-committing form, over a 256-bit key derived from the passphrase with
PBKDF2-HMAC-SHA256 and 600,000 rounds over a 16-byte salt drawn fresh for every single
operation. The marker, the salt and the round count sit in front of the encrypted payload and
are covered by the cipher's authentication tag, so none of them can be edited without the file
refusing to open, and the round count travels with each file so it can be raised later without
making today's files unreadable. The construction is the mystic-crypt library's passphrase
envelope, and all primitives come from that library and Bouncy Castle; nothing is hand-rolled
here.

Three practical consequences: a wrong passphrase fails immediately and as a wrong passphrase
instead of producing plausible rubbish; the same file encrypted twice yields two entirely
different results, so nobody can tell from the outside that they hold the same content; and the
key derivation deliberately costs about a tenth of a second per operation, which is a wall
against guessing and is also why a click is not instant.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[The-Command-Line]]
- [[Secure-your-passwords-with-obfuscation]]
- [[how-to-create-a-plugin]]
