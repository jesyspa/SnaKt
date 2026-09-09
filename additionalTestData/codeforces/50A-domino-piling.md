# Codeforces 50A — Domino piling

- **Contest/problem ID:** 50A
- **Title:** Domino piling
- **Canonical URL:** https://codeforces.com/problemset/problem/50/A
- **Proposed Kotlin signature:** `fun maximumDominoes(rows: Int, columns: Int): Int`

## Paraphrased problem

A rectangular board has `rows * columns` unit squares. Place as many `2 x 1`
dominoes as possible. Dominoes must stay within the board, cover two distinct
unit squares, and may not overlap. Return the maximum possible number of
dominoes.

The dominoes may be oriented either horizontally or vertically.

## Input-domain assumptions

- `1 <= rows && rows <= 16`.
- `1 <= columns && columns <= 16`.
- `rows <= columns` (matching the statement's `M <= N` convention).

These are the original task's bounds, so `rows * columns` is positive and well
within Kotlin `Int` range.

## Correctness properties to verify

For every input satisfying the precondition:

1. `result == (rows * columns) / 2`, using integer division.
2. `result >= 0` and `2 * result <= rows * columns` (the returned dominoes do
   not claim more squares than the board contains).
3. Fewer than two squares remain uncovered:
   `rows * columns - 2 * result >= 0` and
   `rows * columns - 2 * result < 2`.
4. Properties 2 and 3 establish the area upper bound. Attainability additionally
   uses the standard checkerboard construction: tile along an even dimension;
   if both dimensions are odd, tile the even-width portion and then pair all
   but one square in the remaining strip. The scalar function and its contract
   do not model a placement witness.

Property 1 is the compact SnaKt postcondition. The arithmetic clauses expose
the upper bound, while the prose construction supplies the separate
attainability argument that justifies the problem reduction.
