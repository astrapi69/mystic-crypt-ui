# Vibe Coding Rules

Scope: every task in this repository. (Note: Claude Code loads every rule file
on every prompt regardless of any scope hint — keep this corpus small.)

## Short rules for every task

- **PROMPT-PRECISION**: reference existing patterns (internal plugin structure under
  `plugins/`, `FileCryptSupport`, `PasswordVaultFormat`, the mystic-crypt library CLI)
  instead of reinventing. Name file, class, expected behavior.

- **LAYER SEPARATION**: no business/crypto logic in Swing panels or actions. Panels and
  actions delegate to support/worker classes (`*Support`, `*Worker`, `crypto/` package);
  crypto primitives come from the mystic-crypt / crypt-data libraries, not hand-rolled.

- **TESTS**: every behavior change needs tests. `make test` must stay green after every
  change. User-visible UI changes need an e2e UI test (see quality-checks.md).

- **DEPENDENCIES**: no new dependencies without asking first, and a manual check on
  maintenance status and security. Prefer existing dependencies (the astrapi69 library
  family, BouncyCastle). Cryptographic dependencies get extra scrutiny.

- **REFACTORING**: split god-classes instead of whitelisting them.

- **GIT**: issue FIRST (see ai-workflow/github-issue-policy.md). `Closes #NN` in commits.
  Javadoc over inline comments. One concern per PR. Every pushed code change opens a PR
  (see ai-workflow/pr-policy.md).

## Priority (fixed)

1. Merge open PRs
2. P0/P1 bugs (security and data-loss first — this is a password manager)
3. Infrastructure (CI, gates, build)
4. UI fixes
5. Cleanup/refactoring
6. Features
7. Release

Foundation before features.

## Release freeze

When a release is being cut (release branch exists or tagging is in progress), until the
tag is pushed and published: no new PRs against develop, no merges to develop, no new
code — only release workflow. Exception: a P0 hotfix that blocks the release itself.
