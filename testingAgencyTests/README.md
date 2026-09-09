# Testing agency checks

This directory contains bounded, manually launched checks of SnaKt. It is not
part of any Gradle source set or CI workflow.

Run a check from the repository root. Each check covers one topic and keeps a
counter in `run-counts/`; after its configured limit it exits without launching
the underlying test again. The counters are committed so limits survive fresh
automation checkouts.

The diary in `DIARY.md` records the scope, most recent launch, result, and bugs.
