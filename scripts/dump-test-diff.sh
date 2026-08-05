#!/usr/bin/env bash
# dump-test-diff.sh — Run a single failing test and dump the assertion diff.
#
# The Kotlin compiler test framework compares golden files (.fir.diag.txt, .kt
# with diagnostic markers) inside a forked test JVM. Gradle's cross-JVM
# serialization strips AssertionFailedError expected/actual values, so you
# never see the diff in normal test output.
#
# This script works around that by temporarily registering a JUnit 5
# TestWatcher extension (DumpAssertionDiffExtension, in test-fixtures) that
# catches failures inside the test JVM and writes the diff to
# $SNAKT_TEST_DUMP_DIR/test-assertion-dump-*.txt.
#
# It then post-processes each dump into test-assertion-diff-*.txt in the same
# directory — a unified diff with source-position prefixes (e.g.
# "/foo.kt:(23,31):") replaced by ":(_,_):" so methods that only had their
# offsets shifted by unrelated edits do not appear as spurious changes. The
# raw dumps are kept alongside in case the original offsets matter.
#
# Usage:
#   ./scripts/dump-test-diff.sh "testIs_type_contract"
#   SNAKT_TEST_DUMP_DIR=/var/tmp/snakt ./scripts/dump-test-diff.sh "testFoo"

set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <test-method-name-pattern>"
    echo "Example: $0 'testIs_type_contract'"
    echo "Set SNAKT_TEST_DUMP_DIR to override the output directory."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"

TEST_PATTERN="$(gradle_filter "$1")"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Per-user, since the cleanup below globs the directory and a shared /tmp
# yields files owned by someone else.
DUMP_DIR="${SNAKT_TEST_DUMP_DIR:-${TMPDIR:-/tmp}/snakt-test-diff-$(id -u)}"
mkdir -p "$DUMP_DIR"
export SNAKT_TEST_DUMP_DIR="$DUMP_DIR"

SERVICES_DIR="$ROOT_DIR/formver.compiler-plugin/testData/META-INF/services"
SERVICES_FILE="$SERVICES_DIR/org.junit.jupiter.api.extension.Extension"
PLATFORM_PROPS="$ROOT_DIR/formver.compiler-plugin/testData/junit-platform.properties"

rm -f "$DUMP_DIR"/test-assertion-dump-*.txt "$DUMP_DIR"/test-assertion-diff-*.txt

# Register the extension via auto-detection. The DumpAssertionDiffExtension
# class itself lives in test-fixtures and is inert unless both of these files
# are present at test time.
mkdir -p "$SERVICES_DIR"
echo "org.jetbrains.kotlin.formver.plugin.DumpAssertionDiffExtension" > "$SERVICES_FILE"
echo "junit.jupiter.extensions.autodetection.enabled=true" > "$PLATFORM_PROPS"

cleanup() {
    rm -f "$SERVICES_FILE"
    rm -f "$PLATFORM_PROPS"
    rmdir "$SERVICES_DIR" 2>/dev/null || true
    rmdir "$(dirname "$SERVICES_DIR")" 2>/dev/null || true
    # Also clean up the build copy so it doesn't persist across runs
    rm -f "$ROOT_DIR/formver.compiler-plugin/build/resources/test/junit-platform.properties"
    rm -rf "$ROOT_DIR/formver.compiler-plugin/build/resources/test/META-INF/services"
}
trap cleanup EXIT

echo "Running test: $TEST_PATTERN"
echo "Raw dumps at $DUMP_DIR/test-assertion-dump-*.txt; normalized diffs at $DUMP_DIR/test-assertion-diff-*.txt"
echo

# --rerun forces the test task to run even on identical inputs (so re-running
# the script always captures a fresh assertion); upstream tasks like
# processTestResources still honor input tracking and stay UP-TO-DATE when
# they can, which keeps repeat runs cheap.
# -q suppresses per-task lifecycle logs; we expect a failing test, and the
# captured diff is the interesting output. Compile/configuration errors still
# print at ERROR level.
cd "$ROOT_DIR"
./gradlew :formver.compiler-plugin:test \
    --tests "*$TEST_PATTERN*" \
    --rerun \
    --no-daemon \
    -q \
    2>&1 && test_status=0 || test_status=$?

# Replace source-position offsets like ":(23,31):" with ":(_,_):" so methods
# that only shifted by edits to earlier code drop out of the diff. Restricted
# to lines starting with a "/path:" prefix to avoid false matches.
normalize_positions() {
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

shopt -s nullglob
for dump in "$DUMP_DIR"/test-assertion-dump-*.txt; do
    base="$(basename "$dump" .txt)"
    base="${base#test-assertion-dump-}"
    exp_file="$(mktemp)"; act_file="$(mktemp)"
    exp_norm="$(mktemp)"; act_norm="$(mktemp)"
    split_dump "$dump" "$exp_file" "$act_file"
    normalize_positions < "$exp_file" > "$exp_norm"
    normalize_positions < "$act_file" > "$act_norm"
    # -B drops hunks that are pure blank-line drift (golden files don't always
    # end in exactly the same number of newlines); real whitespace differences
    # inside content lines are still reported.
    diff -u -B --label "expected (positions normalized)" --label "actual (positions normalized)" \
        "$exp_norm" "$act_norm" > "$DUMP_DIR/test-assertion-diff-$base.txt" || true
    rm -f "$exp_file" "$act_file" "$exp_norm" "$act_norm"
done

echo
echo "=== Normalized diffs (source-position offsets stripped) ==="
shown=0
for f in "$DUMP_DIR"/test-assertion-diff-*.txt; do
    if [[ -s "$f" ]]; then
        echo
        echo "--- $(basename "$f") ---"
        cat "$f"
        shown=1
    fi
done

if [[ $shown -eq 0 ]]; then
    if compgen -G "$DUMP_DIR/test-assertion-dump-*.txt" >/dev/null; then
        echo "(no real differences after normalizing positions — all changes were just offset shifts)"
        echo "Raw dumps remain at $DUMP_DIR/test-assertion-dump-*.txt"
    else
        echo "(no diffs captured — test may have passed or failed with a non-assertion error)"
    fi
fi

# The diff is output, not a verdict: a caller exec'ing this script gets its
# status as the test's.
exit "$test_status"
