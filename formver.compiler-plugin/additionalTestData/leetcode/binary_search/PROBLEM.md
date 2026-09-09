# Binary Search (LeetCode 704)

Source: https://leetcode.com/problems/binary-search/

## Problem

Given a strictly increasing sequence and a target, return the index of the
target. Return `-1` if the target is absent. The required running time is
logarithmic in the length of the sequence. This SnaKt adaptation represents the
sequence as a `String` and the target as a `Char`, because indexed string reads
are pure expressions supported inside quantified contracts; Kotlin primitive
array and list indexing currently are not.

## Input constraints

- `nums.length` is between 1 and 10,000 inclusive.
- For every valid adjacent pair, `nums[i] < nums[i + 1]`.

For a SnaKt test, non-emptiness and strict ordering should be preconditions.
The midpoint is calculated as `low + (high - low) / 2`.

## Verification contract

Let `r` be the returned value. A complete postcondition is:

- `r == -1 || (0 <= r && r < nums.length)`;
- if `r >= 0`, then `nums[r] == target`;
- if `r == -1`, every valid index contains a value different from `target`.

The implementation must not mutate `nums` and every indexed access must be in
bounds. Useful loop invariants state that the active search interval remains
within the array and that all discarded indices cannot contain the target.
Termination follows because each unsuccessful iteration strictly shrinks that
interval.

This problem stays within SnaKt's demonstrated core: integer variables,
conditionals, array reads, a `while` loop, invariants, and universally
quantified facts. It requires no unsupported collection algorithms or object
modeling.

## Examples

- `nums = "acfik"`, `target = 'i'` returns `3`.
- `nums = "acfik"`, `target = 'd'` returns `-1`.
- `nums = "q"`, `target = 'q'` returns `0`.
- `nums = "q"`, `target = 'a'` returns `-1`.
