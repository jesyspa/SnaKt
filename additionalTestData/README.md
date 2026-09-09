# Additional problem corpus

This directory contains candidate formal-verification problems kept outside
`formver.compiler-plugin/testData`. Nothing in the Gradle build or generated test
suites discovers this directory, so these files do not add to the normal test
runtime.

## Problems

### Codeforces 617A — Elephant

- Source: https://codeforces.com/problemset/problem/617/A
- Input: an integer destination `x` in `1..1_000_000`.
- Task: find the fewest positive moves needed to reach `x` when every move has
  length from 1 through 5.
- Solution: `minimumElephantSteps` computes `ceil(x / 5)` without overflowing.

### LeetCode 58 — Length of Last Word

- Source: https://leetcode.com/problems/length-of-last-word/
- Input: a nonempty string containing English letters and spaces, with at least
  one letter.
- Task: return the length of the final maximal run of letters, ignoring trailing
  spaces.
- Solution: `lengthOfLastWord` scans backward over trailing spaces and then over
  the final word.

The specifications are paraphrased rather than copied from the source sites.
These files are deliberately not test data yet; a later curated change can move
individually reviewed problems into the main corpus.
