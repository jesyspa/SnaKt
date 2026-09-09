# SnaKt testing agency diary

The launch count is intentionally kept low. Each probe covers one topic and is
not part of CI. A blank last-run field means the topic is queued for a future
agency session.

## Test inventory

| Topic | Probe | What it checks | Launches | Last launched (UTC) | Result |
| --- | --- | --- | ---: | --- | --- |
| Existential quantifier translation | `existential-quantifier-translation.sh` | An `exists` contract is converted to Viper existential syntax. | 1 | 2026-09-09 11:00 | Passed (2 harness tests) |
| Existential quantifier impurity | `existential-quantifier-impurity.sh` | A method call in an `exists` body is rejected with a purity diagnostic instead of crashing. | 1 | 2026-09-09 11:00 | Passed (1 harness test) |
| Universal quantifier translation | Planned | A `forAll` contract is translated with the correct bound variable and body. | 0 | — | Queued |
| Quantifier triggers | Planned | Explicit trigger terms survive conversion to Viper. | 0 | — | Queued |
| Integer overflow modes | Planned | Overflow policy is reflected in generated arithmetic checks. | 0 | — | Queued |
| Nullable smart casts | Planned | A null check provides the permissions needed for later dereference. | 0 | — | Queued |
| Loop invariant preservation | Planned | A loop body preserves a declared invariant. | 0 | — | Queued |
| Function preconditions | Planned | An invalid call site produces the expected precondition failure. | 0 | — | Queued |
| Function postconditions | Planned | A false postcondition is reported at the return path. | 0 | — | Queued |
| String indexing bounds | Planned | Out-of-bounds string access is rejected by verification. | 0 | — | Queued |
| Local variable assignment | Planned | Assignment updates the translated local value. | 0 | — | Queued |
| Unsupported feature behavior | Planned | Configured unsupported syntax fails or becomes unreachable as selected. | 0 | — | Queued |

## Bugs found

No bugs found so far.

## Run log

Runs are appended here after execution, including failures and any linked bug
issue.

### 2026-09-09 11:00 UTC

- Ran `existential-quantifier-translation.sh` once with Java 21: 2 harness
  tests passed and no golden files changed.
- Ran `existential-quantifier-impurity.sh` once with Java 21: 1 harness test
  passed and no golden files changed.
- The environment initially supplied Java 25.0.2, which stopped Gradle before
  any test executed. Retried with a temporary Java 21 runtime because SnaKt
  targets JVM 21. This was an environment setup failure, not a SnaKt bug.
- Pre-push verification: Gradle `check` passed with Z3 4.8.7 and the testData
  checks passed. The pre-commit wrapper could not run because `pre-commit` is
  absent and the environment's Python package proxy rejected installation with
  HTTP 403; `git diff --check` passed.
- Bugs found: none.
