# Lessons Learned

Pitfalls from real development (this repo + generalizable classes adopted from
adaptive-learner). Add a lesson when a bug came from a wrong pattern, a workaround for a
library limitation was found, or a workflow gap surfaced.

## Claimed work is not executed work

Output that LOOKS like executed changes is not evidence anything happened: text that
resembles tool calls, or a tool whose output reads like success (a pre-commit hook can
roll a commit back while every line above says "Passed"). Before building on any claimed
change — from another session, a human, or your own previous step — verify the artifact,
not the narrative: `git status`, `git log -1` (did HEAD move?), the gate's own exit code.

### After a merge: is the issue closed, AND did the change land?

A merged PR proves a merge happened, not that the work arrived. Check the issue state and
the branch CONTENT (`git log origin/develop -- <path>`, read the diff). A squash freezes
the branch at merge time — a push made after the merge is silently lost while everything
reads as success. A guessed closing keyword can close a foreign issue.

## Atomic commits are bounded by "green individually", not "one thing"

Each commit is the smallest reversible unit that leaves the tree green. When splitting
creates a broken intermediate state (source deletes what tests still import), the split
is wrong — combine. Conceptual split is a goal; green-individually is the constraint.

## Test a tool through the interface it actually uses

A tool that shells out (git, gradle, a CLI) implicitly depends on the environment it
resolves — cwd, repo root, PATH. Derive the repo root from cwd
(`git rev-parse --show-toplevel`), never from a script's own path — the script-path
approach silently reads the wrong checkout under `git worktree`, and a guard that
inspects the wrong tree reports green ("nothing staged, nothing to check"). Build real
throwaway fixtures (`@TempDir` git repos, keystores, vault files) and invoke the tool
the way the app does.

## A "flaky" test that fails deterministically on unchanged code is stale, not flaky

Before trusting a "flaky" label or adding retries/timeouts: re-run in isolation and
watch the wall clock (deterministic + fast = not a timeout flake), grep the asserted
component's consumers (an element with no consumer never renders), and
`git log <last-tag>..HEAD -- <spec>` (empty = pre-existing, not your diff). When a
feature is removed by design, delete/update its tests in the SAME change.

## Proposed mass scripts are inspected, not executed

A suggested global `sed`/regex sweep: read the matches, map each to its real target,
apply one by one, diff the result. Sweeps that look reasonable in the proposal have bent
different references onto one target.

## Real-data audit BEFORE implementation

When a feature ships with a heuristic, detection rule, or threshold: run the prediction
against real data before writing code, report counts + sample cases, treat the spec as a
starting hypothesis. (Local precedent: the 2026-08-25 legacy-vault migration bug —
"reads legacy.mcrdb, saves source.mcrdb" — only surfaced through an end-to-end test with
a real legacy file, not through unit tests.)

## Gitignored config + stale committed example = silent drift

Every gitignored config that has a committed `.example`/template must be checked when
the config schema changes — CI reads the example, developers read the real file.

## `make test` is not the gate CI runs - and reproducing that gate locally is not the fix

`test` compiles and runs tests; CI runs `build`, which also runs `spotlessJavaCheck` and the full
e2e suite. A branch can be green locally through every test and still turn develop red on
formatting alone - but chasing that by rerunning the full `build` locally (Xvfb + fluxbox, many
minutes) burns local resources on verification GitHub Actions already does on every push. For
everyday changes: run `spotlessApply` before committing (cheap, catches the most common CI-only
failure), then push and let CI run the real gate - react to what it reports instead of blocking
the push on a local rerun. Exception: a release cut, where `release-workflow.md`'s full local gate
stays mandatory (nothing to react to after tagging).

## Run UI e2e tests locally with the Xvfb harness

The full Swing e2e suite needs Xvfb + fluxbox + JDK 25 + `forkEvery=1`
(`make test-e2e`). Running against the live `:0` display hangs.

## mystic-crypt simple obfuscation: `disentangle` is broken upstream

`SimpleObfuscatorExtensions.disentangle` is broken in the library; use
`disentangleBiMap(toCharacterBiMap(rules), text)` instead.

## Dependency bumps: menu-action 5.1 is blocked

menu-action 5.1 pulls model-data up to 3.2.1 and breaks swing-tree-component 3.1.
mystic-crypt 11 + BC jdk18on are in and green (commit 8959046).
