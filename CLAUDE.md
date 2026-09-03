# mystic-crypt-ui

Java 25 Swing desktop password manager and crypto toolbox, built on the astrapi69
library family (mystic-crypt, crypt-data, crypt-api) and BouncyCastle. Gradle build
(wrapper), Spotless formatting, JUnit 5, PIT mutation testing, izpack installer.

## Architecture in short

Swing UI delegates to support/worker classes; all crypto primitives come from the
libraries, never hand-rolled. Features live in internal plugins under `plugins/`
(canonical inventory: the `plugins:` target in the Makefile), each with its own submenu
and settings. `--cli` delegates to the mystic-crypt library's picocli root command —
new CLI capability is built in the library first. Vault format: versioned
`PasswordVaultFormat` (MCRDB2, AEAD + PBKDF2) with legacy migration.

## Make targets

- `make build` / `make build-full` — build (full = with all checks)
- `make test` — the everyday gate; must stay green after every change
- `make test-e2e` — Swing e2e suite (needs Xvfb harness, see rules/lessons-learned.md)
- `make plugins` / `make plugins-install` — build/install internal plugins
- `make build-with-plugins` (alias `make bwp`) — `build` + `plugins-install` together
- `make run` / `make bootRun` — run the app
- `make spotless-java` — format before committing
- `make izpack-installer` — installer
- `make jacoco-report`, `./gradlew pitest` — coverage / mutation reports
- `make dependency-updates` — dependency currency check

## Workflow

Gitflow: `develop` active, `master` = releases. Issue before fix, PR for every pushed
change (see `.claude/rules/ai-workflow/`). Conventional commits, no AI co-author
trailers. TDD for logic changes (`.claude/rules/tdd.md`).

## Rules

Binding rules live in `.claude/rules/`: vibe-coding (priorities, freeze),
coding-standards (Java/git), tdd, quality-checks (pyramid, gates, round-trip duty for
vault changes), architecture (plugin-first, layers), i18n (properties over hardcoded
text, host vs. per-plugin bundles), release-workflow, lessons-learned,
ai-workflow/ (issue-, PR-policy, implementation, documentation).

Session history and discussed-but-not-yet-built ideas: `.claude/JOURNAL.md`.
