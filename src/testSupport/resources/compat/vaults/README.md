# Vaults written by 7.2 and 7.3

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
