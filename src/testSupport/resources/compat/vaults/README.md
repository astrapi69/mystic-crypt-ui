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
