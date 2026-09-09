#!/usr/bin/env bash

set -euo pipefail

agency_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "$agency_dir/.." && pwd)"

cd "$repo_dir"
exec ./agent-scripts/test.sh \
    formver.compiler-plugin/testData/diagnostics/verification/user_invariants/exists_list_get_crash.kt

