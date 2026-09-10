#!/usr/bin/env bash

set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Topic: translating existential quantifiers in contracts and loop invariants.
exec "$script_dir/run-limited.sh" conversion \
    formver.compiler-plugin/testData/diagnostics/verification/user_invariants/exists.kt \
    "${1:-1}"
