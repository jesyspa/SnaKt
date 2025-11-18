# User-Defined Triggers in forAll

This document demonstrates how to use user-defined triggers in `forAll` quantifiers.

## Overview

Triggers are expressions that guide SMT solvers in when to instantiate quantifiers. Previously, SnaKt relied entirely on Viper's automatic trigger inference. Now you can explicitly specify trigger expressions using the `triggers()` function.

## Basic Usage

### Simple Trigger

```kotlin
@AlwaysVerify
fun example(): Int {
    postconditions<Int> { res ->
        forAll<Int> { it ->
            triggers(it * it)  // Specify trigger expression
            it * it >= 0
        }
    }
    return 0
}
```

### Multiple Triggers

You can provide multiple trigger expressions:

```kotlin
@AlwaysVerify
fun example(): Int {
    postconditions<Int> { res ->
        forAll<Int> { it ->
            triggers(it * it, it + 1)  // Multiple triggers
            (it != 0) implies (it * it >= res)
        }
    }
    return 1
}
```

### Triggers in Loop Invariants

Triggers work in loop invariants as well:

```kotlin
@AlwaysVerify
fun example(str: String): Int {
    var i = 0
    while (i < str.length) {
        loopInvariants {
            forAll<Int> {
                triggers(str[it])  // Trigger on array access
                (0 <= it && it < str.length) implies (str[it] != 'x')
            }
        }
        i++
    }
    return i
}
```

### Backward Compatibility

The `triggers()` function is optional. If you don't specify triggers, Viper will automatically infer them as before:

```kotlin
@AlwaysVerify
fun example(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            // No triggers specified - automatic inference
            it * it >= 0
        }
    }
    return 0
}
```

## Implementation Details

### API

The `triggers()` function is added to the `InvariantBuilder` class:

```kotlin
class InvariantBuilder {
    fun triggers(vararg expressions: Any?): Unit
}
```

### Conversion

- When processing a `forAll` block, the compiler separates `triggers()` calls from regular invariants
- Each argument to `triggers()` becomes a simple trigger (one expression per trigger)
- Triggers are passed to Viper's `Forall` construct

### Viper Output

User-defined triggers are converted to Viper `Trigger` objects and included in the generated Viper code, overriding automatic trigger inference.
