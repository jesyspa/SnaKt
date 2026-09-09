# Search Insert Position (LeetCode 35)

Source: https://leetcode.com/problems/search-insert-position/

## Problem

Given a strictly increasing sequence and a target, return the
target's index when it is present. Otherwise, return the index at which the
target can be inserted while preserving the sequence's order. The intended
algorithm runs in logarithmic time. This SnaKt adaptation represents the
sequence as a `String` and the target as a `Char`, because indexed string reads
are pure expressions supported inside quantified contracts; Kotlin primitive
array and list indexing currently are not.

## Input constraints

- `nums.length` is between 1 and 10,000 inclusive.
- For every valid adjacent pair, `nums[i] < nums[i + 1]`.

For a SnaKt test, the size and ordering requirements should be expressed as
preconditions. The numeric LeetCode bounds are useful domain documentation but
are not necessary for memory safety.

## Verification contract

Let `r` be the returned index. A complete postcondition is:

- `0 <= r && r <= nums.length`;
- every valid index smaller than `r` contains a value smaller than `target`;
- every valid index greater than or equal to `r` contains a value greater than
  or equal to `target`.

Strict input ordering makes this insertion point unique. It also implies that
when the target occurs, `r` is its index. The implementation must not mutate
`nums` and must only read valid indices.

This is a good fit for SnaKt because it needs integer arithmetic, indexed array
reads, a `while` loop, loop invariants, and universal quantification, without
collections, allocation, exceptions, recursion, or higher-order library calls.

## Examples

- `nums = "acfh"`, `target = 'f'` returns `2`.
- `nums = "acfh"`, `target = 'b'` returns `1`.
- `nums = "acfh"`, `target = 'z'` returns `4`.
- `nums = "acfh"`, `target = 'A'` returns `0`.
