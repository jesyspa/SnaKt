# Codeforces 1353A — Most Unstable Array

## Source

- Canonical problem: [Codeforces 1353A — Most Unstable Array](https://codeforces.com/problemset/problem/1353/A)
- Problem ID: `1353A`

## Paraphrased requirements

An array has `n` integer elements, and every element must be between `0` and
`m`, inclusive. Its *instability* is the sum of the absolute differences of
each adjacent pair:

`|a[0] - a[1]| + |a[1] - a[2]| + ... + |a[n - 2] - a[n - 1]|`.

Return the greatest instability attainable by any array satisfying those
bounds. The original competitive-programming problem accepts several test
cases; the function proposed below represents one test case and deliberately
contains no console I/O.

## Constraints

- `1 <= n <= 1_000_000_000`
- `1 <= m <= 1_000_000_000`

The greatest possible result is `2 * m <= 2_000_000_000`, so all inputs,
intermediate values, and results fit Kotlin's `Int` range.

## Examples

| `n` | `m` | maximum instability | One witnessing array |
|---:|---:|---:|:---|
| 1 | 100 | 0 | `[0]` |
| 2 | 2 | 2 | `[0, 2]` |
| 5 | 5 | 10 | `[0, 5, 0, 0, 0]` |
| 3 | 1 | 2 | `[0, 1, 0]` |

## Intended Kotlin function

```kotlin
fun maximumInstability(n: Int, m: Int): Int
```

Preconditions should encode the bounds above. A useful postcondition states
the exact maximum in three structural cases: a singleton has no adjacent
pair, a two-element array can span the full range once, and any longer array
can span it twice.

## Why this is suitable for formal verification

The implementation can be a small pure function with no allocation, mutation,
library algorithms, or I/O. Its proof still captures the essential reasoning
of the original problem: every adjacent difference is bounded by `m`, while an
interior peak or valley can contribute at most `2 * m`; explicit endpoint
choices witness each bound. The three length cases exercise branching and
linear integer arithmetic without duplicating the sorting, searching, string,
or factorial examples already present in the main test suite.
