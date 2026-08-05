#!/usr/bin/env bash
# run-test.sh — Run one test in full verification mode and show why it failed.
#
# Gradle's cross-JVM serialization strips expected/actual values off golden-file
# assertions, so a bare run reports only AssertionFailedError. On a golden-file
# mismatch this re-runs through dump-test-diff.sh to recover the diff; for any
# other failure (a thrown exception, a missing directive) the JUnit XML already
# has the real message and stack trace, so it's read and printed directly
# instead of paying for a second build that would recover nothing.
#
# Usage:
#   ./scripts/run-test.sh testAssign_local
#   ./scripts/run-test.sh Assign_local
#   ./scripts/run-test.sh assign_local

set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <test-method-name-pattern>"
    exit 1
fi

PATTERN="$1"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

# Test methods are derived from testData file names, so the owning module can be
# found from the source file: assign_local.kt backs testAssign_local.
stem="$(echo "${PATTERN#test}" | tr '[:upper:]' '[:lower:]')"
module=":formver.compiler-plugin"
while read -r f; do
    base="$(basename "$f" .kt | tr '[:upper:]-' '[:lower:]_')"
    if [[ "$base" == *"$stem"* && "$f" == *"/locality/testData/"* ]]; then
        module=":formver.compiler-plugin:locality"
    fi
done < <(find formver.compiler-plugin/testData formver.compiler-plugin/locality/testData -name "*.kt")

FILTER="$(gradle_filter "$PATTERN")"

echo "Running $PATTERN in $module"
MARKER="$(mktemp)"
if gradle_out="$(./gradlew "$module:test" --tests "*$FILTER*" --no-daemon -q 2>&1)"; then
    status=0
else
    status=$?
fi

if [[ "$status" -eq 0 ]]; then
    if report_ran_tests "$MARKER" "$PATTERN"; then
        rm -f "$MARKER"
        exit 0
    fi
    rm -f "$MARKER"
    exit 1
fi

# Gradle's closing advice is about Gradle, not about the failure.
echo "$gradle_out" | grep -v '^\* Try:\|^> Run with \|^> Get more help ' || true

# DumpAssertionDiffExtension lives in the compiler-plugin test fixtures, which
# are not on the locality test classpath.
if [[ "$module" == *":locality" ]]; then
    rm -f "$MARKER"
    echo
    echo "FAILED. Expected/actual values are in the HTML report:"
    echo "  formver.compiler-plugin/locality/build/reports/tests/test/index.html"
    exit 1
fi

# Look at what actually failed before assuming it's a golden-file mismatch:
# escalating to dump-test-diff.sh only pays off for the assertion family it
# knows how to recover expected/actual from.
failure_info="$(report_first_xml_failure "$MARKER" || true)"
rm -f "$MARKER"

if [[ -z "$failure_info" ]]; then
    # No JUnit XML at all: the task died before any test ran (a real compile
    # error in the plugin or test sources). Gradle's own "e:" lines, already
    # printed above, are the answer.
    exit 1
fi

if is_assertion_failure_type "$(head -1 <<<"$failure_info")"; then
    echo
    echo "FAILED. Recovering the assertion diff:"
    echo
    exec "$SCRIPT_DIR/dump-test-diff.sh" "$FILTER"
fi

echo
echo "FAILED. Not a golden-file assertion — no diff to recover. From the test run:"
echo
tail -n +2 <<<"$failure_info"
exit 1
