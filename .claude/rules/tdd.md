# Test-Driven Development (TDD)

Workflow rule for writing code. Test strategy (pyramid, coverage, mutation testing)
lives in quality-checks.md.

## Mandatory for code changes with logic

"With logic" means: a new behavior, a changed code path, a condition, a calculation,
a validation, a mapping. Pure mechanics fall under the exceptions below.

1. **RED**: write a test that describes the desired change. It MUST fail first.
   No production code before the failing test.
2. **GREEN**: write only the code that makes the test green. YAGNI — no code "for later".
3. **REFACTOR**: clean up smells, duplication, naming (Boy Scout Rule). Tests stay green.

## Test count per feature/fix

Floor for trivial new methods: happy path + one error case.
Target for a real feature or fix — four tests that together secure the behavior:

1. **Reproduction test** — the RED test before the fix/feature.
2. **Happy path** — the expected normal case.
3. **Edge cases** — empty/missing/unexpected inputs (wrong password, corrupt file,
   empty vault).
4. **Boundary values** — the edges of the valid range (key sizes, iteration counts,
   max lengths).

Edge + boundary cases are the standard case for ONE `@ParameterizedTest` with speaking
display names — no copied test methods, and no if-cascades on the parameter inside the
test body (a case needing its own setup is its own test).

No artificial tests for counting — every test checks a real behavior property.

## Bug fixes

ALWAYS write the reproducing test first (RED, proves the bug), then fix until GREEN.
The reproduction test stays in the repo as a regression guard. No fix without an
understood cause.

## Exceptions

TDD is NOT enforced for: pure docs, pure configuration (CI, Makefile, Gradle files)
without logic, mechanical refactors with existing coverage (splits, renames — the
existing suite must stay green), and visual/manual-only Swing aspects not reachable by
the e2e harness. The exceptions never exempt from "`make test` must stay green after
every change".
