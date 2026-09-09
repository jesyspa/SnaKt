# Codeforces 734A — Anton and Danik

- Source: https://codeforces.com/problemset/problem/734/A
- Intended Kotlin API: `fun winner(games: String): Int`
- Result encoding: `1` for Anton, `-1` for Danik, and `0` for a draw.

## Paraphrased task

A sequence records the winner of each chess game. The character `A` denotes an
Anton win and `D` denotes a Danik win. Compare their total wins and return the
encoded overall result above.

## Input constraints / preconditions

- `1 <= games.length && games.length <= 100_000`.
- Every character in `games` is either `A` or `D`.
- The adaptation receives only the game string; the original input's integer
  `n` is represented by `games.length`.

## Required postconditions

Let `a` be the number of indices `i` in `[0, games.length)` for which
`games[i] == 'A'`, and let `d` be the corresponding count for `D`.

- The result is one of `-1`, `0`, or `1`.
- The result is `1` exactly when `a > d`.
- The result is `-1` exactly when `d > a`.
- The result is `0` exactly when `a == d`.

A suitable implementation uses an integer balance initialized to zero, adds
one for `A`, subtracts one for `D`, and compares the final balance with zero.
The loop invariant should connect the balance to the processed prefix.

## Examples

- `winner("ADAAAA") == 1`
- `winner("DDDAAA") == 0`
- `winner("DADDAD") == -1`
