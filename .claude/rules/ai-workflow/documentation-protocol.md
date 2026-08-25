# Documentation Protocol

## CLAUDE.md stays lean

CLAUDE.md is loaded on every prompt (target: under 8000 characters). Only always-relevant
content: project description, stack keywords, architecture in 2-3 sentences, Makefile
targets, plugin model, pointers to `.claude/rules/`. NOT in CLAUDE.md: full directory
trees, dependency tables with versions, per-endpoint/per-class listings, completed-phase
history.

Update it when: a plugin is added/removed, the stack changes, new Make targets, the
architecture shifts.

## CHANGELOG.md

Update immediately when a user-facing change lands — do not accumulate. Group entries
(Added / Changed / Fixed / Security), summarize meaningfully, never paste raw commit
messages.

## Single source of truth for volatile numbers

Numbers that change per session (test counts, plugin counts, coverage) live in ONE
canonical place (the build output, the Makefile plugin list) — docs carry the pointer,
not the number. Norms do not age; stale counts inside instructions read as specification.

## Numeric claims verification

Any numeric claim about the project (test counts, plugin counts, LOC, "N passed") must
be verified by running the authoritative command in the same session it is reported —
not from memory, not inferred, not from an old journal. If it cannot be run, mark the
number "as of <date>". A number the user states in conversation is a starting point,
not authoritative — re-verify before echoing it into any document or commit.

## Self-clarification rule

When a question arises mid-task that context cannot answer, do NOT guess:

1. **Answer it from evidence in the repo** (git history, adjacent files, existing
   patterns) and note the basis in the final report.
2. **Park it with a marker**: take the most conservative assumption, mark the spot with
   `TODO(clarify): <question>`, continue.
3. **Stop and ask ONLY if it blocks meaningful progress** or risks a destructive change.

The final report includes a "Questions and assumptions" section listing parked questions,
evidence-based answers with sources, and any blockers. No silent guess ever ships.

## Communication

Direct, factual, no sugar-coating. If something violates the architecture: say so, do not
silently work around it. Mark suggestions as suggestions.
