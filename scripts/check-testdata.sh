#!/usr/bin/env bash
# check-testdata.sh — Structural checks on testData. Needs no build.
#
# Usage:
#   ./scripts/check-testdata.sh

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

TEST_DATA_DIRS=(
    formver.compiler-plugin/testData
    formver.compiler-plugin/locality/testData
)

status=0

# A missing directory would make find fail, and inside the process
# substitutions below that status is discarded: the checks would quietly cover
# less than they claim to. Checked here, where the failure is still ours to
# report.
for dir in "${TEST_DATA_DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        echo "testData directory is missing: $dir" >&2
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
    echo "testData checks passed"
fi
exit "$status"
