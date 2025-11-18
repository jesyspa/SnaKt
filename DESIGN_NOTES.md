# SnaKt Design Notes

This document explains key design decisions that may not be immediately obvious from reading the code.

---

## Type Safety and the Kotlin Type Checker

SnaKt runs as a compiler plugin after the Kotlin type checker has validated the code. This means all code reaching the verifier is already type-correct according to Kotlin's type system, and the verifier's job is proving user-specified properties (preconditions, postconditions, invariants) rather than re-checking type safety.

This has an important implication for type parameter encoding. When type parameters are converted to `Any?` in the Viper representation (`ProgramConverter.kt:561-563`), this doesn't introduce unsoundness because the Kotlin compiler has already ensured type parameter constraints are satisfied at all call sites. The limitation is that we can't express type-dependent specifications—for instance, we can't verify that `max<T : Comparable<T>>` returns a value `>= both arguments` because the `>=` operation depends on the `Comparable` constraint.

---

## Loop Invariants via Labels

Loop invariants are implemented by attaching them to the continue label (`ControlFlow.kt:82-128`). In Viper, labels can have invariants that are both checked when jumping to the label and assumed when execution reaches it. This gives us proper loop invariant semantics: invariants are checked before the first iteration (on the initial jump), assumed at the start of each iteration, and checked again at the end of each iteration (on the `goto continueLabel`).

The TODO comment at line 107 mentioning "can be rewritten back to invariants once Viper is updated" is misleading—the implementation already works correctly. The final assertions after the loop are additional checks, not a workaround.

---

## When to Add Design Notes

Add to this document when a design decision is non-obvious from the code, when something looks wrong but is actually correct, or when implementation relies on external guarantees (like the Kotlin type checker). Keep notes concise and reference specific files and line numbers.

---

**Last Updated:** 2025-11-18
