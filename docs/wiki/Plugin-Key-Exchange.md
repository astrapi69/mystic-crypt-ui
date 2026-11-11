# Key Exchange

## 1. What it is for

Key Exchange establishes one shared secret between two people who have never exchanged
a secret before, and it does so without either private key ever leaving its own machine.
One side hands out a public key, the other side encapsulates against it and sends back a
short piece of text called a handshake, and both sides end up holding the same secret,
which can then encrypt a message.

Reach for it when the recipient and the sender are two different people on two different
machines. If you only want to see what a key encapsulation does, the neighbouring
**Key Encapsulation Demo** plays both roles at once in a single window and is the better
choice (see [The Key Encapsulation Demo](#the-key-encapsulation-demo) below); if you want to
encrypt a file with a passphrase you already share, that is the file and text encryptor, not
this tool.

## 2. Where to find it

**Plugins** menu, **Key Encapsulation** submenu, item **Key Exchange**. It opens a window
titled "Key Exchange" inside the main window, switching it to its desktop view first if needed.

The same submenu also holds **Key Encapsulation Demo**. The Plugins menu only appears
once the plugin is installed, see [[install-a-plugin]]. Both items are greyed out until you have
signed in to a database, and again while the workspace is locked.

## 3. Using it

The window starts with a short note on what it does, then has one **Algorithm:** box, two
tabs, **Receive** and **Send**, and a bold result line along the bottom that reports what each
button did. Normally the two tabs are two people on two machines, and only the text in between
travels. Every text box has a **Copy** button next to it that puts its content on the
clipboard.

**On the receiving side (the Receive tab):**

1. Choose the algorithm in the **Algorithm:** box. The offered ones are, in this order,
   `ML-KEM-768`, `ML-KEM-512`, `ML-KEM-1024`, `X25519` and `Hybrid X25519 + ML-KEM-768`, and
   starts on the one from the settings.
2. Press **New key pair**. The public half appears under *1. Hand this public key to the
   other side*, and the result line says `made a ML-KEM-768 key pair` (with whichever
   algorithm you chose). Every text this tool produces starts with `MCKX1$`.
3. Give that public key to the other side: copy it out of the box, or press
   **Save public key...** and send the file. **Save my key...** writes the private half
   instead, and asks for confirmation first, in a dialog titled **Save the private key**, with
   the warning that whoever has that file can read everything that is ever sent to this key.
   **Load my key...** takes such a file back up later and sets the Algorithm box to whatever
   the stored key was made for.
4. When the handshake comes back, paste it under *2. Paste the handshake that came back*
   (or press **Load...**) and press **Derive the secret**. The result line says
   `derived the shared secret` and **My secret:** shows an eight character fingerprint.
5. Read the fingerprint out to the other side over a channel you trust, by phone for
   instance. Both sides must show the same eight characters.
6. Paste the encrypted message under *3. A message that arrived with it* and press
   **Read it**. The plain message appears in the box below and the result line says
   `read 23 characters`.

**On the sending side (the Send tab):**

1. Paste the public key you were given under *1. Paste the public key you were given*
   (or press **Load...**) and press **Make a shared secret**. The result line says
   `made a shared secret for ML-KEM-768`, **Shared secret:** shows the fingerprint, and
   the handshake appears under *2. Send this handshake back*.
2. Send that handshake back: copy it, or press **Save...** underneath it.
3. Type your message under *3. A message to send with it* and press **Encrypt it**. The
   encrypted form appears below it and the result line says `encrypted 23 characters`.
   Send that text too, by copy or with the **Save...** button under it.

When something did not work, the result line starts with `not` and names the reason:
`not derived: make or load a key pair first`, `not made: 'hello' is not from this tool`,
`not encrypted: make a shared secret first`, `not read: derive the shared secret first`,
`not saved: make a key pair first`. Nothing is silently skipped, and the fingerprint
stays at `-` when a step failed.

### The Key Encapsulation Demo

**Plugins** menu, **Key Encapsulation** submenu, item **Key Encapsulation Demo** opens a window
of the same name that plays the sender and the recipient at once. It is there to show what an
encapsulation does, not to exchange anything with anybody.

1. Pick the algorithm in **Algorithm:**: `ML-KEM-768`, `ML-KEM-512`, `ML-KEM-1024` or
   `Hybrid X25519 + ML-KEM-768`. It starts on the one from the settings.
2. Press **Run key exchange**. The window makes a key pair, encapsulates a secret against its
   public key and decapsulates it again, and shows the three results in hex:
   **Ciphertext (sender to recipient):**, **Sender shared secret:** and
   **Recipient shared secret:**, each with a **Copy** button.
3. The result line reads `shared secrets match` when both sides arrived at the same secret,
   `shared secrets do not match` when they did not, and `error:` with the reason when the
   platform could not run the algorithm.

Unlike the Key Exchange window, the demo shows the shared secrets themselves: they belong to a
throwaway key pair that never leaves the window, and every press makes a new one.

## 4. Settings

Edited under **File** menu, **Settings...**, tab **Plugin settings**, entry
**Key Encapsulation** in the list on the left. Change the value in the table and press
**Apply**; **Reset to defaults** puts both values back.

| Key | Default | What it changes |
|---|---|---|
| `default.exchange.algorithm` | `ML-KEM-768` | Which algorithm the **Algorithm:** box of Key Exchange starts on. Accepts `ML-KEM-768`, `ML-KEM-512`, `ML-KEM-1024`, `X25519` or `Hybrid X25519 + ML-KEM-768`. |
| `default.algorithm` | `ML-KEM-768` | The same for the neighbouring Key Encapsulation Demo, which does not offer `X25519`. The two settings are independent: changing one does not move the other. |

A value that is not one of the offered names is ignored rather than refused, and the
first offered algorithm applies instead. The setting is read when a tool window is
opened, so a window that is already open keeps whatever is selected in it.

## 5. On the command line

The three steps are also three subcommands of `keyx`, so the two sides can be two
machines. `keyx` comes from the mystic-crypt library, not from this plugin, so it is there
whether or not the plugin is installed. Its texts are the same as the window's, so one side
can use the window and the other the command line. `keyx` on its own prints the usage, and each
subcommand answers `--help`:

```
Usage: mystic-crypt keyx [-hV] [COMMAND]
Exchange a shared secret with another person: 'new' on the recipient's side,
'send' on the sender's, 'receive' back on the recipient's.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  new      Generate this side's key pair. Exit code 0 = generated, 2 = error.
  send     Take the recipient's public key and produce a handshake and a shared
             secret. Exit code 0 = done, 2 = error.
  receive  Take the handshake and arrive at the same shared secret. Exit code 0
             = done, 2 = error.
```

The recipient makes a key pair. `-k, --key` is the file this side's private key is
written to and is required, `-a, --algorithm` picks the algorithm and defaults to
`ML-KEM-768`, `-p, --public` writes the public half to a file:

```
java -jar mystic-crypt-ui.jar --cli keyx new -a ML-KEM-768 -k recipient.key -p recipient.pub
```

```
algorithm: ML-KEM-768
private key written to recipient.key - this file is the secret half, hand out only the public one
public key written to recipient.pub
```

Without `-p` the public half is printed instead of written, as the one line to hand to
the other side:

```
public key: MCKX1$PUB$ML-KEM-768$MIIEsjALBglghkgBZQMEBAIDggShAOLWX0tVWu58c8rQbi0DI...
```

The sender encapsulates against that public key, and can encrypt a message in the same
run. `-r, --recipient` is the file holding the recipient's public key and is required,
`-o, --out` writes the handshake to a file instead of printing it, `-m, --message` is the
message and `-e, --encrypted` the file its encrypted form goes to. The message is an argument,
which every process on the machine can see and the shell keeps in its history, so keep
confidential text to the window, or send it through the file encryption instead:

```
java -jar mystic-crypt-ui.jar --cli keyx send -r recipient.pub -m "meet at eight" -o handshake.txt -e message.txt
```

```
algorithm: ML-KEM-768
handshake written to handshake.txt
shared secret fingerprint: b4ad9ea5 - read it out to the other side to check that both match
encrypted message written to message.txt
```

The recipient turns the handshake into the same secret and reads the message. `-k` is the
private key from the first step, `-s, --handshake` the handshake that came back, and
`-e, --encrypted` the encrypted message to read:

```
java -jar mystic-crypt-ui.jar --cli keyx receive -k recipient.key -s handshake.txt -e message.txt
```

```
algorithm: ML-KEM-768
shared secret fingerprint: b4ad9ea5 - it must match the one the other side read out
message: meet at eight
```

Both sides print a `shared secret fingerprint:` line and say in that same line what to do
with it. If the two differ, the exchange did not happen between the two keys that were
meant, and nothing further should be trusted.

Every subcommand exits 0 when it did what was asked and 2 on an error. The hybrid
algorithm name contains spaces and a plus sign, so it needs quoting:
`-a "Hybrid X25519 + ML-KEM-768"`. A name that is not offered is refused by listing all
of them: `'bogus' is not one of ML-KEM-768, ML-KEM-512, ML-KEM-1024, X25519, Hybrid
X25519 + ML-KEM-768`.

The demo has its counterpart in `kem`, which runs both sides at once like the demo window:
`-a` takes `ML-KEM-512`, `ML-KEM-768` (the default), `ML-KEM-1024` or `hybrid`, and the command
prints the algorithm, the ciphertext, both secrets in hex and `shared secrets match`. It exits 0
when the secrets match and 1 when they do not.

## 6. Limits and pitfalls

- **The tool does not tell you whose public key you pasted.** There is no signature and
  no identity check anywhere in the exchange. Anyone who can swap the public key in
  transit gets a working exchange with you. The eight character fingerprint is the only
  defence: compare it over a channel the attacker does not control.
- **A wrong key pair does not produce an error, it produces a different secret.** Pressing
  **Derive the secret** with a handshake made against somebody else's public key of the
  same algorithm succeeds and shows a fingerprint. Only the fingerprint mismatch and the
  failure of **Read it** reveal it. A handshake for a *different* algorithm is caught by
  name, for example `this handshake is for ML-KEM-512 and these keys are for X25519`.
- **The Algorithm box does nothing on the Send tab.** The sending side takes the algorithm
  from the public key it was given. On the receiving side the box only matters at the
  moment **New key pair** is pressed, changing it afterwards has no effect on the existing
  key pair.
- **Public keys and handshakes look alike and are easy to swap.** Both start with `MCKX1$`.
  Pasting the wrong one is refused with `this is a public key, and a handshake was
  expected` or `this is a handshake, and a public key was expected`.
- **Pressing New key pair throws the previous one away.** The fingerprint resets to `-`
  and any handshake somebody already made against the old public key can no longer be
  opened. The same applies when you press **Make a shared secret** twice: every
  encapsulation produces its own secret, so a message encrypted with the previous one
  will no longer read.
- **Nothing is kept when the window closes.** The key pair, the secrets and the messages
  live in the tool window only, not in the database. **Save my key...** is the one way to
  keep a key pair, and that file is a private key: it belongs where private keys belong,
  not next to the public one you handed out.
- **A hybrid text with one half missing is refused**, with a message about "two halves".
  Truncating one of these lines in an email, or letting a mail client wrap it, breaks it.
  Only surrounding whitespace is trimmed, so a line break in the *middle* of a pasted
  public key, handshake or encrypted message makes it unusable, with `is not from this
  tool` or a decoding failure as the result.
- **Empty or foreign text** gives `there is nothing to read` or
  `'<the text>' is not from this tool`.
- **Loading the wrong file into "Load my key..."** is named rather than left to fail
  later: the public half gives `this is the public half; the private key of this side is
  needed here`, a partial file gives `this stored key is incomplete`.
- **A message that was altered on the way is refused,** it never decrypts into rubbish.
  A single flipped bit makes **Read it** fail.
- **On the command line, `keyx new` overwrites an existing private key file without
  asking.** Pointing `-k` at a file that already holds a key pair replaces it, and every
  message ever sent to the old key becomes unreadable, so pick a name that is not in use
  yet. A file that is not there ends the run with `error:` and the path, and handing the
  public key to `receive -k` is refused with the same "private key of this side" message
  as in the window: `error: this is the public half; the private key of this side is
  needed here`.

## 7. Under the hood

The exchange itself comes from the mystic-crypt library. `ML-KEM-512`, `ML-KEM-768` and
`ML-KEM-1024` are the NIST post-quantum mechanism from FIPS 203 and run on the JDK's own
implementation; `X25519` is the classical elliptic curve agreement from the JDK, where
"encapsulating" means making a throwaway key pair and sending its public half, with the
raw agreement result passed through HKDF; the hybrid runs both and derives one key from
the two results, so it stays safe as long as either half does. In every case the shared
secret is 32 bytes.

The message is encrypted with a key-committing authenticated scheme, AES-GCM with an additional
commitment tag (a BLAKE2b digest over the key and the nonce) bound to the key, which is why a
wrong secret or a modified message fails outright instead of yielding garbage. The secret itself
is never written to the screen or to any file: what you see is the first eight hex characters of
its SHA-256 digest, which is enough to compare two secrets and not enough to reconstruct one.

## 8. See also

- [[install-a-plugin]] - how the plugin gets into the Plugins menu
- [[Getting-Started]] - first launch, database and sign-in
