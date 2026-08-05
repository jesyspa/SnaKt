# AGENTS.md

Tests are golden-file based. Regenerating goldens records whatever the run
produced, so a function that fails verification passes once that failure has
been recorded.

While developing:

    ./scripts/check-conversion.sh [pattern]  # the fast loop, no verification
    ./scripts/run-test.sh <pattern>          # run one test, recovering the assertion diff

Adding or changing a test:

    ./scripts/update-goldens.sh [pattern]    # regenerate, then say what it established
    ./scripts/check-verified.sh <pattern>    # ask that question on its own

A green test means the goldens match. Whether the subject verifies is a separate
question, which `check-verified.sh` answers:

    exit 0   the verifier ran and recorded nothing — it verifies
    exit 1   the verifier ran and recorded diagnostics — that failure is now
             the expectation
    exit 3   a directive turned the verifier off, so the question does not
             apply to this test

Exit 1 is the one to watch. Regenerating records whatever ran, so a test you
meant to verify goes green asserting its own failure; that is only right for a
test whose job is to pin down a known limitation.

Before pushing:

    ./scripts/check-all.sh   # exit 2: nothing failed, but a check was skipped

Verifying is slow, so stay on the fast loop while developing. The other scripts
verify for you when it matters.

Documentation for humans: README.md, dev-info.md, SPECIFICATIONS.md.
