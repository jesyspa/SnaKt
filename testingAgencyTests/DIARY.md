# SnaKt testing agency diary

The testing-agency suite is opt-in and is not connected to CI. “Last launched”
uses UTC. Bugs belong both in the run notes below and in an issue labelled
`bugFound`; no bug has been observed yet.

## Topic inventory

| Topic | Test | What it checks | Last launched | Result |
| --- | --- | --- | --- | --- |
| Existential conversion | `existential-conversion.sh` | `exists<T>` is translated in preconditions, postconditions, and loop invariants with stable Viper output. | 2026-09-10 01:00 UTC | Passed (2/2 harness tests) |
| Existential verification | `existential-verification.sh` | Existential witnesses verify when trigger terms are available and expected warnings remain for documented solver-incompleteness cases. | 2026-09-10 01:00 UTC | Passed (2/2 harness tests) |
| Universal trigger verification | `universal-trigger-verification.sh` | `forAll<T>` expressions carrying explicit trigger terms retain their expected verification behavior. | 2026-09-10 01:00 UTC | Passed (1/1 harness test) |

## Run notes

### 2026-09-10 01:00 UTC

- Automation trigger: scheduled testing-agency run.
- Shared `AUTOMATIONS.md`: unavailable in this checkout; repository `AGENTS.md`
  and `docs/agents-dev.md` were followed instead.
- `existential-conversion.sh`: launched once successfully with Java 21; the
  selected compiler and locality harness tests both passed. An earlier launch
  reached no tests because the environment's default Java 25 was incompatible.
- `existential-verification.sh`: launched once successfully with Java 21 and Z3
  4.8.7; both selected harness tests passed. Two earlier attempts reached no
  SnaKt assertions because Z3 was respectively absent and non-executable.
- `universal-trigger-verification.sh`: launched once successfully with Java 21
  and Z3 4.8.7; its selected harness test passed. One earlier attempt reached no
  SnaKt assertions because the downloaded Z3 had not yet been made executable.
- Bugs: none found. The setup failures above are environment prerequisites, not
  SnaKt behavior, so no `bugFound` issue was created.

## Future topics

Each future test should cover exactly one of these topics (additional topics may
be added as SnaKt evolves):

- Quantifier variable shadowing and nested quantifiers.
- Existential quantifiers over non-`Int` supported types.
- Quantifiers combined with nullable values and smart casts.
- Quantifiers in pure functions.
- Quantifiers interacting with class invariants and heap permissions.
- Diagnostics for unsupported quantifier lambda shapes.
- Loop invariant preservation across `break` and `continue`.
- Gradle plugin option propagation.
- User-friendly versus original Viper error rendering.
- Unsupported-feature fallback behavior.
