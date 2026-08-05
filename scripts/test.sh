#!/usr/bin/env bash
# test.sh — Drive the formver test suite.
#
# Usage:
#   ./scripts/test.sh [pattern]           # conversion only — the fast loop, default
#   ./scripts/test.sh --verify [pattern]  # full pipeline
#   ./scripts/test.sh --update [pattern]  # regenerate goldens, then report what changed
#
# A pattern can be given as the testData file is named (assign_local) or as
# the generated test method (testAssign_local).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"
cd "$SCRIPT_DIR/.."

MODE=conversion
while [[ "${1:-}" == --* ]]; do
    case "$1" in
        --verify) MODE=verify ;;
        --update) MODE=update ;;
        *) echo "Unknown flag: $1" >&2; exit 1 ;;
    esac
    shift
done

PATTERN="${1:-}"

if [[ "$MODE" == conversion ]]; then
    COMPILER_TASK=:formver.compiler-plugin:untilConversion
else
    COMPILER_TASK=:formver.compiler-plugin:test
fi
LOCALITY_TASK=:formver.compiler-plugin:locality:test

# --rerun: an UP-TO-DATE task is green without executing anything.
args=(--rerun --no-daemon -q)
if [[ "$MODE" == update ]]; then
    args+=(-Pkotlin.test.update.test.data=true)
fi
if [[ -n "$PATTERN" ]]; then
    args+=(--tests "*$(gradle_filter "$PATTERN")*")
fi

# DumpAssertionDiffExtension is registered unconditionally on the
# compiler-plugin test classpath and only fires when this is set (see lib.sh).
DUMP_DIR="$(dump_dir_default)"
mkdir -p "$DUMP_DIR"
rm -f "$DUMP_DIR"/test-assertion-dump-*.txt "$DUMP_DIR"/test-assertion-diff-*.txt
export SNAKT_TEST_DUMP_DIR="$DUMP_DIR"

MARKER="$(mktemp)"

matched=0
overall_status=0

run_task() {
    if TASK_OUT="$(./gradlew "$1" "${args[@]}" 2>&1)"; then
        TASK_STATUS=0
    else
        TASK_STATUS=$?
    fi
}

# Look at what actually failed before assuming it's a golden-file mismatch:
# rendering dumps only pays off for the assertion family
# DumpAssertionDiffExtension knows how to recover expected/actual from.
report_compiler_failure() {
    local failure_info
    failure_info="$(report_first_xml_failure "$MARKER" || true)"
    if [[ -z "$failure_info" ]]; then
        # No JUnit XML at all: the task died before any test ran. Gradle's
        # own error output, already printed above, is the answer.
        return
    fi
    if is_assertion_failure_type "$(head -1 <<<"$failure_info")"; then
        echo
        echo "FAILED. Recovering the assertion diff:"
        render_dump_diffs "$DUMP_DIR" || true
    else
        echo
        echo "FAILED. Not a golden-file assertion — no diff to recover. From the test run:"
        echo
        tail -n +2 <<<"$failure_info"
    fi
}

report_locality_failure() {
    echo
    echo "FAILED. Locality has no test-fixtures on its classpath, so it has no"
    echo "dump to recover. Expected/actual values are in the HTML report:"
    echo "  formver.compiler-plugin/locality/build/reports/tests/test/index.html"
}

# In --update mode, a matching test is expected to fail: assertEqualsToFile
# writes the golden and then fails, so only "no tests found" (a pattern that
# doesn't reach this module) is worth telling apart from a real run.
run_module() {
    local task="$1" on_failure="$2"
    run_task "$task"
    if [[ -n "$PATTERN" && "$TASK_OUT" == *"No tests found for given includes"* ]]; then
        return
    fi
    matched=1
    if [[ "$MODE" == update || "$TASK_STATUS" -eq 0 ]]; then
        return
    fi
    overall_status=1
    # Gradle's closing advice is about Gradle, not about the failure.
    echo "$TASK_OUT" | grep -v '^\* Try:\|^> Run with \|^> Get more help ' || true
    "$on_failure"
}

run_module "$COMPILER_TASK" report_compiler_failure
run_module "$LOCALITY_TASK" report_locality_failure
rm -f "$MARKER"

if [[ "$matched" -eq 0 ]]; then
    echo "No test matches '$PATTERN'."
    exit 1
fi

if [[ "$MODE" != update ]]; then
    exit "$overall_status"
fi

# Regeneration records whatever the run produced. A function that fails
# verification has its failure written into the goldens and passes from then
# on, so the diff below is the only place that distinguishes the two.

changed() {
    git status --porcelain -- "$@" | sed -E 's/^.{3}//'
}

# What the golden now says, not just that it changed: a path on its own leaves
# the reader to go and look, and that is the step that gets skipped. A new
# golden has no diff to show, so its whole body is the new content.
show() {
    local cap="$1" file="$2" body
    if git ls-files --error-unmatch "$file" >/dev/null 2>&1; then
        # Drop git's four header lines; the path is already printed above.
        body="$(git diff --no-prefix -- "$file" | tail -n +5)"
    else
        body="$(sed 's/^/+/' "$file")"
    fi
    if [[ "$(wc -l <<<"$body")" -gt "$cap" ]]; then
        head -"$cap" <<<"$body" | sed 's/^/    /'
        echo "    ... truncated, read $file"
    else
        sed 's/^/    /' <<<"$body"
    fi
}

# $2 caps how many lines of each file to show; 0 lists paths only.
report() {
    local header="$1" cap="$2"; shift 2
    local files file
    files="$(changed "$@")"
    if [[ -z "$files" ]]; then
        return 0
    fi
    echo
    echo "$header"
    while IFS= read -r file; do
        echo "  $file"
        if [[ "$cap" -gt 0 ]]; then
            show "$cap" "$file"
        fi
    done <<<"$files"
}

echo
echo "=== golden changes ==="
# Verification diagnostics are a few lines each, and are the thing most likely
# to be recorded by accident, so they are shown whole.
report "verification produced diagnostics for these; confirm that is intended:" 40 \
    '*.viper.diag.txt'
report "conversion output changed:" 40 \
    '*.fir.diag.txt'
report "diagnostic markers changed:" 40 \
    'formver.compiler-plugin/testData/*.kt' \
    'formver.compiler-plugin/locality/testData/*.kt'
report "regenerated test registration; commit as-is:" 0 \
    '*TestGenerated.java'

echo
echo "=== check-testdata.sh ==="
"$SCRIPT_DIR/check-testdata.sh" || true

cat <<'EOF'

Regeneration records whatever the run produced. What is above is what these
tests now assert: read it and confirm it is what you meant.
EOF
