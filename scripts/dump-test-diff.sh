#!/usr/bin/env bash
# dump-test-diff.sh — Run a single failing test and dump the assertion diff.
#
# The Kotlin compiler test framework compares golden files (.fir.diag.txt, .kt
# with diagnostic markers) inside a forked test JVM. Gradle's cross-JVM
# serialization strips AssertionFailedError expected/actual values, so you
# never see the diff in normal test output.
#
# This script works around that by temporarily registering DumpAssertionDiffExtension
# (see lib.sh), which catches failures inside the test JVM and writes the diff to
# $SNAKT_TEST_DUMP_DIR/test-assertion-dump-*.txt, then post-processes each dump into
# test-assertion-diff-*.txt in the same directory — a unified diff with source-position
# prefixes (e.g. "/foo.kt:(23,31):") replaced by ":(_,_):" so methods that only had
# their offsets shifted by unrelated edits do not appear as spurious changes. The raw
# dumps are kept alongside in case the original offsets matter.
#
# run-test.sh does the same thing as part of its one gradle run; this script
# exists for driving the extension directly, standalone.
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

DUMP_DIR="$(dump_dir_default)"
mkdir -p "$DUMP_DIR"
export SNAKT_TEST_DUMP_DIR="$DUMP_DIR"

rm -f "$DUMP_DIR"/test-assertion-dump-*.txt "$DUMP_DIR"/test-assertion-diff-*.txt

install_dump_extension
trap remove_dump_extension EXIT

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

render_dump_diffs "$DUMP_DIR" || true

# The diff is output, not a verdict: the status reported is the test's.
exit "$test_status"
