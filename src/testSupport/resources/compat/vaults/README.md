# Vaults written by 7.2, 7.3 and 8

`vault-7.2.mcrdb` and `vault-7.3.mcrdb` were written by the application jars of the published 7.2 and 7.3 releases,
through each release's own password save path (`ApplicationXmlFileStoreWorker.saveToFileWithPassword`), for #624.
Each holds a root group and a "mykeys" group from the release's own tree factory, and one entry in each group, kept
where the release keeps entries: in the group's element (`GenericTreeElement.defaultContent`), which its tree panel reads
and writes. `ApplicationModelBean.dataOfNodes` stays as the release leaves it; in the 7.3 application jar no class but
`ApplicationModelBean` itself names it.

- "the bank" in the root group: user name, password, URL, notes with umlauts, an expiry date, an attachment
  `codes.txt` and a custom property "TOTP seed". These are all the fields 7.x has
  (`id,title,userName,password,repeat,url,notes,expirable,expires,icon,showPassword,resources,properties`).
- "the mail" in "mykeys": a password and nothing else.

The password is `TestPasswords.VAULTS_OF_7X_FIXTURE`. The entries are invented and guard nothing.

## How they were written

On 2026-11-05, with Temurin 25.0.4:

```
JAVA_HOME=~/.sdkman/candidates/java/25.0.4-tem scripts/fixtures/write-vaults-of-7x.sh <empty directory>
```

The script takes each application jar out of the installer on the release page
(`scripts/fetch-release-jar.sh <version> --without-published-checksum`) and runs
`scripts/fixtures/WriteWithAnOldRelease.java` with nothing but that jar on the class path.

| | 7.2 | 7.3 |
|---|---|---|
| installer, sha256 | `47c60fc2475c6d7d2411eed6dc7d557af36c3798beec2ea3adb83c837401a1b0` | `04312d7a474d2df8445ca81da192b26229203b9e92ac3846b1eb15cd46f88702` |
| application jar, sha256 | `699bc85b9b8a98dba765e59958b5babaf485bd3e6f3ce199f5992a04fe38eba5` | `39ce2c67ca3e833b14c04e4c356225de0d7ea9113f77dc0dc0c9fb19ac84ec9d` |
| vault, sha256 | `17727e56be8f3cdf4d41a9f4265b51ad654928b3de0e4921d5ddd96f9da4727d` | `2705a8ae548070dd37b26fdd32e371ef4643862bd9841ad8e08af8e1c4dbb2e7` |

7.x publishes no checksum, so the installers were not checked against one. The same installers downloaded on
2026-10-27 have the same sha256, and the application jars taken out of them are byte for byte the same.

## That each opens in the release that wrote it

```
printf '%s' <the password> | java -cp mystic-crypt-ui-7.3-app.jar scripts/fixtures/OpenWithAnOldRelease.java vault-7.3.mcrdb
```

prints `OPENED groups=2`, `ENTRY group=mykeys title=the mail` and `ENTRY group=root title=the bank`, for 7.2 and for 7.3
with their own jars.

# Vaults written by 8 with a key file

`vault-8-key-file.mcrdb` and `vault-8-password-and-key-file.mcrdb` were written by the application jar of the
published 8 release, for #650: the first through its key-file save path
(`ApplicationXmlFileStoreWorker.saveToFileWithPrivateKey`), the second through its save path for a password and a key
file together (`saveToFileWithPasswordAndPrivateKey`). Both hold the two groups and entries of the 7.x vaults above.

The key is `src/test/resources/pem/private.pem`, the RSA test key that has been in this repository for years; no new
key was made for them. The password of the second is `TestPasswords.VAULT_OF_8_FIXTURE`. Neither guards anything.

8 encrypted the content behind the key with plain AES, and the password layer of the second with `PBEWithMD5AndDES`
and the fixed salt (both mystic-crypt 9). 8.1.1 and later read only AES-GCM and a salted PBE, which is why both were
refused, and the sign-in called the right key file not valid.

## How they were written

On 2026-11-05, with OpenJDK 25.0.4.1, the jar taken out of the installer on the release page with
`scripts/fetch-release-jar.sh 8 --without-published-checksum`, so it was not checked against a published checksum:

```
printf '%s' <the password> | java -cp mystic-crypt-ui-8-app.jar scripts/fixtures/WriteWithAnOldRelease.java vault-8-key-file.mcrdb --key-file private.pem
printf '%s' <the password> | java -cp mystic-crypt-ui-8-app.jar scripts/fixtures/WriteWithAnOldRelease.java vault-8-password-and-key-file.mcrdb --password-and-key-file private.pem
```

| | sha256 |
|---|---|
| installer of 8 | `31275e12a3473340aeca8d98a12615b6f5b6ba2e281a7e12c00763a5f52c3c13` |
| application jar of 8 | `f093d7261faf5bd8ae5aa2fe717fa5025ce089c264debdef06cdf01b91881985` |
| `vault-8-key-file.mcrdb` | `d91359936762c132e3dab96004cfbf3b1a38711eaf5288ce32c155da44fd60e7` |
| `vault-8-password-and-key-file.mcrdb` | `83a4ea5b96f9d9007322a44975a467a84f7435269a1a5e447d66a294a3730bf4` |

## That 8 opens them and 8.1.1 does not

```
java -cp mystic-crypt-ui-8-app.jar scripts/fixtures/OpenWithAnOldRelease.java vault-8-key-file.mcrdb --key-file private.pem < /dev/null
printf '%s' <the password> | java -cp mystic-crypt-ui-8-app.jar scripts/fixtures/OpenWithAnOldRelease.java vault-8-password-and-key-file.mcrdb --password-and-key-file private.pem
```

print `OPENED groups=2` and the two entries with 8's jar, and `REFUSED AEADBadTagException: Tag mismatch` for both
with the jar of 8.1.1 (installer sha256 `1f9df0fac7dcc513995da5d8c5de011542fdc91bb09f1bf58fdee0df213e551f`, application
jar sha256 `112869045778e3cc90c7688112f35d93111e684ba9d133bb262b0c645dce6f4b`), measured on 2026-11-05.

# A vault written by 8.14

`vault-8.14.mcrdb` was written by the application jar of the published 8.14 release through its
password save path, for #864 and mystic-crypt#278: it pins that an entry property, stored as
`io.github.astrapi69.collection.pair.KeyValuePair` in the vault XML, reads back identically
whatever silly-bean version the class path carries (3.0 and 4.0 ship the identical class: same
fields, same `serialVersionUID`, measured with `javap -p -constants` on both jars on 2026-11-11).
It holds the two groups and entries of the fixtures above; the password is
`TestPasswords.VAULT_OF_8_14_FIXTURE`, and nothing in it guards anything.

Written on 2026-11-11 with Temurin 25, the jar taken out of the installer on the release page with
`scripts/fetch-release-jar.sh 8.14` (checked against the published checksum; jar sha256
`e6602c1ade2511b2f82020c8467d2760f98cdd946afeeaa3058e85098dcc5d02`):

```
printf %s <the password> | java -cp mystic-crypt-ui-8.14-app.jar scripts/fixtures/WriteWithAnOldRelease.java vault-8.14.mcrdb
```

vault sha256: `2c2f1a380c0557606ba9315e2df1d5767ec86c495d4c627d1ef8ddd4159b59eb`. The 9.0 fixtures
(#864) reuse these vaults instead of writing doubles.

# What 8.14 stores beyond plain entries

`vault-8.14-keepass-import.mcrdb` and `vault-8.14-lethenon-wallet.mcrdb` were written by the published 8.14 on
2026-11-14, with Temurin 25.0.5, for what `WhatEightFourteenStoresOpensTest` holds: an entry the release's own
"Import from KeePass" brought in, and a lethenon wallet the release's lethenon plugin put into the vault. Both hold
the two groups and entries of the 8.14 vault above as well; the password is `TestPasswords.VAULT_OF_8_14_FIXTURE`, and
the wallet is sealed with the same password. Nothing in them guards anything; the wallet is a test-network wallet
made for this file.

The application jar is the one above (sha256 `e6602c1ade2511b2f82020c8467d2760f98cdd946afeeaa3058e85098dcc5d02`).
The lethenon plugin is the zip the 8.14 installer packs (installer checked with `sha256sum -c` against the published
`.sha256`; plugin zip sha256 `ac6f91d8872c4482af37e9db0da01eb58ef95f3e2cfa847336a589f21ded30bc`, with lethenon 0.8.0),
its `classes` and `lib` jars on the class path beside the application jar.

```
printf %s <the password> | java -cp mystic-crypt-ui-8.14-app.jar scripts/fixtures/WriteWithAnOldRelease.java \
    vault-8.14-keepass-import.mcrdb --with-keepass-import src/e2eKdbx/resources/kdbx/keepassxc-2.7.10-kdbx4.kdbx fixture
printf %s <the password> | java -cp mystic-crypt-ui-8.14-app.jar:<plugin>/classes:<plugin>/lib/* \
    scripts/fixtures/WriteWithAnOldRelease.java vault-8.14-lethenon-wallet.mcrdb --with-lethenon-wallet testnet
```

- The KeePass import goes the release's way: `KdbxDatabase.read` with its KeePassJava2, then
  `KeePassTreeConverter.toTreeNode` under the root; the KeePassXC fixture `keepassxc-2.7.10-kdbx4.kdbx` brings one
  entry with one history version and a protected property.
- The wallet goes the plugin's way: `WalletVault.create` seals a new wallet with lethenon's `WalletFile`,
  `VaultWalletEntry.write` puts it on a new entry, and the host's `MarkedEntryAccess` adds it - the plugin vault access
  of #675, which only the lethenon plugin uses (8.14 and develop, measured with `git grep VaultAccess`).

vault sha256: keepass-import `2e1b684bc186405e0eb335f5768699bb45444a3dd2c86421ca25880ee2e1ff50`, lethenon-wallet
`37cd3ff37e37ea2981aa98fff85aa1d802ec435af5d841cfbace59137a36bf04`. Each opens in 8.14 itself
(`OpenWithAnOldRelease.java`: `OPENED groups=4` and `OPENED groups=2`), and the values the test expects are the ones
8.14 read from them.

# Every protection of 7.2, 7.3, 8 and 8.1.1

The eight vaults below were written for #864, by the application jars of the published releases, each through the save
path of its protection (`saveToFileWithPassword`, `saveToFileWithPrivateKey`, `saveToFileWithPasswordAndPrivateKey`).
With the four of #624 and #650 above, every one of the 12 combinations of release (7.2, 7.3, 8, 8.1.1) and protection
(password, key file, password and key file) has a vault here, the proof gate of the 9.0 format
(`docs/decisions/vault-content-format-9.0-2026-11-10.md`, section 7); `VaultsOfEveryVariantOpenTest` opens these eight.
Each holds the two groups and entries of the 7.x vaults above.

The key is `src/test/resources/pem/private.pem`, the RSA test key the vaults of 8 with a key file have; the password of
those with a password is `TestPasswords.VAULTS_OF_EVERY_VARIANT_FIXTURE`. Nothing in them guards anything.

## How they were written

In GitHub Actions, not on a machine (AG-021): run
[46120196142](https://github.com/astrapi69/mystic-crypt-ui/actions/runs/46120196142) of
`.github/workflows/write-vaults-of-old-releases.yml`, on `861538a7ee472d1c49e7c4b7beecf46514257e78`, on 2026-11-28 with
Temurin 25.0.5.1+2 (the run's `java -version`), started with

```
gh workflow run write-vaults-of-old-releases.yml --ref test/compat-fixtures-of-every-release
```

It runs `scripts/fixtures/write-vaults-of-every-variant.sh <empty directory>`, which downloads each installer anew,
requires its sha256 and that of the application jar taken out of it to be the ones recorded above for 7.2, 7.3, 8 and
8.1.1 (the run's log shows all eight), writes the vaults with `WriteWithAnOldRelease.java` and nothing but the release's
jar on the class path, and uploads them with their sha256 as the artifact `old-release-vaults`. The files here are that
artifact's, unchanged (`gh run download 46120196142 -n old-release-vaults`, then `sha256sum -c SHA256SUMS`). The run
writes new files each time: the salt and the entry ids are new.

| vault | written by | protection | sha256 |
|---|---|---|---|
| `vault-7.2-key-file.mcrdb` | 7.2 | key file | `358c31bfe73758a0cfdb919b7378dbc4ebf162a2e24fa32fb6926168b81025cc` |
| `vault-7.2-password-and-key-file.mcrdb` | 7.2 | password and key file | `e983ff5ee45f6b5550193c50825305ae04c11705d6683d3f5fbc634b40e8e0d9` |
| `vault-7.3-key-file.mcrdb` | 7.3 | key file | `a9f10c35321eac340c80146865f09ef9858d3a88c7cff26464c74ed7d6435b6e` |
| `vault-7.3-password-and-key-file.mcrdb` | 7.3 | password and key file | `67bcf33fc27506f15a95b631d2e8c82fc46e3e9e0da617f44cbe2e3009e5e9f3` |
| `vault-8.mcrdb` | 8 | password | `bd07d209cc58ae6bd18e581680ca6d32f0cce0aa344758718cdede6452ebe803` |
| `vault-8.1.1.mcrdb` | 8.1.1 | password | `9f8c9afcca2f6859f295820d158c4f1d78f6e64cf0d9def1aaa7ddd575902252` |
| `vault-8.1.1-key-file.mcrdb` | 8.1.1 | key file | `ba5ccbc76af29257823fdb17bee4ae6276b2f747902a25953296392ff2bfe07f` |
| `vault-8.1.1-password-and-key-file.mcrdb` | 8.1.1 | password and key file | `a5eca3bed188d26fd5eb29708c750e7ddb364ac0ba4c27bb2a2927023a882f71` |

## That each opens in the release that wrote it, but one

The same run opens a copy of each vault with the release that wrote it (`OpenWithAnOldRelease.java`) and requires the
entries it reads to be the ones the release wrote: seven print `OPENED groups=2` and the two entries.

8 does not open the vault it wrote with a password alone (and, inferred from the path below, no vault with a password
alone): it prints
`REFUSED NoSuchMethodError: 'java.lang.String io.github.astrapi69.file.read.ReadFileExtensions.readFromFile(java.io.File)'`,
and the script requires exactly that line. 8's password path reads the decrypted file through xstream-extensions'
`XmlFileToObjectExtensions.toObject`, which calls `ReadFileExtensions.readFromFile(File)`, and the `ReadFileExtensions`
packed into 8's application jar has `fromFile` and no `readFromFile` (`javap` on that jar, 2026-11-28). The decryption
writes the vault's xml into a file beside it, `vault-8.decrypted`, which 8 deletes after reading and which stays when
the read fails; that is why the script opens a copy in a directory of its own. Measured on the jar run in GitHub
Actions; that the installed 8 behaves the same is an inference from the installer packing this jar. This build reads
that vault, `VaultsOfEveryVariantOpenTest`.
