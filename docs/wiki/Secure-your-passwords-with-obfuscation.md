There are several ways to simply veil your passwords, like [ROT13](https://en.wikipedia.org/wiki/ROT13) or the [Caesar cipher](https://en.wikipedia.org/wiki/Caesar_cipher): each swaps every letter for another one by a fixed rule.

With mystic-crypt-ui you write the rule yourself. The obfuscation plugin builds a custom [Substitution cipher](https://en.wikipedia.org/wiki/Substitution_cipher) from a table of characters and what replaces each of them, optionally with an operation such as upper case at positions you name, and turns a text into its veiled form and back.

## What it protects against, and what it does not

Obfuscation keeps a password or a note from being read at a glance: over your shoulder, in a
screenshot, on a printed sheet. Whoever sees the veiled text without your rule table sees
nonsense.

It is not encryption. There is no key, no password and no integrity check behind it, and the rule
table is the only thing between the text and whoever reads it:

- Anyone who has the rule table reads the text.
- A longer text can be broken by counting letters, without the table.
- Characters without a rule pass through unchanged, and so do the length, spaces and line breaks,
  which show the shape of what you veiled.

So an obfuscated password in a file, a mail or a note is not protected the way an entry of your
database is, or a file encrypted with the file encryption tool. Keep real secrets there, and use
obfuscation for what may be seen but should not be read in passing.

## How to use it

Open **Plugins > Obfuscation > Simple Obfuscation** or **Operated Obfuscation** while a database
is open, add your rules one character at a time, and press **Obfuscate >**. The rule table lives
only in that window; **Export** saves it as a file encrypted with the key of your database, which
works when the database was opened with a key file.

The two windows differ in the way back. **Simple Obfuscation** reverses a text with
**< Disentangle**. **Operated Obfuscation** veils a text, but its own **< Disentangle** does not
work in this release; the command line's `disentangle` reverses both kinds of rule, given the same
rules that produced the text:

```
java -jar mystic-crypt-ui.jar --cli obfuscate   --rule a=x:UPPERCASE@1,4 --rule b=y --rule c=z --text "cabbage"   ->  zAyyAge
java -jar mystic-crypt-ui.jar --cli disentangle --rule a=x:UPPERCASE@1,4 --rule b=y --rule c=z --text "zAyyAge"   ->  cabbage
```

Pick replacement characters that do not occur in your texts. With the single rule `a` to `x`, the
text `fax` becomes `fxx`, and `fxx` comes back as `faa`: the `x` that was there before is turned
into an `a` as well. Check that a veiled text comes back before you throw the original away.

See [[Plugin-Obfuscation]] for both tool windows, the rule files, the settings, the command line
in full and every pitfall, and [[The-Command-Line]] for how to start the command line.
