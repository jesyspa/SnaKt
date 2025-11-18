# SnaKt Code Improvement Suggestions

This document identifies **structural issues and missing capabilities** in the SnaKt formal verification plugin, focusing on problems that affect correctness, completeness, and expressiveness of verification.

**Note:** This analysis excludes known work-in-progress areas (purity checking, uniqueness typing) and focuses on core architectural issues rather than TODO comments.

**Important Context:** SnaKt runs as a K2 compiler plugin **after** the Kotlin type checker. This means:
- Only type-correct code reaches the verifier
- Type safety is guaranteed by Kotlin, not re-checked by SnaKt
- The verifier focuses on proving user-specified properties (pre/postconditions, invariants)
- See `DESIGN_NOTES.md` for detailed explanations of design decisions

---

## Table of Contents

1. [Critical Soundness Issues](#1-critical-soundness-issues) 🔴
2. [Major Architectural Issues](#2-major-architectural-issues) ⚠️
3. [Missing Kotlin Language Features](#3-missing-kotlin-language-features)
4. [Specification Language Limitations](#4-specification-language-limitations)
5. [Type System Gaps](#5-type-system-gaps)
6. [Missing Viper Capabilities](#6-missing-viper-capabilities)

---

## 1. Critical Soundness Issues 🔴

These issues can cause the verifier to accept incorrect programs or reject correct ones.

### 1.1 Limited Support for Type-Dependent Specifications

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt:561-563`

**Issue:** Type parameters are erased to `Any?` in the Viper encoding.

```kotlin
type is ConeTypeParameterType -> {
    isNullable = true; any()
}
```

**Why This Is Sound (for now):**
- The Kotlin type checker runs first and rejects type-incorrect code
- Code like `broken("hello", 42)` won't compile, so verifier never sees it
- Type safety is guaranteed by Kotlin, not re-checked by SnaKt

**Actual Limitation:** Cannot express **type-dependent specifications**:

```kotlin
fun <T : Comparable<T>> max(x: T, y: T): T {
    // CANNOT specify: result >= x && result >= y
    // because >= depends on T being Comparable
    postconditions<T> { result -> /* ??? how to use Comparable */ }
    return if (x > y) x else y
}
```

**Also Cannot Verify:**
- Generic class invariants that depend on type parameters
- Properties of collections with specific element types
- Relationships between type parameters in multi-parameter generics

**Related:** `ClassTypeEmbedding.kt:14` - class type parameters also not incorporated.

**Impact:** ⚠️ **Limits expressiveness**, not a soundness issue. Generic code can be verified for type-independent properties only.

**Priority:** **MEDIUM** - Important for verifying generic abstractions, but workarounds exist (verify concrete instantiations).

---

### 1.2 Shared Expression Mutable Variable Bug

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/Meta.kt:86-89`

**Issue:** Documented bug where shared expressions with mutable variables produce incorrect results.

```kotlin
/**
 * This solution has some bugs: if a shared expression references a variable and that variable is modified
 * between the shared parts, the second occurrence may have a different value than the first.
 */
```

**Example:**

```kotlin
var counter = 0
fun increment(): Int { return ++counter }

// This should call increment() once, but shared expression may call it twice
val result = increment() ?: 0
```

If the expression is evaluated twice in Viper, the verification model diverges from runtime behavior.

**Fix Required:**
- Always store shared expressions in fresh temporaries when they contain mutable state
- Or analyze purity and only share when provably pure
- Document current limitation if fixing is infeasible

**Priority:** 🔴 **CRITICAL** - Can cause both false positives and false negatives in verification.

---

### 1.3 Unsafe Type Casts Unchecked

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/TypeOp.kt:54-65`

**Issue:** Type casts (`as`) don't verify that the cast is actually valid.

```kotlin
override fun toViper(ctx: LinearizationContext) = inner.toViper(ctx)
// Line 55: // TODO: Do we want to assert `inner isOf type` here before making a cast itself?
```

**Soundness Impact:**

```kotlin
@AlwaysVerify
fun unsafeExample(obj: Any): String {
    postconditions<String> { result -> result.length > 0 }
    return obj as String  // No verification that obj is actually a String!
}
```

If verification doesn't check casts, type safety is compromised.

**Fix Required:** Emit `assert inner isOf targetType` before the cast in Viper encoding.

**Priority:** 🔴 **CRITICAL** - Type safety violation.

---

### 1.4 Equality Doesn't Call equals() Method

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionVisitor.kt:210-213`

**Issue:** Uses structural comparison instead of calling `equals()`.

```kotlin
private fun convertEqCmp(left: ExpEmbedding, right: ExpEmbedding): ExpEmbedding {
    //TODO: replace with call to left.equals()
    return EqCmp(left, right)
}
```

**Correctness Impact:**

```kotlin
data class Person(val name: String, val age: Int)

@AlwaysVerify
fun comparePeople(p1: Person, p2: Person): Boolean {
    return p1 == p2  // Should use equals(), currently uses reference equality
}
```

For data classes and custom types, this gives wrong results.

**Fix Required:**
- Dispatch to `equals()` for non-primitive types
- Keep structural equality for Int, Boolean, etc.
- May require encoding `equals()` contracts

**Priority:** 🔴 **HIGH** - Incorrect semantics for common operations.

---

## 2. Major Architectural Issues ⚠️

These issues significantly limit what can be verified but don't necessarily cause unsoundness.

### 2.1 No Field Access in Specifications

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/purity/ExpPurityVisitor.kt:52-53`

**Issue:** Field access always considered impure.

```kotlin
override fun visitFieldAccess(e: FieldAccess): Boolean = false // TODO
override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): Boolean = false // TODO
```

**Impact:** Cannot write specifications that reference object state:

```kotlin
class BankAccount(private var balance: Int) {
    @AlwaysVerify
    fun deposit(amount: Int) {
        preconditions {
            amount > 0
            // CANNOT WRITE: balance >= 0  ← field access not allowed
        }
        postconditions<Unit> {
            // CANNOT WRITE: balance == old(balance) + amount
        }
        balance += amount
    }
}
```

This is a **major limitation** - most useful specifications need to reference fields!

**Note:** This is marked as WIP (purity system), but it's such a fundamental limitation it's worth highlighting.

**Priority:** ⚠️ **HIGH** - Severely limits expressiveness of specifications.

---

### 2.2 Smart Cast Inhales Too Many Invariants

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionVisitor.kt:427`

**Issue:** When smart-casting `B` to `A`, inhales ALL invariants of `A`, even ones already implied by `B`.

```kotlin
// TODO: when there is a cast from B to A, only inhale invariants of A - invariants of B
exp.withNewTypeInvariants(newType) { access = true }
```

**Impact:**
- Performance: Unnecessary invariants slow verification
- Potential unsoundness: If invariants contradict, could cause spurious failures
- Especially problematic with diamond inheritance

**Example:**
```kotlin
interface Counted { /* invariant: count >= 0 */ }
class Counter : Counted { /* invariant: count >= 0 (inherited) */ }

val obj: Counted = Counter()
if (obj is Counter) {
    // Smart cast inhales count >= 0 AGAIN, even though obj: Counted already has it
}
```

**Fix Required:** Track which invariants are already known and only inhale new ones.

**Priority:** ⚠️ **MEDIUM** - Performance issue, potential correctness issue.

---

### 2.3 No User-Defined Predicates

**File:** `formver.compiler-plugin/viper/src/org/jetbrains/kotlin/formver/viper/ast/Predicate.kt`

**Issue:** Predicates exist in Viper AST but only auto-generated for classes. No user API to define custom predicates.

**Impact:** Cannot define abstractions for complex data structures.

**Example:** Want to write:

```kotlin
// Hypothetical syntax - NOT currently supported:
@Predicate
fun validLinkedList(node: Node?): Boolean {
    node == null || (node.next == null || validLinkedList(node.next))
}

@AlwaysVerify
fun append(head: Node?, elem: Int) {
    preconditions { validLinkedList(head) }
    postconditions<Node> { result -> validLinkedList(result) }
    // ...
}
```

Without predicates, must inline invariants everywhere, leading to:
- Code duplication
- Verification timeout on recursive structures
- Inability to hide representation details

**Fix Required:** Design annotation/syntax for user predicates and expose in conversion.

**Priority:** ⚠️ **HIGH** - Essential for verifying realistic data structures.

---

## 3. Missing Kotlin Language Features

These Kotlin features have no support in the verifier, limiting what code can be verified.

### 3.1 Data Classes

**Evidence:** No test cases, no special handling in conversion.

**Missing:**
- `copy()` method verification
- `component1()`, `component2()`, ... for destructuring
- Auto-generated `equals()`/`hashCode()`/`toString()`

**Impact:** Cannot verify code using data classes (very common in Kotlin).

**Example:**
```kotlin
data class Point(val x: Int, val y: Int)  // ← Can't verify

fun distance(p: Point): Int {
    val (x, y) = p  // ← Destructuring won't work
    return x * x + y * y
}
```

---

### 3.2 Sealed Classes

**Evidence:** No test cases, no handling in type system.

**Missing:**
- Exhaustiveness checking in `when` expressions
- Type hierarchy modeling

**Impact:** Cannot verify code with sealed class hierarchies.

**Example:**
```kotlin
sealed class Result<T>
data class Success<T>(val value: T) : Result<T>()
data class Error(val message: String) : Result<Nothing>()

fun <T> unwrap(result: Result<T>): T = when (result) {
    is Success -> result.value
    is Error -> throw Exception(result.message)
    // Compiler knows this is exhaustive, but verifier doesn't
}
```

---

### 3.3 Companion Objects and Object Declarations

**Evidence:** No test cases, no conversion support.

**Impact:** Cannot access singletons or companion object members.

**Example:**
```kotlin
object MathConstants {
    const val PI = 3.14159
}

class Counter {
    companion object {
        private var nextId = 0
        fun generateId(): Int = nextId++
    }
}

// Both of these won't work in verified code
```

---

### 3.4 Operator Overloading Beyond Basics

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/OperatorExpEmbeddings.kt`

**Supported:** Only `+`, `-`, `*`, `/`, `%`, comparisons on `Int`/`Char`/`String`, and basic boolean logic.

**Missing:**
- `invoke()` - Function objects (partially blocked by `LambdaExp.kt:34` lambda storage TODO)
- `get`/`set` - Array/collection indexing (except `String.get`)
- `contains` - Membership testing
- `rangeTo` - Range creation
- `iterator` - For-loop support
- Unary operators: `++`, `--`, unary `+`, `-`
- All user-defined operator overloads

**Impact:** Cannot verify:
```kotlin
data class Matrix(val data: Array<IntArray>) {
    operator fun get(i: Int, j: Int) = data[i][j]  // Won't work
    operator fun plus(other: Matrix): Matrix = ...  // Won't work
}
```

---

### 3.5 Property Delegation

**Evidence:** No `by lazy`, `by Delegates` in tests or conversion.

**Impact:** Cannot verify delegated properties.

**Example:**
```kotlin
class Example {
    val expensive: String by lazy {
        // Won't work
        computeExpensiveValue()
    }

    var observed: Int by Delegates.observable(0) { _, old, new ->
        // Won't work
        println("Changed from $old to $new")
    }
}
```

---

### 3.6 Type Aliases

**Evidence:** No handling in type embedding.

**Impact:** Type aliases not recognized during verification.

---

### 3.7 Collections and Arrays

**Evidence:** No `arrayOf`, `listOf`, `setOf`, `mapOf` in special functions.

**Impact:** Cannot create or verify operations on standard collections.

**Example:**
```kotlin
fun sumArray(arr: IntArray): Int {  // ← IntArray not supported
    var sum = 0
    for (x in arr) {  // ← Iteration not supported
        sum += x
    }
    return sum
}

fun firstElement(list: List<Int>): Int {  // ← List not supported
    return list[0]  // ← Indexing not supported
}
```

---

### 3.8 Destructuring Declarations

**Evidence:** No `component1`/`component2` in test cases.

**Impact:** Cannot verify destructuring:

```kotlin
val (x, y) = point  // Won't work
val (name, age) = person  // Won't work
```

---

### 3.9 Coroutines

**Evidence:** No `suspend` functions in tests.

**Impact:** Cannot verify coroutines at all. This is probably acceptable given the complexity, but worth documenting.

---

## 4. Specification Language Limitations

These limit what properties can be expressed in verification conditions.

### 4.1 Single-Variable Quantifiers Only

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ForAllEmbedding.kt:19`

**Issue:** ForAll supports only one variable.

```kotlin
// TODO: support multiple variables
val variable: VariableEmbedding,
```

**Impact:** Must nest quantifiers awkwardly:

```kotlin
// Want to write:
forAll { i, j -> i + j == j + i }

// Must write instead:
forAll { i -> forAll { j -> i + j == j + i } }
```

This is:
- Verbose and error-prone
- May impact verification performance (double quantification overhead)
- Harder to specify triggers (if we add them)

---

### 4.2 No User-Specified Triggers

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ForAllEmbedding.kt:28`

**Issue:** No way to specify triggers for quantifier instantiation.

```kotlin
// TODO: right now we hope that Viper will infer triggers successfully, later we might enable user triggers here
triggers = emptyList(),
```

**Impact:**
- Automatic trigger inference can fail
- When it fails: verification becomes incomplete (can't prove valid properties)
- Or: verification becomes slow/non-terminating (too many instantiations)
- No user control to fix these issues

**Example where triggers matter:**
```kotlin
forAll { x: Int -> x * 0 == 0 }
// Needs trigger [x * 0] to instantiate
// But Viper might not infer this
```

**Priority:** ⚠️ **MEDIUM** - Affects verification completeness and performance.

---

### 4.3 No Magic Wands

**Evidence:** Magic wands exist in Viper AST but not exposed to users.

**Impact:** Cannot express conditional permissions or complex separation logic patterns.

**Example use case (not supported):**
```kotlin
// Hypothetical: temporarily give away permission, get it back later
fun processCallback(obj: Object, callback: (Object) -> Unit) {
    // Give callback permission to obj --* Get permission back after callback returns
}
```

This is advanced, but some verification scenarios need it.

---

### 4.4 No Fold/Unfold Exposed

**Evidence:** Predicates managed implicitly.

**Impact:** No fine-grained control over when predicates are folded/unfolded. This can cause:
- Verification failures when verifier doesn't automatically unfold
- Need for user hints in complex scenarios

---

## 5. Type System Gaps

### 5.1 No Generic Type Parameters (See Section 1.1)

Already covered as critical soundness issue.

---

### 5.2 No Variance Support

**Evidence:** No handling of `in`/`out` variance in type conversion.

**Impact:** Cannot soundly verify covariant/contravariant types.

**Example:**
```kotlin
class Box<out T>(val value: T)  // Covariant T

fun processBox(box: Box<Number>) {
    // Should accept Box<Int> since Int <: Number and Box is covariant
}
```

Without variance, invariance is assumed, rejecting valid code.

---

### 5.3 Platform Types from Java Interop

**Evidence:** No handling found.

**Impact:** Calling Java code (platform types with unknown nullability) is unsafe.

---

### 5.4 Nothing and Unit Types

**Status:** ✓ Handled correctly.

**Files:**
- `PretypeEmbedding.kt:36-46` - Definitions
- `RuntimeTypeDomain.kt:394` - Uniqueness axiom for Unit

---

### 5.5 Nullable Types

**Status:** ✓ Well-handled with sound encoding.

**File:** `RuntimeTypeDomain.kt:208-414`

The nullable type encoding appears sound:
- Proper axioms for null smart casts
- Idempotent nullable operation
- Correct typing of null

---

## 6. Missing Viper Capabilities

These Viper features exist but aren't exposed to SnaKt users.

### 6.1 User-Defined Domains

**Evidence:** Only `RuntimeTypeDomain` exists, no user API.

**Impact:** Cannot add theory axioms for custom data structures.

**Example use case:**
```kotlin
// Hypothetical - NOT supported
@Domain
object SetTheory {
    @DomainFunction
    fun empty(): Set

    @DomainFunction
    fun insert(s: Set, x: Int): Set

    @DomainAxiom
    fun insertContains(s: Set, x: Int) {
        assert(contains(insert(s, x), x))
    }
}
```

Without domains, must encode set operations manually, which is inefficient.

---

### 6.2 Permissions Beyond Basic Access

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/SpecialFields.kt:20-22`

**Issue:**
```kotlin
object SpecialFields {
    val all: List<SpecialField> = listOf()  // Empty!
}
```

Limited permission reasoning - no special fields defined.

**Impact:** Cannot express fractional permissions, read permissions, etc.

---

### 6.3 Method Contracts vs Function Axioms

**Observation:** Not clear when to emit Viper `method` vs `function`.

**File:** `RichCallableEmbedding.kt:19`
```kotlin
/**
 * TODO: The Function header will likely be removed as it makes no sense to emit a function header on its own
 */
```

**Impact:** Design unclear, may limit what can be verified.

---

## Summary and Priorities

### 🔴 Critical (Must Fix for Soundness)

1. **Shared expression bug** (Section 1.2) - Causes incorrect verification results
2. **Unchecked casts** (Section 1.3) - Type safety violation
3. **Wrong equality semantics** (Section 1.4) - Incorrect behavior for custom types

### ⚠️ High Priority (Severely Limits Usefulness)

4. **No field access in specs** (Section 2.1) - Can't write contracts referencing object state
5. **No user predicates** (Section 2.3) - Can't verify recursive structures
6. **Data classes not supported** (Section 3.1) - Very common in Kotlin
7. **Collections not supported** (Section 3.7) - Essential data structures

### 📋 Medium Priority (Expand Capabilities)

8. **Type-dependent specifications** (Section 1.1) - Can't verify generic type constraints
9. Smart cast over-approximation (Section 2.2)
10. Sealed classes (Section 3.2)
11. Operator overloading (Section 3.4)
12. Multi-variable quantifiers (Section 4.1)
13. User triggers for quantifiers (Section 4.2)
14. Type variance (Section 5.2)
15. User-defined domains (Section 6.1)

### 📝 Low Priority (Nice to Have)

16. Companion objects (Section 3.3)
17. Property delegation (Section 3.5)
18. Type aliases (Section 3.6)
19. Magic wands (Section 4.3)
20. Explicit fold/unfold (Section 4.4)

---

## Methodology

This analysis was based on:
- Deep code review of conversion, type embedding, and linearization logic
- Systematic search for test cases covering each Kotlin feature
- Comparison with Kotlin language specification
- Review of Viper AST vs exposed user features
- Tracing through example scenarios to identify gaps
- Corrections from maintainer feedback on initial findings

---

**Document Version:** 3.0 (Corrected)
**Date:** 2025-11-18
**Focus:** Structural issues and missing capabilities (excluding WIP: purity, uniqueness)

**Corrections Applied:**
- Loop invariants work correctly via continue label mechanism (Section 2.1 removed)
- Type parameter erasure is not a soundness issue given Kotlin type checker guarantee (Section 1.1 updated)
- `old()` expressions work correctly in current model (Section 4.3 removed)
- See `DESIGN_NOTES.md` for detailed explanations of design decisions
