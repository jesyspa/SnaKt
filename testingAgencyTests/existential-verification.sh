#!/usr/bin/env bash

set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Topic: verifier outcomes for existential witnesses, including trigger limits.
exec "$script_dir/run-limited.sh" verification \
    formver.compiler-plugin/testData/diagnostics/verification/user_invariants/exists.kt \
    "${1:-1}"
