# SnaKt Design Notes

This document explains key design decisions and mechanisms in the SnaKt formal verification plugin that may not be immediately obvious from the code.

---

## Type Safety and the Kotlin Type Checker

**Key Principle:** SnaKt runs as a compiler plugin **after** the Kotlin type checker has validated the code.

### Implications

1. **Type-incorrect code never reaches the verifier**
   - The Kotlin compiler rejects code like `broken("hello", 42)` where a generic function `fun <T> broken(x: T, y: T)` is called with incompatible argument types
   - SnaKt can assume all code it processes is type-correct according to Kotlin's type system
   - This significantly simplifies the verification task

2. **Type parameter erasure is less problematic than it appears**
   - When converting type parameters to `Any?` (see `ProgramConverter.kt:561-563`), we're not introducing unsoundness
   - The Kotlin type checker has already ensured type parameter constraints are satisfied
   - The verifier doesn't need to re-check type compatibility at call sites

3. **What still needs verification**
   - Functional correctness (pre/postconditions, invariants)
   - Runtime properties not checked by the type system (bounds checking, non-nullness beyond static checks)
   - Properties expressed in user specifications

### Example

```kotlin
fun <T> identity(x: T): T = x

// This won't compile, so verifier never sees it:
// val wrong: String = identity(42)

// This compiles and reaches verifier:
val correct: Int = identity(42)
// Verifier checks specifications, not type safety
```

**Conclusion:** Type safety is guaranteed by Kotlin, not by SnaKt. The verifier focuses on proving specifications correct.

---

## Loop Invariants Implementation

**Location:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ControlFlow.kt:82-128`

### How It Works

Loop invariants in SnaKt work correctly by placing them on the **continue label**. Here's the mechanism:

1. **Structure** (see `While` class):
   ```kotlin
   val continueLabel = LabelEmbedding(continueLabelName, invariants)
   val breakLabel = LabelEmbedding(breakLabelName)
   ```

2. **Generated Viper Code** (conceptually):
   ```
   continueLabel:                    // Invariants checked here (on jump)
     if (condition) {
       <body>
       goto continueLabel            // Jump checks invariants
     }
   breakLabel:
   assert invariants                 // Final assertion after loop
   ```

3. **Invariant Semantics** (see `LabelEmbedding.kt:25-27`):
   - Viper labels can have invariants attached
   - When you `goto` a label with invariants, they must hold (checked)
   - When execution reaches the label, invariants are assumed

4. **Verification Flow**:
   - **First iteration**: Jump to continueLabel checks invariants initially hold
   - **Loop body**: Executes with invariants assumed from the label
   - **Iteration end**: `goto continueLabel` checks invariants still hold after body
   - **After loop**: Final assertion verifies invariants hold when exiting

### Why This Works

This is equivalent to traditional loop invariant reasoning:
- **Initialization**: Checked on first jump to continue label
- **Preservation**: Checked on each `goto continueLabel` at iteration end
- **Assumption**: Invariants are assumed at the label, so available in loop body
- **Final state**: Asserted after break label to ensure they hold when loop exits

### Misleading TODO Comment

The comment at line 107 says:
```kotlin
// TODO: this logic can be rewritten back to invariants once the version of Viper is updated
```

This is misleading - the implementation **already works correctly** as loop invariants via the label mechanism. The final assertions (lines 108-112) are additional checks after the loop, not a workaround.

**Action Item:** Consider removing or clarifying this TODO to avoid confusion.

---

## Old Expressions

**Location:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/Invariant.kt:14-21`

### Semantics

The `old()` function in specifications refers to the value of an expression **at the start of the method**.

```kotlin
fun deposit(amount: Int) {
    postconditions<Unit> {
        balance == old(balance) + amount
    }
    balance += amount
}
```

### Why `old(obj).field` and `old(obj.field)` Are Equivalent

In SnaKt's current model:
- `old(obj).field` = "get the old value of `obj`, then access its field"
- `old(obj.field)` = "get the old value of `obj.field`"

These are the same because:
1. Objects are reference types - `obj` itself doesn't change, only its fields do
2. `old(obj)` returns the same reference as `obj` (the reference didn't change)
3. Therefore accessing `.field` on either gives the same result

### When They Could Differ (Not Currently Modeled)

They would differ if we modeled object snapshots:
- `old(obj).field` = field value in the current version of the object
- `old(obj.field)` = field value in the snapshot of the object

But SnaKt doesn't currently maintain object snapshots in this way - `old()` captures values, not object states.

**Conclusion:** The current implementation is correct for SnaKt's model. No issue here.

---

## Type Parameter Encoding Strategy

**Location:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt:561-563`

### Current Approach

```kotlin
type is ConeTypeParameterType -> {
    isNullable = true; any()
}
```

Type parameters are encoded as nullable `Any?` in the Viper representation.

### Why This Is Sound (Given Kotlin Type Checker)

1. **Type safety is pre-checked:** Kotlin compiler ensures type parameter constraints
2. **Uniform representation:** All type parameters map to the same Viper type
3. **Specifications are on values:** User-written contracts reason about values, not types
4. **Runtime types available:** When needed, can use `isOf` to check runtime types

### Limitations

1. **Cannot express type-dependent specifications:**
   ```kotlin
   fun <T : Comparable<T>> max(x: T, y: T): T {
       // Can't specify: result >= x && result >= y
       // because >= depends on T being Comparable
   }
   ```

2. **Cannot verify generic class invariants:**
   ```kotlin
   class Box<T>(val value: T) {
       // Can't specify properties that depend on T
   }
   ```

### Future Improvements

If we need to verify type-dependent properties:
- **Option 1:** Monomorphization - verify each instantiation separately
- **Option 2:** Encode types as domain values with axioms
- **Option 3:** Require explicit witness types for verification

**Current Status:** Acceptable for current use cases, but limits verification of generic abstractions.

---

## When to Add Design Notes

Add to this document when:
1. A design decision is non-obvious from the code
2. Something looks wrong but is actually correct (like loop invariants)
3. There are subtle dependencies on external guarantees (like Kotlin type checker)
4. Implementation differs from typical textbook approaches
5. There are known limitations with clear reasons

Keep notes:
- **Concise** - explain the decision, not the entire implementation
- **Justified** - explain why this approach was chosen
- **Specific** - reference exact files and line numbers
- **Current** - update when code changes

---

**Last Updated:** 2025-11-18
**Contributors:** Analysis based on code review and maintainer clarifications
