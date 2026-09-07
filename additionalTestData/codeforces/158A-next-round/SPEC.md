# Codeforces 158A — Next Round

Source: <https://codeforces.com/problemset/problem/158/A>

Implement `advancingCount(scores: String, k: Int): Int`.

Each score is represented by the character with the same numeric value. This
keeps indexing and ordering identical while using SnaKt's supported immutable
sequence representation.

## Domain

- `1 <= scores.size && scores.size <= 50`;
- `1 <= k && k <= scores.size` (the problem numbers positions from one);
- every score is between `0` and `100`, inclusive;
- scores are sorted in non-increasing order.

## Result

Return the number of positions `i` for which `scores[i]` is positive and is at
least `scores[k - 1]`.

## Verification target

Prove that the result is between `0` and `scores.size` and equals the cardinality
described above. A counting-loop invariant should relate the accumulator to the
already visited prefix. The solution should not mutate `scores` or perform console
I/O.
