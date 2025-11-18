# Writing Specifications in SnaKt

SnaKt allows you to write formal specifications for your Kotlin code using preconditions, postconditions, loop invariants, and quantifiers. These specifications are verified using the Viper framework.

## Basic Annotations

To enable verification for a function, annotate it with `@AlwaysVerify`:

```kotlin
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun increment(x: Int): Int {
    postconditions<Int> { result ->
        result == x + 1
    }
    return x + 1
}
```

## Preconditions

Preconditions specify what must be true when a function is called. They are checked at every call site:

```kotlin
@AlwaysVerify
fun divide(numerator: Int, denominator: Int): Int {
    preconditions {
        denominator != 0  // Requires non-zero denominator
    }
    return numerator / denominator
}
```

Multiple conditions can be specified:

```kotlin
@AlwaysVerify
fun accessArray(arr: IntArray, idx: Int): Int {
    preconditions {
        idx >= 0
        idx < arr.size
    }
    return arr[idx]
}
```

## Postconditions

Postconditions specify what must be true when a function returns. The return value is available as a parameter to the postconditions block:

```kotlin
@AlwaysVerify
fun abs(x: Int): Int {
    postconditions<Int> { result ->
        result >= 0
        result == x || result == -x
    }
    return if (x >= 0) x else -x
}
```

Postconditions can reference the function's parameters:

```kotlin
@AlwaysVerify
fun increment(x: Int): Int {
    postconditions<Int> { result ->
        result > x
    }
    return x + 1
}
```

## Loop Invariants

Loop invariants specify conditions that remain true throughout loop execution. They must be true:
- Before the loop starts
- After each iteration
- When the loop exits

```kotlin
@AlwaysVerify
fun sumUpTo(n: Int): Int {
    preconditions {
        n >= 0
    }
    var sum = 0
    var i = 0
    while (i <= n) {
        loopInvariants {
            i >= 0
            i <= n + 1
            sum == i * (i - 1) / 2
        }
        sum += i
        i++
    }
    return sum
}
```

## Universal Quantification with forAll

The `forAll` function allows you to express properties that hold for all values of a type:

```kotlin
@AlwaysVerify
fun alwaysPositive(): Int {
    postconditions<Int> { result ->
        forAll<Int> { x ->
            x * x >= 0
            x * x >= result
        }
    }
    return 0
}
```

### forAll in Loop Invariants

Quantifiers are particularly useful in loop invariants:

```kotlin
@AlwaysVerify
fun allPositive(arr: IntArray): Boolean {
    var i = 0
    while (i < arr.size) {
        loopInvariants {
            i >= 0
            i <= arr.size
            forAll<Int> { j ->
                (0 <= j && j < i) implies (arr[j] > 0)
            }
        }
        if (arr[i] <= 0) return false
        i++
    }
    return true
}
```

### User-Defined Triggers

By default, Viper automatically infers triggers for quantifiers. However, you can explicitly specify trigger expressions to guide the SMT solver:

```kotlin
@AlwaysVerify
fun withTrigger(): Int {
    postconditions<Int> { result ->
        forAll<Int> { x ->
            triggers(x * x)  // Explicitly specify trigger
            x * x >= 0
        }
    }
    return 0
}
```

You can provide multiple triggers:

```kotlin
@AlwaysVerify
fun multipleTriggers(): Int {
    postconditions<Int> { result ->
        forAll<Int> { x ->
            triggers(x * x, x + 1)  // Multiple triggers
            (x != 0) implies (x * x >= result)
        }
    }
    return 1
}
```

Triggers are especially useful when working with arrays or complex expressions:

```kotlin
@AlwaysVerify
fun stringExample(str: String): Int {
    var i = 0
    while (i < str.length) {
        loopInvariants {
            forAll<Int> { j ->
                triggers(str[j])  // Trigger on array access
                (0 <= j && j < str.length) implies (str[j] != 'x')
            }
        }
        i++
    }
    return i
}
```

**When to use triggers:**
- When automatic trigger inference is insufficient
- To improve verification performance
- When working with custom predicates or complex expressions
- To avoid matching loops in the SMT solver

**Note:** If you don't specify triggers, Viper will use automatic trigger inference, so triggers are entirely optional.

## The implies Operator

The `implies` infix function is useful for conditional properties:

```kotlin
(condition) implies (consequence)
```

This is equivalent to `!condition || consequence` but reads more naturally in specifications.

## Combining Specifications

Preconditions and postconditions can be used together:

```kotlin
@AlwaysVerify
fun safeDivide(numerator: Int, denominator: Int): Int {
    preconditions {
        denominator != 0
    }
    postconditions<Int> { result ->
        result * denominator == numerator
    }
    return numerator / denominator
}
```

## Best Practices

1. **Keep specifications simple**: Complex specifications can be harder to verify
2. **Use loop invariants carefully**: They must be maintained by the loop body
3. **Leverage triggers when needed**: Explicit triggers can improve verification speed
4. **Test your code**: Verification complements but doesn't replace testing
5. **Start small**: Begin with simple postconditions and gradually add more detail

## Common Patterns

### Array bounds checking
```kotlin
forAll<Int> { i ->
    (0 <= i && i < array.size) implies (property(array[i]))
}
```

### Conditional properties
```kotlin
forAll<Int> { x ->
    (x > 0) implies (x * x > 0)
}
```

### Range properties
```kotlin
loopInvariants {
    i >= 0
    i <= n
    forAll<Int> { j ->
        (0 <= j && j < i) implies (processed(j))
    }
}
```
