# 1480. Running Sum of 1d Array

- Canonical source: <https://leetcode.com/problems/running-sum-of-1d-array/>
- Difficulty: Easy
- Expected Kotlin signature: `fun runningSum(nums: IntArray): IntArray`

## Problem

Given an integer array, return an array of the same length whose element at
index `i` is the sum of all input elements from index `0` through `i`,
inclusive.

## Input-domain assumptions

- `nums.size` is in `1..1000`.
- Every element is in `-1_000_000..1_000_000`.
- To keep Kotlin `Int` arithmetic mathematical during verification, every
  prefix sum is assumed to lie in `Int.MIN_VALUE..Int.MAX_VALUE`.

The first two bounds match the published problem contract. The explicit
prefix-sum bound makes the implicit machine-integer requirement visible to the
formal specification.

## Correctness properties to verify

For a returned array `result`:

1. `result.size == nums.size`.
2. For every valid index `i`, `result[i]` equals
   `nums[0] + nums[1] + ... + nums[i]`.
3. Equivalently, `result[0] == nums[0]`, and for every valid `i > 0`,
   `result[i] == result[i - 1] + nums[i]`.
4. The input array is not modified.

The natural loop invariant states that all positions before the loop cursor
already contain their correct prefix sums, while an accumulator equals the
sum of the processed prefix.
