# Architecture Rules

## Layers

1. **Swing UI** — panels, dialogs, actions. No business/crypto logic here; delegate to
   support/worker classes.
2. **Support/worker layer** — `*Support`, `*Worker`, `crypto/` package. Testable without
   a display; no Swing types in their signatures.
3. **Libraries** — mystic-crypt, crypt-data, crypt-api, BouncyCastle. All crypto
   primitives come from here.

## Plugin-first

New features ALWAYS belong in an internal plugin under `plugins/{name}-plugin/`, unless
they touch the core (vault open/save/format, sign-in, main frame/menu shell, settings
infrastructure). Each plugin brings its own submenu, its own settings, and may contribute
CLI commands. The Makefile target list (`plugins:`) is the canonical plugin inventory —
a new plugin is added there, to the izpack installer config, and to the test wiring in
the same change.

## CLI is a backlink to the library

`--cli` delegates to the mystic-crypt library's root command (picocli). New CLI
capability is implemented in the mystic-crypt LIBRARY first (separate repo, separate
release cycle), then consumed here — never forked or duplicated into the UI. Do not
change library code from inside this repo.

## Plugin settings visibility

Every plugin setting MUST either be editable in the plugin's settings UI, or be marked
INTERNAL (debug/tuning only). Hidden settings that change user-visible behavior are
forbidden. Dead settings (fields the code never reads) are forbidden: when adding a
setting, verify the code reads it; when removing a feature, remove its settings with it.

## Architectural decisions

Before implementing a larger architectural decision, check ROADMAP/open issues and
recent journal/PRs for already-planned work in the area. On a conflict between a user
instruction and documented planning: STOP and ask which applies. Never build parallel
systems already slated for replacement. Changing an architectural decision (e.g. vault
format, plugin mechanism, XML persistence) requires asking first.
