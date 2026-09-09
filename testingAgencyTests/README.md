# Testing agency tests

These are opt-in exploratory tests for SnaKt. They are deliberately outside the
regular test-data tree and are not wired into Gradle or CI.

Run one topic from the repository root:

```bash
./testingAgencyTests/run-one.sh exists_explicit_trigger
```

The runner temporarily stages the selected source and its goldens in SnaKt's
diagnostic test-data tree, regenerates the test runner, executes the fast
conversion pipeline, and restores the regular tree on exit. Each source file is
one test topic. Do not run these tests routinely; the diary records the limited
manual executions. Use a JDK supported by the repository (JDK 21 was used for
the recorded run).
