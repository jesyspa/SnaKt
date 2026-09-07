# LeetCode candidate problems

This directory is an intentionally disconnected staging area. It is not an input
to the generated tests under `formver.compiler-plugin/testData`, so adding or
solving these candidates does not enlarge the normal test suite.

Each Kotlin file contains a function implementation, its SnaKt contract, and
the loop invariants needed to connect the two. The contracts are concise,
independently written descriptions of the required behavior; the original
problem statements are not reproduced here. Source identifiers, URLs,
representation notes, and topic tags are recorded in `problems.json`.

The search problems use `String` as a compact immutable sequence because SnaKt's
current verification corpus has mature support for indexed strings. Where the
source problem uses an integer or Boolean array, `problems.json` calls out the
representation adaptation explicitly.
