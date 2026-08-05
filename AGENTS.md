# AGENTS.md

Tests are golden-file based. Regenerating goldens records whatever the run
produced, so a function that fails verification passes once that failure has
been recorded.

While developing:

    ./scripts/check-conversion.sh [pattern]  # the fast loop, no verification
    ./scripts/run-test.sh <pattern>          # run one test, recovering the assertion diff

Adding or changing a test:

    ./scripts/update-goldens.sh [pattern]    # regenerate, then report what needs review
    ./scripts/check-verified.sh <pattern>    # exit 1 if that test records a failure

A recorded failure is the trap above: the test is green because the failure is
what the golden says to expect. Read what the script prints before deciding
whether that is the test you meant to write.

Before pushing:

    ./scripts/check-all.sh

Verifying is slow, so stay on the fast loop while developing. The other scripts
verify for you when it matters.

Documentation for humans: README.md, dev-info.md, SPECIFICATIONS.md.
