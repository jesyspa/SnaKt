#!/usr/bin/env bash
# check-verified.sh — Which tests record a verification failure as expected.
#
# A green suite means the goldens match, not that anything verified. The
# recorded verification diagnostics are the authority, so this reads them
# rather than the build output.
#
# Usage:
#   ./scripts/check-verified.sh              # list every test recording a failure
#   ./scripts/check-verified.sh Factorial    # exit 1 if it records a failure,
#                                            # 2 if there is no such test

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
    echo "$golden does not verify:"
    sed 's/^/  /' "$golden"
done < <(find "${TEST_DATA_DIRS[@]}" -name "*.viper.diag.txt" | sort)

if [[ -z "$PATTERN" ]]; then
    echo
    echo "$recorded test(s) record a verification failure."
    echo "Each one is only correct if it exists to pin down a known limitation."
    exit 0
fi

if [[ "$matched" -eq 0 ]]; then
    # A pattern naming nothing at all must not read like a clean result, so
    # separate "this test has no recorded failure" from "there is no such test".
    # testData names use dashes where the generated method uses underscores,
    # so compare both sides in the same spelling.
    if ! find "${TEST_DATA_DIRS[@]}" -name "*.kt" \
        | sed 's#.*/##; s#\.kt$##; s#-#_#g' \
        | grep -qi -- "${PATTERN//-/_}"; then
        echo "No test matches '$PATTERN'."
        exit 2
    fi
    echo "No recorded verification failure for '$PATTERN'."
    echo "That is not evidence it verified: a test that never ran looks the same."
    echo "To watch it run: ./scripts/run-test.sh $PATTERN"
    exit 0
fi

exit $((recorded > 0 ? 1 : 0))
