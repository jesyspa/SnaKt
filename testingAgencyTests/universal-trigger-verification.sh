#!/usr/bin/env bash

set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Topic: verification of universal quantifiers with explicit trigger terms.
exec "$script_dir/run-limited.sh" verification \
    formver.compiler-plugin/testData/diagnostics/verification/user_invariants/forall_with_triggers.kt \
    "${1:-1}"
