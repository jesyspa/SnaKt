#!/usr/bin/env bash

set -euo pipefail

if [[ "$#" -lt 2 || "$#" -gt 3 ]]; then
    echo "Usage: $0 MODE PATTERN [RUNS]" >&2
    exit 2
fi

mode="$1"
pattern="$2"
runs="${3:-1}"

if [[ ! "$runs" =~ ^[1-3]$ ]]; then
    echo "RUNS must be an integer from 1 to 3; got: $runs" >&2
    exit 2
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

args=()
case "$mode" in
    conversion) ;;
    verification) args+=(--verify) ;;
    *)
        echo "MODE must be conversion or verification; got: $mode" >&2
        exit 2
        ;;
esac

for ((run = 1; run <= runs; run++)); do
    echo "Agency run $run/$runs: $mode test for $pattern"
    "$repo_root/agent-scripts/test.sh" "${args[@]}" "$pattern"
done
