# Bericht: Übernahme der `.claude`-Regeln aus adaptive-learner

Datum: 2026-08-25
Quelle: `/home/astrapi69/dev/git/hub/astrapi69/adaptive-learner/.claude` (26 Dateien, ca. 5.100 Zeilen)
Ziel: `mystic-crypt-ui/.claude/rules/` (vorher: kein `.claude`-Verzeichnis, kein `CLAUDE.md`)

## Zusammenfassung

adaptive-learner ist ein Python/FastAPI + React/TypeScript-Projekt; mystic-crypt-ui ist eine
Java-25-Swing-Anwendung mit Gradle 9.7, Spotless, JUnit 5, PIT-Mutationstests und internen
Plugins. Etwa die Hälfte des Regelwerks ist stack-unabhängige Arbeitsdisziplin (Issue-Pflicht,
PR-Pflicht, TDD, Gate-Prinzipien, Doku-Protokoll) und wurde übernommen bzw. auf Java/Gradle
angepasst. Die andere Hälfte ist an FastAPI/React/Dexie/Tailwind gebunden und wurde bewusst
NICHT übernommen.

Wichtige Erkenntnis aus dem Quell-Regelwerk selbst (dort mit Messung belegt, #2091): jede
Datei unter `.claude/rules/` wird bei JEDEM Prompt in JEDE Session injiziert. Der Korpus ist
laufender Kontext-Kostenfaktor, keine Bibliothek. Deshalb wurde bei der Übernahme stark
kondensiert: aus ca. 5.100 Zeilen Quelle wurden ca. 700 Zeilen Ziel-Regeln.

## Übernommen (angepasst auf Java/Gradle/Swing)

| Ziel-Datei | Quelle | Anpassung |
|---|---|---|
| `.claude/rules/vibe-coding.md` | `vibe-coding.md` | Prioritätenreihenfolge, Release-Freeze, Dependency-Kontrolle, Prompt-Präzision übernommen; Layer-Regel auf Swing (keine Logik in UI-Panels) umformuliert |
| `.claude/rules/coding-standards.md` | `coding-standards.md` | Naming/Format auf Java + Spotless (Eclipse-Formatter) umgestellt; Conventional Commits, Gitflow (develop aktiv, master = Releases), Funktions-Design (max. 40 Zeilen, eine Verantwortung), DRY, Boy-Scout-Regel, Verbot generischer Namen, kein `--amend`+Force-Push auf offene PRs, keine AI-Co-Author-Trailer — alles 1:1 übernommen |
| `.claude/rules/tdd.md` | `tdd.md` | Red-Green-Refactor, Vier-Tests-pro-Feature-Ziel (Repro/Happy/Edge/Boundary), Bugfix = Repro-Test zuerst; Kommandos auf `./gradlew test` / `make test` umgestellt; parametrisierte Tests via JUnit 5 `@ParameterizedTest` |
| `.claude/rules/quality-checks.md` | `quality-checks.md` | Testpyramide (Unit/Integration/Swing-E2E/PIT-Mutation), Coverage-Ziele nach Modultyp, Mutations-Interpretation (kritischer Code: Tests nachziehen, trivialer: ignorieren); die vier Gate-Prinzipien übernommen: Gate schlägt fehl statt zu warnen, fail-closed, meldet was es gemessen hat, „wired ≠ working" |
| `.claude/rules/architecture.md` | `architecture.md` (Teilmenge) | Plugin-first-Regel („neue Features gehören in ein Plugin, außer Core"), Plugin-Settings-Sichtbarkeit (jede Einstellung editierbar in UI oder als INTERNAL markiert; tote Settings verboten), CLI-Backlink-Prinzip (UI-CLI delegiert an mystic-crypt-Library), Schichtung UI → Support/Worker → Library |
| `.claude/rules/ai-workflow/github-issue-policy.md` | dito | Fast wörtlich — Quelle erklärt sich selbst für „ALL repositories" gültig. Issue vor Fix, Reopen statt Duplikat, `Closes #NN`, Issue-Queue-Arbeitsmodus |
| `.claude/rules/ai-workflow/pr-policy.md` | dito | PR-PFLICHT übernommen: jeder gepushte Code-Change bekommt einen PR gegen `develop`, „wurde nicht verlangt" zählt nicht; Ausnahmen (kein Code-Change, Release-Freeze, explizites Opt-out) |
| `.claude/rules/ai-workflow/implementation-workflow.md` | dito | Session-Start (git log, `make test` als grüne Baseline), Reihenfolge für Features und Plugins auf die interne Plugin-Struktur umgeschrieben, „Not allowed"-Liste (keine neuen Dependencies ohne Rückfrage, keine Tests schwächen, nichts „für später" bauen, bei Unklarheit stoppen statt raten) |
| `.claude/rules/ai-workflow/documentation-protocol.md` | dito | Kondensiert: CLAUDE.md-Pflegeregeln (schlank halten), CHANGELOG-Pflicht, Single-Source-of-Truth für volatile Zahlen, Zahlen-Verifikationspflicht (jede numerische Behauptung im selben Kontext per Kommando prüfen), Selbstklärungsregel (Evidenz → TODO-Marker → nur bei Blockade fragen) |
| `.claude/rules/release-workflow.md` | `release-workflow.md` | Stark gekürzt auf den Gradle-Weg: Version in `gradle.properties` (`projectVersion`) als einzige Quelle, CHANGELOG-Gruppierung, kompletter Gate-Lauf vor Tag (`make test`, e2e, PIT-Sichtung, `make build-full`, Installer-Smoke), `make tag-release`, Grundregeln (rote Tests blocken, kein Release mitten im Feature) |
| `.claude/rules/lessons-learned.md` | `lessons-learned.md` + `lessons/core.md` (Auswahl) | Nur die stack-unabhängigen Pitfall-Klassen: behauptete Arbeit ≠ ausgeführte Arbeit (Artefakt prüfen, nicht Narrativ), nach Merge Inhalt UND Issue-Status prüfen, atomare Commits = „einzeln grün", Tool über echtes Interface testen (Repo-Root aus cwd, nie aus `__file__`/Skript-Pfad — gilt genauso für Gradle-Task-Skripte), „flaky" das deterministisch fehlschlägt ist stale, Massen-sed inspizieren statt ausführen, Real-Daten-Audit vor Implementierung; plus vorhandene projektspezifische Lessons (Xvfb-E2E, disentangle-Bug, menu-action-5.1-Blocker) |
| `CLAUDE.md` (neu, Wurzel) | Muster aus `documentation-protocol.md` | Schlanker Projekt-Einstieg: Stack, Make-Targets, Plugin-Modell, Verweis auf `.claude/rules/` |

## Bewusst NICHT übernommen

| Quelle | Grund |
|---|---|
| `design-tokens.md` | CSS-Variablen/Tailwind/Themes — Swing hat kein Token-System; kein Gegenstück |
| `architecture.md` (Großteil) | FastAPI-Layering, Repository-Pattern, Dexie-Dual-Storage, TipTap, React-Routen — stack-fremd |
| `code-hygiene.md` (Großteil) | ruff/ESLint/Prettier/pre-commit-Configs, ApiError/Toast-Ketten. Übernommen wurden nur die Prinzipien (Fehler auf richtiger Schicht, kein Exception-Schlucken, keine generischen Fehlermeldungen, Javadoc statt Inline-Kommentaren) — eingearbeitet in `coding-standards.md` |
| `checks.yaml`, `gates.yaml` + Verifikations-Skripte | Maschinerie (verify_gate_rule_links, Korpus-Ratchet, body_sha) hängt an deren Makefile/CI. Die PRINZIPIEN (Gate↔Regel gekoppelt, Checks nur deklariert abschaltbar, Korpus klein halten) stehen in `quality-checks.md`; die Tooling-Nachrüstung wäre ein eigenes Vorhaben |
| `ai-workflow/testplan-policy.md` | mystic-crypt-ui hat keinen manuellen Testplan. Stattdessen in `implementation-workflow.md`: jede sichtbare UI-Änderung braucht einen E2E-UI-Test (das existierende Äquivalent) |
| `ai-workflow/test-coverage-audits.md` | Audit-Dateikonventionen an deren `docs/audits/` gebunden; Kernidee (Audit nach Feature-Phase / vor Release) als ein Absatz in `quality-checks.md` |
| `lessons/backend.md`, `lessons/frontend.md`, `lessons/content-storage.md`, `lessons/docs-i18n.md`, `lessons/ci-gates.md`, `lessons/release-packaging.md` | Fast vollständig projektspezifisch (Alembic, TipTap, Dexie, Vite, Poetry, install.sh). Einzelne generalisierbare Regeln (gitignorte Config + stale Example, Modul-Level-Caches in Tests, „frozen artifact beweisen") sind als Kurzform in `lessons-learned.md` gelandet |
| `prompts/audit.md` | An deren Audit-Dateistruktur gebunden; bei Bedarf später eigenes Audit-Prompt bauen |
| `settings.json` / `settings.local.json` | Reine Permission-Allowlists mit adaptive-learner-Pfaden und -Kommandos — nicht übertragbar |
| BACKUP-AKZEPTANZTEST, Visual-Baseline-Gate, Feature-Screenshots | An deren Playwright/GH-Actions-Infrastruktur gebunden. Der Geist („manueller Roundtrip mit echten Daten ist das Gate, nicht der Unit-Test") steht als Regel für Datenbank-/Vault-Format-Änderungen in `quality-checks.md` — passend, weil mystic-crypt-ui gerade das MCRDB2-Vault-Format eingeführt hat |

## Konfliktpunkte / Entscheidungen

1. **Co-Author-Trailer**: Quelle verbietet AI-Co-Author-Trailer in Commits. Übernommen — die
   Commits dieser Übernahme folgen dem bereits.
2. **Gitflow**: Quelle nutzt `develop`/`main`; mystic-crypt-ui nutzt `develop`/`master`.
   Regeln entsprechend auf `master` formuliert. Bisher wurde hier oft direkt auf `develop`
   committet; die PR-PFLICHT ändert den Arbeitsmodus. Falls unerwünscht: Regel in
   `ai-workflow/pr-policy.md` streichen oder abschwächen.
3. **Frontmatter (`alwaysApply`/`globs`)**: laut Quell-Beleg (2026-07-28, #2089) lädt Claude
   Code JEDE Regel-Datei unabhängig vom Frontmatter. Die übernommenen Dateien tragen daher
   kein Frontmatter — Scope steht als eine Zeile Klartext am Dateianfang.
4. **Korpus-Größe**: bewusst ~700 statt 5.100 Zeilen. Erweiterungen sollten als Tausch
   gedacht werden (etwas kondensieren, wenn etwas dazukommt), nicht als reine Addition.
