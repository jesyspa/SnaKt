# lib.sh — helpers shared by the scripts in this directory. Source, don't run.

# $0 is the caller's, so BASH_SOURCE is what locates junit_first_failure.py.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Where each module's Gradle test tasks write their JUnit XML. Relative to the
# repository root, which the scripts cd to.
COMPILER_RESULTS_DIR=formver.compiler-plugin/build/test-results
LOCALITY_RESULTS_DIR=formver.compiler-plugin/locality/build/test-results

# Missing python3 must be said out loud: reporting no failures instead reads as
# a run that passed.
need_python3() {
    if command -v python3 >/dev/null 2>&1; then
        return 0
    fi
    echo "python3 is needed to read Gradle's test results, and is not on PATH." >&2
    return 1
}

# Turn a test name into the pattern Gradle's --tests expects.
#
# GenerateTestsKt capitalizes a testData stem and turns dashes into underscores
# to form the method name (non-local-returns.kt backs testNon_local_returns),
# and --tests is case-sensitive. A name already in method form, and a path to
# the .kt, are both accepted.
gradle_filter() {
    local pattern="${1##*/}"
    pattern="${pattern%.kt}"
    if [[ "$pattern" == test* ]]; then
        printf '%s' "$pattern"
    else
        printf '%s%s' "$(printf '%s' "${pattern:0:1}" | tr '[:lower:]' '[:upper:]')" "${pattern:1}" \
            | tr '-' '_'
    fi
}

# True if a JUnit "type" attribute names a golden-file mismatch rather than a
# thrown exception: assertEqualsToFile raises opentest4j's AssertionFailedError,
# and *ComparisonFailure covers both org.junit's and com.intellij's. Only these
# carry expected/actual values for render_dump_diffs to recover.
is_assertion_failure_type() {
    case "$1" in
        org.opentest4j.AssertionFailedError|*ComparisonFailure) return 0 ;;
        *) return 1 ;;
    esac
}

# Print the first failing <testcase> from JUnit XML newer than $1: failure
# "type" on the first line, then "classname.name: message", then stack trace.
# Returns 1 with nothing printed if there is no fresh XML at all, or none of it
# holds a failure.
report_first_xml_failure() {
    need_python3 || return 1
    local marker="$1" dirs=() dir
    for dir in "$COMPILER_RESULTS_DIR" "$LOCALITY_RESULTS_DIR"; do
        if [[ -d "$dir" ]]; then
            dirs+=("$dir")
        fi
    done
    if [[ "${#dirs[@]}" -eq 0 ]]; then
        return 1
    fi
    # Sorted: find's order is the filesystem's, and with two failing tests
    # "the first failure" would vary between runs of the same failure.
    local files=()
    while IFS= read -r f; do
        files+=("$f")
    done < <(find "${dirs[@]}" -name '*.xml' -newer "$marker" | sort)
    if [[ "${#files[@]}" -eq 0 ]]; then
        return 1
    fi
    python3 "$LIB_DIR/junit_first_failure.py" "${files[@]}"
}

# Print "total assertion_failed other_failed skipped unreadable" over the JUnit
# XML in directory $1 written since marker file $2. Prints nothing and returns
# 1 when the module left no fresh XML, 2 when the counts could not be read at
# all; a caller that reports the two as one cause names the wrong one.
count_xml_results() {
    need_python3 || return 2
    local dir="$1" marker="$2"
    if [[ ! -d "$dir" ]]; then
        return 1
    fi
    local files=()
    while IFS= read -r f; do
        files+=("$f")
    done < <(find "$dir" -name '*.xml' -newer "$marker")
    if [[ "${#files[@]}" -eq 0 ]]; then
        return 1
    fi
    python3 "$LIB_DIR/junit_counts.py" "${files[@]}" || return 2
}

# Where DumpAssertionDiffExtension writes its dumps (see docs/agents-dev.md).
# Per-user, because callers glob and clear this directory and a shared /tmp
# would hand them someone else's files.
dump_dir_default() {
    printf '%s' "${SNAKT_TEST_DUMP_DIR:-${TMPDIR:-/tmp}/snakt-test-diff-$(id -u)}"
}

# Replace source-position offsets like ":(23,31):" with ":(_,_):", so a method
# that only shifted because of an edit above it drops out of the diff. Anchored
# on the "/path:" prefix to avoid matching content that looks similar.
normalize_dump_positions() {
    sed -E 's#^(/[^:]+):\([0-9]+,[0-9]+\):#\1:(_,_):#'
}

split_dump() {
    local dump="$1" expected_path="$2" actual_path="$3"
    awk -v exp_out="$expected_path" -v act_out="$actual_path" '
        /^=== EXPECTED ===$/ { side = "expected"; next }
        /^=== ACTUAL ===$/   { side = "actual";   next }
        side == "expected" { print > exp_out }
        side == "actual"   { print > act_out }
    ' "$dump"
}

# Diff every test-assertion-dump-*.txt in $1 into a test-assertion-diff-*.txt
# beside it, then print the non-empty ones. Returns 1 if there were no dumps.
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
        # -B drops hunks that are only blank-line drift; goldens do not always
        # end in the same number of newlines. Whitespace inside a content line
        # is still reported.
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
