# LeetCode 2119 — A Number After a Double Reversal

Source: [LeetCode — A Number After a Double Reversal](https://leetcode.com/problems/a-number-after-a-double-reversal/)

## Requirements

Reverse the decimal digits of a non-negative integer, then reverse the digits
of that result. Decimal representations do not retain leading zeroes. Return
whether the second reversal equals the original integer.

Equivalently, the answer is true exactly when the input is zero or its last
decimal digit is nonzero. A positive number ending in zero loses that zero in
the first reversal and cannot recover it in the second; every other number is
unchanged after two reversals.

## Constraints

- `0 <= num <= 1_000_000`

## Examples

- `isSameAfterReversals(526) == true`: the reversals are `625` and `526`.
- `isSameAfterReversals(1800) == false`: the reversals are `81` and `18`.
- `isSameAfterReversals(0) == true`: reversing zero still yields zero.

## Intended Kotlin interface

```kotlin
fun isSameAfterReversals(num: Int): Boolean
```

The standalone function should not use the LeetCode `Solution` wrapper or any
input/output APIs.

## Formal contract

Under the constraint precondition, the complete result contract can be stated
using only supported scalar operations:

```kotlin
preconditions { num >= 0 && num <= 1_000_000 }
postconditions { result == (num == 0 || num % 10 != 0) }
```

The exact annotation syntax should follow the conventions used by the eventual
additional test fixture.

## Why this is suitable for formal verification

The mathematical observation above reduces the complete behavior to one
Boolean expression over a scalar `Int`. An implementation needs only equality,
Boolean branching, and remainder; Snakt's current fixtures already demonstrate
verification of scalar comparisons, branching, and remainder. There are no
arrays, strings, heap reads, quantifiers, bitwise operations, library calls, or
loop invariants, so this avoids the unsupported features encountered by the
previous pivot-index candidate while still checking a recognizable problem
solution rather than a platform-specific harness.
