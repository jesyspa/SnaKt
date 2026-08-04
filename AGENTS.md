# AGENTS.md

Tests are golden-file based. Regenerating goldens records whatever the run
produced, so a function that fails verification passes once that failure has
been recorded.

While developing:

    ./scripts/check-conversion.sh [pattern]  # the fast loop, no verification
    ./scripts/run-test.sh <pattern>          # run one test, recovering the assertion diff

Adding or changing a test:

    ./scripts/update-goldens.sh [pattern]    # regenerate, then report what needs review

Before pushing:

    ./scripts/check-all.sh

Verifying is slow, so stay on the fast loop while developing. The other scripts
verify for you when it matters.

Documentation for humans: README.md, dev-info.md, SPECIFICATIONS.md.
