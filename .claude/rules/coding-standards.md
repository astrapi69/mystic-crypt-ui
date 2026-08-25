# Coding Standards

Scope: all Java sources (main app + `plugins/`).

## General

Developer: Asterios Raptis (solo developer, AI-assisted).
Goal: pragmatic, maintainable, quickly deliverable. No over-engineering.
When unclear: ask rather than guess.

## Java

- Java 25, Gradle (wrapper: `./gradlew`), version catalog in `gradle/libs.versions.toml`.
- Formatting via Spotless (Eclipse formatter config under `src/test/resources/spotless/`).
  Run `make spotless-java` (or `./gradlew spotlessApply`) before committing — never
  hand-format against the formatter.
- Javadoc for public classes and methods. License header on every file
  (`make license-format` / Spotless licensing).
- JUnit 5 for tests. AssertJ-style fluent assertions where already used.
- Crypto: use mystic-crypt / crypt-data / crypt-api and BouncyCastle. NEVER hand-roll
  primitives, padding, IV/salt handling, or comparisons (timing) — wrap library calls.
- No `System.out.println` for user feedback; UI feedback via dialogs/components, developer
  diagnostics via the existing logging.

## Naming

- PascalCase classes, camelCase methods/fields, UPPER_SNAKE constants.
- Plugin folders: `plugins/{name}-plugin/` (kebab-case), matching Makefile target
  `plugin-{name}`.
- No generic names: `data`, `info`, `result`, `temp`, `item`, `obj`, `val`, `tmp`, `x`
  are forbidden. Use `keyPairInfo`, `encryptionResult`, `vaultEntry`. Exception: loop
  indices and short lambdas.
- No I-prefix for interfaces.

## Formatting (prose + code)

- No em-dash (Unicode U+2014) in code or docs; use hyphens or commas.
- No emojis in code or comments.
- German user-facing text uses real umlauts (ä/ö/ü/ß), never ae/oe/ue transliteration.

## Git

- Conventional Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:` with a
  scope when clear (`feat(keystore): ...`).
- One commit per logical change. Atomic = smallest reversible unit that leaves the tree
  green individually — when a source edit and its test edit would break apart, they land
  in ONE commit (see lessons-learned.md).
- Gitflow: `develop` is the active branch; `master` holds releases (tags vX.Y.Z).
  Branch `feature/*` / `fix/*` / `chore/*` FROM `develop`, PRs AGAINST `develop`.
- No `--amend` + force-push on an open PR — add a new commit instead (an amend can desync
  the PR head and silently drop the amended change on merge).
- Do not add `Co-Authored-By` trailers attributing non-human collaborators (AI tools,
  bots). Exceptions require an explicit note in the commit body.

## Function design and cohesion

- Every method has exactly one responsibility. Max ~40 lines; over 50 is an immediate
  refactoring signal.
- Methods that do multiple things (parse AND save, validate AND transform) get split.
- "// Step 1 / Step 2" comments inside one method = low cohesion; each step is a method.
- One abstraction level per method: high-level methods call low-level helpers, never mix
  UI wiring with byte-level crypto in one method.
- Guard clauses at the top instead of deep nesting. Validate inputs early (crash early).
- Shared data between methods: a small record/class, not loose Maps passed around.
- Hard to test is a design verdict, not a testing problem: if a method needs a pile of
  mocks, decompose it — helpers take plain parameters, no Swing types in logic classes.

## DRY and Boy Scout

- Same logic or constant in two places: extract/centralize. Three duplicates: refactor now.
- Leave code cleaner than found; if you touch a method that violates these rules, fix the
  violation along with it.

## Error handling

- Handle errors at the right layer: library/worker code throws meaningful exceptions;
  UI layer catches and shows the user an actionable dialog.
- No `catch` without logging or handling. Never swallow an exception.
- No bare `catch (Exception e)` where a specific type exists.
- Generic error messages ("Encryption failed") without the reason are forbidden — the
  message must make a GitHub issue actionable without follow-up questions. But NEVER leak
  secrets (passwords, keys, plaintext) into messages or logs.

## Documentation: Javadoc over inline comments

- Prefer self-explanatory names; put explanation in Javadoc, not inline comments.
- Forbidden: comments explaining WHAT the code does, commit-message-style comments,
  authorship markers ("added by AI"), commented-out code (git keeps history).
- Allowed: `TODO(#NN):` with issue reference, a short WHY note for a genuinely
  non-obvious spot (a tricky algorithm step, a workaround for a library quirk).

## Security

- Never commit keys, keystores with real keys, passwords, or test vaults with real data.
- `char[]` for passwords where the API allows; clear sensitive buffers when feasible.
- Validate user-supplied files (size, format) before parsing.
