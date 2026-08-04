#!/usr/bin/env bash
# check-verified.sh — Which tests record a verification failure as expected.
#
# A green suite means the goldens match, not that anything verified. The
# recorded verification diagnostics are the authority, so this reads them
# rather than the build output.
#
# Usage:
#   ./scripts/check-verified.sh              # list every test recording a failure
#   ./scripts/check-verified.sh Max_of_two   # exit 1 if that test does not verify

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

TEST_DATA_DIRS=(
    formver.compiler-plugin/testData
    formver.compiler-plugin/locality/testData
)

PATTERN="${1:-}"

matched=0
recorded=0

while read -r golden; do
    stem="$(basename "$golden" .viper.diag.txt)"
    if [[ -n "$PATTERN" ]]; then
        shopt -s nocasematch
        [[ "$stem" == *"$PATTERN"* ]] || { shopt -u nocasematch; continue; }
        shopt -u nocasematch
    fi
    matched=$((matched + 1))
    [[ -s "$golden" ]] || continue
    recorded=$((recorded + 1))
    echo "$stem does not verify:"
    sed 's/^/  /' "$golden"
done < <(find "${TEST_DATA_DIRS[@]}" -name "*.viper.diag.txt" | sort)

if [[ -z "$PATTERN" ]]; then
    echo
    echo "$recorded test(s) record a verification failure."
    echo "Each one is only correct if it exists to pin down a known limitation."
    exit 0
fi

if [[ "$matched" -eq 0 ]]; then
    # Either the test verifies and has no diagnostics file, or the pattern is
    # wrong; run-test.sh distinguishes the two.
    echo "No verification diagnostics recorded for '$PATTERN'."
    echo "Confirm the test exists and runs: ./scripts/run-test.sh $PATTERN"
    exit 0
fi

exit $((recorded > 0 ? 1 : 0))
