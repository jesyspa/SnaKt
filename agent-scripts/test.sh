#!/usr/bin/env bash
# test.sh — Drive the formver test suite. --help for usage.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=agent-scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"
cd "$SCRIPT_DIR/.."

usage() {
    cat <<'EOF'
Usage:
  ./agent-scripts/test.sh [pattern]                  # conversion only — the fast loop, default
  ./agent-scripts/test.sh --verify [pattern]         # full pipeline
  ./agent-scripts/test.sh --update-goldens [pattern] # regenerate goldens, then report what changed

A pattern can be given as the testData file is named (assign_local), as the
path to it, or as the generated test method (testAssign_local).
EOF
}

MODE=conversion
set_mode() {
    if [[ "$MODE" != conversion && "$MODE" != "$1" ]]; then
        echo "--verify and --update-goldens select different runs; pass one." >&2
        exit 1
    fi
    MODE="$1"
}

while [[ "${1:-}" == -* ]]; do
    case "$1" in
        --verify) set_mode verify ;;
        --update-goldens) set_mode update ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown flag: $1" >&2; echo >&2; usage >&2; exit 1 ;;
    esac
    shift
done

PATTERN="${1:-}"
if [[ $# -gt 1 ]]; then
    echo "Only one pattern is accepted; got: $*" >&2
    exit 1
fi

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

# DumpAssertionDiffExtension only fires when this is set (see docs/agents-dev.md).
DUMP_DIR="$(dump_dir_default)"
mkdir -p "$DUMP_DIR"
rm -f "$DUMP_DIR"/test-assertion-dump-*.txt "$DUMP_DIR"/test-assertion-diff-*.txt
export SNAKT_TEST_DUMP_DIR="$DUMP_DIR"

MARKER="$(mktemp)"

matched=0
overall_status=0
total_tests=0
total_rewritten=0
total_failed=0
total_skipped=0
total_unreadable=0
# Modules that ran but whose count could not be established. Without these the
# summary would describe the modules it did count as if they were the whole run.
no_results=()
unreadable_results=()

run_task() {
    if TASK_OUT="$(./gradlew "$1" "${args[@]}" 2>&1)"; then
        TASK_STATUS=0
    else
        TASK_STATUS=$?
    fi
}

report_compiler_failure() {
    local failure_info
    failure_info="$(report_first_xml_failure "$MARKER" || true)"
    if [[ -z "$failure_info" ]]; then
        # The task died before any test ran; Gradle's error output above says why.
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

tally() {
    local module="$1" counts status ran rewritten failed skipped unreadable
    counts="$(count_xml_results "$2" "$MARKER")" && status=0 || status=$?
    case "$status" in
        1) no_results+=("$module"); return ;;
        2) unreadable_results+=("$module"); return ;;
    esac
    read -r ran rewritten failed skipped unreadable <<<"$counts"
    total_tests=$((total_tests + ran))
    total_rewritten=$((total_rewritten + rewritten))
    total_failed=$((total_failed + failed))
    total_skipped=$((total_skipped + skipped))
    total_unreadable=$((total_unreadable + unreadable))
}

# In --update-goldens mode a matching test is expected to fail: assertEqualsToFile
# writes the golden and then fails. Only "no tests found" means anything there.
run_module() {
    local module="$1" task="$2" results_dir="$3" on_failure="$4"
    run_task "$task"
    if [[ -n "$PATTERN" && "$TASK_OUT" == *"No tests found for given includes"* ]]; then
        return
    fi
    matched=1
    tally "$module" "$results_dir"
    if [[ "$MODE" == update || "$TASK_STATUS" -eq 0 ]]; then
        return
    fi
    overall_status=1
    # Gradle's closing advice is about Gradle, not about the failure.
    echo "$TASK_OUT" | grep -v '^\* Try:\|^> Run with \|^> Get more help ' || true
    "$on_failure"
}

run_module compiler "$COMPILER_TASK" "$COMPILER_RESULTS_DIR" report_compiler_failure
run_module locality "$LOCALITY_TASK" "$LOCALITY_RESULTS_DIR" report_locality_failure
rm -f "$MARKER"

if [[ "$matched" -eq 0 ]]; then
    echo "No test matches '$PATTERN'."
    exit 1
fi

# A run that says nothing reads the same whether it tested everything or
# nothing, so the count is printed even when everything passed.
summary() {
    local failed=$((total_rewritten + total_failed)) line
    local passed=$((total_tests - failed - total_skipped))
    if [[ "$total_tests" -gt 0 ]]; then
        if [[ "$MODE" == update ]]; then
            # assertEqualsToFile writes the golden and then fails, so a mismatch
            # here is a golden that got rewritten. Anything else really failed.
            line="Ran $total_tests tests, $total_rewritten golden(s) rewritten"
            if [[ "$total_failed" -gt 0 ]]; then
                line+=", $total_failed failed for other reasons"
            fi
        else
            line="Ran $total_tests tests, $passed passed, $failed failed"
        fi
        if [[ "$total_skipped" -gt 0 ]]; then
            line+=", $total_skipped skipped"
        fi
        echo "$line."
    fi
    # A module that ran and left no results is the case where a count over the
    # other module alone reads as a clean run.
    local module
    for module in "${no_results[@]+"${no_results[@]}"}"; do
        echo "The $module module produced no test results; see its output above."
    done
    for module in "${unreadable_results[@]+"${unreadable_results[@]}"}"; do
        echo "The $module module's test results could not be read; it is not counted above."
    done
    if [[ "$total_unreadable" -gt 0 ]]; then
        echo "$total_unreadable result file(s) were unparseable, so the count is a lower bound."
    fi
}

echo
summary

if [[ "$MODE" != update ]]; then
    exit "$overall_status"
fi

# --no-renames, so a rename arrives as a delete plus an add rather than as one
# "old -> new" entry that is not a readable path. Paths gone from disk are
# dropped for the same reason.
changed() {
    local file
    while IFS= read -r file; do
        if [[ -f "$file" ]]; then
            printf '%s\n' "$file"
        fi
    done < <(git status --porcelain --no-renames -- "$@" | sed -E 's/^.{3}//')
}

# What the golden now says, not just that it changed: going to look at the file
# is the step that gets skipped. An untracked golden has no diff, so its whole
# body is the new content.
show() {
    local cap="$1" file="$2" body
    if git ls-files --error-unmatch "$file" >/dev/null 2>&1; then
        # Drop git's four header lines; the path is printed above.
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
# Verification diagnostics are short and are what gets recorded by accident,
# so they are shown whole.
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
