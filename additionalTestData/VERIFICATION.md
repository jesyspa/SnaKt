# Independent verification report

## Scope and result

Reviewed every file under `additionalTestData` against the published Codeforces
977A and LeetCode 1281 semantics and the repository's SnaKt conventions. Both
implementations perform the required digit operations, respect the stated input
bounds, terminate, and keep all `Int` arithmetic in range.

The original drafts used self-recursive `@Pure` specification functions in
their postconditions. SnaKt currently reports such functions as a Viper
consistency error unless a well-founded `decreases` clause is supplied, while
the annotation API in this checkout exposes no such clause. Those unverifiable
specification helpers were removed. The executable solutions remain unchanged;
their loop invariants now establish progress and arithmetic bounds directly,
and their published examples are explicit postconditions.

For LeetCode 1281, the added processed-digit bounds prove at each iteration
that multiplying the accumulator by the next digit cannot overflow. For
Codeforces 977A, the counted-loop bounds prove safe subtraction/division and
the invariants trace both published examples.

## Validation performed

- Exhaustively compared LeetCode inputs `1..100000` with an independent digit
  product/sum implementation.
- Compared Codeforces inputs `2..10000` for every `k` in `1..50`, plus decimal
  powers and upper-bound cases, with an independent transition implementation
  (500,100 cases total).
- Confirmed both published examples for each problem.
- Attempted an isolated SnaKt verification run from a temporary clone, without
  adding either fixture to the repository's main `testData` tree and without
  updating repository goldens. Gradle could not start because this environment
  only provides JBR/OpenJDK 25.0.2, which this build rejects while parsing the
  Java version. A compatible project JDK is therefore required for an actual
  compiler-plugin/Viper run.

`AUTOMATIONS.md` was requested by the automation instructions but is absent
from this checkout (a repository-wide filename search found no copy).
