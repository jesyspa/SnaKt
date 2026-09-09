# SnaKt testing agency diary

The checks here are exploratory/manual and are deliberately excluded from CI.
Each check covers exactly one topic. Times are UTC.

## Test inventory

| Topic | Check | Run limit | Last launched | Result |
| --- | --- | ---: | --- | --- |
| Existential quantifier purity: `List.get` in the body is rejected without a compiler crash | `tests/existential-list-get-purity.sh` | 3 | 2026-09-09 23:00 | Blocked before test execution: Gradle cannot run on Java 25.0.2 |
| Existential quantifier lowering for a trivial integer witness | Planned | — | Never | Not run |
| Explicit trigger lowering for universal quantifiers | Planned | — | Never | Not run |
| Loop invariant preservation across `continue` | Planned | — | Never | Not run |
| Nullable smart-cast use in postconditions | Planned | — | Never | Not run |
| Integer overflow behavior in arithmetic specifications | Planned | — | Never | Not run |
| Calls through overridden methods with inherited contracts | Planned | — | Never | Not run |
| Safe-call and Elvis expression conversion | Planned | — | Never | Not run |

## Launch log

### 2026-09-09 23:00 — Existential quantifier purity with `List.get`

- Check: `tests/existential-list-get-purity.sh`
- Intended behavior: SnaKt reports the impure method call in the existential
  body as a purity violation and does not crash during conversion.
- Launches this run: 1 (the tracked counter permits at most 3 total launches).
- Result: blocked before test execution. Gradle 8.14.3 rejected the active Java
  25.0.2 runtime. No compatible JDK was installed, and attempts to fetch a
  temporary JDK 21 from Adoptium and Microsoft were denied by the environment's
  network proxy (HTTP 403). This is an environment limitation, not a SnaKt bug.

## Bugs found

No bugs found so far.

## Automation notes

- `AUTOMATIONS.md` was not present in the repository or adjacent workspace at
  the start of the 2026-09-09 23:00 run, so no shared automation instructions
  could be applied.
- Validation: the check script passed `bash -n`, and the repository testData
  consistency check passed. `check-all.sh` could not complete: `gradle check`
  failed on Java 25.0.2 and `pre-commit` was not installed.
