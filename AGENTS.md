# AGENTS.md

Tests are golden-file based: a test passes when its goldens match. Regenerating
records whatever the run produced, so a function that fails verification passes
from then on once that failure is in the golden. Read what `--update` prints.

    ./agent-scripts/test.sh [pattern]           # conversion only — the fast loop
    ./agent-scripts/test.sh --verify [pattern]  # full pipeline, including verification
    ./agent-scripts/test.sh --update [pattern]  # regenerate goldens, then report what changed

A failing run prints the expected/actual diff. A pattern can be spelled as the
testData file is named (`assign_local`), as the path to it, or as the generated
method (`testAssign_local`).

Before pushing:

    ./agent-scripts/check-all.sh

Exit 1 is a real failure. Exit 2 means nothing failed but a check did not run,
which is what a missing `pre-commit` gives you; install it and rerun rather than
reading 2 as a pass.

Verification is slow. Stay on the fast loop while developing.

More on the scripts: agents-dev.md. Documentation for humans: README.md,
dev-info.md, SPECIFICATIONS.md.
