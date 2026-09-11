# What locking keeps in memory, and for how long

A record of a decision taken in [#242](https://github.com/astrapi69/mystic-crypt-ui/issues/242) on
2026-09-11, not a rule. The binding short form is in the code it describes; this page is why the code
reads the way it does, and what was rejected.

## The decision

Locking clears the key material, not the decrypted content.

Two things live in memory while a vault is open, and they carry different weight. The derived key and
everything it can be rebuilt from is the attack surface: a process that sits locked for hours keeps
it reachable in a core dump, in swap, or in a hibernation image long after the user walked away.
Locking therefore wipes it, every time.

The decrypted content is the cost side. Rebuilding it means reading and decrypting the file again,
and the 600,000 PBKDF2 iterations behind that are what #237 promised the user would not pay on every
short lock. It therefore stays, and the idle watchdog is what bounds how long: when the timer closes
the vault, everything goes, overwritten rather than dereferenced.

Unlocking is no more expensive for this: the key is derived from the entered password either way,
which is what the verifier already does.

What this does not cover: string copies held by Swing components. They cannot be overwritten from
here, and no amount of care elsewhere changes that.

## What that means, field by field

Wiped when the workspace locks:

| what | where | note |
|---|---|---|
| the master password | `MasterPwFileModelBean.masterPw` | replaced by a verifier that recognises it and cannot produce it |
| the repeat of it | `MasterPwFileModelBean.repeatPw` | the same secret typed twice, read by nothing after the vault exists |
| the private key of a key-file vault | `KeyModel.encoded` | for a key-only vault this is not the second way in, it is the only one |
| the system clipboard | | already cleared on lock since #237 |

The derived key needed no change: it is recomputed per operation inside `PassphraseBox.deriveKey` and
is never held in a field. That was measured before the change, not assumed.

Left in place when the workspace locks: the tree, the entries with all six of their character fields,
the custom properties, the attachment bytes. This is the promise from #237.

Wiped when the vault closes - by the idle watchdog, by the menu item, or at the end of the
application: all of the above, plus everything the lock left in place, plus the verifier the lock
created. Overwritten, not dropped, which is what `VaultCloseSupport` and its tests are for.

## The clock is the bound, and it is the user's

`IdleLockDecision.DEFAULT_TIMEOUT_MINUTES` is 15 and `DEFAULT_CLOSE_LOCKED_MINUTES` is 15, both
editable in the general settings. So on the defaults, walking away costs at most half an hour of
decrypted vault: fifteen minutes to lock, fifteen more until it is gone. That number is the only
thing about this decision a user can change, and it is the whole window.

One exception, deliberately: a vault with unsaved changes is not closed by the timer. A locked
workspace has no master password to write them with, and a close that discards somebody's work
silently is worse than the memory it saves (#304). Such a vault stays open, locked, and decrypted
until a person decides. Memory hygiene never costs somebody their entries.

## Why not the alternatives

**Leave everything, and say so.** The argument for it is that an attacker who can read process memory
can also wait until the vault is unlocked, so wiping at lock buys little. That holds for an active
attacker already running on the machine and waiting. It does not hold for the case this is about: the
process sits locked for hours with nobody there, and core dumps, swap files and hibernation images
preserve that state past the process itself. The attack surface is not what an attacker could do, it
is how long the state stands open.

**Wipe everything at lock, re-read the file on unlock.** Honest, and it gives up exactly the property
#237 was built for. Every short lock would then cost 600,000 PBKDF2 iterations plus a decrypt, and a
lock that is expensive is a lock people turn off.

**Derive identity from the content, or keep the key and drop the password.** Neither reduces what is
reachable: the derived key is the thing an offline guesser tests candidates against, so retaining it
instead of the password swaps one secret for an equivalent one.

The split taken here costs nothing on the unlock path, because the password is typed again either
way, and it removes the expensive half of the surface. That is the whole argument.

## What stays unreachable, with the reason

None of these is a gap somebody forgot; each is a limit of the layer.

- **Swing `Document` contents.** A `JPasswordField` holds its text in a document the JDK gives no
  caller a way to overwrite; `getPassword()` hands out a copy and the original stays.
- **String copies of entry text.** `EntryText.asText` produces a `String` for every rendered or
  copied field, and a String cannot be overwritten.
- **Custom property values.** They are `KeyValuePair<String, String>`, and the conversion that worked
  for the entry's own six fields does not work here - it would change the vault format. Measured and
  pinned in `EntryPropertiesTypeIsPartOfTheFormatTest` (#335).
- **The whole-vault plaintext on the key-file and legacy read paths**, where the library decryptors
  return a `String`. The password path already works in `char[]` end to end and wipes it.

## What this is checked by

`LockingErasesTheKeyMaterialUiTest` holds the buffers across a real lock in the running application
and asserts both halves: the key material zero-filled, the entries untouched.
`ClosingAVaultErasesItFromMemoryUiTest` does the same across a close, and
`AutomaticLockHoldsTheInvariantUiTest` drives the watchdog that connects the two.

Every one of those asserts on the buffer rather than on the field, because a null check passes
whether or not anything was overwritten - and half of what is asserted here is that something was
deliberately *not* overwritten, which a null check cannot express at all.
