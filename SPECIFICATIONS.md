# Writing Specifications in SnaKt

SnaKt translates Kotlin code with formal specifications to [Viper](https://www.pm.inf.ethz.ch/research/viper.html) for verification. This guide assumes familiarity with Hoare logic; see the [Viper tutorial](http://viper.ethz.ch/tutorial/) if needed.

## Verification Control

By default, SnaKt only verifies functions with Kotlin `contract { }` blocks. To verify functions with SnaKt specifications:

```kotlin
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify  // Enables verification for this function
fun divide(numerator: Int, denominator: Int): Int {
    preconditions { denominator != 0 }
    return numerator / denominator
}
```

**Annotations:**
- `@AlwaysVerify` — verify this function regardless of plugin settings
- `@NeverVerify` — skip verification even with contracts
- `@NeverConvert` — skip Viper conversion entirely

**Plugin configuration** (in `build.gradle.kts`):
```kotlin
formver {
    verificationTargetsSelection("all_targets")  // Verify all functions
    // or "targets_with_contract" (default) — only Kotlin contract { } blocks
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

Invariants must hold before the loop, after each iteration, and at exit.

## Universal Quantification

Use `forAll<T>` for quantified formulas:

```kotlin
@AlwaysVerify
fun example(arr: IntArray): Unit {
    loopInvariants {
        forAll<Int> { j ->
            (0 <= j && j < i) implies (arr[j] > 0)
        }
    }
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

