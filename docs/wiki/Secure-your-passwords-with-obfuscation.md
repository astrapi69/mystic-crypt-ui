There are several ways to simple veil your passwords like the [ROT13](https://en.wikipedia.org/wiki/ROT13) or the [Caesar cipher](https://en.wikipedia.org/wiki/Caesar_cipher).

With mystic-crypt-ui you can create your custom obfuscation cipher that is a [Substitution cipher](https://en.wikipedia.org/wiki/Substitution_cipher) and save it to an encrypted file. 

Both kinds can be reversed. A rule can be a plain substitution, and it can carry an operation that
applies at named positions, for example `g=x:UPPERCASE@0`; the tool window and the command line
disentangle either of them with the same rules that produced them:

```
--cli obfuscate   --text "geheim" --rule "g=x:UPPERCASE@0"   ->  Geheim
--cli disentangle --text "Geheim" --rule "g=x:UPPERCASE@0"   ->  geheim
```

Older releases could only obfuscate with operations and not reverse them, which is what this page
used to say. See [[Plugin-Obfuscation]] for both tool windows and what each rule file holds.