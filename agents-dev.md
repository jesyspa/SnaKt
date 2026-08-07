# The agent scripts

`agent-scripts/` holds helpers for the test loop described in dev-info.md. They
exist so the loop does not have to be rediscovered from the Gradle files each
time; nothing in the build depends on them, and a human can drive Gradle
directly.

- `test.sh` — the test driver. Conversion only by default (`untilConversion`
  plus the locality tests, which have no verification stage), `--verify` for the
  full pipeline, `--update` to regenerate goldens and report what they now say.
  On failure it prints the expected/actual diff that Gradle's cross-JVM
  serialization strips from golden-file assertions.
- `check-all.sh` — `check`, `pre-commit` and the testData checks together.
  `--rerun` re-executes tests Gradle considers current.
- `check-testdata.sh` — golden files with no source, and empty golden files.
- `lib.sh` — sourced by the others, not run.
- `junit_first_failure.py` — the JUnit XML parser `lib.sh` calls; `tests/run.sh`
  exercises it against fixture XML.

`test.sh` and `check-all.sh` take `--help`. `test.sh` and `lib.sh` need `python3`
on PATH, because the test results they report from are XML; `check-testdata.sh`
needs nothing but a checkout.

## Exit codes

`check-all.sh` reports each of its three checks as passed, failed or skipped,
and exits 1 if any failed. Exit 2 means nothing failed but something did not
run, which is what a missing `pre-commit` gives you: the run says less than the
command promises, so treat it as a state to fix rather than a pass. Install the
hook and get a 0.

## Recovering the diff

`test.sh` prints expected/actual for a failing golden through
`DumpAssertionDiffExtension`, registered on the compiler-plugin test classpath
by `formver.compiler-plugin/test-resources/`. It is registered for every run and
stays inert unless `SNAKT_TEST_DUMP_DIR` is set, which `test.sh` does. The
locality module has no test fixtures on its classpath, so its failures go to the
HTML report instead.

Only golden-file assertions carry expected/actual values to recover. For a
thrown exception `test.sh` prints the failure from the JUnit XML instead, since
there is no diff to show.

Diffs are printed with source-position offsets replaced by `(_,_)`, so a method
that only moved because of an edit earlier in the file drops out.

## Patterns

A test method's name comes from its testData file with the first letter
capitalized and dashes turned into underscores, so `assign_local.kt` backs
`testAssign_local`. Gradle's `--tests` filter is case-sensitive; the scripts
convert for you, so the file name, the path to it and the method name all work
as a pattern.
