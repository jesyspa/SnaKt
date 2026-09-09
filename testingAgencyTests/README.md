# Testing agency tests

This directory contains focused, manually launched probes for SnaKt. They are
deliberately not wired into Gradle or CI. Each executable checks one topic by
selecting the smallest matching golden test through `agent-scripts/test.sh`.

Run a probe from anywhere in the checkout, for example:

```bash
./testingAgencyTests/existential-quantifier-translation.sh
```

Runs and findings are recorded in `DIARY.md`. A probe should normally be run
once per agency session; repeat it only to establish that a failure is stable.

