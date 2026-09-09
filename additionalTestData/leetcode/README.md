# LeetCode problem candidates

This directory contains problem descriptions and candidate solutions selected
for a separate formal-verification pass. It is intentionally outside the main
`testData` tree.

| Number | Problem | Metadata | Why it is suitable |
| --- | --- | --- | --- |
| 1480 | Running Sum of 1d Array | [1480-running-sum-of-1d-array.md](1480-running-sum-of-1d-array.md), [solution](1480-running-sum-of-1d-array.kt) | A single loop with a prefix-recurrence invariant. |
| 1929 | Concatenation of Array | [1929-concatenation-of-array.md](1929-concatenation-of-array.md), [solution](1929-concatenation-of-array.kt) | Direct index mapping with no arithmetic overflow. |

The implementations retain the expected LeetCode signatures and do not mutate
their inputs. `IntArray` conversion has no existing verification example in the
main suite and is therefore an explicit target for the independent verifier.
