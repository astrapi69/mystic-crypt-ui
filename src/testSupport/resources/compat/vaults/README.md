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
