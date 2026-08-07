#!/usr/bin/env bash
# check-all.sh — Everything CI enforces, in one command. --help for usage.

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

usage() {
    cat <<'EOF'
Usage:
  ./scripts/check-all.sh
  ./scripts/check-all.sh --rerun   # re-execute tests gradle considers current
EOF
}

args=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        # An UP-TO-DATE test task is green without executing anything. That is
        # a true statement about unchanged inputs, and not always the question.
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
# Also registered as a pre-commit hook; called directly here too, so this
# still runs when pre-commit isn't installed.
./scripts/check-testdata.sh || testdata_result=failed

# Enforced by a CI workflow. No git hook is installed by default.
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
elif [[ "$skipped" == 1 ]]; then
    exit 2
else
    exit 0
fi
