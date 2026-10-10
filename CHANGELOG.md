## Change log
----------------------

Version 8.9 (unreleased)
-------------

CHANGED:
- build and tests only: two checks that develop's branch protection requires. `release-freeze` is red on every other pull request into develop while a pull request from a `release/*` branch is open, so a merge during a freeze is refused in the browser and through auto-merge too; the release pull request itself and one labelled `release-content` pass (#516). `closing-references` is red on a pull request that would close an issue no line names on its own as `Closes #NN`, so a release pull request listing "KNOWN AND NOT FIXED: #NN" no longer closes that issue, as 8.7 and 8.8 did (#555)

Version 8.8
-------------

The release in which the Lethenon plugin joins the nodes of the test network - it synchronises with one, hands it transfers and runs one - and moves to lethenon 0.4.0, which refuses every chain an earlier version wrote. It also says what a public start of the main chain runs into, and the main chain itself waits for lethenon 1.0.0.

ADDED:
- the Lethenon plugin says what a public start of lethenon's main chain runs into: "Before You Start" shows, in short, what needs an authorisation under MiCA and what does not, each line with its article, the notice that this is not legal advice, and a button for each of lethenon's five launch documents, which carry the provisions in full with their sources. The mine window shows the same view before the choice between the test network and the main chain, while that choice is open. The view is the plugin's first text in German as well as English; on a JVM whose language is German it shows in German, the rest of the plugin stays English. The end-to-end suites now run in English whatever the desktop's language, since they look for English text (#540)
- the Lethenon plugin synchronises a chain file with a node: "Synchronise with a Node" takes the host and port of a running lethenon node, and for Tor its SOCKS proxy, and brings the chain file up to that node's tip through the chain library's own sync. The node is asked for blocks only, never for a balance; every block is verified here by the library's replay, and the file is written once, at the end, only when the chain grew - a sync that fails leaves it as it was. A file that does not exist yet starts from the node's genesis block, taken on first use, and the window says so. Nodes run on the test network only, so a main chain file is refused before anything is sent. The sync runs in the background and the application stays usable meanwhile, since through Tor it can take minutes (#530 step 4)
- the Lethenon plugin hands a transfer to a node: with a node named in the send window, the signed transfer goes to that running node, which serves the chain file, instead of waiting next to the file - what lethenon's `send --node` does. Afterwards the window reads the node's pool, which the node keeps next to the chain file, because only the pool says whether the node admitted the transfer; a node that cannot be reached leaves nothing waiting anywhere, and one that serves another chain file is named with where to look (#530 step 3)
- the Lethenon plugin runs a node: "Run a Node" starts a node of the chain library on a chain file of the test network, listening on a port and connected to the peers named by host and port, and, with mining ticked, mining on its tip and pool for the Ed25519 account of a wallet. Every second the window shows the height, the connected peers, the transfers waiting, the blocks mined and why connections were refused; stopping waits for the mining round in progress and says where the node stopped. Closing the window stops the node too, so no node keeps listening without a window. The node keeps the chain file and the pool next to it, so the other tools of the plugin see what it adopted (#530 step 2)

CHANGED:
- the Lethenon plugin starts a test chain: where the chain file does not exist yet, the mine window asks which chain the genesis block starts, and the test network (`lethenon-test-2`) is the default, since lethenon puts every new scheme on the test chain first (its ADR 0001). The main chain is in the list, and cannot be started before lethenon 1.0.0 (below). Once the chain file exists its genesis block decides and the choice is closed; the report names the chain the block was written to (#518, step 1 of #530)
- the Lethenon plugin builds on lethenon 0.4.0, the consensus break of lethenon#137, by way of 0.3.0. **A chain started before - from this plugin in 8.7, or from the lethenon command line 0.1.0 to 0.3.0 - is refused as a whole, by its identifier: the main chain is `lethenon-2` now and the test network `lethenon-test-2`, and a chain under `lethenon-1` or `lethenon-test-1` was started under the earlier rules.** Start a new test chain from the mine window. A new chain's genesis block pays its reward, 1,984 LETH, to the burn account, which nobody can spend, and the next block pays the wallet that mines it (lethenon#111, lethenon#148); the block reward declines to a tail of 66 LETH (lethenon#133). **The main chain cannot be started before lethenon 1.0.0**: it starts only from the genesis block the library fixes in its code, and 0.4.0 has none. The mine window offers it as starting with 1.0.0 and refuses it with the library's reason, and every tool of the plugin refuses a `lethenon-2` chain however it was made (lethenon#161, #544). Every refusal of a chain names its file: a file the library cannot read with its size and the library's reason (#532), a chain it refuses with the library's reason (#535, #549)
- the libraries, against 8.7: lethenon 0.2.0 to 0.4.0 (#532, #544), resourcebundle-core 6.0 to 6.1 (#533), mystic-crypt 13.4 to 13.5, Guava 33.7.2, Commons Lang 3.21.0 and MigLayout 11.5.1 (#546). KeePassJava2 3.1.0, pf4j 3.16.0 and izpack-ant 5.2.7 are not in it and wait for sessions of their own: the first changes the KDBX API, the second how plugins are found, the third the installer (#546)
- build and tests only: the UI tests delete the temporary home directory each of them creates; the rules say a red test's result is copied away before anything reruns, after an hour at the 8.7 gate went into reproducing what one file would have said (#522); Mockito 5.24.0 and the build plugins Spotless 8.10.4, Lombok 9.8.0 and Gradle Versions 0.65.0 (#546)

FIXED:
- texts with an apostrophe lost it on the way to the screen - "the certificate's" showed as "the certificates", 34 of them across the application and six plugins - and the question before a checksum file is replaced said "null already exists" instead of naming the file. The message lookup with a default ran each text through MessageFormat, twice, with nothing to format it with (resourcebundle-core#21). resourcebundle-core 6.1 returns such a text as written, and formats one with parameters once; every plugin takes the library's version from the application's version catalog now, so it is named in one place (#533)

FORMAT:
- a Lethenon chain file 8.7 wrote is refused by 8.8, as a whole, in every window of the plugin that reads a chain file - Verify a Chain, Show a Chain, Show a Balance, Send LETH, Mine a Pun, Sweep One-Time Payments, Synchronise with a Node and Run a Node - and the refusal names the file: "<the file's absolute path>: block 0: chain 'lethenon-1' was started under the rules before lethenon 0.4.0; under the rules from 0.4.0 on the main chain is 'lethenon-2'", and the same for a test chain under `lethenon-test-1`. Nothing is written, the file stays as it was; start a new test chain from the mine window. In the other direction, 8.7 refuses a chain file 8.8 writes at its genesis block. Measured between the chain libraries the two releases carry, 0.2.0 and 0.4.0, by lethenon's compatibility measurement for 0.4.0 in both directions (`docs/launch/compatibility-0.4.0/measure.sh` in lethenon, lethenon#160), and here by `ChainReplaySupportTest`, `MiningSupportTest` and `SyncSupportTest` on a chain under `lethenon-1`; the other windows read the file through the same `ChainReplaySupport` as the first of these (#535, #549)
- the main chain, `lethenon-2`, can be started only with lethenon 1.0.0, which carries its genesis block in its code. Until then 8.8 offers it in the mine window as starting with lethenon 1.0.0, refuses to start it with the library's reason, and refuses every `lethenon-2` chain file, however it was made (lethenon#161, #544). A chain 8.8 starts is a test chain, `lethenon-test-2`, whose genesis block pays the burn account
- wallet files do not change: lethenon 0.4.0 opens the wallets of 0.1.0 to 0.3.0, and 0.2.0, the library of 8.7, opens a wallet of 0.4.0, measured by the same lethenon measurement. The plugin creates no wallet file
- the vault format does not change: a vault 8.7 wrote opens in 8.8 with every entry as written, and a vault 8.8 writes opens in 8.7, with and without an entry's history and the names of protected properties. Measured with the published 8.7 application jar, its installer checked against the sha256 of the release, through the tests that pin a release in both directions (`./gradlew test --rerun --tests '*TheEnvelopeMoveKeepsTheVaultFormatTest' --tests '*VaultOpensInTheLastReleaseTest' -PformatCompatibilityRelease=8.7 -PenvelopeMoveRelease=8.7`): the five that apply to 8.7 pass. The other two assert that the release refuses an entry's history and protected property names, which holds for 8.5.1, the release the build pins (#402), and not for 8.7, which reads both. No commit since 8.7 touches the vault's classes, and mystic-crypt 13.5 changes only the exceptions its envelope declares (#546)

KNOWN AND NOT FIXED:
- the Lethenon plugin cannot start the main chain: lethenon 0.4.0 carries no genesis block for it, which comes with lethenon 1.0.0. The mine window lists the main chain as starting with 1.0.0 and refuses it; every other part of #535 is done (#535)

Version 8.7
-------------

The release the Lethenon plugin arrives in, the one in which the vault's envelope and the key file reader move into the libraries without the format moving, and the one after which no test run touches the desktop it is started from.

ADDED:
- a Lethenon plugin: the protest chain lethenon as a plugin with its own submenu and one setting, the chain file. Its first tool replays a chain file through the chain library and reports what it verified - every block hash, every signature, every state transition and the supply - or the reason it refused the chain (#450)
- the Lethenon plugin shows the blocks of a chain: height, the pun each block was mined with, whom it paid, its transfers, its time and its difficulty. The whole chain is replayed before a single block is listed, so a refused chain shows no blocks, only the reason (#450)
- the Lethenon plugin shows what a wallet holds in a chain: its two direct accounts, which it can spend from, and apart from them the payments to its one-time destinations, kept apart and never added to the spendable amount: they reach the account through a sweep. The window also shows the wallet's published address, which is what a payer needs. The balance comes from replaying the chain file, never from a server. The wallet's password is used once, wiped and cleared from the field, and appears in no text the window shows (#450)
- the Lethenon plugin sends LETH with a memo to an account key: from the wallet's Ed25519 or ML-DSA-65 account, with an optional fee. The transfer is signed and waits next to the chain file until the next block carries it, in the chain library's own pending format, so the command line sees it too. The nonce and the balance come from the replayed chain and the transfers already waiting, so a second transfer before the next block gets the next nonce and cannot spend the same money twice; a transfer the account cannot cover is refused before anything is written. The wallet's password is used once and wiped (#450)
- the Lethenon plugin mines a pun: the next block of the chain file, carrying the transfers that wait next to it and paying the wallet's Ed25519 account. Where there is no chain file yet, the block is its genesis. The extended chain is replayed before anything is written, so a chain that does not verify, a waiting transfer the chain would refuse, or a pun that runs out of attempts leaves the chain file and the waiting transfers exactly as they were (#450)
- the Lethenon plugin pays a published address: the send window takes an account key or a published address, chosen rather than guessed from the text, and pays an address at a one-time destination derived for that payment alone, so two payments to one address land on keys with nothing in common. The report names the address and never the destination: the sender's own screen is a place where that link would be written down (#450)
- the Lethenon plugin sweeps one-time payments onto the wallet's own account, one transfer per destination with that destination's own nonce, through the chain library's own sweep - the plugin has no sweeping loop of its own. The window says what that costs before anything is signed and again afterwards: the chain then shows those destinations and the account together. Receiving is unlinkable; spending is the moment that ends. A sweep that finds nothing writes nothing (#450)
- the password hashing tool can be used without a vault, and while the workspace is locked: it reads typed input, keeps its result in its own window and writes nothing. Everything a plugin offers while locked is now clicked by an end-to-end test, menu item by menu item and button by button, which fails if any of it lifts the lock, shows the vault or an entry of it, or writes the vault's file. A file chooser is cancelled in that test, never approved, and the test says so (#301)

CHANGED:
- the vault's passphrase construction and the key file reader come from the libraries now: PassphraseEnvelope in mystic-crypt 13.4 and AnyKeyFileReader in crypt-data 13.1 replace this application's own PassphraseBox and KeyFiles, which moved there. The file format does not change, and that is measured rather than assumed: a vault the published 8.6 writes opens in this build, and one this build writes opens in 8.6, each direction pinned by its own test against the release jar. crypt-data is declared directly from now on, for the host and the plugins alike (#490)
- the buffers that erase a decrypted vault, written here for #294, come from mystic-crypt 13.3 as SecretBuffers and WipingCharWriter, unchanged in behaviour; the tests that exist because of #294 still prove the erasing (#480)
- generating a key in PKCS#1 for an algorithm without a traditional form - X25519, X448, ML-KEM, ML-DSA - is refused rather than answered with PKCS#8: the command line (`--cli keygen`, `convert`) ends with exit 2 and writes nothing, and the generate-keys window, which already closed its key format box to PKCS#8 for those algorithms, now says why, in the same words as the command line (#435, #480)
- the Lethenon plugin builds on lethenon 0.2.0: it replays test chains (`lethenon-test-1`) the command line starts with `mine --testnet`, which 0.1.0 refused at their genesis block, and opens wallets the 0.2.0 command line creates in its new envelope as well as those 0.1.0 created. A chain started from the plugin is still a main chain; choosing the test chain there is #518 (#517)
- KeePassJava2 moves from 2.2.6 to 3.0.0: one KDBX implementation instead of four, the Jackson classes renamed (JacksonDatabase to KdbxDatabase and so on), no generics on Database, Group and Entry, the credentials now org.linguafranca.pwdb.format.KdbxCredentials, and the artifact org.linguafranca.pwdb:KeePassJava2.kdbx.database. 18 files, renames and two casts where the de-generified containers hand back the interface type. The reflection capsule survives unchanged: measured on the 3.0.0 jar, the field names uuid, times and history are the same on KdbxEntry and KdbxGroup, and its guard test resolves all of them. Group times are now readable through the Group interface (jorabin/KeePassJava2#99), which #473 harvests separately (#467)
- the libraries, against 8.6: mystic-crypt 12.2 to 13.4, crypt-data 13.1 (declared directly), Bouncy Castle 1.85.2/1.85 to 1.86 (#434), KeePassJava2 2.2.4 to 3.0.0, swing-base-components 5.1 to 5.2, and lethenon 0.2.0 for the new plugin
- build and tests only: every test task of every build - the host's suites and each plugin build, whatever starts them - runs on an Xvfb the build starts itself, never on the display of the shell it was started from; on a desktop that display is the person's real screen, and the robot used to click and type there (#504, #505). `make run` starts the development build in a profile of its own and never touches the installed release (#498, #501). `make bump-check` checks a crypt-data or mystic-crypt bump in the host and every plugin, also against a candidate that is only published locally (#482, #508). `make merge-pr` waits for every required check, including those that have not reported yet, and leaves the checkout alone (#493, #496). A commit authored or committed as an AI tool is refused by the hook and by CI (#444). The build takes nothing from the local Maven repository unless asked with `-PuseMavenLocal`, and resolves snapshots from the Central Portal (#464, #475); the remote publishing repository is gone - it named the shut-down OSSRH staging endpoint, this application has never been on Maven Central (measured 404), and the only publish left is `make publish-local`, which the plugin builds use (#453). A failing UI test reports what was on screen (#484), and the tests provoke failed writes by the shape of the path rather than by permissions, which root ignores (#458, #506). The KDBX round trip runs once per class instead of once per test (#416); the release probe declares its own output encoding (#456); the locked-state tests assert the plugins granted, not only those offered (#469, #476); a new build script under `gradle/` is no longer ignored by git (#448). Documentation: the anchor javadoc of the plugin interface sits on its method (#376), and the funding milestone page says how the work is produced (#420)

FIXED:
- a remembered sign-in whose key file path is empty no longer ends the application before the sign-in dialog appears, on Java 24 and later: an empty path is no key file, and only a file is one. Before, the way out was deleting `memoizedSignin.json` by hand; the application does not write that value itself, a hand edit or another tool does (#499)
- a save that failed because the vault's file could not be created - its folder gone or replaced, for example - is reported again. Naming the file for the message created it, failed the same way as the save, and no message appeared; the changes stayed unsaved and ending still asked. The message now names the file without touching the disk (#512)
- the signature tool no longer offers to generate a key it cannot generate: with RSA, ECDSA or DSA chosen, Generate key pair is off and the panel says why before it is pressed - the key has to come from a file. Those algorithms stay in the list, because a key loaded from a file signs and verifies with them; choosing Ed25519, ML-DSA or SLH-DSA turns the button back on (#488)
- a KeePass export with text outside ASCII - "ü", "é", "中文" - written by a Java whose default encoding is not UTF-8 produced a file that the library itself then refused to load. KeePassJava2 2.2.6 writes the XML in UTF-8 whatever the default, measured in a child JVM with `-Dfile.encoding=ISO-8859-1`, and a test guards it (jorabin/KeePassJava2#104)
- an exported KeePass file carried its attachments twice, in the inner header and again in the XML, and KeePassXC warned about it on every read. KeePassJava2 2.2.5 writes them once; measured with keepassxc-cli 2.7.10, no warning, and the attachment reads back byte for byte (#379)
- the combo boxes over a fixed list of choices - the key size of a new private key, the view mode in the settings, the checksum algorithm, the recipient kind of a Lethenon transfer - listed their values in an order that could change from one start to the next. They list them in their declared order now (swing-base-components 5.2), pinned by a test
- the installer's plugin pack named the menu designer among what it installs. The menu designer is development tooling and is not shipped; nothing was installed wrongly, the sentence was, and a test now checks the sentence against what the pack installs (#436)

FORMAT:
- the vault format does not change: a vault the published 8.6 writes opens in 8.7, and one 8.7 writes opens in 8.6, each direction pinned by its own test against the release jar (#490)
- the Lethenon plugin is new in 8.7; 8.6 has no part that reads anything of lethenon's. The plugin creates no wallet file: it opens the wallet files lethenon's command line writes with `wallet create` and `wallet restore` - those of lethenon 0.2.0, sealed in the envelope `LETHWF`, and those of 0.1.0, which keep opening and are never rewritten. lethenon 0.1.0 cannot open a `LETHWF` wallet. A chain the plugin starts is a main chain (`lethenon-1`), which lethenon 0.1.0 replays and mines on as well (#517)

KNOWN AND NOT FIXED:
- the Lethenon plugin cannot start a test chain: its mine window has no choice of chain, so a chain started there is always a main chain. A test chain started on the command line can be followed and extended in the plugin (#518)

Version 8.6
-------------

The release the KeePass bridge and the vault format version land in: a KDBX file written here is the one that was read, a vault says which format wrote it, and five places where the application kept data where the user could not see it are closed.

SECURITY:
- the protection of a KeePass custom property was lost on import and on export. A property marked protected in KeePass - a TOTP seed, a recovery code - came into the vault without that mark, and every export wrote it back unprotected, so KeePass and KeePassXC showed it unmasked and did not protect it in memory. Both directions keep it now. A vault that ever imported a protected property does not carry the mark any more, and cannot get it back from the file it came from: after exporting, set the protection again in the target (#389)
- a generated master password was put on the system clipboard without being asked for. Pressing Generate while creating a database filled both password fields and also copied the password - the credential that opens the whole database, in a surface every other program of the same user can read, before its owner had read it and before it protected anything. Nobody clears that clipboard either, because the reason it is there is that they are about to paste it. It is now shown and not copied: the two fields are filled, nothing touches the clipboard, and whoever wants it elsewhere copies it themselves - at which point the timer below applies and the path there was their own decision (#367)
- the private key that opens a key-file database was written inside it. `masterPw`, `repeatPw` and `lockVerifier` are kept out of the payload deliberately; `privateKeyInfo` was not, so the encoded key was serialized into the vault on every save. For a key-only database that is not an escalation on its own - whoever decrypts the payload holds the key already - but a key file usually protects more than this one database, and copying it inside spreads a credential the user keeps deliberately separate into a file they may sync, back up or hand to someone for support. The sharper case is a database opened with a key and later saved with a password: the key then rested on the strength of a passphrase it was chosen to be independent of. The key is now left out of the file and taken from the key file picked at sign-in, on both key paths. A database written before this still opens, and the key stays in it until the next save overwrites the file (#350)
- locking forgets the key material, and closing takes the rest. Locking wiped the master password and left the repeat of it and the private key of a key-file vault in memory, where a core dump, a swap file or a hibernation image reaches them hours after the user walked away; for a key-only vault the key is the only way in, so forgetting the password and keeping the key forgot the wrong door. The decrypted content stays on purpose, so that a short lock does not cost 600,000 iterations again, and the idle watchdog closes the vault and takes it. Closing now erases what it had been leaving: the attachments, the key, and the clipboard (#242)
- an entry no longer prints the values of its own custom properties. A custom property is where a KeePass user keeps a TOTP seed, a recovery code or a PIN; the rule that keeps secrets out of what an object prints covered the six fields and the history, and was measured still printing `properties=[KeyValuePair(key=TOTP seed, value=...)]` on the released 8.5.1 jar. Every property value drops out now, not only the protected ones (#404)
- the output console survived locking with everything it had ever shown. Measured after a lock: the window still on the desktop, 452 lines, 39410 characters, the vault's full path twice in it out of the stack trace of a save that failed. Locking closes every window on the desktop now, and the console gives the standard streams back and erases its buffer when it goes. The buffer also keeps at most a configured number of lines, 2000 by default, instead of growing for the whole session (#375)
- opening a database in the old format wrote the whole decrypted database to disk next to it and deleted it afterwards. Deleting is not erasing; the blocks stay on the medium until something reuses them, and a password database should never have been there unencrypted. That path decrypts in memory now, like every other one (#353)
- replacing the master password in memory compared references, so a replacement with equal content wiped an array the caller was still using, and a replacement that only looked different left the old one behind. It compares content now, and the array a field replaces is erased (#351)

ADDED:
- a copied password clears itself again. "Copy Password" and "Copy Username" put a secret into a surface every other program of the same user can read, and until now only locking or closing the database took it out again - so on a machine left unlocked it stayed there for as long as the user kept working. It is now cleared after twenty seconds, configurable in the general settings, 0 turns it off. The clear looks before it writes: if the clipboard no longer holds what was copied, because the user copied something else in the meantime, it is left alone - a clear that eats the user's own copy is how a security feature gets switched off (#352)
- the conversion wizard's Target step says which file it is converting. A target path can look exactly like a source path, and nothing on the screen said which one was which; the file being converted now stands above the operations, read-only, because it is chosen in the step before. The per-operation default target no longer overwrites a path the user typed or picked (#297)

CHANGED:
- "Autotype" is gone, with the Selenium dependency it needed. It was a shipped context-menu item on every entry with a URL, its field selectors matched almost no real login form, and it turned the entry's password into a Java String on the way into the browser - the class of leak the rest of this release spends its effort closing (#347)

FIXED:
- a KeePass round trip was not one. The import filed the database under a node renamed "Imported from" and the file's name, and the export hung everything under a new root of its own, so each round trip added a level and lost the root's name (#377); every exported file called itself "New Database created by KeePassJava2" (#378); and every export gave each entry and group a new identifier, stamped all timestamps - an entry's and a group's - with the moment of the export, and dropped the entry history. Measured against a database written by KeePassXC 2.7.10 and read back with keepassxc-cli: structure, names, identifiers, all four timestamps with the expiry flag of every entry and every group (#413), icon indices, custom properties with their protection, attachments and history now come back as they went in (#384). An exported database is named after the vault and described as exported from mystic-crypt-ui
- a duplicated entry no longer shares its protected property names or its history with the original, and carries no history of its own: it is a new entry, not a later version of the old one (#384)
- a save that could not be written said nothing and marked the database as saved. A full disk, a directory the user cannot write or a share that went away left the application showing a saved vault, the previous version on disk, and the unsaved changes discarded without a question by the next ending. The failure is reported with the file and the reason, the changes stay unsaved, and ending asks (#424)
- the obfuscation panels crashed instead of refusing when no key was available. Both encrypt an exported rule table with the key of the signed-in database; without one they threw where a refusal belonged (#357)
- the checksum panel's "Save checksum" button was offered when it cannot work. A checksum over typed text names no file to write beside, and the refusal arrived after the press, in a line the user had to notice; the button is off in that state and its tooltip says why (#320)
- an OpenPGP key file is named instead of reported as malformed. OpenPGP armour and PEM share the same BEGIN line, so the reader got as far as the base64 and told the user their perfectly good key was "malformed PEM data"; the armour header is read first now and the answer names the family, including signatures and private key blocks. Converting OpenPGP is still out of scope (#321)

FORMAT:
- a vault says which format wrote it: `formatVersion="2"`, an attribute of its root element. 8.5 and 8.5.1 pass over that attribute - measured with both release jars, each of which opens a vault carrying it and refuses one carrying an unknown element instead - so a vault 8.6 writes still opens in them as long as it holds nothing they have no field for. An entry's KeePass history and the names of its protected properties are such things. The KeePass import fills them since the converter moved to KeePassJava2's Jackson model (#384): a vault that carries an imported history or protected property names needs 8.6 (#402)
- an element a build does not know is skipped rather than refusing the vault. 8.5 refuses the whole vault for one and tells the user the password is wrong; this holds from 8.6 on, on all three ways a vault is protected - the two key-file paths read and wrote through a second serializer that knew neither the version nor how to skip, and now go through the same one as the password path (#402)
- 8.5.1 and older do not import a KDBX file exported by 8.6: their reader requires an empty `DefaultUserName` element the KDBX format does not call for, and the export no longer writes one. KeePass and KeePassXC read the file (#384)
- a vault in a newer format than the build knows opens read-only: the content is shown, opening it names the format version it needs, and it takes no changes and no save - reading it skipped what this build does not know, and a write would remove that from the file without anybody noticing (#402)

Version 8.5.1
-------------

A patch for 8.5, built from the release it patches rather than from the development branch, so that it carries these three fixes and nothing else. All three were found by measurement in the running 8.5, not reported by a user, and all three concern what the application does with data the user has not asked it to touch.

SECURITY:

- Ctrl+C on a node of the database tree put every password of that node on the system clipboard. The tree copied the node's internal text representation, which listed each entry with its title, user name and password - 857 characters for a single entry, measured - and the clipboard is a surface every other program of the same user can read. The tree now copies the node's name and nothing else, and no model object of this application prints a password, a key or a master password in its text representation any more, which is checked for every such object by a test. A password leaves the vault through "Copy Password" and no other way (#388)

FIXED:

- "Exit" in the File menu ended the application without asking about unsaved changes. Every other way of ending asked; this one, wired since 2020 to a library action whose whole body ends the process, did not, and a user who chose it expecting the question lost every change since the last save. There is now one way to end - the question, the vault closed, then the exit - and both the menu item and the window's close button go through it (#386)
- ending the application never overwrote the decrypted vault. Closing a vault erases its content from memory since 8.5; ending the application did not, on either path, so the one moment the vault's life in memory was supposed to end was the moment its content was left exactly as it was, for a core dump, a swap file or a hibernation image. Measured with the entry's password held across the ending: intact before, zero-filled now. Ending goes through the same close as the menu item, so what closing erases and what ending erases cannot drift apart (#387)

FORMAT:

- unchanged. A vault written by 8.5.1 opens in 8.5 and one written by 8.5 opens in 8.5.1; nothing the vault file stores was added, removed or renamed

Version 8.5
-------------

The bracket around this release: the application does not decide about the user's data. Three ways it did are closed, and all three were reachable without an attacker, in ordinary use.

FIXED:

- an edit typed into an entry dialog was lost to the automatic lock. The dialog writes onto the entry that lives in the tree, but nothing set the "unsaved" flag, and every decision downstream read that flag alone: the lock saved nothing and the close that follows fifteen minutes later wiped the model. A user who typed, left the dialog open and walked away came back to a vault without the change. An open editor now counts as work in flight, so the close leaves the vault alone (#303)
- the automatic lock wrote to the file without being asked, and the automatic close could discard. Locking now keeps the changes in memory and writes nothing; "save when locking" is a setting, off by default, for whoever wants the old behaviour knowingly; the automatic close only closes a clean model, and a dirty one stays locked until somebody unlocks it and decides. Unlocking says in one line that unsaved changes are waiting, and ending the application from a locked workspace asks before discarding instead of offering a save it cannot honour - the master password is not in memory while locked (#304)
- "Save As" and "New database" replaced an existing database without asking. Save As retargeted the open model to whatever the file chooser returned and stored it there, and the writer replaces: picking another vault in that chooser destroyed it, encrypted the open one over it with the open one's master password, and said nothing. "New database" onto an existing file had the same shape - its existence check only decided whether to create an empty file first. Both now ask, naming the file, and the question says that nothing can produce a database again (#300)
- the action that puts the vault window back on screen asked nothing about the lock. Not reachable from the user interface, where the menu item is disabled, but it was the second line that was missing (#285)
- cancelling the save-before-close question closed the application anyway, so Cancel meant the same as No and every change since the last save was gone before the user could react (#288)
- creating a vault while one was already open kept the open vault's content and only changed where saving went: the new database received the other one's entries under its own master password, and a change made to the open one never reached its file (#279)
- after cancelling the sign-in there was no way into a vault without restarting (#266)
- the decrypted database lived in objects that cannot be overwritten, so locking could drop references but not erase. Measured on a heap dump taken after a save and a close: an entry's title was lying in the heap six times over and the master password three times, and what is left of either now is one copy of the database inside a library buffer this application cannot reach - nothing of it is referenced any more (#294)
- the master password and the decrypted database no longer become Java Strings on the way to and from the file. The whole database was serialized into one String on every save and read back out of one on every load, and the master password was converted at the moment of use; none of the four could be overwritten. Every buffer on that path is characters or bytes now, and is overwritten once it has been used. Two paths keep one String each and say so: a database protected by a password AND a key file, and reading a database written before version 8.3, both of which go through library interfaces that take a String (#294)
- "make license-format" stamped the project's licence header onto resources - help files, launchers, a third party's licence text and Spotless's own import-order configuration, after which the build could no longer configure itself. The header goes on Java sources and on nothing else (#282)

ADDED:

- a database can be closed while the application runs. That state did not exist: the save-if-dirty question lived inside the window-closing listener, so ending the application was the only way to reach it. Closing now empties the model and overwrites what it held, and three callers use the one path - a "Close Database" entry, opening another database, and creating one (#281)
- the workspace locks itself after fifteen idle minutes, configurable, 0 turns it off - locking used to be a deliberate user action and nothing else, so an open vault stayed open for as long as the application ran. A locked vault is closed after another fifteen so its decrypted content leaves memory (#241, #242)
- entries get an identifier when they are created, and entries in a vault written before identifiers existed get one when it is opened. Loading does not mark the vault as changed: nothing is written that the user did not ask to write. An identifier is stable once the vault has been saved after that migration (#272)
- the modification timestamp is kept up to date when an entry is edited, and stays empty until a real edit - filling it on load would invent a fact that reads later as measured (#273)
- the checksum tool writes the checksum file, not only reads one: the coreutils form that `sha256sum -c` reads, next to the file it describes. An existing one is replaced only after a question (#296)

CHANGED:

- the lock now has an invariant rather than one regression test per door: every action in the application's action package is fired with a vault locked, and three properties are asserted for each - the locked state holds, the vault stays off screen, the vault file is not written. Exactly one exception, unlocking with the master password. A new action joins it by existing (#284)
- `master` carries the release again, as a numbered step in the release process rather than a habit (#248)
- dependency currency: Spotless 8.10.2, JaCoCo 0.8.15 (pinned rather than left to the Gradle default), pf4j 3.15.1, Lombok 1.18.48, PIT 1.30.0 (#277)

KNOWN AND NOT FIXED IN THIS RELEASE:

- thirteen places still write over an existing file without asking: exported PEM and signature files, generated keys, the persisted menu layout, plugin settings. None of them is a database. They are named one by one in `SilentOverwriteInventoryTest`, and a fourteenth arriving unnoticed fails that test (#300)
- the decrypted vault does not leave memory completely while locked: entry custom properties, attachment bytes, private key bytes and the Swing password fields' copies are dropped rather than overwritten (#242)

Version 8.4
-------------

SECURITY:

- creating a new database while the workspace was locked signed the application back in without the master password. The flow set the signed-in state and rebuilt the menu, and nothing in it asked whether the workspace it landed in was locked, so the application treated the workspace as signed in and offered the complete signed-in menu; what that made reachable differs between the affected versions and is described in the advisory. Executed on 8.1.1, 8.2 and 8.3 - 8.3 is affected although it carries the lock fix from GHSA-6c69-wmrw-76vg, so updating to 8.3 was not enough. Creating a vault is now refused while another one is locked, with a message that says so, and the refusal is asked in the action rather than only at the button, so a keyboard shortcut or a persisted menu layout cannot walk around it (#270, advisory GHSA-jjf2-7wrm-2gp5)
- the master password is no longer kept while the workspace is locked: locking replaces it with a verifier - a salt of 16 bytes drawn fresh for it, and what PBKDF2-HMAC-SHA256 derives from the password over 600000 iterations - which can only answer whether an entered password matches, compared in constant time, and the entered characters are cleared after the comparison. This covers the master password only: the decrypted vault contents still stay in memory while the workspace is locked, which is the part of #242 that stays open (#242)

ADDED:

- what the application offers without a sign-in is decided by a list of what is public, not by a list of what is hidden - an entry is private until it is named. The plugins tab in the settings asks the same decision (#232)
- the conversion wizard says what each step is for, before its first field (#261)

FIXED:

- changing the checksum algorithm kept the checksum file and the checksum that belonged to the algorithm just left (#259)
- the sign-in dialog closed after a wrong password instead of letting the password be typed again (#251)
- "make izpack-installer" failed confusingly when run on its own and left a 0-byte installer behind (#245)

CHANGED:

- the toolbar asks the same decision as the menu instead of carrying its own list, and the one item whose admissibility depends on the state asks a predicate rather than a list of names (#269)
- the lock decision is a display-free predicate outside the Swing frame, so it can be mutation tested (#252)
- the lock and sign-in tests assert the property their names claim - a real screen, a real file, the real clipboard - instead of a stand-in flag (#250, #263)
- the repository opens the next development cycle right after a tag, so develop never carries a released version number (#247)

Version 8.3
-------------

SECURITY:

- locking the workspace left the decrypted vault on screen and fully operable. "Lock workspace" switched the frame into its neutral view and disabled the signed-in menus and the toolbar, which made the state look locked, but the vault window stayed on the desktop with the entry table in it - and the entry context menu is built on that table, so it was never covered by the menu bar walk. Executed on the released 8.2 after locking: title and user name readable, "Copy Password" put the entry's real password on the system clipboard, "Copy Username" the user name, "edit..." changed the entry and "delete" removed it, none of it asking for the master password. Locking now closes the vault window, decides what to show from the sign-in state instead of from whether a vault object exists, and clears the system clipboard, so a password copied before the lock does not survive it. 8.1.1 was affected as well, but only where the vault had been opened as the "Key database" window in the desktop view; in the panel view the vault was gone after locking. Versions before 8.1.1 have no working lock action at all and are not affected (#237, advisory GHSA-6c69-wmrw-76vg)
- the security policy no longer sends a finder to the public issue tracker: vulnerabilities are reported through GitHub's private vulnerability reporting, and the policy now also states how a defect found by the maintainer is handled - public issue, advisory after the fixed release. The supported-versions table names minor lines rather than releases, so a patch release cannot make it stale again (#239)

ADDED:

- the checksum tool follows the algorithm: switching the algorithm looks for the checksum file that belongs to it next to the download, instead of keeping the one found for the previous algorithm (#233)

FIXED:

- comparing a checksum against a checksum file compared the whole line, so a file in the usual "<hash>  <name>" form never matched even when the hash did. The hash is now read out of the file, in the coreutils, BSD and bare-hash forms (#230)

CHANGED:

- every release now publishes a .sha256 and a .sha512 file next to the installer, so a download can be checked against something other than trust (#236)
- releases are tagged with git itself instead of the grgit plugin, which wrote the tag from a task that had to run even when the build had failed (#235)
- "make build-full" keeps the test report it produced instead of wiping it with the packaging step that runs afterwards (#234)
- the lock regression test fires the entry action through the real context menu instead of asserting that the table is merely reachable, and a second test pins that locking clears the clipboard (#243)

Version 8.2
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
- key exchange and key encapsulation demo: both panels explain what they simulate before the first field, and every key, handshake, ciphertext or secret text area has a copy button next to it
- file and text encryptor: explains what it does, that a lost passphrase cannot be recovered, and which tab to use, before the first field
- key store manager gained a guided "Create Key Store..." wizard next to "Manage Key Store": file, type and a twice-asked password, an optional first key pair, then a review before Finish - refuses to overwrite an existing file the same way the certificate and conversion wizards do, and hands off to "Manage Key Store" on success
- every password field gets FlatLaf's built-in reveal button, so a typo is no longer only found out after the password already failed
- imported KeePass entries show the timestamps they carry: created, last accessed, last modified, and the exact expiry time of day the day-precision expiry date cannot show
- imported KeePass entries and groups show the icon their owner picked in KeePass, instead of this application's generic one
- the hex encrypt/decrypt demo next to a generated key pair works with EC keys as well, through ECIES, and no longer only with RSA

CHANGED:

- every panel holds its state in a model object with its components bound to it, so a panel's state is readable at any moment instead of living scattered across the widgets
- the tree hides the root when only one root may exist, so an imported KeePass database no longer appears as a subtree under a node the user cannot remove
- tree nodes can be moved: up, down, and into another node, with the moves that would break the tree refused rather than half-performed
- the installer registers a desktop entry that works, and the install instructions name the Java 25 requirement
- the conversion plugin now offers one guided wizard (Source, Target, Review) instead of two separate tools; the "Convert DER to PEM" and "Convert key file..." menu items are gone, replaced by a single "Convert Key/Certificate..." entry that walks through picking the file, choosing the conversion and confirming the destination, and refuses to overwrite an existing file the same way the certificate wizard does
- key generation demo: "Save certificate..." now enables for EC keys too, not RSA only; a disabled certificate or encrypt/decrypt control now says why, visibly, not only on hover

FIXED:

- the key generation window's encrypt and decrypt form no longer shifts when the reason for those buttons being unavailable is a long one. The reason label shared a layout column with the two buttons, and such a column is as wide as its widest member wants to be, so the longer wording that came with EC support pushed the right half of the form nearly four hundred pixels across. The label now takes its width from the buttons beside it rather than from its own text, and the full reason stays reachable from its tooltip as well as from the ones on both buttons
- the key store command is taken from the library instead of being duplicated in the application
- signing with an existing key file failed for keys on named elliptic curves, because the JDK provider refused what Bouncy Castle had generated; classical signing and key reading now go through Bouncy Castle throughout
- a self-signed certificate written by the key store tool used SHA256withRSA where RFC 4055 requires SHA256withRSAandMGF1
- the sign-in dialog always rendered with Nimbus instead of the configured look and feel (not even the application's own FlatLaf Light default), because it applied a hardcoded choice before the persisted one was loaded
- the intro text on the key exchange, key encapsulation demo and file/text encryptor panels ran off in one unwrapped line instead of wrapping, and now reads as a framed note
- an invalid distinguished name when adding a key pair to a key store used to surface BouncyCastle's raw internal parsing error ("badly formatted directory string") - it now names the expected format instead
- the console tool opened packed down to its content's tiny preferred size on its very first frame instead of the configured docked height - it self-healed a moment later on a lucky coincidental resize, which is why it looked fine in earlier testing despite never actually being fixed
- converting a key to PKCS#1 silently produced a PKCS#8 file for the algorithms that have no traditional form of their own (the edwards and montgomery families, Diffie-Hellman and the post-quantum families): the conversion reported success and wrote a file whose shape was not the one asked for, and it now refuses before writing, naming the algorithm

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
