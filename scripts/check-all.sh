#!/usr/bin/env bash
# check-all.sh — Everything CI enforces, in one command.
#
# Usage:
#   ./scripts/check-all.sh

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

status=0

# detekt, apiCheck and every module's test task.
./gradlew check --no-daemon || status=1

./scripts/check-testdata.sh || status=1

# Enforced by a CI workflow. No git hook is installed by default.
if command -v pre-commit >/dev/null; then
    pre-commit run --all-files || status=1
else
    echo "pre-commit not installed; CI runs it"
    status=1
fi

exit "$status"
