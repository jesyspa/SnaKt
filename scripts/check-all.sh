#!/usr/bin/env bash
# check-all.sh — Everything CI enforces, in one command.
#
# Usage:
#   ./scripts/check-all.sh
#   ./scripts/check-all.sh --rerun   # re-execute tests gradle considers current

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

status=0

args=()
if [[ "${1:-}" == "--rerun" ]]; then
    # An UP-TO-DATE test task is green without executing anything. That is a
    # true statement about unchanged inputs, and not always the question.
    args+=(--rerun)
fi

# detekt, apiCheck and every module's test task.
./gradlew check --no-daemon "${args[@]}" || status=1

./scripts/check-testdata.sh || status=1

# Enforced by a CI workflow. No git hook is installed by default.
if command -v pre-commit >/dev/null; then
    pre-commit run --all-files || status=1
else
    echo "pre-commit not installed; CI runs it"
    status=1
fi

exit "$status"
