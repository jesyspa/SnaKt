#!/usr/bin/env bash
# Agency test topic: existential quantifiers survive conversion and verification.
# Launch budget: 3 total launches. Record every launch in DIARY.md.

set -euo pipefail

agency_dir="$(cd "$(dirname "$0")" && pwd)"
repo_root="$(cd "$agency_dir/.." && pwd)"

cd "$repo_root"
./agent-scripts/test.sh --verify \
    formver.compiler-plugin/testData/diagnostics/verification/user_invariants/exists.kt
