#!/usr/bin/env bash
# run-test.sh — Run one test in full verification mode and show why it failed.
#
# Gradle's cross-JVM serialization strips expected/actual values off golden-file
# assertions, so a bare run reports only AssertionFailedError. On failure this
# re-runs through dump-test-diff.sh to recover the diff.
#
# Usage:
#   ./scripts/run-test.sh testMax_of_two
#   ./scripts/run-test.sh Max_of_two
#   ./scripts/run-test.sh max_of_two

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
# found from the source file: max_of_two.kt backs testMax_of_two.
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
if ./gradlew "$module:test" --tests "*$FILTER*" --no-daemon -q 2>&1; then
    if report_ran_tests "$MARKER" "$PATTERN"; then
        rm -f "$MARKER"
        exit 0
    fi
    rm -f "$MARKER"
    exit 1
fi
rm -f "$MARKER"

# DumpAssertionDiffExtension lives in the compiler-plugin test fixtures, which
# are not on the locality test classpath.
if [[ "$module" == *":locality" ]]; then
    echo
    echo "FAILED. Expected/actual values are in the HTML report:"
    echo "  formver.compiler-plugin/locality/build/reports/tests/test/index.html"
    exit 1
fi

echo
echo "FAILED. Recovering the assertion diff:"
echo
exec "$SCRIPT_DIR/dump-test-diff.sh" "$FILTER"
