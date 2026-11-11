# The Command Line

Much of what mystic-crypt-ui does with keys, hashes, checksums and signatures is also reachable
from a terminal, without opening a window at all. The command line is the same application: one
jar, one `--cli` switch, and the commands of the mystic-crypt library the desktop app is built on,
mystic-crypt 13.9 in this release.

This page shows how to start it, how to hand it a password without exposing it, where the
commands come from, which commands and options exist, and what three of them look like in
practice.

## 1. Starting it

### The runnable jar with `--cli`

`make build` packages the runnable jar with all its dependencies into
`build/libs/mystic-crypt-ui-<version>-all.jar` (the version comes from `projectVersion` in
`gradle.properties`). After an installation, the same jar sits in the installation directory
under the name `mystic-crypt-ui.jar`, which is the name the examples on this page use.

Put `--cli` first, then the command and its options:

```
java -jar mystic-crypt-ui.jar --cli checksum --algorithm SHA-256 notes.txt
```

`--cli` has to be the **first** argument. The application looks at its first argument only: when
it is `--cli`, the switch is dropped, the rest goes to the command line, and the application ends
with the command's exit code. Anything else, `--cli` in a later position included, starts the
window as usual. On the command line path the application never creates a window, so this also
works on a machine with no display, over SSH, or in a script.

Before the command runs, the application registers Bouncy Castle, loads and starts the installed
plugins and adds the commands they contribute (see sections 2 and 4); afterwards it stops the
plugins again.

To see the whole list of commands that your installation actually offers:

```
java -jar mystic-crypt-ui.jar --cli --help
```

Every command has its own help, and so does every subcommand; `help <command>` prints the same:

```
java -jar mystic-crypt-ui.jar --cli encrypt --help
java -jar mystic-crypt-ui.jar --cli share split --help
java -jar mystic-crypt-ui.jar --cli help keyx
```

`-V` or `--version` works on every command and prints `mystic-crypt CLI 1.0`. That text is fixed in
the library and does not change with its version.

### The `mystic-crypt` launcher scripts

The installer offers an optional pack, "Command-line interface (the 'mystic-crypt' console
tool)". It installs two small launcher scripts next to the jar: `mystic-crypt` for Unix-like
systems and `mystic-crypt.bat` for Windows. They pass everything through to the command line, so
there is no `--cli` and no `java -jar` to type:

```
./mystic-crypt hash --algorithm argon2id
mystic-crypt.bat kem --algorithm hybrid
```

The launcher scripts run the mystic-crypt library's entry point straight out of the jar. Since
every command comes from the library, their `--help` lists the same commands as the jar's. What
they leave out is the plugin system: no plugin is loaded, so a command contributed by a plugin you
installed yourself is missing there, and for that one, go through the jar with `--cli`.

### Passwords, passphrases and other secrets

Seven commands need a secret: `hash` and `verify` (a password), `checksum --hmac` (a MAC key),
`encrypt` and `decrypt` (a passphrase), `keystore` (the key store password) and `share split` (the
secret to split). Each takes it in one of two ways that keep it off the command line.

**Asked for in the terminal.** Leave the secret out, and the command asks for it without echoing
what you type: `password: `, `MAC key: `, `passphrase: `, `key store password: ` or `secret: `.

```
$ java -jar mystic-crypt-ui.jar --cli hash
password:
$argon2id$v=19$m=65536,t=3,p=1$p6mYmXxuA0xndrp97niMEA$qsK7vV6sDSFtZNs7DHyn+1I0VE9dmBpmlCqaXBq21Ws
```

The prompt only appears when standard input and standard output are both the terminal. Redirect
the output into a file or pipe something in, and there is nobody to ask: the command stops with
`a password is required: pass --password-stdin, or run in a terminal to be asked for it` (or the
same sentence for its own kind of secret). Most commands print that as an `error:` line and end
with exit code 2; `hash` and `keystore` print it inside a Java stack trace and end with 1.

**From standard input, for scripts.** Each of those commands has a `-stdin` option that reads the
secret from the first line of standard input: `--password-stdin` (`hash`, `verify`, `keystore`),
`--key-stdin` (`checksum`), `--passphrase-stdin` (`encrypt`, `decrypt`) and `--secret-stdin`
(`share split`). In bash, read the secret once into a variable and hand it over with `printf`,
which is built into the shell and therefore never shows up in the process list:

```
read -rs -p 'key store password: ' STORE_PASSWORD; echo
printf '%s\n' "$STORE_PASSWORD" | java -jar mystic-crypt-ui.jar --cli keystore list --file my.p12 --password-stdin > entries.txt
```

A file only you can read works as well: `--password-stdin < store-password.txt`.

The options that take the secret as a value, `hash` and `verify --password`, `checksum --key`,
`encrypt` and `decrypt --passphrase`, `keystore --password <value>` and `share split --secret`,
still work in this release. They print a warning on standard error, because an argument is visible
to every process on the machine and ends up in the shell history, and mystic-crypt 14.0 removes
them. Do not use them.

### Output, redirection and logging

The result of a command goes to standard output and nothing else does, so redirecting it gives a
usable file:

```
java -jar mystic-crypt-ui.jar --cli checksum --algorithm SHA-256 notes.txt > notes.sha256
```

Everything else goes to standard error: the reports of a command that wrote a file
(`encrypted 29 bytes to notes.txt.enc`), warnings and errors, and the noise of the plugin system.
Through the jar, standard error starts with three lines from SLF4J saying that no logging provider
was found, and every installed plugin logs an `INFO` entry when it is started and another when it
is stopped. None of that reaches standard output.

`-Dmystic.crypt.ui.cli.verbose=true` (before `-jar`) keeps the level of the plugin system's
logging as it is instead of turning it down to warnings. That only has an effect when Logback is
on the class path, and the application jar does not bring it, which is what the three SLF4J lines
say.

### Which plugins the command line loads

By default it reads the same plugins directory the desktop app uses,
`~/.config/mystic-crypt-ui/plugins` (see [Install a plugin](install-a-plugin)). Point it
somewhere else with `-Dmystic.crypt.ui.plugins.dir=/path/to/plugins`, which is handy for trying
out a freshly built plugin without installing it.

## 2. Where the commands come from

**Every command on the command line comes from the mystic-crypt library.** Password hashing, key
generation, key encapsulation, checksums, PEM/DER conversion, obfuscation, certificates, key store
management, signatures, file and text encryption, secret sharing, key exchange and the state of a
release key are all the library's own commands. The application reuses the library's root command
as it is, without reimplementing anything, so every command the library gains is available here as
soon as the library is updated. New command line capability is therefore built in the library
first, never forked into the user interface.

A plugin may still add a command of its own for what the library does not cover, through the
`PluginCommandContribution` extension point: it contributes picocli commands, which are then added
to the library's root command. **None of the plugins that ship with the application uses that
extension point**; with all of them installed, `--cli --help` lists exactly the library's commands.
Several plugins have a command line counterpart in the library instead: the file encryption tool in
`encrypt` and `decrypt`, the secret sharing tool in `share`, the key exchange tool in `keyx`, the
password hashing tool in `hash` and `verify`, the obfuscation tool in `obfuscate` and
`disentangle`.

Two things can go wrong while the contributed commands are collected, and neither takes the
command line down:

- A plugin whose commands cannot be built is skipped with a note on standard error,
  `the commands of <class> are not available: <reason>`, and the rest of the command line still
  works. A single contributed command that picocli cannot use is left out the same way
  (`a plugin command is not usable and was left out: <reason>`).
- A contributed command whose name the library already uses is left out, again with a note on
  standard error, and the library keeps the name. The note reads
  `the plugin command 'hash' was left out: the name is already in use by mystic-crypt`.
  Without that check a clash would either make the whole command line fail to assemble, taking
  every other command with it, or let a plugin quietly replace `hash` with something else.

## 3. The available commands

The list below is the library's root command in mystic-crypt 13.9. Run `--cli --help` to see what
*your* installation offers, which is the authoritative list.

| Command | What it does |
|---|---|
| `hash` | Hash a password with Argon2id, PBKDF2, bcrypt or scrypt and print the encoded hash. |
| `verify` | Verify a password against an encoded hash. The algorithm is read from the hash itself. Exit code 0 = matches, 1 = does not match, 2 = the hash is not of a known algorithm. |
| `keygen` | Generate a key pair and print (or write) its keys as PEM. Exit code 0 = generated, 2 = error. |
| `kem` | Run a two-party ML-KEM or hybrid X25519+ML-KEM key encapsulation and show that both sides derive the same shared secret. |
| `checksum` | Compute the checksum of a file: a plain digest, which answers whether the file changed, or with `--hmac` a keyed MAC, which answers whether it changed at the hands of someone without the key. |
| `der2pem` | Deprecated: use `convert` instead, which detects what the file is and converts in both directions. Convert a DER-encoded private key to PEM. |
| `obfuscate` | Obfuscate text with a character-substitution map (`--rule a=x`), optionally with an operation at named positions (`--rule a=x:UPPERCASE@0,3`). Exit code 0 = obfuscated, 2 = error. |
| `disentangle` | Recover text obfuscated by `obfuscate`, using the same rules, operated ones included. |
| `cert` | Create a self-signed X.509 certificate and write it as PEM, with basic constraints, key usage and subject alternative names. Exit code 0 = written, 2 = error. |
| `keystore` | Inspect and manage a Java key store (PKCS12, JKS or JCEKS), through six subcommands. |
| `sign` | Sign a file (or standard input) with an RSA, ECDSA, DSA, Ed25519, ML-DSA or SLH-DSA private key, from a PEM or DER file. |
| `verify-signature` | Verify a signature made by `sign`, with the public key. Exit code 0 = valid, 1 = invalid, 2 = an error before verification. |
| `encrypt` | Encrypt a file or a text with a passphrase (AES-GCM, PBKDF2-HMAC-SHA256). Exit code 0 = encrypted, 2 = error. |
| `decrypt` | Decrypt a file or a text encrypted by `encrypt`. Exit code 0 = decrypted, 1 = wrong passphrase or altered data, 2 = error. |
| `share` | Split a secret into shares and combine them back (Shamir secret sharing). |
| `keyx` | Exchange a shared secret with another person: `new` on the recipient's side, `send` on the sender's, `receive` back on the recipient's. |
| `convert` | Say what a key or certificate file is, and convert it between PEM and DER or between PKCS#1 and PKCS#8. Exit code 0 = done, 2 = error. |
| `release-key` | Show the state of an OpenPGP release key in a GnuPG home, read with Bouncy Castle: nothing is written to the home and gpg is never run. |
| `help` | Display help information about the specified command. |

`keystore`, `share`, `keyx` and `release-key` group their work into subcommands; `--cli keystore
--help` and its siblings list them.

### The options of each command

Every command and every subcommand also takes `-h`/`--help` and `-V`/`--version`, which are left
out below. "Required" means the command refuses to run without it. The options that take a secret
as a value are marked deprecated, see [Passwords, passphrases and other
secrets](#passwords-passphrases-and-other-secrets).

- **`hash`**: `-a`, `--algorithm` `argon2id` (default), `pbkdf2`, `bcrypt` or `scrypt`;
  `--password-stdin`; `--password` (deprecated). Without either, it asks.
- **`verify`**: `--hash` (required), the encoded hash, in single quotes so the shell leaves its
  dollar signs alone; `--password-stdin`; `--password` (deprecated).
- **`keygen`**: `-a`, `--algorithm` (default `RSA`; for example `EC`, `X25519`, `X448`,
  `ML-KEM-768`, `ML-DSA-65`, dashes or underscores); `-c`, `--curve`, the named curve of an EC key
  (`secp256r1`, `secp384r1`, `secp521r1`); `-s`, `--size` in bits for RSA and other size-based
  algorithms (default `2048`); `--format` `pkcs8` (default) or `pkcs1`; `--out-private` and
  `--out-public`, files to write instead of standard output; `--print-details`, which says what was
  generated.
- **`kem`**: `-a`, `--algorithm` `ML-KEM-512`, `ML-KEM-768` (default), `ML-KEM-1024` or `hybrid`.
- **`checksum`**: the file, as the only argument; `-a`, `--algorithm`, a JDK digest name such as
  `MD5`, `SHA-1`, `SHA-256` (default) or `SHA-512`, with `--hmac` a MAC name such as `HmacSHA256`
  (the default then); `--hmac`; `--key-stdin`; `--key` (deprecated). With `--hmac` and neither key
  option, it asks.
- **`der2pem`**: `--in` (required), the DER private key; `--out`, a file instead of standard output.
- **`obfuscate`**: `--rule` (required, repeatable), `a=x` or `a=x:OPERATION@positions`; `--text`;
  `--text-stdin`.
- **`disentangle`**: `--rule` (required, repeatable), the same rules; `--text`; `--text-stdin`.
- **`cert`**: `--out` (required); `-a`, `--algorithm` (default `RSA`); `-s`, `--size` (default
  `2048`); `--days` (default `365`); `--subject` (default `CN=mystic-crypt`);
  `--signature-algorithm` (default `SHA256withRSA`); `--basic-constraints` `ca` or `end-entity`,
  optionally with a path length as in `ca,pathlen=1`; `--key-usage`, comma separated
  (`digitalSignature`, `nonRepudiation`, `keyEncipherment`, `dataEncipherment`, `keyAgreement`,
  `keyCertSign`, `cRLSign`, `encipherOnly`, `decipherOnly`); `--san type:value`, repeatable
  (`dns:`, `ip:`, `email:`, `uri:`); `--critical`, which of `basic-constraints`, `key-usage` and
  `san` to mark critical.
- **`keystore`**, subcommands `list`, `create`, `add-keypair`, `import-cert`, `export-cert` and
  `delete`. All of them take `-f`, `--file` (required); `-t`, `--type` `PKCS12` (default), `JKS` or
  `JCEKS`; `--password-stdin`; and `-p`, `--password`, which asks in the terminal when it is given
  without a value and is deprecated with one. On top of that:
  - `add-keypair`: `-a`, `--alias` (required); `-A`, `--algorithm` (default `RSA`; for example
    `EC`, `DSA`, `ML-DSA-65`); `-d`, `--dname` (default `CN=mystic-crypt`); `--days-valid`
    (default `365`); `--key-size` (default `2048`).
  - `import-cert`: `-a`, `--alias` (required); `-c`, `--certificate` (required), a PEM or DER file.
  - `export-cert`: `-a`, `--alias` (required); `-o`, `--out` (required), where the PEM goes.
  - `delete`: `-a`, `--alias` (required).
- **`sign`**: `-a`, `--algorithm` (required: `RSA`, `EC` or `ECDSA`, `DSA`, `Ed25519`,
  `ML-DSA-44`, `ML-DSA-65`, `ML-DSA-87`, an SLH-DSA parameter set such as `SLH-DSA-SHA2-128S`, or a
  JCA name such as `SHA512withRSA`); `--in` (required), the file or `-` for standard input; `--key`
  (required), the private key, PEM or DER; `--signature` (required), where the raw signature goes.
- **`verify-signature`**: the same four, with `--key` the public key and `--signature` the file to
  check.
- **`encrypt`**: `-i`, `--in`, a file or `-` for standard input; `-t`, `--text`; `--text-stdin`;
  `-o`, `--out`, without which the result is printed as base64; `--passphrase-stdin`; `-p`,
  `--passphrase` (deprecated).
- **`decrypt`**: the same options; `--text` and `--text-stdin` take the base64 text, and without
  `--out` the result is printed as text.
- **`share split`**: `-t`, `--threshold` (required); `-n`, `--shares` (required);
  `--secret-stdin`; `--file`, a file to split instead; `-o`, `--out`; `--secret` (deprecated).
- **`share combine`**: `--share`, repeatable; `--file`, one share per line; `-o`, `--out`.
- **`keyx new`**: `-k`, `--key` (required), where the private key goes; `-p`, `--public`, where the
  public key goes, printed without it; `-a`, `--algorithm` `ML-KEM-768` (default), `ML-KEM-512`,
  `ML-KEM-1024`, `X25519` or `Hybrid X25519 + ML-KEM-768`.
- **`keyx send`**: `-r`, `--recipient` (required), the recipient's public key; `-o`, `--out`, where
  the handshake goes, printed without it; `-m`, `--message`; `-e`, `--encrypted`, where the
  encrypted message goes.
- **`keyx receive`**: `-k`, `--key` (required); `-s`, `--handshake` (required); `-e`,
  `--encrypted`, the encrypted message to read.
- **`convert`**: `-i`, `--in` (required); `--to` `pem`, `der`, `pkcs1` or `pkcs8`; `-o`, `--out`,
  without which the result is printed, which only works for PEM; `--describe`, which only says what
  the file is.
- **`release-key status`**: `--home`, the GnuPG home (default `GNUPGHOME`, else `~/.gnupg`);
  `--key`, the fingerprint or long key id of the one certificate to show; `--json`. Exit code 0 =
  read, 1 = the key named with `--key` is not in the home, 2 = the home cannot be read.
- **`help`**: the command to describe, as its argument.

## 4. Plugin commands come and go with the plugin

Nothing in the table above depends on a plugin being installed, because none of the shipped
plugins contributes a command. The rules below are what to expect from a plugin you write or
install yourself, whose command is only there while the plugin is installed **and** enabled:

- A plugin that is not in the plugins directory contributes nothing. Its command is simply
  absent from `--cli --help`, and using it ends in `Unmatched argument at index 0: 'mine'`,
  followed by the usage of the root command, with exit code 2.
- A plugin that is disabled under **File > Settings...**, tab **Plugins**, is not started, so its
  commands are gone too. That state is kept in a `disabled.txt` in the plugins directory and
  survives a restart, which means disabling a plugin in the window also takes its commands off the
  command line.
- Enabling it again, or copying its `.zip` into the plugins directory, brings the commands back
  on the next run. Nothing else has to be configured.

The internal plugins can be built and installed in one go with `make plugins-install`; the
`plugins:` target in the `Makefile` is the canonical list of them.

## 5. Worked example: `encrypt` and `decrypt`

Encrypting and decrypting a file or a piece of text with a passphrase, the same two operations the
file encryption tool's window offers.

**A file.** `--in` is what to encrypt, `--out` what to write. Leave the passphrase out, and the
command asks for it:

```
$ java -jar mystic-crypt-ui.jar --cli encrypt --in notes.txt --out notes.txt.enc
passphrase:
encrypted 29 bytes to notes.txt.enc
```

Decrypting takes the file back:

```
$ java -jar mystic-crypt-ui.jar --cli decrypt --in notes.txt.enc --out notes-again.txt
passphrase:
decrypted 29 bytes to notes-again.txt
```

In a script, add `--passphrase-stdin` and hand the passphrase over on standard input, as described
in [Passwords, passphrases and other secrets](#passwords-passphrases-and-other-secrets).

`--out` is optional. Left out, `encrypt` prints the result as base64 and `decrypt` prints it as
text. Given, the file is written even if a file of that name is already there, so pick a name that
is free.

**A text.** Use `--text` instead of `--in`, and the result is printed instead of written:

```
$ java -jar mystic-crypt-ui.jar --cli encrypt --text "meet at six"
passphrase:
TUNGSUxF5BefKqDjb6SVM8l2mdJ/7Q...

$ java -jar mystic-crypt-ui.jar --cli decrypt --text "<the base64 text encrypt printed>"
passphrase:
meet at six
```

Like any argument, the value of `--text` is visible to the other processes on the machine, so keep
it to texts that may be seen; a confidential text belongs in a file.

`--text-stdin` and `--in -` read the data from standard input. Then standard input cannot carry the
passphrase as well (`--passphrase-stdin` refuses the combination), and it is not a terminal to ask
in either, so in this release they only work with the deprecated `--passphrase`. For anything long,
write it to a file and use `--in`.

A wrong passphrase, or data that was altered, ends with exit code 1 and
`error: could not decrypt: the passphrase is wrong or the data was altered`.

## 6. Worked example: `share`

Splitting a secret into shares so that several of them are needed to get it back, and putting it
together again. The secret sharing tool's window does the same with shares of its own format, see
[[Plugin-Secret-Sharing]].

**Splitting.** `--threshold` is how many shares are needed later, `--shares` how many to produce.
Leave the secret out, and the command asks for it:

```
$ java -jar mystic-crypt-ui.jar --cli share split --threshold 3 --shares 5 --out shares.txt
secret:
wrote 5 shares to shares.txt; any 3 of them reconstruct the secret
```

Each line of the file is one share, and each one carries the identifier of the split it belongs
to, the threshold, the number of shares that were produced and its own index:

```
mcs1:45cae493ecc3f0f2:3:5:1:Z0GiMVnUOgJgDnWVN3cZNww:d0806efc
mcs1:45cae493ecc3f0f2:3:5:2:vZgxd_DEAeUYvRkEnpvQoPY:0ce48cd3
mcs1:45cae493ecc3f0f2:3:5:3:u7ezI9FxVpcU1kzizI-78o4:66870e4d
mcs1:45cae493ecc3f0f2:3:5:4:rw86n3ZNkJ4v9igr7-Xma7g:ef6e613c
mcs1:45cae493ecc3f0f2:3:5:5:qSC4y1f4x-wjnX3NvfGNOcA:b94c15f8
```

Without `--out` the shares are printed one per line instead. Use `--file` to split the content of
a file instead of a typed secret, and `--secret-stdin` in a script. Hand the lines out
separately: whoever collects the threshold number of them can rebuild the secret.

**Combining.** Either give the shares one by one with a repeated `--share`, or read them from a
file with `--file`:

```
$ java -jar mystic-crypt-ui.jar --cli share combine \
    --share='mcs1:45cae493ecc3f0f2:3:5:1:Z0GiMVnUOgJgDnWVN3cZNww:d0806efc' \
    --share='mcs1:45cae493ecc3f0f2:3:5:3:u7ezI9FxVpcU1kzizI-78o4:66870e4d' \
    --share='mcs1:45cae493ecc3f0f2:3:5:5:qSC4y1f4x-wjnX3NvfGNOcA:b94c15f8'
an example secret

$ java -jar mystic-crypt-ui.jar --cli share combine --file shares.txt
an example secret
```

Any three of the five work, order does not matter. With too few, the command says so:
`error: this split needs 3 shares, but 2 were given`. Add `--out` to write the rebuilt secret to a
file rather than print it.

## 7. Worked example: `keyx`

Agreeing on a shared secret with someone who only ever sees your public key. The three
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
shared secret fingerprint: 99973411 - read it out to the other side to check that both match
encrypted message written to message.enc
```

`--message` takes the text as an argument, and `keyx send` has no standard input variant for it in
this release, so the other processes on the machine can see it while the command runs, and the
shell history keeps it afterwards.

Without `--out` the handshake is printed as `handshake: MCKX1$HS$ML-KEM-768$...` instead. The
handshake and the encrypted message go back to the recipient, who is the only side that can turn
the handshake into the secret, because that takes `alice.key`.

**Step 3, the recipient derives the same secret and reads the message.** `--handshake` is the file
that came back:

```
$ java -jar mystic-crypt-ui.jar --cli keyx receive --key alice.key --handshake handshake.txt --encrypted message.enc
algorithm: ML-KEM-768
shared secret fingerprint: 99973411 - it must match the one the other side read out
message: meet at six
```

Both sides print the fingerprint of their secret. Compare them over a channel you trust: if the
two differ, the exchange did not happen between the two keys that were meant.

## See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[how-to-create-a-plugin]]
- [[Plugin-Password-Hashing]]
- [[Plugin-Secret-Sharing]]
- [[Plugin-Obfuscation]]
- [[Secure-your-passwords-with-obfuscation]]
