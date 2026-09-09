# Codeforces 4A — Watermelon

- **Contest/problem ID:** 4A
- **Title:** Watermelon
- **Canonical URL:** https://codeforces.com/problemset/problem/4/A
- **Proposed Kotlin signature:** `fun canSplitEvenly(weight: Int): Boolean`

## Paraphrased problem

Given the integer weight of a watermelon, decide whether it can be divided into
exactly two pieces such that both pieces have positive, even integer weights.
The pieces do not have to weigh the same.

The original console program prints `YES` when such a division exists and `NO`
otherwise. The proposed pure function models those answers as `true` and
`false`.

## Input-domain assumptions

- `1 <= weight && weight <= 100` (the bounds from the Codeforces task).
- Weight is measured as a whole number of kilograms.
- Both resulting pieces must be strictly positive.

These bounds make all relevant `Int` arithmetic overflow-free.

## Correctness properties to verify

For every input satisfying the precondition:

1. `result` is true exactly when `weight > 2 && weight % 2 == 0`.
2. If `result` is true, there exist positive even integers `left` and `right`
   with `left + right == weight` (a concrete witness is `left == 2` and
   `right == weight - 2`).
3. If `result` is false, no two positive even integers can sum to `weight`.

Property 1 is the practical SnaKt postcondition. Properties 2 and 3 explain why
that executable characterization is both sufficient and necessary.
