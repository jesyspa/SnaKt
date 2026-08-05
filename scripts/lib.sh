# lib.sh — helpers shared by the scripts in this directory. Source, don't run.

# Sourced, not executed, so the caller's $0/SCRIPT_DIR can't be relied on to
# find the Python helpers below.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC2034 # used by callers to cd into, not within this file
ROOT_DIR="$(cd "$LIB_DIR/.." && pwd)"

# Gradle's JUnit XML carries failure messages with escaped entities and line
# breaks, so the functions below parse it rather than grepping it. Absence of
# the parser must say so: silently reporting no tests reads as a passing run
# that executed nothing.
need_python3() {
    if command -v python3 >/dev/null 2>&1; then
        return 0
    fi
    echo "python3 is needed to read Gradle's test results, and is not on PATH." >&2
    return 1
}

# Turn a test name into the pattern Gradle's --tests expects.
#
# GenerateTestsKt capitalizes the first letter of a testData file's stem and
# turns dashes into underscores to form the JUnit method name (assign_local.kt
# backs testAssign_local, non-local-returns.kt backs testNon_local_returns),
# and the --tests filter is case-sensitive, so a pattern taken verbatim from
# the file name would match nothing. A name already in method form is passed
# through.
gradle_filter() {
    local pattern="$1"
    if [[ "$pattern" == test* ]]; then
        printf '%s' "$pattern"
    else
        printf '%s%s' "$(printf '%s' "${pattern:0:1}" | tr '[:lower:]' '[:upper:]')" "${pattern:1}" \
            | tr '-' '_'
    fi
}

# True if a JUnit <failure>/<error> "type" attribute names an exception
# DumpAssertionDiffExtension can pull expected/actual out of: opentest4j's
# AssertionFailedError (assertEqualsToFile) or a *ComparisonFailure (covers
# both org.junit.ComparisonFailure and com.intellij's FileComparisonFailure).
# Anything else is a thrown exception, not a golden-file mismatch, and there
# is no diff for render_dump_diffs to recover.
is_assertion_failure_type() {
    case "$1" in
        org.opentest4j.AssertionFailedError|*ComparisonFailure) return 0 ;;
        *) return 1 ;;
    esac
}

# Print the first failing <testcase> from JUnit XML newer than $1, across
# formver.compiler-plugin's and formver.compiler-plugin/locality's
# test-results directories: its failure "type" on the first line, then
# "classname.name: message", then a few lines of stack trace.
#
# Reused so callers can decide what a failure actually was before acting on
# it, instead of assuming a golden-file mismatch and rendering dumps only to
# find nothing there. Returns 1 with nothing printed if there is no fresh XML
# at all (the run died before any test executed) or no failing testcase in it.
report_first_xml_failure() {
    need_python3 || return 1
    local marker="$1" dirs=() dir
    for dir in formver.compiler-plugin/build/test-results \
               formver.compiler-plugin/locality/build/test-results; do
        if [[ -d "$dir" ]]; then
            dirs+=("$dir")
        fi
    done
    if [[ "${#dirs[@]}" -eq 0 ]]; then
        return 1
    fi
    local files=()
    while IFS= read -r f; do
        files+=("$f")
    done < <(find "${dirs[@]}" -name '*.xml' -newer "$marker")
    if [[ "${#files[@]}" -eq 0 ]]; then
        return 1
    fi
    python3 "$LIB_DIR/junit_first_failure.py" "${files[@]}"
}

# DumpAssertionDiffExtension (formver.compiler-plugin test-fixtures) is a
# JUnit 5 TestWatcher, registered unconditionally via test-resources/, that
# catches a failing golden-file assertion inside the forked test JVM — before
# Gradle's cross-JVM result serialization strips the expected/actual values
# off it — and writes them to $SNAKT_TEST_DUMP_DIR/test-assertion-dump-*.txt
# whenever that variable is set. It only reaches :formver.compiler-plugin:test
# and :untilConversion; :formver.compiler-plugin:locality has no test-fixtures
# on its classpath to find the class by.

# Default location for assertion dumps, overridable via SNAKT_TEST_DUMP_DIR.
# Per-user: callers glob and clear this directory, and a shared /tmp would
# pick up files left behind by someone else.
dump_dir_default() {
    printf '%s' "${SNAKT_TEST_DUMP_DIR:-${TMPDIR:-/tmp}/snakt-test-diff-$(id -u)}"
}

# Replace source-position offsets like ":(23,31):" with ":(_,_):" so methods
# that only shifted by edits to earlier code drop out of the diff. Restricted
# to lines starting with a "/path:" prefix to avoid false matches.
normalize_dump_positions() {
    sed -E 's#^(/[^:]+):\([0-9]+,[0-9]+\):#\1:(_,_):#'
}

# Split a dump file at the "=== ACTUAL ===" marker into two files.
split_dump() {
    local dump="$1" expected_path="$2" actual_path="$3"
    awk -v exp_out="$expected_path" -v act_out="$actual_path" '
        /^=== EXPECTED ===$/ { side = "expected"; next }
        /^=== ACTUAL ===$/   { side = "actual";   next }
        side == "expected" { print > exp_out }
        side == "actual"   { print > act_out }
    ' "$dump"
}

# Turn every test-assertion-dump-*.txt in $1 into a test-assertion-diff-*.txt
# alongside it (split into expected/actual, normalize position offsets, unified
# diff), then print the non-empty diffs. Returns 1 if no dump files were
# present at all (nothing for the caller to have recovered).
render_dump_diffs() {
    local dump_dir="$1"
    # A subshell, so nullglob does not leak into the caller's globbing.
    (
    shopt -s nullglob
    local dump base exp_file act_file exp_norm act_norm
    for dump in "$dump_dir"/test-assertion-dump-*.txt; do
        base="$(basename "$dump" .txt)"
        base="${base#test-assertion-dump-}"
        exp_file="$(mktemp)"; act_file="$(mktemp)"
        exp_norm="$(mktemp)"; act_norm="$(mktemp)"
        split_dump "$dump" "$exp_file" "$act_file"
        normalize_dump_positions < "$exp_file" > "$exp_norm"
        normalize_dump_positions < "$act_file" > "$act_norm"
        # -B drops hunks that are pure blank-line drift (golden files don't
        # always end in exactly the same number of newlines); real whitespace
        # differences inside content lines are still reported.
        diff -u -B --label "expected (positions normalized)" --label "actual (positions normalized)" \
            "$exp_norm" "$act_norm" > "$dump_dir/test-assertion-diff-$base.txt" || true
        rm -f "$exp_file" "$act_file" "$exp_norm" "$act_norm"
    done

    echo
    echo "=== Normalized diffs (source-position offsets stripped) ==="
    local f shown=0
    for f in "$dump_dir"/test-assertion-diff-*.txt; do
        if [[ -s "$f" ]]; then
            echo
            echo "--- $(basename "$f") ---"
            cat "$f"
            shown=1
        fi
    done

    if [[ $shown -eq 1 ]]; then
        exit 0
    fi
    if compgen -G "$dump_dir/test-assertion-dump-*.txt" >/dev/null; then
        echo "(no real differences after normalizing positions — all changes were just offset shifts)"
        echo "Raw dumps remain at $dump_dir/test-assertion-dump-*.txt"
        exit 0
    fi
    echo "(no diffs captured — test may have passed or failed with a non-assertion error)"
    exit 1
    )
}
