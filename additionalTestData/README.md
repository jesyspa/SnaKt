# Additional scouting test data

This directory contains independently collected programming problems and their
candidate Kotlin/SnaKt solutions. It is intentionally outside
`formver.compiler-plugin/testData`: neither test generation nor the normal
golden-file suite discovers these files, so the scouting set cannot increase
the default test workload.

Each source-specific subdirectory keeps the collector's paraphrased problem
metadata beside a self-contained solution with the same numeric identifier.
The solutions are candidates for an independent formal-verification pass; they
must not be copied into the main test-data tree merely to run lightweight
Kotlin checks. See `VERIFICATION.md` for the independent audit and the exact
limits of the verification performed in this environment.

The scalar Codeforces examples use verifier constructs already represented in
the repository's golden tests. The LeetCode examples deliberately exercise
`IntArray`; they have intended contracts and inductive loop invariants, but
this checkout has no `IntArray` embedding. They are therefore algorithmically
reviewed candidates, not formally verified SnaKt regression tests.
