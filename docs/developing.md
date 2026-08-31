# Developing the plugin

Publishing a new Silicon build: publish-silicon.md.

## Toolchain

The build runs on any JDK from 21 to 25 and compiles with a toolchain pinned to
21, so the bytecode does not depend on which JDK launched Gradle. The pinned JDK
is provisioned automatically when it is not installed.

The Gradle version in the wrapper is a floor rather than a preference: Gradle 8
compiles the Kotlin build scripts with a compiler that rejects JDK 25, and
detekt below 2.0 embeds a compiler that does the same and runs inside the
daemon.

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
before opening a PR.

All three only check the goldens. Regenerating them is a separate thing, run by
passing `-Pkotlin.test.update.test.data=true` to `test`: it rewrites the golden
files and writes the diagnostic markers into the `.kt`, which a new test needs.
Verification runs, because the goldens include its output.

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
`pre-commit install`. Besides the formatting hooks it runs two checks that need
no build: one over the testData tree, and the tests for the repository's own
scripts.
