#!/usr/bin/env bash
set -euo pipefail

readonly TEST_NAME="${1:-exists_boolean_witness}"
readonly MAX_RUNS=3
readonly AGENCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_DIR="$(cd "$AGENCY_DIR/.." && pwd)"
readonly TEST_DATA_DIR="$REPO_DIR/formver.compiler-plugin/testData/diagnostics/verification/user_invariants"
readonly GENERATED_FILE="$REPO_DIR/formver.compiler-plugin/test-gen/org/jetbrains/kotlin/formver/plugin/runners/PhasedDiagnosticTestGenerated.java"
readonly RUN_COUNT_FILE="$AGENCY_DIR/.${TEST_NAME}.runs"

if [[ ! -f "$AGENCY_DIR/$TEST_NAME.kt" ]]; then
    echo "Unknown agency test: $TEST_NAME" >&2
    exit 2
fi

run_count=0
if [[ -f "$RUN_COUNT_FILE" ]]; then
    read -r run_count < "$RUN_COUNT_FILE"
fi
if (( run_count >= MAX_RUNS )); then
    echo "$TEST_NAME has reached its $MAX_RUNS-run lifetime limit" >&2
    exit 2
fi

tmp_dir="$(mktemp -d)"
cleanup() {
    rm -f "$TEST_DATA_DIR/$TEST_NAME.kt" \
        "$TEST_DATA_DIR/$TEST_NAME.fir.diag.txt" \
        "$TEST_DATA_DIR/$TEST_NAME.viper.diag.txt"
    if [[ -f "$tmp_dir/generated.java" ]]; then
        cp "$tmp_dir/generated.java" "$GENERATED_FILE"
    fi
    rm -rf "$tmp_dir"
}
trap cleanup EXIT

cp "$GENERATED_FILE" "$tmp_dir/generated.java"
cp "$AGENCY_DIR/$TEST_NAME.kt" "$TEST_DATA_DIR/$TEST_NAME.kt"
for suffix in fir.diag.txt viper.diag.txt; do
    if [[ -f "$AGENCY_DIR/$TEST_NAME.$suffix" ]]; then
        cp "$AGENCY_DIR/$TEST_NAME.$suffix" "$TEST_DATA_DIR/$TEST_NAME.$suffix"
    fi
done

cd "$REPO_DIR"
./agent-scripts/test.sh --verify "$TEST_NAME"
printf '%s\n' "$((run_count + 1))" > "$RUN_COUNT_FILE"
