# Roadmap

State of 2026-11-24, kept by the maintainer. The GitHub milestones decide which release an issue belongs to; this page
says in which order the releases come, why, and which decisions they wait for. Counts of open issues are not kept here,
the milestones show them. The dates after 8.16 are proposals.

## In short

- **8.16** comes out around 2026-11-27. It brings the fix for GHSA-m42j-97pf-6mc7 to users through mystic-crypt
  13.11.1, three major releases of runtime libraries, fewer bundled libraries, and dialogs that open over the
  application window.
- **8.17**, December, makes the release and the supply chain checkable: a signed installer, verified dependencies, an
  SBOM, tests on Windows and macOS, an automated release smoke. It also takes the work that is ready as draft pull
  requests.
- **8.18**, January, separates the plugins from the host and checks their origin, depending on decision E2.
- **9.0**, first quarter of 2027, brings the new vault format, and with it everything that touches the format.
- **First of all:** the decision on the direction of the project (E1), before the end of November.

## Decisions the roadmap needs

These five decisions settle what comes after 8.16. Without E1 and E2, 8.18 and 9.0 cannot be planned firmly.

| Decision | By | What depends on it | Recommendation |
|---|---|---|---|
| **E1 Direction:** a workbench for keys and secrets, or a password manager? Is a Gradle plugin for build secrets worth it? Does "no auto-type, no browser, no mobile" stand publicly? | 2026-11-29 | the funding application, the scope of 9.0, the README | decide now, then one paragraph in the README |
| **E2 Plugin API and the origin of plugins** (#1094, #1092, #1093) | 2026-12-01 | 8.17 or 8.18, and 9.0 | an API module after the pf4j pattern, OpenPGP signatures, a list of allowed plugins in the vault |
| **E3 Scope of 9.0:** keys as an entry type, the plugin list in the format | 2026-12-15 | 9.0 | decide with E1, into the format rather than into a later migration |
| **E4 Order of the library releases** | 2026-12-01 | the consumers, Renovate | crypt-data first, then mystic-crypt 13.12, then crypt-api |
| **E5 Cyber Resilience Act:** which obligations apply to the project (manufacturer, reporting, SBOM) | 2026-12-31 | reporting, the SBOM | legal advice |

**Open question for E2:** the [8.17 milestone](https://github.com/astrapi69/mystic-crypt-ui/milestone/4) carries
#1092 and #1093, as decided on 2026-11-20 (analysis and issues until the 8.16 tag, the work in 8.17). An earlier draft of
this roadmap put them into 8.18 so that 8.17 stays one cycle. Which one holds is part of E2.

## Now: 8.16

Cut-off 2026-11-24, cut 2026-11-26, release around 2026-11-27. The
[8.16 milestone](https://github.com/astrapi69/mystic-crypt-ui/milestone/3) has no open issue left.

**In it**, as the `changelog.d/` fragments and the merged pull requests on develop describe:

- mystic-crypt 13.11.1 with the fix for GHSA-m42j-97pf-6mc7;
- file-worker 21.1, gson-extensions 4.1 and swing-table-components 4.0, three major releases;
- no longer shipped: swing-components and JavaHelp (with iText 5 under the AGPL-3), xml-extensions, the Tango and Remix
  icon libraries, and SwingX (#1117);
- the license texts of the bundled libraries that ship without one, under `legal/third-party`, named by Help > Licence
  (#1014);
- the dialogs open over the application window instead of on the first screen (#63);
- the sign-in dialog offers the key file of the database chosen, and a setting "Remember key files" (#997);
- View > Look and Feel saves the choice (#1064);
- corrections and new cases in the manual test plan, among them the release smoke of 8.16 (#1095).

**Known issues:** #962, an end-to-end test that lost a click on a settings tab once; #1065, the Metal theme comes back
as Ocean, its fix is ready for 8.17.

**After the tag:** 8.16 is entered in GHSA-m42j-97pf-6mc7 as the patched version of mystic-crypt-ui.

## December: 8.17, foundation and supply chain

8.17 makes the release and the supply chain checkable. The target is mid-December.

| Area | Issues |
|---|---|
| Ready as draft pull requests, merged after the 8.16 tag | #1017 a switch that marks a custom field as protected (#1138), #1001 the unlock prompt says the clipboard was emptied (#1140), #1124 the "Remember key files" notice deletes the kept-aside copies (#1141), #1053 the key is derived off the event thread (#1143, #1144), #1118 export of chosen entries into a KeePass file of their own (#1145), #1065 the Metal theme (#1146), #1002 Apply in the settings (#1147) |
| Supply chain | #1108 OpenPGP signature of the installer and a published fingerprint, #1109 dependency verification in the build, #1110 an SBOM per release, #1103 a check for references to private notes before every push |
| Tests and release | #1004 an automated release smoke against the installed artifact, #1111 nightly tests on Windows and macOS, #1112 a test with a large vault, #920 reproducible builds |
| Dependencies | the dependency groups D to H, Renovate for every repository (#1123) |
| Entries | #1120 a Wi-Fi QR code for an entry, #1122 whether "Remember key files" also keeps key file paths out of the vault |
| Plugin API, see E2 | #1092 an API module of its own, #1093 the origin of plugin zips |
| Follow-ups | #1091 OwnedDialogs goes once astrapi69/swing-base-components#29 is fixed, #1010 icons in the tree and in dialogs, #815 the application icon in dialogs, #962 the lost settings tab click |

Precondition: decision E4, so that 8.17 can take mystic-crypt 13.12.

## January: 8.18, plugins and entries

8.18 separates the plugins from the host and checks their origin, if E2 does not put that into 8.17. The target is the end
of January.

| Area | Issues |
|---|---|
| Plugin API | #1092 an API module of its own after the pf4j pattern, a version check through `plugin.requires`, disabled plugins with their reason in the settings |
| Origin of plugins, part 1 | #1093 OpenPGP signatures of the plugin zips, a signing subkey of their own, a development mode only in the isolated profile |
| Entries | #1119 the KDBX import merges an entry whose ID is already in the vault |

After that, one plugin can move into a repository of its own as proof; the candidate is kem-demo-plugin.

## First quarter of 2027: 9.0

9.0 is the release with the new vault format. Everything that touches the format belongs into it, so that there is only
one migration. Changing the vault format needs the maintainer's word first (`.claude/rules/architecture.md`).

| Area | Content |
|---|---|
| Format | #865 the model, the mapper and the container, #864 checked-in fixtures for all 12 combinations of release and variant, #866 legacy vaults convert on save, with a backup and a notice, #775 silly-bean leaves |
| Origin of plugins, part 2 | a list of allowed plugins (ID, version, SHA-256) in the vault, protected by the master password; an unknown or changed plugin starts only after explicit consent |
| Product, depending on E1 and E3 | keys as an entry type of their own; a Gradle plugin for build secrets would be a project of its own beside 9.0 |

Open: without E1, 9.0 brings a new format but no new visible function.

After 8.18 and not yet in a milestone: sharing entries encrypted to the recipient's public key and signed with one's
own, with a trust decision on import after the pattern of KeePassXC KeeShare, built together with the signature check
of #1093.

## Libraries of the family

Latest releases measured on 2026-11-24 with `gh release list`.

| Repository | Latest release | Next | Proposal |
|---|---|---|---|
| crypt-data | 13.3.1 (2026-11-14) | 13.3.2 or 13.4 | December, first |
| mystic-crypt | 13.11.1 (2026-11-19) | 13.12, with jobj-core 10.0 (mystic-crypt#332, merged) | December, for 8.17 |
| crypt-api | 10.2 (2026-09-07) | 10.3 or 11.0; describe the release steps first (crypt-api#57) | December |
| lethenon | 0.9.0 (2026-11-12) | 0.10.0; before it, an audit of the network code and mutation tests | January |
| swing-base-components | 5.2.1 (2026-11-04) | swing-base-components#29: a dialog centred over its parent | December, for #1091 |
| swing-wizard | | swing-wizard#11: swingx-all out | December |

## Ongoing, and deliberately not planned

**Ongoing, in every cycle:** a dependency currency check at the start of the cycle, with self-hosted Renovate and no
waiting period. The test plan and the rules are kept with the work, not as projects of their own.

**Deliberately not planned until E1 is decided:**

- auto-type (#354), browser integration, a mobile app;
- loading plugins over the network (pf4j-update): the protection of #1093 rests on its absence;
- gradle/actions from v6 on, because of the terms of use of its caching component.
