#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_NAME="${1:?usage: run-test.sh TEST_NAME [--update-goldens]}"
MODE="${2:---verify}"
CASE_DIR="$SCRIPT_DIR/cases/$TEST_NAME"
STAGED_DIR="$REPO_ROOT/formver.compiler-plugin/testData/diagnostics/testing_agency"
RUNNER="$REPO_ROOT/formver.compiler-plugin/test-gen/org/jetbrains/kotlin/formver/plugin/runners/PhasedDiagnosticTestGenerated.java"
RUNNER_BACKUP="$(mktemp)"

if [[ ! -d "$CASE_DIR" ]]; then
    echo "Unknown testing-agency test: $TEST_NAME" >&2
    exit 1
fi
if [[ "$MODE" != "--verify" && "$MODE" != "--update-goldens" ]]; then
    echo "Mode must be --verify or --update-goldens" >&2
    exit 1
fi

cp "$RUNNER" "$RUNNER_BACKUP"
cleanup() {
    cp "$RUNNER_BACKUP" "$RUNNER"
    rm -f "$RUNNER_BACKUP"
    rm -rf "$STAGED_DIR"
}
trap cleanup EXIT

mkdir -p "$STAGED_DIR"
cp "$CASE_DIR"/* "$STAGED_DIR/"

cd "$REPO_ROOT"
./gradlew :formver.compiler-plugin:generateTests --no-daemon -q
set +e
./agent-scripts/test.sh "$MODE" "$STAGED_DIR/$TEST_NAME.kt"
TEST_STATUS=$?
set -e

if [[ "$MODE" == "--update-goldens" ]]; then
    find "$STAGED_DIR" -maxdepth 1 -type f ! -name '*.kt' -exec cp {} "$CASE_DIR/" \;
fi

exit "$TEST_STATUS"
