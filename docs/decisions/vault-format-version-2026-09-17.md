# How the vault says which format wrote it, and what an older build does with a newer one

A record of the decisions taken in [#402](https://github.com/astrapi69/mystic-crypt-ui/issues/402) on
2026-09-16 and 2026-09-17, not a rule. The binding short form is in `VaultXmlCodec`,
`ApplicationXmlFileStoreWorker` and `DesktopMenu`.

## The state

- The vault's root element carries `formatVersion` as an **attribute**. Every vault up to and
  including 8.5.1 has none. 8.6 writes `2` (`VaultXmlCodec.FORMAT_VERSION`).
- Reading skips an element the build does not know. All three ways a vault is protected read and
  write through `VaultXmlCodec`.
- A vault whose version is higher than the build's **opens read-only**: it is shown, opening it
  names the version it needs, and it takes no changes and no save - every way of changing it and
  both save actions refuse with that message (#405), and `VaultXmlCodec.toXml` and
  `ApplicationXmlFileStoreWorker.storeApplicationFile` refuse the write underneath.
- An entry's history and the names of its protected properties are `null` when empty, so a vault
  without them writes no element for them.

## Why an attribute

Measured against the published 8.5 and 8.5.1 application jars, probe source in #402: both read a
vault with an unknown attribute on the root, and both refuse a vault with an unknown element - which
their sign-in reports as "Password is not valid". The attribute is what keeps a vault 8.6 writes
readable by them. `MCRDB2` has no version of its own beyond the magic, and a new magic would have
locked 8.5 out of every vault, not only the ones that need 8.6.

## Why read-only and not open-and-save or refuse

Skipping unknown elements is what stops a newer vault reading as a wrong password. It also means the
model in memory lacks them, so a save would remove them from the file. Refusing to open would keep
the file safe and the user locked out of their own passwords; opening normally would keep the user in
and lose data on the next save. Read-only does neither (the maintainer's Q1).

## What checks it

- `VaultOpensInTheLastReleaseTest` runs the release named by `formatCompatibilityRelease` in
  `gradle.properties` (8.5.1) in a child JVM, from the jar in the published installer checked against
  its published sha256: a vault 8.6 writes opens with every entry as written, and one carrying a
  history or protected property names is refused. It fails in CI without the jar and skips locally
  without network (the maintainer's Q2).
- `VaultFormatVersionTest`: the attribute, the skipped element, all three protection paths, the
  refusal to write a newer vault and that the refusal leaves it dirty.
- `ANewerFormatOpensReadOnlyUiTest`: a newer vault opens readable and names its version; Save, Save
  As and the toolbar's Save are disabled; every way of changing it is refused and leaves the model
  unchanged and clean.
