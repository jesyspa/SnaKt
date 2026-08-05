# Info for plugin developers

Publishing a new Silicon build: internal-dev.md.

## Tests

We use the test framework built for kotlinc. A test is a `.kt` file under
`formver.compiler-plugin/testData/diagnostics/` annotated with expected
diagnostics, alongside golden files holding the diagnostic text:

- `.fir.diag.txt` — the conversion output, including the generated Viper code.
- `.viper.diag.txt` — verification diagnostics. Present only where verification
  reported something.

The test runners are generated from the testData tree as part of
`compileTestKotlin`, so a new file is picked up on the next build.

The pipeline splits into conversion (uniqueness checking, conversion, purity
checking) and verification (Viper consistency checking and verification):

| Task                       | Conversion | Verification                |
|:---------------------------|:-----------|:----------------------------|
| `./gradlew test`           | every test | every test                  |
| `./gradlew update`         | every test | where conversion changed    |
| `./gradlew untilConversion`| every test | never                       |

Use `untilConversion` as much as possible while developing, and `test` last,
before opening a PR. `update` re-verifies only the tests whose conversion output
changed.

Regenerating golden files runs verification, since the goldens include its
output.

Pass `-Pkotlin.test.update.test.data=true` to regenerate golden files. This also
writes the diagnostic markers into the `.kt`, which a new test needs.

### Directives

Test files support directives that control how they run, written as `// NAME` at
the top of the file. `FULL_JDK` and `WITH_STDLIB` come from the Kotlin test
framework; ours are declared in `FormVerDirectives`, in
`formver.compiler-plugin/test-fixtures/org/jetbrains/kotlin/formver/plugin/services/ExtensionRegistrarConfigurator.kt`.

Which checks run:

- `NEVER_VALIDATE` — convert but do not verify. Consistency checking still runs.
  This is how a test that is not meant to reach the verifier says so.
- `UNIQUE_CHECK_ONLY` — uniqueness checking, with locality first. No conversion.
- `LOCALITY_CHECK_ONLY` — locality checking alone, uniqueness off. No conversion.
- `ALWAYS_VALIDATE` — verify every target. Verification is already the default,
  so this changes nothing on its own; it earns its place by overriding the two
  `*_CHECK_ONLY` directives above.

What the diagnostic contains:

- `FULL_VIPER_DUMP` — the whole Viper program.
- `RENDER_PREDICATES` — class predicates. Cannot be combined with the above.
- `DUMP_UNIQUENESS_CFG` — the control-flow graph with flow information.

And `REPLACE_STDLIB_EXTENSIONS` substitutes stdlib functions such as `run` with
versions whose bodies the plugin can see.

## Checks

`./gradlew check` runs detekt, `apiCheck` and every module's tests.

A separate CI workflow runs `pre-commit`; install the hook locally with
`pre-commit install`. Besides the formatting hooks it runs `check-testdata.sh`
and the script tests, both of which need no build.

## Scripts

`scripts/` holds helpers for the loop above:

- `check-conversion.sh` — the fast loop: `untilConversion` plus the locality
  tests, which need no verification.
- `run-test.sh` — run one test, recovering the expected/actual diff that
  Gradle's cross-JVM serialization strips from golden-file assertions.
- `update-goldens.sh` — regenerate goldens, show what they now say, and end with
  what the run established.
- `check-verified.sh` — whether a test verifies, records a failure as its
  expectation, or does not run the verifier at all. Read off the recorded
  diagnostics and the test's directives rather than the build output. Exits 0,
  1 and 3 for those three cases, and 2 when the pattern names no test.
- `check-all.sh` — `check`, `pre-commit` and the testData checks together.
  `--rerun` re-executes tests Gradle considers current. Exits 1 if a check
  failed and 2 if none failed but one was skipped, which is what a missing
  `pre-commit` gives you.
- `check-testdata.sh` — golden files with no source, and empty golden files.
- `dump-test-diff.sh` — the diff recovery `run-test.sh` escalates to.
- `lib.sh` — sourced by the others, not run.
- `junit_ran_tests.py`, `junit_first_failure.py` — the JUnit XML parsers `lib.sh`
  calls; `tests/run.sh` exercises them against fixture XML.

They need `python3` on PATH: the test results they report from are XML.

Which tests a directive silences is decided in `ExtensionRegistrarConfigurator`,
and `check-verified.sh` mirrors it: `NEVER_VALIDATE` forces verification off and
beats `ALWAYS_VALIDATE`, which in turn overrides the two `*_CHECK_ONLY`
directives. The locality module has no verification stage at all.

A test method's name comes from its testData file with the first letter
capitalized, so `assign_local.kt` backs `testAssign_local`. Gradle's `--tests`
filter is case-sensitive; the scripts capitalize for you, so any of the three
spellings works as a pattern.
