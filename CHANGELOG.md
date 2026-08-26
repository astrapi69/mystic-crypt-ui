## Change log
----------------------

Version 8.2-SNAPSHOT
-------------

ADDED:

- four new internal plugins, bringing the count to thirteen (the Makefile "plugins:" target is the canonical list): a key store manager, a file and text encryptor, a post-quantum signature tool, and a secret splitter based on Shamir secret sharing
- new command line: `--cli` hands over to the mystic-crypt library's picocli root command, and every plugin may contribute its own commands to it
- per-plugin settings: each plugin declares its own configuration with defaults and descriptions, editable in the settings dialog, stored one properties file per plugin
- key store manager: create, open and fill PKCS12, JKS and JCEKS stores, import keys and certificates the tool did not make itself, and read what a store holds
- file and text encryptor: a passphrase encrypts a file or a piece of text, asked for twice when encrypting so that a typo cannot silently cost the content
- post-quantum signature tool: sign and verify with ML-DSA and SLH-DSA, and with the classical RSA, ECDSA and Ed25519, using key files that already exist rather than only freshly generated ones
- secret splitter: split a secret into shares of which a chosen number are needed to put it back together
- password hashing gained bcrypt and scrypt next to Argon2id and PBKDF2, with the algorithm detected from the stored hash when verifying
- checksums gained message authentication: a keyed HMAC answers whether a file was changed by someone without the key, next to the digest that answers whether it was changed at all
- key conversion tool: a key or certificate file is examined, told what it is, and offered the conversions that make sense for it (PEM, DER, PKCS#1, PKCS#8, X.509)
- key generation says which curve a key is on and which format it is written in
- the certificate wizard's extensions do what they say: basic constraints, key usage and subject alternative names are built from readable text and land in the certificate
- mutation testing (PIT) with a workflow, and the tests the first run showed were missing

CHANGED:

- every panel holds its state in a model object with its components bound to it, so a panel's state is readable at any moment instead of living scattered across the widgets
- the tree hides the root when only one root may exist, so an imported KeePass database no longer appears as a subtree under a node the user cannot remove
- tree nodes can be moved: up, down, and into another node, with the moves that would break the tree refused rather than half-performed
- the installer registers a desktop entry that works, and the install instructions name the Java 25 requirement

FIXED:

- the key store command is taken from the library instead of being duplicated in the application
- signing with an existing key file failed for keys on named elliptic curves, because the JDK provider refused what Bouncy Castle had generated; classical signing and key reading now go through Bouncy Castle throughout
- a self-signed certificate written by the key store tool used SHA256withRSA where RFC 4055 requires SHA256withRSAandMGF1

SECURITY:

- a database protected by a password alone was encrypted with PBEWithMD5AndDES using a salt built into the source and 19 iterations - a 56-bit cipher with no per-file salt, which a modern machine takes apart. It is now AES-GCM with a 256-bit key derived by PBKDF2-HMAC-SHA256 over 600,000 iterations and a random 16-byte salt per file, marked "MCRDB2" so the format is recognisable. Databases in the old format are still read and are written back in the new one, so opening and saving an existing database migrates it

ADDED:

- new "Key Exchange" tool for an exchange between two people, where each side holds only its own half: one side hands out a public key, the other encapsulates against it and sends back a handshake, and both arrive at the same shared secret without either private key ever travelling
- the shared secret encrypts a message, and both sides can compare an eight character fingerprint to check they hold the same one
- supported algorithms: ML-KEM 512, 768 and 1024, X25519, and the hybrid of X25519 with ML-KEM-768, which stays secure as long as either half does
- new command line side of the exchange with `--cli keyx new|send|receive`, one run per step, so the two sides can be two machines
- new setting for the algorithm the exchange starts with, separate from the one the demo uses

Version 8.1.1
-------------

CHANGED:

- the released artifacts are signed with a certificate that identifies the project (CN=Asterios Raptis, O=mystic-crypt-ui, 4096-bit RSA, SHA384withRSA) instead of the previous placeholder certificate CN=Test subject

Version 8.1
-------------

ADDED:

- new plugin system based on pf4j: nine internal plugins that contribute their tools to the "Plugins" menu
- new internal plugin for simple and operated obfuscation
- new internal plugin for verifying checksums
- new internal plugin for converting der files to pem
- new internal plugin for the output console
- new internal plugin for key generation, extended with the modern algorithms X25519, X448, ML-KEM-768 and ML-DSA-65
- new internal plugin for creating X.509 certificates through the certificate wizard
- new internal plugin for hashing and verifying passwords with Argon2id or PBKDF2
- new internal plugin demonstrating ML-KEM and hybrid X25519+ML-KEM key encapsulation
- new internal plugin "Menu Designer" for viewing, editing, applying and saving the application menu as xml
- new user defined menu layout: a menubar.xml in the configuration directory is applied on start
- new settings dialog with a plugins tab (enable, disable, install from zip) and a general tab for the look and feel
- new feature Search, Lock Workspace and Save As, which were unwired stubs before
- new command line interface shipped with the installer as an optional pack

CHANGED:

- the certificate wizard creates and saves a certificate now instead of exiting the application
- menu layout follows the common conventions: Look and Feel and View Mode moved under a new View menu, Help stays last
- the sign-in dialog submits on Enter in the master password field
- each plugin groups its items under its own submenu of the "Plugins" menu
- update of mystic-crypt dependency to new major version 11.0.0, which carries the command line interface
- update of crypt-api dependency to new major version 10.1
- update of crypt-data dependency to new major version 11.1
- update of menu-action dependency to new major version 5.1
- update of model-data dependency to new version 3.2.1
- update of swing-tree-component dependency to new version 3.2
- update of gen-tree dependency to new major version 11.1
- unified all Bouncy Castle artifacts on the jdk18on family, replacing the jdk15on ones
- removed the unused dependencies swingx-all, sqlite-jdbc, jackson-databind, imgscalr-lib, batik-codec, batik-transcoder, jxlayer, swing-layout, swing-worker and bcprov-ext

FIXED:

- fixed a ConcurrentModificationException when closing the application with plugins started
- fixed needless look-and-feel churn on start

Version 8
-------------

ADDED:

- new format of database file not compatible with version 7.x

CHANGED:

- update of gradle to new version 8.8

Version 7.3
-------------

ADDED:

- new feature for an auto type from the mystic crypt entry
- new feature for save before close
- new feature for duplicate mystic crypt entry in table

CHANGED:

- update of gradle to new patch version 8.2.1
- update of gradle plugin dependency io.freefair.gradle:lombok-plugin to new minor version 8.1.0
- update of dependency commons-codec to new minor version 1.16.0
- update of mystic-crypt dependency to new minor version to 8.1
- update of crypt-api dependency to new minor version to 8.6
- update of crypt-data dependency to new minor version to 8.5

Version 7.2
-------------

ADDED:

- new popup menu for duplicate an existing tree node

CHANGED:

- removed spring-boot dependencies

Version 7.1
-------------

ADDED:

- new button for show the password in the mystic entry dialog
- new button for generate a new password in the mystic entry dialog

CHANGED:

- the new dialog is synchronized now with the sign in dialog

Version 7
-------------

ADDED:

- new dependency tree-api in minor version 1.2
- new tab for save properties in the mystic entry entity

CHANGED:

- update of gradle plugin dependency com.github.ben-manes.versions.gradle.plugin to new version 0.46.0
- update of dependency gen-tree to new minor version 7.4

Version 6.1
-------------

ADDED:

- new tab in the mystic crypt entry for adding file attachments

CHANGED:

- update of izpack version from 4.x to new 5.x
- update of dependency spring-boot to new version 2.7.4
- update of io.spring.gradle:dependency-management-plugin to new version 1.0.14.RELEASE

Version 6
-------------

ADDED:

- new dependency 'io.github.astrapi69:state' in version 6

CHANGED:

- update of gradle to new version 7.5.1
- update of dependency spring-boot to new version 2.7.3
- update of dependency lombok to new version 1.18.24
- update of com.github.ben-manes.versions.gradle.plugin to new version 0.42.0
- update of io.spring.gradle:dependency-management-plugin to new version 1.0.11.RELEASE
- update of com.bmuschko:gradle-izpack-plugin to new version 3.2
- update of mystic-crypt dependency version to 8
- update of crypt-api dependency version to 8.3
- update of crypt-data dependency version to 8.2

Version 5.4
-------------

ADDED:

- gradle as build system
- simple checksum feature added
- izpack installer introduced for create installer from application

CHANGED:

- update to new group package name io.github.astrapi69
- update of model-object version to 1.8
- update of model-type-safe version to 1.8
- extracted project properties to gradle.properties

Version 5.3
-------------

ADDED:

- simple obfuscation feature added
- integration of spring-boot
- new look and feel menu items


CHANGED:

- update of parent version to 2.1.4
- update of mystic-crypt version to 5.8
- update of guava version to 27.0-jre

Version 5.2.1
-------------

ADDED:

- obfuscation entry can be delete now

Version 5.2
-------------

ADDED:

- obfuscation entry can be edited now

CHANGED:

- update of mystic-crypt version to 5.5

Version 5.1
-------------

ADDED:

- new obfuscation internal frame created to obfuscate with upper- lowercase operation with indexes

CHANGED:

- removed unneeded .0 at the end of version
- removed old obfuscation internal frame


Notable links:
[keep a changelog](http://keepachangelog.com/en/1.0.0/) Don’t let your friends dump git logs into changelogs
