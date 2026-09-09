#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
agency_dir="$(cd "$script_dir/.." && pwd)"
repo_dir="$(cd "$agency_dir/.." && pwd)"
run_dir="$agency_dir/run-counts"
run_file="$run_dir/existential-list-get-purity.count"
run_limit=3

mkdir -p "$run_dir"
run_count=0
if [[ -f "$run_file" ]]; then
    read -r run_count < "$run_file"
fi

if (( run_count >= run_limit )); then
    echo "SKIP: existential List.get purity check reached its $run_limit-run limit."
    exit 0
fi

next_count=$((run_count + 1))
printf '%s\n' "$next_count" > "$run_file"
echo "Run $next_count/$run_limit: existential List.get purity rejection"

cd "$repo_dir"
./agent-scripts/test.sh \
    formver.compiler-plugin/testData/diagnostics/verification/user_invariants/exists_list_get_crash.kt
