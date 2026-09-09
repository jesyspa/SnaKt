#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 3 ]]; then
    echo "Usage: $0 <test-name> <maximum-runs> <test-pattern>" >&2
    exit 2
fi

agency_dir="$(cd "$(dirname "$0")" && pwd)"
repo_dir="$(cd "$agency_dir/.." && pwd)"
test_name="$1"
maximum_runs="$2"
test_pattern="$3"
counts_file="$agency_dir/run-counts.tsv"

if [[ ! "$maximum_runs" =~ ^[1-9][0-9]*$ ]]; then
    echo "Maximum runs must be a positive integer." >&2
    exit 2
fi

completed_runs="$(awk -F '\t' -v name="$test_name" '$1 == name { print $2 }' "$counts_file")"
completed_runs="${completed_runs:-0}"
if (( completed_runs >= maximum_runs )); then
    echo "$test_name has already used all $maximum_runs permitted runs." >&2
    exit 2
fi

cd "$repo_dir"
if ./agent-scripts/test.sh --verify "$test_pattern"; then
    result=passed
    status=0
else
    result=failed
    status=1
fi

next_run=$((completed_runs + 1))
awk -F '\t' -v OFS='\t' -v name="$test_name" -v count="$next_run" -v result="$result" '
    $1 == name { $2 = count; $4 = result; found = 1 }
    { print }
    END { if (!found) print name, count, "'"$maximum_runs"'", result }
' "$counts_file" > "$counts_file.tmp"
mv "$counts_file.tmp" "$counts_file"

exit "$status"
