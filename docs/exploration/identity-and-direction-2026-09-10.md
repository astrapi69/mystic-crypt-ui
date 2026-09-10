# What mystic-crypt-ui can be that nothing else is

Exploration, not a decision. It exists so the decision in section 8 can be taken deliberately
rather than by drift, and so the answer is written down where the next reader finds it.

Starting point, from the comparison with the field: as a password manager this loses to KeePass.
As a key toolbench on the JVM with a declared plugin trust boundary it is something KeePass is
not. So the question is not which feature is missing. It is which identity is chosen. A
one-person project can do one thing particularly well, not five.

---

## 1. The decision that orders everything else

**Proposal:** mystic-crypt-ui is the workbench for keys and secrets of developers and
administrators on the JVM. The vault is a function of that, not the purpose.

What follows from it:

- The audience is not "everyone with passwords". It is people who handle SSH keys,
  certificates, API tokens, signing keys and build secrets, and who use four tools and a text
  file for it today.
- KeePass is not a competitor but a neighbour. KDBX import and export are the bridge, lossless,
  in both directions.
- Auto-type, browser integration, mobile: not on the list, explicitly. That is KeePass ground,
  and nobody wins there against twenty years of head start.

Without this decision every roadmap is a wish list.

---

## 2. What the project already has and does not yet play

| Asset | Today | What it can become |
|---|---|---|
| mystic-crypt, crypt-api, crypt-data | a library under the application, a CLI with keygen, convert, checksum | the heart of the product, made visible instead of hidden |
| the plugin model with `isUsableWithoutAVault()` | a trust boundary as code, enforced since #232 | extensibility for organisations with their own formats, without a plugin undercutting the lock |
| the inventory test and the lock invariant | security properties checked by machine (#284) | a published list of what the application guarantees, with the test behind each line |
| two advisories in one week, honestly written | a reputational risk if left as is | a reputational gain once it is visible as a way of working: found, measured, published, fixed |
| JVM, MIT, one repository per layer | embeddable | library and application as one ecosystem other Java projects can use |

---

## 3. The wedge: secrets in the build

This is the one point recommended as the main direction, because it satisfies three things at
once: real pain, a JVM home game, and it uses everything that is already there.

**The problem.** Every Java developer has secrets in `gradle.properties`, in environment
variables, in `~/.m2/settings.xml`, in CI secrets nobody rotates. Signing keys for Maven Central
sit as base64 in GitHub secrets. There is no tool on the JVM that connects a local vault to the
build.

**The proposal.** A Gradle plugin and a Maven counterpart that resolve secrets from an unlocked
mystic-crypt vault. `secret("central.token")` in the build instead of cleartext in a file. The
vault is unlocked once per session; after that a local agent serves the values, similar to an
SSH agent. Rotation is changing one entry, not hunting through five files.

**Why that is special.** KeePass cannot do it and does not want to. 1Password and HashiCorp
Vault can, but as a cloud service or a server installation. A local, file-based, MIT-licensed
vault that speaks directly to a Gradle build does not exist.

**What has to be in place for it.** Exactly the things that are mandatory anyway: a clean close
path, automatic locking, a model that does not confuse two vaults. The wedge presupposes the
security work of this week; it does not replace it.

---

## 4. Second direction, smaller: keys as first-class entries

KeePass stores an SSH key as an attachment. mystic-crypt-ui can work with it, because the
library is underneath.

- An entry type "key" with format, algorithm, fingerprint, and an expiry date for certificates.
- Operations directly on the entry: convert the format, export the public half, check the
  fingerprint, compute a checksum. These are the functions that already exist as plugins, bound
  to the entry instead of to a window.
- Expiry warnings for certificates and tokens. A vault that says what becomes invalid in thirty
  days.

This is cheaper than the wedge because the code exists, and it makes the library visible inside
the product. On its own it is not a reason to switch, which is why it is the second direction
and not the first.

---

## 5. Third direction, almost free: verifiability as a feature

A small project can be more transparent than a large one. That is rarely used.

- A page "what this application guarantees", every sentence with the test that holds it next to
  it. The inventory test and the lock invariant are the beginning; the close rule and the
  automatic lock join them.
- Reproducible builds, signed installers with checksums, an SBOM, CodeQL. Most of that exists or
  half exists.
- Make the advisories visible as a way of working rather than hiding them. Two honest advisories
  in one week are a better sign to a security reviewer than two years of silence.

This wins no users, but it wins the users who count: the ones who look before they deploy.

---

## 6. What to leave alone

- **Synchronisation** before the data model. Already settled.
- **Auto-type, browser, mobile.** Someone else's ground.
- **New plugins with no tie to the identity.** Every plugin should answer why it belongs in a key
  workbench.
- **Features before trust.** While the close path and the automatic lock are missing, every new
  feature is a promise on a foundation that gave way twice in one week.

---

## 7. Order

1. **The foundation, no exceptions:** #279 in 8.5, the close path, #241 automatic lock, a
   lossless KDBX round trip. That is three or four releases, and they are the precondition for
   everything below.
2. **Fix the identity in writing:** README, project description, one paragraph that says what
   this is and what it is not. Costs an hour, orders everything after it.
3. **Keys as an entry type** with the operations that already exist. Makes the library visible,
   small effort.
4. **The wedge:** the Gradle plugin plus the agent. That is the piece somebody switches for.
5. **Verifiability** in parallel, because it costs almost nothing and grows with every test.

### State on 2026-09-10, measured rather than assumed

The foundation in point 1 was written when it was still ahead of us. It is behind us:

| Item | State |
|---|---|
| #279 second vault takes the first one's content | closed, in 8.5-SNAPSHOT |
| #281 no way to close a vault | closed, the close path exists |
| #266 no way back into a vault after cancelling the sign-in | closed |
| #241 no automatic lock | closed |
| #242 the vault stays decrypted while locked | closed |
| #272 / #273 entry identity and modification timestamps | closed |
| #284 / #285 the lock invariant and the door it found | closed |
| lossless KDBX round trip | **open, and not even filed as an issue yet** |

So the only item of the foundation still outstanding is the KDBX round trip, which loses
identity and timestamps today. Point 1 is one issue away from done, and points 2 and 3 are
reachable in this release cycle rather than in three.

---

## 8. What has to be decided

1. Is the workbench identity the one you want? If you want a password manager, the answer to the
   original question is a different and considerably longer one.
2. Is the build wedge the direction you would put two months into? It is the only point here that
   supplies a reason to switch, and the only one with real effort behind it.
3. Are you prepared to list auto-type, browser and mobile publicly as "not planned"? That is the
   part of the decision that hurts, and the part that makes it credible.

---

## Short form

- What is special is an identity, not a feature: a workbench for keys and secrets on the JVM,
  with the vault as part of it.
- The wedge somebody switches for is the connection between vault and build: secrets in Gradle
  and Maven out of a local vault instead of cleartext files.
- Before it comes the foundation without which every promise is hollow: the close path, the
  automatic lock, a lossless KDBX round trip. All of it is done except the round trip.
- Verifiability as a feature is almost free and wins the users who look before they deploy.
- KeePass ground is not entered, and that is said publicly.
