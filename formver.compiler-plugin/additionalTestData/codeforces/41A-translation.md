# Codeforces 41A — Translation

- Source: https://codeforces.com/problemset/problem/41/A
- Intended Kotlin API: `fun isReverse(word: String, candidate: String): Boolean`

## Paraphrased task

Determine whether `candidate` contains exactly the characters of `word` in
reverse order. Matching is case-sensitive.

## Input constraints / preconditions

- `1 <= word.length && word.length <= 100`.
- `1 <= candidate.length && candidate.length <= 100`.
- Both strings contain lowercase English letters only.

The function remains well-defined when the lengths differ and must return
`false`; consequently, equal lengths need not be a precondition.

## Required postconditions

The result is `true` exactly when both of the following hold:

- `word.length == candidate.length`.
- For every index `i` in `[0, word.length)`,
  `word[i] == candidate[word.length - 1 - i]`.

The implementation should compare mirrored positions in a simple index-based
loop and return early, or retain a Boolean accumulator. A loop invariant should
state that every already-processed index satisfies the mirrored equality.

## Examples

- `isReverse("code", "edoc") == true`
- `isReverse("abb", "aba") == false`
- `isReverse("a", "aa") == false`
