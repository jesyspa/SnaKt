# Independent verification audit

Audit date: 2026-09-09.

## Status

| Candidate | Algorithm and contract review | SnaKt status |
| --- | --- | --- |
| Codeforces 4A | Correct on the stated `1..100` domain. The Boolean characterization is necessary and sufficient. | Uses supported scalar constructs, but no fresh verifier result was produced in this environment. |
| Codeforces 50A | Correct on the stated bounded board domain. The formula is the area upper bound and is attainable by a checkerboard construction. | Uses supported scalar constructs, but no fresh verifier result was produced in this environment. |
| LeetCode 1480 | Correct recurrence and an inductive cursor/processed-prefix invariant. The bounds imply `abs(prefix) <= 1_000_000_000`, so `Int` accumulation cannot overflow. | **Not formally verified:** this checkout has no `IntArray` embedding. |
| LeetCode 1929 | Correct two-copy index mapping and an inductive processed-prefix invariant. `2 * nums.size <= 2000`, so allocation/index arithmetic cannot overflow. | **Not formally verified:** this checkout has no `IntArray` embedding. |

The LeetCode input-preservation clauses express the intended frame condition,
and both implementations only read `nums`. Their inductiveness was checked by
inspection: initialization makes each quantified processed range empty; each
body establishes exactly the element(s) admitted when `index` is incremented;
and loop exit turns `completed < index` into the postcondition's full valid
range. This is not a substitute for converter and solver output.

The scalar contracts characterize the returned values, but they do not encode
the existential split witnesses for 4A or a domino placement witness for 50A.
Those reductions were checked mathematically from the accompanying problem
statements. In particular, the arithmetic remainder clauses in 50A establish
an upper bound; attainability needs the separate tiling construction now stated
in its metadata.

## Commands and environment

The audit found OpenJDK `25.0.2` and no `kotlinc` or `z3` executable. A
representative repository verification was attempted without changing test
data or goldens:

```text
./agent-scripts/test.sh --verify sum_of_1_to_n
```

It exited 1 before any tests ran. Gradle reported `25.0.2`; both the compiler
and locality modules produced no test results. Consequently, this run does not
claim solver-backed verification for any candidate.

Repository inspection found a special embedding for `BooleanArray` in
`FullySpecialKotlinFunction.kt`, but no corresponding `IntArray` embedding in
the converter. There are also no `IntArray` examples in the main test-data
suite. The two `IntArray` candidates must remain outside the supported/formally
verified category until conversion support exists and a compatible JDK plus Z3
can run the full verification pipeline.
