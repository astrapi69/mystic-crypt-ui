# Split and Combine

This tool answers the question every password manager eventually raises: what happens to the
master password, or to a key file, if the one person who knows it cannot use it any more. It
splits a secret into a number of shares, of which a smaller number is enough to put it back
together (three of five, say), while anything fewer reveals nothing at all. Reach for it when you
want a secret to survive you without handing it whole to anybody; reach for the obfuscation or
file encryption tools instead when you want to protect a secret you will keep using yourself.

## Where to find it

**Plugins** menu, submenu **Secret Sharing**, item **Split and Combine**.

The item only appears once the secret sharing plugin is installed and enabled. If the **Plugins**
menu is missing entirely, no plugin is installed yet; see [[install-a-plugin]]. The item is enabled
while a database is open; before you sign in, and while the workspace is locked, it is greyed out.
Locking the workspace also closes an open **Split and Combine** window, shares and rebuilt secret
with it.

## Using it

The tool opens as a window inside the main frame. The upper half splits a secret, the lower half
puts one back together, and a bold line at the very bottom reports the result of whatever you
last did.

### Splitting a secret

1. Type the secret into **Secret:**. The field is masked, like a password field.
   To split a file instead (a key file, for example), leave **Secret:** empty, put the path into
   **or file:** (the **...** button next to it opens a file chooser), and tick
   **split the file instead of the secret above**. The checkbox is what decides which of the two
   is used, not which one you filled in.
2. Set **Shares needed:** to how many shares must come together to rebuild the secret. The
   smallest accepted value is 2.
3. Set **Shares produced:** to how many shares you want in total. It has to be at least as large
   as **Shares needed:**.
4. Press **Split**. The **Shares:** box fills with one share per line, each a single line of text
   that looks like this:

   ```
   MCSS1$3$5$2$Base64OfTheShare$a1b2c3d4
   ```

   The bottom line reports, for example, `5 shares, 3 of them are enough - keep them apart`.
   If it did not work, the line starts with `not split:` and says why, for instance
   `not split: there is no secret to split` or
   `not split: at least two shares have to be needed - with one, a share is the secret`.
5. Distribute the shares. **Save shares** writes the whole box into one text file, which is handy
   for moving them off the machine but is not where they should stay; the confirmation says so
   itself: `wrote the shares to <name> - they belong apart, not in one file`. The file name is
   taken from the **or file:** field with `.shares.txt` appended, and an existing file is never
   overwritten (`'<path>' already exists - pick another name or remove it first`).

### Putting a secret back together

1. Get the shares into the **Shares:** box, one per line. Paste them, or use **Load shares** to
   read them from a file whose path you first put into **or file:**. Loading reports
   `read the shares from <name>`; if the path is not a file you get
   `not loaded: choose the file the shares are in`.
2. Press **Combine**. On success the secret appears in **Rebuilt secret:** and the bottom line
   says how much came back, for example `rebuilt 36 bytes`. On failure the bottom line starts
   with `not combined:` and **Rebuilt secret:** stays empty, so an empty field is never a secret
   that happened to be empty.
3. To keep the result, put a path into **Write it to:** (again with a **...** file chooser) and
   press **Save rebuilt secret**. It reports `wrote the secret to <name>`. It refuses an empty
   path (`choose a file to write the secret to`), an empty result
   (`there is nothing to save - combine the shares first`), and an existing file.

The order of the shares does not matter, extra spaces around a pasted share do not matter, and
handing over more shares than are needed is fine.

## Settings

Edited under **File > Settings...**, tab **Plugin settings**, entry **Secret Sharing** in the
list on the left. Change the value cell in the table and press **Apply**; **Reset to defaults**
throws the stored values away. The new values are picked up the next time you open the
**Split and Combine** window, not in a window that is already open.

| Key | Default | What it changes |
|---|---|---|
| `default.threshold` | `3` | How many shares are needed to rebuild the secret, and therefore what **Shares needed:** starts at. Values below 2 are ignored and 2 is used. |
| `default.total.shares` | `5` | How many shares are produced, and therefore what **Shares produced:** starts at. A value below the threshold is ignored and the threshold is used. |

Both are only starting values for the window. Whatever you set in the spinners for a single split
wins over them, and neither is used by the command line, which requires the two numbers to be
given explicitly.

## On the command line

Splitting and combining are commands of the mystic-crypt library, not of this plugin: `share`,
with the subcommands `split` and `combine`. They are there whether or not the secret sharing
plugin is installed. Reach them through the application jar with the `--cli` switch, from the
directory the application was installed into (see [[The-Command-Line]]):

```
java -jar mystic-crypt-ui.jar --cli share
```

Called without a subcommand it prints its usage:

```
Usage: mystic-crypt share [-hV] [COMMAND]
Split a secret into shares and combine them back (Shamir secret sharing).
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  split    Split a secret into shares, one per line. Exit code 0 = split, 2 =
             error.
  combine  Combine shares back into the secret. Exit code 0 = combined, 1 = the
             shares do not reconstruct a secret, 2 = error.
```

`--help` works on the command and on both subcommands. Because `share` comes from the library, the
separate `mystic-crypt` launcher script reaches it as well, there without the `--cli` switch and
without loading any plugin.

**The shares of the command line and the shares of the window are not the same shares.** The
window writes `MCSS1$...` lines, the library writes `mcs1:...` lines, and neither side reads the
other's. Handing a window share to `share combine` ends in

```
error: not a mystic-crypt share: a share line looks like 'mcs1:<splitId>:<threshold>:<total>:<index>:<value>:<checksum>' but was 'MCSS1$3$5$2$Base64OfTheShare$a1b2c3d4'
```

with exit code 2. Split and combine on the same side.

### share split

```
Usage: mystic-crypt share split [-hV] [--secret-stdin] [--file=<file>]
                                -n=<shares> [-o=<out>] [--secret=<secret>]
                                -t=<threshold>
Split a secret into shares, one per line. Exit code 0 = split, 2 = error.
      --file=<file>       Split the contents of this file instead of a text
                            secret.
  -h, --help              Show this help message and exit.
  -n, --shares=<shares>   How many shares to produce.
  -o, --out=<out>         Write the shares to this file instead of printing
                            them.
      --secret=<secret>   The secret to split. Deprecated, it puts the secret
                            into the process arguments and goes away in 14.0:
                            use --secret-stdin, or leave both out to be asked
                            in a terminal.
      --secret-stdin      Read the secret from the first line of standard input.
  -t, --threshold=<threshold>
                          How many shares are needed to reconstruct the secret.
  -V, --version           Print version information and exit.
```

`-t` and `-n` are both required, which is the reason neither of the plugin settings above reaches
the command line.

Leave the secret out, and the command asks for it in the terminal without showing what you type,
then prints one share per line:

```
$ java -jar mystic-crypt-ui.jar --cli share split -t 3 -n 5
secret:
mcs1:45cae493ecc3f0f2:3:5:1:Z0GiMVnUOgJgDnWVN3cZNww:d0806efc
mcs1:45cae493ecc3f0f2:3:5:2:vZgxd_DEAeUYvRkEnpvQoPY:0ce48cd3
mcs1:45cae493ecc3f0f2:3:5:3:u7ezI9FxVpcU1kzizI-78o4:66870e4d
mcs1:45cae493ecc3f0f2:3:5:4:rw86n3ZNkJ4v9igr7-Xma7g:ef6e613c
mcs1:45cae493ecc3f0f2:3:5:5:qSC4y1f4x-wjnX3NvfGNOcA:b94c15f8
```

The prompt only comes up in a terminal. In a script, `--secret-stdin` takes the secret from the
first line of standard input; [[The-Command-Line]] shows how to fill that line without the secret
appearing in any command. `--secret` with the secret as its value still works in this release,
prints a warning, because every process on the machine can read an argument, and goes away with
mystic-crypt 14.0.

Split a file rather than a typed secret with `--file`, and write the shares straight to a file
with `-o`:

```
java -jar mystic-crypt-ui.jar --cli share split --file ~/keys/master.key -t 3 -n 5 -o ~/shares.txt
```

```
wrote 5 shares to /home/you/shares.txt; any 3 of them reconstruct the secret
```

Without a terminal and without any of `--secret-stdin`, `--file` and `--secret`, the command says
`error: a secret is required: pass --secret-stdin, or run in a terminal to be asked for it` and
exits with 2. They are not refused as a combination: given more than one, `--file` wins over
`--secret-stdin`, which wins over `--secret`. An existing `-o` target is **overwritten without
asking**, unlike **Save shares** in the window, so point `-o` at a name that is still free. The
command line also refuses more shares than the secret has bytes, in its own words:
`error: totalShares (10) must not exceed the secret length in bytes (4)`.

### share combine

```
Usage: mystic-crypt share combine [-hV] [--file=<file>] [-o=<out>]
                                  [--share=<shareLines>]...
Combine shares back into the secret. Exit code 0 = combined, 1 = the shares do
not reconstruct a secret, 2 = error.
      --file=<file>          Read the share lines from this file, one per line.
  -h, --help                 Show this help message and exit.
  -o, --out=<out>            Write the secret to this file instead of printing
                               it.
      --share=<shareLines>   A share line, repeatable.
  -V, --version              Print version information and exit.
```

Give `--share` once per share, each one quoted so the shell hands it over untouched:

```
java -jar mystic-crypt-ui.jar --cli share combine \
  --share 'mcs1:45cae493ecc3f0f2:3:5:1:Z0GiMVnUOgJgDnWVN3cZNww:d0806efc' \
  --share 'mcs1:45cae493ecc3f0f2:3:5:3:u7ezI9FxVpcU1kzizI-78o4:66870e4d' \
  --share 'mcs1:45cae493ecc3f0f2:3:5:5:qSC4y1f4x-wjnX3NvfGNOcA:b94c15f8'
```

```
an example secret
```

Or read them from a file, one share per line, and write the result to a file:

```
java -jar mystic-crypt-ui.jar --cli share combine --file ~/shares.txt -o ~/master.key
```

```
wrote the reconstructed secret to /home/you/master.key
```

`--share` and `--file` can be used together, and at least one of them is required
(`error: no shares were given: pass --share <line> (repeatable) or --file <file>`). Blank lines in
the file are skipped. Without `-o` the secret goes to standard output as text; with `-o` the exact
bytes are written, which is what you want for a key file. An existing `-o` target is overwritten
here as well.

### What the exit codes mean

`combine` separates a failure to read the shares from a failure to rebuild a secret from them, and
a script can act on the difference:

| Exit code | `share split` | `share combine` |
|---|---|---|
| 0 | split | combined, the secret is on standard output or in `-o` |
| 1 | not used | the shares do not reconstruct a secret |
| 2 | error | an error before there was anything to reconstruct |

Exit code 1 is what fewer shares than needed gives
(`error: this split needs 3 shares, but 2 were given`), and so do shares from two different splits
(`error: these shares belong to different splits: <id> and <id>. Combining them would rebuild
nonsense rather than either secret`) and the same share handed over twice
(`error: share 1 was given more than once; the same share twice does not count twice`). Exit code
2 is for a line that is no share at all, or one whose check value does not fit
(`error: share 1 of split <id> does not match its own checksum, so it was altered or mistyped`).
Every refusal writes its message to standard error and prints no secret, so a non-zero exit is
always safe to read as "no secret came back".

## Limits and pitfalls

- **Fewer shares than needed is a hard stop, not a bad guess.** Hand over two shares of a
  three-of-five split and you get `this needs 3 shares and there are 2`, not a wrong secret.
  This is worth knowing because the underlying splitter, left to itself, would happily return
  garbage; the share lines carry the threshold so that cannot happen here.
- **The number of shares cannot exceed the length of the secret in bytes.** A five-character
  password cannot become ten shares: `a secret of 5 bytes cannot be split into 10 shares - at most 5, so use a longer secret or
  fewer shares`. This surprises people the first time, because
  the **Shares produced:** spinner goes up to 255 regardless. 255 is the absolute ceiling.
- **A threshold of 1 is refused on purpose.** With one, every share is the secret, so the tool
  says `at least two shares have to be needed - with one, a share is the secret`.
- **A mistyped share is caught, not rebuilt.** Every share carries a check value, so a character
  gone wrong while copying off paper gives you
  `share 1 is damaged - its check value does not fit, so something was mistyped or lost while copying it` instead of a silently wrong password. Editing the threshold inside a share to talk
  it down does not work either, the check covers that too.
- **Shares of two different splits do not mix, as long as the numbers differ.** Feeding a
  two-of-three share to a three-of-five set gives
  `these shares are not from the same secret - one says ... , another ...`. But two separate
  splits that used the *same* threshold and total look alike to the tool, and mixing those
  produces a wrong secret without complaint. Keep each set together and label it with what it is.
- **The same share twice is one share.** Pasting a line twice to reach the count gives
  `the same share was given twice`.
- **Binary secrets do not survive the window intact.** The **Rebuilt secret:** box is text, so a
  key file combined there is shown, and saved, as text; bytes that are not valid UTF-8 come back
  changed. Split and combine key files on the command line with `--file` and `-o`, which move
  the raw bytes and are tested to return a key file byte for byte. Splitting a file in the window
  is fine, it is only the rebuilt side that is textual.
- **Nothing confirms the secret you typed.** The **Secret:** field is masked and there is no
  second field to repeat it. A typo is split faithfully, and you will only find out when the
  rebuilt secret does not open anything. Rebuild it once from the shares and compare before you
  hand any share out.
- **Saving and loading shares are not symmetric.** **Save shares** appends `.shares.txt` to the
  path in **or file:**, so `/home/you/secret` becomes `/home/you/secret.shares.txt`, and an empty
  field means a file called `shares.shares.txt` in whatever directory the application was started
  from. **Load shares** reads the path exactly as written, so point it at the full name including
  the suffix.
- **Shares are not protected by anything.** They are not encrypted and need no password. Whoever
  collects enough of them has the secret. That is the whole design: keep them apart, in different
  hands or different places, and count how many you have given out.
- **There is no share revocation.** A share cannot be withdrawn once it has left the house. If
  you want a share to stop counting, split the secret again and replace every share.

## Under the hood

Splitting uses Shamir's secret sharing through the BouncyCastle splitter, reached through the
crypt-data library rather than being implemented here; nothing about the mathematics is
hand-rolled. A share line is `MCSS1`, the threshold, the number of shares, the share's own index,
the share value in Base64, and a check value, joined by `$`. The check value is the first eight
hexadecimal characters of a SHA-256 over everything before it. It is there to catch a share that
was mistyped or truncated while being copied, and it does that reliably; it is not a signature,
since anyone editing a share can recompute it, so it protects against accidents rather than
against a person. The typed secret is held as a character array and the byte copy handed to the
splitter is wiped as soon as the split is done, but the rebuilt secret and the shares themselves
sit in the window in plain view until you close it. Splitting draws its randomness from the
system's secure random source, so splitting the same secret twice gives different shares, and
shares from the two runs are not interchangeable.

## See also

- [[Getting-Started]] - the master password this tool most often gets pointed at
- [[install-a-plugin]] - getting the **Plugins** menu to appear in the first place
- [[create-new-mystic-crypt-database]]
- [[Secure-your-passwords-with-obfuscation]]
