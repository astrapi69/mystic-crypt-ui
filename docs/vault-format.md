# The database format

How a mystic-crypt-ui database (`*.mcrdb`) is laid out on disk, what protects it, what is still
read from older releases, and the rule that governs changes to any of it.

The code this document describes:

| What | Where |
|---|---|
| The format of a password-protected database | `src/main/java/io/github/astrapi69/mystic/crypt/app/file/xml/PasswordVaultFormat.java` |
| The passphrase construction it is built on | `src/main/java/io/github/astrapi69/mystic/crypt/crypto/PassphraseBox.java` |
| Reading a database (all sign-in variants) | `src/main/java/io/github/astrapi69/mystic/crypt/app/file/xml/ApplicationXmlFileReader.java` |
| Writing a database (all sign-in variants) | `src/main/java/io/github/astrapi69/mystic/crypt/app/file/xml/ApplicationXmlFileStoreWorker.java` |
| The AEAD primitive | `KeyCommittingAeadEncryptor` in the mystic-crypt library (version pinned in `gradle/libs.versions.toml`) |

## There is more than one format

The content of a database is always the same thing first: the `ApplicationModelBean` serialized to
XML by XStream (`ObjectToXmlExtensions.toXml`). What happens to that XML afterwards depends on how
the user signs in.

`SignInType.toSignInType(MasterPwFileModelBean)` decides on writing and
`PasswordType.resolve(withMasterPw, withKeyFile)` decides on reading, both from the two flags
`isWithMasterPw()` and `isWithKeyFile()` on `MasterPwFileModelBean`:

| Sign-in variant | Written by | On-disk shape |
|---|---|---|
| Master password only | `ApplicationXmlFileStoreWorker.saveToFileWithPassword` | MCRDB2, described below |
| Private key file only | `saveToFileWithPrivateKey` | serialized `AesRsaCryptModel`, no marker |
| Password and private key file | `saveToFileWithPasswordAndPrivateKey` | serialized `AesRsaCryptModel`, no marker |

Only the master-password variant has a versioned, marked format. That is the one called "the vault
format" in this repository, and the rest of this page is mostly about it; the key file variants get
their own section at the end.

Note that the file does not record which variant produced it. The routing comes from the sign-in
dialog's model, not from the bytes. A key-file database therefore does not carry the MCRDB2 marker,
and reading it through the password-only path will not yield the XML.

## MCRDB2: the byte layout

`PasswordVaultFormat.encrypt` hands the UTF-8 bytes of the XML to
`PassphraseBox.encrypt(MAGIC, plaintext, password)`. What lands on disk:

```
offset  length   field
------  -------  ------------------------------------------------------------
     0        6  marker, the ASCII bytes "MCRDB2"
     6       16  salt, drawn fresh from SecureRandom on every single save
    22        4  iteration count, big endian int (ByteBuffer.putInt)
------  -------  header ends at 26; everything above is the AEAD associated data
    26       12  GCM nonce (IV)
    38        n  AES-GCM ciphertext of the XML, n = length of the UTF-8 plaintext
  38+n       16  GCM authentication tag (128 bit), appended by Cipher.doFinal
  54+n       32  key commitment tag, Blake2b
```

Fixed overhead is therefore 86 bytes: 6 + 16 + 4 + 12 + 16 + 32. Encrypting an 8-byte plaintext
produces a 94-byte file.

The three header fields come from `PassphraseBox`:

```java
public static final int SALT_LENGTH = 16;
public static final int ITERATIONS = 600_000;
public static final int KEY_LENGTH_BITS = 256;

public static int headerLength(final byte[] magic)
{
    return magic.length + SALT_LENGTH + Integer.BYTES;
}
```

`PasswordVaultFormat.SALT_LENGTH` and `PasswordVaultFormat.ITERATIONS` are aliases of those
constants, so the format class has no numbers of its own.

The three payload fields come from `KeyCommittingAeadEncryptor` in the mystic-crypt library, which
documents its output as `IV (12 bytes) || Ciphertext || CommitmentTag (32 bytes)` and uses a
128-bit GCM tag, which the JDK appends to the ciphertext inside `doFinal`. The commitment tag is
`Blake2b(commitmentKey || iv || associatedData)` where the commitment key is itself
`Blake2b(mainKey)`, and it is fed into the cipher's associated data as well as being written out.
It is what makes the construction key-committing: on decryption the tag is recomputed and compared
before the cipher runs, so a ciphertext is bound to one specific key.

### The header is authenticated

The whole 26-byte header is passed as associated data to both `encrypt` and `decrypt`:

```java
byte[] header = ByteBuffer.allocate(headerLength(magic)).put(magic).put(salt)
    .putInt(ITERATIONS).array();
byte[] payload = new KeyCommittingAeadEncryptor(deriveKey(passphrase, salt, ITERATIONS))
    .encrypt(plaintext, header);
return ByteBuffer.allocate(header.length + payload.length).put(header).put(payload).array();
```

Marker, salt and iteration count therefore cannot be edited without the file refusing to open. Three
properties follow, and each is pinned by a test in
`src/test/java/io/github/astrapi69/mystic/crypt/app/file/xml/PasswordVaultFormatTest.java`:

- a wrong password fails as a wrong password, immediately and always
  (`aWrongPasswordDoesNotOpenTheNewFormat`),
- a file changed by so much as one bit refuses to open instead of opening with changed content
  (`theNewFormatRefusesAFileThatWasTamperedWith`, `theHeaderIsPartOfWhatIsAuthenticated`),
- the same database saved twice produces different bytes, because the salt and the nonce are fresh
  each time (`everySaveDrawsItsOwnSalt`).

### Reading it back

`PassphraseBox.decrypt` slices the header back apart and, crucially, derives the key from the salt
and the iteration count **found in the file**, not from today's constants:

```java
byte[] header = Arrays.copyOf(content, headerLength);
byte[] salt = Arrays.copyOfRange(header, magic.length, magic.length + SALT_LENGTH);
int iterations = ByteBuffer.wrap(header, magic.length + SALT_LENGTH, Integer.BYTES).getInt();
byte[] payload = Arrays.copyOfRange(content, headerLength, content.length);
// the header is the associated data, so a changed salt or iteration count breaks the tag
return new KeyCommittingAeadEncryptor(deriveKey(passphrase, salt, iterations))
    .decrypt(payload, header);
```

Two guard clauses come first and produce their own messages: content that does not start with the
marker ("this is not something this application encrypted: the marker is missing") and content whose
length is not greater than the header ("truncated: the marker is there but there is no content
behind it"). Below that, `KeyCommittingAeadEncryptor` rejects a payload shorter than
nonce + commitment tag + 1 byte.

`PasswordVaultFormat.decrypt(File, String)` reads the whole file into memory with
`Files.readAllBytes`. Databases are small; nothing streams.

## Key derivation

| Parameter | Value | Source |
|---|---|---|
| Algorithm | `PBKDF2WithHmacSHA256` | `PassphraseBox.KEY_DERIVATION_ALGORITHM` |
| Derived key length | 256 bits, used as an AES key | `PassphraseBox.KEY_LENGTH_BITS` |
| Salt | 16 bytes from `new SecureRandom()`, per save | `PassphraseBox.SALT_LENGTH` |
| Iterations for a file written today | 600,000 | `PassphraseBox.ITERATIONS` |
| Iterations used when reading | whatever the file records | `PassphraseBox.decrypt` |

```java
public static SecretKey deriveKey(final String passphrase, final byte[] salt,
    final int iterations) throws Exception
{
    PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray(), salt, iterations,
        KEY_LENGTH_BITS);
    try
    {
        byte[] keyBytes = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
            .generateSecret(keySpec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    finally
    {
        // the spec holds a copy of the passphrase; there is no reason to leave it lying around
        keySpec.clearPassword();
    }
}
```

Two deliberate choices worth knowing before touching this method:

- The salt comes from `new SecureRandom()`, not from `SecureRandom.getInstanceStrong()`. The source
  comment gives the reason: on Linux the strong instance can resolve to the blocking source, and
  encrypting must never hang waiting for entropy.
- `keySpec.clearPassword()` runs in a `finally` block, so the copy of the passphrase held by the
  spec does not outlive the derivation.

Because the cost is written into every file, raising it is a one-line change to
`PassphraseBox.ITERATIONS`: new saves get the higher cost, existing files keep opening with the cost
they recorded. Both users of `PassphraseBox` inherit that change at once, so the file-crypt plugin's
format is affected too (see below).

## Why there is a marker, and how a new version would be added

The marker exists so that a file can say what it is. Two things depend on that:

1. **Format detection.** `PasswordVaultFormat.isCurrentFormat(byte[])` delegates to
   `PassphraseBox.hasMagic`, a prefix comparison: content that is non-null, at least as long as the
   marker, and begins with those bytes. It is a prefix test, so `"MCRDB2andmore"` matches while
   `"MCRDB"` and `"MCRDB1xxxx"` do not. `decrypt` uses it as the only branch it takes:

   ```java
   byte[] fileContent = Files.readAllBytes(applicationFile.toPath());
   if (!isCurrentFormat(fileContent))
   {
       return decryptLegacy(applicationFile, password);
   }
   return new String(PassphraseBox.decrypt(MAGIC, fileContent, password), StandardCharsets.UTF_8);
   ```

2. **Not confusing two formats.** The marker is part of the associated data, so a file cannot be
   relabelled from one format into another without the authentication failing.

The `2` in `MCRDB2` is the version. What requires a new one and what does not:

- **No new marker needed:** raising `ITERATIONS`. The count is a field in the file, and reading uses
  the file's value. `aFileCanBeReadBackWithAnIterationCountOtherThanTodaysDefault` is the test that
  pins this.
- **New marker needed:** any change to the construction itself. A different KDF, a different cipher,
  a different key length, another header field, a different field order.

Adding one means, concretely:

1. A new `MAGIC` constant. The header arithmetic follows automatically, because
   `PassphraseBox.headerLength(magic)` is derived from `magic.length` rather than hard-coded, and
   both encrypt and decrypt take the marker as a parameter. A marker of a different length is
   therefore not a special case.
2. Writing switches to the new marker only. `PasswordVaultFormat.encrypt` is the single write path;
   there is deliberately no way to write an older format again.
3. Reading gains one branch and loses none: try the new marker, then MCRDB2, then fall through to
   the legacy path. Every format ever written stays readable, or the update loses databases.
4. A round trip through the running application proves it, not unit tests alone. See the last
   section.

## The legacy format, and how a database migrates

Every release up to and including 8.1.1 wrote a password-protected database like this, and files in that shape are
still opened:

| Property | Value |
|---|---|
| Algorithm | `SunJCEAlgorithm.PBEWithMD5AndDES` |
| Salt | `CompoundAlgorithm.SALT`, a constant 8-byte array in a library published on Maven Central |
| Iterations | `CompoundAlgorithm.ITERATIONCOUNT`, which is 19 |
| Marker | none |
| Authentication | none |

The salt being a published constant is the core of the problem: it was the same eight bytes on every
installation in the world, so one precomputation works against everybody's file. Together with a
56-bit cipher and 19 iterations there was very little between the file and whoever holds it. And
because the cipher authenticates nothing, a wrong password produced plausible-looking rubbish rather
than an error, which is why the legacy test
(`theOldFormatCannotEvenTellAWrongPasswordApart`) can only assert that the content does not come
back, not that decryption throws.

`decryptLegacy` pins those parameters by hand:

```java
// salt and iteration count MUST be pinned exactly as they were when the file was written:
// without them the decryptor draws its own random salt, which can never match
CryptModel<Cipher, String, String> legacyModel = CryptModel
    .<Cipher, String, String> builder().key(password)
    .algorithm(SunJCEAlgorithm.PBEWithMD5AndDES).salt(CompoundAlgorithm.SALT)
    .iterationCount(CompoundAlgorithm.ITERATIONCOUNT).build();
File decrypted = new PBEFileDecryptor(legacyModel).decrypt(applicationFile);
try
{
    return ReadFileExtensions.fromFile(decrypted);
}
finally
{
    DeleteFileExtensions.delete(decrypted);
}
```

Pinning is not optional: the library's PBE helpers now generate a fresh random salt per call, so
without the explicit `salt` and `iterationCount` the decryptor would never reproduce the key the old
file was written with.

`PBEFileDecryptor` only works on files, so this path briefly writes the decrypted XML next to the
encrypted one, as `<basename>.decrypted` in the same directory (`legacy.mcrdb` becomes
`legacy.decrypted`). The `finally` block removes it. This is the one place where a decrypted
database touches the disk, and it is walked at most once per database, because the next save writes
MCRDB2.

**The migration path is "open it and save it".** There is no migration command and no conversion
dialog. `decrypt` reads whichever format the file is in, `saveToFileWithPassword` always writes
MCRDB2, so a database moves formats by being used. The user-visible consequence is that a database
stays in the old format until it is saved once.

Note the trap that this design contains and that has already been sprung once here: the database
carries its own path inside its XML and saves itself back to that path, so a migration bug can write
the new file somewhere other than where the old one was read from.

## What the key file variants do differently

Neither key file variant uses `PasswordVaultFormat`, `PassphraseBox`, PBKDF2 or a marker. They use
the mystic-crypt library's public-key encryptors, and the shape on disk is a Java-serialized
`AesRsaCryptModel` produced by `SerializationUtils.serialize`, holding two fields:

- `encryptedKey`: a freshly generated 128-bit AES key
  (`SecretKeyFactoryExtensions.newSecretKey(AesAlgorithm.AES.getAlgorithm(), 128)`), encrypted with
  the public key, default transformation `RSA/ECB/OAEPWithSHA1AndMGF1Padding`.
- `symmetricKeyEncryptedObject`: nonce followed by the AES-GCM ciphertext of the payload, with
  `MysticSymmetricAlgorithm.AES_GCM_NO_PADDING` as the transformation.

Differences that matter when working on these paths:

- **The public key is derived, not stored.** Both write paths take the private key from the key file
  (or from `modelObject.getPrivateKeyInfo()` when the session already holds one) and compute the
  public key with `PrivateKeyExtensions.generatePublicKey(privateKey)`.
- **Password plus key file is two layers, and the inner one is not this format.** The XML is first
  run through `PasswordStringEncryptor` (library `pw` package: NFC-normalized password, an 8-byte
  random salt per call, an AES-CBC based PBE at 65,536 iterations, Base64-encoded, salt
  prepended), and that Base64 string is then encrypted to the public key. Reading undoes it in the
  opposite order. The transformation is `CompoundAlgorithm.PBE_WITH_SHA1_AND_128BIT_AES_CBC_BC`,
  whose algorithm string is `PBEWITHSHA1AND128BITAES-CBC-BC`. It exists only in the Bouncy Castle
  provider, which is why the format tests register that provider before touching anything.
- **Key file reading differs between the two variants.** The key-file-only read path checks
  `PemObjectReader.isPemObject(keyFile)` and reads PEM or DER accordingly; the password-plus-key-file
  path calls `PrivateKeyReader.readPemPrivateKey` unconditionally.
- **The private-key write path used to touch the disk in the clear.** `saveToFileWithPrivateKey`
  wrote the plaintext XML to the application file and overwrote the same path with the encrypted
  bytes on the next line, so anything that came in between got the whole database as readable XML
  (#29). It now encrypts in memory and writes once, which is what the password path had always
  done:

  ```java
  // straight from memory into the encrypted file: the xml is never written anywhere in the
  // clear, not even into a temporary file that is deleted afterwards - deleting is not
  // wiping, and a temporary directory is a poor place for a decrypted password database
  byte[] fileContent = RuntimeExceptionDecorator
      .decorate(() -> PasswordVaultFormat.encrypt(xml, password));
  ```

- **Nothing about these variants is versioned.** No marker, no recorded cost, no associated data. A
  change to them cannot be detected from the file, which is an argument for giving them a marked
  format rather than for leaving them as they are.

## The same construction, one more user

`PassphraseBox` is deliberately generic in its marker, and has a second user: the file-crypt plugin,
`plugins/file-crypt-plugin/src/main/java/io/github/astrapi69/mystic/crypt/plugin/filecrypt/FileCryptSupport.java`,
whose files start with the ASCII marker `MCFILE1` and otherwise have exactly the layout above (a
7-byte marker instead of a 6-byte one, so its header is 27 bytes). Encrypted files get the extension
`.mcenc` by default.

Practical consequence: a change to `PassphraseBox` changes both formats at once. Anything done there
has to be proven for the database and for the plugin.

## Any change here is proven by a real round trip

This is a binding rule, not a suggestion. It is written down in `.claude/rules/quality-checks.md`
under "Round-trip acceptance for vault/format changes (MANDATORY)":

> Any change to the vault format (`PasswordVaultFormat`, MCRDB2, migration paths) or to file
> encryption is proven by a REAL round-trip, not only unit tests: create/encrypt with real data,
> reopen/decrypt, verify content - through the real app path (e2e test or manual run), including the
> legacy-migration path when touched.

The reason it exists, from the same file: five consecutive "fixed" backup releases in a sibling
project had green unit tests and no working round trip, because synthetic fixtures miss schema drift
and serialization edge cases. It was then confirmed here: the 2026-08-25 legacy-migration bug, which
read `legacy.mcrdb` and saved `source.mcrdb`, survived a green unit test suite. Unit tests over the
format class exercise `encrypt` and `decrypt`; they do not exercise the path the application takes
from the sign-in dialog through the model to the file it decides to write.

Where the two levels live:

| Level | Test |
|---|---|
| Format, in isolation | `src/test/java/io/github/astrapi69/mystic/crypt/app/file/xml/PasswordVaultFormatTest.java` (round trip, legacy read, migration on save, fresh salt per save, wrong password, tampering, header authentication, truncation, recorded iteration count, marker recognition) |
| Password round trip through the UI | `src/test/java/io/github/astrapi69/mystic/crypt/ui/SaveAndReopenDatabaseUiTest.java` |
| Legacy migration through the UI | `src/test/java/io/github/astrapi69/mystic/crypt/ui/LegacyDatabaseMigrationUiTest.java` |
| Key file variant through the UI | `src/test/java/io/github/astrapi69/mystic/crypt/ui/CreateDatabaseWithKeyFileUiTest.java` |

`LegacyDatabaseMigrationUiTest` is the shape to copy for a format change: it builds a fixture in the
old format, asserts the fixture really is in the old format ("otherwise this test proves nothing"),
drives the real sign-in dialog, adds a node, saves through the File menu, waits for the file on disk
to carry the new marker, shuts the application down, signs in again and checks the node is still
there.

Run them with `make test` (everything) or `make test-e2e` (the `io.github.astrapi69.mystic.crypt.ui.*`
suites). The e2e suite needs the Xvfb harness described in `.claude/rules/lessons-learned.md`;
running it against a live `:0` display hangs.

`PassphraseBox` itself has no test class of its own. It is covered through `PasswordVaultFormatTest`
and through the file-crypt plugin's tests, which is worth remembering when changing it: neither
suite is named after it.
