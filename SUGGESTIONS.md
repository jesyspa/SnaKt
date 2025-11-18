# SnaKt Code Improvement Suggestions

This document identifies structural issues and missing capabilities in the SnaKt formal verification plugin, focusing on problems affecting correctness, completeness, and expressiveness. It excludes known work-in-progress areas (purity checking, uniqueness typing).

**Context:** SnaKt runs after the Kotlin type checker, so type safety is already guaranteed. The verifier focuses on proving user-specified properties. See `DESIGN_NOTES.md` for design decisions.

---

## Critical Soundness Issues 🔴

### Shared Expression Mutable Variable Bug

**File:** `Meta.kt:86-89`

Shared expressions (used for `?.` and `?:`) may evaluate twice if they reference mutable variables, causing verification results to diverge from runtime behavior. For example, `increment() ?: 0` might call `increment()` twice in the Viper encoding.

**Fix:** Store shared expressions in temporaries when they contain mutable state, or restrict sharing to provably pure expressions.

**Priority:** 🔴 **CRITICAL**

---

### Unchecked Type Casts

**File:** `TypeOp.kt:54-65`

Type casts (`as`) don't verify the cast is valid—they just change the type in the Viper encoding. This could allow a function to return `obj as String` without proving `obj` is actually a `String`.

**Fix:** Emit `assert inner isOf targetType` before casts.

**Priority:** 🔴 **CRITICAL**

---

### Equality Uses Structural Comparison

**File:** `StmtConversionVisitor.kt:210-213`

The `==` operator uses Viper's structural equality instead of calling `equals()`, giving wrong results for data classes and custom types.

**Fix:** Dispatch to `equals()` for non-primitive types while keeping structural comparison for `Int`, `Boolean`, etc. May require encoding `equals()` contracts.

**Priority:** 🔴 **HIGH**

---

## Major Architectural Issues ⚠️

### No Field Access in Specifications

**File:** `ExpPurityVisitor.kt:52-53`

Field access is always considered impure, preventing specifications like `postconditions { balance == old(balance) + amount }`. This severely limits what can be expressed in contracts.

**Note:** This is WIP (purity system) but is such a fundamental limitation it's worth highlighting.

**Priority:** ⚠️ **HIGH**

---

### Smart Cast Over-Approximation

**File:** `StmtConversionVisitor.kt:427`

Smart-casting from type `B` to `A` inhales all invariants of `A`, even those already implied by `B`. This causes unnecessary verification overhead and potential spurious failures with diamond inheritance.

**Fix:** Track which invariants are already known and only inhale new ones.

**Priority:** ⚠️ **MEDIUM**

---

### No User-Defined Predicates

**File:** `viper/ast/Predicate.kt`

Predicates exist in Viper AST but are only auto-generated for classes. Users can't define custom predicates for recursive data structures, forcing them to inline invariants everywhere and preventing verification of realistic data structures like linked lists or trees.

**Priority:** ⚠️ **HIGH**

---

## Missing Kotlin Language Features

**Data Classes:** No support for `copy()`, component functions, or auto-generated methods.

**Sealed Classes:** No exhaustiveness checking or hierarchy modeling.

**Companion Objects:** Cannot access singleton objects or companion members.

**Operator Overloading:** Only basic arithmetic, comparison, and boolean logic on `Int`/`Char`/`String`. Missing: `invoke()`, `get`/`set`, `contains`, `rangeTo`, `iterator`, unary operators, all user-defined operators.

**Collections and Arrays:** No support for `List`, `Array`, `Set`, `Map`, or operations on them. This is a major limitation for real-world code.

**Property Delegation:** No support for `by lazy`, `Delegates.observable`, etc.

**Other:** Type aliases, destructuring declarations, coroutines all unsupported.

---

## Specification Language Limitations

**Single-Variable Quantifiers:** `forAll` only supports one variable, requiring awkward nesting for multi-variable properties.

**No User Triggers:** Quantifiers rely on Viper's automatic trigger inference, which can fail (causing incompleteness) or generate too many instantiations (causing timeouts). No user control to fix these issues.

**No Magic Wands:** Cannot express conditional permissions or complex separation logic patterns.

**No Explicit Fold/Unfold:** Predicates are managed implicitly, causing verification failures when automatic unfolding fails.

---

## Type System Gaps

**Limited Type-Dependent Specifications:** Type parameters are erased to `Any?`. While this is sound (Kotlin type checker ensures type safety), it prevents expressing specifications that depend on type constraints. For instance, can't verify `max<T : Comparable<T>>` returns `result >= x && result >= y` because `>=` depends on the `Comparable` bound. Also can't verify generic class invariants or collection element properties.

**No Variance Support:** Type variance (`in`/`out`) isn't handled, assuming invariance for all types.

**No Platform Types:** No handling of Java interop types with unknown nullability.

**Nullable Types:** These are well-handled with sound encoding via the runtime type domain.

---

## Missing Viper Capabilities

**User-Defined Domains:** Only `RuntimeTypeDomain` exists. Users can't add theory axioms for custom data structures like mathematical sets or sequences.

**Limited Permissions:** `SpecialFields` is empty—no fractional permissions, read permissions, or other advanced permission reasoning.

**Method vs Function Unclear:** Not clear when to emit Viper `method` vs `function`, and the design is still in flux.

---

## Summary and Priorities

**Critical:** Shared expression bug, unchecked casts, wrong equality semantics.

**High Priority:** No field access in specs, no user predicates, data classes, collections/arrays.

**Medium Priority:** Type-dependent specifications, smart cast over-approximation, sealed classes, operator overloading, multi-variable quantifiers, user triggers, type variance, user-defined domains.

**Low Priority:** Companion objects, property delegation, type aliases, magic wands, explicit fold/unfold.

---

**Version:** 3.0 (Corrected)
**Date:** 2025-11-18
