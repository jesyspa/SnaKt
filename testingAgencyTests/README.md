# Testing agency tests

These are opt-in exploratory regression checks for SnaKt. They are deliberately
kept outside the Gradle source sets and CI configuration.

Run a test from the repository root. Every script accepts an optional positive
run count and refuses to launch its expensive SnaKt test more than three times
per invocation:

```bash
./testingAgencyTests/existential-conversion.sh
./testingAgencyTests/existential-verification.sh
./testingAgencyTests/universal-trigger-verification.sh
```

The scripts exercise one topic each by selecting one canonical golden testData
case through `agent-scripts/test.sh`. Update `DIARY.md` after every launch; the
scripts do not update it automatically because test interpretation belongs in
the diary, not in generated output.
