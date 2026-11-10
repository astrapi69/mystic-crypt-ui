# Password Hashing

## 1. What it is for

This tool turns a password into a value you can store, and checks a password against such a
stored value later. That is what you need when you build or maintain a system that has to
recognise a password at login without ever keeping the password itself: a user table, a config
file, a service account list.

Hashing is one-way. Nothing in this application, and nothing anywhere else, gets the password
back out of a hash. If you want something you can read again later, you want the database
entries of mystic-crypt itself, the file encryption tool, or the reversible obfuscation tool
instead. Reach for Password Hashing only when "is this the same password as before?" is the
question you need answered.

## 2. Where to find it

**Plugins** menu, submenu **Password Hashing**, item **Password Hashing**.

The tool opens as its own window inside the main application window. If the **Plugins** menu has
no **Password Hashing** submenu, the plugin is not installed or has been disabled: open
**File** > **Settings...**, tab **Plugins**, and install or enable it there (see
[[install-a-plugin]]).

## 3. Using it

1. Open the tool. The **Algorithm:** box starts on whatever the settings say, Argon2id unless
   you changed it. Under the box a short line describes the selected algorithm.
2. Pick the algorithm you need:
   - **Argon2id**, described as "memory hard, the one to pick when nothing else is required".
   - **PBKDF2**, "the one certifications ask for".
   - **bcrypt**, "long established; looks at the first 72 bytes of a password only".
   - **scrypt**, "memory hard, older than Argon2id and widely available".
3. Type the password into the **Password:** field.
4. Press **Hash**. When it worked, the encoded hash appears in the **Hash:** area and the line at
   the bottom of the window says how long it took, for example `Argon2id took 87 ms`. For bcrypt
   and scrypt the cost is appended, because it can be read out of the hash itself:
   `bcrypt took 61 ms at 2^10 rounds`, `scrypt took 133 ms at N=16384 r=8 p=1`.
   When it did not work, the **Hash:** area is emptied and the line starts with `not hashed:`
   followed by the reason, for example `not hashed: there is no password to hash`.
5. To check a password, make sure a hash is in the **Hash:** area. That area is editable, so it
   can be the hash you just made or one you paste in from elsewhere. Type the password into
   **Verify password:** and press **Verify**. The line at the bottom then reads:
   - `matches (Argon2id)` when the password is the one the hash was made from, naming the
     algorithm that recognised it,
   - `does not match` when it is not,
   - `hash a password first` when the **Hash:** area is empty,
   - `this does not look like a hash any of these algorithms made` when the pasted value is not
     recognisable as a hash of the four algorithms.
6. Select the hash in the **Hash:** area and copy it out before you close the window. There is no
   copy button, and nothing is saved: closing the window loses the hash.

## 4. Settings

| Key | Default | What it changes |
|---|---|---|
| `default.algorithm` | `Argon2id` | Which algorithm the **Algorithm:** box starts on. Accepted values are the four the tool offers: `Argon2id`, `PBKDF2`, `bcrypt`, `scrypt`. Anything else falls back to `Argon2id`. |

Edited under **File** > **Settings...**, tab **Plugin settings**, entry **Password Hash**: change
the value cell and press **Apply**, or press **Reset to defaults** to get the declared default
back. The setting only pre-selects: you can pick any of the four in the window at any time. It is
read when a tool window opens, so a changed value applies to the next window you open, not to one
that is already on screen.

## 5. On the command line

This plugin contributes no commands of its own, and no plugin in this application does: everything
on the command line comes from the mystic-crypt library, reachable through `--cli`. For hashing
that is the library's `hash` and `verify` commands, and they cover all four algorithms the window
offers: `argon2id`, `pbkdf2`, `bcrypt` and `scrypt`.

Hash a password (the jar name carries the version of your installation):

```
$ java -jar mystic-crypt-ui-8.2-SNAPSHOT-all.jar --cli hash --password "correct horse battery staple"
$argon2id$v=19$m=65536,t=3,p=1$g/KAgjvZMzj3dRKZnzvFmw$DQJEHtn7FgUqM3FYLmQPJ8zqxi4I71e8nYpVP+kUpO8
```

A password given as an argument is visible in the process list of the machine. `--password-stdin`
reads it from the first line of standard input instead, and `--algorithm`, short `-a`, picks the
algorithm (`argon2id`, the default, `pbkdf2`, `bcrypt` or `scrypt`):

```
$ printf 'correct horse battery staple\n' | java -jar mystic-crypt-ui-8.2-SNAPSHOT-all.jar --cli hash --algorithm pbkdf2 --password-stdin
$pbkdf2-sha256$i=600000$csj4mUh03+IGUHJxemHR3w$qNiO3GUJYFD5ROiDwA05Fv2z2bQHO8FC6uiOjbIz5TE
$ printf 'correct horse battery staple\n' | java -jar mystic-crypt-ui-8.2-SNAPSHOT-all.jar --cli hash -a bcrypt --password-stdin
$2y$10$XMZJnxdM95bQLym9vkB9ZOTVDgcR45FQOiHg8TdMLqh6cPh7Zr7cq
$ printf 'correct horse battery staple\n' | java -jar mystic-crypt-ui-8.2-SNAPSHOT-all.jar --cli hash -a scrypt --password-stdin
$scrypt$ln=15,r=8,p=1$1zG29TsNWMKpUncVYPbVow$KfcjeX3OXTYV8jDlFTeNZgYDhy6W2g9HOFk139yF2RM
```

Check a password against a hash. There is no algorithm option on `verify`, and there is nothing to
select: which algorithm made a hash is read out of the hash itself, and the name in brackets after
the answer is the one it found. Put the hash in single quotes, otherwise the shell eats the dollar
signs:

```
$ java -jar mystic-crypt-ui-8.2-SNAPSHOT-all.jar --cli verify --hash '$argon2id$v=19$m=65536,t=3,p=1$g/KAgjvZMzj3dRKZnzvFmw$DQJEHtn7FgUqM3FYLmQPJ8zqxi4I71e8nYpVP+kUpO8' --password "correct horse battery staple"
matches (ARGON2ID)
$ echo $?
0
```

A bcrypt or scrypt hash is checked the same way and answers `matches (BCRYPT)` or
`matches (SCRYPT)`. A wrong password prints `does not match` with the same algorithm in brackets
and exits with 1, which is what makes the command usable in a script. A value it does not
recognise as a hash at all is the third case: it prints an error naming the prefixes it knows and
exits with 2.

## 6. Limits and pitfalls

- **bcrypt refuses a long password instead of silently shortening it.** bcrypt looks at the first
  72 bytes of a password and no further. Hashing a longer one would mean only part of it is ever
  checked, and nothing in the window would say so, so the tool refuses and says why:

  ```
  not hashed: bcrypt only looks at the first 72 bytes of a password, and this one is 80 - use Argon2id or scrypt for a password this long
  ```

  The **Hash:** area is emptied at the same time, so no half-valid value stays on screen. Note
  that 72 is bytes, not characters: umlauts, accents and emoji count for more than one each. Argon2id, PBKDF2 and scrypt take a long passphrase in full,
  and every character of it counts.
- **The same password hashed twice gives two different hashes.** That is the point: every hash
  gets a fresh random salt, so two accounts with the same password do not look the same in your
  database. Never compare two hashes for equality to decide whether two passwords are equal, use
  **Verify**.
- **There is no confirmation field.** A typo in **Password:** is hashed just as happily as the
  password you meant. Verify the password right after hashing it, that is the only check the tool
  can give you. A trailing space is a different password.
- **An empty field gives a flat answer, not a complaint.** An empty **Password:** field produces
  `not hashed: there is no password to hash`; an empty **Verify password:** field produces
  `does not match`, not a hint that you forgot to type something.
- **The algorithm box has no effect on Verify.** Which algorithm made a hash is read out of the
  hash itself, so a pasted value is checked correctly whatever the box happens to show. The other
  way round, changing the box does not re-hash anything: press **Hash** again.
- **A pasted value that merely contains a colon is taken for a PBKDF2 hash.** The answer is then
  `does not match` rather than `this does not look like a hash any of these algorithms made`, so
  a wrong paste can look like a wrong password. Check what you pasted before you conclude the
  password is wrong.
- **A tampered hash stops matching.** Editing any part of a stored hash, for instance rewriting a
  scrypt hash to claim a cheaper cost than the one that made it, produces `does not match`. There
  is no way to make an existing hash cheaper to check.
- **The scrypt form is this application's own.** A scrypt hash from this tool looks like
  `scrypt$16384$8$1$` followed by Base64, a form built here because the underlying library hands
  out raw bytes and takes the parameters separately. Other systems will generally not accept it
  as it stands. The Argon2id, PBKDF2 and bcrypt values are in their usual encoded forms and
  travel fine.
- **The command line takes all four algorithms, but not the window's scrypt form.** An Argon2id,
  PBKDF2 or bcrypt hash made in the window is checked by `--cli verify` as it stands. A scrypt
  hash is not: the command line writes and reads the library's `$scrypt$ln=15,r=8,p=1$` form,
  while the window writes the `scrypt$16384$8$1$` form described above. Handed the window's value,
  `--cli verify` answers `error: ... is not an encoded hash of a known algorithm: it starts with
  none of $argon2id$, $pbkdf2-sha256$, $scrypt$, $2a$, $2b$ or $2y$` and exits with 2.
- **Nothing is stored.** The hash is not written into your database, no history is kept, and the
  window starts empty every time. Copy what you need out before closing it.
- **Hashing is meant to be slow.** A few tenths of a second per hash is normal and desirable; the
  memory hard algorithms also want real memory while they run (Argon2id here takes 64 MB per
  hash). This is a tool for hashing one password at a time, not for processing a list.

## 7. Under the hood

The hashing and the checking are done by the mystic-crypt library and BouncyCastle, nothing is
implemented in the tool itself except the way an scrypt result is written down. With the library
version this ships with (mystic-crypt 12.0.0) the parameters are: Argon2id with 3 passes over 64 MB
at parallelism 1, a 16 byte random salt and a 32 byte hash; PBKDF2 with HMAC-SHA256 over 600000
iterations and a 256 bit result; bcrypt at 2^10 rounds; scrypt at N=16384, r=8, p=1. None of them
is configurable from the window, the cost that was used travels inside the hash. That is also why
verification needs nothing remembered alongside the hash: the salt, the cost and the algorithm
marker are all part of the stored string, which is what lets the tool answer `matches (bcrypt)`
for a value it has never seen before. The password itself stays in the window's fields for as
long as the window is open, so close the tool when you are done with it.

## 8. See also

- [[Getting-Started]]
- [[install-a-plugin]]
- [[Secure-your-passwords-with-obfuscation]]
