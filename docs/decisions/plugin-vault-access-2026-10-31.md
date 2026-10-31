# How a plugin reaches the open vault: only its own entries, refused when it may not write

A record of the decision taken in [#675](https://github.com/astrapi69/mystic-crypt-ui/issues/675) on
2026-10-31, not a rule. It describes the decision; the code it describes is built after the tests
for it are written and red. The first consumer is the lethenon plugin, whose wallet lives in the
vault (`lethenon-wallet-in-vault-2026-10-30.md`).

## The decision

Taken by the maintainer on 2026-10-31: a narrow plugin interface after the principle of least
privilege, instead of access through `MysticCryptApplicationFrame.getInstance()`.

1. **A plugin sees and changes only the entries that carry its own marker.** The marker is a custom
   property of the entry. Entries without it are invisible to the plugin: they are not in what it
   reads, and an attempt to change, replace or remove one is refused. A plugin cannot take the
   marker off an entry through this interface either, since that would turn one of its entries
   into one it could then no longer be held to.
2. **The marker belongs to the plugin's own namespace.** A plugin declares its marker; the host
   accepts it only when it starts with the plugin's id, without the `-plugin` suffix, and a dot:
   `lethenon-plugin` may mark `lethenon.wallet`, not `keystore.something`. A declaration outside the
   namespace is refused when the plugin is connected, and that plugin gets no access at all. So no
   plugin can claim the entries of another by naming its marker.
3. **The host hands each plugin its own access.** The plugin does not ask for the vault; it declares
   that it wants access (a new extension point), and the host connects it with an access object
   bound to that plugin's marker. There is no static way in.
4. **The interface refuses on its own.** Adding or changing is refused, with the reason in the
   message, when no vault is open, when the vault is locked, or when it is read-only (#405). The
   check is in the host, not left to each plugin.
5. **An event before the vault locks or closes.** Every connected plugin is told before the host
   locks or closes the vault, while the entries are still there, so it can drop and overwrite what
   it holds. After the event the plugin's access reads as "no vault open" until a vault is open and
   unlocked again.
6. **Copies out, checked changes in.** What the plugin reads are copies of its entries; a change goes
   back through the interface, which checks the marker on the stored entry and on the changed one,
   then marks the vault changed and refreshes the tree, as the host's own edits do
   (`SecretKeyTreeWithContentPanel.reloadApplicationTreeModel`). A new entry goes into the root
   group. The group selected in the tree is known only to the tree panel; handing it to the
   interface would tie the interface to the window, and a plugin finds its entries by its marker
   wherever the user moves them.

## Why not `getInstance()`

The plugins reach the host frame today through `MysticCryptApplicationFrame.getInstance()`, mostly
to open dialogs over it. Through the same door a plugin could read every entry of the vault, change
any of them, and write the vault without the read-only check. Nothing in that path knows which
plugin is asking. The interface above is the only way the lethenon plugin reaches the vault; the
existing uses of `getInstance()` in the plugins are recorded in an issue of their own, not changed
here.

## Tests first (red before the code)

- a plugin reads only the entries with its marker; an entry without it is not among them;
- a plugin can neither change nor remove an entry without its marker, also when it names that
  entry's identifier, and the entry is unchanged afterwards;
- a change that takes the marker off is refused, and the stored entry keeps it;
- a marker outside the plugin's namespace is refused at connection, and the plugin gets no access;
- adding and changing are refused with no vault open, with a locked vault and with a read-only one,
  and the vault is unchanged;
- an added entry is in the root group, gets an identifier, the vault is marked changed, and the
  entry survives a save and a reopen;
- the event reaches each connected plugin before the entries are wiped, once per lock or close, and
  the access reads as "no vault open" afterwards.

## Rejected

- **Access through `getInstance()`**: no scope, no read-only check, no way to know which plugin acts.
- **A static accessor in the host** (`VaultAccess.forMarker(...)`): any plugin could pass any marker.
- **The marker in the plugin's descriptor only**: the namespace rule would still be needed, and a
  declaration in code is checked by the same test as the rest of the interface.
