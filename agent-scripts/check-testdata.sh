#!/usr/bin/env bash
# check-testdata.sh — Structural checks on test data. Takes no arguments and
# needs no build.

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

TEST_DATA_DIRS=(
    formver.compiler-plugin/testData
    formver.compiler-plugin/bugReproductions
    formver.compiler-plugin/locality/testData
)

status=0

# The process substitutions below discard find's status, so a missing directory
# would silently narrow the checks. Caught here instead.
for dir in "${TEST_DATA_DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        echo "test data directory is missing: $dir" >&2
        exit 1
    fi
done

golden_files() {
    find "${TEST_DATA_DIRS[@]}" \
        \( -name "*.fir.diag.txt" -o -name "*.viper.diag.txt" \)
}

# Golden files are keyed to a .kt of the same stem. Renaming or deleting the
# source leaves the golden behind, asserted against nothing.
while read -r f; do
    src="${f%.fir.diag.txt}"
    src="${src%.viper.diag.txt}"
    if [ ! -f "$src.kt" ]; then
        echo "golden file with no .kt source: $f"
        status=1
    fi
done < <(golden_files)

# DiagnosticsCollector writes an empty file rather than deleting it when the
# diagnostics it recorded go away.
while read -r f; do
    if [ ! -s "$f" ]; then
        echo "empty golden file: $f"
        status=1
    fi
done < <(golden_files)

if [ "$status" -eq 0 ]; then
    echo "test data checks passed"
fi
exit "$status"
