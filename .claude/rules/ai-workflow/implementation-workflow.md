# Implementation Workflow

## Session start

1. Review recent changes: `git log --oneline -10`.
2. Run `make test` (establish a green baseline) unless the session is docs-only.
3. Only then start on the task.

Fresh-worktree precondition: a new `git worktree add` has no build state; the first
`make test` there does the full Gradle build. Never mask exit codes with `| tail`; after
every commit verify HEAD moved (`git log -1`) — a hook can roll a commit back while
printing "Passed".

## Order for new features

1. Check whether the feature belongs in a plugin or in the core (architecture.md).
2. Look at existing patterns (e.g. how `plugins/file-crypt-plugin` is structured).
3. Model/support layer first (testable without Swing), TDD per tdd.md.
4. UI wiring (panel, action, menu contribution).
5. CLI capability goes into the mystic-crypt library first, then is consumed here.
6. Unit + integration tests; for every user-visible UI change at least one e2e UI test
   (the `*UiTest` suites are this repo's manual-testplan equivalent).
7. Conventional commit, push, PR against `develop` (PR-PFLICHT).

## Order for new plugins

1. Scaffold `plugins/{name}-plugin/` following an existing plugin.
2. Wire the Makefile target `plugin-{name}` + `plugins:` aggregate, the izpack installer
   config, and the test framework in the same change.
3. Submenu + settings (visible or INTERNAL, architecture.md), tests, e2e test.

## Not allowed (AI-specific)

- Introduce new dependencies without asking first.
- Change architectural decisions (vault format, plugin mechanism, persistence) without
  asking.
- Change mystic-crypt / crypt-data library code from inside this repo (separate repos,
  separate release cycles).
- Generate code "for later". Only what is needed now.
- Delete, comment out, weaken, or `@Disabled` existing tests to make `make test` green.
- Hand-roll crypto where the libraries provide it.
- In autonomous mode, guess when something is unclear. Stop and document the
  uncertainty instead.
