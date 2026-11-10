# The Command Line

Much of what mystic-crypt-ui does with keys, hashes, checksums and signatures is also reachable
from a terminal, without opening a window at all. The command line is the same application: one
jar, one `--cli` switch, and the commands of the mystic-crypt library the desktop app is built on.

This page shows how to start it, where the commands come from, which commands exist, and what
three of them look like in practice.

## 1. Starting it

### The runnable jar with `--cli`

`make build` packages the runnable jar with all its dependencies into
`build/libs/mystic-crypt-ui-<version>-all.jar` (the version comes from `projectVersion` in
`gradle.properties`). After an installation, the same jar sits in the installation directory
under the name `mystic-crypt-ui.jar`.

Put `--cli` first, then the command and its options:

```
java -jar mystic-crypt-ui.jar --cli checksum --algorithm SHA-256 notes.txt
```

`--cli` has to be the **first** argument, otherwise the application starts its window as usual.
With it, the application never touches Swing, so this also works on a machine with no display,
over SSH, or in a script.

To see the whole list of commands that your installation actually offers:

```
java -jar mystic-crypt-ui.jar --cli --help
```

Every command has its own help, and so does every subcommand:

```
java -jar mystic-crypt-ui.jar --cli encrypt --help
java -jar mystic-crypt-ui.jar --cli share split --help
```

### The `mystic-crypt` launcher scripts

The installer offers an optional pack, "Command-line interface (the 'mystic-crypt' console
tool)". It installs two small launcher scripts next to the jar: `mystic-crypt` for Unix-like
systems and `mystic-crypt.bat` for Windows. They pass everything through to the command line, so
there is no `--cli` and no `java -jar` to type:

```
./mystic-crypt hash --algorithm argon2id --password-stdin
```

The launcher scripts run the mystic-crypt library's entry point straight out of the jar, so they
offer the library's commands only. That used to leave commands out and no longer does: every
command now comes from the library, so their help output is the same as the jar's. Only a command
contributed by a plugin you installed yourself would be missing from it, and for that one, go
through the jar with `--cli`.

### Output, redirection and logging

The result of a command goes to standard output and nothing else does, so redirecting it gives a
usable file:

```
java -jar mystic-crypt-ui.jar --cli keystore list --file my.p12 > entries.txt
```

To keep the plugin system's logging exactly as it is instead of turning it down to warnings,
start with `-Dmystic.crypt.ui.cli.verbose=true`.

### Which plugins the command line loads

By default it reads the same plugins directory the desktop app uses,
`~/.config/mystic-crypt-ui/plugins` (see [Install a plugin](install-a-plugin)). Point it
somewhere else with `-Dmystic.crypt.ui.plugins.dir=/path/to/plugins`, which is handy for trying
out a freshly built plugin without installing it.

## 2. Where the commands come from

**Every command on the command line comes from the mystic-crypt library.** Password hashing, key
generation, key encapsulation, checksums, PEM/DER conversion, obfuscation, certificates, key store
management, signatures, file and text encryption, secret sharing and key exchange are all the
library's own commands. The application reuses the library's root command as it is, without
reimplementing anything, so every command the library gains is available here as soon as the
library is updated. New command line capability is therefore built in the library first, never
forked into the user interface.

A plugin may still add a command of its own for what the library does not cover, through the
`PluginCommandContribution` extension point: it contributes picocli commands, which are then added
to the library's root command. **None of the thirteen plugins that ship with the application uses
that extension point.** Three of them did until mystic-crypt 12.0.0: file-crypt contributed
`filecrypt`, secret-sharing contributed `share`, kem-demo contributed `keyx`. When the library grew
`encrypt`, `decrypt`, `share` and `keyx` of its own, those three plugin commands were deleted
rather than kept alongside. The plugins themselves are still there, with their tool windows and
their settings; only their command line side is gone.

Two things can go wrong while the contributed commands are collected, and neither takes the
command line down:

- A plugin whose commands cannot be built is skipped with a note on standard error, and the rest
  of the command line still works.
- A contributed command whose name the library already uses is left out, again with a note on
  standard error, and the library keeps the name. The note reads
  `the plugin command 'hash' was left out: the name is already in use by mystic-crypt`.
  Without that check a clash would either make the whole command line fail to assemble, taking
  every other command with it, or let a plugin quietly replace `hash` with something else.

## 3. The available commands

The list below is the library's root command as it stands. Run `--cli --help` to see what *your*
installation offers, which is the authoritative list.

| Command | What it does |
|---|---|
| `hash` | Hash a password with Argon2id or PBKDF2 and print the encoded hash. |
| `verify` | Verify a password against an encoded hash. The algorithm is read from the hash itself. Exit code 0 = matches, 1 = does not match, 2 = the hash is not of a known algorithm. |
| `keygen` | Generate a key pair and print (or write) its keys as PEM. Exit code 0 = generated, 2 = error. |
| `kem` | Run a two-party ML-KEM or hybrid X25519+ML-KEM key encapsulation and show that both sides derive the same shared secret. |
| `checksum` | Compute the checksum of a file: a plain digest, which answers whether the file changed, or with `--hmac` a keyed MAC, which answers whether it changed at the hands of someone without the key. |
| `der2pem` | Deprecated: use `convert` instead, which detects what the file is and converts in both directions. Convert a DER-encoded private key to PEM. |
| `obfuscate` | Obfuscate text with a character-substitution map (`--rule a=x`), optionally with an operation at named positions (`--rule a=x:UPPERCASE@0,3`). Exit code 0 = obfuscated, 2 = error. |
| `disentangle` | Recover text obfuscated by `obfuscate`, using the same `--rule a=x` map. |
| `cert` | Create a self-signed X.509 certificate and write it as PEM, with basic constraints, key usage and subject alternative names. Exit code 0 = written, 2 = error. |
| `keystore` | Inspect and manage a Java key store (PKCS12, JKS or JCEKS). |
| `sign` | Sign a file (or standard input) with an RSA, ECDSA, DSA, Ed25519, ML-DSA or SLH-DSA private key, from a PEM or DER file. |
| `verify-signature` | Verify an Ed25519, ML-DSA or SLH-DSA signature; exit code 0 means valid, 1 invalid, 2 an error before verification. |
| `encrypt` | Encrypt a file or a text with a passphrase (AES-GCM, PBKDF2-HMAC-SHA256). Exit code 0 = encrypted, 2 = error. |
| `decrypt` | Decrypt a file or a text encrypted by `encrypt`. Exit code 0 = decrypted, 1 = wrong passphrase or altered data, 2 = error. |
| `share` | Split a secret into shares and combine them back (Shamir secret sharing). |
| `keyx` | Exchange a shared secret with another person: `new` on the recipient's side, `send` on the sender's, `receive` back on the recipient's. |
| `convert` | Say what a key or certificate file is, and convert it between PEM and DER or between PKCS#1 and PKCS#8. Exit code 0 = done, 2 = error. |
| `help` | Display help information about the specified command. |

`keystore`, `share` and `keyx` group their work into subcommands; `--cli keystore --help` and its
siblings list them.

## 4. Plugin commands come and go with the plugin

Nothing in the table above depends on a plugin being installed, because none of the shipped
plugins contributes a command. The rules below are what to expect from a plugin you write or
install yourself, whose command is only there while the plugin is installed **and** enabled:

- A plugin that is not in the plugins directory contributes nothing. Its command is simply
  absent from `--cli --help`, and using it ends in
  `Unmatched arguments from index 0: 'mine'`, followed by picocli's guess at what you meant.
- A plugin that is disabled under **Settings > Plugins** is not started, so its commands are
  gone too. That state is kept in a `disabled.txt` in the plugins directory and survives a
  restart, which means disabling a plugin in the window also takes its commands off the command
  line.
- Enabling it again, or copying its `.zip` into the plugins directory, brings the commands back
  on the next run. Nothing else has to be configured.

The internal plugins can be built and installed in one go with `make plugins-install`; the
`plugins:` target in the `Makefile` is the canonical list of them.

## 5. Worked example: `encrypt` and `decrypt`

Encrypting and decrypting a file or a piece of text with a passphrase, the same two operations the
file-crypt plugin's tool window offers. This was the plugin's own `filecrypt` command until
mystic-crypt 12.0.0; the library's `encrypt` and `decrypt` do the job now.

**A file.** `--in` is what to encrypt, `--out` what to write. Prefer `--passphrase-stdin` over
`--passphrase`, so the passphrase never ends up in the shell history or in the process arguments:

```
$ printf 'my passphrase\n' | java -jar mystic-crypt-ui.jar --cli encrypt --in notes.txt --out notes.txt.enc --passphrase-stdin
encrypted 20 bytes to notes.txt.enc
```

Decrypting takes the file back:

```
$ printf 'my passphrase\n' | java -jar mystic-crypt-ui.jar --cli decrypt --in notes.txt.enc --out notes-again.txt --passphrase-stdin
decrypted 20 bytes to notes-again.txt
```

`--out` is optional. Left out, `encrypt` prints the result as base64 and `decrypt` prints it as
text. Given, the file is written even if a file of that name is already there, so pick a name that
is free. `--in -` reads the input from standard input.

**A text.** Use `--text` instead of `--in`, and the result is printed instead of written:

```
$ java -jar mystic-crypt-ui.jar --cli encrypt --text "meet at six" --passphrase "my passphrase"
TUNSWVBUAQAJJ8Brg2w1EbH/27k50tetiMIOIgqFRU4B4zjDeWzMcgP3hbfdGZyCNyaLshKzhi1dGY/OmAxrPmJj9dfnbMa7VRAWFdvlK7u+MfZudN+T9TlED3Vfe8rcwds=

$ java -jar mystic-crypt-ui.jar --cli decrypt --text "TUNSWVBUAQAJJ8Brg2w1EbH/27k50tetiMIOIgqFRU4B4zjDeWzMcgP3...wds=" --passphrase "my passphrase"
meet at six
```

`--text-stdin` reads the text from standard input, which is the better way for anything long, and
`--passphrase-stdin` works here too: it takes the first line of standard input, so it cannot be
combined with `--text-stdin`.

A wrong passphrase, or data that was altered, ends with exit code 1 and
`error: could not decrypt: the passphrase is wrong or the data was altered`.

## 6. Worked example: `share`

Splitting a secret into shares so that several of them are needed to get it back, and putting it
together again. This was the secret-sharing plugin's command until mystic-crypt 12.0.0; the
library's `share` has taken its place, with option names of its own.

**Splitting.** `--threshold` is how many shares are needed later, `--shares` how many to produce,
and `--secret-stdin` keeps the secret out of the process arguments:

```
$ printf 'the master password\n' | java -jar mystic-crypt-ui.jar --cli share split --secret-stdin --threshold 3 --shares 5 --out shares.txt
wrote 5 shares to shares.txt; any 3 of them reconstruct the secret
```

Each line of the file is one share, and each one carries the identifier of the split it belongs
to, the threshold, the number of shares that were produced and its own index:

```
mcs1:6f3e47ccb9606b8f:3:5:1:7bCHc3Lwj6Da5levbviep4uRGQ:ffa1c790
mcs1:6f3e47ccb9606b8f:3:5:2:3v4gMfb5od5YeWVRtg0BjKyQ2A:3506291c
mcs1:6f3e47ccb9606b8f:3:5:3:RybCYuloXQrn7RKOuYbsXEhzpQ:4fa48725
mcs1:6f3e47ccb9606b8f:3:5:4:Aaux6OL4CF9khDAU3VhtgbJRBA:b75313e2
mcs1:6f3e47ccb9606b8f:3:5:5:mHNTu_1p9IvbEEfL0tOAUVayeQ:b7239308
```

Without `--out` the shares are printed one per line instead. Use `--file` in place of
`--secret-stdin` to split the content of a file, or `--secret` to pass the secret as a value,
which is convenient and leaves it in the process list. Hand the lines out separately: whoever
collects the threshold number of them can rebuild the secret.

**Combining.** Either give the shares one by one with a repeated `--share`, or read them from a
file with `--file`:

```
$ java -jar mystic-crypt-ui.jar --cli share combine \
    --share='mcs1:6f3e47ccb9606b8f:3:5:1:7bCHc3Lwj6Da5levbviep4uRGQ:ffa1c790' \
    --share='mcs1:6f3e47ccb9606b8f:3:5:3:RybCYuloXQrn7RKOuYbsXEhzpQ:4fa48725' \
    --share='mcs1:6f3e47ccb9606b8f:3:5:5:mHNTu_1p9IvbEEfL0tOAUVayeQ:b7239308'
the master password

$ java -jar mystic-crypt-ui.jar --cli share combine --file shares.txt
the master password
```

Any three of the five work, order does not matter. With too few, the command says so:
`error: this split needs 3 shares, but 2 were given`. Add `--out` to write the rebuilt secret to a
file rather than print it.

## 7. Worked example: `keyx`

Agreeing on a shared secret with someone who only ever sees your public key. This was the
kem-demo plugin's command until mystic-crypt 12.0.0 and is the library's now. The three
subcommands are three separate runs on purpose, because the two sides are normally two people on
two machines.

**Step 1, the recipient makes a key pair.** The private half goes into the file named by `--key`
and stays there; `--public` writes the public half to a file, which can then be handed out:

```
$ java -jar mystic-crypt-ui.jar --cli keyx new --key alice.key --public alice.pub
algorithm: ML-KEM-768
private key written to alice.key - this file is the secret half, hand out only the public one
public key written to alice.pub
```

Without `--public` the public half is printed instead, as a single line:

```
public key: MCKX1$PUB$ML-KEM-768$MIIEsjALBglghkgBZQMEBAIDggShALeVckbqPL+0Psm4udzFtoc4nlTDihN8m...
```

`--algorithm` picks the scheme, `ML-KEM-768` by default; `ML-KEM-512`, `ML-KEM-1024`, `X25519`
and `Hybrid X25519 + ML-KEM-768` are the other choices (the last one has spaces in its name, so
it needs quoting). The key files are written even if files of those names are already there, so
run this where nothing is at risk. Whoever holds `alice.key` can read everything sent to it, so
treat it like any other private key.

**Step 2, the sender turns that public key into a handshake.** `--out` writes the handshake to a
file; `--message` and `--encrypted` optionally add a message encrypted with the secret that was
just derived:

```
$ java -jar mystic-crypt-ui.jar --cli keyx send --recipient alice.pub --message "meet at six" --out handshake.txt --encrypted message.enc
algorithm: ML-KEM-768
handshake written to handshake.txt
shared secret fingerprint: edabe5b2 - read it out to the other side to check that both match
encrypted message written to message.enc
```

Without `--out` the handshake is printed as `handshake: MCKX1$HS$ML-KEM-768$...` instead. The
handshake and the encrypted message go back to the recipient, who is the only side that can turn
the handshake into the secret, because that takes `alice.key`.

**Step 3, the recipient derives the same secret and reads the message.** `--handshake` is the file
that came back:

```
$ java -jar mystic-crypt-ui.jar --cli keyx receive --key alice.key --handshake handshake.txt --encrypted message.enc
algorithm: ML-KEM-768
shared secret fingerprint: edabe5b2 - it must match the one the other side read out
message: meet at six
```

Both sides print the fingerprint of their secret. Compare them over a channel you trust: if the
two differ, the exchange did not happen between the two keys that were meant.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
- [[Secure-your-passwords-with-obfuscation]]
