# Codeforces 1512A — Spy Detected!

Source: <https://codeforces.com/problemset/problem/1512/A>

Implement `uniqueIndex(values: String): Int`.

Each character represents one source-array value. Only equality matters to this
problem, so this immutable representation preserves its semantics while staying
within SnaKt's currently supported sequence types.

## Representation

The returned position is zero-based, unlike the one-based output of the original
console problem.

## Domain

- `3 <= values.size && values.size <= 100`;
- there is a value `common` occurring exactly `values.size - 1` times;
- the remaining value differs from `common` and occurs exactly once.

## Result

Return the unique index `result` whose value differs from the common value. Thus
`result` is in bounds, and every other valid index holds a value different from
`values[result]`.

## Verification target

Prove the returned index is in bounds and satisfies the uniqueness condition.
The domain may be expressed as quantified preconditions or through an equivalent
helper predicate. The solution should not mutate `values` or perform console I/O.
