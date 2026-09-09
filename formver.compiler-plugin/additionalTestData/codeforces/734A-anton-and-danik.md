# Codeforces 734A — Anton and Danik

Source: https://codeforces.com/problemset/problem/734/A

## Paraphrased problem

A string records the winner of each game between Anton and Danik. Each character
is `A` for an Anton win or `D` for a Danik win. Determine who won more games, or
report a draw when their win counts are equal.

The original problem prints a name. For a small, directly specifiable FormVer
function, use this equivalent numeric result:

- `1` means Anton won more games;
- `0` means the totals are equal;
- `-1` means Danik won more games.

## Kotlin skeleton

```kotlin
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun antonAndDanik(games: String): Int {
    // TODO: implement a linear scan and supply the contracts/invariants below.
}
```

Only Kotlin constructs already exercised by the FormVer string tests are
needed: `String.length`, indexed character reads, integer locals, a `while`
loop, and `if` branches.

## Formal specification intent

Define the mathematical prefix score

`score(games, k) = (# of 'A' in games[0 until k]) - (# of 'D' in games[0 until k])`.

The function's precondition is:

- every valid index of `games` contains either `A` or `D`.

Its postconditions are:

- the result is one of `-1`, `0`, and `1`;
- the result is `1` exactly when `score(games, games.length) > 0`;
- the result is `0` exactly when `score(games, games.length) == 0`;
- the result is `-1` exactly when `score(games, games.length) < 0`.

An implementation should scan with an index `i` and an integer accumulator
`balance`. At the loop head, maintain:

- `0 <= i && i <= games.length`;
- `balance == score(games, i)`;
- all inspected characters, at indices below `i`, are `A` or `D` (which follows
  from the precondition but is useful when connecting the current character to
  the accumulator update).

After the loop, compare `balance` with zero to obtain the required result. The
solution agent may encode `score` as a FormVer-compatible pure specification
helper, or spell out an equivalent ghost/counting relation supported by the
plugin; it must not weaken the exact three-way postcondition above.

## Suggested checks

- `"A"` gives `1`.
- `"D"` gives `-1`.
- `"AD"` gives `0`.
- `"AAADD"` gives `1`.
- The empty string gives `0` under this function-level contract, even though
  the contest input constrains the number of games to be positive.
