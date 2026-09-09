# SnaKt testing agency diary

This diary tracks isolated, opt-in exploratory tests. A test is launched only
when its row is updated; none of these tests are part of CI.

| Topic | Test | Last launched (UTC) | Launch count | Result | Bugs |
| --- | --- | --- | ---: | --- | --- |
| Existential quantifier with an explicit string-access trigger | `cases/exists_explicit_trigger/exists_explicit_trigger.kt` | 2026-09-09 13:11 | 2 | Passed fast conversion: 1/1 | None |

## Candidate topics

- Existential quantifiers nested under universal quantifiers
- Explicit multiple triggers on existential quantifiers
- Existential predicates over strings and arrays
- Preconditions and postconditions involving integer overflow boundaries
- `old` expressions over mutable fields
- Loop invariants across `break` and `continue`
- Permission fractions using `read()` and `write()`
- Manual folding and unfolding of uniqueness predicates
- Nullable unique values and smart casts
- Gradle plugin target-selection option precedence
- User-friendly versus original Viper error rendering
- Unsupported-feature behavior modes

## Bug log

No bugs found yet.

## 2026-09-09 13:11 UTC

- Tested conversion of one existential quantifier containing one explicit
  string-access trigger.
- Final command: `./testingAgencyTests/run-one.sh exists_explicit_trigger`.
- Final result: one test passed. The generated Viper contains an `exists`
  expression whose trigger is the corresponding string-index expression.
- Launch count is two: the first agency-runner launch exposed an incomplete
  test fixture (`VIPER_TEXT` marker missing), and the corrected second launch
  passed. Earlier environment/setup attempts did not execute this final test.
- No SnaKt bug was found; no issue was created.
