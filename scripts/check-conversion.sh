#!/usr/bin/env bash
# check-conversion.sh — The fast loop: everything that runs without Z3.
#
# Usage:
#   ./scripts/check-conversion.sh                # every test
#   ./scripts/check-conversion.sh Assign_local   # one test

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

PATTERN="${1:-}"

args=(--no-daemon -q)
if [[ -n "$PATTERN" ]]; then
    args+=(--tests "*$PATTERN*")
fi

status=0

# Conversion only; verification is left to update-goldens.sh and check-all.sh.
./gradlew :formver.compiler-plugin:untilConversion "${args[@]}" || status=1

# Locality has no verification stage, so its whole suite belongs to this loop.
./gradlew :formver.compiler-plugin:locality:test "${args[@]}" || status=1

if [[ "$status" -ne 0 ]]; then
    cat <<'EOF'

Gradle strips expected/actual off golden-file assertions. To see what differs:

    ./scripts/run-test.sh <pattern>
EOF
fi

exit "$status"
