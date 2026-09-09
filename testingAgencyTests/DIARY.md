# SnaKt testing-agency diary

These tests are exploratory checks kept out of CI. Each case covers one topic and is
run only occasionally with `./testingAgencyTests/run-test.sh <case-name>`.

| Topic | Test | Last run (UTC) | Result | Bugs |
| --- | --- | --- | --- | --- |
| Existential quantifier over `Boolean` | `exists_boolean_witness` checks that returning `true` proves a postcondition requiring some true Boolean witness. | 2026-09-09 19:08 | Failed: SnaKt emitted `VIPER_VERIFICATION_ERROR` because the witness was not proved. | [#297](https://github.com/JetBrains/SnaKt/issues/297) |

## Candidate topics

- Existential quantifiers over nullable reference values.
- Existential quantifiers nested inside universal quantifiers.
- Quantifier trigger validation and malformed trigger diagnostics.
- Integer overflow behavior at `Int.MIN_VALUE` and `Int.MAX_VALUE`.
- Smart casts after compound Boolean conditions.
- Aliasing across unique-value function calls.
- Loop invariants with `break` and `continue` paths.
- Pure functions used inside preconditions and postconditions.
- String indexing boundary conditions.
- Inheritance and overridden method contracts.

## Bugs found

### 2026-09-09 — Boolean existential witness is not proved

`exists_boolean_witness` expects `exists<Boolean> { it }` to hold. The function
returns `true`, which is a direct witness, but verification reports that the
postcondition might not hold. Recorded as [#297](https://github.com/JetBrains/SnaKt/issues/297)
with label `bugFound`.
