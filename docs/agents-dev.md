# The agent scripts

Depth behind AGENTS.md, which has the commands themselves.

`agent-scripts/` wraps the Gradle test tasks so the loop does not have to be
rediscovered from the build files each time. Nothing in the build depends on
them.

- `test.sh` — the test driver. Conversion only by default (`untilConversion`
  plus the locality tests, which have no verification stage), `--verify` for the
  full pipeline, `--update-goldens` to regenerate goldens and report what they
  now say.
- `check-all.sh` — `check`, `pre-commit` and the testData checks together.
  `--rerun` re-executes tests Gradle considers current.
- `check-testdata.sh` — golden files with no source, and empty golden files.
- `lib.sh` — sourced by the others, not run.
- `junit_first_failure.py`, `junit_counts.py` — the JUnit XML parsers `lib.sh`
  calls, for the first failure and for the run's counts; `tests/run.sh`
  exercises both against fixture XML.

`test.sh` and `check-all.sh` take `--help`. `test.sh` and `lib.sh` need `python3`
on PATH, because the test results they report from are XML; `check-testdata.sh`
needs nothing but a checkout.

## Exit codes

`check-all.sh` reports each of its three checks as passed, failed or skipped. A
skip means the run covered less than the command promises, and the gap is in the
setup rather than in the code, so it gets its own exit code rather than being
folded into either 0 or 1.

## Recovering the diff

Gradle's cross-JVM result serialization strips the expected/actual values off a
golden-file assertion before they reach the console.
`DumpAssertionDiffExtension`, registered on the compiler-plugin test classpath
by `formver.compiler-plugin/test-resources/`, catches them inside the test JVM
instead. It is registered for every run and stays inert unless
`SNAKT_TEST_DUMP_DIR` is set, which `test.sh` does. The locality module has no
test fixtures on its classpath, so its failures go to the HTML report instead.

Only golden-file assertions carry values to recover. For a thrown exception
`test.sh` prints the failure from the JUnit XML.

Diffs are printed with source-position offsets replaced by `(_,_)`, so a method
that only moved because of an edit earlier in the file drops out.

## Counts

Every run closes with how many tests it ran and how they went, counted over the
JUnit XML each module wrote since the run started. A silent success and a
success that tested nothing are otherwise the same output. A module that ran
and left no results says so on its own line rather than being left out of the
total, which would make the other module's count read as the whole run. Under
`--update-goldens` a golden-file mismatch is the expected outcome and is
reported as a rewrite; anything else is still a failure.

## Patterns

`GenerateTestsKt` capitalizes a testData file's stem and turns dashes into
underscores to form the method name, so `assign_local.kt` backs
`testAssign_local` and `non-local-returns.kt` backs `testNon_local_returns`.
Gradle's `--tests` filter is case-sensitive, and the scripts convert for you.

## Regenerating

`--update-goldens` regenerates and then prints what each golden now says,
because regeneration records whatever the run produced: a function that fails
verification has that failure written into `<name>.viper.diag.txt` and passes
from then on. Verification diagnostics are printed whole, since that is the
change most likely to be recorded by accident.

This is not `./gradlew update`, which is a test mode: convert everything, and
verify only where conversion changed.
