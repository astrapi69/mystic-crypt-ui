[![Java CI with Gradle](https://github.com/astrapi69/mystic-crypt-ui/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/astrapi69/mystic-crypt-ui/actions/workflows/gradle.yml)
# Overview

This swing application provides the following features:

 * Save passwords with file attachments in a tree manner
 * check the checksum of downloaded files with the most common algorithms
 * Creation of private and public keys with 1024, 2048 and 4096 bit length
 * Save the created private and public keys
 * Obfuscate text with specified map that can be exported and imported
 * Entries in the existing obfuscation map can be edited, deleted and new entries can be added

# Install

Windows, Linux and Mac users can download and install it with
[izpack installer](https://sourceforge.net/projects/mysticcrypt/files/5.1/installer.jar/download).
Note: for unix users dont forget to set the execute bit for start the jar file.
[![Download mystic-crypt](https://a.fsdn.com/con/app/sf-download-button)](https://sourceforge.net/projects/mysticcrypt/files/latest/download)
[![Download mystic-crypt](https://img.shields.io/sourceforge/dm/mysticcrypt.svg)](https://sourceforge.net/projects/mysticcrypt/files/latest/download)

# Build from source

For users that want to build it from the source files can clone this git repository. It is a gradle project, so you will
need gradle and a JDK installation.

Then compile the project:

```
./gradlew build
```

The jar file is now generated and is located in the path ```/build/libs/mystic-crypt-ui-current-version.jar``` and ready
for execution.

Note:
Replace *current-version* with the current version of the project.

# Installer tool

Here is the installer tool that is used for deploy the final application.

* [izpack](http://izpack.org/) IzPack is a widely used tool for packaging applications on the Java™ platform.

For more information about how to create the izpack installer see the [wiki izpack section](https://github.com/astrapi69/mystic-crypt-ui/wiki/How-to-create-izpack-installer-with-gradle)

## License

The source code comes under the liberal MIT License.

## Want to Help and improve it? ###

The source code for mystic-crypt-ui are on GitHub. Please feel free to fork and send pull requests!

Create your own fork of [astrapi69/mystic-crypt-ui/fork](https://github.com/astrapi69/mystic-crypt-ui/fork)

To share your changes, [submit a pull request](https://github.com/astrapi69/mystic-crypt-ui/pull/new/develop).

Don't forget to add new units tests on your changes.

## Contacting the Developer

Do not hesitate to contact the mystic-crypt-ui developers with your questions, concerns, comments, bug reports, or feature requests.
- Feature requests, questions and bug reports can be reported at the [issues page](https://github.com/astrapi69/mystic-crypt-ui/issues).

## Note

No animals were harmed in the making of this application.

# Donate

If you like this application, please consider a donation through
<a href="https://flattr.com/submit/auto?fid=r7vp62&url=https%3A%2F%2Fgithub.com%2Flightblueseas%2Fmystic-crypt-ui" target="_blank">
<img src="http://button.flattr.com/flattr-badge-large.png" alt="Flattr this" title="Flattr this" border="0">
</a>
