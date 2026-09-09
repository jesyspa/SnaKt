# SnaKt testing agency

This directory contains manually scheduled, bounded probes of SnaKt. They are
deliberately not wired into Gradle or CI. Each executable test covers one topic
and delegates to the repository's golden-file driver without updating goldens.

Run a test from the repository root. A test refuses to execute after its limit
in `run-counts.tsv` is reached. After a run, update `DIARY.md` with the outcome.
