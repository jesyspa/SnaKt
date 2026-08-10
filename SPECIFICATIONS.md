# Writing Specifications in SnaKt

SnaKt translates Kotlin code with formal specifications to [Viper](https://www.pm.inf.ethz.ch/research/viper.html) for verification. This guide assumes familiarity with Hoare logic; see the [Viper tutorial](http://viper.ethz.ch/tutorial/) if needed.

## Verification Control

By default, SnaKt verifies the functions that have a contract: a Kotlin `contract { }` block, or a
SnaKt `preconditions { }` or `postconditions<T> { }` block. Everything else is left alone:

```kotlin
import org.jetbrains.kotlin.formver.plugin.*

fun divide(numerator: Int, denominator: Int): Int {
    preconditions { denominator != 0 }   // enough on its own to make this a target
    return numerator / denominator
}

@AlwaysVerify  // verify this one too, though it specifies nothing
fun half(x: Int) = divide(x, 2)
```

Callers of a specified function may assume its postconditions, so a function that carries a
specification is verified against it rather than believed. Suppressing that — with `@NeverVerify`,
or by setting the selection to `no_targets` — turns the specification into an assumption that
nothing checks.

**Annotations:**
- `@AlwaysVerify` — verify this function regardless of plugin settings
- `@NeverVerify` — skip verification even with contracts
- `@NeverConvert` — skip Viper conversion entirely

**Plugin configuration** (in `build.gradle.kts`):
```kotlin
formver {
    verificationTargetsSelection("all_targets")  // Verify all functions
    // or "targets_with_contract" (default) — only functions carrying a contract
    // or "no_targets" — disable verification
}
```

Note that `@AlwaysVerify` overrides plugin settings.

## Preconditions and Postconditions

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

Multiple conditions are implicitly conjoined. The postconditions block receives the return value as its parameter.

## Loop Invariants

```kotlin
@AlwaysVerify
fun sumUpTo(n: Int): Int {
    preconditions { n >= 0 }
    var sum = 0
    var i = 0
    while (i <= n) {
        loopInvariants {
            i >= 0
            sum == i * (i - 1) / 2
        }
        sum += i
        i++
    }
    return sum
}
```

The rules are as follows:
- Loop invariant must hold when the loop is entered.
- The loop body may assume the condition holds.
- Loop invariant must hold after each iteration.
- Loop invariant must hold when the loop is exited.
- Code after the loop may assume the condition fails.

## Universal Quantification

Use `forAll<T>` for quantified formulas:

```kotlin
@AlwaysVerify
fun example(arr: IntArray): Unit {
    preconditions {
        forAll<Int> { j ->
            (0 <= j && j < arr.size()) implies (arr[j] > 0)
        }
    }
    // ...
}
```

The `implies` infix operator is provided for convenience (`a implies b` ≡ `!a || b`).

### Triggers

By default, Viper infers triggers automatically. You can specify them explicitly:

```kotlin
forAll<Int> { x ->
    triggers(x * x)  // Single trigger
    x * x >= 0
}

forAll<Int> { x ->
    triggers(x * x, x + 1)  // Multiple triggers
    x != 0 implies (x * x > 0)
}
```

Each argument to `triggers()` becomes a separate trigger. This differs from Viper syntax where you can group multiple expressions in a single trigger; currently SnaKt only supports simple (single-expression) triggers.

## Additional Plugin Options

```kotlin
formver {
    errorStyle("user_friendly")  // or "original_viper", "both"
    logLevel("only_warnings")    // or "short_viper_dump", "full_viper_dump"
    unsupportedFeatureBehaviour("throw_exception")  // or "assume_unreachable"
}
```
