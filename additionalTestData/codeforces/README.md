# Codeforces additional verification problems

This directory is an opt-in problem corpus. It is deliberately outside
`formver.compiler-plugin/testData`, so the generated test suite does not discover it.

Each problem directory contains:

- `SPEC.md`: a concise, independently worded functional specification and the
  intended verification obligations;
- `cases.txt`: concrete input/output fixtures, including the public samples and
  a few boundary cases;
- `solution.kt`: a SnaKt-annotated solution whose contracts and loop invariants
  have been checked by the conversion pipeline and Silicon.

The Kotlin solutions expose pure functions rather than console I/O. Immutable
strings stand in for primitive arrays because SnaKt cannot currently embed
`IntArray`; each affected specification documents its value encoding. Source
links identify the original public problems, and the descriptions here are
paraphrases rather than copies of the Codeforces statements.

| ID | Title | Suggested pure function | Main proof feature |
| --- | --- | --- | --- |
| [4A](https://codeforces.com/problemset/problem/4/A) | Watermelon | `canSplitEven(weight: Int): Boolean` | integer arithmetic and branching |
| [158A](https://codeforces.com/problemset/problem/158/A) | Next Round | `advancingCount(scores: String, k: Int): Int` | bounded counting loop |
| [1512A](https://codeforces.com/problemset/problem/1512/A) | Spy Detected! | `uniqueIndex(values: String): Int` | search with uniqueness precondition |
