#!/usr/bin/env bash
# check-conversion.sh — The fast loop: everything that runs without Z3.
#
# Usage:
#   ./scripts/check-conversion.sh                # every test
#   ./scripts/check-conversion.sh Assign_local   # one test
#   ./scripts/check-conversion.sh assign_local   # same, as the file is named

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

PATTERN="${1:-}"


args=(--no-daemon -q)
if [[ -n "$PATTERN" ]]; then
    args+=(--tests "*$(gradle_filter "$PATTERN")*")
fi

status=0
matched=0

# A pattern naming a test in one module leaves the other with nothing to run,
# which Gradle reports as a failure.
run_module() {
    local out
    if out="$(./gradlew "$1" "${args[@]}" 2>&1)"; then
        matched=1
        return
    fi
    if [[ -n "$PATTERN" && "$out" == *"No tests found for given includes"* ]]; then
        return
    fi
    matched=1
    # Gradle's closing advice is about Gradle, not about the failure.
    echo "$out" | grep -v '^\* Try:\|^> Run with \|^> Get more help '
    status=1
}

# Conversion only; verification is left to update-goldens.sh and check-all.sh.
run_module :formver.compiler-plugin:untilConversion

# Locality has no verification stage, so its whole suite belongs to this loop.
run_module :formver.compiler-plugin:locality:test

if [[ "$matched" -eq 0 ]]; then
    echo "No test matches '$PATTERN'."
    exit 1
fi

if [[ "$status" -ne 0 ]]; then
    echo
    echo "Gradle strips expected/actual off golden-file assertions."
    echo "To see what differs:"
    echo
    echo "    ./scripts/run-test.sh ${PATTERN:-<pattern>}"
fi

exit "$status"
