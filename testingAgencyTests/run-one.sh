#!/usr/bin/env bash

set -euo pipefail

readonly AGENCY_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly REPO_DIR="$(cd "$AGENCY_DIR/.." && pwd)"
readonly TOPIC="${1:-}"
readonly CASE_DIR="$AGENCY_DIR/cases/$TOPIC"
readonly STAGE_DIR="$REPO_DIR/formver.compiler-plugin/testData/diagnostics/testing_agency"

if [[ -z "$TOPIC" || ! -d "$CASE_DIR" ]]; then
    echo "Usage: $0 <topic>" >&2
    echo "Available topics:" >&2
    find "$AGENCY_DIR/cases" -mindepth 1 -maxdepth 1 -type d -printf '  %f\n' | sort >&2
    exit 2
fi

cleanup() {
    find "$STAGE_DIR" -maxdepth 1 -type f -delete 2>/dev/null || true
    rmdir "$STAGE_DIR" 2>/dev/null || true
    "$REPO_DIR/gradlew" -q :formver.compiler-plugin:generateTests
}
trap cleanup EXIT

mkdir -p "$STAGE_DIR"
cp "$CASE_DIR"/* "$STAGE_DIR"/
"$REPO_DIR/gradlew" -q :formver.compiler-plugin:generateTests
"$REPO_DIR/agent-scripts/test.sh" "$TOPIC"
