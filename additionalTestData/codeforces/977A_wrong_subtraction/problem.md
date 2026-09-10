# Codeforces 977A — Wrong Subtraction

Source: [Codeforces problem 977A](https://codeforces.com/problemset/problem/977/A)

## Problem

Vanya starts with an integer `n` and performs exactly `k` operations. On each
operation:

- if the final decimal digit of the current value is nonzero, subtract `1`;
- otherwise, divide the current value by `10`.

Return the value after all `k` operations.

## Constraints

- `2 <= n <= 1_000_000_000`
- `1 <= k <= 50`

## Function fixture

Implement the following Kotlin function without console I/O:

```kotlin
fun wrongSubtraction(n: Int, k: Int): Int
```

The preconditions are the constraints above. The result must equal the value
obtained by applying the stated transition exactly `k` times.

## Examples

```text
wrongSubtraction(512, 4) == 50
wrongSubtraction(1_000_000_000, 9) == 1
```

## Formal-verification focus

Specify the input constraints and use a counted loop whose invariant connects
the current value and iteration count to the repeated transition. Verification
should establish termination, absence of arithmetic errors, and the two
transition cases on every iteration.
