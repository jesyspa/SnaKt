# 1929. Concatenation of Array

- Canonical source: <https://leetcode.com/problems/concatenation-of-array/>
- Difficulty: Easy
- Expected Kotlin signature: `fun getConcatenation(nums: IntArray): IntArray`

## Problem

Given an integer array `nums` of length `n`, return an array of length `2 * n`
formed by placing two copies of `nums` consecutively. Thus the first and second
halves both reproduce the input in the same order.

## Input-domain assumptions

- `nums.size` is in `1..1000`.
- Every element is in `1..1000`.

These are the bounds in the published problem contract. In particular,
`2 * nums.size` is safely representable as a Kotlin `Int`.

## Correctness properties to verify

Let `n == nums.size`. For a returned array `result`:

1. `result.size == 2 * n`.
2. For every `i` in `0 until n`, `result[i] == nums[i]`.
3. For every `i` in `0 until n`, `result[i + n] == nums[i]`.
4. Equivalently, for every valid result index `j`,
   `result[j] == nums[j % n]`.
5. The input array is not modified.

A suitable loop invariant says that, before each iteration `i`, both output
positions corresponding to every index below `i` have been filled with the
matching input value.
