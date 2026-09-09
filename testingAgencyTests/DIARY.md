# SnaKt testing agency diary

These tests are exploratory, manually launched, and deliberately excluded from CI. Each fixture covers one topic. `run-test.sh` caps each fixture at three successful launches; the hidden counter is committed so the limit survives scheduled runs.

## Topic backlog

- Existential quantifiers over non-integer primitive types.
- Nested quantifiers with shadowed lambda parameters.
- Quantifiers composed with short-circuit boolean operators.
- Quantifier purity diagnostics for property getters.
- Nullable values in preconditions and postconditions.
- Safe calls combined with user invariants.
- Loop invariants at zero iterations.
- Integer overflow boundaries in verified arithmetic.
- Function overload resolution inside specifications.
- Inherited property permissions in postconditions.

## Runs

### Boolean existential witness

- Fixture: `exists_boolean_witness.kt`
- Scope: conversion and full Viper verification of `exists<Boolean>` in a precondition, with the quantified witness tied to a function parameter.
- Expected: SnaKt emits a Boolean-typed existential and verifies the function without diagnostics.
- Last launched: 2026-09-09 21:08 UTC (`./testingAgencyTests/run-test.sh exists_boolean_witness`, full conversion and Viper verification).
- Successful launches: 1/3.
- Result: passed; the generated Viper used `exists anon: Bool` and Silicon verified the function.
- Bugs found: none.

## Automation notes

- `AUTOMATIONS.md` was requested but was absent from the checkout on 2026-09-09 at 21:00 UTC.
- The checkout image supplied only JBR 25.0.2 and no Z3. This run used Temurin 21.0.12.1 and the repository-required Z3 4.8.7 from temporary locations.
