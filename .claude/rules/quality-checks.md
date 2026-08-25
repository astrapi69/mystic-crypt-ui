# Quality Checks and Test Strategy

## Test pyramid

```
        /  E2E   \      Swing UI tests (AssertJ-Swing shape), few, critical flows
       / Integration\   whole features through real files/keystores (tmp dirs)
      /  Unit Tests  \  JUnit 5, business logic in isolation
     / Mutation (PIT) \ verifies tests actually catch bugs
```

- Everyday gate: `make test`. Must stay green after every change.
- E2E: `make test-e2e` — needs Xvfb + fluxbox + JDK 25 + forkEvery=1; running against a
  live `:0` display hangs (see lessons-learned.md).
- Mutation testing: `./gradlew pitest` (report: `build/reports/pitest`), CI via
  `mutation.yml`. Run nightly/before release, not per commit.
  Surviving mutants in critical code (crypto, vault format, key handling): add tests
  immediately. In trivial code (formatting, logging): ignore or document.

## Coverage targets per module type

| Module type | Target |
|---|---|
| Crypto/vault format code (`crypto/`, `PasswordVaultFormat`, `*Support`) | HIGH (>= 80%), mutation-hardened |
| Plugin logic | HIGH (>= 80%) |
| Workers/actions (thin delegation) | MEDIUM (>= 60%) |
| Swing panels | via e2e flows, not line coverage |
| Data-critical flows (vault save/load, migration, export) | MUST-HAVE e2e |

100% is not the goal; meaningful coverage is. Tests assert real behavior properties;
regression pins for known bug classes count more than line count.

## Round-trip acceptance for vault/format changes (MANDATORY)

Any change to the vault format (`PasswordVaultFormat`, MCRDB2, migration paths) or to
file encryption is proven by a REAL round-trip, not only unit tests: create/encrypt with
real data, reopen/decrypt, verify content — through the real app path (e2e test or manual
run), including the legacy-migration path when touched. Origin (adopted from
adaptive-learner's BACKUP-AKZEPTANZTEST): five consecutive "fixed" backup releases had
green unit tests and no working round-trip; synthetic fixtures miss schema drift and
serialization edge cases. Also verified locally here: the 2026-08-25 migration bug
(reads legacy.mcrdb, saves source.mcrdb) survived green unit tests.

## Test naming

`methodUnderTest_expectedOutcome_whenCondition` or JUnit 5 `@DisplayName` — the name
describes the behavior, not the implementation. Test through the real interface: build
real temp files/keystores in `@TempDir`, invoke tools the way the app invokes them;
mocks only for genuinely external things.

## Gate principles (adopted, tooling not yet ported)

1. **A gate that cannot check must never report green** (fail closed): missing config,
   crashed helper, empty file set — none of these are "nothing to find".
2. **A check that warns instead of failing enforces nothing.** Disabling a check is
   allowed only declared, with a reason visible in the diff — never silently.
3. **A gate reports WHAT it measured** (set sizes); "0 findings" and "0 files scanned"
   must not print the same green.
4. **Wired is not working**: a new CI workflow, hook, or scheduled job is triggered at
   least once end-to-end in the same session it is wired, and the first run's result is
   recorded. Untriggered infrastructure is a hypothesis, not a feature.

## Rule corpus has a cost

Every file under `.claude/rules/` is injected into every prompt of every session. A new
rule section is a trade, not an addition: condense or delete something first, or justify
the growth in the commit message.

## Coverage audits

After a major feature phase or before a release, run a focused coverage/gap audit over
the changed area (`make jacoco-report` for numbers); on request, a full one.
