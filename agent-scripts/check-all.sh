#!/usr/bin/env bash
# check-all.sh — Everything CI enforces, in one command. --help for usage.

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

usage() {
    cat <<'EOF'
Usage:
  ./agent-scripts/check-all.sh
  ./agent-scripts/check-all.sh --rerun   # re-execute tests gradle considers current
EOF
}

args=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        # An UP-TO-DATE test task is green without executing anything.
        --rerun) args+=(--rerun) ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; echo >&2; usage >&2; exit 1 ;;
    esac
    shift
done

gradle_result=passed
# detekt, apiCheck and every module's test task.
./gradlew check --no-daemon "${args[@]}" || gradle_result=failed

testdata_result=passed
# Called directly as well as through pre-commit, so it runs even when
# pre-commit is missing.
./agent-scripts/check-testdata.sh || testdata_result=failed

# CI enforces this; no git hook is installed by default.
if command -v pre-commit >/dev/null; then
    precommit_result=passed
    pre-commit run --all-files || precommit_result=failed
else
    precommit_result=skipped
fi

echo
echo "Summary:"
printf '  %-18s %s\n' "gradle check:" "$gradle_result"
printf '  %-18s %s\n' "check-testdata.sh:" "$testdata_result"
if [[ "$precommit_result" == skipped ]]; then
    printf '  %-18s %s (install: pip install pre-commit)\n' "pre-commit:" "$precommit_result"
else
    printf '  %-18s %s\n' "pre-commit:" "$precommit_result"
fi

failed=0
skipped=0
for result in "$gradle_result" "$testdata_result" "$precommit_result"; do
    [[ "$result" == failed ]] && failed=1
    [[ "$result" == skipped ]] && skipped=1
done

if [[ "$failed" == 1 ]]; then
    exit 1
fi

# A skip is not a pass: the run covered less than this script promises, and the
# gap is in the setup, not the code. Distinct from 1, still to be fixed.
if [[ "$skipped" == 1 ]]; then
    echo
    echo "Exit 2: nothing failed, but a check above did not run. Install what it"
    echo "needs and run again for a clean 0."
    exit 2
fi

exit 0
