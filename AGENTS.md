# AGENTS.md

Tests are golden-file based. Regenerating goldens records whatever the run
produced, so a function that fails verification passes once that failure has
been recorded.

Adding or changing a test:

    ./scripts/update-goldens.sh [pattern]   # regenerate, then report what needs review
    ./scripts/run-test.sh <pattern>         # run one test, recovering the assertion diff

Before pushing:

    ./scripts/check-all.sh

Use `./gradlew untilConversion` as much as possible while developing. Verifying
is slow, so leave it until last; the scripts above run it for you.

Documentation for humans: README.md, dev-info.md, SPECIFICATIONS.md.
