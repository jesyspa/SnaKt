# AGENTS.md

Tests are golden-file based: a test passes when its goldens match. Regenerating
records whatever the run produced, so a function that fails verification passes
from then on once that failure is in the golden. Read what `--update` prints.

    ./scripts/test.sh [pattern]           # conversion only — the fast loop
    ./scripts/test.sh --verify [pattern]  # full pipeline, including verification
    ./scripts/test.sh --update [pattern]  # regenerate goldens, then report what changed

A failing run prints the expected/actual diff itself; there is no second command
to reach for. A pattern can be spelled as the testData file is named
(`assign_local`) or as the generated method (`testAssign_local`).

Before pushing:

    ./scripts/check-all.sh   # exit 2: nothing failed, but a check was skipped

Verification is slow. Stay on the fast loop while developing.

Documentation for humans: README.md, dev-info.md, SPECIFICATIONS.md.
