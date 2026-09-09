# SnaKt testing-agency diary

These tests are exploratory, deliberately bounded, and are not wired into CI.
Each script covers exactly one topic. Before launching one, check its launch
budget below and record the result after it finishes.

## Test inventory

| Test | Topic | Launch budget | Launches used | Last launched (UTC) | Latest result |
| --- | --- | ---: | ---: | --- | --- |
| `existential_quantifier_verification.sh` | Existential quantifiers survive Kotlin-to-Viper conversion and the verification pipeline | 3 | 3 | 2026-09-09 17:06 | Inconclusive: environment prevented verification; launch budget exhausted |

## Run log

### 2026-09-09 17:00 UTC — existential quantifier verification

- Scope: the dedicated `exists.kt` fixture, including conversion of existential
  expressions and the verifier's expected outcomes.
- Command: `./testingAgencyTests/existential_quantifier_verification.sh`
- Launch 1: no tests ran because the environment's Java 25 runtime was not
  accepted by the build.
- Launch 2: with the bundled Java 21 runtime, conversion passed but verification
  could not start because Z3 was not installed.
- Launch 3: with the documented Z3 4.8.7 downloaded, conversion passed but
  verification could not start because archive extraction did not preserve the
  executable bit.
- Result: inconclusive. The test's three-launch budget is exhausted; do not run
  this script again. No SnaKt verification assertion failed.
- Bugs found: none. All failures were test-environment prerequisites, so no
  `bugFound` issue was created.
- Shared automation instructions: `AUTOMATIONS.md` was not present in this
  checkout, so no shared instructions could be applied.

## Candidate topics for later runs

Each candidate should get its own single-topic test script before it is run:

- quantified-expression trigger selection
- nullable smart-cast verification
- loop-invariant preservation
- non-local return conversion
- generic type conversion
- string indexing bounds
- Gradle plugin configuration diagnostics
