# SnaKt testing diary

## Coverage catalogue

| Topic | Agency test | Run limit | Last launched (UTC) | Last result | Notes / bugs |
| --- | --- | ---: | --- | --- | --- |
| Existential quantifier translation and verification | `existential-translation.sh` | 3 | 2026-09-09 15:00 | Passed (final launch) | Covers preconditions, postconditions, loop invariants, and expected verifier incompleteness diagnostics. All 3 permitted launches used: two exposed missing test dependencies before execution; the dependency-complete launch passed. |
| Impure expressions in existential bodies | `existential-purity-rejection.sh` | 3 | 2026-09-09 15:00 | Passed | Regression probe produced the expected purity diagnostic instead of a compiler crash. 2 of 3 permitted launches used; the first stopped because Z3 was absent. |
| Universal quantifier translation | Planned | — | Never | Not run | Candidate future single-topic probe. |
| Quantifier trigger translation | Planned | — | Never | Not run | Candidate future single-topic probe. |
| Integer overflow modelling | Planned | — | Never | Not run | Candidate future single-topic probe. |
| Loop invariant preservation | Planned | — | Never | Not run | Candidate future single-topic probe. |
| Nullable smart casts in specifications | Planned | — | Never | Not run | Candidate future single-topic probe. |
| List bounds and standard-library replacements | Planned | — | Never | Not run | Candidate future single-topic probe. |
| Uniqueness use-after-move diagnostics | Planned | — | Never | Not run | Candidate future single-topic probe. |
| Locality of captured values | Planned | — | Never | Not run | Candidate future single-topic probe. |

## Run log

Runs are append-only. A golden-matching run is only a regression result; the
expected diagnostics must still be inspected before classifying it as healthy.

### 2026-09-09 15:00 UTC

- Shared guidance check: `AUTOMATIONS.md` was not present in the checkout or
  elsewhere under `/workspaces`; the repository `AGENTS.md` instructions were
  followed.
- Existential translation/verification: launch 1 stopped before tests because
  Java 25.0.2 was incompatible with the build; launch 2 reached the test but
  could not start the verifier because Z3 was absent; launch 3 used Java
  21.0.11 and the project-documented Z3 4.8.7, and both generated phases
  passed. The existing expected verifier warnings were inspected and are the
  documented quantifier-instantiation incompleteness cases, not new bugs.
- Impure existential body rejection: the first launch stopped because Z3 was
  absent. The second used Java 21.0.11 and Z3 4.8.7; the single generated test
  passed and retained the expected diagnostic that method calls are unsupported
  in quantifier bodies.
- Bugs found: none. No `bugFound` issue was created.
