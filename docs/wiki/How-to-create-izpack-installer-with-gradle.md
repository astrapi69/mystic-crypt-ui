
# Overview

Before you create the IzPack installer file, ensure that you have followed the preconditions in the [DEPLOYMENT-INFO](https://github.com/astrapi69/mystic-crypt-ui/blob/develop/DEPLOYMENT-INFO.md). 

1. Set the property `createIzPackInstaller` to `true` in the project `gradle.properties` file.
2. Execute the `clean` task to delete the `mystic-crypt-ui/build` directory.
3. Run the `withAllDependenciesJar` task to create the jar with the suffix `*-all.jar`.
4. Execute the `jar` task using Gradle. This process generates the jar files in the `mystic-crypt-ui/build/libs` directory and the signed jar files in the `mystic-crypt-ui/build/signed` directory.
5. Run the `izPackCreateInstaller` task. This automatically retrieves the signed jar file and generates the installation jar file in the project's distributions directory `mystic-crypt-ui/build/distributions`. The signed installation jar file can now be deployed to any OS.

There is a new Gradle task, `createIzPackInstallerFromSignedJar`, located in the `mystic-crypt-ui/gradle/packaging.gradle` file, that reproduces the above behavior. If the flag `createIzPackInstaller` is set to `true`, an installer jar will automatically be created in the `mystic-crypt-ui/build/distributions` directory.

For more details, refer to the IzPack task in the `build.gradle` file or visit the [Gradle IzPack Plugin page](https://github.com/bmuschko/gradle-izpack-plugin).

# Notes

Since version 6, the `build.gradle` file is split into several sections. One of them is the `izpack.gradle` file, which contains the IzPack task. In the `packaging.gradle` file, tasks for generating the jar, signed jar, and `withAllDependenciesJar` are located.

# Troubleshooting

If no installation jar file is generated, ensure that the `createIzPackInstaller` property is set to `true`.

If you encounter the following error:

```shell
Execution failed for task ':jar'.
> Could not get unknown property 'release.mystic-crypt.key.alias' for root project 'mystic-crypt-ui' of type org.gradle.api.Project.
```

This error means that the property `release.mystic-crypt.key.alias` for the keystore is not set. In the project `gradle.properties`, the following properties for the keystore should be defined:

```
##############################
# Keystore Properties Keys   #
##############################
mysticCryptStoreFile=release.mystic-crypt.store.file
mysticCryptStoreType=release.mystic-crypt.store.type
mysticCryptStorePassword=release.mystic-crypt.store.password
mysticCryptKeyPassword=release.mystic-crypt.key.password
mysticCryptKeyAlias=release.mystic-crypt.key.alias
```

Follow the same pattern as described in this [article](https://asterios-raptis.medium.com/protect-sensible-data-in-your-build-gradle-file-27c908293711).

Additionally, you may encounter the error:

```
> jarsigner returned: 1
```

This can occur if the path to the keystore is not properly set in the `~/.gradle/gradle.properties`.

# IzPack Migration to IzPack 5

The project has migrated to the new major version, IzPack 5.x.

Versions before 6.x used the old IzPack 4.x.
