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

Test files support directives that control how they run. Ours are declared in
`FormVerDirectives`, in
`formver.compiler-plugin/test-fixtures/org/jetbrains/kotlin/formver/plugin/services/ExtensionRegistrarConfigurator.kt`.
`FULL_JDK` and `WITH_STDLIB` come from the Kotlin test framework.

## Checks

`./gradlew check` runs detekt, `apiCheck` and every module's tests.

A separate CI workflow runs `pre-commit`; install the hook locally with
`pre-commit install`.

## Scripts

`scripts/` holds helpers for the loop above:

- `check-conversion.sh` — the fast loop: `untilConversion` plus the locality
  tests, which need no verification.
- `run-test.sh` — run one test, recovering the expected/actual diff that
  Gradle's cross-JVM serialization strips from golden-file assertions.
- `update-goldens.sh` — regenerate goldens and report what changed.
- `check-verified.sh` — which tests record a verification failure as expected,
  read off the recorded diagnostics rather than the build output.
- `check-all.sh` — `check`, `pre-commit` and the testData checks together.
- `check-testdata.sh` — golden files with no source, and empty golden files.
- `dump-test-diff.sh` — the diff recovery `run-test.sh` escalates to.
- `lib.sh` — sourced by the others, not run.

They need `python3` on PATH: the test results they report from are XML.

A test method's name comes from its testData file with the first letter
capitalized, so `assign_local.kt` backs `testAssign_local`. Gradle's `--tests`
filter is case-sensitive; the scripts capitalize for you, so any of the three
spellings works as a pattern.
