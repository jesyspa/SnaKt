# SnaKt Code Improvement Suggestions

This document identifies structural issues and missing capabilities in the SnaKt formal verification plugin, focusing on problems affecting correctness, completeness, and expressiveness. It excludes known work-in-progress areas (purity checking, uniqueness typing).

**Context:** SnaKt runs after the Kotlin type checker, so type safety is already guaranteed. The verifier focuses on proving user-specified properties. See `DESIGN_NOTES.md` for design decisions.

---

## Critical Soundness Issues 🔴

### Shared Expression Mutable Variable Bug

**File:** `Meta.kt:86-89`

Shared expressions (used for `?.` and `?:`) may evaluate twice if they reference mutable variables, causing verification results to diverge from runtime behavior. Consider:

```kotlin
var counter = 0
fun increment(): Int = ++counter

val result = increment() ?: 0
```

The Viper encoding might evaluate `increment()` twice, getting different values for the two occurrences of the shared expression.

**Fix:** Store shared expressions in temporaries when they contain mutable state, or restrict sharing to provably pure expressions.

**Priority:** 🔴 **CRITICAL**

---

### Unchecked Type Casts

**File:** `TypeOp.kt:54-65`

Type casts (`as`) don't verify the cast is valid—they just change the type in the Viper encoding. This allows writing:

```kotlin
@AlwaysVerify
fun unsafeExample(obj: Any): String {
    postconditions<String> { result -> result.length > 0 }
    return obj as String  // No check that obj is actually a String!
}
```

**Fix:** Emit `assert inner isOf targetType` before casts.

**Priority:** 🔴 **CRITICAL**

---

### Equality Uses Structural Comparison

**File:** `StmtConversionVisitor.kt:210-213`

The `==` operator uses Viper's structural equality instead of calling `equals()`, giving wrong results for data classes and custom types:

```kotlin
data class Person(val name: String, val age: Int)

fun comparePeople(p1: Person, p2: Person): Boolean {
    return p1 == p2  // Uses reference equality, not equals()!
}
```

**Fix:** Dispatch to `equals()` for non-primitive types while keeping structural comparison for `Int`, `Boolean`, etc. May require encoding `equals()` contracts.

**Priority:** 🔴 **HIGH**

---

## Major Architectural Issues ⚠️

### No Field Access in Specifications

**File:** `ExpPurityVisitor.kt:52-53`

Field access is always considered impure, preventing specifications that reference object state:

```kotlin
class BankAccount(private var balance: Int) {
    fun deposit(amount: Int) {
        preconditions { amount > 0 }
        postconditions<Unit> {
            // CANNOT WRITE: balance == old(balance) + amount
        }
        balance += amount
    }
}
```

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

Predicates exist in Viper AST but are only auto-generated for classes. Users can't define custom predicates for recursive data structures. For example, there's no way to write:

```kotlin
// Hypothetical syntax - NOT supported:
@Predicate
fun validLinkedList(node: Node?): Boolean {
    node == null || (node.next == null || validLinkedList(node.next))
}
```

This forces users to inline invariants everywhere, preventing verification of realistic data structures like linked lists or trees.

**Priority:** ⚠️ **HIGH**

---

## Missing Kotlin Language Features

### Data Classes

No support for `copy()`, component functions (`component1()`, `component2()`, ...), or auto-generated methods. This means code like:

```kotlin
data class Point(val x: Int, val y: Int)

fun translate(p: Point): Point {
    val (x, y) = p  // Destructuring won't work
    return p.copy(x = x + 1)  // copy() won't work
}
```

cannot be verified.

---

### Sealed Classes

No exhaustiveness checking or hierarchy modeling, so the verifier can't verify that all cases are handled:

```kotlin
sealed class Result<T>
data class Success<T>(val value: T) : Result<T>()
data class Error(val message: String) : Result<Nothing>()

fun <T> unwrap(result: Result<T>): T = when (result) {
    is Success -> result.value
    is Error -> throw Exception(result.message)
    // Compiler knows this is exhaustive, verifier doesn't
}
```

---

### Collections and Arrays

No support for `List`, `Array`, `Set`, `Map`, or operations on them:

```kotlin
fun sumArray(arr: IntArray): Int {  // IntArray not supported
    var sum = 0
    for (x in arr) {  // Iteration not supported
        sum += x
    }
    return sum
}
```

This is a major limitation for real-world code.

---

### Other Missing Features

**Companion Objects:** Cannot access singleton objects or companion members.

**Operator Overloading:** Only basic arithmetic, comparison, and boolean logic on `Int`/`Char`/`String`. Missing: `invoke()`, `get`/`set`, `contains`, `rangeTo`, `iterator`, unary operators, all user-defined operators.

**Property Delegation:** No support for `by lazy`, `Delegates.observable`, etc.

**Miscellaneous:** Type aliases, destructuring declarations, coroutines all unsupported.

---

## Specification Language Limitations

### Single-Variable Quantifiers

`forAll` only supports one variable, requiring awkward nesting:

```kotlin
// Want to write:
forAll { i, j -> i + j == j + i }

// Must write instead:
forAll { i -> forAll { j -> i + j == j + i } }
```

This is verbose and may impact verification performance.

---

### No User Triggers

Quantifiers rely on Viper's automatic trigger inference, which can fail (causing incompleteness) or generate too many instantiations (causing timeouts). Users have no control to fix these issues when they occur.

---

### Other Limitations

**No Magic Wands:** Cannot express conditional permissions or complex separation logic patterns.

**No Explicit Fold/Unfold:** Predicates are managed implicitly, causing verification failures when automatic unfolding fails.

---

## Type System Gaps

### Limited Type-Dependent Specifications

Type parameters are erased to `Any?` (`ProgramConverter.kt:561-563`). While this is sound (Kotlin type checker ensures type safety), it prevents expressing specifications that depend on type constraints:

```kotlin
fun <T : Comparable<T>> max(x: T, y: T): T {
    // CANNOT specify: result >= x && result >= y
    // because >= depends on the Comparable bound
    postconditions<T> { result -> /* ??? */ }
    return if (x > y) x else y
}
```

Also can't verify generic class invariants that depend on type parameters, or properties that depend on collection element types.

---

### Other Type System Gaps

**No Variance Support:** Type variance (`in`/`out`) isn't handled, assuming invariance for all types.

**No Platform Types:** No handling of Java interop types with unknown nullability.

**Nullable Types:** Well-handled with sound encoding via the runtime type domain.

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
